package be.fedict.commons.eid.client;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import javax.smartcardio.ATR;
import javax.smartcardio.Card;
import javax.smartcardio.CardTerminal;

public class BeIDCardEventsManager implements CardEventsListener
{
	private final static byte[]				ATR_PATTERN	=new byte[]{0x3b,(byte)0x98,0x00,0x40,0x00,(byte)0x00,0x00,0x00,0x01,0x01,(byte)0xad,0x13,0x10};
	private final static byte[]				ATR_MASK	=new byte[]{(byte)0xff,(byte)0xff,0x00,(byte)0xff,0x00,0x00,0x00,0x00,(byte)0xff,(byte)0xff,(byte)0xff,(byte)0xff,(byte)0xf0};

	private CardAndTerminalEventsManager	cardAndTerminalEventsManager;
	private boolean							terminalManagerIsPrivate;
	private Map<CardTerminal,BeIDCard>		terminalsAndCards;
	private Set<BeIDCardEventsListener>		listeners;
	private final Logger					logger;

	/*
	 * a BeIDCardEventsManager with a default (void) logger and a private
	 * CardAndTerminalEventsManager that is automatically started and stopped
	 * with the BeIDCardEventsManager
	 */
	public BeIDCardEventsManager()
	{
		this(new VoidLogger());
	}

	/*
	 * a BeIDCardEventsManager logging to logger, and a private Terminalmanager
	 * that is automatically started and stopped with the BeIDCardEventsManager
	 */
	public BeIDCardEventsManager(Logger logger)
	{
		this(logger,new CardAndTerminalEventsManager());
		this.terminalManagerIsPrivate=true;
	}

	/*
	 * a BeIDCardEventsManager with a default (void) logger, caller supplies a
	 * CardAndTerminalEventsManager. note: caller is responsible for start()in
	 * the supplied CardAndTerminalEventsManager, it will not be automatically
	 * started!
	 */
	public BeIDCardEventsManager(CardAndTerminalEventsManager cardAndTerminalEventsManager)
	{
		this(new VoidLogger(),cardAndTerminalEventsManager);
	}

	/*
	 * a BeIDCardEventsManager logging to logger, caller supplies a
	 * CardAndTerminalEventsManager. note: caller is responsible for start()in
	 * the supplied CardAndTerminalEventsManager, it will not be automatically
	 * started!
	 */
	public BeIDCardEventsManager(Logger logger,CardAndTerminalEventsManager cardAndTerminalEventsManager)
	{
		this.logger=logger;
		this.listeners=new HashSet<BeIDCardEventsListener>();
		this.terminalsAndCards=new HashMap<CardTerminal,BeIDCard>();
		this.cardAndTerminalEventsManager=cardAndTerminalEventsManager;
		this.cardAndTerminalEventsManager.addCardListener(this);
	}

	public BeIDCardEventsManager start()
	{
		if(terminalManagerIsPrivate)
			cardAndTerminalEventsManager.start();
		return this;
	}

	// add a BeIDCardEventsListener
	public BeIDCardEventsManager addListener(BeIDCardEventsListener listener)
	{
		synchronized(listeners)
		{
			listeners.add(listener);
		}
		return this;
	}

	// remove a BeIDCardEventsListener
	public BeIDCardEventsManager removeListener(BeIDCardEventsListener listener)
	{
		synchronized(listeners)
		{
			listeners.remove(listener);
		}
		return this;
	}

	public BeIDCardEventsManager stop()
	{
		if(terminalManagerIsPrivate)
			cardAndTerminalEventsManager.stop();
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

	// public CardTerminal findFirstBeIDCardTerminal() throws CardException
	// {
	// for(CardTerminal terminalWithCard :
	// cardAndTerminalEventsManager.getTerminalsWithCards())
	// {
	// Card card=terminalWithCard.connect("*");
	// }
	// }

	private boolean matchesEidAtr(ATR atr)
	{
		byte[] atrBytes=atr.getBytes();
		if(atrBytes.length!=ATR_PATTERN.length)
			return false;
		for(int idx=0;idx<atrBytes.length;idx++)
			atrBytes[idx]&=ATR_MASK[idx];
		if(Arrays.equals(atrBytes,ATR_PATTERN))
			return true;
		return false;
	}

	@Override
	public void cardInserted(CardTerminal cardTerminal,Card card)
	{
		if(card!=null&&matchesEidAtr(card.getATR()))
		{
			BeIDCard beIDCard=new BeIDCard(card,logger);

			synchronized(terminalsAndCards)
			{
				terminalsAndCards.put(cardTerminal,beIDCard);
			}

			synchronized(listeners)
			{
				for(BeIDCardEventsListener listener:listeners)
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
				for(BeIDCardEventsListener listener:listeners)
					listener.eIDCardRemoved(cardTerminal,beIDCard);
			}
		}
	}
}