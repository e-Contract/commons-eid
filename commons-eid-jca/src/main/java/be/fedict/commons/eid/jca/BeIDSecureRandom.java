/*
 * Commons eID Project.
 * Copyright (C) 2008-2013 FedICT.
 * Copyright (C) 2017-2020 e-Contract.be BV.
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

import java.security.SecureRandomSpi;

import javax.smartcardio.CardException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import be.fedict.commons.eid.client.BeIDCard;
import be.fedict.commons.eid.client.BeIDCards;
import be.fedict.commons.eid.client.CancelledException;

/**
 * eID based implementation of a secure random generator. Can be used to seed
 * for example a mutual SSL handshake. This secure random generator does not
 * feature eID auto recovery.
 * <p/>
 * Usage:
 * 
 * <pre>
 * SecureRandom secureRandom = SecureRandom.getInstance(&quot;BeID&quot;);
 * </pre>
 * 
 * @author Frank Cornelis
 * 
 */
public class BeIDSecureRandom extends SecureRandomSpi {

	private static final long serialVersionUID = 1L;

	private static final Logger LOGGER = LoggerFactory.getLogger(BeIDSecureRandom.class);

	private BeIDCard beIDCard;

	@Override
	protected void engineSetSeed(final byte[] seed) {
		LOGGER.debug("engineSetSeed");
	}

	@Override
	protected void engineNextBytes(final byte[] bytes) {
		LOGGER.debug("engineNextBytes: {} bytes", bytes.length);
		BeIDCard beIDCard = getBeIDCard();
		byte[] randomData;
		try {
			try {
				randomData = beIDCard.getChallenge(bytes.length);
			} catch (CardException e) {
				beIDCard = getBeIDCard(true);
				randomData = beIDCard.getChallenge(bytes.length);
			}
		} catch (final CardException e) {
			throw new RuntimeException(e);
		}
		System.arraycopy(randomData, 0, bytes, 0, bytes.length);
	}

	@Override
	protected byte[] engineGenerateSeed(final int numBytes) {
		LOGGER.debug("engineGenerateSeed: {} bytes", numBytes);
		final BeIDCard beIDCard = getBeIDCard();
		byte[] randomData;
		try {
			randomData = beIDCard.getChallenge(numBytes);
		} catch (final CardException e) {
			throw new RuntimeException(e);
		}
		return randomData;
	}

	private BeIDCard getBeIDCard(boolean autoRecover) {
		if (autoRecover) {
			this.beIDCard = null;
		}
		return getBeIDCard();
	}

	private BeIDCard getBeIDCard() {
		if (null != this.beIDCard) {
			return this.beIDCard;
		}
		final BeIDCards beIDCards = new BeIDCards();
		try {
			this.beIDCard = beIDCards.getOneBeIDCard();
		} catch (final CancelledException e) {
			throw new RuntimeException(e);
		}
		return this.beIDCard;
	}
}
