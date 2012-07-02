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
 * Manual exercise for CardAndTerminalEventsManager.
 * Prints events and list of readers with cards.
 * [short readername] ... 
 * readers with cards inserted have a "*" behind their short name
 * 
 * @author Frank Marien
 * 
 */

package test.integ.be.fedict.commons.eid.client;
import java.util.Random;
import java.util.Set;
import javax.smartcardio.Card;
import javax.smartcardio.CardTerminal;
import org.junit.Test;
import be.fedict.commons.eid.client.CardAndTerminalEventsManager;
import be.fedict.commons.eid.client.CardEventsListener;
import be.fedict.commons.eid.client.CardTerminalEventsListener;


public class CardAndTerminalEventsManagerExercises implements CardTerminalEventsListener,CardEventsListener
{
	private CardAndTerminalEventsManager cardAndTerminalEventsManager;
	
	/*
	 * Exercises cardAndTerminalEventsManager procedural call:
	 * instantiate, then call getTerminalsPresent and/or getTerminalsWithCards
	 */
	@Test
	public void testSynchronous() throws Exception
	{
		CardAndTerminalEventsManager cardAndTerminalEventsManager=new CardAndTerminalEventsManager(new TestLogger());
		
		System.out.println("Terminals Connected:");
		Set<CardTerminal> terminals=cardAndTerminalEventsManager.getTerminalsPresent();
		for(CardTerminal terminal : terminals)
			System.out.println("\t" + StringUtils.getShortTerminalname(terminal.getName()));
		System.out.println();
		
		System.out.println("Terminals With Cards:");
		Set<CardTerminal> terminalsWithCards=cardAndTerminalEventsManager.getTerminalsWithCards();
		for(CardTerminal terminal : terminalsWithCards)
			System.out.println("\t" + StringUtils.getShortTerminalname(terminal.getName()));
	}
	
	//---------------------------------------------------------------------------------------------
	
	/*
	 * Exercises asynchronous run with callbacks:
	 * instantiate, register listeners, call start().
	 * The test then loops and adds/removes a listener in some pseudo-random timing pattern.
	 * This is to ensure that the list of listeners remains properly synchronized
	 * in relation to it being iterated whenever events are being sent to listeners
	 * this test never returns.. since it requires someone to attach/detach readers and
	 * insert/remove cards this is no problem until we automate those using e.g.
	 * http://www.lynxmotion.com/p-816-al5d-robotic-arm-combo-kit-free-software.aspx
	 * 
	 * While running this test, the operator should attach and detach at least 2 card terminals,
	 * insert and remove cards from them, in all possible permutations. The state displayed should,
	 * at all times, reflect the state of the readers and their cards within 250-400 ms.
	 */

	@Test
	public void testAsynchronous() throws Exception
	{
		Random random=new Random(0);
		cardAndTerminalEventsManager=new CardAndTerminalEventsManager(new TestLogger());
		cardAndTerminalEventsManager.addCardTerminalListener(this);
		cardAndTerminalEventsManager.addCardListener(this);
		
		//cardAndTerminalEventsManager.ignoreCardEventsFor("VASCO DP905");
		
		cardAndTerminalEventsManager.start();
		
		CardTerminalEventsListener dummyCTL=new CardTerminalEventsListener()
		{
			@Override public void terminalException(Throwable throwable) 				{}
			@Override public void terminalDetached(CardTerminal cardTerminal) 			{}
			@Override public void terminalAttached(CardTerminal cardTerminal) 			{}
		};
		
		CardEventsListener dummyCL=new CardEventsListener()
		{
			
			@Override public void cardRemoved(CardTerminal cardTerminal) 				{}
			@Override public void cardInserted(CardTerminal cardTerminal, Card card) 	{}
		};
		
		System.err.println("main thread running.. do some card tricks..");
		
		for(;;)
		{
			System.err.print("+");
			cardAndTerminalEventsManager.addCardTerminalListener(dummyCTL);
			cardAndTerminalEventsManager.addCardListener(dummyCL);
			Thread.sleep(random.nextInt(100));
			System.err.print("-");
			cardAndTerminalEventsManager.removeCardTerminalListener(dummyCTL);
			cardAndTerminalEventsManager.removeCardListener(dummyCL);
			Thread.sleep(random.nextInt(100));
		}
	}
	
	//---------------------------------------------------------------------------------------------
	
	/*
	 * Exercise CardAndTerminalEventsManager's start() stop() semantics, with regards to its
	 * worker thread. This test starts and stops a CardAndTerminalEventsManager randomly.
	 * It should remain in a consistent state at all times and detect terminal
	 * attaches/detaches and card inserts/removals as usual (while running, of course..)
	 */
	@Test
	public void testStartStop() throws Exception
	{
		Random random=new Random(0);
		cardAndTerminalEventsManager=new CardAndTerminalEventsManager(new TestLogger());
		cardAndTerminalEventsManager.addCardTerminalListener(this);
		cardAndTerminalEventsManager.addCardListener(this);
		cardAndTerminalEventsManager.start();
		
		for(;;)
		{
			System.err.print("+");
			cardAndTerminalEventsManager.start();
			Thread.sleep(random.nextInt(2000));
			System.err.print("-");
			cardAndTerminalEventsManager.stop();
			Thread.sleep(random.nextInt(2000));
		}
	}
	
	/*
	 * Exercise CardAndTerminalEventsManager's consistency with synchronous and asynchronous
	 * operations mixed. Async test with random synchronous calls mixed in
	 */
	@Test
	public void testSyncAsyncMix() throws Exception
	{
		Random random=new Random(0);
		cardAndTerminalEventsManager=new CardAndTerminalEventsManager(new TestLogger());
		cardAndTerminalEventsManager.addCardTerminalListener(this);
		cardAndTerminalEventsManager.addCardListener(this);
		cardAndTerminalEventsManager.start();
		
		for(;;)
		{
			if(random.nextBoolean())
			{
				System.err.print("T");
				cardAndTerminalEventsManager.getTerminalsPresent();
			}
			else
			{
				System.err.print("C");
				cardAndTerminalEventsManager.getTerminalsWithCards();
			}
			Thread.sleep(random.nextInt(10));
		}
	}
	
	//----------------------------- callbacks that just print to stderr -------------------
	
	@Override
	public void terminalAttached(CardTerminal terminalAttached)
	{
		System.err.println("Terminal Attached [" + terminalAttached.getName() + "]");
		StringUtils.printTerminalOverviewLine(cardAndTerminalEventsManager);
	}
	
	@Override
	public void terminalDetached(CardTerminal terminalDetached)
	{
		System.err.println("Terminal Detached [" + terminalDetached.getName() + "]");
		StringUtils.printTerminalOverviewLine(cardAndTerminalEventsManager);
	}

	@Override
	public void cardInserted(CardTerminal cardTerminal, Card card)
	{
		if(card!=null)
			System.err.println("Card [" + StringUtils.atrToString(card.getATR()) + "] Inserted Into Terminal [" + cardTerminal.getName() + "]");
		else
			System.err.println("Card present but failed to connect()");
		StringUtils.printTerminalOverviewLine(cardAndTerminalEventsManager);
	}
	
	@Override
	public void cardRemoved(CardTerminal terminalWithCardRemoved)
	{
		System.err.println("Card Removed From [" + terminalWithCardRemoved.getName() + "]");
		StringUtils.printTerminalOverviewLine(cardAndTerminalEventsManager);
	}

	@Override
	public void terminalException(Throwable throwable)
	{
		System.err.println("Exception: " + throwable.getLocalizedMessage());
	}
}
