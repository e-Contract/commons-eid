package be.fedict.commons.eid.examples.events;

import java.util.Set;
import javax.smartcardio.CardTerminal;
import be.fedict.commons.eid.client.BeIDCard;
import be.fedict.commons.eid.client.BeIDCardEventsListener;
import be.fedict.commons.eid.client.BeIDCardEventsManager;

public class BeIDCardEventsExamples
{
	/*
	 * get information about BeID cards currently inserted, from the current thread:
	 */
	public void demonstrate_basic_synchronous_usage()
	{
		//-------------------------------------------------------------------------------------------------------
		// instantiate a BeIDCardManager with default settings (no logging, private CardAndTerminalEventsManager)
		//-------------------------------------------------------------------------------------------------------
		BeIDCardEventsManager beIDCardManager=new BeIDCardEventsManager();
		
		//-------------------------------------------------------------------------------------------------------
		// ask it for all CardTerminals that currently contain BeID cards
		//-------------------------------------------------------------------------------------------------------
		Set<CardTerminal> terminalsWithBeIDCards=beIDCardManager.getTerminalsWithBeIDCardsPresent();
		
		//-------------------------------------------------------------------------------------------------------
		// either say there are none, or if there are, list them
		//-------------------------------------------------------------------------------------------------------
		if(terminalsWithBeIDCards.isEmpty())
		{
			System.out.println("No Terminals With BeID Cards Found");
		}
		else
		{
			System.out.println("Terminals With BeID Cards:");
			
			for(CardTerminal terminal : terminalsWithBeIDCards)
			{
				System.out.println("-" + terminal.getName());
			}
		}
	}
	
	/*
	 * get information about BeID cards being inserted and removed, while doing something else:
	 */
	public BeIDCardEventsExamples demonstrate_basic_asynchronous_usage() throws InterruptedException
	{
		//-------------------------------------------------------------------------------------------------------
		// instantiate a BeIDCardManager with default settings (no logging, private CardAndTerminalEventsManager)
		//-------------------------------------------------------------------------------------------------------
		BeIDCardEventsManager beIDCardManager=new BeIDCardEventsManager();
		
		//-------------------------------------------------------------------------------------------------------	
		// register a BeIDCardManagerListener
		//-------------------------------------------------------------------------------------------------------
		beIDCardManager.addBeIDCardEventListener(new BeIDCardEventsListener()
		{
			@Override
			public void eIDCardRemoved(CardTerminal cardTerminal,BeIDCard card)
			{
				System.out.println("BeID Card Removed From Card Termimal [" + cardTerminal.getName() + "]\n");
			}
			
			@Override
			public void eIDCardInserted(CardTerminal cardTerminal,BeIDCard card)
			{
				System.out.println("BeID Card Inserted Into Card Termimal [" + cardTerminal.getName() + "]\n");	
			}
		});
			
		//-------------------------------------------------------------------------------------------------------
		// start the BeIDCardManager instance.
		//-------------------------------------------------------------------------------------------------------
		beIDCardManager.start();
		
		//-------------------------------------------------------------------------------------------------------
		// the main thread goes off and does other things (for this example, just loop and sleep)
		//-------------------------------------------------------------------------------------------------------
		System.err.println("Now.. insert and remove some BeID cards..");
		for(;;)
			Thread.sleep(2000);
	}
	
	//-------------------------------------------------------------------------------------------------------
	
	public static void main(String[] args) throws InterruptedException
	{
		BeIDCardEventsExamples examples=new BeIDCardEventsExamples();
		examples.demonstrate_basic_synchronous_usage();
		examples.demonstrate_basic_asynchronous_usage();
	}
}
