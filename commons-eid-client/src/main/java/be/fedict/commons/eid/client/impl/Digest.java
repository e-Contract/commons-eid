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

package be.fedict.commons.eid.client.impl;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public enum Digest {

	PLAIN_TEXT("2.16.56.1.2.1.3.1", new byte[]{0x30, (byte) 0xff, 0x30, 0x09,
			0x06, 0x07, 0x60, 0x38, 0x01, 0x02, 0x01, 0x03, 0x01, 0x04,
			(byte) 0xff}),

	SHA1("SHA1", new byte[]{0x30, 0x1f, 0x30, 0x07, 0x06, 0x05, 0x2b, 0x0e,
			0x03, 0x02, 0x1a, 0x04, 0x14}),

	SHA224("SHA224", new byte[]{0x30, 0x2b, 0x30, 0x0b, 0x06, 0x09, 0x60,
			(byte) 0x86, 0x48, 0x01, 0x65, 0x03, 0x04, 0x02, 0x04, 0x04, 0x1c}),

	SHA256("SHA256", new byte[]{0x30, 0x2f, 0x30, 0x0b, 0x06, 0x09, 0x60,
			(byte) 0x86, 0x48, 0x01, 0x65, 0x03, 0x04, 0x02, 0x01, 0x04, 0x20}),

	SHA384("SHA384", new byte[]{0x30, 0x3f, 0x30, 0x0b, 0x06, 0x09, 0x60,
			(byte) 0x86, 0x48, 0x01, 0x65, 0x03, 0x04, 0x02, 0x02, 0x04, 0x30}),

	SHA512("SHA512", new byte[]{0x30, 0x4f, 0x30, 0x0b, 0x06, 0x09, 0x60,
			(byte) 0x86, 0x48, 0x01, 0x65, 0x03, 0x04, 0x02, 0x03, 0x04, 0x40}),

	RIPEMD160("RIPEMD160", new byte[]{0x30, 0x1f, 0x30, 0x07, 0x06, 0x05, 0x2b,
			0x24, 0x03, 0x02, 0x01, 0x04, 0x14}),

	RIPEMD128("RIPEMD128", new byte[]{0x30, 0x1b, 0x30, 0x07, 0x06, 0x05, 0x2b,
			0x24, 0x03, 0x02, 0x02, 0x04, 0x10}),

	RIPEMD256("RIPEMD256", new byte[]{0x30, 0x2b, 0x30, 0x07, 0x06, 0x05, 0x2b,
			0x24, 0x03, 0x02, 0x03, 0x04, 0x20}),

	SHA1PSS("SHA1PSS", new byte[]{}, 0x10),

	SHA256PSS("SHA256PSS", new byte[]{}, 0x20);

	private static Map<String, Digest> digests;
	private final String name;
	private final byte[] prefix;
	private final byte algorithmReference;

	static {
		Digest.digests = new HashMap<String, Digest>();
		for (Digest digest : Digest.values())
			Digest.digests.put(digest.name, digest);
	}

	private Digest(String name, byte[] prefix, int algorithmReference) {
		this.name = name;
		this.prefix = prefix;
		this.algorithmReference = (byte) algorithmReference;
	}

	private Digest(String name, byte[] prefix) {
		this(name, prefix, 0x01); // default algorithm reference: PKCS#1
	}

	public byte[] getPrefix(int valueLength) {
		if (this.equals(PLAIN_TEXT)) {
			byte[] digestInfoPrefix = Arrays.copyOf(this.prefix,
					this.prefix.length);
			digestInfoPrefix[1] = (byte) (valueLength + 13);
			digestInfoPrefix[14] = (byte) valueLength;
			return digestInfoPrefix;
		}

		return this.prefix;
	}

	public static Digest byName(String name) {
		Digest digest = Digest.digests.get(name.toUpperCase().replace("-", ""));
		if (digest == null)
			throw new RuntimeException("digest not supported: " + name);
		return digest;
	}

	public byte getAlgorithmReference() {
		return algorithmReference;
	}
}
