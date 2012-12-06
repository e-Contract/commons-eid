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

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import javax.smartcardio.ATR;
import javax.smartcardio.Card;
import javax.smartcardio.CardTerminal;

import be.fedict.commons.eid.client.CardAndTerminalManager.PROTOCOL;
import be.fedict.commons.eid.client.event.BeIDCardEventsListener;
import be.fedict.commons.eid.client.event.CardEventsListener;
import be.fedict.commons.eid.client.impl.VoidLogger;
import be.fedict.commons.eid.client.spi.Logger;

/**
 * A BeIDCardManager uses a {@link CardAndTerminalManager} to detect Card Insertion
 * and Removal Events, distinguishes between Belgian eID and other cards,
 * calls any registered BeIDCardEventsListeners for eID cards inserted and removed,
 * and any registered CardEventsListener for other cards being inserted and removed.
 * Note that by default, a BeIDCardManager will only connect to cards using card protocol
 * "T=0" to ensure optimal compatibility with Belgian eID cards in all card readers, meaning that
 * if you wish to use its "other card" facility you may have to supply your own CardAndTerminalManager
 * with a protocol setting of "ALL".
 * @author Frank Marien
 * @author Frank Cornelis
 */

public class BeIDCardManager {
	private CardAndTerminalManager cardAndTerminalManager;
	private boolean terminalManagerIsPrivate;
	private Map<CardTerminal, BeIDCard> terminalsAndCards;
	private Set<BeIDCardEventsListener> beIdListeners;
	private Set<CardEventsListener> otherCardListeners;
	private final Logger logger;

	/**
	 * Instantiate a BeIDCardManager with a default (void) logger and a private
	 * CardAndTerminalManager that is automatically started and stopped with the
	 * BeIDCardManager, and that only connects to Cards with Protocol T=0
	 */
	public BeIDCardManager() {
		this(new VoidLogger());
	}

	/**
	 * Instantiate BeIDCardManager logging to logger, and a private CardAndTerminalManager that
	 * is automatically started and stopped with the BeIDCardManager,
	 * and that only connects to Cards with Protocol T=0.
	 * @param logger an instance of be.fedict.commons.eid.spi.Logger that will be send all the logs
	 */
	public BeIDCardManager(final Logger logger) {
		this(logger, new CardAndTerminalManager());
		this.terminalManagerIsPrivate = true;
	}

	/**
	 * Instantiate a BeIDCardManager with a default (void) logger, caller supplies a
	 * CardAndTerminalManager. note: caller is responsible for start()in the
	 * supplied CardAndTerminalManager, it will not be automatically started.
	 * The supplied CardAndTerminalManager should allow protocol T0 ("T=0")
	 * or ANY ("*") for BeIDCards to work.
	 * @param cardAndTerminalManager the CardAndTerminalManager to use
	 */
	public BeIDCardManager(final CardAndTerminalManager cardAndTerminalManager) {
		this(new VoidLogger(), cardAndTerminalManager);
	}

