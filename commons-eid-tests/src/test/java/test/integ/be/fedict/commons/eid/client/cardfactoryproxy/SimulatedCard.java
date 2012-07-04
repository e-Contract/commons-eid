package test.integ.be.fedict.commons.eid.client.cardfactoryproxy;

import javax.smartcardio.ATR;
import javax.smartcardio.Card;
import javax.smartcardio.CardChannel;
import javax.smartcardio.CardException;

public class SimulatedCard extends Card {
	private ATR atr;
	private String protocol;

	public SimulatedCard(ATR atr) {
		super();
		this.atr = atr;
	}

	@Override
	public void beginExclusive() throws CardException {
		throw new RuntimeException("Not Implemented In SimulatedCard");
	}

	@Override
	public void disconnect(boolean arg0) throws CardException {
		throw new RuntimeException("Not Implemented In SimulatedCard");
	}

	@Override
	public void endExclusive() throws CardException {
		throw new RuntimeException("Not Implemented In SimulatedCard");
	}

	@Override
	public ATR getATR() {
		return atr;
	}

	@Override
	public CardChannel getBasicChannel() {
		throw new RuntimeException("Not Implemented In SimulatedCard");
	}

	@Override
	public String getProtocol() {
		return protocol;
	}

	@Override
	public CardChannel openLogicalChannel() throws CardException {
		throw new RuntimeException("Not Implemented In SimulatedCard");
	}

	@Override
	public byte[] transmitControlCommand(int arg0, byte[] arg1)
			throws CardException {
		throw new RuntimeException("Not Implemented In SimulatedCard");
	}
}
