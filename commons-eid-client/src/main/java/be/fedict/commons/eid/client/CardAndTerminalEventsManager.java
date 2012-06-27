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
 * A CardAndTerminalEventsManager connects to the CardTerminal PCSC subsystem, and maintains
 * state information on any CardTerminals attached and cards inserted
 * Register a CardEventsListener to get callbacks for reader attach/detach
 * and card insert/removal events.
 * 
 * @author Frank Marien
 * 
 */
package be.fedict.commons.eid.client;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import javax.smartcardio.Card;
import javax.smartcardio.CardException;
import javax.smartcardio.CardTerminal;
import javax.smartcardio.CardTerminals;
import javax.smartcardio.CardTerminals.State;
import javax.smartcardio.TerminalFactory;

public class CardAndTerminalEventsManager implements Runnable
{
	private static final int				DEFAULT_DELAY	=250;
	private boolean							running,initialized,autoconnect,artificialEvents;
	private Thread							thread;
	private Set<CardTerminal>				terminalsPresent,terminalsWithCards;
	private CardTerminals					cardTerminals;
	private Set<String>						terminalsToIgnoreCardEventsFor;
	private Set<CardTerminalEventsListener>	cardTerminalEventsListeners;
	private Set<CardEventsListener>			cardEventsListeners;
	private int								delay;
	private Logger							logger;

	// ----- various constructors ------

	public CardAndTerminalEventsManager()
	{
		this(new VoidLogger());
	}

	public CardAndTerminalEventsManager(Logger logger)
	{
		this(logger,null);
	}

	public CardAndTerminalEventsManager(CardTerminals cardTerminals)
	{
		this(new VoidLogger(),cardTerminals);
	}

	public CardAndTerminalEventsManager(Logger logger, CardTerminals cardTerminals)
	{
		// work around implementation bug in some GNU/Linux JRE's that causes
		// libpcsc not to be found.
		LibJ2PCSCGNULinuxFix.fixNativeLibrary(logger);

		this.cardTerminalEventsListeners=new HashSet<CardTerminalEventsListener>();
		this.cardEventsListeners=new HashSet<CardEventsListener>();
		this.terminalsToIgnoreCardEventsFor=new HashSet<String>();
		this.delay=DEFAULT_DELAY;
		this.logger=logger;
		this.running=false;
		this.initialized=false;
		this.autoconnect=true;
		this.artificialEvents=false;
		
		if(cardTerminals==null)
		{
			TerminalFactory terminalFactory=TerminalFactory.getDefault();
			this.cardTerminals=terminalFactory.terminals();
		}
		else
		{
			this.cardTerminals=cardTerminals;
		}
		
		try
		{
			// initial update, so that the first getters called will have results
			// regardless of running state
			updateTerminalsPresent();
		}
		catch(CardException cex)
		{
			logCardException(cex,"Cannot enumerate card terminals [0] (No Card Readers Connected?)");
		}
	}

	// --------------------------------------------------------------------------------------------------

	// add a CardTerminalEventsListener
	public CardAndTerminalEventsManager addCardTerminalListener(CardTerminalEventsListener listener)
	{
		synchronized(cardTerminalEventsListeners)
		{
			cardTerminalEventsListeners.add(listener);
		}
		return this;
	}

	// add a CardEventsListener
	public CardAndTerminalEventsManager addCardListener(CardEventsListener listener)
	{
		synchronized(cardEventsListeners)
		{
			cardEventsListeners.add(listener);
		}
		return this;
	}

	// --------------------------------------------------------------------------------------------------

	// start this CardAndTerminalEventsManager in the background as a Thread
	public CardAndTerminalEventsManager start()
	{
		logger.debug("CardAndTerminalEventsManager worker thread start requested.");
		thread=new Thread(this,"CardAndTerminalEventsManager");
		thread.setDaemon(true);
		thread.start();
		return this;
	}
	
	// --------------------------------------------------------------------------------------------------

