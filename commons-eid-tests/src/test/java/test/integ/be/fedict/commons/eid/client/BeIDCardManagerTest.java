/*
 * Commons eID Project.
 * Copyright (C) 2008-2013 FedICT.
 * Copyright (C) 2017-2020 e-Contract.be BVBA.
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

import java.util.Locale;

import javax.smartcardio.CardTerminal;

import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import be.fedict.commons.eid.client.BeIDCard;
import be.fedict.commons.eid.client.BeIDCardManager;
import be.fedict.commons.eid.client.event.BeIDCardEventsListener;

public class BeIDCardManagerTest {

	private static final Logger LOGGER = LoggerFactory.getLogger(BeIDCardManagerTest.class);

	@Test
	public void testListenerModification() throws Exception {
		final TestLogger logger = new TestLogger();
		final BeIDCardManager beIDCardManager = new BeIDCardManager(logger);
		beIDCardManager.setLocale(Locale.FRENCH);
		final Object waitObject = new Object();
		beIDCardManager
				.addBeIDCardEventListener(new BeIDCardEventsTestListener(beIDCardManager, waitObject, true, false));
		beIDCardManager
				.addBeIDCardEventListener(new BeIDCardEventsTestListener(beIDCardManager, waitObject, false, false));
		beIDCardManager
				.addBeIDCardEventListener(new BeIDCardEventsTestListener(beIDCardManager, waitObject, false, true));
		beIDCardManager.start();
		synchronized (waitObject) {
			waitObject.wait();
		}
	}

	@Test
	public void testExceptionsInListener() throws Exception {
		final TestLogger logger = new TestLogger();
		final BeIDCardManager beIDCardManager = new BeIDCardManager(logger);
		beIDCardManager.setLocale(Locale.GERMAN);
		final Object waitObject = new Object();
		beIDCardManager
				.addBeIDCardEventListener(new BeIDCardEventsTestListener(beIDCardManager, waitObject, true, false));
		beIDCardManager
				.addBeIDCardEventListener(new BeIDCardEventsTestListener(beIDCardManager, waitObject, false, false));
		beIDCardManager
				.addBeIDCardEventListener(new BeIDCardEventsTestListener(beIDCardManager, waitObject, false, true));
		beIDCardManager.start();
		synchronized (waitObject) {
			waitObject.wait();
		}
	}

	@Test
	public void testRefreshCards() throws Exception {
		final TestLogger logger = new TestLogger();
		final BeIDCardManager beIDCardManager = new BeIDCardManager(logger);
		beIDCardManager.setLocale(Locale.ENGLISH);

		beIDCardManager.start();
		beIDCardManager.refreshCards();
	}

	private final class BeIDCardEventsTestListener implements BeIDCardEventsListener {

		private final Object waitObject;

		private final BeIDCardManager manager;

		private final boolean removeAfterCardInserted;
		private final boolean throwNPE;

		public BeIDCardEventsTestListener(final BeIDCardManager manager, final Object waitObject,
				final boolean removeAfterCardInserted, final boolean throwNPE) {
			this.manager = manager;
			this.waitObject = waitObject;
			this.removeAfterCardInserted = removeAfterCardInserted;
			this.throwNPE = throwNPE;
		}

		@Override
		public void eIDCardRemoved(final CardTerminal cardTerminal, final BeIDCard card) {
			LOGGER.debug("eID card removed");

			synchronized (this.waitObject) {
				this.waitObject.notify();
			}

			if (this.throwNPE) {
				throw new NullPointerException("Fake NPE attempting to trash a BeIDCardEventsListener");
			}
		}

		@Override
		public void eIDCardInserted(final CardTerminal cardTerminal, final BeIDCard card) {
			LOGGER.debug("eID card added");
			LOGGER.debug("locale: {}", card.getLocale());
			if (this.removeAfterCardInserted) {
				this.manager.removeBeIDCardListener(this);
			}

			if (this.throwNPE) {
				throw new NullPointerException("Fake NPE attempting to trash a BeIDCardEventsListener");
			}
		}

		@Override
		public void eIDCardEventsInitialized() {
			System.out.println("BeID Card Events Initialised");

		}
	}
}
