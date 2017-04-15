/*
 * Commons eID Project.
 * Copyright (C) 2008-2013 FedICT.
 * Copyright (C) 2017 e-Contract.be BVBA.
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

package be.fedict.commons.eid.jca;

import java.security.KeyStore;
import java.security.NoSuchAlgorithmException;
import java.security.Provider;
import java.security.SecureRandom;
import java.security.Signature;
import java.util.HashMap;
import java.util.Map;

import javax.net.ssl.KeyManagerFactory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The JCA security provider. Provides an eID based {@link KeyStore},
 * {@link Signature}, {@link KeyManagerFactory}, and {@link SecureRandom}.
 * <p/>
 * Usage:
 * 
 * <pre>
 * import java.security.Security;
 * import be.fedict.commons.eid.jca.BeIDProvider;
 * 
 * ...
 * Security.addProvider(new BeIDProvider());
 * </pre>
 * 
 * @see BeIDKeyStore
 * @see BeIDSignature
 * @see BeIDKeyManagerFactory
 * @see BeIDSecureRandom
 * @author Frank Cornelis
 * 
 */
public class BeIDProvider extends Provider {

	private static final long serialVersionUID = 1L;

	public static final String NAME = "BeIDProvider";

	private static final Logger LOGGER = LoggerFactory.getLogger(BeIDProvider.class);

	public BeIDProvider() {
		super(NAME, 1.0, "BeID Provider");

		putService(new BeIDService(this, "KeyStore", "BeID", BeIDKeyStore.class.getName()));

		final Map<String, String> signatureServiceAttributes = new HashMap<String, String>();
		signatureServiceAttributes.put("SupportedKeyClasses", BeIDPrivateKey.class.getName());
		putService(new BeIDService(this, "Signature", "SHA1withRSA", BeIDSignature.class.getName(),
				signatureServiceAttributes));
		putService(new BeIDService(this, "Signature", "SHA224withRSA", BeIDSignature.class.getName(),
				signatureServiceAttributes));
		putService(new BeIDService(this, "Signature", "SHA256withRSA", BeIDSignature.class.getName(),
				signatureServiceAttributes));
		putService(new BeIDService(this, "Signature", "SHA384withRSA", BeIDSignature.class.getName(),
				signatureServiceAttributes));
		putService(new BeIDService(this, "Signature", "SHA512withRSA", BeIDSignature.class.getName(),
				signatureServiceAttributes));
		putService(new BeIDService(this, "Signature", "NONEwithRSA", BeIDSignature.class.getName(),
				signatureServiceAttributes));
		putService(new BeIDService(this, "Signature", "RIPEMD128withRSA", BeIDSignature.class.getName(),
				signatureServiceAttributes));
		putService(new BeIDService(this, "Signature", "RIPEMD160withRSA", BeIDSignature.class.getName(),
				signatureServiceAttributes));
		putService(new BeIDService(this, "Signature", "RIPEMD256withRSA", BeIDSignature.class.getName(),
				signatureServiceAttributes));
		putService(new BeIDService(this, "Signature", "SHA1withRSAandMGF1", BeIDSignature.class.getName(),
				signatureServiceAttributes));
		putService(new BeIDService(this, "Signature", "SHA256withRSAandMGF1", BeIDSignature.class.getName(),
				signatureServiceAttributes));

		putService(new BeIDService(this, "KeyManagerFactory", "BeID", BeIDKeyManagerFactory.class.getName()));

		putService(new BeIDService(this, "SecureRandom", "BeID", BeIDSecureRandom.class.getName()));
	}

	/**
	 * Inner class used by {@link BeIDProvider}.
	 * 
	 * @author Frank Cornelis
	 * 
	 */
	private static final class BeIDService extends Service {

		public BeIDService(final Provider provider, final String type, final String algorithm, final String className) {
			super(provider, type, algorithm, className, null, null);
		}

		public BeIDService(final Provider provider, final String type, final String algorithm, final String className,
				final Map<String, String> attributes) {
			super(provider, type, algorithm, className, null, attributes);
		}

		@Override
		public Object newInstance(final Object constructorParameter) throws NoSuchAlgorithmException {
			LOGGER.debug("newInstance: {}", super.getType());
			if (super.getType().equals("Signature")) {
				return new BeIDSignature(this.getAlgorithm());
			}
			return super.newInstance(constructorParameter);
		}

		@Override
		public boolean supportsParameter(final Object parameter) {
			LOGGER.debug("supportedParameter: {}", parameter);
			return super.supportsParameter(parameter);
		}
	}
}
