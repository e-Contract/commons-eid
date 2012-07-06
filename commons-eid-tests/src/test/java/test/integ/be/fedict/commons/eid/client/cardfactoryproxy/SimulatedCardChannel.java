package test.integ.be.fedict.commons.eid.client.cardfactoryproxy;

import java.nio.ByteBuffer;
import javax.smartcardio.Card;
import javax.smartcardio.CardChannel;
import javax.smartcardio.CardException;
import javax.smartcardio.CommandAPDU;
import javax.smartcardio.ResponseAPDU;

public class SimulatedCardChannel extends CardChannel {
	private SimulatedCard card;

	public SimulatedCardChannel(SimulatedCard card) {
		this.card = card;
	}

	@Override
	public void close() throws CardException {
		throw new RuntimeException("Not Implemented In SimulatedCardChannel");
	}

	@Override
	public Card getCard() {
		return this.card;
	}

	@Override
	public int getChannelNumber() {
		return 0;
	}

	@Override
	public ResponseAPDU transmit(CommandAPDU apdu) throws CardException {
		return this.card.transmit(apdu);
	}

	@Override
	public int transmit(ByteBuffer bb0, ByteBuffer bb1) throws CardException {
		throw new RuntimeException("Not Implemented In SimulatedCardChannel");
	}
}
