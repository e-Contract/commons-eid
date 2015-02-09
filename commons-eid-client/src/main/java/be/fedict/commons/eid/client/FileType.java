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

package be.fedict.commons.eid.client;

/**
 * The FileType enum encapsulates the fileID, keyID, and some usage parameters
 * about the files stored on an BeIDCard. This allows a simple, unified
 * interface to many methods, and auxiliary information to be derived rather
 * than supplied separately.
 * 
 * @author Frank Cornelis
 * @author Frank Marien
 * 
 */
public enum FileType {
	Identity(new byte[]{0x3F, 0x00, (byte) 0xDF, 0x01, 0x40, 0x31}, 179), IdentitySignature(
			new byte[]{0x3F, 0x00, (byte) 0xDF, 0x01, 0x40, 0x32}, 128), Address(
			new byte[]{0x3F, 0x00, (byte) 0xDF, 0x01, 0x40, 0x33}, 121), AddressSignature(
			new byte[]{0x3F, 0x00, (byte) 0xDF, 0x01, 0x40, 0x34}, 128), Photo(
			new byte[]{0x3F, 0x00, (byte) 0xDF, 0x01, 0x40, 0x35}, 3064), AuthentificationCertificate(
			new byte[]{0x3F, 0x00, (byte) 0xDF, 0x00, 0x50, 0x38}, 1061, 0x82), NonRepudiationCertificate(
			new byte[]{0x3F, 0x00, (byte) 0xDF, 0x00, 0x50, 0x39}, 1082, 0x83), CACertificate(
			new byte[]{0x3F, 0x00, (byte) 0xDF, 0x00, 0x50, 0x3A}, 1044), RootCertificate(
			new byte[]{0x3F, 0x00, (byte) 0xDF, 0x00, 0x50, 0x3B}, 914), RRNCertificate(
			new byte[]{0x3F, 0x00, (byte) 0xDF, 0x00, 0x50, 0x3C}, 820);

	private final byte[] fileId;
	private final byte keyId;
	private final int estimatedMaxSize;

	private FileType(final byte[] fileId, final int estimatedMaxSize) {
		this.fileId = fileId;
		this.keyId = -1;
		this.estimatedMaxSize = estimatedMaxSize;
	}

	private FileType(final byte[] fileId, final int estimatedMaxSize,
			final int keyId) {
		this.fileId = fileId;
		this.keyId = (byte) keyId;
		this.estimatedMaxSize = estimatedMaxSize;
	}

	public byte[] getFileId() {
		return this.fileId;
	}

	public byte getKeyId() {
		return this.keyId;
	}

	public boolean isCertificateUserCanSignWith() {
		return this.keyId != -1;
	}

	public boolean chainIncludesCitizenCA() {
		return this.isCertificateUserCanSignWith();
	}

	public int getEstimatedMaxSize() {
		return this.estimatedMaxSize;
	}
}
