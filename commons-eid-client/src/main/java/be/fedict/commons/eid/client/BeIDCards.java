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

import java.util.ArrayList;
import java.util.Collection;
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

/**
 * BeIDCards is a synchronous approach to Belgian Identity Cards and their
 * presence in the user's system, as opposed to the asynchronous, event-driven
 * approach of {@link BeIDCardManager} (but BeIDCards uses an underlying {@link BeIDCardManager}
 * to achieve it's goals). It's main purpose is to have a very simple way to get a user's
 * BeIDCard instance, abstracting away and delegating issues such as terminal connection,
 * card insertion, and handling multiple elegible cards.
 * <p>
 * BeIDCards handle user interaction (if any) through an instance of BeIDCardsUI, which can be
 * supplied at construction, or left to the supplied default, which will instantiate a 
 * be.fedict.eid.commons.dialogs.DefaultBeIDCardsUI (which needs to be available on the class path)
 * 
 * @author Frank Marien
 *
 */
public class BeIDCards {
	private static final String UI_MISSING_LOG_MESSAGE = "No BeIDCardsUI set and can't load DefaultBeIDCardsUI";
	private static final String DEFAULT_UI_IMPLEMENTATION = "be.fedict.eid.commons.dialogs.DefaultBeIDCardsUI";

	private final Logger logger;
	private BeIDCardManager cardManager;
	private boolean initialized, uiSelectingCard;
	private Map<CardTerminal, BeIDCard> beIDTerminalsAndCards;
	private Sleeper initSleeper, beIDSleeper;
	private BeIDCardsUI ui;

	/**
	 * Instantiate a BeIDCards with a default (void) logger using the default UI
	 */
	public BeIDCards() {
		this(new VoidLogger());
	}

	/**
	 * Instantiate a BeIDCards logging to logger, using the default UI.
	 * @param logger an instance of be.fedict.commons.eid.spi.Logger that will be send all the logs
	 */
	public BeIDCards(final Logger logger) {
		this(logger, null);
	}

	/**
	 * a BeIDCards logging to logger, using the supplied BeIDCardsUI.
	 * @param logger an instance of be.fedict.commons.eid.spi.Logger that will be send all the logs
	 * @param ui an instance of be.fedict.commons.eid.client.spi.BeIDCardsUI that will be
	 * called upon for any user interaction required to handle other calls
	 */
	public BeIDCards(final Logger logger, final BeIDCardsUI ui) {
		this.logger = logger;
		this.ui = ui;
		this.cardManager = new BeIDCardManager();
		this.initSleeper = new Sleeper();
		this.beIDSleeper = new Sleeper();
		this.beIDTerminalsAndCards = new HashMap<CardTerminal, BeIDCard>();
		this.initialized = false;
		this.uiSelectingCard = false;

		this.cardManager.addBeIDCardEventListener(new BeIDCardEventsListener() {
			@Override
			public void eIDCardInserted(final CardTerminal cardTerminal,
					final BeIDCard card) {
				BeIDCards.this.logger.debug("eID Card Insertion Reported");

				if (BeIDCards.this.uiSelectingCard) {
					try {
						BeIDCards.this.getUI().eIDCardInsertedDuringSelection(
								card);
					} catch (final Exception ex) {
						BeIDCards.this.logger
								.error("Exception in UI:eIDCardInserted"
										+ ex.getMessage());
					}
				}

				synchronized (BeIDCards.this.beIDTerminalsAndCards) {
					BeIDCards.this.beIDTerminalsAndCards
							.put(cardTerminal, card);
					BeIDCards.this.beIDSleeper.awaken();
				}
			}

			@Override
			public void eIDCardRemoved(final CardTerminal cardTerminal,
					final BeIDCard card) {
				BeIDCards.this.logger.debug("eID Card Removal Reported");

				if (BeIDCards.this.uiSelectingCard) {
					try {
						BeIDCards.this.getUI().eIDCardRemovedDuringSelection(
								card);
					} catch (final Exception ex) {
						BeIDCards.this.logger
								.error("Exception in UI:eIDCardRemoved"
										+ ex.getMessage());
					}
				}

				synchronized (BeIDCards.this.beIDTerminalsAndCards) {
					BeIDCards.this.beIDTerminalsAndCards.remove(cardTerminal);
					BeIDCards.this.beIDSleeper.awaken();
				}
			}

			@Override
			public void eIDCardEventsInitialized() {
				BeIDCards.this.initialized = true;
				BeIDCards.this.initSleeper.awaken();
			}
		});

		this.cardManager.start();
	}

