package be.fedict.commons.eid.client;

import javax.smartcardio.CardTerminal;

public interface CardTerminalEventsListener {
	void terminalAttached(CardTerminal cardTerminal);
	void terminalDetached(CardTerminal cardTerminal);
	void terminalException(Throwable throwable);
}
