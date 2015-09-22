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
package be.fedict.commons.eid.consumer;

import java.io.Serializable;

import be.fedict.commons.eid.consumer.tlv.ByteArrayField;

/**
 * Holds the CARD DATA information returned by BeIDCard.getCardData() (see
 * BELPIC APPLICATION V2.0 specs, GET CARD DATA, p49.)
 * 
 * @author Frank Marien
 */
public class CardData implements Serializable {

	private static final long serialVersionUID = 1L;

	// pkcs1 support masks
	private static final int RSASSA_PKCS1_15_MASK = 1;
	private static final int RSASSA_PSS_MASK = 2;
	private static final int RSAES_PKCS1_15_MASK = 4;
	private static final int RSAES_OAEP_MASK = 8;
	private static final int RSAKEM_MASK = 16;

	// lifecycle state values
	private static final int DEACTIVATED = 0x0f;
	private static final int ACTIVATED = 0x8a;
	private static final int LOCKED = 0xff;

	@ByteArrayField(offset = 0, length = 16)
	public byte[] serialNumber;

	@ByteArrayField(offset = 0, length = 2)
	public int axaltoReservedNumber;

	@ByteArrayField(offset = 2, length = 2)
	public int chipManufacturer;

	@ByteArrayField(offset = 4, length = 12)
	public byte[] chipSerialNumber;

	@ByteArrayField(offset = 16, length = 1)
	public int componentCode;

	@ByteArrayField(offset = 17, length = 1)
	public int osNumber;

	@ByteArrayField(offset = 18, length = 1)
	public int osVersion;

	@ByteArrayField(offset = 19, length = 1)
	public int softmaskNumber;

	@ByteArrayField(offset = 20, length = 1)
	public int softmaskVersion;

	@ByteArrayField(offset = 21, length = 1)
	public int applicationVersion;

	@ByteArrayField(offset = 22, length = 2)
	public int globalOSVersion;

	@ByteArrayField(offset = 24, length = 1)
	public int applicationInterfaceVersion;

	@ByteArrayField(offset = 25, length = 1)
	public int pkcs1Support;

	@ByteArrayField(offset = 26, length = 1)
	public int keyExchangeVersion;

	@ByteArrayField(offset = 27, length = 1)
	public int applicationLifeCycle;

	// ----------------------------------------------------------------------
	/**
	 * get the complete card serial number "he serial number is composed of 2
	 * bytes reserved for axalto, 2 bytes identifying the chip manufacturer, and
	 * 12 bytes identifying uniquely the chip inside all chips from this
	 * manufacturer."
	 * 
	 * @return the complete 16-byte card serial number
	 */
	public byte[] getSerialNumber() {
		return this.serialNumber;
	}

	/**
	 * get the "2 bytes reserved for axalto" from the card serial number
	 * 
	 * @return the value of the 2 bytes reserved for axalto from the card serial
	 *         number
	 */
	public int getAxaltoReservedNumber() {
		return this.axaltoReservedNumber;
	}

	/**
	 * get the "2 bytes identifying the chip manufacturer" from the card serial
	 * number
	 * 
	 * @return the value of the 2 bytes identifying the chip manufacturer from
	 *         the card serial number
	 */
	public int getChipManufacturer() {
		return this.chipManufacturer;
	}

	/**
	 * get the 12 bytes uniquely identifying the chip inside all chips from this
	 * manufacturer
	 * 
	 * @return the 12 bytes uniquely identifying the chip inside all chips from
	 *         this manufacturer
	 */
	public byte[] getChipSerialNumber() {
		return this.chipSerialNumber;
	}

	/**
	 * get the "component code" byte
	 * 
	 * @return the "component code" byte
	 */
	public int getComponentCode() {
		return this.componentCode;
	}

	/**
	 * get the "OS number" byte
	 * 
	 * @return the "OS number" byte
	 */
	public int getOsNumber() {
		return this.osNumber;
	}

	/**
	 * get the "OS version" byte
	 * 
	 * @return the "OS version" byte
	 */
	public int getOsVersion() {
		return this.osVersion;
	}

	/**
	 * get the "Softmask number" byte
	 * 
	 * @return the "Softmask number" byte
	 */
	public int getSoftmaskNumber() {
		return this.softmaskNumber;
	}

	/**
	 * get the "Softmask version" byte
	 * 
	 * @return the "Softmask version" byte
	 */
	public int getSoftmaskVersion() {
		return this.softmaskVersion;
	}

	/**
	 * get the "Application version" byte
	 * 
	 * @return the "Application version" byte
	 */
	public int getApplicationVersion() {
		return this.applicationVersion;
	}

	/**
	 * get the 2 "Global OS version" bytes "This global number is unique for a
	 * given set composed of: Component code || OS number || OS version ||
	 * Softmask number || Softmask version || Application version"
	 * 
	 * @return the 2 "Global OS version" bytes
	 */
	public int getGlobalOSVersion() {
		return this.globalOSVersion;
	}

