/*
 * Commons eID Project.
 * Copyright (C) 2008-2013 FedICT.
 * Copyright (C) 2015-2023 e-Contract.be BV.
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

package be.fedict.commons.eid.jca;

import be.fedict.commons.eid.client.BeIDCard;
import be.fedict.commons.eid.client.FileType;

public class BeIDPrivateKey extends AbstractBeIDPrivateKey {

	public BeIDPrivateKey(FileType certificateFileType, BeIDCard beIDCard, boolean logoff, boolean allowFailingLogoff,
			boolean autoRecovery, BeIDKeyStore beIDKeyStore, String applicationName) {
		super(certificateFileType, beIDCard, logoff, allowFailingLogoff, autoRecovery, beIDKeyStore, applicationName);
	}

	private static final long serialVersionUID = 1L;

}
