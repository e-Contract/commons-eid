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

import static org.junit.Assert.assertNotNull;

import javax.smartcardio.CardTerminal;

import org.junit.Test;

import be.fedict.commons.eid.client.BeIDCard;
import be.fedict.commons.eid.client.CardManager;

public class BeIDCardTest {

	@Test
	public void testBasic() throws Exception {
		TestLogger logger = new TestLogger();
		CardManager cardManager = new CardManager(logger);
		CardTerminal cardTerminal = cardManager.findFirstBeIDCardTerminal();

		assertNotNull(cardTerminal);

		BeIDCard beIDCard = new BeIDCard(cardTerminal, logger);

		beIDCard.close();
	}
}
