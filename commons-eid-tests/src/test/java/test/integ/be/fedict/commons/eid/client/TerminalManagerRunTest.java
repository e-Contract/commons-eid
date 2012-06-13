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
	 * Exercises synchronous run with callbacks:
	 * instantiate, register listeners, call run() (which blocks)
	 */
	@Test
	public void testSyncRun() throws Exception
	{
		terminalManager=new TerminalManager(new TestLogger());
		terminalManager.addListener(this);
		terminalManager.run();
	}
	
	/*
	 * Exercises asynchronous run with callbacks:
	 * instantiate, register listeners, call start().
	 * The test then loops and prints some messages to avoid
	 * the terminalManager going out of scope when the test ends
	 * (TerminalManagers are Daemon Threads when used like this.. The test would end
	 * before anything was detected otherwise)
	 */
	@Test
	public void testASyncRun() throws Exception
	{
		terminalManager=new TerminalManager(new TestLogger());
		terminalManager.addListener(this);
		terminalManager.start();
		
		for(int i=0;i<20;i++)
		{
			System.err.println("main thread spending time.. do some card tricks..");
			Thread.sleep(2000);
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
		System.err.println("Card [" + new String(StringUtils.byteArrayToHexString(card.getATR().getBytes())) + "] Inserted Into Terminal [" + StringUtils.getShortTerminalname(cardTerminal.getName()) + "]");
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
