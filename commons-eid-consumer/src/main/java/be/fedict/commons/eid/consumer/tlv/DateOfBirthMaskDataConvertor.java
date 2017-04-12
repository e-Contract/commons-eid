/*
 * Commons eID Project.
 * Copyright (C) 2017 Peter Mylemans.
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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import be.fedict.commons.eid.consumer.DateMask;

/**
 * Convertor for eID date of birth mask field.
 *
 * @author Peter Mylmeans
 */
public class DateOfBirthMaskDataConvertor implements DataConvertor<DateMask> {

    private static final Log LOG = LogFactory.getLog(DateOfBirthMaskDataConvertor.class);

    @Override
    public DateMask convert(final byte[] value) throws DataConvertorException {
        String dateOfBirthStr;
        try {
            dateOfBirthStr = new String(value, "UTF-8").trim();
        } catch (final UnsupportedEncodingException uex) {
            throw new DataConvertorException("UTF-8 not supported");
        }
        LOG.debug("\"" + dateOfBirthStr + "\"");

        if (dateOfBirthStr.length() == 4) {
            return DateMask.YYYY;
        } else {
            return DateMask.YYYY_MM_DD;
        }
    }

}
