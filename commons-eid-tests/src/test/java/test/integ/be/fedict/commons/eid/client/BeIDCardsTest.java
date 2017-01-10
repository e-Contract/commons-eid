/*
 * Commons eID Project.
 * Copyright (C) 2008-2013 FedICT.
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

import static org.junit.Assert.assertNotNull;

import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.Test;

import be.fedict.commons.eid.client.BeIDCard;
import be.fedict.commons.eid.client.BeIDCards;
import be.fedict.commons.eid.client.CancelledException;
import be.fedict.commons.eid.client.FileType;
import be.fedict.commons.eid.consumer.Identity;
import be.fedict.commons.eid.consumer.tlv.TlvParser;

public class BeIDCardsTest {
	private static final Log LOG = LogFactory.getLog(BeIDCardsTest.class);

	@Test
	public void waitInsertAndRemove() throws Exception {
		LOG.debug("creating beIDCards Instance");
		final BeIDCards beIDCards = new BeIDCards(new TestLogger());
		assertNotNull(beIDCards);

		LOG.debug("asking beIDCards Instance for One BeIDCard");

		try {
			final BeIDCard beIDCard = beIDCards.getOneBeIDCard();
			assertNotNull(beIDCard);

			LOG.debug("reading identity file");
			final byte[] identityFile = beIDCard.readFile(FileType.Identity);
			final Identity identity = TlvParser.parse(identityFile,
					Identity.class);
			LOG.debug("card holder is " + identity.getFirstName() + " "
					+ identity.getName());

			if (beIDCards.getAllBeIDCards().contains(beIDCard)) {
				LOG.debug("waiting for card removal");
				beIDCards.waitUntilCardRemoved(beIDCard);
			}

			LOG.debug("card removed");
		} catch (final CancelledException cex) {
			LOG.error("Cancelled By User");
		}

	}

	@Test
	public void testGetAllBeIDCards() throws Exception {
		LOG.debug("creating beIDCards Instance");
		final BeIDCards beIDCards = new BeIDCards(new TestLogger());
		assertNotNull(beIDCards);

		LOG.debug("beIDCards Instance for all BeIDCards");

		final Set<BeIDCard> allCards = beIDCards.getAllBeIDCards();

		// All cards should work
		for (BeIDCard beIDCard : allCards) {
			// Invalidate card for further use
			beIDCard.beginExclusive();
		}

		final Set<BeIDCard> allCards2 = beIDCards.getAllBeIDCards();

		// All cards should work
		for (BeIDCard beIDCard : allCards2) {
			beIDCard.beginExclusive();
			beIDCard.endExclusive();
		}
	}

	@Test
	public void testGetOneBeIDCards() throws Exception {
		LOG.debug("creating beIDCards Instance");
		final BeIDCards beIDCards = new BeIDCards(new TestLogger());
		assertNotNull(beIDCards);

		LOG.debug("beIDCards Instance for all BeIDCards");

		final BeIDCard beIDCard = beIDCards.getOneBeIDCard();

		// Invalidate card for further use
		beIDCard.beginExclusive();

		final BeIDCard beIDCard2 = beIDCards.getOneBeIDCard();

		// Card should work
		beIDCard2.beginExclusive();
		beIDCard2.endExclusive();
	}
}
