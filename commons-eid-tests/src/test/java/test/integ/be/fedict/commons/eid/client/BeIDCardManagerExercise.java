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
import java.math.BigInteger;
import javax.smartcardio.Card;
import javax.smartcardio.CardTerminal;
import org.junit.Test;
import be.fedict.commons.eid.client.BeIDCard;
import be.fedict.commons.eid.client.BeIDCardManager;
import be.fedict.commons.eid.client.event.BeIDCardEventsListener;
import be.fedict.commons.eid.client.event.CardEventsListener;

public class BeIDCardManagerExercise
		implements
			BeIDCardEventsListener,
			CardEventsListener {
	private BeIDCardManager beIDCardManager;

	// ---------------------------------------------------------------------------------------------

	@Test
	public void testAsynchronous() throws Exception {
		this.beIDCardManager = new BeIDCardManager(new TestLogger());
		this.beIDCardManager.addBeIDCardEventListener(this);
		this.beIDCardManager.addOtherCardEventListener(this);
		this.beIDCardManager.start();

		System.err.println("main thread running.. do some card tricks..");

		for (;;) {
			System.out.println(".");
			Thread.sleep(500);
		}
	}

	// ------------ callbacks that just print to stderr ---------------

	@Override
	public void eIDCardInserted(final CardTerminal cardTerminal,
			final BeIDCard card) {
		// save card files for later card simulator use
		// try
		// {
		// byte[] identityTLV=card.readFile(FileType.Identity);
		// byte[] addressTLV=card.readFile(FileType.Address);
		//
		// Identity identity=TlvParser.parse(identityTLV,Identity.class);
		// System.out.println(identity.firstName+" "+identity.name);
		//
		// Address address=TlvParser.parse(addressTLV,Address.class);
		// System.out.println(address.streetAndNumber);
		//
		// File atrFile=new File("/tmp/Alice_ATR.bin");
		// OutputStream os=new FileOutputStream(atrFile);
		// os.write(card.getATR().getBytes());
		//
		// for(FileType fileType:FileType.values())
		// {
		// byte[] tlvData=card.readFile(fileType);
		// System.err.println("Read ["+fileType+"] -> "+tlvData.length+" bytes.");
		// File file=new File("/tmp/Alice_"+fileType+".tlv");
		// OutputStream stream=new FileOutputStream(file);
		// stream.write(tlvData);
		// }
		//
		// }
		// catch(CardException e)
		// {
		// // TODO Auto-generated catch block
		// e.printStackTrace();
		// }
		// catch(IOException e)
		// {
		// // TODO Auto-generated catch block
		// e.printStackTrace();
		// }

		System.err.println("eID Card Inserted Into ["
				+ StringUtils.getShortTerminalname(cardTerminal.getName())
				+ "]");
		// StringUtils.printTerminalOverviewLine(beIDCardManager);
	}

	@Override
	public void eIDCardRemoved(final CardTerminal cardTerminal,
			final BeIDCard card) {
		System.err.println("eID Card Removed From ["
				+ StringUtils.getShortTerminalname(cardTerminal.getName())
				+ "]");
	}

	@Override
	public void cardInserted(final CardTerminal cardTerminal, final Card card) {
		if (card != null) {
			System.out.println("Other Card ["
					+ String.format("%x", new BigInteger(1, card.getATR()
							.getBytes())) + "] Inserted Into Terminal ["
					+ cardTerminal.getName() + "]");
		} else {
			System.out.println("Other Card Inserted Into Terminal ["
					+ cardTerminal.getName() + "] but failed to connect()");
		}
	}

	@Override
	public void cardRemoved(final CardTerminal cardTerminal) {
		System.out.println("Other Card Removed From [" + cardTerminal.getName()
				+ "]");
	}

	@Override
	public void cardEventsInitialized() {
		System.out.println("Other Card Events Initialised");

	}

	@Override
	public void eIDCardEventsInitialized() {
		System.out.println("BeID Card Events Initialised");

	}
}
