package test.integ.be.fedict.commons.eid.client.cardfactoryproxy;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import javax.smartcardio.Card;
import javax.smartcardio.CardException;
import javax.smartcardio.CardTerminal;

public class SimulatedCardTerminal extends CardTerminal
{
	private 		String 					name;
	private 		SimulatedCard			card;
	private final 	Semaphore 				changed;
	private 		SimulatedCardTerminals	terminals;
	
	//---------------------------------------------------------
	
	public SimulatedCardTerminal(String name)
	{
		super();
		this.name=name;
		changed=new Semaphore(1, false);
	}
	
	public void insertCard(SimulatedCard card)
	{
		if(this.card!=null)
			throw new RuntimeException("Can't Insert 2 Cards in one Card Reader");
		this.card=card;
		changed.release();
		if(terminals!=null)
			terminals.propagateCardEvent();
	}

	public void removeCard()
	{
		if(this.card==null)
			throw new RuntimeException("Can't Remove Card From Empty Reader");
		this.card=null;
		changed.release();
		if(terminals!=null)
			terminals.propagateCardEvent();
	}
	
	//-----------------------------------------------------------
	
	@Override
	public Card connect(String protocol) throws CardException
	{
		if(!isCardPresent())
			throw new CardException("No Card Present");
		return card;
	}

	@Override
	public String getName()
	{
		return name;
	}

	@Override
	public boolean isCardPresent() throws CardException
	{
		return card!=null;
	}

	@Override
	public boolean waitForCardAbsent(long timeout) throws CardException
	{
		return waitForCardState(false,timeout);
	}

	@Override
	public boolean waitForCardPresent(long timeout) throws CardException
	{
		return waitForCardState(true,timeout);
	}
	
	private synchronized boolean waitForCardState(boolean state, long timeout) throws CardException
	{
		if(isCardPresent()==state)
			return true;
	
		try
		{
			return changed.tryAcquire(timeout,TimeUnit.MILLISECONDS);
		}
		catch(InterruptedException iex)
		{
			throw new CardException("Interrupted Waiting For Card " + (state?"Presence":"Absence"),iex);
		}
	}

	public void setTerminals(SimulatedCardTerminals terminals)
	{
		this.terminals = terminals;
	}
}
