/*
 * Commons eID Project.
 * Copyright (C) 2008-2013 FedICT.
 * Copyright (C) 2020-2023 e-Contract.be BV.
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

	PLAIN_TEXT(null,
			new byte[] { 0x30, (byte) 0xff, 0x30, 0x09, 0x06, 0x07, 0x60, 0x38, 0x01, 0x02, 0x01, 0x03, 0x01, 0x04,
					(byte) 0xff, }),

	SHA_1("SHA-1",
			new byte[] { 0x30, 0x21, 0x30, 0x09, 0x06, 0x05, 0x2b, 0x0e, 0x03, 0x02, 0x1a, 0x05, 0x00, 0x04, 0x14, }),

	SHA_224("SHA-224",
			new byte[] { 0x30, 0x2d, 0x30, 0x0d, 0x06, 0x09, 0x60, (byte) 0x86, 0x48, 0x01, 0x65, 0x03, 0x04, 0x02,
					0x04, 0x05, 0x00, 0x04, 0x1c, }),

	SHA_256("SHA-256",
			new byte[] { 0x30, 0x31, 0x30, 0x0d, 0x06, 0x09, 0x60, (byte) 0x86, 0x48, 0x01, 0x65, 0x03, 0x04, 0x02,
					0x01, 0x05, 0x00, 0x04, 0x20, }),

	SHA_384("SHA-384",
			new byte[] { 0x30, 0x41, 0x30, 0x0d, 0x06, 0x09, 0x60, (byte) 0x86, 0x48, 0x01, 0x65, 0x03, 0x04, 0x02,
					0x02, 0x05, 0x00, 0x04, 0x30, }),

	SHA_512("SHA-512",
			new byte[] { 0x30, 0x51, 0x30, 0x0d, 0x06, 0x09, 0x60, (byte) 0x86, 0x48, 0x01, 0x65, 0x03, 0x04, 0x02,
					0x03, 0x05, 0x00, 0x04, 0x40, }),

	RIPEMD_128("RIPEMD128",
			new byte[] { 0x30, 0x1d, 0x30, 0x09, 0x06, 0x05, 0x2b, 0x24, 0x03, 0x02, 0x02, 0x05, 0x00, 0x04, 0x10, }),

	RIPEMD_160("RIPEMD160",
			new byte[] { 0x30, 0x21, 0x30, 0x09, 0x06, 0x05, 0x2b, 0x24, 0x03, 0x02, 0x01, 0x05, 0x00, 0x04, 0x14, }),

	RIPEMD_256("RIPEMD256",
			new byte[] { 0x30, 0x2d, 0x30, 0x09, 0x06, 0x05, 0x2b, 0x24, 0x03, 0x02, 0x03, 0x05, 0x00, 0x04, 0x20, }),

	SHA3_256("SHA3-256",
			new byte[] { 0x30, 0x31, 0x30, 0x0D, 0x06, 0x09, 0x60, (byte) 0x86, 0x48, 0x01, 0x65, 0x03, 0x04, 0x02,
					0x08, 0x05, 0x00, 0x04, 0x20, }),

	SHA3_384("SHA3-384",
			new byte[] { 0x30, 0x41, 0x30, 0x0D, 0x06, 0x09, 0x60, (byte) 0x86, 0x48, 0x01, 0x65, 0x03, 0x04, 0x02,
					0x09, 0x05, 0x00, 0x04, 0x30, }),

	SHA3_512("SHA3-512",
			new byte[] { 0x30, 0x51, 0x30, 0x0D, 0x06, 0x09, 0x60, (byte) 0x86, 0x48, 0x01, 0x65, 0x03, 0x04, 0x02,
					0x0A, 0x05, 0x00, 0x04, 0x40, }),

	SHA_1_PSS("SHA-1", new byte[] {}, 0x10),

	SHA_256_PSS("SHA-256", new byte[] {}, 0x20),

	ECDSA_SHA_2_256("SHA-256", new byte[] {}, 0x01, true),

	ECDSA_SHA_2_384("SHA-384", new byte[] {}, 0x02, true),

	ECDSA_SHA_2_512("SHA-512", new byte[] {}, 0x04, true),

	ECDSA_SHA_3_256("SHA3-256", new byte[] {}, 0x08, true),

	ECDSA_SHA_3_384("SHA3-384", new byte[] {}, 0x10, true),

	ECDSA_SHA_3_512("SHA3-512", new byte[] {}, 0x20, true),

	ECDSA_SHA_2_256_P1363("SHA-256", new byte[] {}, 0x01, true, true),

	ECDSA_SHA_2_384_P1363("SHA-384", new byte[] {}, 0x02, true, true),

	ECDSA_SHA_2_512_P1363("SHA-512", new byte[] {}, 0x04, true, true),

	ECDSA_SHA_3_256_P1363("SHA3-256", new byte[] {}, 0x08, true, true),

	ECDSA_SHA_3_384_P1363("SHA3-384", new byte[] {}, 0x10, true, true),

	ECDSA_SHA_3_512_P1363("SHA3-512", new byte[] {}, 0x20, true, true),

	NONE(null, new byte[] {}),

	ECDSA_NONE(null, new byte[] {}, 0x40, true),

	ECDSA_NONE_P1363(null, new byte[] {}, 0x40, true, true);

	private final byte[] prefix;
	private final byte algorithmReference;
	private final boolean ec;
	private final boolean p1363;
	private final String algorithm;

	private BeIDDigest(String algorithm, final byte[] prefix, final int algorithmReference, final boolean ec,
			final boolean p1363) {
		this.algorithm = algorithm;
		this.prefix = prefix;
		this.algorithmReference = (byte) algorithmReference;
		this.ec = ec;
		this.p1363 = p1363;
	}

	private BeIDDigest(String algorithm, final byte[] prefix, final int algorithmReference, final boolean ec) {
		this(algorithm, prefix, algorithmReference, ec, false);
	}

	private BeIDDigest(String algorithm, final byte[] prefix, final int algorithmReference) {
		this(algorithm, prefix, algorithmReference, false, false);
	}

	private BeIDDigest(String algorithm, final byte[] prefix) {
		this(null, prefix, 0x01); // default algorithm reference: PKCS#1
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

	public String getAlgorithm() {
		return this.algorithm;
	}

	public MessageDigest getMessageDigestInstance() throws NoSuchAlgorithmException {
		if (null == this.algorithm) {
			return null;
		}
		return MessageDigest.getInstance(this.algorithm);
	}

	public boolean isEc() {
		return this.ec;
	}

	public boolean isP1363() {
		return this.p1363;
	}
}
