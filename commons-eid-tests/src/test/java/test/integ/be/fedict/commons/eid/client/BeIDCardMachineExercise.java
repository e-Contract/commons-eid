package test.integ.be.fedict.commons.eid.client;

import java.io.IOException;
import java.math.BigInteger;
import org.junit.Test;
import test.integ.be.fedict.commons.eid.client.cardfactoryproxy.ErrorCapableBeIdCard;
import be.fedict.commons.eid.client.BeIDCardMachine;
import be.fedict.commons.eid.consumer.Address;
import be.fedict.commons.eid.consumer.Identity;
import be.fedict.commons.eid.consumer.tlv.TlvParser;

public class BeIDCardMachineExercise {
	private BeIDCardMachine machine;

	@Test
	public void beidcardmachinetest() throws IOException {
		ErrorCapableBeIdCard card = new ErrorCapableBeIdCard("Alice", 0);
		System.err.println("Card ["
				+ String.format("%x", new BigInteger(1, card.getATR()
						.getBytes())) + "]");
		machine = new BeIDCardMachine(card, new TestLogger());

		card.introduceRandomResponse();

		byte[] idData = machine.getIdentity();
		if (idData != null) {
			Identity identity = TlvParser.parse(idData, Identity.class);
			System.out.println(identity.firstName + " " + identity.name);
		}
		card.introduceCardException();

		byte[] addrData = machine.getAddress();
		if (addrData != null) {
			Address address = TlvParser.parse(addrData, Address.class);
			System.out.println(address.streetAndNumber);
		}
	}
}
