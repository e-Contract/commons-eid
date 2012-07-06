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

package be.fedict.commons.eid.client;

public enum BeIDFileType {

	Identity(new byte[]{0x3F, 0x00, (byte) 0xDF, 0x01, 0x40, 0x31}, 179), IdentitySignature(
			new byte[]{0x3F, 0x00, (byte) 0xDF, 0x01, 0x40, 0x32}, 128), Address(
			new byte[]{0x3F, 0x00, (byte) 0xDF, 0x01, 0x40, 0x33}, 121), AddressSignature(
			new byte[]{0x3F, 0x00, (byte) 0xDF, 0x01, 0x40, 0x34}, 128), Photo(
			new byte[]{0x3F, 0x00, (byte) 0xDF, 0x01, 0x40, 0x35}, 3064), AuthentificationCertificate(
			new byte[]{0x3F, 0x00, (byte) 0xDF, 0x00, 0x50, 0x38}, 1061), SigningCertificate(
			new byte[]{0x3F, 0x00, (byte) 0xDF, 0x00, 0x50, 0x39}, 1082), CACertificate(
			new byte[]{0x3F, 0x00, (byte) 0xDF, 0x00, 0x50, 0x3A}, 1044), RootCertificate(
			new byte[]{0x3F, 0x00, (byte) 0xDF, 0x00, 0x50, 0x3B}, 914), RRNCertificate(
			new byte[]{0x3F, 0x00, (byte) 0xDF, 0x00, 0x50, 0x3C}, 820);

	private final byte[] fileId;

	private final int estimatedMaxSize;

	private BeIDFileType(byte[] fileId, int estimatedMaxSize) {
		this.fileId = fileId;
		this.estimatedMaxSize = estimatedMaxSize;
	}

	public byte[] getFileId() {
		return this.fileId;
	}

	public int getEstimatedMaxSize() {
		return this.estimatedMaxSize;
	}
}
