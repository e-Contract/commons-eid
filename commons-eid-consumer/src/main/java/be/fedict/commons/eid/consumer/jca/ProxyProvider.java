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

package be.fedict.commons.eid.consumer.jca;

import java.security.NoSuchAlgorithmException;
import java.security.Provider;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * JCA security provider for proxy signatures.
 * <p/>
 * Usage:
 * 
 * <pre>
 * Security.addProvider(new ProxyProvider());
 * </pre>
 * 
 * @see ProxyKeyStore
 * @author Frank Cornelis
 * 
 */
public class ProxyProvider extends Provider {

	private static final Log LOG = LogFactory.getLog(ProxyProvider.class);

	public static final String NAME = "ProxyProvider";

	private static final long serialVersionUID = 1L;

	public ProxyProvider() {
		super(NAME, 1.0, "Proxy Signature Provider");
		putService(new ProxyService(this, "KeyStore", "ProxyBeID",
				ProxyKeyStore.class.getName()));

		final Map<String, String> signatureServiceAttributes = new HashMap<String, String>();
		signatureServiceAttributes.put("SupportedKeyClasses",
				ProxyPrivateKey.class.getName());
		putService(new ProxyService(this, "Signature", "SHA1withRSA",
				ProxySignature.class.getName(), signatureServiceAttributes));
		putService(new ProxyService(this, "Signature", "SHA256withRSA",
				ProxySignature.class.getName(), signatureServiceAttributes));
		putService(new ProxyService(this, "Signature", "SHA384withRSA",
				ProxySignature.class.getName(), signatureServiceAttributes));
		putService(new ProxyService(this, "Signature", "SHA512withRSA",
				ProxySignature.class.getName(), signatureServiceAttributes));
	}

	public static final class ProxyService extends Service {

		public ProxyService(final Provider provider, final String type,
				final String algorithm, final String className) {
			super(provider, type, algorithm, className, null, null);
		}

		public ProxyService(final Provider provider, final String type,
				final String algorithm, final String className,
				final Map<String, String> attributes) {
			super(provider, type, algorithm, className, null, attributes);
		}

		@Override
		public Object newInstance(final Object constructorParameter)
				throws NoSuchAlgorithmException {
			LOG.debug("type: " + super.getType());
			LOG.debug("newInstance: " + constructorParameter);
			if (super.getType().equals("Signature")) {
				return new ProxySignature(this.getAlgorithm());
			}
			return super.newInstance(constructorParameter);
		}

		@Override
		public boolean supportsParameter(final Object parameter) {
			LOG.debug("supportsParameter: " + parameter);
			return super.supportsParameter(parameter);
		}
	}
}
