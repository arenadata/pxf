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
static void BuildUriForCancel(PxfFdwScanState *pxfsstate);
static void BuildUriForRead(PxfFdwScanState *pxfsstate);
static void BuildUriForWrite(PxfFdwModifyState *pxfmstate);
#if PG_VERSION_NUM >= 90600
static size_t FillBuffer(PxfFdwScanState *pxfsstate, char *start, int minlen, int maxlen);
#else
static size_t FillBuffer(PxfFdwScanState *pxfsstate, char *start, size_t size);
#endif

static void
PxfBridgeExportAbortCallback(ResourceReleasePhase phase,
							 bool isCommit,
							 bool isTopLevel,
							 void *arg)
{
	PxfFdwModifyState *pxfmstate = arg;

	if (phase != RESOURCE_RELEASE_AFTER_LOCKS)
		return;

	if (pxfmstate->common->owner == CurrentResourceOwner)
	{
		if (isCommit)
			elog(LOG, "pxf BridgeExport reference leak: %p still referenced", arg);

		PxfBridgeCleanup(pxfmstate);
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

	UnregisterResourceReleaseCallback(PxfBridgeExportAbortCallback, pxfmstate);

	churl_cleanup(pxfmstate->common->churl_handle, IsAbortInProgress());
	pxfmstate->common->churl_handle = NULL;

	churl_headers_cleanup(pxfmstate->common->churl_headers);
	pxfmstate->common->churl_headers = NULL;

	if (pxfmstate->common->uri.data)
	{
		pfree(pxfmstate->common->uri.data);
	}

	if (pxfmstate->common->options)
	{
		pfree(pxfmstate->common->options);
	}
}

static void
PxfBridgeImportAbortCallback(ResourceReleasePhase phase,
							 bool isCommit,
							 bool isTopLevel,
							 void *arg)
{
	PxfFdwScanState *pxfsstate = arg;

	if (phase != RESOURCE_RELEASE_AFTER_LOCKS)
		return;

	if (pxfsstate->common->owner == CurrentResourceOwner)
	{
		if (isCommit)
			elog(LOG, "pxf BridgeExport reference leak: %p still referenced", arg);

		PxfBridgeImportCleanup(pxfsstate);
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

	UnregisterResourceReleaseCallback(PxfBridgeImportAbortCallback, pxfsstate);

	if (IsAbortInProgress())
	{
		int savedInterruptHoldoffCount = InterruptHoldoffCount;
		long local_port = churl_get_local_port(pxfsstate->common->churl_handle);

		churl_cleanup(pxfsstate->common->churl_handle, true);
		pxfsstate->common->churl_handle = NULL;

		if (local_port > 0)
		{
			PG_TRY();
			{
				churl_headers_override(pxfsstate->common->churl_headers, "X-GP-CLIENT-PORT", psprintf("%li", local_port));

				BuildUriForCancel(pxfsstate);

				pxfsstate->common->churl_handle = churl_init_upload_timeout(pxfsstate->common->uri.data, pxfsstate->common->churl_headers, 1L);

				churl_cleanup(pxfsstate->common->churl_handle, false);
				pxfsstate->common->churl_handle = NULL;
			}
			PG_CATCH();
			{
				InterruptHoldoffCount = savedInterruptHoldoffCount;

				if (!elog_dismiss(WARNING))
				{
					FlushErrorState();
					elog(WARNING, "unable to dismiss error");
				}

				churl_cleanup(pxfsstate->common->churl_handle, true);
				pxfsstate->common->churl_handle = NULL;
			}
			PG_END_TRY();
		}
	}
	else
	{
		churl_cleanup(pxfsstate->common->churl_handle, IsAbortInProgress());
		pxfsstate->common->churl_handle = NULL;
	}

	churl_headers_cleanup(pxfsstate->common->churl_headers);
	pxfsstate->common->churl_headers = NULL;

	if (pxfsstate->common->uri.data)
	{
		pfree(pxfsstate->common->uri.data);
	}

	if (pxfsstate->common->options)
	{
		pfree(pxfsstate->common->options);
	}
}

/*
 * Sets up data before starting import
 */
void
PxfBridgeImportStart(PxfFdwScanState *pxfsstate)
{
	pxfsstate->common->churl_headers = churl_headers_init();

	BuildUriForRead(pxfsstate);
	BuildHttpHeaders(pxfsstate->common->churl_headers,
					 pxfsstate->common->options,
					 pxfsstate->relation,
					 pxfsstate->filter_str,
					 pxfsstate->retrieved_attrs,
					 pxfsstate->projectionInfo);

	pxfsstate->common->churl_handle = churl_init_download(pxfsstate->common->uri.data, pxfsstate->common->churl_headers);
	pxfsstate->common->owner = CurrentResourceOwner;

	RegisterResourceReleaseCallback(PxfBridgeImportAbortCallback, pxfsstate);

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
	pxfmstate->common->churl_headers = churl_headers_init();
	BuildHttpHeaders(pxfmstate->common->churl_headers,
					 pxfmstate->common->options,
					 pxfmstate->relation,
					 NULL,
					 NULL,
					 NULL);
	pxfmstate->common->churl_handle = churl_init_upload(pxfmstate->common->uri.data, pxfmstate->common->churl_headers);
	pxfmstate->common->owner = CurrentResourceOwner;

	RegisterResourceReleaseCallback(PxfBridgeExportAbortCallback, pxfmstate);
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
		 PXF_SEGMENT_ID, n, pxfsstate->common->options->resource);

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
		n = churl_write(pxfmstate->common->churl_handle, databuf, datalen);
		elog(DEBUG5, "pxf PxfBridgeWrite: segment %d wrote %zu bytes to %s", PXF_SEGMENT_ID, n, pxfmstate->common->options->resource);
	}

	return (int) n;
}

/*
 * Format the URI for cancel by adding PXF service endpoint details
 */
static void
BuildUriForCancel(PxfFdwScanState *pxfsstate)
{
	PxfOptions *options = pxfsstate->common->options;

	resetStringInfo(&pxfsstate->common->uri);
	appendStringInfo(&pxfsstate->common->uri, "http://%s:%d/%s/cancel", options->pxf_host, options->pxf_port, PXF_SERVICE_PREFIX);
	elog(DEBUG2, "pxf_fdw: uri %s for cancel", pxfsstate->common->uri.data);
}

/*
 * Format the URI for reading by adding PXF service endpoint details
 */
static void
BuildUriForRead(PxfFdwScanState *pxfsstate)
{
	PxfOptions *options = pxfsstate->common->options;

	resetStringInfo(&pxfsstate->common->uri);
	appendStringInfo(&pxfsstate->common->uri, "http://%s:%d/%s/read", options->pxf_host, options->pxf_port, PXF_SERVICE_PREFIX);
	elog(DEBUG2, "pxf_fdw: uri %s for read", pxfsstate->common->uri.data);
}

/*
 * Format the URI for writing by adding PXF service endpoint details
 */
static void
BuildUriForWrite(PxfFdwModifyState *pxfmstate)
{
	PxfOptions *options = pxfmstate->common->options;

	resetStringInfo(&pxfmstate->common->uri);
	appendStringInfo(&pxfmstate->common->uri, "http://%s:%d/%s/write", options->pxf_host, options->pxf_port, PXF_SERVICE_PREFIX);
	elog(DEBUG2, "pxf_fdw: uri %s with file name for write: %s", pxfmstate->common->uri.data, options->resource);
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
