/*
 * Commons eID Project.
 * Copyright (C) 2008-2013 FedICT.
 * Copyright (C) 2017-2024 e-Contract.be BV.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License version
 * 3.0 as published by the Free Software Foundation.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, see 
 * http://www.gnu.org/licenses/.
 */

package be.fedict.commons.eid.consumer.tlv;

import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.util.GregorianCalendar;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Convertor for eID date of birth field.
 * 
 * @author Frank Cornelis
 * 
 */
public class DateOfBirthDataConvertor implements DataConvertor<GregorianCalendar> {

	private static final Logger LOGGER = LoggerFactory.getLogger(DateOfBirthDataConvertor.class);

	@Override
	public GregorianCalendar convert(final byte[] value) throws DataConvertorException {
		String dateOfBirthStr;
		dateOfBirthStr = new String(value, StandardCharsets.UTF_8).trim();
		LOGGER.debug("\"{}\"", dateOfBirthStr);
		/*
		 * First try to detect the German format as there are cases in which a German
		 * format contains both dots and spaces.
		 */
		int spaceIdx = dateOfBirthStr.indexOf('.');
		if (-1 == spaceIdx) {
			spaceIdx = dateOfBirthStr.indexOf(' ');
		}

		if (spaceIdx > 0) {
			final String dayStr = dateOfBirthStr.substring(0, spaceIdx);
			LOGGER.debug("day: \"{}\"", dayStr);
			final int day = Integer.parseInt(dayStr);
			String monthStr = dateOfBirthStr.substring(spaceIdx + 1, dateOfBirthStr.length() - 4 - 1);
			if (monthStr.endsWith(".")) {
				monthStr = monthStr.substring(0, monthStr.length() - 1);
			}
			LOGGER.debug("month: \"{}\"", monthStr);
			final String yearStr = dateOfBirthStr.substring(dateOfBirthStr.length() - 4);
			LOGGER.debug("year: \"{}\"", yearStr);
			final int year = Integer.parseInt(yearStr);
			final int month = toMonth(monthStr);
			return new GregorianCalendar(year, month, day);
		}

		if (dateOfBirthStr.length() == 4) {
			/*
			 * "case II2b2". Only a birth year is given
			 * 
			 * there's no way of representing "missing" fields via GregorianCalendar, so we
			 * set Jan 1st
			 */
			return new GregorianCalendar(Integer.parseInt(dateOfBirthStr), 0, 1);
		}

		throw new DataConvertorException("Unsupported Birth Date Format [" + dateOfBirthStr + "]");
	}

	private static final String[][] MONTHS = new String[][] { new String[] { "JAN" }, new String[] { "FEV", "FEB" },
			new String[] { "MARS", "MAAR", "MÄR" }, new String[] { "AVR", "APR" }, new String[] { "MAI", "MEI" },
			new String[] { "JUIN", "JUN" }, new String[] { "JUIL", "JUL" }, new String[] { "AOUT", "AUG" },
			new String[] { "SEPT", "SEP" }, new String[] { "OCT", "OKT" }, new String[] { "NOV" },
			new String[] { "DEC", "DEZ" } };

	private int toMonth(String monthStr) throws DataConvertorException {
		monthStr = monthStr.trim();
		for (int monthIdx = 0; monthIdx < MONTHS.length; monthIdx++) {
			final String[] monthNames = MONTHS[monthIdx];
			for (String monthName : monthNames) {
				if (monthName.equals(monthStr)) {
					return monthIdx;
				}
			}
		}
		throw new DataConvertorException("unknown month: " + monthStr);
	}
}
