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

package test.integ.be.fedict.commons.eid.client.simulation;

import java.io.IOException;
import java.io.InputStream;
import javax.smartcardio.ATR;
import org.apache.commons.io.IOUtils;
import be.fedict.commons.eid.client.FileType;

public class SimulatedBeIDCard extends SimulatedCard {
	public SimulatedBeIDCard(String profile) {
		super(null);

		InputStream atrInputStream = SimulatedBeIDCard.class
				.getResourceAsStream("/" + profile + "_ATR.bin");

		try {
			setATR(new ATR(IOUtils.toByteArray(atrInputStream)));
		} catch (IOException e) {
			// missing _ATR file, set ATR from testcard
			setATR(new ATR(new byte[]{0x3b, (byte) 0x98, 0x13, 0x40, 0x0a,
					(byte) 0xa5, 0x03, 0x01, 0x01, 0x01, (byte) 0xad, 0x13,
					0x11}));
		}

		setFilesFromProfile(profile);
	}

	public SimulatedBeIDCard(ATR atr) {
		super(atr);
	}

	public SimulatedBeIDCard setFilesFromProfile(String profile) {
		for (FileType type : FileType.values()) {
			try {
				setFileFromProfile(type, profile);
			} catch (IOException iox) {
				System.err.println("Card Has No " + type);
			}
		}

		return this;
	}

	public SimulatedBeIDCard setFileFromProfile(FileType type, String profile)
			throws IOException {
		InputStream idInputStream = SimulatedBeIDCard.class
				.getResourceAsStream("/" + profile + "_" + type + ".tlv");
		setFile(type.getFileId(), IOUtils.toByteArray(idInputStream));
		return this;
	}
}
