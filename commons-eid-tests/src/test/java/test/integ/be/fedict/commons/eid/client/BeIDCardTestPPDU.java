/*
 * Commons eID Project.
 * Copyright (C) 2008-2013 FedICT.
 * Copyright (C) 2015 e-Contract.be BVBA.
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

import be.fedict.commons.eid.client.BeIDCard;
import be.fedict.commons.eid.client.impl.CCID;

public class BeIDCardTestPPDU extends BeIDCardTest {

	protected BeIDCard getBeIDCard() {
		CCID.addPPDUName("digipass 870");
		CCID.addPPDUName("digipass 875");
		CCID.addPPDUName("digipass 920");

		return super.getBeIDCard();
	}
}