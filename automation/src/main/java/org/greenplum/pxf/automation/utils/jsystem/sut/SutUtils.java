package org.greenplum.pxf.automation.utils.jsystem.sut;

import jsystem.framework.sut.Sut;

import org.w3c.dom.DOMException;

/**
 * Utilities for working and parsing SUT
 */
public abstract class SutUtils {

	/**
	 * Gets value from SUT file
	 * 
	 * @param sut - the tag with cluster description
	 * @param xpath - the xpath
	 * @return value from given xpath in given sut file 
	 * @throws DOMException if an error occurs
	 * @throws Exception if an error occurs
	 */
	public static String getValue(Sut sut, String xpath) throws DOMException,
			Exception {

		return sut.getAllValues(xpath).get(0).getTextContent();
	}
}
