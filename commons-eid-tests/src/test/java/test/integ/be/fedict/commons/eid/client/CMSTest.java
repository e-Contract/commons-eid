/*
 * Commons eID Project.
 * Copyright (C) 2013-2024 e-Contract.be BV.
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

package test.integ.be.fedict.commons.eid.client;

import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.Security;
import java.security.cert.X509Certificate;
import java.security.interfaces.ECPrivateKey;

import org.bouncycastle.cms.CMSProcessableByteArray;
import org.bouncycastle.cms.CMSSignedData;
import org.bouncycastle.cms.CMSSignedDataGenerator;
import org.bouncycastle.cms.CMSTypedData;
import org.bouncycastle.cms.jcajce.JcaSignerInfoGeneratorBuilder;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.bouncycastle.operator.jcajce.JcaDigestCalculatorProviderBuilder;
import org.junit.jupiter.api.Test;

import be.fedict.commons.eid.jca.BeIDProvider;

public class CMSTest {

	@Test
	public void testCMSSignature() throws Exception {
		Security.insertProviderAt(new BeIDProvider(), 1);
		Security.addProvider(new BouncyCastleProvider());

		KeyStore keyStore = KeyStore.getInstance("BeID");
		keyStore.load(null);
		PrivateKey privateKey = (PrivateKey) keyStore.getKey("Authentication", null);
		String signatureAlgo;
		if (privateKey instanceof ECPrivateKey) {
			signatureAlgo = "SHA256withECDSA";
		} else {
			signatureAlgo = "SHA256withRSA";
		}
		X509Certificate certificate = (X509Certificate) keyStore.getCertificate("Authentication");

		CMSTypedData msg = new CMSProcessableByteArray("Hello world!".getBytes());

		CMSSignedDataGenerator gen = new CMSSignedDataGenerator();
		ContentSigner contentSigner = new JcaContentSignerBuilder(signatureAlgo).build(privateKey);

		gen.addSignerInfoGenerator(
				new JcaSignerInfoGeneratorBuilder(new JcaDigestCalculatorProviderBuilder().setProvider("BC").build())
						.build(contentSigner, certificate));

		CMSSignedData sigData = gen.generate(msg, false);
	}
}
