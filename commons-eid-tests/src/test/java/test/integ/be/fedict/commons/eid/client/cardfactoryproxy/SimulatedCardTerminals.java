package test.integ.be.fedict.commons.eid.client.cardfactoryproxy;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import javax.smartcardio.CardException;
import javax.smartcardio.CardTerminal;
import javax.smartcardio.CardTerminals;

public class SimulatedCardTerminals extends CardTerminals
{
	private Set<SimulatedCardTerminal> 	terminals;
	private final Semaphore changed = new Semaphore(1, false);
	
	public SimulatedCardTerminals()
	{
		terminals=new HashSet<SimulatedCardTerminal>();
	}

	public synchronized SimulatedCardTerminals attachCardTerminal(SimulatedCardTerminal terminal)
	{
		terminal.setTerminals(this);
		terminals.add(terminal);
		changed.release();
		return this;
	}

	public synchronized SimulatedCardTerminals detachCardTerminal(SimulatedCardTerminal terminal)
	{
		terminal.setTerminals(null);
		terminals.remove(terminal);
		changed.release();
		return this;
	}
	
	public SimulatedCardTerminals propagateCardEvent()
	{
		changed.release();
		return this;
	}

	@Override
	public synchronized List<CardTerminal> list(State state) throws CardException
	{
		switch(state)
		{
			case ALL: 
				return Collections.unmodifiableList(new ArrayList<CardTerminal>(terminals));
		
			case CARD_PRESENT:
			{
				ArrayList<CardTerminal> presentList=new ArrayList<CardTerminal>();
				for(CardTerminal terminal : terminals)
				{
					if(terminal.isCardPresent())
						presentList.add(terminal);
				}
				return Collections.unmodifiableList(presentList);
			}
			
			case CARD_ABSENT:
			{
				ArrayList<CardTerminal> absentList=new ArrayList<CardTerminal>();
				for(CardTerminal terminal : terminals)
				{
					if(!terminal.isCardPresent())
						absentList.add(terminal);
				}
				return Collections.unmodifiableList(absentList);
			}
			
			default: 
				throw new CardException("list with CARD_INSERTION or CARD_REMOVAL not supported in SimulatedCardTerminals");
		
		}
	}

	@Override
	public boolean waitForChange(long timeout) throws CardException
	{
		try
		{
			return changed.tryAcquire(timeout,TimeUnit.MILLISECONDS);
		}
		catch(InterruptedException iex)
		{
			throw new CardException("Interrupted Waiting For Change",iex);
		}
	}
}