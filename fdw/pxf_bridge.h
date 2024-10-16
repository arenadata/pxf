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

#ifndef _PXFBRIDGE_H
#define _PXFBRIDGE_H

#include "libchurl.h"

#include "pxf_option.h"

#include "commands/copy.h"
#include "cdb/cdbvars.h"
#include "nodes/execnodes.h"
#include "nodes/parsenodes.h"
#include "nodes/pg_list.h"

#define PXF_SEGMENT_ID                 GpIdentity.segindex
#define PXF_SEGMENT_COUNT              getgpsegmentCount()

/*
 * Execution state of a foreign scan using pxf_fdw.
 */
typedef struct PxfFdwScanState
{
	CHURL_HEADERS churl_headers;
	CHURL_HANDLE churl_handle;
	StringInfoData uri;
	Relation	relation;
	char	   *filter_str;
#if PG_VERSION_NUM >= 90600
	ExprState  *quals;
#else
	List	   *quals;
#endif
	List	   *retrieved_attrs;
	PxfOptions *options;
	CopyState	cstate;
	ProjectionInfo *projectionInfo;
	void           *pxfcstate;
} PxfFdwScanState;

/*
 * Execution state of a foreign insert operation.
 */
typedef struct PxfFdwModifyState
{
	CopyState	cstate;			/* state of writing to PXF */

	CHURL_HANDLE churl_handle;	/* curl handle */
	CHURL_HEADERS churl_headers;	/* curl headers */
	StringInfoData uri;			/* rest endpoint URI for modify */
	Relation	relation;
	PxfOptions *options;		/* FDW options */

#if PG_VERSION_NUM < 90600
	Datum	   *values;			/* List of values exported for the row */
	bool	   *nulls;			/* List of null fields for the exported row */
#endif
} PxfFdwModifyState;

/* Clean up churl related data structures from the context */
void		PxfBridgeImportCleanup(PxfFdwScanState *pxfsstate);
void		PxfBridgeCleanup(PxfFdwModifyState *context);

/* Sets up data before starting import */
void		PxfBridgeImportStart(PxfFdwScanState *pxfsstate);

/* Sets up data before starting export */
void		PxfBridgeExportStart(PxfFdwModifyState *pxfmstate);

/* Reads data from the PXF server into the given buffer of a given size */
#if PG_VERSION_NUM >= 90600
int			PxfBridgeRead(void *outbuf, int minlen, int maxlen, void *extra);
#else
int			PxfBridgeRead(void *outbuf, int datasize, void *extra);
#endif

/* Writes data from the given buffer of a given size to the PXF server */
int			PxfBridgeWrite(PxfFdwModifyState *context, char *databuf, int datalen);

#endif							/* _PXFBRIDGE_H */
