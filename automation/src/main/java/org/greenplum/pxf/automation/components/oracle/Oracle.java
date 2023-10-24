package org.greenplum.pxf.automation.components.oracle;

import org.greenplum.pxf.automation.components.common.DbSystemObject;
import org.greenplum.pxf.automation.structures.tables.basic.Table;
import org.greenplum.pxf.automation.utils.jsystem.report.ReportUtils;

import java.sql.DatabaseMetaData;
import java.sql.ResultSet;

/**
 * Oracle System Object
 */
public class Oracle extends DbSystemObject {

    private String password;

    public Oracle() {
    }

    public Oracle(boolean silenceReport) {
        super(silenceReport);
    }

    public String getPassword() {
        return password;
    }
    public String getDriver() {
        return driver;
    }

    @Override
    public void init() throws Exception {
        super.init();

        password = this.getProperty("password");
        if (userName == null) {
            userName = System.getProperty("user.name");
        }
        driver = "oracle.jdbc.driver.OracleDriver";
        address = "jdbc:oracle:thin:" + userName + "/" + password + "@" + host + ":" + port + "/ORCL";

        connect();
    }

    @Override
    public void createDataBase(String schemaName, boolean ignoreFail, String encoding, String localeCollate, String localeCollateType) throws Exception {
    }

    @Override
    public void createTableAndVerify(Table table) throws Exception {
        ReportUtils.startLevel(report, getClass(), "Create and Verify Table: " + table.getFullName());
        try {
            dropTable(table, true);
            createTable(table);
        } catch (Exception e) {
            ReportUtils.stopLevel(report);
            throw e;
        }
        if (!checkTableExists(table)) {
            ReportUtils.stopLevel(report);
            throw new Exception("Table " + table.getName() + " do not exists");
        }
        ReportUtils.stopLevel(report);
    }

    @Override
    public void dropTable(Table table, boolean cascade) throws Exception {
        String queryDropTable = "DROP TABLE " + table.getSchema() + "." + table.getName();
        runQuery(queryDropTable, true, false);
    }

    @Override
    public boolean checkTableExists(Table table) throws Exception {
        DatabaseMetaData metaData = dbConnection.getMetaData();
        String tableName = "";
        if (table != null) {
            tableName = table.getName().toUpperCase();
        }
        ResultSet res = metaData.getTables(null, null, "%" + tableName, new String[]{"TABLE", "FOREIGN TABLE"});
        return res.next();
    }

    public int getValueFromQuery(String query) throws Exception {
        ReportUtils.report(report, getClass(), "Get value - query: " + query);
        ResultSet res = stmt.executeQuery(query);
        res.next();
        int value = res.getInt(1);
        ReportUtils.report(report, getClass(), "Value: [" + value + "]");
        return value;
    }
}