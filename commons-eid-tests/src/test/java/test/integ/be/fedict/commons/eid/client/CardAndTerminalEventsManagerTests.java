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
import java.util.HashSet;
import java.util.Random;
import java.util.Set;
import javax.smartcardio.ATR;
import javax.smartcardio.Card;
import javax.smartcardio.CardTerminal;
import org.junit.Before;
import org.junit.Test;
import test.integ.be.fedict.commons.eid.client.cardfactoryproxy.SimulatedCard;
import test.integ.be.fedict.commons.eid.client.cardfactoryproxy.SimulatedCardTerminal;
import test.integ.be.fedict.commons.eid.client.cardfactoryproxy.SimulatedCardTerminals;
import be.fedict.commons.eid.client.CardAndTerminalEventsManager;
import be.fedict.commons.eid.client.CardEventsListener;
import be.fedict.commons.eid.client.CardTerminalEventsListener;
import static org.junit.Assert.*;


public class CardAndTerminalEventsManagerTests implements CardTerminalEventsListener,CardEventsListener
{
	private SimulatedCard[] 		simulatedCard;
	private SimulatedCardTerminal[]	simulatedCardTerminal;
	private SimulatedCardTerminals	simulatedCardTerminals;
	
	@Before
	public void setUp()
	{
		simulatedCard=new SimulatedCard[8];
		for(int i=0;i<simulatedCard.length;i++)
			simulatedCard[i]=new SimulatedCard(new ATR(new byte[]{0x3b,(byte)0x98,(byte)i,0x40,(byte)i,(byte)i,(byte)i,(byte)i,0x01,0x01,(byte)0xad,0x13,0x10}));
		simulatedCardTerminal=new SimulatedCardTerminal[8];
		for(int i=0;i<simulatedCardTerminal.length;i++)
			simulatedCardTerminal[i]=new SimulatedCardTerminal("Fedix SCR " + i);
		simulatedCardTerminals=new SimulatedCardTerminals();
	}
	
	/*
	 * Tests cardAndTerminalEventsManager procedural call:
	 * instantiate, then call getTerminalsPresent and/or getTerminalsWithCards
	 */
	@Test
	public void testSynchronous() throws Exception
	{
		CardAndTerminalEventsManager cardAndTerminalEventsManager=new CardAndTerminalEventsManager(new TestLogger(),simulatedCardTerminals);

		for(int terminal=0;terminal<simulatedCardTerminal.length;terminal++)
		{
			simulatedCardTerminals.attachCardTerminal(simulatedCardTerminal[terminal]);
						
			for(int card=0;card<simulatedCard.length;card++)
			{
				simulatedCardTerminal[terminal].insertCard(simulatedCard[card]);
				StringUtils.printTerminalAndCardOverviewLine(cardAndTerminalEventsManager);
				simulatedCardTerminal[terminal].removeCard();
			}
			
			simulatedCardTerminals.detachCardTerminal(simulatedCardTerminal[terminal]);
		}
	}
	
	//---------------------------------------------------------------------------------------------

	private class RecordKeepingCardTerminalEventsListener implements CardTerminalEventsListener
	{
		private Set<CardTerminal> recordedState;
		
		public RecordKeepingCardTerminalEventsListener()
		{
			super();
			this.recordedState=new HashSet<CardTerminal>();
		}

		@Override
		public void terminalAttached(CardTerminal cardTerminal)
		{
			recordedState.add(cardTerminal);
			
		}

		@Override
		public void terminalDetached(CardTerminal cardTerminal)
		{
			recordedState.remove(cardTerminal);
			
		}

		@Override
		public void terminalException(Throwable throwable)
		{
		}

		public Set<CardTerminal> getRecordedState()
		{
			return recordedState;
		}
	}
	
