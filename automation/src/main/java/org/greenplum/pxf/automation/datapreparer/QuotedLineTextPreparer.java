package org.greenplum.pxf.automation.datapreparer;

import java.util.ArrayList;

import org.apache.commons.lang.StringUtils;

import org.greenplum.pxf.automation.fileformats.IDataPreparer;
import org.greenplum.pxf.automation.structures.tables.basic.Table;

/**
 * Prepare quoted multilined fields data
 */
public class QuotedLineTextPreparer implements IDataPreparer {

	private String lineSeparator = System.lineSeparator();

	public QuotedLineTextPreparer() {
		// in case "line.separator" property had empty value put '\n'
		if (StringUtils.isEmpty(lineSeparator)) {
			lineSeparator = "\n";
		}
	}

	@Override
	public Object[] prepareData(int rows, Table dataTable) {

		Object[] data = new Object[rows];

		// run from 0 to rows and generate data into dataTable and data
		for (int i = 0, num1 = 1; i < rows; i++, num1++) {

			ArrayList<String> row = new ArrayList<>();

			// on every even i, add new line in the second field
            row.add(String.valueOf(num1));
            if (i % 2 != 0) {
                row.add("\"aaa_" + num1 + "\"");
            } else {
                row.add("\"aaa_" + num1 + lineSeparator + "_c\"");
            }
            row.add(String.valueOf(num1 + 1));
            dataTable.addRow(row);
            data[i] = row;
        }
		return data;
	}
}