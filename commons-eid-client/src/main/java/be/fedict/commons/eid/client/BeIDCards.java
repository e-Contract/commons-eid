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
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import javax.smartcardio.CardTerminal;

import be.fedict.commons.eid.client.CardAndTerminalManager.PROTOCOL;
import be.fedict.commons.eid.client.event.BeIDCardEventsListener;
import be.fedict.commons.eid.client.event.CardTerminalEventsListener;
import be.fedict.commons.eid.client.impl.VoidLogger;
import be.fedict.commons.eid.client.spi.BeIDCardsUI;
import be.fedict.commons.eid.client.spi.Logger;
import be.fedict.commons.eid.client.spi.Sleeper;

/**
 * BeIDCards is a synchronous approach to Belgian Identity Cards and their
 * presence in the user's system, as opposed to the asynchronous, event-driven
 * approach of {@link BeIDCardManager} (but BeIDCards uses an underlying
 * {@link BeIDCardManager} to achieve it's goals). It's main purpose is to have
 * a very simple way to get a user's BeIDCard instance, abstracting away and
 * delegating issues such as terminal connection, card insertion, and handling
 * multiple eligible cards.
 * <p>
 * BeIDCards handle user interaction (if any) through an instance of
 * BeIDCardsUI, which can be supplied at construction, or left to the supplied
 * default, which will instantiate a
 * be.fedict.eid.commons.dialogs.DefaultBeIDCardsUI (which needs to be available
 * on the class path)
 * 
 * @author Frank Marien
 * 
 */

public class BeIDCards {
	private static final String UI_MISSING_LOG_MESSAGE = "No BeIDCardsUI set and can't load DefaultBeIDCardsUI";
	private static final String DEFAULT_UI_IMPLEMENTATION = "be.fedict.eid.commons.dialogs.DefaultBeIDCardsUI";

	private final Logger logger;
	private CardAndTerminalManager cardAndTerminalManager;
	private BeIDCardManager cardManager;
	private boolean terminalsInitialized, cardsInitialized, uiSelectingCard;
	private Map<CardTerminal, BeIDCard> beIDTerminalsAndCards;
	private Sleeper terminalManagerInitSleeper, cardTerminalSleeper;
	private Sleeper cardManagerInitSleeper, beIDSleeper;
	private BeIDCardsUI ui;
	private int cardTerminalsAttached;
	private Locale locale;

	/**
	 * a BeIDCards without logging, using the default BeIDCardsUI and locale
	 */
	public BeIDCards() {
		this(new VoidLogger(), null, Locale.getDefault());
	}

	/**
	 * a BeIDCards without logging, using the default BeIDCardsUI and supplied locale
	 * 
	 * @param locale
	 *           the locale that will be used in any UI interaction, and set
	 *           on all BeIDCard instances returned.
	 */
	public BeIDCards(Locale locale) {
		this(new VoidLogger(), null, locale);
	}

	/**
	 * a BeIDCards without logging, using the supplied BeIDCardsUI and default locale
	 * 
	 * @param ui
	 *            an instance of be.fedict.commons.eid.client.spi.BeIDCardsUI
	 *            that will be called upon for any user interaction required to
	 *            handle other calls
	 */
	public BeIDCards(final BeIDCardsUI ui) {
		this(new VoidLogger(), ui, Locale.getDefault());
	}

	/**
	 * a BeIDCards without logging, using the supplied BeIDCardsUI and locale
	 *
	 * @param ui
	 *            an instance of be.fedict.commons.eid.client.spi.BeIDCardsUI
	 *            that will be called upon for any user interaction required to
	 *            handle other calls
	 * @param locale
	 *           the locale that will be used in any UI interaction, and set
	 *           on all BeIDCard instances returned.
	 */
	public BeIDCards(final BeIDCardsUI ui, final Locale locale) {
		this(new VoidLogger(), ui, locale);
	}

	/**
	 * a BeIDCards logging to logger, using the default BeIDCardsUI and locale
	 * 
	 * @param logger
	 *            an instance of be.fedict.commons.eid.spi.Logger that will be
	 *            send all the logs
	 */
	public BeIDCards(final Logger logger) {
		this(logger, null, Locale.getDefault());
	}

