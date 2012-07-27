/*
 * Commons eID Project.
 * Copyright (C) 2008-2012 FedICT.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License version
 * 3.0 as published by the Free Software Foundation.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, see 
 * http://www.gnu.org/licenses/.
 */

/**
 * Manual exercise for BeIDCardManager.
 * Prints events and list of readers with eid cards.
 * [short readername] ... 
 * 
 * @author Frank Marien
 * 
 */

package test.integ.be.fedict.commons.eid.client;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.math.BigInteger;
import javax.smartcardio.Card;
import javax.smartcardio.CardException;
import javax.smartcardio.CardTerminal;
import org.junit.Test;
import be.fedict.commons.eid.client.BeIDCard;
import be.fedict.commons.eid.client.BeIDCardManager;
import be.fedict.commons.eid.client.BeIDFileType;
import be.fedict.commons.eid.client.event.BeIDCardEventsListener;
import be.fedict.commons.eid.client.event.CardEventsListener;
import be.fedict.commons.eid.consumer.Address;
import be.fedict.commons.eid.consumer.Identity;
import be.fedict.commons.eid.consumer.tlv.TlvParser;

public class BeIDCardEventsManagerExercise
		implements
			BeIDCardEventsListener,
			CardEventsListener {
	private BeIDCardManager beIDCardManager;

	// ---------------------------------------------------------------------------------------------

	@Test
	public void testAsynchronous() throws Exception {
		beIDCardManager = new BeIDCardManager(new TestLogger());
		beIDCardManager.addBeIDCardEventListener(this);
		beIDCardManager.addOtherCardEventListener(this);
		beIDCardManager.start();

		System.err.println("main thread running.. do some card tricks..");

		for (;;) {
			for (CardTerminal terminal : beIDCardManager
					.getTerminalsWithBeIDCardsPresent())
				System.out.print("[" + terminal.getName() + "]");
			System.out.println(".");
			Thread.sleep(500);
		}
	}

	// ----------------------------- callbacks that just print to stderr
	// -------------------

	@Override
	public void eIDCardInserted(CardTerminal cardTerminal, BeIDCard card) {
		try {
			byte[] identityTLV = card.readFile(BeIDFileType.Identity);
			byte[] addressTLV = card.readFile(BeIDFileType.Address);

			Identity identity = TlvParser.parse(identityTLV, Identity.class);
			System.out.println(identity.firstName + " " + identity.name);

			Address address = TlvParser.parse(addressTLV, Address.class);
			System.out.println(address.streetAndNumber);

			File atrFile = new File("/tmp/Alice_ATR.bin");
			OutputStream os = new FileOutputStream(atrFile);
			os.write(card.getATR().getBytes());

			for (BeIDFileType fileType : BeIDFileType.values()) {
				byte[] tlvData = card.readFile(fileType);
				System.err.println("Read [" + fileType + "] -> "
						+ tlvData.length + " bytes.");
				File file = new File("/tmp/Alice_" + fileType + ".tlv");
				OutputStream stream = new FileOutputStream(file);
				stream.write(tlvData);
			}

		} catch (CardException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		System.err.println("eID Card Inserted Into ["
				+ StringUtils.getShortTerminalname(cardTerminal.getName())
				+ "]");
		// StringUtils.printTerminalOverviewLine(beIDCardManager);
	}

	@Override
	public void eIDCardRemoved(CardTerminal cardTerminal, BeIDCard card) {
		System.err.println("eID Card Removed From ["
				+ StringUtils.getShortTerminalname(cardTerminal.getName())
				+ "]");
	}

	@Override
	public void cardInserted(CardTerminal cardTerminal, Card card) {
		if (card != null)
			System.out.println("Other Card ["
					+ String.format("%x", new BigInteger(1, card.getATR()
							.getBytes())) + "] Inserted Into Terminal ["
					+ cardTerminal.getName() + "]");
		else
			System.out.println("Other Card Inserted Into Terminal ["
					+ cardTerminal.getName() + "] but failed to connect()");
	}

	@Override
	public void cardRemoved(CardTerminal cardTerminal) {
		System.out.println("Other Card Removed From [" + cardTerminal.getName()
				+ "]");
	}
}
