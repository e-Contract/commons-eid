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

/**
 * A CardAndTerminalManager connects to the CardTerminal PCSC subsystem, and maintains
 * state information on any CardTerminals attached and cards inserted
 * Register a CardEventsListener to get callbacks for reader attach/detach
 * and card insert/removal events.
 * 
 * @author Frank Marien
 * 
 */
package be.fedict.commons.eid.client;

import java.util.HashSet;
import java.util.Set;
import javax.smartcardio.Card;
import javax.smartcardio.CardException;
import javax.smartcardio.CardTerminal;
import javax.smartcardio.CardTerminals;
import javax.smartcardio.CardTerminals.State;
import javax.smartcardio.TerminalFactory;
import be.fedict.commons.eid.client.event.CardEventsListener;
import be.fedict.commons.eid.client.event.CardTerminalEventsListener;
import be.fedict.commons.eid.client.impl.LibJ2PCSCGNULinuxFix;
import be.fedict.commons.eid.client.impl.VoidLogger;
import be.fedict.commons.eid.client.spi.Logger;

public class CardAndTerminalManager implements Runnable {
	private static final int DEFAULT_DELAY = 250;
	private boolean running, subSystemInitialized, autoconnect;
	private Thread worker;
	private Set<CardTerminal> terminalsPresent, terminalsWithCards;
	private CardTerminals cardTerminals;
	private Set<String> terminalsToIgnoreCardEventsFor;
	private Set<CardTerminalEventsListener> cardTerminalEventsListeners;
	private Set<CardEventsListener> cardEventsListeners;
	private int delay;
	private Logger logger;
	private PROTOCOL protocol;

	public enum PROTOCOL {
		T0("T=0"), T1("T=1"), TCL("T=CL"), ANY("*");

		private final String protocol;

		PROTOCOL(final String protocol) {
			this.protocol = protocol;
		}

		String getProtocol() {
			return this.protocol;
		}
	}

	// ----- various constructors ------

	public CardAndTerminalManager() {
		this(new VoidLogger());
	}

	public CardAndTerminalManager(final Logger logger) {
		this(logger, null);
	}

	public CardAndTerminalManager(final CardTerminals cardTerminals) {
		this(new VoidLogger(), cardTerminals);
	}

	public CardAndTerminalManager(final Logger logger,
			final CardTerminals cardTerminals) {
		// work around implementation bug in some GNU/Linux JRE's that causes
		// libpcsc not to be found.
		LibJ2PCSCGNULinuxFix.fixNativeLibrary(logger);

		this.cardTerminalEventsListeners = new HashSet<CardTerminalEventsListener>();
		this.cardEventsListeners = new HashSet<CardEventsListener>();
		this.terminalsToIgnoreCardEventsFor = new HashSet<String>();
		this.delay = DEFAULT_DELAY;
		this.logger = logger;
		this.running = false;
		this.subSystemInitialized = false;
		this.autoconnect = true;
		this.protocol = PROTOCOL.ANY;

		if (cardTerminals == null) {
			final TerminalFactory terminalFactory = TerminalFactory
					.getDefault();
			this.cardTerminals = terminalFactory.terminals();
		} else {
			this.cardTerminals = cardTerminals;
		}
	}

	// --------------------------------------------------------------------------------------------------

	// add a CardTerminalEventsListener
	public CardAndTerminalManager addCardTerminalListener(
			final CardTerminalEventsListener listener) {
		synchronized (this.cardTerminalEventsListeners) {
			this.cardTerminalEventsListeners.add(listener);
		}
		return this;
	}

	// add a CardEventsListener
	public CardAndTerminalManager addCardListener(
			final CardEventsListener listener) {
		synchronized (this.cardEventsListeners) {
			this.cardEventsListeners.add(listener);
		}
		return this;
	}

	// --------------------------------------------------------------------------------------------------

	// start this CardAndTerminalManager
	public CardAndTerminalManager start() {
		this.logger
				.debug("CardAndTerminalManager worker thread start requested.");
		this.worker = new Thread(this, "CardAndTerminalManager");
		this.worker.setDaemon(true);
		this.worker.start();
		return this;
	}

	// --------------------------------------------------------------------------------------------------

	// remove a CardTerminalEventsListener
	public CardAndTerminalManager removeCardTerminalListener(
			final CardTerminalEventsListener listener) {
		synchronized (this.cardTerminalEventsListeners) {
			this.cardTerminalEventsListeners.remove(listener);
		}
		return this;
	}

	// remove a CardEventsListener
	public CardAndTerminalManager removeCardListener(
			final CardEventsListener listener) {
		synchronized (this.cardEventsListeners) {
			this.cardEventsListeners.remove(listener);
		}
		return this;
	}

