package org.greenplum.pxf.automation.utils.curl;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Utility class to interact with curl
 */
public class CurlUtils {
    private static final String PROTOCOL = "http";
    private static final String DEFAULT_REQUEST = "GET";
    private String command;

    public CurlUtils(String host, String port, String path) {
        Map<String, String> headers = new HashMap<>();
        headers.put("X-GP-USER", "hacker");
        formCommand(host, port, path, DEFAULT_REQUEST, headers, null);
    }
    public CurlUtils(String host, String port, String path, String requestType, Map<String, String> headers, List<String> params) {
        formCommand(host, port, path, requestType, headers, params);
    }

    public String getCommand() {
        return command;
    }

    private void formCommand(String host, String port, String path, String requestType, Map<String, String> headers, List<String> params) {
        StringBuilder str = new StringBuilder();
        str.append("curl -X ").append(requestType);
        str.append(" \"" + PROTOCOL + "://").append(host).append(":").append(port).append("/").append(path).append("\" ");

        if (headers != null && !headers.isEmpty()) {
            for (Map.Entry<String, String> entry : headers.entrySet()) {
                str.append("--header \"").append(entry.getKey()).append("=").append(entry.getValue()).append("\" ");
            }
        }

        if (params != null && !params.isEmpty()) {
            for (String param: params) {
                str.append("--data \"").append(param).append("\" ");
            }
        }
        command = str.toString();
    }

}
