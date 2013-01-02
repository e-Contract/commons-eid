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

/**
 * A CardAndTerminalManager maintains an active state overview of
 * all javax.smartcardio.CardTerminal attached to a system's pcsc
 * subsystem, and notifies registered:
 * <ul>
 * <li>CardTerminalEventsListeners of any CardTerminals Attached or Detached
 * <li>CardEventsListeners of any Cards inserted into or removed from any attached CardTerminals
 * </ul>
 * Note that at the level of CardAndTerminalManager there is no distinction between types of cards
 * or terminals: They are merely reported using the standard javax.smartcardio classes.
 * 
 * @author Frank Marien
 *
 */
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

	/**
	 * Instantiate a CardAndTerminalManager working on the standard smartcardio CardTerminals,
	 * and without any logging.
	 */
	public CardAndTerminalManager() {
		this(new VoidLogger());
	}

	/**
	 * Instantiate a CardAndTerminalManager working on the standard smartcardio CardTerminals,
	 * and logging to the Logger implementation given.
	 * @param logger an instance of be.fedict.commons.eid.spi.Logger that will be send all the logs
	 */
	public CardAndTerminalManager(final Logger logger) {
		this(logger, null);
	}

	/**
	 * Instantiate a CardAndTerminalManager working on a specific CardTerminals instance
	 * and without any logging. In normal operation, you would use the constructor that 
	 * takes no CardTerminals parameter, but using this one you could, for example
	 * obtain a CardTerminals instance from a different TerminalFactory, or from your
	 * own implementation.
	 * @param cardTerminals instance to obtain terminal and card events from
	 */
	public CardAndTerminalManager(final CardTerminals cardTerminals) {
		this(new VoidLogger(), cardTerminals);
	}

	/**
	 * Instantiate a CardAndTerminalManager working on a specific CardTerminals instance,
	 * and that logs to the given Logger.In normal operation, you would use the constructor that 
	 * takes no CardTerminals parameter, but using this one you could, for example
	 * obtain a CardTerminals instance from a different TerminalFactory, or from your
	 * own implementation.
	 * @param logger an instance of be.fedict.commons.eid.spi.Logger that will be send all the logs
	 * @param cardTerminals instance to obtain terminal and card events from
	 */
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

	/**
	 * Register a CardTerminalEventsListener instance. This will subsequently
	 * be called for any Terminal Attaches/Detaches on CardTerminals that we're not ignoring
	 * @see #ignoreCardEventsFor(String)
	 * @param listener the CardTerminalEventsListener to be registered
	 * @return this CardAndTerminalManager to allow for method chaining.
	 */
	public CardAndTerminalManager addCardTerminalListener(
			final CardTerminalEventsListener listener) {
		synchronized (this.cardTerminalEventsListeners) {
			this.cardTerminalEventsListeners.add(listener);
		}
		return this;
	}

	/**
	 * Register a CardEventsListener instance. This will subsequently
	 * be called for any Card Inserts/Removals on CardTerminals that we're not ignoring 
	 * @see #ignoreCardEventsFor(String)
	 * @param listener the CardEventsListener to be registered
	 * @return this CardAndTerminalManager to allow for method chaining.
	 */
	public CardAndTerminalManager addCardListener(
			final CardEventsListener listener) {
		synchronized (this.cardEventsListeners) {
			this.cardEventsListeners.add(listener);
		}
		return this;
	}

	// --------------------------------------------------------------------------------------------------

	/**
	 * Start this CardAndTerminalManager. Doing this after registering 
	 * one or more CardTerminalEventsListener and/or CardEventsListener instances
	 * will cause these be be called with the initial situation: The terminals
	 * and cards already present. Calling start() before registering any listeners
	 * will cause these to not see the initial situation.
	 * @return this CardAndTerminalManager to allow for method chaining.
	 */
	public CardAndTerminalManager start() {
		this.logger
				.debug("CardAndTerminalManager worker thread start requested.");
		this.worker = new Thread(this, "CardAndTerminalManager");
		this.worker.setDaemon(true);
		this.worker.start();
		return this;
	}

	// --------------------------------------------------------------------------------------------------

	/**
	 * Unregister a CardTerminalEventsListener instance.
	 * @param listener the CardTerminalEventsListener to be unregistered
	 * @return this CardAndTerminalManager to allow for method chaining.
	 */
	public CardAndTerminalManager removeCardTerminalListener(
			final CardTerminalEventsListener listener) {
		synchronized (this.cardTerminalEventsListeners) {
			this.cardTerminalEventsListeners.remove(listener);
		}
		return this;
	}

	/**
	 * Unregister a CardEventsListener instance. 
	 * @param listener the CardEventsListener to be unregistered
	 * @return this CardAndTerminalManager to allow for method chaining.
	 */
	public CardAndTerminalManager removeCardListener(
			final CardEventsListener listener) {
		synchronized (this.cardEventsListeners) {
			this.cardEventsListeners.remove(listener);
		}
		return this;
	}

	// -----------------------------------------------------------------------

	/**
	 * Start ignoring the CardTerminal with the name given. A CardTerminal's
	 * name is the exact String as returned by {@link javax.smartcardio.CardTerminal#getName() CardTerminal.getName()}
	 * Note that this name is neither very stable, nor portable between operating systems: it is
	 * constructed by the PCSC subsystem in an arbitrary fashion, and may change between releases.
	 * @param terminalName
	 * @return this CardAndTerminalManager to allow for method chaining.
	 */
	public CardAndTerminalManager ignoreCardEventsFor(final String terminalName) {
		synchronized (this.terminalsToIgnoreCardEventsFor) {
			this.terminalsToIgnoreCardEventsFor.add(terminalName);
		}
		return this;
	}

	/**
	 * Start accepting events for the CardTerminal with the name given, where
	 * these were being ignored due to a previous call to {@link #ignoreCardEventsFor(String)}.
	 * @param terminalName
	 * @return this CardAndTerminalManager to allow for method chaining.
	 */
	public CardAndTerminalManager acceptCardEventsFor(final String terminalName) {
		synchronized (this.terminalsToIgnoreCardEventsFor) {
			this.terminalsToIgnoreCardEventsFor.remove(terminalName);
		}
		return this;
	}

	// -----------------------------------------------------------------------

	/**
	 * Stop this CardAndTerminalManager. This will may block until the 
	 * worker thread has returned, meaning that after this call returns,
	 * no registered listeners will receive any more events.
	 * @return this CardAndTerminalManager to allow for method chaining.
	 */
	public CardAndTerminalManager stop() throws InterruptedException {
		this.logger
				.debug("CardAndTerminalManager worker thread stop requested.");
		this.running = false;
		this.worker.interrupt();
		this.worker.join();
		return this;
	}

	/**
	 * Returns the PCSC polling delay currently in use
	 * @return the PCSC polling delay currently in use
	 */
	public int getDelay() {
		return this.delay;
	}

	/**
	 * Set the PCSC polling delay. A CardAndTerminalsManager will wait
	 * for a maximum of newDelay milliseconds for new events to be received,
	 * before issuing a new call to the PCSC subsystem. The higher this number,
	 * the less CPU this CardAndTerminalsManager will take, but the greater the
	 * chance that terminal attach/detach events will be noticed late.
	 * @param newDelay the new delay to trust the PCSC subsystem for
	 * @return this CardAndTerminalManager to allow for method chaining.
	 */
	public CardAndTerminalManager setDelay(final int newDelay) {
		this.delay = newDelay;
		return this;
	}

	/**
	 * Return whether this CardAndTerminalsManager will automatically connect()
	 * to any cards inserted.
	 * @return this CardAndTerminalManager to allow for method chaining.
	 */
	public boolean isAutoconnect() {
		return this.autoconnect;
	}

	/**
	 * Set whether this CardAndTerminalsManager will automatically connect()
	 * to any cards inserted.
	 * @return this CardAndTerminalManager to allow for method chaining.
	 */
	public CardAndTerminalManager setAutoconnect(final boolean newAutoConnect) {
		this.autoconnect = newAutoConnect;
		return this;
	}

	/**
	 * return which card protocols this CardAndTerminalsManager will attempt
	 * to connect to cards with. (if autoconnect is true, see {@link CardAndTerminalManager#setAutoconnect(boolean)})
	 * the default is PROTOCOL.ANY which allows any protocol.
	 * @return the currently attempted protocol(s)
	 */
	public PROTOCOL getProtocol() {
		return this.protocol;
	}

	/**
	 * Determines which card protocols this CardAndTerminalsManager will attempt
	 * to connect to cards with. (if autoconnect is true, see {@link CardAndTerminalManager#setAutoconnect(boolean)})
	 * the default is PROTOCOL.ANY which allows any protocol.
	 *
	 * @param newProtocol the card protocol(s) to attempt connection to the cards with
	 * @return this CardAndTerminalManager to allow for method chaining.
	 */
	public CardAndTerminalManager setProtocol(final PROTOCOL newProtocol) {
		this.protocol = newProtocol;
		return this;
	}

	// ---------------------------
	// Private Implementation.. 
	// ---------------------------

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

	// -------------------------------------------------
	// --------- private convenience methods -----------
	// -------------------------------------------------

	// return to the uninitialized state
	private void clear() {
		// if we were already initialized, we may have sent attached and insert
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
	private void listenersTerminalsAttached(final Set<CardTerminal> attached) {
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
	private void listenersTerminalsDetached(final Set<CardTerminal> detached) {
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
	private void listenersTerminalsWithCardsRemoved(
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
	private void listenersTerminalsWithCardsInserted(
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
