/*
 * Commons eID Project.
 * Copyright (C) 2008-2013 FedICT.
 * Copyright (C) 2017 e-Contract.be BVBA.
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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import be.fedict.commons.eid.consumer.SpecialOrganisation;

/**
 * Data convertor for special organization eID identity field.
 * 
 * @author Frank Cornelis
 * 
 */
public class SpecialOrganisationConvertor implements DataConvertor<SpecialOrganisation> {

	private static final Logger LOGGER = LoggerFactory.getLogger(SpecialOrganisationConvertor.class);

	@Override
	public SpecialOrganisation convert(final byte[] value) throws DataConvertorException {
		if (null == value) {
			return SpecialOrganisation.UNSPECIFIED;
		}
		String key;
		try {
			key = new String(value, "UTF-8");
		} catch (final UnsupportedEncodingException uex) {
			throw new DataConvertorException("string error: " + uex.getMessage());
		}
		LOGGER.debug("key: \"{}\"", key);
		final SpecialOrganisation specialOrganisation = SpecialOrganisation.toSpecialOrganisation(key);
		return specialOrganisation;
	}
}
