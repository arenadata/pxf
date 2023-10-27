/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 *
 */

#include "pxf_bridge.h"
#include "pxf_header.h"

#include "cdb/cdbtm.h"
#include "cdb/cdbvars.h"
#include "access/xact.h"

/* helper function declarations */
static void PxfBridgeCancel(PxfFdwCommonState *common);
static char *BuildUriForCancel(PxfFdwCommonState *common);
static void BuildUriForRead(PxfFdwScanState *pxfsstate);
static void BuildUriForWrite(PxfFdwModifyState *pxfmstate);
#if PG_VERSION_NUM >= 90600
static size_t FillBuffer(PxfFdwScanState *pxfsstate, char *start, int minlen, int maxlen);
#else
static size_t FillBuffer(PxfFdwScanState *pxfsstate, char *start, size_t size);
#endif

static void
PxfBridgeAbortCallback(ResourceReleasePhase phase,
							 bool isCommit,
							 bool isTopLevel,
							 void *arg)
{
	PxfFdwCommonState *common = arg;

	if (phase != RESOURCE_RELEASE_AFTER_LOCKS)
		return;

	if (common->owner == CurrentResourceOwner)
	{
		if (isCommit)
			elog(LOG, "pxf BridgeExport reference leak: %p still referenced", arg);

		PxfBridgeCancel(common);
	}
}

static void
PxfBridgeCancel(PxfFdwCommonState *common)
{
	UnregisterResourceReleaseCallback(PxfBridgeAbortCallback, common);

	long local_port = churl_get_local_port(common->churl_handle);

	if (local_port > 0)
	{
		int savedInterruptHoldoffCount = InterruptHoldoffCount;

		PG_TRY();
		{
			churl_headers_append(common->churl_headers, "X-GP-CLIENT-PORT", psprintf("%li", local_port));

			char *uri = BuildUriForCancel(common);

			CHURL_HANDLE churl_handle = churl_init_upload_timeout(uri, common->churl_headers, 1L);

			churl_cleanup(churl_handle, false);
		}
		PG_CATCH();
		{
			InterruptHoldoffCount = savedInterruptHoldoffCount;

			if (!elog_dismiss(WARNING))
			{
				FlushErrorState();
				elog(WARNING, "unable to dismiss error");
			}
		}
		PG_END_TRY();
	}
}

/*
 * Clean up churl related data structures from the PXF FDW scan state.
 */
void
PxfBridgeImportCleanup(PxfFdwScanState *pxfsstate)
{
	if (pxfsstate == NULL)
		return;

	UnregisterResourceReleaseCallback(PxfBridgeAbortCallback, pxfsstate->common);

	churl_cleanup(pxfsstate->common->churl_handle, false);
	pxfsstate->common->churl_handle = NULL;

	churl_headers_cleanup(pxfsstate->common->churl_headers);
	pxfsstate->common->churl_headers = NULL;

	if (pxfsstate->uri.data)
	{
		pfree(pxfsstate->uri.data);
	}

	if (pxfsstate->options)
	{
		pfree(pxfsstate->options);
	}
}

/*
 * Clean up churl related data structures from the PXF FDW modify state.
 */
void
PxfBridgeCleanup(PxfFdwModifyState *pxfmstate)
{
	if (pxfmstate == NULL)
		return;

	churl_cleanup(pxfmstate->churl_handle, false);
	pxfmstate->churl_handle = NULL;

	churl_headers_cleanup(pxfmstate->churl_headers);
	pxfmstate->churl_headers = NULL;

	if (pxfmstate->uri.data)
	{
		pfree(pxfmstate->uri.data);
	}

	if (pxfmstate->options)
	{
		pfree(pxfmstate->options);
	}
}

/*
 * Sets up data before starting import
 */
void
PxfBridgeImportStart(PxfFdwScanState *pxfsstate)
{
	pxfsstate->common = palloc0(sizeof(PxfFdwCommonState));
	pxfsstate->common->pxf_host = pstrdup(pxfsstate->options->pxf_host);
	pxfsstate->common->pxf_port = pxfsstate->options->pxf_port;
	pxfsstate->common->churl_headers = churl_headers_init();

	BuildUriForRead(pxfsstate);
	BuildHttpHeaders(pxfsstate->common->churl_headers,
					 pxfsstate->options,
					 pxfsstate->relation,
					 pxfsstate->filter_str,
					 pxfsstate->retrieved_attrs,
					 pxfsstate->projectionInfo);

	pxfsstate->common->churl_handle = churl_init_download(pxfsstate->uri.data, pxfsstate->common->churl_headers);
	pxfsstate->common->owner = CurrentResourceOwner;

	RegisterResourceReleaseCallback(PxfBridgeAbortCallback, pxfsstate->common);

	/* read some bytes to make sure the connection is established */
	churl_read_check_connectivity(pxfsstate->common->churl_handle);
}