	/**
	 * Instantiate a BeIDCardManager logging to logger, caller supplies a
	 * CardAndTerminalManager. note: caller is responsible for start()in the
	 * supplied CardAndTerminalManager, it will not be automatically started!
	 * @param logger an instance of be.fedict.commons.eid.spi.Logger that will be send all the logs
	 * @param cardAndTerminalManager the CardAndTerminalManager to use
	 */
	public BeIDCardManager(final Logger logger,
			final CardAndTerminalManager cardAndTerminalManager) {
		this.logger = logger;
		this.beIdListeners = new HashSet<BeIDCardEventsListener>();
		this.otherCardListeners = new HashSet<CardEventsListener>();
		this.terminalsAndCards = new HashMap<CardTerminal, BeIDCard>();

		this.cardAndTerminalManager = cardAndTerminalManager;
		if (this.terminalManagerIsPrivate) {
			this.cardAndTerminalManager.setProtocol(PROTOCOL.T0);
		}

		this.cardAndTerminalManager.addCardListener(new CardEventsListener() {
			@Override
			public void cardInserted(final CardTerminal cardTerminal,
					final Card card) {
				if (card != null && matchesEidAtr(card.getATR())) {
					final BeIDCard beIDCard = new BeIDCard(card,
							BeIDCardManager.this.logger);

					synchronized (BeIDCardManager.this.terminalsAndCards) {
						BeIDCardManager.this.terminalsAndCards.put(
								cardTerminal, beIDCard);
					}

					Set<BeIDCardEventsListener> copyOfListeners = null;

					synchronized (BeIDCardManager.this.beIdListeners) {
						copyOfListeners = new HashSet<BeIDCardEventsListener>(
								BeIDCardManager.this.beIdListeners);
					}

					for (BeIDCardEventsListener listener : copyOfListeners) {
						try {
							listener.eIDCardInserted(cardTerminal, beIDCard);
						} catch (final Throwable thrownInListener) {
							BeIDCardManager.this.logger
									.error("Exception thrown in BeIDCardEventsListener.eIDCardInserted:"
											+ thrownInListener.getMessage());
						}
					}
				} else {
					Set<CardEventsListener> copyOfListeners = null;

					synchronized (BeIDCardManager.this.otherCardListeners) {
						copyOfListeners = new HashSet<CardEventsListener>(
								BeIDCardManager.this.otherCardListeners);
					}

					for (CardEventsListener listener : copyOfListeners) {
						try {
							listener.cardInserted(cardTerminal, card);
						} catch (final Throwable thrownInListener) {
							BeIDCardManager.this.logger
									.error("Exception thrown in CardEventsListener.cardInserted:"
											+ thrownInListener.getMessage());
						}
					}
				}
			}

			@Override
			public void cardRemoved(final CardTerminal cardTerminal) {
				final BeIDCard beIDCard = BeIDCardManager.this.terminalsAndCards
						.get(cardTerminal);
				if (beIDCard != null) {
					beIDCard.close();
					synchronized (BeIDCardManager.this.terminalsAndCards) {
						BeIDCardManager.this.terminalsAndCards
								.remove(cardTerminal);
					}

					Set<BeIDCardEventsListener> copyOfListeners = null;

					synchronized (BeIDCardManager.this.beIdListeners) {
						copyOfListeners = new HashSet<BeIDCardEventsListener>(
								BeIDCardManager.this.beIdListeners);
					}

					for (BeIDCardEventsListener listener : copyOfListeners) {
						try {
							listener.eIDCardRemoved(cardTerminal, beIDCard);
						} catch (final Throwable thrownInListener) {
							BeIDCardManager.this.logger
									.error("Exception thrown in BeIDCardEventsListener.eIDCardRemoved:"
											+ thrownInListener.getMessage());
						}

					}
				} else {
					Set<CardEventsListener> copyOfListeners = null;

					synchronized (BeIDCardManager.this.otherCardListeners) {
						copyOfListeners = new HashSet<CardEventsListener>(
								BeIDCardManager.this.otherCardListeners);
					}

					for (CardEventsListener listener : copyOfListeners) {
						try {
							listener.cardRemoved(cardTerminal);
						} catch (final Throwable thrownInListener) {
							BeIDCardManager.this.logger
									.error("Exception thrown in CardEventsListener.cardRemoved:"
											+ thrownInListener.getMessage());
						}
					}
				}
			}

			@Override
			public void cardEventsInitialized() {
				Set<BeIDCardEventsListener> copyOfBeIDCardEventsListeners = null;

				synchronized (BeIDCardManager.this.beIdListeners) {
					copyOfBeIDCardEventsListeners = new HashSet<BeIDCardEventsListener>(
							BeIDCardManager.this.beIdListeners);
				}

				for (BeIDCardEventsListener listener : copyOfBeIDCardEventsListeners) {
					try {
						listener.eIDCardEventsInitialized();
					} catch (final Throwable thrownInListener) {
						BeIDCardManager.this.logger
								.error("Exception thrown in BeIDCardEventsListener.eIDCardInserted:"
										+ thrownInListener.getMessage());
					}
				}

				Set<CardEventsListener> copyOfOtherCardEventsListeners = null;

				synchronized (BeIDCardManager.this.otherCardListeners) {
					copyOfOtherCardEventsListeners = new HashSet<CardEventsListener>(
							BeIDCardManager.this.otherCardListeners);
				}

				for (CardEventsListener listener : copyOfOtherCardEventsListeners) {
					try {
						listener.cardEventsInitialized();
					} catch (final Throwable thrownInListener) {
						BeIDCardManager.this.logger
								.error("Exception thrown in BeIDCardEventsListener.eIDCardInserted:"
										+ thrownInListener.getMessage());
					}
				}
			}
		});
	}