	// -----------------------------------------------------------------------

	public CardAndTerminalManager ignoreCardEventsFor(final String terminalName) {
		synchronized (this.terminalsToIgnoreCardEventsFor) {
			this.terminalsToIgnoreCardEventsFor.add(terminalName);
		}
		return this;
	}

	public CardAndTerminalManager acceptCardEventsFor(final String terminalName) {
		synchronized (this.terminalsToIgnoreCardEventsFor) {
			this.terminalsToIgnoreCardEventsFor.remove(terminalName);
		}
		return this;
	}

	// -----------------------------------------------------------------------

	// stop this CardAndTerminalManager's worker thread.
	public CardAndTerminalManager stop() throws InterruptedException {
		this.logger
				.debug("CardAndTerminalManager worker thread stop requested.");
		this.running = false;
		this.worker.interrupt();
		this.worker.join();
		return this;
	}

	// -----------------------------------------------------------------------

	@Override
	public void run() {
		this.running = true;
		this.logger.debug("CardAndTerminalManager worker thread started.");

		try {
			// do an initial run, making sure current status is detected
			// this sends terminal attach and card insert events for this
			// initial state to any listeners
			handlePCSCEvents();

			// advise listeners that initial state was sent, and that any
			// further events are relative to this
			listenersInitialized();

			// keep updating
			while (this.running) {
				handlePCSCEvents();
			}
		} catch (final InterruptedException iex) {
			if (this.running) {
				this.logger
						.error("CardAndTerminalManager worker thread unexpectedly interrupted: "
								+ iex.getLocalizedMessage());
			}
		}

		this.logger.debug("CardAndTerminalManager worker thread ended.");
	}

	private void handlePCSCEvents() throws InterruptedException {
		if (!this.subSystemInitialized) {
			this.logger.debug("subsystem not initialized");
			try {
				if (this.terminalsPresent == null
						|| this.terminalsWithCards == null) {
					this.terminalsPresent = new HashSet<CardTerminal>(
							this.cardTerminals.list(State.ALL));
					this.terminalsWithCards = terminalsWithCardsIn(this.terminalsPresent);
				}

				listenersTerminalsAttachedCardsInserted(this.terminalsPresent,
						this.terminalsWithCards);
				this.subSystemInitialized = true;

			} catch (final CardException cex) {
				logCardException(cex,
						"Cannot enumerate card terminals [1] (No Card Readers Connected?)");
				clear();
				sleepForDelay();
				return;
			}
		}

		try {
			// can't use waitForChange properly, that is in blocking mode,
			// without delay argument,
			// since it sometimes misses reader attach events.. (TODO: test on
			// other platforms)
			// this limits us to what is basically a polling strategy, with a
			// small speed
			// gain where waitForChange *does* detect events (because it will
			// return faster than delay)
			// for most events this will make reaction instantaneous, and worst
			// case = delay
			this.cardTerminals.waitForChange(this.delay);
		} catch (final CardException cex) {
			// waitForChange fails (e.g. PCSC is there but no readers)
			logCardException(cex,
					"Cannot wait for card terminal events [2] (No Card Readers Connected?)");
			clear();
			sleepForDelay();
			return;
		} catch (final IllegalStateException ise) {
			// waitForChange fails (e.g. PCSC is not there)
			this.logger
					.debug("Cannot wait for card terminal changes (no PCSC subsystem?): "
							+ ise.getLocalizedMessage());
			clear();
			sleepForDelay();
			return;
		}

		// get here when event has occured or delay time has passed

		try {
			// get fresh state
			final Set<CardTerminal> currentTerminals = new HashSet<CardTerminal>(
					this.cardTerminals.list(State.ALL));
			final Set<CardTerminal> currentTerminalsWithCards = terminalsWithCardsIn(currentTerminals);

			// determine terminals that were attached since previous state
			final Set<CardTerminal> terminalsAttached = new HashSet<CardTerminal>(
					currentTerminals);
			terminalsAttached.removeAll(this.terminalsPresent);

			// determine terminals that had cards inserted since previous state
			final Set<CardTerminal> terminalsWithCardsInserted = new HashSet<CardTerminal>(
					currentTerminalsWithCards);
			terminalsWithCardsInserted.removeAll(this.terminalsWithCards);

			// determine terminals that had cards removed since previous state
			final Set<CardTerminal> terminalsWithCardsRemoved = new HashSet<CardTerminal>(
					this.terminalsWithCards);
			terminalsWithCardsRemoved.removeAll(currentTerminalsWithCards);

			// determine terminals detached since previous state
			final Set<CardTerminal> terminalsDetached = new HashSet<CardTerminal>(
					this.terminalsPresent);
			terminalsDetached.removeAll(currentTerminals);

			// keep fresh state to compare to next time (and to return to
			// synchronous callers)
			this.terminalsPresent = currentTerminals;
			this.terminalsWithCards = currentTerminalsWithCards;

			// advise the listeners where appropriate, always in the order
			// attach, insert, remove, detach
			listenersUpdateInSequence(terminalsAttached,
					terminalsWithCardsInserted, terminalsWithCardsRemoved,
					terminalsDetached);
		} catch (final CardException cex) {
			// if a CardException occurs, assume we're out of readers (only
			// CardTerminals.list throws that here)
			// CardTerminal fails in that case, instead of simply seeing zero
			// CardTerminals.
			logCardException(cex,
					"Cannot wait for card terminal changes (no PCSC subsystem?)");
			clear();
			sleepForDelay();
		}
	}

