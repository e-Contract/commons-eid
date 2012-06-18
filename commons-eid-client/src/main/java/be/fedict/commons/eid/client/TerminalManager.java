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
 * A TerminalManager connects to the CardTerminal PCSC subsystem, and maintains
 * state information on any CardTerminals attached and cards inserted
 * Register a TerminalManagerListener to get callbacks for reader attach/detach
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


public class TerminalManager implements Runnable
{		
	private static final int 				DEFAULT_DELAY=250;
	
	private boolean							running,initialized,autoconnect;
	private Thread							thread;
	private TerminalFactory 				terminalFactory;
	private CardTerminals					cardTerminals;
	private Set<CardTerminal>				terminalsPresent,terminalsWithCards;
	private Set<TerminalManagerListener>	listeners;
	private int								delay;
	private Logger 							logger;
	
	//----- various constructors ------
	
	public TerminalManager()
	{
		this(DEFAULT_DELAY,new VoidLogger());
	}
	
	public TerminalManager(Logger logger)
	{
		this(DEFAULT_DELAY,logger);
	}
	
	public TerminalManager(int delay)
	{
		this(delay,new VoidLogger());
	}
	
	public TerminalManager(int delay, Logger logger)
	{
		LibJ2PCSCLinuxFix.fixLinuxNativeLibrary();
		this.listeners					=new HashSet<TerminalManagerListener>();
		this.delay						=delay;
		this.logger						=logger;
		this.running					=false;
		this.initialized				=false;
		this.autoconnect				=true;
		this.terminalFactory=TerminalFactory.getDefault();
		this.cardTerminals=terminalFactory.terminals();
		clear();	
	}
	
	// add a TerminalManagerListener
	public TerminalManager addListener(TerminalManagerListener listener)
	{
		synchronized(listeners) { listeners.add(listener); }
		return this;
	}
	
	// start this TerminalManager in the background as a Thread
	public TerminalManager start()
	{
		thread=new Thread(this,"TerminalManager");
		thread.setDaemon(true);
		thread.start();
		return this;
	}
	
	// remove a TerminalManagerListener
	public TerminalManager removeListener(TerminalManagerListener listener)
	{
		synchronized(listeners) { listeners.remove(listener); }
		return this;
	}
	
	// stop this Terminalmanager running in the background as a Thread
	// interrupts any Thread.sleep() or CardTerminal.waitForChange() calls in progress to avoid having
	// to wait for potentially long <delay> settings
	public TerminalManager stop()
	{
		running=false;
		thread.interrupt();
		return this;
	}
	
	@Override
	public void run()
	{
		running=true;
		
		try
		{
			while(running)
				handlePCSCEvents();
		}
		catch (InterruptedException ie)
		{
			if(running)
				logger.error("Event Loop Unexpectedly Interrupted: " + ie.getLocalizedMessage());		
		}
	}

	private void handlePCSCEvents() throws InterruptedException
	{
		if(terminalsPresent==null || terminalsWithCards==null)
		{
			try
			{
				if(terminalsPresent==null)
					updateTerminalsPresent();
				if(terminalsWithCards==null)
					updateTerminalsWithCards();
				listenersTerminalsAttachedCardsInserted(terminalsPresent,terminalsWithCards);	
				initialized=true;
			}
			catch(CardException cex)
			{
				logger.debug("Cannot enumerate card terminals [1] (No Card Readers Connected?): " + cex.getLocalizedMessage());
				clear();
				listenersException(cex);
				return;
			}
		}
		
		try
		{
			// can't use waitForChange properly, that is in blocking mode, without delay argument, 
			// since it sometimes misses reader attach events.. (TODO: test on other platforms)
			// this limits us to what is basically a polling strategy, with a small speed
			// gain where waitForChange *does* detect events (because it will return faster than delay)
			// for most events this will make reaction instantaneous, and worst case = delay
			cardTerminals.waitForChange(delay);
		}
		catch(CardException cex)
		{
			// waitForChange fails (e.g. PCSC is there but no readers)
			logger.debug("Cannot wait for card terminal events [2] (No Card Readers Connected?): " + cex.getLocalizedMessage());
			listenersException(cex);
			clear();
			sleepForDelay();
			return;
		}
		catch(IllegalStateException ise)
		{
			// waitForChange fails (e.g. PCSC is not there)
			logger.debug("Cannot wait for card terminal changes (no PCSC subsystem?): " + ise.getLocalizedMessage());
			listenersException(ise);
			clear();
			sleepForDelay();
			return;
		}
		
		// get here when even has occured or delay time has passed
		
		try
		{
			// get fresh state
			Set<CardTerminal> currentTerminals=new HashSet<CardTerminal>(cardTerminals.list(State.ALL));
			Set<CardTerminal> currentTerminalsWithCards=new HashSet<CardTerminal>(cardTerminals.list(State.CARD_PRESENT));
			
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
			
			// keep fresh state to compare to next time
			this.terminalsPresent=currentTerminals;	
			this.terminalsWithCards=currentTerminalsWithCards;
			
			// advise the listeners where appropriate, always in the order attach, insert, remove, detach
			listenersUpdateInSequence(terminalsAttached, terminalsWithCardsInserted, terminalsWithCardsRemoved, terminalsDetached);
		}
		catch(CardException cex)
		{
			// if a CardException occurs, assume we're out of readers (only CardTerminals.list throws that here) 
			// CardTerminal fails in that case, instead of simply seeing zero CardTerminals.
			logger.debug("Cannot enumerate card terminals [3] (No Card Readers Connected?): " + cex.getLocalizedMessage());
			clear();
			sleepForDelay();
		}
	}

	
	//-------------------------------------
	//--------- getters/setters -----------
	//-------------------------------------
	
	
	// get polling/retry delay currently in use
	public int getDelay()
	{
		return delay;
	}

