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
import be.fedict.commons.eid.client.BeIDCardEventsManager;
import be.fedict.commons.eid.client.BeIDFileType;
import be.fedict.commons.eid.client.event.BeIDCardEventsListener;
import be.fedict.commons.eid.consumer.BeIDIntegrity;
import be.fedict.commons.eid.consumer.Identity;

public class BeIDCardEventsManagerTest {

	private static final Log LOG = LogFactory
			.getLog(BeIDCardEventsManagerTest.class);

	@Test
	public void testReadFiles() throws Exception {
		TestLogger logger = new TestLogger();
		BeIDCardEventsManager beIDCardEventsManager = new BeIDCardEventsManager(
				logger);
		BeIDCard beIDCard = beIDCardEventsManager.getFirstBeIDCard();
		assertNotNull(beIDCard);

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

	@Test
	public void testListenerModification() throws Exception {
		TestLogger logger = new TestLogger();
		BeIDCardEventsManager beIDCardEventsManager = new BeIDCardEventsManager(
				logger);
		Object waitObject = new Object();
		beIDCardEventsManager
				.addBeIDCardEventListener(new BeIDCardEventsTestListener(
						beIDCardEventsManager, waitObject, true));
		beIDCardEventsManager
				.addBeIDCardEventListener(new BeIDCardEventsTestListener(
						beIDCardEventsManager, waitObject, false));
		beIDCardEventsManager.start();
		synchronized (waitObject) {
			waitObject.wait();
		}
	}

	private final class BeIDCardEventsTestListener implements
			BeIDCardEventsListener {

		private final Object waitObject;

		private final BeIDCardEventsManager manager;

		private final boolean removeAfterCardInserted;

		public BeIDCardEventsTestListener(BeIDCardEventsManager manager,
				Object waitObject, boolean removeAfterCardInserted) {
			this.manager = manager;
			this.waitObject = waitObject;
			this.removeAfterCardInserted = removeAfterCardInserted;
		}

		@Override
		public void eIDCardRemoved(CardTerminal cardTerminal, BeIDCard card) {
			LOG.debug("eID card removed");
			synchronized (this.waitObject) {
				this.waitObject.notify();
			}
		}

		@Override
		public void eIDCardInserted(CardTerminal cardTerminal, BeIDCard card) {
			LOG.debug("eID card added");
			if (this.removeAfterCardInserted) {
				this.manager.removeBeIDCardListener(this);
			}
		}
	}
}