	/**
	 * get the "Application interface version" byte
	 * 
	 * @return the "Application interface version" byte
	 */
	public int getApplicationInterfaceVersion() {
		return this.applicationInterfaceVersion;
	}

	/**
	 * get the "PKCS#1 support".
	 * 
	 * byte
	 * 
	 * b7 b6 b5 b4 b3 b2 b1 b0 Meaning
	 * 
	 * -- -- -- -- -- -- -- 1 RSASSA-PKCS1 v1.5 supported (MD5 and SHA-1)
	 * 
	 * -- -- -- -- -- -- 1 -- RSASSA-PSS supported (SHA-1)
	 * 
	 * -- -- -- -- -- 1 -- -- RSAES-PKCS1 v1.5 supported
	 * 
	 * -- -- -- -- 1 -- -- -- RSAES-OAEP supported
	 * 
	 * -- -- -- 1 -- -- -- -- RSA-KEM supported
	 * 
	 * Notice that the above is as specified within BELPIC 2.0, which is not in
	 * production (yet). For now you simply get "21", the PKCS#1 version number.
	 * 
	 * @return the "PKCS#1 support" byte
	 */
	public int getPkcs1Support() {
		return this.pkcs1Support;
	}

	/**
	 * get the "Key exchange version" byte
	 * 
	 * @return the "Key exchange version" byte
	 */
	public int getKeyExchangeVersion() {
		return this.keyExchangeVersion;
	}

	/**
	 * get the "Application Life cycle" byte CAUTION: state of 0x0f
	 * (DEACTIVATED) has been observed in otherwise active cards. It's a bad
	 * idea to make a functional decision about card state based on this byte.
	 * 
	 * @return the "Application Life cycle" byte
	 */
	public int getApplicationLifeCycle() {
		return this.applicationLifeCycle;
	}

	// -----------------------------------------------------------------------
	/**
	 * Convenience method to test whether this card supports RSASSA-PKCS1 v1.5.
	 * BELPIC v2.0 only!
	 * 
	 * @return true if card supports RSASSA-PKCS1 v1.5, false otherwise
	 */
	public boolean isRSASSAPKCS115Supported() {
		return (this.getPkcs1Support() & RSASSA_PKCS1_15_MASK) > 0;
	}

	/**
	 * Convenience method to test whether this card supports RSASSA-PSS. BELPIC
	 * v2.0 only!
	 * 
	 * @return true if card supports RSASSA-PSS, false otherwise
	 */
	public boolean isRSASSAPSSSupported() {
		return (this.getPkcs1Support() & RSASSA_PSS_MASK) > 0;
	}

	/**
	 * Convenience method to test whether this card supports RSAES-PKCS1 v1.5.
	 * BELPIC v2.0 only!
	 * 
	 * @return true if card supports RSAES-PKCS1 v1.5, false otherwise
	 */
	public boolean isRSAESPKCS115Supported() {
		return (this.getPkcs1Support() & RSAES_PKCS1_15_MASK) > 0;
	}

	/**
	 * Convenience method to test whether this card supports RSAES-OAEP. BELPIC
	 * v2.0 only!
	 * 
	 * @return true if card supports RSAES-OAEP, false otherwise
	 */
	public boolean isRSAESOAEPSupported() {
		return (this.getPkcs1Support() & RSAES_OAEP_MASK) > 0;
	}

	/**
	 * Convenience method to test whether this card supports RSA-KEM. BELPIC
	 * v2.0 only!
	 * 
	 * @return true if card supports RSA-KEM, false otherwise
	 */
	public boolean isRSAKEMSupported() {
		return (this.getPkcs1Support() & RSAKEM_MASK) > 0;
	}

	/**
	 * Convenience method to test whether this card is in the ACTIVATED state.
	 * CAUTION: state DEACTIVATED has been observed in otherwise active cards.
	 * 
	 * @returns true if card is in ACTIVATED state, false otherwise
	 */
	public boolean isActivated() {
		return (this.getApplicationLifeCycle() == ACTIVATED);
	}

	/**
	 * Convenience method to test whether this card is in the DEACTIVATED state.
	 * CAUTION: state DEACTIVATED has been observed in otherwise active cards.
	 * 
	 * @returns true if card is in DEACTIVATED state, false otherwise
	 */
	public boolean isDeactivated() {
		return (this.getApplicationLifeCycle() == DEACTIVATED);
	}

	/**
	 * Convenience method to test whether this card is in the LOCKED state.
	 * CAUTION: state DEACTIVATED has been observed in otherwise active cards.
	 * 
	 * @returns true if card is in LOCKED state, false otherwise
	 */
	public boolean isLocked() {
		return (this.getApplicationLifeCycle() == LOCKED);
	}
}
