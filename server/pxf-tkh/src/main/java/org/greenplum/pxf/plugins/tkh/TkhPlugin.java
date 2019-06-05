package org.greenplum.pxf.plugins.tkh;

import org.apache.commons.lang.StringUtils;
import org.greenplum.pxf.api.model.BasePlugin;
import org.greenplum.pxf.api.model.RequestContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Clickhouse plugin base. Responsible for settings parsing
 */
public class TkhPlugin extends BasePlugin {
    private static final String CH_URL_CONFIGURATION = "clickhouse.url";
    private static final String CH_URL_OPTION = "URL";

    private static final String CH_BATCH_CONFIGURATION = "clickhouse.batch";

    private static final String CH_TIMEOUT_CONFIGURATION = "clickhouse.timeout";

    protected String host;
    protected String tableName;

    protected int batchSize = BATCH_SIZE_DEFAULT;
    protected static final int BATCH_SIZE_DEFAULT = 49152;

    protected Integer networkTimeout = NETWORK_TIMEOUT_DEFAULT;
    protected static final int NETWORK_TIMEOUT_DEFAULT = 10000;

    private static final Logger LOG = LoggerFactory.getLogger(TkhPlugin.class);

    @Override
    public void initialize(RequestContext context) {
        super.initialize(context);

        // Required parameter. Can be auto-overwritten by user options
        host = configuration.get(CH_URL_CONFIGURATION);
        assertMandatoryParameter(host, CH_URL_CONFIGURATION, CH_URL_OPTION);

        // Required metadata
        tableName = context.getDataSource();
        if (StringUtils.isBlank(tableName)) {
            throw new IllegalArgumentException("Data source must be provided");
        }

        // Optional parameters
        batchSize = configuration.getInt(CH_BATCH_CONFIGURATION, BATCH_SIZE_DEFAULT);

        if (batchSize == 0) {
            batchSize = 1;
        } else if (batchSize < 0) {
            throw new IllegalArgumentException(String.format(
                    "Property %s has incorrect value %s : must be a non-negative integer", CH_BATCH_CONFIGURATION, batchSize));
        }

        String queryTimeoutString = configuration.get(CH_TIMEOUT_CONFIGURATION);
        if (StringUtils.isNotBlank(queryTimeoutString)) {
            try {
                networkTimeout = Integer.parseUnsignedInt(queryTimeoutString);
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException(String.format(
                        "Property %s has incorrect value %s : must be a non-negative integer",
                        CH_TIMEOUT_CONFIGURATION, queryTimeoutString), e);
            }
        }
    }

    /**
     * Asserts whether a given parameter has non-empty value, throws IllegalArgumentException otherwise
     *
     * @param value      value to check
     * @param paramName  parameter name
     * @param optionName name of the option for a given parameter
     */
    private void assertMandatoryParameter(String value, String paramName, String optionName) {
        if (StringUtils.isBlank(value)) {
            throw new IllegalArgumentException(String.format(
                    "Required parameter %s is missing or empty in jdbc-site.xml and option %s is not specified in table definition.", paramName, optionName)
            );
        }
    }
}