	// remove a CardTerminalEventsListener
	public CardAndTerminalEventsManager removeCardTerminalListener(CardTerminalEventsListener listener)
	{
		synchronized(cardTerminalEventsListeners)
		{
			cardTerminalEventsListeners.remove(listener);
		}
		return this;
	}

	// remove a CardEventsListener
	public CardAndTerminalEventsManager removeCardListener(CardEventsListener listener)
	{
		synchronized(cardEventsListeners)
		{
			cardEventsListeners.remove(listener);
		}
		return this;
	}

	// -----------------------------------------------------------------------

	public boolean ignoreCardEventsFor(String terminalName)
	{
		synchronized(terminalsToIgnoreCardEventsFor)
		{
			return terminalsToIgnoreCardEventsFor.add(terminalName);
		}
	}

	public boolean acceptCardEventsFor(String terminalName)
	{
		synchronized(terminalsToIgnoreCardEventsFor)
		{
			return terminalsToIgnoreCardEventsFor.remove(terminalName);
		}
	}
	
	// -----------------------------------------------------------------------

	// stop this Terminalmanager's worked thread.
	public CardAndTerminalEventsManager stop()
	{
		logger.debug("CardAndTerminalEventsManager worker thread stop requested.");
		running=false;
		return this;
	}
	
	// -----------------------------------------------------------------------

	@Override
	public void run()
	{
		running=true;
		logger.debug("CardAndTerminalEventsManager worker thread started.");

		try
		{
			while(running)
				handlePCSCEvents();
		}
		catch(InterruptedException ie)
		{
			if(running)
				logger.error("CardAndTerminalEventsManager worker thread unexpectedly interrupted: "+ie.getLocalizedMessage());
		}

		logger.debug("CardAndTerminalEventsManager worker thread ended.");
	}

	private void handlePCSCEvents() throws InterruptedException
	{
		if(!initialized)
		{
			logger.debug("not initialized");
			try
			{
				if(terminalsPresent==null || terminalsWithCards==null)
					updateTerminalsPresent();
				
				if(artificialEvents)
					listenersTerminalsAttachedCardsInserted(terminalsPresent,terminalsWithCards);
				initialized=true;
			}
			catch(CardException cex)
			{
				logCardException(cex,"Cannot enumerate card terminals [1] (No Card Readers Connected?)");
				clear();
				listenersException(cex);
				sleepForDelay();
				return;
			}
		}

		try
		{
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
			cardTerminals.waitForChange(delay);
		}
		catch(CardException cex)
		{
			// waitForChange fails (e.g. PCSC is there but no readers)
			logCardException(cex,"Cannot wait for card terminal events [2] (No Card Readers Connected?)");
			listenersException(cex);
			clear();
			sleepForDelay();
			return;
		}
		catch(IllegalStateException ise)
		{
			// waitForChange fails (e.g. PCSC is not there)
			logger.debug("Cannot wait for card terminal changes (no PCSC subsystem?): "+ise.getLocalizedMessage());
			listenersException(ise);
			clear();
			sleepForDelay();
			return;
		}

		// get here when event has occured or delay time has passed

		try
		{
			// get fresh state
			Set<CardTerminal> currentTerminals=new HashSet<CardTerminal>(cardTerminals.list(State.ALL));
			Set<CardTerminal> currentTerminalsWithCards=terminalsWithCardsIn(currentTerminals);

			// determine terminals that were attached since previous state
			Set<CardTerminal> terminalsAttached=new HashSet<CardTerminal>(currentTerminals);
			terminalsAttached.removeAll(this.terminalsPresent);

			// determine terminals that had cards inserted since previous state
			Set<CardTerminal> terminalsWithCardsInserted=new HashSet<CardTerminal>(currentTerminalsWithCards);
			terminalsWithCardsInserted.removeAll(this.terminalsWithCards);

			// determine terminals that had cards removed since previous state
			Set<CardTerminal> terminalsWithCardsRemoved=new HashSet<CardTerminal>(this.terminalsWithCards);
			terminalsWithCardsRemoved.removeAll(currentTerminalsWithCards);

			// determine terminals detached since previous state
			Set<CardTerminal> terminalsDetached=new HashSet<CardTerminal>(this.terminalsPresent);
			terminalsDetached.removeAll(currentTerminals);

			// keep fresh state to compare to next time (and to return to synchronous callers)
			this.terminalsPresent=currentTerminals;
			this.terminalsWithCards=currentTerminalsWithCards;

			// advise the listeners where appropriate, always in the order
			// attach, insert, remove, detach
			listenersUpdateInSequence(terminalsAttached,terminalsWithCardsInserted,terminalsWithCardsRemoved,terminalsDetached);
		}
		catch(CardException cex)
		{
			// if a CardException occurs, assume we're out of readers (only
			// CardTerminals.list throws that here)
			// CardTerminal fails in that case, instead of simply seeing zero
			// CardTerminals.
			logCardException(cex,"Cannot wait for card terminal changes (no PCSC subsystem?)");
			listenersException(cex);
			clear();
			sleepForDelay();
		}
	}