	// set polling/retry delay.
	public TerminalManager setDelay(int delay)
	{
		this.delay=delay;
		return this;
	}
	
	
	public boolean isAutoconnect()
	{
		return autoconnect;
	}

	public TerminalManager setAutoconnect(boolean autoconnect)
	{
		this.autoconnect = autoconnect;
		return this;
	}

	/*
	 * getTerminalspresent returns a Set of Terminals connected at time of call, when not running,
	 * or at last event, when running.
	 */
	public Set<CardTerminal> getTerminalsPresent() throws CardException
	{
		if(running)
		{
			// we're running, terminalsPresent is automatically updated.
			// but we're then also using it internally, so return a copy for safety
			return new HashSet<CardTerminal>(this.terminalsPresent);
		}
		else
		{
			// we're not running, the lists are not automatically maintained
			// so update the list now, but return the internal value since we're not
			// using it ourselves.
			updateTerminalsPresent();
			return this.terminalsPresent;
		}
		
	}
	
	/*
	 * getTerminalsWithCards returns a Set of Terminals connected and with a card inserted, at time of call, when not running,
	 * or at last event, when running.
	 */
	public Set<CardTerminal> getTerminalsWithCards() throws CardException
	{
		if(running)
		{
			// we're running, terminalsWithCards is automatically updated.
			// but we're then also using it internally, so return a copy for safety
			return new HashSet<CardTerminal>(this.terminalsWithCards);
		}
		else
		{
			// we're not running, the lists are not automatically maintained
			// so update the list now, but return the internal value since we're not
			// using it ourselves.
			updateTerminalsWithCards();
			return this.terminalsWithCards;
		}	
	}
	
	//-------------------------------------------------
	//--------- private convenience methods -----------
	//-------------------------------------------------
	
	// return to the uninitialized state
	private void clear()
	{
		// if we were already intialized, we may have sent attached and insert events
		// we now pretend to remove and detach all that we know of, for consistency
		if(initialized)
			listenersCardsRemovedTerminalsDetached(terminalsWithCards, terminalsPresent);
		terminalsPresent=null;
		terminalsWithCards=null;
		initialized=false;
	}
	
	private void listenersTerminalsAttachedCardsInserted(Set<CardTerminal> attached, Set<CardTerminal> inserted) throws CardException
	{
		synchronized (listeners)
		{
			listenersTerminalsAttached(attached);
			listenersTerminalsWithCardsInserted(inserted);	
		}
	}
	
	private void listenersCardsRemovedTerminalsDetached(Set<CardTerminal> removed, Set<CardTerminal> detached)
	{
		synchronized (listeners)
		{
			listenersTerminalsWithCardsRemoved(removed);
			listenersTerminalsDetached(detached);
		}
	}
	
	private void listenersUpdateInSequence(Set<CardTerminal> attached, Set<CardTerminal> inserted, Set<CardTerminal> removed, Set<CardTerminal> detached) throws CardException
	{
		synchronized (listeners)
		{
			listenersTerminalsAttached(attached);
			listenersTerminalsWithCardsInserted(inserted);
			listenersTerminalsWithCardsRemoved(removed);
			listenersTerminalsDetached(detached);
		}
	}

	// Tell listeners about attached readers
	private void listenersTerminalsAttached(Set<CardTerminal> attached)
	{
		for(CardTerminal terminal : attached)
		{
			for(TerminalManagerListener listener: listeners)
				listener.terminalAttached(terminal);
		}
	}
	
	// Tell listeners about detached readers
	private void listenersTerminalsDetached(Set<CardTerminal> detached)
	{
		for(CardTerminal terminal : detached)
		{
			for(TerminalManagerListener listener: listeners)
				listener.terminalDetached(terminal);
		}
	}
	
	// Tell listeners about removed cards
	private void listenersTerminalsWithCardsRemoved(Set<CardTerminal> removed) 
	{
		for(CardTerminal terminalWithCardRemoved : removed)
		{
			for(TerminalManagerListener listener: listeners)
				listener.cardRemoved(terminalWithCardRemoved);
		}
	}

	// Tell listeners about inserted cards. giving them the CardTerminal and a Card object
	// if autoconnect is enabled (the default), the card argument may be automatically
	// filled out, but it may still be null, if the connect failed.
	private void listenersTerminalsWithCardsInserted(Set<CardTerminal> inserted)
	{
		for(CardTerminal terminal : inserted)
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
			
			for(TerminalManagerListener listener: listeners)
				listener.cardInserted(terminal,card);
		}
	}
	
	/*
	 *  Tell listeners about exceptions
	 */
	private void listenersException(Throwable throwable)
	{
		synchronized(listeners)
		{
			for(TerminalManagerListener listener: listeners)
				listener.terminalException(throwable);
		}
	}
	
	private void updateTerminalsPresent() throws CardException
	{
		terminalsPresent=new HashSet<CardTerminal>(cardTerminals.list(State.ALL));
	}
	
	private void updateTerminalsWithCards() throws CardException
	{
		terminalsWithCards=new HashSet<CardTerminal>(cardTerminals.list(State.CARD_PRESENT));
	}
	
	private void sleepForDelay() throws InterruptedException
	{
		Thread.sleep(delay);
	}
}
