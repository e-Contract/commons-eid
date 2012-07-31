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
import be.fedict.commons.eid.client.BeIDCardManager;
import be.fedict.commons.eid.client.BeIDFileType;
import be.fedict.commons.eid.client.event.BeIDCardEventsListener;
import be.fedict.commons.eid.consumer.BeIDIntegrity;
import be.fedict.commons.eid.consumer.Identity;

public class BeIDCardManagerTest {

	private static final Log LOG = LogFactory.getLog(BeIDCardManagerTest.class);

	@Test
	public void testReadFiles() throws Exception {
		TestLogger logger = new TestLogger();
		BeIDCardManager beIDCardManager = new BeIDCardManager(logger);
		BeIDCard beIDCard = beIDCardManager.getFirstBeIDCard();
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

		byte[] photo = beIDCard.readFile(BeIDFileType.Photo);

		beIDCard.close();

		BeIDIntegrity beIDIntegrity = new BeIDIntegrity();
		Identity identity = beIDIntegrity.getVerifiedIdentity(identityFile,
				identitySignatureFile, photo, rrnCertificate);

		assertNotNull(identity);
		assertNotNull(identity.getNationalNumber());
	}

	@Test
	public void testListenerModification() throws Exception {
		TestLogger logger = new TestLogger();
		BeIDCardManager beIDCardManager = new BeIDCardManager(logger);
		Object waitObject = new Object();
		beIDCardManager
				.addBeIDCardEventListener(new BeIDCardEventsTestListener(
						beIDCardManager, waitObject, true, false));
		beIDCardManager
				.addBeIDCardEventListener(new BeIDCardEventsTestListener(
						beIDCardManager, waitObject, false, false));
		beIDCardManager
				.addBeIDCardEventListener(new BeIDCardEventsTestListener(
						beIDCardManager, waitObject, false, true));
		beIDCardManager.start();
		synchronized (waitObject) {
			waitObject.wait();
		}
	}

	@Test
	public void testExceptionsInListener() throws Exception {
		TestLogger logger = new TestLogger();
		BeIDCardManager beIDCardManager = new BeIDCardManager(logger);
		Object waitObject = new Object();
		beIDCardManager
				.addBeIDCardEventListener(new BeIDCardEventsTestListener(
						beIDCardManager, waitObject, true, false));
		beIDCardManager
				.addBeIDCardEventListener(new BeIDCardEventsTestListener(
						beIDCardManager, waitObject, false, false));
		beIDCardManager
				.addBeIDCardEventListener(new BeIDCardEventsTestListener(
						beIDCardManager, waitObject, false, true));
		beIDCardManager.start();
		synchronized (waitObject) {
			waitObject.wait();
		}
	}

	private final class BeIDCardEventsTestListener implements
			BeIDCardEventsListener {

		private final Object waitObject;

		private final BeIDCardManager manager;

		private final boolean removeAfterCardInserted;
		private final boolean throwNPE;

		public BeIDCardEventsTestListener(BeIDCardManager manager,
				Object waitObject, boolean removeAfterCardInserted,
				boolean throwNPE) {
			this.manager = manager;
			this.waitObject = waitObject;
			this.removeAfterCardInserted = removeAfterCardInserted;
			this.throwNPE = throwNPE;
		}

		@Override
		public void eIDCardRemoved(CardTerminal cardTerminal, BeIDCard card) {
			LOG.debug("eID card removed");

			synchronized (this.waitObject) {
				this.waitObject.notify();
			}

			if (this.throwNPE) {
				throw new NullPointerException(
						"Fake NPE attempting to trash a BeIDCardEventsListener");
			}
		}

		@Override
		public void eIDCardInserted(CardTerminal cardTerminal, BeIDCard card) {
			LOG.debug("eID card added");
			if (this.removeAfterCardInserted) {
				this.manager.removeBeIDCardListener(this);
			}

			if (this.throwNPE) {
				throw new NullPointerException(
						"Fake NPE attempting to trash a BeIDCardEventsListener");
			}
		}
	}
}