	// ---------------------------------------------------------------------------------------------------

	private boolean areCardEventsIgnoredFor(final CardTerminal cardTerminal) {
		synchronized (this.terminalsToIgnoreCardEventsFor) {
			for (String prefixToMatch : this.terminalsToIgnoreCardEventsFor) {
				if (cardTerminal.getName().startsWith(prefixToMatch)) {
					return true;
				}
			}
		}

		return false;
	}

	private Set<CardTerminal> terminalsWithCardsIn(
			final Set<CardTerminal> terminals) {
		final Set<CardTerminal> terminalsWithCards = new HashSet<CardTerminal>();

		synchronized (this.terminalsToIgnoreCardEventsFor) {
			for (CardTerminal terminal : terminals) {
				try {
					if (terminal.isCardPresent()
							&& !this.areCardEventsIgnoredFor(terminal)) {
						terminalsWithCards.add(terminal);
					}
				} catch (final CardException cex) {
					this.logger
							.error("Problem determining card presence in terminal ["
									+ terminal.getName() + "]");
				}
			}
		}

		return terminalsWithCards;
	}

	// -------------------------------------
	// --------- getters/setters -----------
	// -------------------------------------

	// get polling/retry delay currently in use
	public int getDelay() {
		return this.delay;
	}

	// set polling/retry delay.
	public CardAndTerminalManager setDelay(final int delay) {
		this.delay = delay;
		return this;
	}

	public boolean isAutoconnect() {
		return this.autoconnect;
	}

	public CardAndTerminalManager setAutoconnect(final boolean autoconnect) {
		this.autoconnect = autoconnect;
		return this;
	}

	public PROTOCOL getProtocol() {
		return this.protocol;
	}

	public CardAndTerminalManager setProtocol(final PROTOCOL protocol) {
		this.protocol = protocol;
		return this;
	}

	// -------------------------------------------------
	// --------- private convenience methods -----------
	// -------------------------------------------------

	// return to the uninitialized state
	private void clear() {
		// if we were already intialized, we may have sent attached and insert
		// events we now pretend to remove and detach all that we know of, for
		// consistency
		if (this.subSystemInitialized) {
			listenersCardsRemovedTerminalsDetached(this.terminalsWithCards,
					this.terminalsPresent);
		}
		this.terminalsPresent = null;
		this.terminalsWithCards = null;
		this.subSystemInitialized = false;
		this.logger.debug("cleared");
	}

	private void listenersTerminalsAttachedCardsInserted(
			final Set<CardTerminal> attached, final Set<CardTerminal> inserted)
			throws CardException {
		listenersTerminalsAttached(attached);
		listenersTerminalsWithCardsInserted(inserted);
	}

	private void listenersCardsRemovedTerminalsDetached(
			final Set<CardTerminal> removed, final Set<CardTerminal> detached) {
		listenersTerminalsWithCardsRemoved(removed);
		listenersTerminalsDetached(detached);
	}

	private void listenersUpdateInSequence(final Set<CardTerminal> attached,
			final Set<CardTerminal> inserted, final Set<CardTerminal> removed,
			final Set<CardTerminal> detached) throws CardException {
		listenersTerminalsAttached(attached);
		listenersTerminalsWithCardsInserted(inserted);
		listenersTerminalsWithCardsRemoved(removed);
		listenersTerminalsDetached(detached);
	}

	private void listenersInitialized() {
		listenersTerminalEventsInitialized();
		listenersCardEventsInitialized();
	}

	private void listenersCardEventsInitialized() {
		Set<CardEventsListener> copyOfListeners;

		synchronized (this.cardEventsListeners) {
			copyOfListeners = new HashSet<CardEventsListener>(
					this.cardEventsListeners);
		}

		for (CardEventsListener listener : copyOfListeners) {
			try {
				listener.cardEventsInitialized();
			} catch (final Exception thrownInListener) {
				this.logger
						.error("Exception thrown in CardEventsListener.cardRemoved:"
								+ thrownInListener.getMessage());
			}
		}
	}

