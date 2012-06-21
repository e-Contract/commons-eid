package be.fedict.commons.eid.client;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.smartcardio.ATR;
import javax.smartcardio.Card;
import javax.smartcardio.CardTerminal;

public class BeIDCardManager implements TerminalManagerListener
{
	private final static byte[] ATR_PATTERN 	= new byte[] {0x3b,(byte)0x98,0x00,0x40,0x00,(byte)0x00,0x00,0x00,0x01,0x01,(byte)0xad,0x13,0x10 };
	private final static byte[] ATR_MASK 		= new byte[] {(byte)0xff,(byte)0xff,0x00,(byte)0xff,0x00,0x00,0x00,0x00,(byte)0xff,(byte)0xff,(byte)0xff,(byte)0xff,(byte)0xf0};

	private TerminalManager 				terminalManager;
	private boolean							terminalManagerIsPrivate;
	private Map<CardTerminal,BeIDCard> 		terminalsAndCards;
	private Set<BeIDCardManagerListener>	listeners;
	private final Logger 					logger;

	/*
	 * a BeIDCardManager with a default (void) logger and a private TerminalManager
	 * that is automatically started and stopped with the BeIDCardManager
	 */
	public BeIDCardManager()
	{
		this(new VoidLogger());
	}
	
	/*
	 * a BeIDCardManager logging to logger, and a private Terminalmanager
	 * that is automatically started and stopped with the BeIDCardManager
	 */
	public BeIDCardManager(Logger logger)
	{
		this(logger, new TerminalManager());
		this.terminalManagerIsPrivate=true;
	}
	
	/*
	 * a BeIDCardManager with a default (void) logger, caller supplies a TerminalManager.
	 * note: caller is responsible for start()in the supplied TerminalManager, it will not
	 * be automatically started!
	 */
	public BeIDCardManager(TerminalManager terminalManager)
	{
		this(new VoidLogger(), terminalManager);
	}
	
	/*
	 * a BeIDCardManager logging to logger, caller supplies a TerminalManager.
	 * note: caller is responsible for start()in the supplied TerminalManager, it will not
	 * be automatically started!
	 */
	public BeIDCardManager(Logger logger, TerminalManager terminalManager) 
	{
		this.logger=logger;
		this.listeners=new HashSet<BeIDCardManagerListener>();
		this.terminalsAndCards=new HashMap<CardTerminal,BeIDCard>();
		this.terminalManager=terminalManager;
		this.terminalManager.addListener(this);
	}
	
	public BeIDCardManager start()
	{
		if(terminalManagerIsPrivate)
			terminalManager.start();
		return this;
	}
	
	// add a BeIDCardManagerListener
	public BeIDCardManager addListener(BeIDCardManagerListener listener)
	{
		synchronized(listeners) { listeners.add(listener); }
		return this;
	}
	
	// remove a BeIDCardManagerListener
	public BeIDCardManager removeListener(BeIDCardManagerListener listener)
	{
		synchronized(listeners) { listeners.remove(listener); }
		return this;
	}
	
	public BeIDCardManager stop()
	{
		if(terminalManagerIsPrivate)
			terminalManager.stop();
		return this;
	}
	
	public Map<CardTerminal,BeIDCard> getTerminalsWithBeIDCards()
	{
		Map<CardTerminal,BeIDCard> copyOfTerminalsAndCards;
		
		synchronized(terminalsAndCards)
		{
			copyOfTerminalsAndCards=new HashMap<CardTerminal,BeIDCard>(terminalsAndCards);
		}
		
		return copyOfTerminalsAndCards;
	}
	
//	public CardTerminal findFirstBeIDCardTerminal() throws CardException
//	{
//		for(CardTerminal terminalWithCard : terminalManager.getTerminalsWithCards())
//		{
//			Card card=terminalWithCard.connect("*");
//		}
//	}
	
	private boolean matchesEidAtr(ATR atr)
	{
		byte[] atrBytes = atr.getBytes();
		if (atrBytes.length != ATR_PATTERN.length)
			return false;
		for (int idx = 0; idx < atrBytes.length; idx++)
			atrBytes[idx] &= ATR_MASK[idx];
		if (Arrays.equals(atrBytes, ATR_PATTERN))
			return true;
		return false;
	}

	@Override
	public void terminalAttached(CardTerminal cardTerminal)
	{
		// TODO Auto-generated method stub
		
	}

	@Override
	public void terminalDetached(CardTerminal cardTerminal)
	{
		// TODO Auto-generated method stub
		
	}

	@Override
	public void cardInserted(CardTerminal cardTerminal, Card card)
	{
		if(card!=null && matchesEidAtr(card.getATR()))
		{
			BeIDCard beIDCard=new BeIDCard(card,logger);
			
			synchronized(terminalsAndCards)
			{
				terminalsAndCards.put(cardTerminal,beIDCard);
			}
			
			synchronized(listeners)
			{
				for(BeIDCardManagerListener listener: listeners)
					listener.eIDCardInserted(cardTerminal,beIDCard);
			}	
		}
	}

	@Override
	public void cardRemoved(CardTerminal cardTerminal)
	{
		BeIDCard beIDCard=terminalsAndCards.get(cardTerminal);
		if(beIDCard!=null)
		{
			synchronized(terminalsAndCards)
			{
				terminalsAndCards.remove(cardTerminal);
			}
			
			synchronized(listeners)
			{
				for(BeIDCardManagerListener listener: listeners)
					listener.eIDCardRemoved(cardTerminal,beIDCard);
			}
		}
	}

	@Override
	public void terminalException(Throwable throwable)
	{
		// TODO Auto-generated method stub
		
	}
}