package test.integ.be.fedict.commons.eid.consumer;

import java.math.BigInteger;

import org.junit.Test;

import be.fedict.commons.eid.consumer.CardData;
import be.fedict.commons.eid.consumer.tlv.ByteArrayParser;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertFalse;

public class ByteArrayParserTest {

	@Test
	public void testByteArrayParser() {
		final byte[] cardDataBytes = new BigInteger(
				"534c494e33660013930d2061c018063fd0004801011100020001010f", 16)
				.toByteArray();
		final byte[] serialExpected = new byte[16];
		final byte[] chipSerialExpected = new byte[12];
		System.arraycopy(cardDataBytes, 0, serialExpected, 0, 16);
		System.arraycopy(cardDataBytes, 4, chipSerialExpected, 0, 12);

		final CardData cardData = ByteArrayParser.parse(cardDataBytes,
				CardData.class);
		assertEquals(cardData.applicationInterfaceVersion, 0);
		assertEquals(cardData.applicationLifeCycle, 15);
		assertEquals(cardData.applicationVersion, 17);
		assertEquals(cardData.componentCode, 208);
		assertEquals(cardData.globalOSVersion, 2);
		assertEquals(cardData.keyExchangeVersion, 1);
		assertEquals(cardData.osNumber, 0);
		assertEquals(cardData.osVersion, 72);
		assertEquals(cardData.pkcs1Support, 1);
		assertArrayEquals(cardData.serialNumber, serialExpected);
		assertEquals(cardData.axaltoReservedNumber, 21324);
		assertEquals(cardData.chipManufacturer, 18766);
		assertArrayEquals(cardData.chipSerialNumber, chipSerialExpected);
		assertEquals(cardData.softmaskNumber, 1);
		assertEquals(cardData.softmaskVersion, 1);
		assertTrue(cardData.isRSASSAPKCS115Supported());
		assertFalse(cardData.isRSASSAPSSSupported());
		assertFalse(cardData.isRSAESPKCS115Supported());
		assertFalse(cardData.isRSAESOAEPSupported());
		assertFalse(cardData.isRSAKEMSupported());
		assertTrue(cardData.isDeactivated());
		assertFalse(cardData.isActivated());
		assertFalse(cardData.isLocked());
	}

}
