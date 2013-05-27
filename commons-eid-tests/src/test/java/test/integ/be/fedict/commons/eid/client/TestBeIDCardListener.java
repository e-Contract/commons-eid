/*
 * Commons eID Project.
 * Copyright (C) 2008-2013 FedICT.
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

package test.integ.be.fedict.commons.eid.client;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import be.fedict.commons.eid.client.FileType;
import be.fedict.commons.eid.client.event.BeIDCardListener;

public class TestBeIDCardListener implements BeIDCardListener {

	private static final Log LOG = LogFactory
			.getLog(TestBeIDCardListener.class);

	@Override
	public void notifyReadProgress(final FileType fileType, final int offset,
			final int estimatedMaxSize) {
		LOG.debug("read progress of " + fileType.name() + ":" + offset + " of "
				+ estimatedMaxSize);
	}

	@Override
	public void notifySigningBegin(final FileType keyType) {
		LOG.debug("signing with "
				+ (keyType == FileType.AuthentificationCertificate
						? "authentication"
						: "non-repudiation") + " key has begun");
	}

	@Override
	public void notifySigningEnd(final FileType keyType) {
		LOG.debug("signing with "
				+ (keyType == FileType.AuthentificationCertificate
						? "authentication"
						: "non-repudiation") + " key has ended");
	}
}
