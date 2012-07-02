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
 * Manual exercise for BeIDCardManager.
 * Prints events and list of readers with eid cards.
 * [short readername] ... 
 * 
 * @author Frank Marien
 * 
 */

package test.integ.be.fedict.commons.eid.client;
import java.io.IOException;
import java.math.BigInteger;

import javax.smartcardio.Card;
import javax.smartcardio.CardException;
import javax.smartcardio.CardTerminal;

import org.junit.Test;

import be.fedict.commons.eid.client.BeIDCard;
import be.fedict.commons.eid.client.BeIDCardEventsListener;
import be.fedict.commons.eid.client.BeIDCardEventsManager;
import be.fedict.commons.eid.client.BeIDFileType;
import be.fedict.commons.eid.client.CardEventsListener;
import be.fedict.commons.eid.consumer.Identity;
import be.fedict.commons.eid.consumer.tlv.TlvParser;


public class BeIDCardEventsManagerExercise implements BeIDCardEventsListener, CardEventsListener
{
	private BeIDCardEventsManager beIDCardManager;
	
	
	//---------------------------------------------------------------------------------------------
	
	@Test
	public void testAsynchronous() throws Exception
	{
		beIDCardManager=new BeIDCardEventsManager(new TestLogger());
		beIDCardManager.addBeIDCardEventListener(this);
		beIDCardManager.addOtherCardEventListener(this);
		beIDCardManager.start();
		
		System.err.println("main thread running.. do some card tricks..");
		
		for(;;)
		{
			for(CardTerminal terminal : beIDCardManager.getTerminalsWithBeIDCardsPresent())
				System.out.print("[" + terminal.getName() + "]");
			System.out.println(".");
			Thread.sleep(500);
		}
	}
	
	//----------------------------- callbacks that just print to stderr -------------------
	
	@Override
	public void eIDCardInserted(CardTerminal cardTerminal, BeIDCard card)
	{
		try
		{
			byte[] identityFile=card.readFile(BeIDFileType.Identity);
			Identity identity=TlvParser.parse(identityFile, Identity.class);
			System.out.println(identity.firstName + " " + identity.name);
		}
		catch (CardException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		catch (IOException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		System.err.println("eID Card Inserted Into [" + StringUtils.getShortTerminalname(cardTerminal.getName()) + "]");
		//StringUtils.printTerminalOverviewLine(beIDCardManager);
	}

	@Override
	public void eIDCardRemoved(CardTerminal cardTerminal, BeIDCard card)
	{
		System.err.println("eID Card Removed From [" + StringUtils.getShortTerminalname(cardTerminal.getName()) + "]");	
	}

	@Override
	public void cardInserted(CardTerminal cardTerminal,Card card)
	{
		if(card!=null)
			System.out.println("Other Card [" + String.format("%x",new BigInteger(1,card.getATR().getBytes())) + "] Inserted Into Terminal [" + cardTerminal.getName() + "]");
		else
			System.out.println("Other Card Inserted Into Terminal [" + cardTerminal.getName() + "] but failed to connect()");
	}

	@Override
	public void cardRemoved(CardTerminal cardTerminal)
	{
		System.out.println("Other Card Removed From [" + cardTerminal.getName() + "]");	
	}
}