	// ---------------------------------------------------------------------------------------------------
	
	private boolean areCardEventsIgnoredFor(CardTerminal cardTerminal)
	{
		synchronized(terminalsToIgnoreCardEventsFor)
		{
			for(String prefixToMatch : terminalsToIgnoreCardEventsFor)
			{
				if(cardTerminal.getName().startsWith(prefixToMatch))
					return true;
			}
		}
		
		return false;
	}

	private Set<CardTerminal> terminalsWithCardsIn(Set<CardTerminal> terminals)
	{
		Set<CardTerminal> terminalsWithCards=new HashSet<CardTerminal>();
		
		synchronized(terminalsToIgnoreCardEventsFor)
		{
			for(CardTerminal terminal:terminals)
			{
				try
				{
					if(terminal.isCardPresent() && !areCardEventsIgnoredFor(terminal))
						terminalsWithCards.add(terminal);
				}
				catch(CardException e)
				{
					logger.error("Problem determining card presence in terminal ["+terminal.getName()+"]");
				}
			}
		}
		
		return terminalsWithCards;
	}

	// -------------------------------------
	// --------- getters/setters -----------
	// -------------------------------------

	// get polling/retry delay currently in use
	public int getDelay()
	{
		return delay;
	}

	// set polling/retry delay.
	public CardAndTerminalEventsManager setDelay(int delay)
	{
		this.delay=delay;
		return this;
	}

	public boolean isAutoconnect()
	{
		return autoconnect;
	}

	public CardAndTerminalEventsManager setAutoconnect(boolean autoconnect)
	{
		this.autoconnect=autoconnect;
		return this;
	}

	/*
	 * getTerminalspresent returns a Set of Terminals connected at time of call,
	 * when not running, or at last event, when running.
	 */
	public Set<CardTerminal> getTerminalsPresent() throws CardException
	{
		if(!running)
			updateTerminalsPresent();
		return Collections.unmodifiableSet(terminalsPresent);
	}

	/*
	 * getTerminalsWithCards returns a Set of Terminals connected and with a
	 * card inserted, at time of call, when not running, or at last event, when
	 * running.
	 */
	public Set<CardTerminal> getTerminalsWithCards() throws CardException
	{
		if(!running)
			updateTerminalsPresent();
		return Collections.unmodifiableSet(terminalsWithCards);
	}
	
	/*
	 * return whether this instance is running, and will update itself
	 */
	public boolean isRunning()
	{
		return running;
	}
	
	public boolean sendsArtificialEvents()
	{
		return artificialEvents;
	}

	public void setArtificialEvents(boolean artificialEvents)
	{
		this.artificialEvents=artificialEvents;
	}

	// -------------------------------------------------
	// --------- private convenience methods -----------
	// -------------------------------------------------

	