	@Test
	public void testTerminalAttachDetachDetection() throws Exception
	{
		Random random=new Random(0);
		Set<CardTerminal> expectedState=new HashSet<CardTerminal>();
		CardAndTerminalEventsManager cardAndTerminalEventsManager=new CardAndTerminalEventsManager(new TestLogger(),simulatedCardTerminals);
		RecordKeepingCardTerminalEventsListener recorder=new RecordKeepingCardTerminalEventsListener();
		cardAndTerminalEventsManager.addCardTerminalListener(recorder);
		cardAndTerminalEventsManager.start();
		
		System.err.println("attaching and detaching some simulated cardterminals");
		
		boolean[] attached=new boolean[simulatedCardTerminal.length];
		
		for(int i=0;i<100000;i++)
		{
			int terminalToAttach=random.nextInt(simulatedCardTerminal.length);
			if(!attached[terminalToAttach])
			{
				System.out.println("attached [" + simulatedCardTerminal[terminalToAttach].getName() + "]");
				expectedState.add(simulatedCardTerminal[terminalToAttach]);
				simulatedCardTerminals.attachCardTerminal(simulatedCardTerminal[terminalToAttach]);
				attached[terminalToAttach]=true;
			}
			
			int terminalToDetach=random.nextInt(simulatedCardTerminal.length);
			if(attached[terminalToDetach])
			{
				System.out.println("detached [" + simulatedCardTerminal[terminalToDetach].getName() + "]");
				expectedState.remove(simulatedCardTerminal[terminalToDetach]);
				simulatedCardTerminals.detachCardTerminal(simulatedCardTerminal[terminalToDetach]);
				attached[terminalToAttach]=false;
			}
		}
		
		
		assertEquals(expectedState,recorder.getRecordedState());
	}
	
//	@Test
//	public void testCardInsertRemoveDetection() throws Exception
//	{
//		Random random=new Random(0);
//		Set<Card> expectedState=new HashSet<Card>();
//		CardAndTerminalEventsManager cardAndTerminalEventsManager=new CardAndTerminalEventsManager(new TestLogger(),simulatedCardTerminals);
//		RecordKeepingCardTerminalEventsListener recorder=new RecordKeepingCardTerminalEventsListener();
//		cardAndTerminalEventsManager.addCardTerminalListener(recorder);
//		cardAndTerminalEventsManager.start();
//		
//		simulatedCardTerminals.attachCardTerminal(simulatedCardTerminal[0]);
//		
//		System.err.println("attaching and detaching some simulated cardterminals");
//		
//		boolean[] inserted=new boolean[simulatedCard.length];
//		
//		for(int i=0;i<100000;i++)
//		{
//			int cardToInsert=random.nextInt(simulatedCard.length);
//			if(!inserted[cardToInsert])
//			{
//				System.out.println("inserted [" + StringUtils.byteArrayToHexString(simulatedCard[cardToInsert].getATR().getBytes()) + "]");
//				expectedState.add(simulatedCard[cardToInsert]);
//				simulatedCardTerminal[0].insertCard(simulatedCard[cardToInsert]);
//				inserted[cardToInsert]=true;
//			}
//			
//			int cardToRemove=random.nextInt(simulatedCard.length);
//			if(inserted[cardToRemove])
//			{
//				System.out.println("removed [" + StringUtils.byteArrayToHexString(simulatedCard[cardToInsert].getATR().getBytes()) + "]");
//				expectedState.remove(simulatedCardTerminal[cardToRemove]);
//				simulatedCardTerminal[0].removeCard(simulatedCard[cardToRemove]);
//				inserted[cardToRemove]=false;
//			}
//		}
//		
//		
//		assertEquals(expectedState,recorder.getRecordedState());
//	}
	
	//----------------------------- callbacks that just print to stderr -------------------
	
	@Override
	public void terminalAttached(CardTerminal terminalAttached)
	{
		System.err.println("Terminal Attached [" + terminalAttached.getName() + "]");
	}
	
	@Override
	public void terminalDetached(CardTerminal terminalDetached)
	{
		System.err.println("Terminal Detached [" + terminalDetached.getName() + "]");
	}

	@Override
	public void cardInserted(CardTerminal cardTerminal, Card card)
	{
		if(card!=null)
			System.err.println("Card [" + new String(StringUtils.byteArrayToHexString(card.getATR().getBytes())) + "] Inserted Into Terminal [" + cardTerminal.getName() + "]");
		else
			System.err.println("Card present but failed to connect()");
	}
	
	@Override
	public void cardRemoved(CardTerminal terminalWithCardRemoved)
	{
		System.err.println("Card Removed From [" + terminalWithCardRemoved.getName() + "]");
	}

	@Override
	public void terminalException(Throwable throwable)
	{
		System.err.println("Exception: " + throwable.getLocalizedMessage());
	}
}