	/**
	 * a BeIDCards logging to logger, using the supplied locale
	 * 
	 * @param logger
	 *            an instance of be.fedict.commons.eid.spi.Logger that will be
	 *            send all the logs
	 * @param locale
	 *           the locale that will be used in any UI interaction, and set
	 *           on all BeIDCard instances returned.
	 */
	public BeIDCards(final Logger logger, final Locale locale) {
		this(logger, null, locale);
	}

	/**
	 * a BeIDCards logging to logger, using the supplied BeIDCardsUI and the default locale
	 * 
	 * @param logger
	 *            an instance of be.fedict.commons.eid.spi.Logger that will be
	 *            send all the logs
	 * @param ui
	 *            an instance of be.fedict.commons.eid.client.spi.BeIDCardsUI
	 *            that will be called upon for any user interaction required to
	 *            handle other calls
	 */
	public BeIDCards(final Logger logger, final BeIDCardsUI ui) {
		this(logger, ui, Locale.getDefault());
	}

	/**
	 * a BeIDCards logging to logger, using the supplied BeIDCardsUI and locale
	 * 
	 * @param logger
	 *            an instance of be.fedict.commons.eid.spi.Logger that will be
	 *            send all the logs
	 * @param ui
	 *            an instance of be.fedict.commons.eid.client.spi.BeIDCardsUI
	 *            that will be called upon for any user interaction required to
	 *            handle other calls
	 * @param locale
	 *           the locale that will be used in any UI interaction, and set
	 *           on all BeIDCard instances returned.
	 */
	public BeIDCards(final Logger logger, final BeIDCardsUI ui,
			final Locale locale) {
		this.logger = logger;
		this.ui = ui;
		this.locale = locale;
		this.cardAndTerminalManager = new CardAndTerminalManager(logger);
		this.cardAndTerminalManager.setProtocol(PROTOCOL.T0);
		this.cardManager = new BeIDCardManager(logger,
				this.cardAndTerminalManager);
		this.terminalManagerInitSleeper = new Sleeper();
		this.cardManagerInitSleeper = new Sleeper();
		this.cardTerminalSleeper = new Sleeper();
		this.beIDSleeper = new Sleeper();
		this.beIDTerminalsAndCards = new HashMap<CardTerminal, BeIDCard>();
		this.terminalsInitialized = false;
		this.cardsInitialized = false;
		this.uiSelectingCard = false;

		this.cardAndTerminalManager
				.addCardTerminalListener(new CardTerminalEventsListener() {

					@Override
					public void terminalEventsInitialized() {
						BeIDCards.this.terminalsInitialized = true;
						BeIDCards.this.terminalManagerInitSleeper.awaken();
					}

					@Override
					public void terminalDetached(CardTerminal cardTerminal) {
						BeIDCards.this.cardTerminalsAttached--;
						BeIDCards.this.cardTerminalSleeper.awaken();

					}

					@Override
					public void terminalAttached(CardTerminal cardTerminal) {
						BeIDCards.this.cardTerminalsAttached++;
						BeIDCards.this.cardTerminalSleeper.awaken();
					}
				});

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
				BeIDCards.this.logger.debug("eIDCardEventsInitialized");
				BeIDCards.this.cardsInitialized = true;
				BeIDCards.this.cardManagerInitSleeper.awaken();
			}
		});

		if (this.locale != null) {
			this.cardManager.setLocale(this.locale);
		}

		this.cardAndTerminalManager.start();
	}

	/**
	 * set the Locale of all BeIDCard instances returned by all subsequent
	 * getXXX calls. This is the equivalent of calling setLocale() on each
	 * BeIDCard instance returned.
	 * 
	 * @param newLocale
	 * @return this BeIDCards instance, to allow method chaining
	 */
	public final BeIDCards setLocale(final Locale newLocale) {
		this.locale = newLocale;
		this.cardManager.setLocale(newLocale);
		if (this.ui != null) {
			this.ui.setLocale(newLocale);
		}
		return this;
	}

	/**
	 * Return whether any BeID Cards are currently present.
	 * 
	 * @return true if one or more BeID Cards are inserted in one or more
	 *         connected CardTerminals, false if zero BeID Cards are present
	 */
	public boolean hasBeIDCards() {
		return this.hasBeIDCards(null);
	}

	/**
	 * Return whether any BeID Cards are currently present.
	 * 
	 * @param terminal
	 *            if not null, only this terminal will be considered in
	 *            determining whether beID Cards are present.
	 * 
	 * @return true if one or more BeID Cards are inserted in one or more
	 *         connected CardTerminals, false if zero BeID Cards are present
	 */
	public boolean hasBeIDCards(CardTerminal terminal) {
		waitUntilCardsInitialized();
		boolean has;

		synchronized (this.beIDTerminalsAndCards) {
			if (terminal != null) {
				has = this.beIDTerminalsAndCards.containsKey(terminal);
			} else {
				has = !this.beIDTerminalsAndCards.isEmpty();
			}
		}
		this.logger.debug("hasBeIDCards returns " + has);
		return has;
	}

	/**
	 * return Set of all BeID Cards present. Will return empty Set if no BeID
	 * cards are present at time of call
	 * 
	 * @return a (possibly empty) set of all BeID Cards inserted at time of call
	 */
	public Set<BeIDCard> getAllBeIDCards() {
		waitUntilCardsInitialized();

		synchronized (this.beIDTerminalsAndCards) {
			return new HashSet<BeIDCard>(this.beIDTerminalsAndCards.values());
		}
	}

	/**
	 * return exactly one BeID Card.
	 * 
	 * This may block when called when no BeID Cards are present, until at least
	 * one BeID card is inserted, at which point this will be returned. If, at
	 * time of call, more than one BeID card is present, will request the UI to
	 * select between those, and return the selected card. If the UI is called
	 * upon to request the user to select between different cards, or to insert
	 * one card, and the user declines, CancelledException is thrown.
	 * 
	 * @return a BeIDCard instance. The only one present, or one chosen out of
	 *         several by the user
	 * @throws CancelledException
	 */
	public BeIDCard getOneBeIDCard() throws CancelledException {
		return this.getOneBeIDCard(null);
	}

	/**
	 * return a BeID Card inserted into a given CardTerminal
	 * 
	 * @param terminal
	 *            if not null, only BeID Cards in this particular CardTerminal
	 *            will be considered.
	 * 
	 *            May block when called when no BeID Cards are present, until at
	 *            least one BeID card is inserted, at which point this will be
	 *            returned. If, at time of call, more than one BeID card is
	 *            present, will request the UI to select between those, and
	 *            return the selected card. If the UI is called upon to request
	 *            the user to select between different cards, or to insert one
	 *            card, and the user declines, CancelledException is thrown.
	 * 
	 * @return a BeIDCard instance. The only one present, or one chosen out of
	 *         several by the user
	 * @throws CancelledException
	 */
	public BeIDCard getOneBeIDCard(CardTerminal terminal)
			throws CancelledException {
		BeIDCard selectedCard = null;

		do {
			waitForAtLeastOneCardTerminal();
			waitForAtLeastOneBeIDCard(terminal);

			// copy current list of BeID Cards to avoid holding a lock on it
			// during possible selectBeIDCard dialog.
			// (because we'd deadlock when user inserts/removes a card while
			// selectBeIDCard has not returned)

			Map<CardTerminal, BeIDCard> currentBeIDCards = null;
			synchronized (this.beIDTerminalsAndCards) {
				currentBeIDCards = new HashMap<CardTerminal, BeIDCard>(
						this.beIDTerminalsAndCards);
			}

			if (terminal != null) {
				// if selecting by terminal and we have a card in the requested
				// one,
				// return that immediately. (this will return null if the
				// terminal we want doesn't
				// have a card, and continue the loop.
				selectedCard = currentBeIDCards.get(terminal);
			} else if (currentBeIDCards.size() == 1) {

				// we have only one BeID card. return it.
				selectedCard = currentBeIDCards.values().iterator().next();
			} else {
				// more than one, call upon the UI to obtain a selection
				try {
					this.logger.debug("selecting");
					this.uiSelectingCard = true;
					selectedCard = getUI().selectBeIDCard(
							currentBeIDCards.values());
				} catch (final OutOfCardsException oocex) {
					// if we run out of cards, waitForAtLeastOneBeIDCard will
					// ask for one in the next loop
				} finally {
					this.uiSelectingCard = false;
					this.logger.debug("no longer selecting");
				}
			}
		} while (selectedCard == null);

		return selectedCard;
	}

	/**
	 * wait for a particular BeID card to be removed. Note that this only works
	 * with BeID objects that were acquired using either the
	 * {@link #getOneBeIDCard()} or {@link #getAllBeIDCards()} methods from the
	 * same BeIDCards instance. If, at time of call, that particular card is
	 * present, the UI is called upon to prompt the user to remove that card.
	 * 
	 * @return this BeIDCards instance to allow for method chaining
	 */
	public BeIDCards waitUntilCardRemoved(final BeIDCard card) {
		if (this.getAllBeIDCards().contains(card)) {
			try {
				this.logger
						.debug("waitUntilCardRemoved blocking until card removed");
				this.getUI().adviseBeIDCardRemovalRequired();
				while (this.getAllBeIDCards().contains(card)) {
					this.beIDSleeper.sleepUntilAwakened();
				}
			} finally {
				this.getUI().adviseEnd();
			}
		}
		this.logger.debug("waitUntilCardRemoved returning");
		return this;
	}

	public boolean hasCardTerminals() {
		waitUntilTerminalsInitialized();
		return this.cardTerminalsAttached > 0;
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
				if (this.locale != null) {
					this.ui.setLocale(this.locale);
				}
			} catch (final Exception e) {
				this.logger.error(UI_MISSING_LOG_MESSAGE);
				throw new UnsupportedOperationException(UI_MISSING_LOG_MESSAGE,
						e);
			}
		}

		return this.ui;
	}

	private void waitUntilCardsInitialized() {
		while (!this.cardsInitialized) {
			this.logger
					.debug("Waiting for CardAndTerminalManager Cards initialisation");
			this.cardManagerInitSleeper.sleepUntilAwakened();
			this.logger
					.debug("CardAndTerminalManager now has cards initialized");
		}
	}

	private void waitUntilTerminalsInitialized() {
		while (!this.terminalsInitialized) {
			this.logger
					.debug("Waiting for CardAndTerminalManager Terminals initialisation");
			this.terminalManagerInitSleeper.sleepUntilAwakened();
			this.logger
					.debug("CardAndTerminalManager now has terminals initialized");
		}
	}

	private void waitForAtLeastOneBeIDCard(CardTerminal terminal) {
		if (!this.hasBeIDCards(terminal)) {
			try {
				this.getUI().adviseBeIDCardRequired();
				while (!this.hasBeIDCards(terminal)) {
					this.beIDSleeper.sleepUntilAwakened();
				}
			} finally {
				this.getUI().adviseEnd();
			}
		}
	}

	private void waitForAtLeastOneCardTerminal() {
		if (!this.hasCardTerminals()) {
			try {
				this.getUI().adviseCardTerminalRequired();
				while (!this.hasCardTerminals()) {
					this.cardTerminalSleeper.sleepUntilAwakened();
				}
			} finally {
				this.getUI().adviseEnd();
			}

			// if we just found our first CardTerminal, give us 100ms
			// to get notified about any eID cards that may already present in
			// that CardTerminal
			// we'll get notified about any cards much faster than 100ms,
			// and worst case, 100ms is not noticeable. Better than calling
			// adviseBeIDCardRequired and adviseEnd
			// with a few seconds in between.
			if (!this.hasBeIDCards()) {
				try {
					Thread.sleep(100);
				} catch (InterruptedException e) {
					// intentionally empty
				}
			}
		}
	}
}
