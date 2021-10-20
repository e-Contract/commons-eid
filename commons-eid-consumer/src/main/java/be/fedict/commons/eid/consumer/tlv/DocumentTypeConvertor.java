/*
 * Commons eID Project.
 * Copyright (C) 2008-2013 FedICT.
 * Copyright (C) 2017-2021 e-Contract.be BV.
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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import be.fedict.commons.eid.consumer.DocumentType;

/**
 * Data Convertor for eID document type.
 * 
 * @author Frank Cornelis
 * 
 */
public class DocumentTypeConvertor implements DataConvertor<DocumentType> {

	private static final Logger LOGGER = LoggerFactory.getLogger(DocumentTypeConvertor.class);

	@Override
	public DocumentType convert(final byte[] value) throws DataConvertorException {
		LOGGER.debug("# bytes for document type field: {}", value.length);
		/*
		 * More recent eID cards use 2 bytes per default for the document type field.
		 */
		final DocumentType documentType = DocumentType.toDocumentType(value);
		if (null == documentType) {
			LOGGER.warn("unknown document type: {}", DocumentType.toString(value));
		}
		return documentType;
	}
}
