package org.greenplum.pxf.service.rest;

import org.greenplum.pxf.api.model.RequestContext;
import org.greenplum.pxf.service.HttpHeaderDecoder;
import org.greenplum.pxf.service.RequestParser;
import org.greenplum.pxf.service.controller.PxfErrorReporter;
import org.greenplum.pxf.service.controller.ReadService;
import org.greenplum.pxf.service.spring.PxfThreadPoolTaskExecutor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import javax.servlet.http.HttpServletRequest;

/**
 * PXF REST endpoint for read data requests.
 */
@RestController
@RequestMapping("/pxf")
public class PxfReadResource extends PxfBaseResource<StreamingResponseBody> {

    public static final String X_GP_CLIENT_PORT_HEADER = "x-gp-client-port";
    private final ReadService readService;

    /**
     * Creates a new instance of the resource with Request parser and read service implementation.
     *
     * @param parser      http request parser
     * @param readService read service implementation
     */
    public PxfReadResource(RequestParser<MultiValueMap<String, String>> parser,
                           ReadService readService) {
        super(RequestContext.RequestType.READ_BRIDGE, parser);
        this.readService = readService;
    }

    /**
     * REST endpoint for read data requests.
     *
     * @param headers http headers from request that carry all parameters
     * @return response object containing stream that will output records
     */
    @GetMapping(value = "/read", produces = MediaType.APPLICATION_OCTET_STREAM_VALUE)
    public ResponseEntity<StreamingResponseBody> read(@RequestHeader MultiValueMap<String, String> headers,
                                                      HttpServletRequest request) {
        // we need this header to uniquely identify the read request for possible cancellation
        headers.add(X_GP_CLIENT_PORT_HEADER, String.valueOf(request.getRemotePort()));
        return processRequest(headers, request);
    }

    /**
     * REST endpoint for canceling read data requests.
     *
     * @param headers http headers from request that carry all parameters
     * @return true if read request found and cancelled, false otherwise
     */
    @PostMapping(value = "/cancel", produces = MediaType.TEXT_PLAIN_VALUE)
    public ResponseEntity<String> cancel(@RequestHeader MultiValueMap<String, String> headers) {
        return ResponseEntity.ok(String.valueOf(readService.cancelRead(parser.parseRequest(headers, requestType))));
    }

    @Override
    protected StreamingResponseBody produceResponse(RequestContext context, HttpServletRequest request) {
        // return a lambda that will be executed asynchronously
        return os -> readService.readData(context, os);
    }
}
