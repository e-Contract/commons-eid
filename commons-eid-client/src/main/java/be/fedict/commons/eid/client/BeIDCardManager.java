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
import be.fedict.commons.eid.client.event.BeIDCardEventsListener;
import be.fedict.commons.eid.client.event.CardEventsListener;
import be.fedict.commons.eid.client.impl.VoidLogger;
import be.fedict.commons.eid.client.spi.Logger;

public class BeIDCardManager {
	private CardAndTerminalManager cardAndTerminalManager;
	private boolean terminalManagerIsPrivate;
	private Map<CardTerminal, BeIDCard> terminalsAndCards;
	private Set<BeIDCardEventsListener> beIdListeners;
	private Set<CardEventsListener> otherCardListeners;
	private final Logger logger;

	/*
	 * a BeIDCardManager with a default (void) logger and a private
	 * CardAndTerminalManager that is automatically started and stopped with the
	 * BeIDCardManager
	 */
	public BeIDCardManager() {
		this(new VoidLogger());
	}

	/*
	 * a BeIDCardManager logging to logger, and a private Terminalmanager that
	 * is automatically started and stopped with the BeIDCardManager
	 */
	public BeIDCardManager(Logger logger) {
		this(logger, new CardAndTerminalManager());
		this.terminalManagerIsPrivate = true;
	}

	/*
	 * a BeIDCardManager with a default (void) logger, caller supplies a
	 * CardAndTerminalManager. note: caller is responsible for start()in the
	 * supplied CardAndTerminalManager, it will not be automatically started!
	 */
	public BeIDCardManager(CardAndTerminalManager cardAndTerminalManager) {
		this(new VoidLogger(), cardAndTerminalManager);
	}

	/*
	 * a BeIDCardManager logging to logger, caller supplies a
	 * CardAndTerminalManager. note: caller is responsible for start()in the
	 * supplied CardAndTerminalManager, it will not be automatically started!
	 */
	public BeIDCardManager(Logger logger,
			CardAndTerminalManager cardAndTerminalManager) {
		this.logger = logger;
		this.beIdListeners = new HashSet<BeIDCardEventsListener>();
		this.otherCardListeners = new HashSet<CardEventsListener>();
		this.terminalsAndCards = new HashMap<CardTerminal, BeIDCard>();
		this.cardAndTerminalManager = cardAndTerminalManager;
		this.cardAndTerminalManager.addCardListener(new CardEventsListener() {
			@Override
			public void cardInserted(CardTerminal cardTerminal, Card card) {
				if (card != null && matchesEidAtr(card.getATR())) {
					BeIDCard beIDCard = new BeIDCard(card,
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
						} catch (Throwable thrownInListener) {
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
						} catch (Throwable thrownInListener) {
							BeIDCardManager.this.logger
									.error("Exception thrown in CardEventsListener.cardInserted:"
											+ thrownInListener.getMessage());
						}
					}
				}
			}

			@Override
			public void cardRemoved(CardTerminal cardTerminal) {
				BeIDCard beIDCard = BeIDCardManager.this.terminalsAndCards
						.get(cardTerminal);
				if (beIDCard != null) {
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
						} catch (Throwable thrownInListener) {
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
						} catch (Throwable thrownInListener) {
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
					} catch (Throwable thrownInListener) {
						BeIDCardManager.this.logger
								.error("Exception thrown in BeIDCardEventsListener.eIDCardInserted:"
										+ thrownInListener.getMessage());
					}
				}

				Set<CardEventsListener> copyOfOtherCardEventsListeners = null;

				synchronized (BeIDCardManager.this.beIdListeners) {
					copyOfOtherCardEventsListeners = new HashSet<CardEventsListener>(
							BeIDCardManager.this.otherCardListeners);
				}

				for (CardEventsListener listener : copyOfOtherCardEventsListeners) {
					try {
						listener.cardEventsInitialized();
					} catch (Throwable thrownInListener) {
						BeIDCardManager.this.logger
								.error("Exception thrown in BeIDCardEventsListener.eIDCardInserted:"
										+ thrownInListener.getMessage());
					}
				}
			}
		});
	}

	public BeIDCardManager start() {
		if (this.terminalManagerIsPrivate) {
			this.cardAndTerminalManager.start();
		}
		return this;
	}

	// add a BeIDCardEventsListener to be notified of BeID cards being inserted
	public BeIDCardManager addBeIDCardEventListener(
			BeIDCardEventsListener listener) {
		synchronized (this.beIdListeners) {
			this.beIdListeners.add(listener);
		}
		return this;
	}

	// remove a BeIDCardEventsListener
	public BeIDCardManager removeBeIDCardListener(
			BeIDCardEventsListener listener) {
		synchronized (this.beIdListeners) {
			this.beIdListeners.remove(listener);
		}
		return this;
	}

	// add a CardEventsListener to get notified of other cards being
	// inserted/removed
	public BeIDCardManager addOtherCardEventListener(CardEventsListener listener) {
		synchronized (this.otherCardListeners) {
			this.otherCardListeners.add(listener);
		}
		return this;
	}

	// remove a CardEventsListener
	public BeIDCardManager removeOtherCardEventListener(
			BeIDCardEventsListener listener) {
		synchronized (this.otherCardListeners) {
			this.otherCardListeners.remove(listener);
		}
		return this;
	}

	public BeIDCardManager stop() throws InterruptedException {
		if (this.terminalManagerIsPrivate) {
			this.cardAndTerminalManager.stop();
		}
		return this;
	}

	/*
	 * Private Support methods
	 */

	private final static byte[] ATR_PATTERN = new byte[]{0x3b, (byte) 0x98,
			0x00, 0x40, 0x00, (byte) 0x00, 0x00, 0x00, 0x01, 0x01, (byte) 0xad,
			0x13, 0x10};
	private final static byte[] ATR_MASK = new byte[]{(byte) 0xff, (byte) 0xff,
			0x00, (byte) 0xff, 0x00, 0x00, 0x00, 0x00, (byte) 0xff,
			(byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xf0};

	private boolean matchesEidAtr(ATR atr) {
		byte[] atrBytes = atr.getBytes();
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
