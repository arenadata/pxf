package org.greenplum.pxf.plugins.tkh;

import org.apache.commons.lang.StringUtils;
import org.greenplum.pxf.api.model.BasePlugin;
import org.greenplum.pxf.api.model.RequestContext;
import org.greenplum.pxf.plugins.tkh.distribution.*;

/**
 * Clickhouse plugin base. Responsible for settings parsing
 */
public class TkhPlugin extends BasePlugin {
    private static final String CH_DISTRIBUTION_TYPE_CONFIGURATION = "clickhouse.distribution";
    private static final String CH_URL_CONFIGURATION = "clickhouse.url";
    private static final String CH_BATCH_CONFIGURATION = "clickhouse.batch";
    private static final String CH_TIMEOUT_CONFIGURATION = "clickhouse.timeout";

    protected String tableName;

    protected int batchSize = BATCH_SIZE_DEFAULT;
    protected static final int BATCH_SIZE_DEFAULT = 49152;

    protected Integer networkTimeout = NETWORK_TIMEOUT_DEFAULT;
    protected static final int NETWORK_TIMEOUT_DEFAULT = 10000;

    protected DistributionManager distributionManager;

    @Override
    public void initialize(RequestContext context) {
        super.initialize(context);

        // Set distribution manager
        setDistributionManager();

        // Table name (required)
        tableName = context.getDataSource();
        if (StringUtils.isBlank(tableName)) {
            throw new IllegalArgumentException("Table name must be provided");
        }

        // Batch size (optional)
        batchSize = configuration.getInt(CH_BATCH_CONFIGURATION, BATCH_SIZE_DEFAULT);
        if (batchSize == 0) {
            batchSize = 1;
        } else if (batchSize < 0) {
            throw new IllegalArgumentException(String.format(
                    "Property %s has incorrect value %s : must be a non-negative integer", CH_BATCH_CONFIGURATION, batchSize));
        }

        // Timeout (optional)
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
     * Set the distribution manager
     */
    private void setDistributionManager() {
        DistributionType dType;

        String distributionTypeString = configuration.get(CH_DISTRIBUTION_TYPE_CONFIGURATION);
        if (distributionTypeString != null) {
            dType = DistributionType.valueOf(distributionTypeString);
        }
        else {
            dType = DistributionType.LIST;
        }

        switch (dType) {
            case LIST:
                String urlString = configuration.get(CH_URL_CONFIGURATION);
                distributionManager = new SimpleDistributionManager(urlString);
                break;
            default:
                throw new RuntimeException("Illegal distribution type");
        }

        distributionManager.chooseHost();
    }
}
