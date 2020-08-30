/*
 * Commons eID Project.
 * Copyright (C) 2008-2013 FedICT.
 * Copyright (C) 2017-2020 e-Contract.be BV.
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import javax.smartcardio.CardTerminal;

import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import be.fedict.commons.eid.client.BeIDCard;
import be.fedict.commons.eid.client.BeIDCards;
import be.fedict.commons.eid.client.CancelledException;
import be.fedict.commons.eid.client.FileType;
import be.fedict.commons.eid.consumer.Identity;
import be.fedict.commons.eid.consumer.tlv.TlvParser;

public class BeIDCardsSpecificTerminalTest {

	private static final Logger LOGGER = LoggerFactory.getLogger(BeIDCardsSpecificTerminalTest.class);

	@Test
	public void waitInsertAndRemove() throws Exception {
		LOGGER.debug("creating beIDCards Instance");
		final BeIDCards beIDCards = new BeIDCards(new TestLogger());
		assertNotNull(beIDCards);

		LOGGER.debug("asking beIDCards Instance for One BeIDCard");

		try {
			BeIDCard beIDCard = beIDCards.getOneBeIDCard();
			assertNotNull(beIDCard);

			LOGGER.debug("imprinting upon a particular CardTerminal");
			final CardTerminal terminal = beIDCard.getCardTerminal();

			LOGGER.debug("reading identity file");
			byte[] identityFile = beIDCard.readFile(FileType.Identity);
			Identity identity = TlvParser.parse(identityFile, Identity.class);
			LOGGER.debug("card holder is {} {}", identity.getFirstName(), identity.getName());
			String userId = identity.getNationalNumber();

			if (beIDCards.getAllBeIDCards().contains(beIDCard)) {
				LOGGER.debug("waiting for card removal");
				beIDCards.waitUntilCardRemoved(beIDCard);
			}

			LOGGER.debug("We want only a card from our imprinted CardTerminal back");
			beIDCard = beIDCards.getOneBeIDCard(terminal);
			assertNotNull(beIDCard);

			identityFile = beIDCard.readFile(FileType.Identity);
			identity = TlvParser.parse(identityFile, Identity.class);
			assertEquals(userId, identity.getNationalNumber());

		} catch (final CancelledException cex) {
			LOGGER.error("Cancelled By User");
		}
	}
}
