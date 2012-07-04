package be.fedict.commons.eid.client;

import javax.smartcardio.CardTerminal;

public interface BeIDCardManagerListener {
	void eIDCardInserted(CardTerminal cardTerminal, BeIDCard card);
	void eIDCardRemoved(CardTerminal cardTerminal, BeIDCard card);
}
