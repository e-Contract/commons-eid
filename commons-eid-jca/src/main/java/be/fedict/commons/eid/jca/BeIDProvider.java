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

package be.fedict.commons.eid.jca;

import java.security.NoSuchAlgorithmException;
import java.security.Provider;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class BeIDProvider extends Provider {

	private static final long serialVersionUID = 1L;

	public static final String NAME = "BeIDProvider";

	private static final Log LOG = LogFactory.getLog(BeIDProvider.class);

	public BeIDProvider() {
		super(NAME, 1.0, "BeID Provider");

		putService(new BeIDService(this, "KeyStore", "BeID", BeIDKeyStore.class
				.getName()));

		Map<String, String> signatureServiceAttributes = new HashMap<String, String>();
		signatureServiceAttributes.put("SupportedKeyClasses",
				BeIDPrivateKey.class.getName());
		putService(new BeIDService(this, "Signature", "SHA1withRSA",
				BeIDSignature.class.getName(), signatureServiceAttributes));
		putService(new BeIDService(this, "Signature", "SHA256withRSA",
				BeIDSignature.class.getName(), signatureServiceAttributes));
		putService(new BeIDService(this, "Signature", "SHA384withRSA",
				BeIDSignature.class.getName(), signatureServiceAttributes));
		putService(new BeIDService(this, "Signature", "SHA512withRSA",
				BeIDSignature.class.getName(), signatureServiceAttributes));
	}

	public static final class BeIDService extends Service {

		public BeIDService(Provider provider, String type, String algorithm,
				String className) {
			super(provider, type, algorithm, className, null, null);
		}

		public BeIDService(Provider provider, String type, String algorithm,
				String className, Map<String, String> attributes) {
			super(provider, type, algorithm, className, null, attributes);
		}

		@Override
		public Object newInstance(Object constructorParameter)
				throws NoSuchAlgorithmException {
			LOG.debug("newInstance");
			if (super.getType().equals("Signature")) {
				return new BeIDSignature(this.getAlgorithm());
			}
			return super.newInstance(constructorParameter);
		}

		@Override
		public boolean supportsParameter(Object parameter) {
			LOG.debug("supportedParameter: " + parameter);
			return super.supportsParameter(parameter);
		}
	}
}
