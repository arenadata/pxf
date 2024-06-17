package org.greenplum.pxf.plugins.jdbc;

import lombok.Getter;

import java.sql.Connection;
import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.DriverPropertyInfo;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.util.Properties;
import java.util.logging.Logger;

import static org.mockito.Mockito.mock;

public class FakeJdbcDriver implements Driver {
    @Getter
    private static final Driver mockDriver = mock(FakeJdbcDriver.class);
    static {
        try {
            DriverManager.registerDriver(mockDriver);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public Connection connect(String url, Properties info) throws SQLException {
        return null;
    }

    public boolean acceptsURL(String url) throws SQLException {
        return false;
    }

    public DriverPropertyInfo[] getPropertyInfo(String url, Properties info) throws SQLException {
        return new DriverPropertyInfo[0];
    }

    public int getMajorVersion() {
        return 0;
    }

    public int getMinorVersion() {
        return 0;
    }

    public boolean jdbcCompliant() {
        return false;
    }

    public Logger getParentLogger() throws SQLFeatureNotSupportedException {
        return null;
    }
}