	/**
	 * Starts this BeIDCardManager. If no CardAndTerminalManager was given at 
	 * construction, this will start our private CardAndTerminalManager.
	 * After this, any registered listeners will start receiving their designated events,
	 * including the existing state. 
	 * If a CardAndTerminalManager was given at construction, this has no effect.
	 * @return this BeIDCardManager to allow for method chaining
	 */
	public BeIDCardManager start() {
		if (this.terminalManagerIsPrivate) {
			this.cardAndTerminalManager.start();
		}
		return this;
	}

	/**
	 * add a BeIDCardEventsListener to be notified of BeID cards being inserted and removed.
	 * @param listener the BeIDCardEventsListener to notify about BeID card insertions and removals
	 * @return this BeIDCardManager to allow for method chaining
	 */
	public BeIDCardManager addBeIDCardEventListener(
			final BeIDCardEventsListener listener) {
		synchronized (this.beIdListeners) {
			this.beIdListeners.add(listener);
		}
		return this;
	}

	/**
	 * remove a BeIDCardEventsListener from being notified of BeID cards being inserted and removed.
	 * @param listener the BeIDCardEventsListener stop notifying about BeID card insertions and removals
	 * @return this BeIDCardManager to allow for method chaining
	 */
	public BeIDCardManager removeBeIDCardListener(
			final BeIDCardEventsListener listener) {
		synchronized (this.beIdListeners) {
			this.beIdListeners.remove(listener);
		}
		return this;
	}

	/**
	 * add a CardEventsListener to be notified of non-BeID cards being inserted and removed.
	 * Note that this is the same interface than in {@link CardAndTerminalManager#addCardListener(CardEventsListener)}
	 * with one notable semantic difference: a BeIDCardManager will call its CardEventsListeners only
	 * for non-eID cards, while a CardAndTerminalManager will call them for all card events: If you
	 * instantiate your own CardAndTerminalManager and supply it to a BeIDCardManager, you will get 2
	 * card insert events if you register your BeIDCardEventsListerer to the BeIDCardManager and your
	 * CardEventsListeners to the CardAndTerminalManager: Register both with the BeIDCardManager to avoid this.
	 * @param listener the CardEventsListener to notify about non-BeID card insertions and removals.
	 * @return this BeIDCardManager to allow for method chaining
	 */
	public BeIDCardManager addOtherCardEventListener(
			final CardEventsListener listener) {
		synchronized (this.otherCardListeners) {
			this.otherCardListeners.add(listener);
		}
		return this;
	}

	/**
	 * remove a CardEventsListener from being notified of non-BeID cards being inserted and removed.
	 * @param listener the CardEventsListener to stop notifying about non-BeID card insertions and removals
	 * @return this BeIDCardManager to allow for method chaining
	 */
	public BeIDCardManager removeOtherCardEventListener(
			final BeIDCardEventsListener listener) {
		synchronized (this.otherCardListeners) {
			this.otherCardListeners.remove(listener);
		}
		return this;
	}

	/**
	 * Stops this BeIDCardManager. If no CardAndTerminalManager was given at 
	 * construction, this will stop our private CardAndTerminalManager.
	 * After this, no registered listeners will receive any more events.
	 * If a CardAndTerminalManager was given at construction, this has no effect.
	 * @return this BeIDCardManager to allow for method chaining
	 */
	public BeIDCardManager stop() throws InterruptedException {
		if (this.terminalManagerIsPrivate) {
			this.cardAndTerminalManager.stop();
		}
		return this;
	}

	/*
	 * Private Support methods. Shamelessly copied from eid-applet-core
	 */

	private final static byte[] ATR_PATTERN = new byte[]{0x3b, (byte) 0x98,
			0x00, 0x40, 0x00, (byte) 0x00, 0x00, 0x00, 0x01, 0x01, (byte) 0xad,
			0x13, 0x10};
	private final static byte[] ATR_MASK = new byte[]{(byte) 0xff, (byte) 0xff,
			0x00, (byte) 0xff, 0x00, 0x00, 0x00, 0x00, (byte) 0xff,
			(byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xf0};

	private boolean matchesEidAtr(final ATR atr) {
		final byte[] atrBytes = atr.getBytes();
		if (atrBytes.length != ATR_PATTERN.length) {
			return false;
		}
		for (int idx = 0; idx < atrBytes.length; idx++) {
			atrBytes[idx] &= ATR_MASK[idx];
		}
		if (Arrays.equals(atrBytes, ATR_PATTERN)) {
			return true;
		}
		return false;
	}
}
