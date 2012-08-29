/*
 * Commons eID Project.
 * Copyright (C) 2008-2012 FedICT.
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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import be.fedict.commons.eid.consumer.DocumentType;

/**
 * Data Convertor for eID document type.
 * 
 * @author Frank Cornelis
 * 
 */
public class DocumentTypeConvertor implements DataConvertor<DocumentType> {

	private static final Log LOG = LogFactory
			.getLog(DocumentTypeConvertor.class);

	public DocumentType convert(final byte[] value)
			throws DataConvertorException {
		LOG.debug("# bytes for document type field: " + value.length);
		/*
		 * More recent eID cards use 2 bytes per default for the document type
		 * field.
		 */
		final DocumentType documentType = DocumentType.toDocumentType(value);
		if (null == documentType) {
			LOG.debug("unknown document type: " + DocumentType.toString(value));
		}
		return documentType;
	}
}
