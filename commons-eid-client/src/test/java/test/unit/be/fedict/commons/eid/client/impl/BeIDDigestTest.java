/*
 * Commons eID Project.
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

package test.unit.be.fedict.commons.eid.client.impl;

import java.security.MessageDigest;

import org.bouncycastle.asn1.ASN1ObjectIdentifier;
import org.bouncycastle.asn1.DERNull;
import org.bouncycastle.asn1.oiw.OIWObjectIdentifiers;
import org.bouncycastle.asn1.util.ASN1Dump;
import org.bouncycastle.asn1.x509.AlgorithmIdentifier;
import org.bouncycastle.asn1.x509.DigestInfo;
import org.bouncycastle.util.encoders.Hex;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BeIDDigestTest {

	private static final Logger LOGGER = LoggerFactory.getLogger(BeIDDigestTest.class);

	@Test
	public void testDigestInfoSHA1() throws Exception {
		byte[] data = "hello world".getBytes();
		MessageDigest messageDigest = MessageDigest.getInstance("SHA1");
		byte[] digest = messageDigest.digest(data);

		ASN1ObjectIdentifier hashAlgoId = OIWObjectIdentifiers.idSHA1;
		DigestInfo digestInfo = new DigestInfo(new AlgorithmIdentifier(hashAlgoId, DERNull.INSTANCE), digest);
		LOGGER.debug("ASN.1: {}", ASN1Dump.dumpAsString(digestInfo));
		LOGGER.debug("Hexadecimal: {}", Hex.toHexString(digestInfo.getEncoded()));
		LOGGER.debug("Java: {}", toJavaHex(digestInfo.getEncoded()));
	}

	private String toJavaHex(byte[] data) {
		StringBuffer stringBuffer = new StringBuffer();
		for (byte b : data) {
			stringBuffer.append(String.format("0x%02X, ", b));
		}
		return stringBuffer.toString();
	}
}
