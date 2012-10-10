package be.fedict.commons.eid.jca;

import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;

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
