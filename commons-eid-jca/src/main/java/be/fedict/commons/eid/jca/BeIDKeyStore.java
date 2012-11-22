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

import java.awt.Component;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.Key;
import java.security.KeyStore;
import java.security.KeyStore.LoadStoreParameter;
import java.security.KeyStoreException;
import java.security.KeyStoreSpi;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Date;
import java.util.Enumeration;
import java.util.List;
import java.util.Locale;
import java.util.Vector;

import javax.swing.JFrame;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import be.fedict.commons.eid.client.BeIDCard;
import be.fedict.commons.eid.client.BeIDCards;
import be.fedict.commons.eid.client.CancelledException;
import be.fedict.commons.eid.client.FileType;
import be.fedict.commons.eid.client.impl.VoidLogger;
import be.fedict.commons.eid.client.spi.BeIDCardUI;
import be.fedict.commons.eid.client.spi.BeIDCardsUI;
import be.fedict.eid.commons.dialogs.DefaultBeIDCardUI;
import be.fedict.eid.commons.dialogs.DefaultBeIDCardsUI;
import be.fedict.eid.commons.dialogs.Messages;

/**
 * eID based JCA {@link KeyStore}. Used to load eID key material via standard
 * JCA API calls. Once the JCA security provider has been registered you have a
 * new key store available named "BeID". Two key aliases are available:
 * <ul>
 * <li>"Authentication" which gives you access to the eID authentication private
 * key and corresponding certificate chain.</li>
 * <li>"Signature" which gives you access to the eID non-repudiation private key
 * and corresponding certificate chain.</li>
 * </ul>
 * Further the Citizen CA certificate can be accessed via the "CA" alias and the
 * Root CA certificate can be accessed via the "Root" alias.
 * <p/>
 * Supports the eID specific {@link BeIDKeyStoreParameter} key store parameter.
 * You can also let any {@link JFrame} implement the
 * {@link KeyStore.LoadStoreParameter} interface. If you pass this to
 * {@link KeyStore#load(LoadStoreParameter)} the keystore will use that Swing
 * frame as parent for positioning the dialogs.
 * <p/>
 * Usage:
 * 
 * <pre>
 * import java.security.KeyStore;
 * import java.security.cert.X509Certificate;
 * import java.security.PrivateKey;
 * 
 * ...
 * KeyStore keyStore = KeyStore.getInstance("BeID");
 * keyStore.load(null);
 * X509Certificate authnCertificate = (X509Certificate) keyStore
 * 			.getCertificate("Authentication");
 * PrivateKey authnPrivateKey = (PrivateKey) keyStore.getKey(
 * 			"Authentication", null);
 * Certificate[] signCertificateChain = keyStore.getCertificateChain("Signature");
 * </pre>
 * 
 * @see BeIDKeyStoreParameter
 * @see BeIDProvider
 * @author Frank Cornelis
 * 
 */
public class BeIDKeyStore extends KeyStoreSpi {

	private static final Log LOG = LogFactory.getLog(BeIDKeyStore.class);

	private BeIDKeyStoreParameter keyStoreParameter;

	private BeIDCard beIDCard;

	@Override
	public Key engineGetKey(final String alias, final char[] password)
			throws NoSuchAlgorithmException, UnrecoverableKeyException {
		LOG.debug("engineGetKey: " + alias);
		final BeIDCard beIDCard = getBeIDCard();
		boolean logoff;
		if (null == this.keyStoreParameter) {
			logoff = false;
		} else {
			logoff = this.keyStoreParameter.getLogoff();
		}
		if ("Authentication".equals(alias)) {
			final BeIDPrivateKey beIDPrivateKey = new BeIDPrivateKey(
					FileType.AuthentificationCertificate, beIDCard, logoff);
			return beIDPrivateKey;
		}
		if ("Signature".equals(alias)) {
			final BeIDPrivateKey beIDPrivateKey = new BeIDPrivateKey(
					FileType.NonRepudiationCertificate, beIDCard, logoff);
			return beIDPrivateKey;
		}
		return null;
	}

	@Override
	public Certificate[] engineGetCertificateChain(final String alias) {
		LOG.debug("engineGetCertificateChain: " + alias);
		final BeIDCard beIDCard = getBeIDCard();
		if ("Signature".equals(alias)) {
			try {
				final List<X509Certificate> signingCertificateChain = beIDCard
						.getSigningCertificateChain();
				return signingCertificateChain.toArray(new X509Certificate[]{});
			} catch (final Exception ex) {
				LOG.error("error: " + ex.getMessage(), ex);
				return null;
			}
		}
		if ("Authentication".equals(alias)) {
			try {
				final List<X509Certificate> signingCertificateChain = beIDCard
						.getAuthenticationCertificateChain();
				return signingCertificateChain.toArray(new X509Certificate[]{});
			} catch (final Exception ex) {
				LOG.error("error: " + ex.getMessage(), ex);
				return null;
			}
		}
		return null;
	}

