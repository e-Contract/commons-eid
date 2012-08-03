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

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.Key;
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
import java.util.Vector;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import be.fedict.commons.eid.client.BeIDCard;
import be.fedict.commons.eid.client.BeIDFileType;

public class BeIDKeyStore extends KeyStoreSpi {

	private static final Log LOG = LogFactory.getLog(BeIDKeyStore.class);

	private BeIDCard beIDCard;

	private boolean logoff;

	@Override
	public Key engineGetKey(String alias, char[] password)
			throws NoSuchAlgorithmException, UnrecoverableKeyException {
		LOG.debug("engineGetKey: " + alias);
		if ("Authentication".equals(alias)) {
			BeIDPrivateKey beIDPrivateKey = new BeIDPrivateKey(
					BeIDFileType.AuthentificationCertificate, this.beIDCard,
					this.logoff);
			return beIDPrivateKey;
		}
		if ("Signature".equals(alias)) {
			BeIDPrivateKey beIDPrivateKey = new BeIDPrivateKey(
					BeIDFileType.SigningCertificate, this.beIDCard, this.logoff);
			return beIDPrivateKey;
		}
		return null;
	}

	@Override
	public Certificate[] engineGetCertificateChain(String alias) {
		LOG.debug("engineGetCertificateChain: " + alias);
		if ("Signature".equals(alias)) {
			try {
				List<X509Certificate> signingCertificateChain = this.beIDCard
						.getSigningCertificateChain();
				return signingCertificateChain
						.toArray(new X509Certificate[] {});
			} catch (Exception e) {
				LOG.error("error: " + e.getMessage(), e);
				return null;
			}
		}
		if ("Authentication".equals(alias)) {
			try {
				List<X509Certificate> signingCertificateChain = this.beIDCard
						.getAuthenticationCertificateChain();
				return signingCertificateChain
						.toArray(new X509Certificate[] {});
			} catch (Exception e) {
				LOG.error("error: " + e.getMessage(), e);
				return null;
			}
		}
		return null;
	}

	@Override
	public Certificate engineGetCertificate(String alias) {
		LOG.debug("engineGetCertificate: " + alias);
		if ("Signature".equals(alias)) {
			try {
				return this.beIDCard.getSigningCertificate();
			} catch (Exception e) {
				LOG.warn("error: " + e.getMessage(), e);
				return null;
			}
		}
		if ("Authentication".equals(alias)) {
			try {
				return this.beIDCard.getAuthenticationCertificate();
			} catch (Exception e) {
				LOG.warn("error: " + e.getMessage(), e);
				return null;
			}
		}
		return null;
	}

	@Override
	public Date engineGetCreationDate(String alias) {
		X509Certificate certificate = (X509Certificate) engineGetCertificate(alias);
		if (null == certificate) {
			return null;
		}
		return certificate.getNotBefore();
	}

	@Override
	public void engineSetKeyEntry(String alias, Key key, char[] password,
			Certificate[] chain) throws KeyStoreException {
	}

	@Override
	public void engineSetKeyEntry(String alias, byte[] key, Certificate[] chain)
			throws KeyStoreException {
	}

	@Override
	public void engineSetCertificateEntry(String alias, Certificate cert)
			throws KeyStoreException {
	}

	@Override
	public void engineDeleteEntry(String alias) throws KeyStoreException {
	}

	@Override
	public Enumeration<String> engineAliases() {
		LOG.debug("engineAliases");
		Vector<String> aliases = new Vector<String>();
		aliases.add("Authentication");
		aliases.add("Signature");
		return aliases.elements();
	}

	@Override
	public boolean engineContainsAlias(String alias) {
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
	public boolean engineIsKeyEntry(String alias) {
		if ("Authentication".equals(alias)) {
			return true;
		}
		if ("Signature".equals(alias)) {
			return true;
		}
		return false;
	}

	@Override
	public boolean engineIsCertificateEntry(String alias) {
		if ("Authentication".equals(alias)) {
			return true;
		}
		if ("Signature".equals(alias)) {
			return true;
		}
		return false;
	}

	@Override
	public String engineGetCertificateAlias(Certificate cert) {
		return null;
	}

	@Override
	public void engineStore(OutputStream stream, char[] password)
			throws IOException, NoSuchAlgorithmException, CertificateException {
	}

	@Override
	public void engineLoad(InputStream stream, char[] password)
			throws IOException, NoSuchAlgorithmException, CertificateException {
	}

	@Override
	public void engineLoad(LoadStoreParameter param) throws IOException,
			NoSuchAlgorithmException, CertificateException {
		if (false == param instanceof BeIDKeyStoreParameter) {
			throw new NoSuchAlgorithmException();
		}
		BeIDKeyStoreParameter keyStoreParameter = (BeIDKeyStoreParameter) param;
		LOG.debug("engineLoad");
		this.beIDCard = keyStoreParameter.getBeIDCard();
		this.logoff = keyStoreParameter.getLogoff();
	}
}
