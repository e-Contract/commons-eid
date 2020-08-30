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

package test.unit.be.fedict.commons.eid.consumer;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.math.BigInteger;

import org.junit.Test;

import be.fedict.commons.eid.consumer.CardData;
import be.fedict.commons.eid.consumer.tlv.ByteArrayField;
import be.fedict.commons.eid.consumer.tlv.ByteArrayParser;

public class ByteArrayParserTest {

	@Test
	public void testByteArrayParser() {
		final byte[] cardDataBytes = new BigInteger("534c494e33660013930d2061c018063fd0004801011100020001010f", 16)
				.toByteArray();
		final byte[] serialExpected = new byte[16];
		final byte[] chipSerialExpected = new byte[12];
		System.arraycopy(cardDataBytes, 0, serialExpected, 0, 16);
		System.arraycopy(cardDataBytes, 4, chipSerialExpected, 0, 12);

		final CardData cardData = ByteArrayParser.parse(cardDataBytes, CardData.class);
		assertEquals(0, cardData.applicationInterfaceVersion);
		assertEquals(15, cardData.applicationLifeCycle);
		assertEquals(17, cardData.applicationVersion);
		assertEquals(208, cardData.componentCode);
		assertEquals(2, cardData.globalOSVersion);
		assertEquals(1, cardData.keyExchangeVersion);
		assertEquals(0, cardData.osNumber);
		assertEquals(72, cardData.osVersion);
		assertEquals(1, cardData.pkcs1Support);
		assertArrayEquals(serialExpected, cardData.serialNumber);
		assertEquals(21324, cardData.axaltoReservedNumber);
		assertEquals(18766, cardData.chipManufacturer);
		assertArrayEquals(chipSerialExpected, cardData.chipSerialNumber);
		assertEquals(1, cardData.softmaskNumber);
		assertEquals(1, cardData.softmaskVersion);
		assertTrue(cardData.isRSASSAPKCS115Supported());
		assertFalse(cardData.isRSASSAPSSSupported());
		assertFalse(cardData.isRSAESPKCS115Supported());
		assertFalse(cardData.isRSAESOAEPSupported());
		assertFalse(cardData.isRSAKEMSupported());
		assertTrue(cardData.isDeactivated());
		assertFalse(cardData.isActivated());
		assertFalse(cardData.isLocked());
	}

	@Test
	public void testIntegerParser() {
		final byte[] data = new byte[] { 0x00, 0x01 };
		TestData result = ByteArrayParser.parse(data, TestData.class);
		assertNotNull(result);
		assertEquals(new Integer(1), result.field);
		assertNull(result.unavailableField);
	}

	public final static class TestData {
		@ByteArrayField(offset = 0, length = 2)
		public Integer field;

		@ByteArrayField(offset = 2, length = 1)
		public Integer unavailableField;
	}
}
