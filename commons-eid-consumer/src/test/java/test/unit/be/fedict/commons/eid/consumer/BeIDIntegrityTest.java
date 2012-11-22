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

package test.unit.be.fedict.commons.eid.consumer;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import java.security.cert.X509Certificate;

import org.apache.commons.io.IOUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.Test;

import be.fedict.commons.eid.consumer.Address;
import be.fedict.commons.eid.consumer.BeIDIntegrity;
import be.fedict.commons.eid.consumer.Identity;

public class BeIDIntegrityTest {

	private static final Log LOG = LogFactory.getLog(BeIDIntegrityTest.class);

	@Test
	public void testIdentityIntegrity() throws Exception {
		// setup
		byte[] identityFile = IOUtils.toByteArray(BeIDIntegrityTest.class
				.getResourceAsStream("/test-identity.tlv"));
		byte[] identitySignatureFile = IOUtils
				.toByteArray(BeIDIntegrityTest.class
						.getResourceAsStream("/test-identity-sign.der"));
		byte[] rrnCertFile = IOUtils.toByteArray(BeIDIntegrityTest.class
				.getResourceAsStream("/test-rrn-cert.der"));
		BeIDIntegrity beIDIntegrity = new BeIDIntegrity();

		// operate
		X509Certificate rrnCert = beIDIntegrity.loadCertificate(rrnCertFile);
		Identity identity = beIDIntegrity.getVerifiedIdentity(identityFile,
				identitySignatureFile, rrnCert);

		// verify
		assertNotNull(identity);
		LOG.debug("name: " + identity.getFirstName());
		assertEquals("Alice Geldigekaart", identity.getFirstName());
	}

	@Test
	public void testIdentityIntegrityCorruption() throws Exception {
		// setup
		byte[] identityFile = IOUtils.toByteArray(BeIDIntegrityTest.class
				.getResourceAsStream("/test-identity.tlv"));
		byte[] identitySignatureFile = IOUtils
				.toByteArray(BeIDIntegrityTest.class
						.getResourceAsStream("/test-identity-sign.der"));
		byte[] rrnCertFile = IOUtils.toByteArray(BeIDIntegrityTest.class
				.getResourceAsStream("/test-rrn-cert.der"));
		BeIDIntegrity beIDIntegrity = new BeIDIntegrity();

		// setup: corrupt identity file
		identityFile[0] = 4;

		// operate
		X509Certificate rrnCert = beIDIntegrity.loadCertificate(rrnCertFile);

		// operate & verify
		try {
			beIDIntegrity.getVerifiedIdentity(identityFile,
					identitySignatureFile, rrnCert);
			fail();
		} catch (SecurityException e) {
			// expected
		}
	}

	@Test
	public void testPhotoIntegrity() throws Exception {
		// setup
		byte[] identityFile = IOUtils.toByteArray(BeIDIntegrityTest.class
				.getResourceAsStream("/test-identity.tlv"));
		byte[] identitySignatureFile = IOUtils
				.toByteArray(BeIDIntegrityTest.class
						.getResourceAsStream("/test-identity-sign.der"));
		byte[] rrnCertFile = IOUtils.toByteArray(BeIDIntegrityTest.class
				.getResourceAsStream("/test-rrn-cert.der"));
		byte[] photoData = IOUtils.toByteArray(BeIDIntegrityTest.class
				.getResourceAsStream("/test-photo.jpg"));
		BeIDIntegrity beIDIntegrity = new BeIDIntegrity();

		// operate
		X509Certificate rrnCert = beIDIntegrity.loadCertificate(rrnCertFile);
		Identity identity = beIDIntegrity.getVerifiedIdentity(identityFile,
				identitySignatureFile, photoData, rrnCert);

		// verify
		assertNotNull(identity);
		LOG.debug("name: " + identity.getFirstName());
		assertEquals("Alice Geldigekaart", identity.getFirstName());
	}

	@Test
	public void testPhotoIntegrityCorruption() throws Exception {
		// setup
		byte[] identityFile = IOUtils.toByteArray(BeIDIntegrityTest.class
				.getResourceAsStream("/test-identity.tlv"));
		byte[] identitySignatureFile = IOUtils
				.toByteArray(BeIDIntegrityTest.class
						.getResourceAsStream("/test-identity-sign.der"));
		byte[] rrnCertFile = IOUtils.toByteArray(BeIDIntegrityTest.class
				.getResourceAsStream("/test-rrn-cert.der"));
		byte[] photoData = IOUtils.toByteArray(BeIDIntegrityTest.class
				.getResourceAsStream("/test-photo.jpg"));
		BeIDIntegrity beIDIntegrity = new BeIDIntegrity();

		// setup: corrupt photo
		photoData[0] = 0;

		// operate
		X509Certificate rrnCert = beIDIntegrity.loadCertificate(rrnCertFile);

		// operate & verify
		try {
			beIDIntegrity.getVerifiedIdentity(identityFile,
					identitySignatureFile, photoData, rrnCert);
			fail();
		} catch (SecurityException e) {
			// expected
		}
	}

	@Test
	public void testAddressIntegrity() throws Exception {
		// setup
		byte[] identitySignatureFile = IOUtils
				.toByteArray(BeIDIntegrityTest.class
						.getResourceAsStream("/test-identity-sign.der"));
		byte[] rrnCertFile = IOUtils.toByteArray(BeIDIntegrityTest.class
				.getResourceAsStream("/test-rrn-cert.der"));
		byte[] addressFile = IOUtils.toByteArray(BeIDIntegrityTest.class
				.getResourceAsStream("/test-address.tlv"));
		byte[] addressSignatureFile = IOUtils
				.toByteArray(BeIDIntegrityTest.class
						.getResourceAsStream("/test-address-sign.der"));

		BeIDIntegrity beIDIntegrity = new BeIDIntegrity();

		// operate
		X509Certificate rrnCert = beIDIntegrity.loadCertificate(rrnCertFile);
		Address address = beIDIntegrity.getVerifiedAddress(addressFile,
				identitySignatureFile, addressSignatureFile, rrnCert);

		// verify
		assertNotNull(address);
		LOG.debug("ZIP: " + address.getZip());
		assertEquals("2000", address.getZip());
	}

	@Test
	public void testAddressIntegrityCorruption() throws Exception {
		// setup
		byte[] identitySignatureFile = IOUtils
				.toByteArray(BeIDIntegrityTest.class
						.getResourceAsStream("/test-identity-sign.der"));
		byte[] rrnCertFile = IOUtils.toByteArray(BeIDIntegrityTest.class
				.getResourceAsStream("/test-rrn-cert.der"));
		byte[] addressFile = IOUtils.toByteArray(BeIDIntegrityTest.class
				.getResourceAsStream("/test-address.tlv"));
		byte[] addressSignatureFile = IOUtils
				.toByteArray(BeIDIntegrityTest.class
						.getResourceAsStream("/test-address-sign.der"));

		// setup: corrupt address
		addressFile[0] = 123;

		BeIDIntegrity beIDIntegrity = new BeIDIntegrity();

		// operate
		X509Certificate rrnCert = beIDIntegrity.loadCertificate(rrnCertFile);

		// operate & verify
		try {
			beIDIntegrity.getVerifiedAddress(addressFile,
					identitySignatureFile, addressSignatureFile, rrnCert);
			fail();
		} catch (SecurityException e) {
			// expected
		}
	}
}
