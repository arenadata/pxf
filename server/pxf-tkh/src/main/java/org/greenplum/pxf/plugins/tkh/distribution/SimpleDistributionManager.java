package org.greenplum.pxf.plugins.tkh.distribution;

import java.util.Random;
import java.util.stream.Stream;

import org.apache.commons.lang3.StringUtils;

/**
 * A simple host manager that chooses a host from settings string randomly
 */
public class SimpleDistributionManager implements DistributionManager {
    String[] hosts;
    Random random;

    String chosen = null;

    /**
     * Construct a SimpleDistributionManager.
     *
     * @param urlString a string consisting of comma-separated list of hosts.
     * After split, whitespace is trimmed.
     */
    public SimpleDistributionManager(String urlString) {
        if (urlString == null) {
            throw new IllegalArgumentException("\"URL\" setting is required for this host distribution");
        }

        Object[] hostsRaw = Stream.of(urlString.split(",")).map(String::trim).filter(StringUtils::isNotBlank).toArray();
        if (hostsRaw.length == 0) {
            throw new IllegalArgumentException(String.format(
                "The provided setting \"URL\" '%s' has invalid format. The correct format of this setting is a comma-separated list of hosts. Trailing whitespace characters for each host name are discarded.",
                urlString
            ));
        }
        hosts = new String[hostsRaw.length];
        for (int i = 0; i < hosts.length; i++) {
            hosts[i] = String.class.cast(hostsRaw[i]);
        }

        random = new Random();
    }

    @Override
    public void chooseHost() {
        chosen = hosts[random.nextInt(hosts.length)];
    }

    @Override
    public String getHost() throws IllegalStateException {
        if (chosen == null) {
            throw new IllegalStateException("chooseHost() must be called before getHost()");
        }
        return chosen;
    }
}
