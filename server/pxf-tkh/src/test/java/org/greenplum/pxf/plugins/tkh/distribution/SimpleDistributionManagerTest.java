package org.greenplum.pxf.plugins.tkh.distribution;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.Assert.assertArrayEquals;

public class SimpleDistributionManagerTest {
    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Test
    public void testOneHost() {
        Set<String> expected = new HashSet<String>();
        expected.add("clickhouse:does");

        String url = "clickhouse:does";

        DistributionManager dm = new SimpleDistributionManager(url);

        checkChosenHostSetDifference(expected, dm);
    }

    @Test
    public void testTwoHosts() {
        Set<String> expected = new HashSet<String>();
        expected.add("clickhouse:does");
        expected.add("not");

        String url = "clickhouse:does,not";

        DistributionManager dm = new SimpleDistributionManager(url);

        checkChosenHostSetDifference(expected, dm);
    }

    @Test
    public void testThreeHosts() {
        Set<String> expected = new HashSet<String>();
        expected.add("clickhouse:does");
        expected.add("not");
        expected.add("slow:down");

        String url = "clickhouse:does,not,slow:down";

        DistributionManager dm = new SimpleDistributionManager(url);

        checkChosenHostSetDifference(expected, dm);
    }

    @Test
    public void testOneHostWithSpaces() {
        Set<String> expected = new HashSet<String>();
        expected.add("clickhouse:does");

        String url = " clickhouse:does\t";

        DistributionManager dm = new SimpleDistributionManager(url);

        checkChosenHostSetDifference(expected, dm);
    }

    @Test
    public void testTwoHostsWithSpaces() {
        Set<String> expected = new HashSet<String>();
        expected.add("clickhouse:does");
        expected.add("not");

        String url = "  clickhouse:does , \tnot ";

        DistributionManager dm = new SimpleDistributionManager(url);

        checkChosenHostSetDifference(expected, dm);
    }

    @Test
    public void testInvalidNull() {
        thrown.expect(IllegalArgumentException.class);

        new SimpleDistributionManager(null);
    }

    @Test
    public void testInvalidComma() {
        thrown.expect(IllegalArgumentException.class);

        String url = ",";

        new SimpleDistributionManager(url);
    }

    /**
     * Return the difference between {@link DistributionManager}-returned set of hosts and expected set of hosts
     * @param expected
     * @param dm
     * @return difference set converted to array
     */
    private String[] chosenHostSetDifference(Set<String> expected, DistributionManager dm) {
        final int CHECK_MULTIPLER = 10;

        Set<String> collectedHosts = new HashSet<String>();

        for (int i = 0; i < expected.size() * CHECK_MULTIPLER; i++) {
            dm.chooseHost();
            collectedHosts.add(dm.getHost());

            if (collectedHosts.size() > expected.size()) {
                collectedHosts.removeAll(expected);
                return collectedHosts.toArray(new String[]{});
            }
        }

        if (collectedHosts.size() != expected.size()) {
            collectedHosts.removeAll(expected);
            return collectedHosts.toArray(new String[]{});
        }

        for (int i = 0; i < expected.size() * CHECK_MULTIPLER; i++) {
            collectedHosts.add(dm.getHost());
        }

        collectedHosts.removeAll(expected);
        return collectedHosts.toArray(new String[]{});
    }

    /**
     * Incapsulates {@link SimpleDistributionManagerTest#chosenHostSetDifference}.
     * Also produces output when sets differ (JUnit does not)
     * @param expected
     * @param dm
     */
    private void checkChosenHostSetDifference(Set<String> expected, DistributionManager dm) {
        String[] difference = chosenHostSetDifference(expected, dm);
        if (difference.length > 0) {
            System.out.print(
                "Sets differ. Difference: {" +
                Stream.of(difference).map(s -> "'" + s + "'").collect(Collectors.joining(", ")) +
                "}"
            );
        }
        assertArrayEquals(new String[]{}, chosenHostSetDifference(expected, dm));
    }
}
