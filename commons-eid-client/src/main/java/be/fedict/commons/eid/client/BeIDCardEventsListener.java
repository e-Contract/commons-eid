package be.fedict.commons.eid.client;

import javax.smartcardio.CardTerminal;

public interface BeIDCardEventsListener {
	void eIDCardInserted(CardTerminal cardTerminal, BELPICCard card);
	void eIDCardRemoved(CardTerminal cardTerminal, BELPICCard card);
}
