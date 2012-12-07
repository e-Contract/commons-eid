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

package test.integ.be.fedict.commons.eid.client;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import javax.smartcardio.CardTerminal;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.Test;
import be.fedict.commons.eid.client.BeIDCard;
import be.fedict.commons.eid.client.BeIDCards;
import be.fedict.commons.eid.client.CancelledException;
import be.fedict.commons.eid.client.FileType;
import be.fedict.commons.eid.consumer.Identity;
import be.fedict.commons.eid.consumer.tlv.TlvParser;

public class BeIDCardsSpecificTerminalExercise {
	private static final Log LOG = LogFactory
			.getLog(BeIDCardsSpecificTerminalExercise.class);

	@Test
	public void waitInsertAndRemove() throws Exception {
		LOG.debug("creating beIDCards Instance");
		final BeIDCards beIDCards = new BeIDCards(new TestLogger());
		assertNotNull(beIDCards);

		LOG.debug("asking beIDCards Instance for One BeIDCard");

		try {
			BeIDCard beIDCard = beIDCards.getOneBeIDCard();
			assertNotNull(beIDCard);

			LOG.debug("imprinting upon a particular CardTerminal");
			final CardTerminal terminal = beIDCard.getCardTerminal();

			LOG.debug("reading identity file");
			byte[] identityFile = beIDCard.readFile(FileType.Identity);
			Identity identity = TlvParser.parse(identityFile,
					Identity.class);
			LOG.debug("card holder is " + identity.getFirstName() + " "
					+ identity.getName());
            String userId = identity.getNationalNumber();

			if (beIDCards.getAllBeIDCards().contains(beIDCard)) {
				LOG.debug("waiting for card removal");
				beIDCards.waitUntilCardRemoved(beIDCard);
			}

			LOG
					.debug("We want only a card from our imprinted CardTerminal back");
			beIDCard = beIDCards.getOneBeIDCard(terminal);
			assertNotNull(beIDCard);

            identityFile = beIDCard.readFile(FileType.Identity);
            identity = TlvParser.parse(identityFile,
                    Identity.class);
            assertEquals(userId, identity.getNationalNumber());

		} catch (final CancelledException cex) {
			LOG.error("Cancelled By User");
		}

	}
}
