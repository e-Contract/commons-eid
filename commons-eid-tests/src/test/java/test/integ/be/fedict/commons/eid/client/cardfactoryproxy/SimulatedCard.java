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

package test.integ.be.fedict.commons.eid.client.cardfactoryproxy;

import java.math.BigInteger;
import java.util.HashMap;
import java.util.Map;
import javax.smartcardio.ATR;
import javax.smartcardio.Card;
import javax.smartcardio.CardChannel;
import javax.smartcardio.CardException;
import javax.smartcardio.CommandAPDU;
import javax.smartcardio.ResponseAPDU;

public class SimulatedCard extends Card {
	protected static final ResponseAPDU OK = new ResponseAPDU(new byte[]{
			(byte) 0x90, 0x00});
	protected static final ResponseAPDU COMMAND_NOT_AVAILABLE = new ResponseAPDU(
			new byte[]{0x6d, 0x00});
	protected static final ResponseAPDU FILE_NOT_FOUND = new ResponseAPDU(
			new byte[]{0x6a, (byte) 0x82});
	protected static final ResponseAPDU OFFSET_OUTSIDE_EF = new ResponseAPDU(
			new byte[]{0x6b, (byte) 0x00});

	protected ATR atr;
	protected String protocol;
	protected Map<BigInteger, byte[]> files;
	protected byte[] selectedFile;

	public SimulatedCard(ATR atr) {
		super();
		this.atr = atr;
		this.files = new HashMap<BigInteger, byte[]>();
	}

	public void setATR(ATR atr) {
		this.atr = atr;
	}

	@Override
	public void beginExclusive() throws CardException {
		throw new RuntimeException("Not Implemented In SimulatedCard");
	}

	@Override
	public void disconnect(boolean arg0) throws CardException {
		throw new RuntimeException("Not Implemented In SimulatedCard");
	}

	@Override
	public void endExclusive() throws CardException {
		throw new RuntimeException("Not Implemented In SimulatedCard");
	}

	@Override
	public ATR getATR() {
		return atr;
	}

	@Override
	public CardChannel getBasicChannel() {
		return new SimulatedCardChannel(this);
	}

	@Override
	public String getProtocol() {
		return protocol;
	}

	@Override
	public CardChannel openLogicalChannel() throws CardException {
		throw new RuntimeException("Not Implemented In SimulatedCard");
	}

	@Override
	public byte[] transmitControlCommand(int arg0, byte[] arg1)
			throws CardException {
		throw new RuntimeException("Not Implemented In SimulatedCard");
	}

	protected ResponseAPDU transmit(CommandAPDU apdu) throws CardException {
		// "SELECT FILE"
		if (apdu.getCLA() == 0x00 && apdu.getINS() == 0xA4
				&& apdu.getP1() == 0x08 && apdu.getP2() == 0x0C) {
			return selectFile(apdu.getData());
		}
		// "READ BINARY"
		else if (apdu.getCLA() == 0x00 && apdu.getINS() == 0xB0) {
			int offset = (apdu.getP1() << 8) + apdu.getP2();
			return readBinary(offset, (int) apdu.getNe());
		}
		return COMMAND_NOT_AVAILABLE;
	}

	protected ResponseAPDU readBinary(int offset, int length) {
		// how many bytes left to read
		int lengthToReturn = selectedFile.length - offset;

		// limited to number of bytes requested
		if (lengthToReturn > length)
			lengthToReturn = length;

		// there are no more bytes..
		if (lengthToReturn == 0)
			return OFFSET_OUTSIDE_EF;

		// reserve number of bytes + 2 for trailer
		byte[] response = new byte[lengthToReturn + 2];

		// copy the bytes from the selected file..
		System.arraycopy(selectedFile, offset, response, 0, lengthToReturn);

		// add the trailer with OK response
		response[lengthToReturn] = (byte) 0x90;
		response[lengthToReturn + 1] = 0x00;

		// return as an BeIDCommandAPDU..
		return new ResponseAPDU(response);
	}

	protected ResponseAPDU selectFile(byte[] fileId) {
		selectedFile = files.get(new BigInteger(fileId));
		if (selectedFile == null)
			return FILE_NOT_FOUND;
		return OK;
	}

	public SimulatedCard setFile(byte[] fileId, byte[] fileData) {
		files.put(new BigInteger(fileId), fileData);
		return this;
	}

	public SimulatedCard removeFile(byte[] fileId) {
		files.remove(fileId);
		return this;
	}
}