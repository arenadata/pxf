package org.greenplum.pxf.plugins.tkh.distribution;

public interface DistributionManager {
    /**
     * Choose a host among the available ones
     */
    public void chooseHost();

    /**
     * @return chosen host URL
     */
    String getHost() throws IllegalStateException;
}
