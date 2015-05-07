/*
 * Commons eID Project.
 * Copyright (C) 2013 e-Contract.be BVBA.
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

package test.integ.be.fedict.commons.eid.client;

import java.io.IOException;

import javax.smartcardio.CardException;
import javax.swing.JOptionPane;

import org.junit.Test;

import be.fedict.commons.eid.client.BeIDCard;
import be.fedict.commons.eid.client.BeIDCards;
import be.fedict.commons.eid.client.FileType;
import be.fedict.commons.eid.consumer.Identity;
import be.fedict.commons.eid.consumer.tlv.TlvParser;

/**
 * See also: https://groups.google.com/forum/#!topic/eid-applet/N1mVFnYJ3VM
 * 
 * @author Frank Cornelis
 * 
 */
public class DisconnectConnectTest {

	@Test
	public void testDisconnectConnect() throws Exception {
		JOptionPane.showMessageDialog(null, "Connect card");
		BeIDCards cards = new BeIDCards(new TestLogger());
		// we ask all cards to avoid going through the default UI
		BeIDCard card1 = cards.getAllBeIDCards().iterator().next();
		Identity identity1 = readIdentity(card1);
		JOptionPane.showMessageDialog(null,
				"Card read: " + identity1.getFirstName());
		card1.close();
		cards.close();
		JOptionPane.showMessageDialog(null,
				"Disconnect and reconnect the reader");
		cards = new BeIDCards();
		BeIDCard card2 = cards.getAllBeIDCards().iterator().next();
		Identity identity2 = readIdentity(card2);
		JOptionPane.showMessageDialog(null,
				"Card read: " + identity2.getFirstName());
		cards.close();
	}

	private Identity readIdentity(BeIDCard card) throws CardException,
			IOException, InterruptedException {
		byte[] idData = card.readFile(FileType.Identity);
		Identity identity = TlvParser.parse(idData, Identity.class);
		return identity;
	}
}
