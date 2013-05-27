/*
 * Commons eID Project.
 * Copyright (C) 2008-2013 FedICT.
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

import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;

/**
 * Utility class simplifying the use of the BeID Card for Mutual SSL authentication to
 * two actions:
 * <ul>
 * <li> Security.addProvider(new BeIDProvider());
 * <li> set your connection's socketfactory to BeIDSocketFactory.getSSLSocketFactory() before opening the connection.
 * </ul>
 * @author Frank Marien
 */
public class BeIDSocketFactory {
	private static SSLSocketFactory socketFactorSingleton;

	public static SSLSocketFactory getSSLSocketFactory()
			throws NoSuchAlgorithmException, KeyManagementException {
		if (BeIDSocketFactory.socketFactorSingleton == null) {
			final SSLContext sslContext = SSLContext.getInstance("TLS");
			final KeyManagerFactory keyManagerFactory = KeyManagerFactory
					.getInstance("BeID");
			sslContext.init(keyManagerFactory.getKeyManagers(), null,
					SecureRandom.getInstance("BeID"));
			socketFactorSingleton = sslContext.getSocketFactory();
		}

		return socketFactorSingleton;
	}
}