/*
 * Sets up data before starting export
 */
void
PxfBridgeExportStart(PxfFdwModifyState *pxfmstate)
{
	BuildUriForWrite(pxfmstate);
	pxfmstate->churl_headers = churl_headers_init();
	BuildHttpHeaders(pxfmstate->churl_headers,
					 pxfmstate->options,
					 pxfmstate->relation,
					 NULL,
					 NULL,
					 NULL);
	pxfmstate->churl_handle = churl_init_upload(pxfmstate->uri.data, pxfmstate->churl_headers);
}

/*
 * Reads data from the PXF server into the given buffer of a given size
 */
int
#if PG_VERSION_NUM >= 90600
PxfBridgeRead(void *outbuf, int minlen, int maxlen, void *extra)
#else
PxfBridgeRead(void *outbuf, int datasize, void *extra)
#endif
{
	size_t		n = 0;
	PxfFdwScanState *pxfsstate = (PxfFdwScanState *) extra;

#if PG_VERSION_NUM >= 90600
	n = FillBuffer(pxfsstate, outbuf, minlen, maxlen);
#else
	n = FillBuffer(pxfsstate, outbuf, datasize);
#endif

	if (n == 0)
	{
		/* check if the connection terminated with an error */
		churl_read_check_connectivity(pxfsstate->common->churl_handle);
	}

	elog(DEBUG5, "pxf PxfBridgeRead: segment %d read %zu bytes from %s",
		 PXF_SEGMENT_ID, n, pxfsstate->options->resource);

	return (int) n;
}

/*
 * Writes data from the given buffer of a given size to the PXF server
 */
int
PxfBridgeWrite(PxfFdwModifyState *pxfmstate, char *databuf, int datalen)
{
	size_t		n = 0;

	if (datalen > 0)
	{
		n = churl_write(pxfmstate->churl_handle, databuf, datalen);
		elog(DEBUG5, "pxf PxfBridgeWrite: segment %d wrote %zu bytes to %s", PXF_SEGMENT_ID, n, pxfmstate->options->resource);
	}

	return (int) n;
}

/*
 * Format the URI for cancel by adding PXF service endpoint details
 */
static char *
BuildUriForCancel(PxfFdwCommonState *common)
{
	StringInfoData uri;

	initStringInfo(&uri);
	appendStringInfo(&uri, "http://%s:%d/%s/cancel", common->pxf_host, common->pxf_port, PXF_SERVICE_PREFIX);
	elog(DEBUG2, "pxf_fdw: uri %s for cancel", uri.data);
	return uri.data;
}

/*
 * Format the URI for reading by adding PXF service endpoint details
 */
static void
BuildUriForRead(PxfFdwScanState *pxfsstate)
{
	PxfOptions *options = pxfsstate->options;

	resetStringInfo(&pxfsstate->uri);
	appendStringInfo(&pxfsstate->uri, "http://%s:%d/%s/read", options->pxf_host, options->pxf_port, PXF_SERVICE_PREFIX);
	elog(DEBUG2, "pxf_fdw: uri %s for read", pxfsstate->uri.data);
}

/*
 * Format the URI for writing by adding PXF service endpoint details
 */
static void
BuildUriForWrite(PxfFdwModifyState *pxfmstate)
{
	PxfOptions *options = pxfmstate->options;

	resetStringInfo(&pxfmstate->uri);
	appendStringInfo(&pxfmstate->uri, "http://%s:%d/%s/write", options->pxf_host, options->pxf_port, PXF_SERVICE_PREFIX);
	elog(DEBUG2, "pxf_fdw: uri %s with file name for write: %s", pxfmstate->uri.data, options->resource);
}

/*
 * Read data from churl until the buffer is full or there is no more data to be read
 */
static size_t
#if PG_VERSION_NUM >= 90600
FillBuffer(PxfFdwScanState *pxfsstate, char *start, int minlen, int maxlen)
#else
FillBuffer(PxfFdwScanState *pxfsstate, char *start, size_t size)
#endif
{
	size_t		n = 0;
	char	   *ptr = start;
#if PG_VERSION_NUM >= 90600
	char	   *minend = ptr + minlen;
	char	   *maxend = ptr + maxlen;

	while (ptr < minend)
	{
		n = churl_read(pxfsstate->common->churl_handle, ptr, maxend - ptr);
#else
	char	   *end = ptr + size;

	while (ptr < end)
	{
		n = churl_read(pxfsstate->common->churl_handle, ptr, end - ptr);
#endif
		if (n == 0)
			break;

		ptr += n;
	}

	return ptr - start;
}
