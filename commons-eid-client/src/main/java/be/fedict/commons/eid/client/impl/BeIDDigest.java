/*
 * Commons eID Project.
 * Copyright (C) 2008-2013 FedICT.
 * Copyright (C) 2020 e-Contract.be BV.
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

package be.fedict.commons.eid.client.impl;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;

/**
 * Enumeration of all supported eID digest algorithms. Also holds all PKCS#1
 * DigestInfo prefixes.
 * <p/>
 * Every DigestAlgorithmIdentifier also contains a NULL DER parameter. This
 * improves compatibility.
 * 
 * @author Frank Cornelis
 * 
 */
public enum BeIDDigest {

	PLAIN_TEXT(new byte[] { 0x30, (byte) 0xff, 0x30, 0x09, 0x06, 0x07, 0x60, 0x38, 0x01, 0x02, 0x01, 0x03, 0x01, 0x04,
			(byte) 0xff, }),

	SHA_1(new byte[] { 0x30, 0x21, 0x30, 0x09, 0x06, 0x05, 0x2b, 0x0e, 0x03, 0x02, 0x1a, 0x05, 0x00, 0x04, 0x14, }),

	SHA_224(new byte[] { 0x30, 0x2d, 0x30, 0x0d, 0x06, 0x09, 0x60, (byte) 0x86, 0x48, 0x01, 0x65, 0x03, 0x04, 0x02,
			0x04, 0x05, 0x00, 0x04, 0x1c, }),

	SHA_256(new byte[] { 0x30, 0x31, 0x30, 0x0d, 0x06, 0x09, 0x60, (byte) 0x86, 0x48, 0x01, 0x65, 0x03, 0x04, 0x02,
			0x01, 0x05, 0x00, 0x04, 0x20, }),

	SHA_384(new byte[] { 0x30, 0x41, 0x30, 0x0d, 0x06, 0x09, 0x60, (byte) 0x86, 0x48, 0x01, 0x65, 0x03, 0x04, 0x02,
			0x02, 0x05, 0x00, 0x04, 0x30, }),

	SHA_512(new byte[] { 0x30, 0x51, 0x30, 0x0d, 0x06, 0x09, 0x60, (byte) 0x86, 0x48, 0x01, 0x65, 0x03, 0x04, 0x02,
			0x03, 0x05, 0x00, 0x04, 0x40, }),

	RIPEMD_128(
			new byte[] { 0x30, 0x1d, 0x30, 0x09, 0x06, 0x05, 0x2b, 0x24, 0x03, 0x02, 0x02, 0x05, 0x00, 0x04, 0x10, }),

	RIPEMD_160(
			new byte[] { 0x30, 0x21, 0x30, 0x09, 0x06, 0x05, 0x2b, 0x24, 0x03, 0x02, 0x01, 0x05, 0x00, 0x04, 0x14, }),

	RIPEMD_256(
			new byte[] { 0x30, 0x2d, 0x30, 0x09, 0x06, 0x05, 0x2b, 0x24, 0x03, 0x02, 0x03, 0x05, 0x00, 0x04, 0x20, }),

	SHA_1_PSS(new byte[] {}, 0x10),

	SHA_256_PSS(new byte[] {}, 0x20),

	ECDSA_SHA_2_256(new byte[] {}, 0x01, true),

	ECDSA_SHA_2_384(new byte[] {}, 0x02, true),

	ECDSA_SHA_2_512(new byte[] {}, 0x04, true),

	ECDSA_SHA_3_256(new byte[] {}, 0x08, true),

	ECDSA_SHA_3_384(new byte[] {}, 0x10, true),

	ECDSA_SHA_3_512(new byte[] {}, 0x20, true),

	NONE(new byte[] {}),

	ECDSA_NONE(new byte[] {}, 0x40, true);

	private final byte[] prefix;
	private final byte algorithmReference;
	private final boolean ec;

	private BeIDDigest(final byte[] prefix, final int algorithmReference, final boolean ec) {
		this.prefix = prefix;
		this.algorithmReference = (byte) algorithmReference;
		this.ec = ec;
	}

	private BeIDDigest(final byte[] prefix, final int algorithmReference) {
		this(prefix, algorithmReference, false);
	}

	private BeIDDigest(final byte[] prefix) {
		this(prefix, 0x01); // default algorithm reference: PKCS#1
	}

	public static BeIDDigest getInstance(final String name) {
		return valueOf(name);
	}

	public byte[] getPrefix(final int valueLength) {
		if (this.equals(PLAIN_TEXT)) {
			final byte[] digestInfoPrefix = Arrays.copyOf(this.prefix, this.prefix.length);
			digestInfoPrefix[1] = (byte) (valueLength + 13);
			digestInfoPrefix[14] = (byte) valueLength;
			return digestInfoPrefix;
		}

		return this.prefix;
	}

	public byte getAlgorithmReference() {
		return this.algorithmReference;
	}

	public String getStandardName() {
		return this.name().replace('_', '-');
	}

	public MessageDigest getMessageDigestInstance() throws NoSuchAlgorithmException {
		return MessageDigest.getInstance(this.getStandardName());
	}

	public boolean isEc() {
		return this.ec;
	}
}
