/*
 * Commons eID Project.
 * Copyright (C) 2008-2017 FedICT.
 * Copyright (C) 2017 Peter Mylemans.
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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import be.fedict.commons.eid.consumer.DateMask;

/**
 * Convertor for eID date of birth mask field.
 *
 * @author Peter Mylmeans
 */
public class DateOfBirthMaskDataConvertor implements DataConvertor<DateMask> {

	private static final Logger LOGGER = LoggerFactory.getLogger(DateOfBirthMaskDataConvertor.class);

	@Override
	public DateMask convert(final byte[] value) throws DataConvertorException {
		String dateOfBirthStr;
		dateOfBirthStr = new String(value, StandardCharsets.UTF_8).trim();
		LOGGER.debug("\"{}\"", dateOfBirthStr);

		if (dateOfBirthStr.length() == 4) {
			return DateMask.YYYY;
		} else {
			return DateMask.YYYY_MM_DD;
		}
	}
}