	/**
	 * Return whether any BeID Cards are currently present.
	 * @return true if one or more BeID Cards are inserted in
	 * one or more connected CardTerminals, false if zero BeID Cards
	 * are present
	 */
	public boolean hasBeIDCards() {
		waitUntilInitialized();
		boolean has;
		synchronized (this.beIDTerminalsAndCards) {
			has = (!this.beIDTerminalsAndCards.isEmpty());
		}
		return has;
	}

	/**
	 * return Set of all BeID Cards present. Will return empty Set if no BeID
	 * cards are present at time of call
	 * @return a (possibly empty) set of all BeID Cards inserted at time of call
	 */
	public Set<BeIDCard> getAllBeIDCards() {
		waitUntilInitialized();

		synchronized (this.beIDTerminalsAndCards) {
			return new HashSet<BeIDCard>(this.beIDTerminalsAndCards.values());
		}
	}

	/**
	 * return exactly one BeID Card.
	 * 
	 * This may block when called when no BeID Cards are present, 
	 * until at least one BeID card is inserted, at which point
	 * this will be returned. If, at time of call, more than one BeID card is
	 * present, will request the UI to select between those, and return the
	 * selected card. If the UI is called upon to request the user to select between
	 * different cards, or to insert one card, and the user declines, CancelledException
	 * is thrown.
	 * 
	 * @return a BeIDCard instance. The only one present, or one chosen out of several by the user
	 * @throws CancelledException
	 */
	public BeIDCard getOneBeIDCard() throws CancelledException {
		BeIDCard selectedCard = null;

		do {
			waitForAtLeastOneBeIDCard();

			// copy current list of BeID Cards to avoid holding a lock on it during possible selectBeIDCard dialog.
			// (because we'd deadlock when user inserts/removes a card while selectBeIDCard has not returned)
			Collection<BeIDCard> currentBeIDCards = null;
			synchronized (this.beIDTerminalsAndCards) {
				currentBeIDCards = new ArrayList<BeIDCard>(
						this.beIDTerminalsAndCards.values());
			}

			if (currentBeIDCards.size() == 1) {
				// only one BeID card. return it.
				selectedCard = currentBeIDCards.iterator().next();
			} else {
				// more than one, call upon the UI to obtain a selection
				try {
					System.err.println("selecting");
					this.uiSelectingCard = true;
					selectedCard = getUI().selectBeIDCard(currentBeIDCards);
				} catch (final OutOfCardsException oocex) {
					// if we run out of cards, waitForAtLeastOneBeIDCard will ask for one in the next loop
				} finally {
					this.uiSelectingCard = false;
					System.err.println("no longer selecting");
				}
			}
		} while (selectedCard == null);

		return selectedCard;
	}

	/**
	 * wait for a particular BeID card to be removed. Note that this only works
	 * with BeID objects that were acquired using either the {@link getOneBeIDCard()} or
	 * {@link getAllBeIDCards()} methods from the same BeIDCards instance
	 * @return this BeIDCards instance to allow for method chaining
	 */
	public BeIDCards waitUntilCardRemoved(final BeIDCard card) {
		while (this.getAllBeIDCards().contains(card)) {
			this.beIDSleeper.sleepUntilAwakened();
		}

		return this;
	}

	/**
	 * call close() if you no longer need this BeIDCards instance.
	 */
	public BeIDCards close() throws InterruptedException {
		this.cardManager.stop();
		return this;
	}

	/*
	 * Private, supporting methods
	 * **********************************************
	 */

	private BeIDCardsUI getUI() {
		if (this.ui == null) {
			try {
				final ClassLoader classLoader = BeIDCard.class.getClassLoader();
				final Class<?> uiClass = classLoader
						.loadClass(DEFAULT_UI_IMPLEMENTATION);
				this.ui = (BeIDCardsUI) uiClass.newInstance();
			} catch (final Exception e) {
				this.logger.error(UI_MISSING_LOG_MESSAGE);
				throw new UnsupportedOperationException(UI_MISSING_LOG_MESSAGE,
						e);
			}
		}

		return this.ui;
	}

	private void waitUntilInitialized() {
		while (!this.initialized) {
			this.initSleeper.sleepUntilAwakened();
		}
	}

	private void waitForAtLeastOneBeIDCard() {
		if (!this.hasBeIDCards()) {
			try {
				this.getUI().adviseBeIDCardRequired();
				while (!this.hasBeIDCards()) {
					this.beIDSleeper.sleepUntilAwakened();
				}
			} finally {
				this.getUI().adviseEnd();
			}
		}
	}
}
