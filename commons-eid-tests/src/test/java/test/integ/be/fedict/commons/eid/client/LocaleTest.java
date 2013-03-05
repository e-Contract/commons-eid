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

import java.util.Locale;

import org.junit.Test;

import be.fedict.commons.eid.client.BeIDCard;
import be.fedict.commons.eid.client.BeIDCards;
import be.fedict.commons.eid.client.FileType;
import be.fedict.commons.eid.client.impl.BeIDDigest;

public class LocaleTest {
	@Test
	public void testLocale() throws Exception {
		final BeIDCards cards = new BeIDCards(Locale.FRENCH);

		BeIDCard card = cards.getOneBeIDCard();
		card.sign(new byte[]{0x00, 0x00, 0x00, 0x00}, BeIDDigest.PLAIN_TEXT,
				FileType.NonRepudiationCertificate, false);
		card.setLocale(new Locale("nl"));
		card.sign(new byte[]{0x00, 0x00, 0x00, 0x00}, BeIDDigest.PLAIN_TEXT,
				FileType.NonRepudiationCertificate, false);

		cards.setLocale(Locale.GERMAN);
		card = cards.getOneBeIDCard();
		card.sign(new byte[]{0x00, 0x00, 0x00, 0x00}, BeIDDigest.PLAIN_TEXT,
				FileType.NonRepudiationCertificate, false);
	}
}