	@Override
	public Certificate engineGetCertificate(final String alias) {
		LOG.debug("engineGetCertificate: " + alias);
		final BeIDCard beIDCard = getBeIDCard();
		if ("Signature".equals(alias)) {
			try {
				return beIDCard.getSigningCertificate();
			} catch (final Exception ex) {
				LOG.warn("error: " + ex.getMessage(), ex);
				return null;
			}
		}
		if ("Authentication".equals(alias)) {
			try {
				return beIDCard.getAuthenticationCertificate();
			} catch (final Exception ex) {
				LOG.warn("error: " + ex.getMessage(), ex);
				return null;
			}
		}
		if ("CA".equals(alias)) {
			try {
				return beIDCard.getCACertificate();
			} catch (Exception e) {
				LOG.warn("error: " + e.getMessage(), e);
				return null;
			}
		}
		if ("Root".equals(alias)) {
			try {
				return beIDCard.getRootCACertificate();
			} catch (Exception e) {
				LOG.warn("error: " + e.getMessage(), e);
				return null;
			}
		}
		return null;
	}

	@Override
	public Date engineGetCreationDate(final String alias) {
		final X509Certificate certificate = (X509Certificate) this
				.engineGetCertificate(alias);
		if (null == certificate) {
			return null;
		}
		return certificate.getNotBefore();
	}

	@Override
	public void engineSetKeyEntry(final String alias, final Key key,
			final char[] password, final Certificate[] chain)
			throws KeyStoreException {
		throw new KeyStoreException();
	}

	@Override
	public void engineSetKeyEntry(final String alias, final byte[] key,
			final Certificate[] chain) throws KeyStoreException {
		throw new KeyStoreException();
	}

	@Override
	public void engineSetCertificateEntry(final String alias,
			final Certificate cert) throws KeyStoreException {
		throw new KeyStoreException();
	}

	@Override
	public void engineDeleteEntry(final String alias) throws KeyStoreException {
		throw new KeyStoreException();
	}

	@Override
	public Enumeration<String> engineAliases() {
		LOG.debug("engineAliases");
		final Vector<String> aliases = new Vector<String>();
		aliases.add("Authentication");
		aliases.add("Signature");
		return aliases.elements();
	}

	@Override
	public boolean engineContainsAlias(final String alias) {
		if ("Authentication".equals(alias)) {
			return true;
		}
		if ("Signature".equals(alias)) {
			return true;
		}
		return false;
	}

	@Override
	public int engineSize() {
		return 2;
	}

	@Override
	public boolean engineIsKeyEntry(final String alias) {
		if ("Authentication".equals(alias)) {
			return true;
		}
		if ("Signature".equals(alias)) {
			return true;
		}
		return false;
	}

	@Override
	public boolean engineIsCertificateEntry(final String alias) {
		if ("Authentication".equals(alias)) {
			return true;
		}
		if ("Signature".equals(alias)) {
			return true;
		}
		return false;
	}

	@Override
	public String engineGetCertificateAlias(final Certificate cert) {
		return null;
	}

	@Override
	public void engineStore(final OutputStream stream, final char[] password)
			throws IOException, NoSuchAlgorithmException, CertificateException {
	}

	@Override
	public void engineLoad(final InputStream stream, final char[] password)
			throws IOException, NoSuchAlgorithmException, CertificateException {
	}

	@Override
	public void engineLoad(final LoadStoreParameter param) throws IOException,
			NoSuchAlgorithmException, CertificateException {
		LOG.debug("engineLoad");
		if (null == param) {
			return;
		}
		if (param instanceof BeIDKeyStoreParameter) {
			this.keyStoreParameter = (BeIDKeyStoreParameter) param;
			return;
		}
		if (param instanceof JFrame) {
			this.keyStoreParameter = new BeIDKeyStoreParameter();
			JFrame frame = (JFrame) param;
			this.keyStoreParameter.setParentComponent(frame);
			return;
		}
		throw new NoSuchAlgorithmException();
	}

	private BeIDCard getBeIDCard() {
		if (null != this.beIDCard) {
			return this.beIDCard;
		}
		if (null != this.keyStoreParameter) {
			this.beIDCard = this.keyStoreParameter.getBeIDCard();
		}
		if (null != this.beIDCard) {
			return this.beIDCard;
		}
		Component parentComponent;
		Locale locale;
		if (null != this.keyStoreParameter) {
			parentComponent = this.keyStoreParameter.getParentComponent();
			locale = this.keyStoreParameter.getLocale();
		} else {
			parentComponent = null;
			locale = null;
		}
		if (null == locale) {
			locale = Locale.getDefault();
		}
		final Messages messages = new Messages(locale);
		final BeIDCardsUI ui = new DefaultBeIDCardsUI(parentComponent, messages);
		final BeIDCards beIDCards = new BeIDCards(new VoidLogger(), ui);
		try {
			this.beIDCard = beIDCards.getOneBeIDCard();
			final BeIDCardUI userInterface = new DefaultBeIDCardUI(
					parentComponent, messages);
			this.beIDCard.setUI(userInterface);
		} catch (final CancelledException cex) {
			throw new SecurityException("user cancelled");
		}
		if (null == this.beIDCard) {
			throw new SecurityException("missing eID card");
		}
		return this.beIDCard;
	}
}