	private void listenersTerminalEventsInitialized() {
		Set<CardTerminalEventsListener> copyOfListeners;

		synchronized (this.cardTerminalEventsListeners) {
			copyOfListeners = new HashSet<CardTerminalEventsListener>(
					this.cardTerminalEventsListeners);
		}

		for (CardTerminalEventsListener listener : copyOfListeners) {
			try {
				listener.terminalEventsInitialized();
			} catch (final Exception thrownInListener) {
				this.logger
						.error("Exception thrown in CardTerminalEventsListener.terminalAttached:"
								+ thrownInListener.getMessage());
			}
		}
	}

	// Tell listeners about attached readers
	protected void listenersTerminalsAttached(final Set<CardTerminal> attached) {
		if (!attached.isEmpty()) {
			Set<CardTerminalEventsListener> copyOfListeners;

			synchronized (this.cardTerminalEventsListeners) {
				copyOfListeners = new HashSet<CardTerminalEventsListener>(
						this.cardTerminalEventsListeners);
			}

			for (CardTerminal terminal : attached) {
				for (CardTerminalEventsListener listener : copyOfListeners) {
					try {
						listener.terminalAttached(terminal);
					} catch (final Exception thrownInListener) {
						this.logger
								.error("Exception thrown in CardTerminalEventsListener.terminalAttached:"
										+ thrownInListener.getMessage());
					}
				}
			}
		}
	}

	// Tell listeners about detached readers
	protected void listenersTerminalsDetached(final Set<CardTerminal> detached) {
		if (!detached.isEmpty()) {
			Set<CardTerminalEventsListener> copyOfListeners;

			synchronized (this.cardTerminalEventsListeners) {
				copyOfListeners = new HashSet<CardTerminalEventsListener>(
						this.cardTerminalEventsListeners);
			}

			for (CardTerminal terminal : detached) {
				for (CardTerminalEventsListener listener : copyOfListeners) {
					try {
						listener.terminalDetached(terminal);
					} catch (final Exception thrownInListener) {
						this.logger
								.error("Exception thrown in CardTerminalEventsListener.terminalDetached:"
										+ thrownInListener.getMessage());
					}
				}
			}
		}
	}

	// Tell listeners about removed cards
	protected void listenersTerminalsWithCardsRemoved(
			final Set<CardTerminal> removed) {
		if (!removed.isEmpty()) {
			Set<CardEventsListener> copyOfListeners;

			synchronized (this.cardEventsListeners) {
				copyOfListeners = new HashSet<CardEventsListener>(
						this.cardEventsListeners);
			}

			for (CardTerminal terminal : removed) {
				for (CardEventsListener listener : copyOfListeners) {
					try {
						listener.cardRemoved(terminal);
					} catch (final Exception thrownInListener) {
						this.logger
								.error("Exception thrown in CardEventsListener.cardRemoved:"
										+ thrownInListener.getMessage());
					}
				}
			}
		}
	}

	// Tell listeners about inserted cards. giving them the CardTerminal and a
	// Card object
	// if this.autoconnect is enabled (the default), the card argument may be
	// automatically
	// filled out, but it may still be null, if the connect failed.
	protected void listenersTerminalsWithCardsInserted(
			final Set<CardTerminal> inserted) {
		if (!inserted.isEmpty()) {
			Set<CardEventsListener> copyOfListeners;

			synchronized (this.cardEventsListeners) {
				copyOfListeners = new HashSet<CardEventsListener>(
						this.cardEventsListeners);
			}

			for (CardTerminal terminal : inserted) {
				Card card = null;

				if (this.autoconnect) {
					try {
						card = terminal.connect(this.protocol.getProtocol());
					} catch (final CardException cex) {
						this.logger.debug("terminal.connect("
								+ this.protocol.getProtocol() + ") failed. "
								+ cex.getMessage());
					}
				}

				for (CardEventsListener listener : copyOfListeners) {
					try {
						listener.cardInserted(terminal, card);
					} catch (final Exception thrownInListener) {
						this.logger
								.error("Exception thrown in CardEventsListener.cardInserted:"
										+ thrownInListener.getMessage());
					}

				}
			}
		}
	}

	private void sleepForDelay() throws InterruptedException {
		Thread.sleep(this.delay);
	}

	private void logCardException(final CardException cex, final String where) {
		this.logger.debug(where + ": " + cex.getMessage());
		this.logger.debug("no card readers connected?");
		final Throwable cause = cex.getCause();
		if (cause == null) {
			return;
		}
		this.logger.debug("cause: " + cause.getMessage());
		this.logger.debug("cause type: " + cause.getClass().getName());
	}
}
