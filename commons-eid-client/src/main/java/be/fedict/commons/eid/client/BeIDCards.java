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

package be.fedict.commons.eid.client;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import javax.smartcardio.CardTerminal;
import be.fedict.commons.eid.client.event.BeIDCardEventsListener;
import be.fedict.commons.eid.client.impl.VoidLogger;
import be.fedict.commons.eid.client.spi.BeIDCardsUI;
import be.fedict.commons.eid.client.spi.Logger;
import be.fedict.commons.eid.client.spi.Sleeper;

public class BeIDCards {
	private static final String UI_MISSING_LOG_MESSAGE = "No BeIDCardsUI set and can't load DefaultBeIDCardsUI";
	private static final String DEFAULT_UI_IMPLEMENTATION = "be.fedict.eid.commons.dialogs.DefaultBeIDCardsUI";

	private final Logger logger;
	private BeIDCardManager cardManager;
	private boolean initialized;
	private Map<CardTerminal, BeIDCard> beIDTerminalsAndCards;
	private Sleeper initSleeper, beIDSleeper;
	private BeIDCardsUI ui;

	/*
	 * a BeIDCards with a default (void) logger using the default UI
	 */
	public BeIDCards() {
		this(new VoidLogger());
	}

	/*
	 * a BeIDCards logging to logger, using default UI
	 */
	public BeIDCards(Logger logger) {
		this(logger, null);
	}

	/*
	 * a BeIDCards logging to logger, caller supplies a BeIDCardManager. note:
	 * caller is responsible for start()ing the supplied BeIDCardManager
	 */
	public BeIDCards(Logger logger, BeIDCardsUI ui) {
		this.logger = logger;
		this.ui = ui;
		this.cardManager = new BeIDCardManager();
		this.initSleeper = new Sleeper();
		this.beIDSleeper = new Sleeper();
		this.beIDTerminalsAndCards = new HashMap<CardTerminal, BeIDCard>();
		this.initialized = false;

		this.cardManager.addBeIDCardEventListener(new BeIDCardEventsListener() {
			@Override
			public void eIDCardInserted(CardTerminal cardTerminal, BeIDCard card) {
				BeIDCards.this.logger.debug("eID Card Insertion Reported");
				synchronized (BeIDCards.this.beIDTerminalsAndCards) {
					BeIDCards.this.beIDTerminalsAndCards
							.put(cardTerminal, card);
					beIDSleeper.awaken();
				}
			}

			@Override
			public void eIDCardRemoved(CardTerminal cardTerminal, BeIDCard card) {
				BeIDCards.this.logger.debug("eID Card Removal Reported");
				synchronized (BeIDCards.this.beIDTerminalsAndCards) {
					BeIDCards.this.beIDTerminalsAndCards.remove(cardTerminal);
					beIDSleeper.awaken();
				}
			}

			@Override
			public void eIDCardEventsInitialized() {
				BeIDCards.this.initialized = true;
				initSleeper.awaken();
			}
		});

		this.cardManager.start();
	}

	/*
	 * return Set of all BeID Cards present. Will return empty Set if no BeID
	 * cards are present at time of call
	 */
	public Set<BeIDCard> getAllBeIDCards() {
		waitUntilInitialized();

		synchronized (this.beIDTerminalsAndCards) {
			return new HashSet<BeIDCard>(this.beIDTerminalsAndCards.values());
		}
	}

	/*
	 * return Set of all BeID Cards present. Will block if no BeID Cards are
	 * present at time of call Note that if multiple cards are present, this
	 * will return them in arbitrary order
	 */
	public Set<BeIDCard> getAtLeastOneBeIDCard() {
		waitForAtLeastOneBeIDCard();

		synchronized (this.beIDTerminalsAndCards) {
			return new HashSet<BeIDCard>(this.beIDTerminalsAndCards.values());
		}
	}

	/*
	 * return exactly one BeID Card.
	 * 
	 * This will block until at least one BeID card is inserted, at which point
	 * this will be returned. If, at time of call, more than one BeID card is
	 * present, will request the UI to select between those, and return the
	 * selected card.
	 */
	public BeIDCard getOneBeIDCard() {
		waitForAtLeastOneBeIDCard();

		synchronized (this.beIDTerminalsAndCards) {
			if (this.beIDTerminalsAndCards.size() == 1) {
				return this.beIDTerminalsAndCards.values().iterator().next();
			} else {
				return getUI().selectBeIDCard(
						this.beIDTerminalsAndCards.values());
			}
		}
	}

	/*
	 * wait for a particular BeID card to be removed
	 * Note that this only works with BeID objects that were acquired using 
	 * one of the getXXX methods from the same BeIDCards instance
	 */
	public BeIDCards waitUntilCardRemoved(BeIDCard card) {
		while (getAllBeIDCards().contains(card)) {
			beIDSleeper.sleepUntilAwakened();
		}

		return this;
	}

	/*
	 * call close() if you no longer need this BeIDCards instance. this stops a
	 * private cardManager, if present
	 */
	public BeIDCards close() throws InterruptedException {
		this.cardManager.stop();
		return this;
	}

	// ----------------------------------------------------------------------------------------------------------------------------------

	private BeIDCardsUI getUI() {
		if (this.ui == null) {
			try {
				ClassLoader classLoader = BeIDCard.class.getClassLoader();
				Class<?> uiClass = classLoader
						.loadClass(DEFAULT_UI_IMPLEMENTATION);
				this.ui = (BeIDCardsUI) uiClass.newInstance();
			} catch (Exception e) {
				logger.error(UI_MISSING_LOG_MESSAGE);
				throw new UnsupportedOperationException(UI_MISSING_LOG_MESSAGE,
						e);
			}
		}

		return this.ui;
	}

	public void setUI(BeIDCardsUI ui) {
		this.ui = ui;
	}

	public boolean hasBeIDCards() {
		waitUntilInitialized();
		boolean has;
		synchronized (this.beIDTerminalsAndCards) {
			has = (!beIDTerminalsAndCards.isEmpty());
		}
		return has;
	}

	/*
	 * Private, supporting methods
	 * **********************************************
	 */

	private void waitUntilInitialized() {
		while (!this.initialized)
			initSleeper.sleepUntilAwakened();
	}

	private void waitForAtLeastOneBeIDCard() {
		if (!hasBeIDCards()) {
			try {
				getUI().adviseBeIDCardRequired();
				while (!hasBeIDCards()) {
					beIDSleeper.sleepUntilAwakened();
				}
			} finally {
				getUI().adviseEnd();
			}
		}
	}
}
