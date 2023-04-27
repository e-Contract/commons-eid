/*
 * Commons eID Project.
 * Copyright (C) 2022-2023 e-Contract.be BV.
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
import java.math.BigInteger;
import java.security.cert.CertificateException;
import java.security.interfaces.ECPrivateKey;
import java.security.interfaces.ECPublicKey;
import java.security.spec.ECParameterSpec;

import javax.smartcardio.CardException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import be.fedict.commons.eid.client.BeIDCard;
import be.fedict.commons.eid.client.FileType;

public class BeIDECPrivateKey extends AbstractBeIDPrivateKey implements ECPrivateKey {

	private static final long serialVersionUID = 1L;

	private static final Logger LOGGER = LoggerFactory.getLogger(BeIDECPrivateKey.class);

	public BeIDECPrivateKey(FileType certificateFileType, BeIDCard beIDCard, boolean logoff, boolean allowFailingLogoff,
			boolean autoRecovery, BeIDKeyStore beIDKeyStore, String applicationName) {
		super(certificateFileType, beIDCard, logoff, allowFailingLogoff, autoRecovery, beIDKeyStore, applicationName);
	}

	@Override
	public ECParameterSpec getParams() {
		LOGGER.debug("getParams()");
		if (super.authenticationCertificate == null) {
			try {
				super.authenticationCertificate = super.beIDCard.getAuthenticationCertificate();
			} catch (IOException | InterruptedException | CertificateException | CardException e) {
				// don't fail here
			}
		}
		ECPublicKey ecPublicKey = (ECPublicKey) super.authenticationCertificate.getPublicKey();
		return ecPublicKey.getParams();
	}

	@Override
	public BigInteger getS() {
		LOGGER.debug("getS()");
		throw new UnsupportedOperationException();
	}
}