	// return to the uninitialized state
	private void clear()
	{
		// if we were already intialized, we may have sent attached and insert
		// events we now pretend to remove and detach all that we know of, for
		// consistency
		if(artificialEvents && initialized)
			listenersCardsRemovedTerminalsDetached(terminalsWithCards,terminalsPresent);
		terminalsPresent=null;
		terminalsWithCards=null;
		initialized=false;
		logger.debug("cleared");
	}

	private void listenersTerminalsAttachedCardsInserted(Set<CardTerminal> attached,Set<CardTerminal> inserted) throws CardException
	{
		listenersTerminalsAttached(attached);
		listenersTerminalsWithCardsInserted(inserted);
	}

	private void listenersCardsRemovedTerminalsDetached(Set<CardTerminal> removed,Set<CardTerminal> detached)
	{
		listenersTerminalsWithCardsRemoved(removed);
		listenersTerminalsDetached(detached);
	}

	private void listenersUpdateInSequence(Set<CardTerminal> attached,Set<CardTerminal> inserted,Set<CardTerminal> removed,Set<CardTerminal> detached) throws CardException
	{
		listenersTerminalsAttached(attached);
		listenersTerminalsWithCardsInserted(inserted);
		listenersTerminalsWithCardsRemoved(removed);
		listenersTerminalsDetached(detached);
	}

	// Tell listeners about attached readers
	protected void listenersTerminalsAttached(Set<CardTerminal> attached)
	{
		for(CardTerminal terminal:attached)
		{
			synchronized(cardTerminalEventsListeners)
			{
				for(CardTerminalEventsListener listener:cardTerminalEventsListeners)
					listener.terminalAttached(terminal);
			}
		}
	}

	// Tell listeners about detached readers
	protected void listenersTerminalsDetached(Set<CardTerminal> detached)
	{
		for(CardTerminal terminal:detached)
		{
			synchronized(cardTerminalEventsListeners)
			{
				for(CardTerminalEventsListener listener:cardTerminalEventsListeners)
					listener.terminalDetached(terminal);
			}
		}
	}

	// Tell listeners about removed cards
	protected void listenersTerminalsWithCardsRemoved(Set<CardTerminal> removed)
	{
		for(CardTerminal terminal:removed)
		{
			synchronized(cardEventsListeners)
			{
				for(CardEventsListener listener:cardEventsListeners)
					listener.cardRemoved(terminal);
			}

		}
	}

	// Tell listeners about inserted cards. giving them the CardTerminal and a
	// Card object
	// if autoconnect is enabled (the default), the card argument may be
	// automatically
	// filled out, but it may still be null, if the connect failed.
	protected void listenersTerminalsWithCardsInserted(Set<CardTerminal> inserted)
	{
		for(CardTerminal terminal:inserted)
		{
			Card card=null;

			if(autoconnect)
			{
				try
				{
					card=terminal.connect("*");
				}
				catch(CardException cex)
				{
					listenersException(cex);
				}
			}
			synchronized(cardEventsListeners)
			{
				for(CardEventsListener listener:cardEventsListeners)
					listener.cardInserted(terminal,card);
			}
		}
	}

	/*
	 * Tell listeners about exceptions
	 */
	private void listenersException(Throwable throwable)
	{
		synchronized(cardTerminalEventsListeners)
		{
			for(CardTerminalEventsListener listener:cardTerminalEventsListeners)
				listener.terminalException(throwable);
		}
	}

	private void updateTerminalsPresent() throws CardException
	{
		terminalsPresent=new HashSet<CardTerminal>(cardTerminals.list(State.ALL));
		terminalsWithCards=terminalsWithCardsIn(terminalsPresent);
	}

	private void sleepForDelay() throws InterruptedException
	{
		Thread.sleep(delay);
	}

	private void logCardException(CardException cex,String where)
	{
		this.logger.debug(where+": "+cex.getMessage());
		this.logger.debug("no card readers connected?");
		Throwable cause=cex.getCause();
		if(cause==null)
			return;
		this.logger.debug("cause: "+cause.getMessage());
		this.logger.debug("cause type: "+cause.getClass().getName());
	}
}
