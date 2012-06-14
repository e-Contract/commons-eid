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

package test.integ.be.fedict.commons.eid.client;

import static org.junit.Assert.assertNotNull;

import java.io.ByteArrayInputStream;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;

import javax.smartcardio.CardTerminal;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.Test;

import be.fedict.commons.eid.client.BeIDCard;
import be.fedict.commons.eid.client.BeIDFileType;
import be.fedict.commons.eid.client.CardManager;
import be.fedict.commons.eid.server.BeIDIntegrity;
import be.fedict.commons.eid.server.Identity;

public class BeIDCardTest {

	private static final Log LOG = LogFactory.getLog(BeIDCardTest.class);

	@Test
	public void testReadFiles() throws Exception {
		TestLogger logger = new TestLogger();
		CardManager cardManager = new CardManager(logger);
		CardTerminal cardTerminal = cardManager.findFirstBeIDCardTerminal();

		assertNotNull(cardTerminal);

		BeIDCard beIDCard = new BeIDCard(cardTerminal, logger);

		beIDCard.addCardListener(new TestBeIDCardListener());

		LOG.debug("reading identity file");
		byte[] identityFile = beIDCard.readFile(BeIDFileType.Identity);
		LOG.debug("reading identity signature file");
		byte[] identitySignatureFile = beIDCard
				.readFile(BeIDFileType.IdentitySignature);
		LOG.debug("reading RRN certificate file");
		byte[] rrnCertificateFile = beIDCard
				.readFile(BeIDFileType.RRNCertificate);

		CertificateFactory certificateFactory = CertificateFactory
				.getInstance("X.509");
		X509Certificate rrnCertificate = (X509Certificate) certificateFactory
				.generateCertificate(new ByteArrayInputStream(
						rrnCertificateFile));

		beIDCard.close();

		BeIDIntegrity beIDIntegrity = new BeIDIntegrity();
		Identity identity = beIDIntegrity.getVerifiedIdentity(identityFile,
				identitySignatureFile, rrnCertificate);

		assertNotNull(identity);
		assertNotNull(identity.getNationalNumber());
	}
}
