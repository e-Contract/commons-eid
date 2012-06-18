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
 * Manual exercise for TerminalManager.
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

import be.fedict.commons.eid.client.TerminalManager;
import be.fedict.commons.eid.client.TerminalManagerListener;


public class TerminalManagerRunTest implements TerminalManagerListener
{
	private TerminalManager terminalManager;
	
	/*
	 * Exercises terminalManager procedural call:
	 * instantiate, then call getTerminalsPresent and/or getTerminalsWithCards
	 */
	@Test
	public void testProcedural() throws Exception
	{
		TerminalManager terminalManager=new TerminalManager(new TestLogger());
		
		System.out.println("Terminals Connected:");
		Set<CardTerminal> terminals=terminalManager.getTerminalsPresent();
		for(CardTerminal terminal : terminals)
			System.out.println("\t" + StringUtils.getShortTerminalname(terminal.getName()));
		System.out.println();
		
		System.out.println("Terminals With Cards:");
		Set<CardTerminal> terminalsWithCards=terminalManager.getTerminalsWithCards();
		for(CardTerminal terminal : terminalsWithCards)
			System.out.println("\t" + StringUtils.getShortTerminalname(terminal.getName()));
	}
	
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
		terminalManager=new TerminalManager(new TestLogger());
		terminalManager.addListener(this);
		terminalManager.start();
		
		TerminalManagerListener dummy=new TerminalManagerListener()
		{
			@Override
			public void terminalException(Throwable throwable) 				{}
			@Override
			public void terminalDetached(CardTerminal cardTerminal) 		{}
			@Override
			public void terminalAttached(CardTerminal cardTerminal) 		{}
			@Override
			public void cardRemoved(CardTerminal cardTerminal) 				{}
			@Override
			public void cardInserted(CardTerminal cardTerminal, Card card) 	{}
		};
		
		System.err.println("main thread running.. do some card tricks..");
		
		for(;;)
		{
			System.err.print("+");
			terminalManager.addListener(dummy);
			Thread.sleep(random.nextInt(100));
			System.err.print("-");
			terminalManager.removeListener(dummy);
			Thread.sleep(random.nextInt(100));
		}
	}
	
	@Override
	public void terminalAttached(CardTerminal terminalAttached)
	{
		System.err.println("Terminal Attached [" + StringUtils.getShortTerminalname(terminalAttached.getName()) + "]");
		StringUtils.printTerminalOverviewLine(terminalManager);
	}
	
	@Override
	public void terminalDetached(CardTerminal terminalDetached)
	{
		System.err.println("Terminal Detached [" + StringUtils.getShortTerminalname(terminalDetached.getName()) + "]");
		StringUtils.printTerminalOverviewLine(terminalManager);
	}

	@Override
	public void cardInserted(CardTerminal cardTerminal, Card card)
	{
		if(card!=null)
			System.err.println("Card [" + new String(StringUtils.byteArrayToHexString(card.getATR().getBytes())) + "] Inserted Into Terminal [" + StringUtils.getShortTerminalname(cardTerminal.getName()) + "]");
		else
			System.err.println("Card present but failed to connect()");
		StringUtils.printTerminalOverviewLine(terminalManager);
	}
	
	@Override
	public void cardRemoved(CardTerminal terminalWithCardRemoved)
	{
		System.err.println("Card Removed From [" + StringUtils.getShortTerminalname(terminalWithCardRemoved.getName()) + "]");
		StringUtils.printTerminalOverviewLine(terminalManager);
	}

	@Override
	public void terminalException(Throwable throwable)
	{
		System.err.println("Exception: " + throwable.getLocalizedMessage());
	}
	
	//---------------------------------------------------------------------------------------------
}
