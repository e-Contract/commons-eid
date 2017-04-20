/*
 * Commons eID Project.
 * Copyright (C) 2012-2013 FedICT.
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

package be.fedict.commons.eid.jca.ssl;

import java.security.InvalidAlgorithmParameterException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;

import javax.net.ssl.KeyManager;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.KeyManagerFactorySpi;
import javax.net.ssl.ManagerFactoryParameters;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * eID specific {@link KeyManagerFactory}. Can be used for mutual TLS
 * authentication.
 * <p/>
 * Usage:
 * 
 * <pre>
 * import javax.net.ssl.KeyManagerFactory;
 * import javax.net.ssl.SSLContext;
 * ...
 * KeyManagerFactory keyManagerFactory = KeyManagerFactory.getInstance(&quot;BeID&quot;);
 * SSLContext sslContext = SSLContext.getInstance(&quot;TLS&quot;);
 * sslContext.init(keyManagerFactory.getKeyManagers(), ..., ...);
 * </pre>
 * 
 * @see BeIDX509KeyManager
 * @see BeIDManagerFactoryParameters
 * @author Frank Cornelis
 * 
 */
public class BeIDKeyManagerFactory extends KeyManagerFactorySpi {

	private static final Logger LOGGER = LoggerFactory.getLogger(BeIDKeyManagerFactory.class);

	private BeIDManagerFactoryParameters beIDSpec;

	@Override
	protected KeyManager[] engineGetKeyManagers() {
		LOGGER.debug("engineGetKeyManagers");
		KeyManager beidKeyManager;
		try {
			beidKeyManager = new BeIDX509KeyManager(this.beIDSpec);
		} catch (final Exception e) {
			throw new IllegalStateException(e);
		}
		final KeyManager[] keyManagers = new KeyManager[] { beidKeyManager };
		return keyManagers;
	}

	@Override
	protected void engineInit(final ManagerFactoryParameters spec) throws InvalidAlgorithmParameterException {
		LOGGER.debug("engineInit(spec)");
		if (null == spec) {
			return;
		}
		if (false == spec instanceof BeIDManagerFactoryParameters) {
			throw new InvalidAlgorithmParameterException();
		}
		this.beIDSpec = (BeIDManagerFactoryParameters) spec;
	}

	@Override
	protected void engineInit(final KeyStore keyStore, final char[] password)
			throws KeyStoreException, NoSuchAlgorithmException, UnrecoverableKeyException {
		LOGGER.debug("engineInit(KeyStore,password)");
	}
}
