/*
 * Commons eID Project.
 * Copyright (C) 2008-2013 FedICT.
 * Copyright (C) 2017-2020 e-Contract.be BV.
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

package test.unit.be.fedict.commons.eid.consumer.tlv;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.Calendar;
import java.util.GregorianCalendar;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import be.fedict.commons.eid.consumer.Address;
import be.fedict.commons.eid.consumer.DateMask;
import be.fedict.commons.eid.consumer.DocumentType;
import be.fedict.commons.eid.consumer.Gender;
import be.fedict.commons.eid.consumer.Identity;
import be.fedict.commons.eid.consumer.SpecialOrganisation;
import be.fedict.commons.eid.consumer.SpecialStatus;
import be.fedict.commons.eid.consumer.WorkPermit;
import be.fedict.commons.eid.consumer.tlv.TlvField;
import be.fedict.commons.eid.consumer.tlv.TlvParser;

public class TlvParserTest {

	private static final Logger LOGGER = LoggerFactory.getLogger(TlvParserTest.class);

	@Test
	public void parseIdentityFile() throws Exception {
		// setup
		final InputStream idInputStream = TlvParserTest.class.getResourceAsStream("/id-alice.tlv");
		final byte[] idFile = IOUtils.toByteArray(idInputStream);

		// operate
		final Identity identity = TlvParser.parse(idFile, Identity.class);

		// verify
		assertNotNull(identity);
		LOGGER.debug("name: {}", identity.name);
		assertEquals("SPECIMEN", identity.name);
		LOGGER.debug("first name: {}", identity.firstName);
		assertEquals("Alice Geldigekaart2266", identity.firstName);
		LOGGER.debug("card number: {}", identity.cardNumber);
		assertEquals("000000226635", identity.cardNumber);
		LOGGER.debug("card validity date begin: {}", identity.cardValidityDateBegin.getTime());
		assertEquals(new GregorianCalendar(2005, 7, 8), identity.cardValidityDateBegin);
		LOGGER.debug("card validity date end: {}", identity.cardValidityDateEnd.getTime());
		assertEquals(new GregorianCalendar(2010, 7, 8), identity.cardValidityDateEnd);
		LOGGER.debug("Card Delivery Municipality: {}", identity.cardDeliveryMunicipality);
		assertEquals("Certipost Specimen", identity.cardDeliveryMunicipality);
		LOGGER.debug("national number: {}", identity.nationalNumber);
		assertEquals("71715100070", identity.nationalNumber);
		LOGGER.debug("middle name: {}", identity.middleName);
		assertEquals("A", identity.middleName);
		LOGGER.debug("nationality: {}", identity.nationality);
		assertEquals("Belg", identity.nationality);
		LOGGER.debug("place of birth: {}", identity.placeOfBirth);
		assertEquals("Hamont-Achel", identity.placeOfBirth);
		LOGGER.debug("gender: {}", identity.gender);
		assertEquals(Gender.FEMALE, identity.gender);
		assertNotNull(identity.dateOfBirth);
		LOGGER.debug("date of birth: {}", identity.dateOfBirth.getTime());
		assertEquals(new GregorianCalendar(1971, 0, 1), identity.dateOfBirth);
		assertEquals(DateMask.YYYY_MM_DD, identity.dateOfBirthMask);
		LOGGER.debug("special status: {}", identity.specialStatus);
		assertEquals(SpecialStatus.NO_STATUS, identity.specialStatus);
		assertNull(identity.getSpecialOrganisation());

		assertNotNull(identity.getData());
	}

	@Test
	public void parseIdentityFile2() throws Exception {
		// setup
		final InputStream idInputStream = TlvParserTest.class.getResourceAsStream("/id-alice-2.tlv");
		final byte[] idFile = IOUtils.toByteArray(idInputStream);

		// operate
		final Identity identity = TlvParser.parse(idFile, Identity.class);

		// verify
		assertNotNull(identity);
		LOGGER.debug("name: {}", identity.name);
		assertEquals("SPECIMEN", identity.name);
		LOGGER.debug("first name: {}", identity.firstName);
		assertEquals("Alice Geldigekaart0126", identity.firstName);
		LOGGER.debug("card number: {}", identity.cardNumber);
		assertEquals("000000012629", identity.cardNumber);
		LOGGER.debug("card validity date begin: {}", identity.cardValidityDateBegin.getTime());
		assertEquals(new GregorianCalendar(2003, 9, 24), identity.cardValidityDateBegin);
		LOGGER.debug("card validity date end: {}", identity.cardValidityDateEnd.getTime());
		assertEquals(new GregorianCalendar(2008, 9, 24), identity.cardValidityDateEnd);
		LOGGER.debug("Card Delivery Municipality: {}", identity.cardDeliveryMunicipality);
		assertEquals("Certipost Specimen", identity.cardDeliveryMunicipality);
		LOGGER.debug("national number: {}", identity.nationalNumber);
		assertEquals("71715100070", identity.nationalNumber);
		LOGGER.debug("middle name: {}", identity.middleName);
		assertEquals("A", identity.middleName);
		LOGGER.debug("nationality: {}", identity.nationality);
		assertEquals("Belg", identity.nationality);
		LOGGER.debug("place of birth: {}", identity.placeOfBirth);
		assertEquals("Hamont-Achel", identity.placeOfBirth);
		LOGGER.debug("gender: {}", identity.gender);
		assertEquals(Gender.FEMALE, identity.gender);
		assertNotNull(identity.dateOfBirth);
		LOGGER.debug("date of birth: {}", identity.dateOfBirth.getTime());
		assertEquals(new GregorianCalendar(1971, 0, 1), identity.dateOfBirth);
		assertEquals(DateMask.YYYY_MM_DD, identity.dateOfBirthMask);
		assertNull(identity.getSpecialOrganisation());
	}

	@Test
	public void testYellowCane() throws Exception {
		// setup
		final byte[] idFile = IOUtils.toByteArray(TlvParserTest.class.getResourceAsStream("/yellow-cane.tlv"));

		// operate
		final Identity identity = TlvParser.parse(idFile, Identity.class);

		// verify
		LOGGER.debug("special status: {}", identity.specialStatus);
		assertEquals(SpecialStatus.YELLOW_CANE, identity.specialStatus);
		assertTrue(identity.specialStatus.hasBadSight());
		assertTrue(identity.specialStatus.hasYellowCane());
		assertFalse(identity.specialStatus.hasWhiteCane());
	}

	@Test
	public void testWhiteCane() throws Exception {
		// setup
		final byte[] idFile = IOUtils.toByteArray(TlvParserTest.class.getResourceAsStream("/white-cane.tlv"));

		// operate
		final Identity identity = TlvParser.parse(idFile, Identity.class);

		// verify
		LOGGER.debug("special status: {}", identity.specialStatus);
		assertEquals(SpecialStatus.WHITE_CANE, identity.specialStatus);
		assertTrue(identity.specialStatus.hasBadSight());
		assertTrue(identity.specialStatus.hasWhiteCane());
		assertFalse(identity.specialStatus.hasYellowCane());
	}

	@Test
	public void testExtendedMinority() throws Exception {
		// setup
		final byte[] idFile = IOUtils.toByteArray(TlvParserTest.class.getResourceAsStream("/extended-minority.tlv"));

		// operate
		final Identity identity = TlvParser.parse(idFile, Identity.class);

		// verify
		LOGGER.debug("special status: {}", identity.specialStatus);
		assertEquals(SpecialStatus.EXTENDED_MINORITY, identity.specialStatus);
		assertFalse(identity.specialStatus.hasBadSight());
		assertTrue(identity.specialStatus.hasExtendedMinority());
		LOGGER.debug("special organisation: \"{}\"", identity.getSpecialOrganisation());
		assertNull(identity.getSpecialOrganisation());
	}

	@Test
	public void parseAddressFile() throws Exception {
		// setup
		final InputStream addressInputStream = TlvParserTest.class.getResourceAsStream("/address-alice.tlv");
		final byte[] addressFile = IOUtils.toByteArray(addressInputStream);

		// operate
		final Address address = TlvParser.parse(addressFile, Address.class);

		// verify
		assertNotNull(address);
		LOGGER.debug("street and number: {}", address.streetAndNumber);
		assertEquals("Meirplaats 1 bus 1", address.streetAndNumber);
		LOGGER.debug("zip: {}", address.zip);
		assertEquals("2000", address.zip);
		LOGGER.debug("municipality: {}", address.municipality);
		assertEquals("Antwerpen", address.municipality);

		assertNotNull(address.getData());
	}

	@Test
	public void testYearOnlyDate() throws Exception {
		final byte[] yearOnlyTLV = new byte[] { 12, 4, '1', '9', '8', '4' };
		final Identity identity = TlvParser.parse(yearOnlyTLV, Identity.class);
		assertEquals(1984, identity.getDateOfBirth().get(Calendar.YEAR));
		assertEquals(DateMask.YYYY, identity.dateOfBirthMask);
	}

	@Test
	public void testInvalidDateTruncatedYear() throws Exception {
		final byte[] yearOnlyTLV = new byte[] { 12, 3, '9', '8', '4' };

		try {
			TlvParser.parse(yearOnlyTLV, Identity.class);
			fail("Parser failed to throw exception at invalid date");
		} catch (final RuntimeException rte) {
			// expected
		}
	}

	@Test
	public void testInvalidDateUnknownMonth() throws Exception {
		final byte[] yearOnlyTLV = new byte[] { 12, 12, '2', '0', ' ', 'J', 'U', 'N', 'O', ' ', '1', '9', '6', '4' };

		try {
			TlvParser.parse(yearOnlyTLV, Identity.class);
			fail("Parser failed to throw exception at invalid month");
		} catch (final RuntimeException rte) {
			// expected
		}
	}

	@Test
	public void testInvalidDateMissingDayOfMonth() throws Exception {
		final byte[] yearOnlyTLV = new byte[] { 12, 8, 'S', 'E', 'P', ' ', '1', '9', '6', '4' };

		try {
			TlvParser.parse(yearOnlyTLV, Identity.class);
			fail("Parser failed to throw exception at missing day of month");
		} catch (final RuntimeException rte) {
			// expected
		}
	}

	public static class LargeField {
		@TlvField(1)
		public byte[] field1;

		@TlvField(100)
		public byte[] field2;
	}

	@Test
	public void testLargeField() throws Exception {
		// setup
		final ByteArrayOutputStream byteStream = new ByteArrayOutputStream();

		// field length < 0x80
		byteStream.write(1); // tag
		byteStream.write(0x7f); // length
		for (int i = 0; i < 0x7f; i++) {
			byteStream.write(0x12); // data
		}

		// field length = 0x80
		byteStream.write(2); // tag
		byteStream.write(0x81); // length
		byteStream.write(0x00);
		for (int i = 0; i < 0x80; i++) {
			byteStream.write(0x34); // data
		}

		// field length = 0x3fff
		byteStream.write(3); // tag
		byteStream.write(0xff); // length
		byteStream.write(0x7f);
		for (int i = 0; i < 0x3fff; i++) {
			byteStream.write(0x56); // data
		}

		// field length = 0x4000
		byteStream.write(4); // tag
		byteStream.write(0x81); // length
		byteStream.write(0x80);
		byteStream.write(0x00);
		for (int i = 0; i < 0x4000; i++) {
			byteStream.write(0x78); // data
		}

		// our check field
		byteStream.write(100);
		byteStream.write(4);
		byteStream.write(0xca);
		byteStream.write(0xfe);
		byteStream.write(0xba);
		byteStream.write(0xbe);
		final byte[] file = byteStream.toByteArray();

		// operate
		final LargeField largeField = TlvParser.parse(file, LargeField.class);

		// verify
		assertEquals(0x7f, largeField.field1.length);
		assertArrayEquals(new byte[] { (byte) 0xca, (byte) 0xfe, (byte) 0xba, (byte) 0xbe }, largeField.field2);
	}

	public static class MiddlewareEIDFile {
		@TlvField(1)
		public byte[] identityFile;
	}

	@Test
	public void testParseMiddlewareEIDFile() throws Exception {
		final byte[] eidFile = IOUtils.toByteArray(TlvParserTest.class.getResourceAsStream("/71715100070.eid"));
		final MiddlewareEIDFile middlewareEIDFile = TlvParser.parse(eidFile, MiddlewareEIDFile.class);
		final Identity identity = TlvParser.parse(middlewareEIDFile.identityFile, Identity.class);
		LOGGER.debug("identity: {}", identity);
		LOGGER.debug("identity NRN: {}", identity.nationalNumber);
		assertEquals("71715100070", identity.nationalNumber);
		LOGGER.debug("special status: {}", identity.specialStatus);
	}

	@Test
	public void testForeignerIdentityFile() throws Exception {
		// setup
		final InputStream inputStream = TlvParserTest.class.getResourceAsStream("/id-foreigner.tlv");
		final byte[] identityData = IOUtils.toByteArray(inputStream);

		// operate
		final Identity identity = TlvParser.parse(identityData, Identity.class);

		// verify
		LOGGER.debug("name: {}", identity.getName());
		LOGGER.debug("first name: {}", identity.getFirstName());
		LOGGER.debug("document type: {}", identity.getDocumentType());
		assertEquals(DocumentType.FOREIGNER_E_PLUS, identity.getDocumentType());
		assertNotNull(identity.getDuplicate());
		LOGGER.debug("duplicate: {}", identity.getDuplicate());
		LOGGER.debug("special organisation: \"{}\"", identity.getSpecialOrganisation());
		assertEquals(SpecialOrganisation.UNSPECIFIED, identity.getSpecialOrganisation());
	}

	@Test
	public void testGermanIdentityFileDoB() throws Exception {
		// setup
		final byte[] idFileCaseInTheField = new byte[] { 12, 12, '2', '3', '.', 'S', 'E', 'P', '.', ' ', '1', '9', '8',
				'2' };

		// operate
		final Identity identity = TlvParser.parse(idFileCaseInTheField, Identity.class);

		// verify
		assertNotNull(identity.getDateOfBirth());
		LOGGER.debug("date of birth: {}", identity.getDateOfBirth().getTime());

		final byte[] idFile = new byte[] { 12, 11, '2', '3', '.', 'S', 'E', 'P', '.', '1', '9', '8', '2' };
		final Identity identity2 = TlvParser.parse(idFile, Identity.class);
		assertEquals(identity.getDateOfBirth(), identity2.getDateOfBirth());
	}

	@Test
	public void testIdentityFileDoBYearOnlyWithSpaces() throws Exception {
		// setup
		final byte[] idFile = new byte[] { 12, 12, ' ', ' ', ' ', ' ', ' ', ' ', ' ', ' ', '1', '9', '6', '2' };

		// operate
		final Identity identity = TlvParser.parse(idFile, Identity.class);

		// verify
		assertNotNull(identity.getDateOfBirth());
		LOGGER.debug("date of birth: {}", identity.getDateOfBirth().getTime());
		assertEquals(1962, identity.getDateOfBirth().get(Calendar.YEAR));
		assertEquals(0, identity.getDateOfBirth().get(Calendar.MONTH));
		assertEquals(1, identity.getDateOfBirth().get(Calendar.DAY_OF_MONTH));
	}

	@Test
	public void testParseOldIdentityFile() throws Exception {
		// setup
		final InputStream inputStream = TlvParserTest.class.getResourceAsStream("/old-eid.txt");
		final byte[] base64IdentityData = IOUtils.toByteArray(inputStream);
		final byte[] identityData = Base64.decodeBase64(base64IdentityData);

		// operate
		final Identity identity = TlvParser.parse(identityData, Identity.class);

		// verify
		LOGGER.debug("name: {}", identity.getName());
		LOGGER.debug("first name: {}", identity.getFirstName());
		LOGGER.debug("document type: {}", identity.getDocumentType());
		LOGGER.debug("card validity date begin: {}", identity.getCardValidityDateBegin().getTime());
		assertEquals(DocumentType.BELGIAN_CITIZEN, identity.getDocumentType());
	}

	@Test
	public void testParseNewIdentityFile() throws Exception {
		// setup
		final InputStream inputStream = TlvParserTest.class.getResourceAsStream("/new-eid.txt");
		final byte[] base64IdentityData = IOUtils.toByteArray(inputStream);
		final byte[] identityData = Base64.decodeBase64(base64IdentityData);

		// operate
		final Identity identity = TlvParser.parse(identityData, Identity.class);

		// verify
		LOGGER.debug("name: {}", identity.getName());
		LOGGER.debug("first name: {}", identity.getFirstName());
		LOGGER.debug("card validity date begin: {}", identity.getCardValidityDateBegin().getTime());
		LOGGER.debug("document type: {}", identity.getDocumentType());
		assertEquals(DocumentType.BELGIAN_CITIZEN, identity.getDocumentType());
		assertNull(identity.getDuplicate());
		assertFalse(identity.isMemberOfFamily());
	}

	@Test
	public void testHCard() throws Exception {
		// setup
		final InputStream inputStream = TlvParserTest.class.getResourceAsStream("/h-card.tlv");
		final byte[] identityData = IOUtils.toByteArray(inputStream);

		// operate
		final Identity identity = TlvParser.parse(identityData, Identity.class);

		// verify
		LOGGER.debug("document type: {}", identity.getDocumentType());
		assertEquals(DocumentType.EUROPEAN_BLUE_CARD_H, identity.getDocumentType());
		LOGGER.debug("duplicate: {}", identity.getDuplicate());
		assertEquals("01", identity.getDuplicate());
		assertTrue(identity.isMemberOfFamily());
		LOGGER.debug("special organisation: \"{}\"", identity.getSpecialOrganisation());
		assertEquals(SpecialOrganisation.UNSPECIFIED, identity.getSpecialOrganisation());
	}

	@Test
	public void testDuplicate02() throws Exception {
		// setup
		final InputStream inputStream = TlvParserTest.class.getResourceAsStream("/duplicate-02.tlv");
		final byte[] identityData = IOUtils.toByteArray(inputStream);

		// operate
		final Identity identity = TlvParser.parse(identityData, Identity.class);

		// verify
		LOGGER.debug("document type: {}", identity.getDocumentType());
		assertEquals(DocumentType.FOREIGNER_A, identity.getDocumentType());
		LOGGER.debug("duplicate: {}", identity.getDuplicate());
		assertEquals("02", identity.getDuplicate());
		LOGGER.debug("member of family: {}", identity.isMemberOfFamily());
		assertTrue(identity.isMemberOfFamily());
		LOGGER.debug("special organisation: \"{}\"", identity.getSpecialOrganisation());
		assertEquals(SpecialOrganisation.RESEARCHER, identity.getSpecialOrganisation());
	}

	@Test
	public void parseDateAndCountry() throws Exception {
		// setup
		final InputStream idInputStream = TlvParserTest.class.getResourceAsStream("/dateandcountry.tlv");
		final byte[] idFile = IOUtils.toByteArray(idInputStream);

		// operate
		final Identity identity = TlvParser.parse(idFile, Identity.class);
		LOGGER.debug("date and country: {}", identity.getDateAndCountryOfProtection());
		assertEquals("13.08.2014-IT", identity.getDateAndCountryOfProtection());
	}

	@Test
	public void testTrimData() throws Exception {
		// setup
		final InputStream addressInputStream = TlvParserTest.class.getResourceAsStream("/address-fcorneli.tlv");
		final byte[] addressFile = IOUtils.toByteArray(addressInputStream);

		// operate
		final Address address = TlvParser.parse(addressFile, Address.class);

		// verify
		assertNotNull(address);
		LOGGER.debug("street and number: {}", address.streetAndNumber);
		assertEquals("Elfbunderslaan 76", address.streetAndNumber);
	}

	@Test
	public void testForeignerWorkPermit() throws Exception {
		// setup
		final InputStream idInputStream = TlvParserTest.class.getResourceAsStream("/idFile-foreigner.dat");
		final byte[] idFile = IOUtils.toByteArray(idInputStream);

		// operate
		final Identity identity = TlvParser.parse(idFile, Identity.class);
		assertTrue(identity.isMemberOfFamily());
		LOGGER.debug("document type: {}", identity.getDocumentType());
		LOGGER.debug("work permit: {}", identity.getWorkPermit());
		assertEquals(WorkPermit.JOB_MARKET_NONE, identity.getWorkPermit());
		assertEquals("9", identity.getWorkPermit().getKey());
		assertEquals("", identity.getEmployerVATNumber1());
		assertEquals("", identity.getEmployerVATNumber2());
		assertEquals("", identity.getRegionalFileNumber());
	}
}
