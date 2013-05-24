package be.fedict.commons.eid.consumer;

import java.io.Serializable;

import be.fedict.commons.eid.consumer.tlv.ByteArrayField;

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

	public byte[] getSerialNumber() {
		return this.serialNumber;
	}

	public int getAxaltoReservedNumber() {
		return this.axaltoReservedNumber;
	}

	public int getChipManufacturer() {
		return this.chipManufacturer;
	}

	public byte[] getChipSerialNumber() {
		return this.chipSerialNumber;
	}

	public int getComponentCode() {
		return this.componentCode;
	}

	public int getOsNumber() {
		return this.osNumber;
	}

	public int getOsVersion() {
		return this.osVersion;
	}

	public int getSoftmaskNumber() {
		return this.softmaskNumber;
	}

	public int getSoftmaskVersion() {
		return this.softmaskVersion;
	}

	public int getApplicationVersion() {
		return this.applicationVersion;
	}

	public int getGlobalOSVersion() {
		return this.globalOSVersion;
	}

	public int getApplicationInterfaceVersion() {
		return this.applicationInterfaceVersion;
	}

	public int getPkcs1Support() {
		return this.pkcs1Support;
	}

	public int getKeyExchangeVersion() {
		return this.keyExchangeVersion;
	}

	public int getApplicationLifeCycle() {
		return this.applicationLifeCycle;
	}

	// -----------------------------------------------------------------------

	public boolean isRSASSAPKCS115Supported() {
		return (this.getPkcs1Support() & RSASSA_PKCS1_15_MASK) > 0;
	}

	public boolean isRSASSAPSSSupported() {
		return (this.getPkcs1Support() & RSASSA_PSS_MASK) > 0;
	}

	public boolean isRSAESPKCS115Supported() {
		return (this.getPkcs1Support() & RSAES_PKCS1_15_MASK) > 0;
	}

	public boolean isRSAESOAEPSupported() {
		return (this.getPkcs1Support() & RSAES_OAEP_MASK) > 0;
	}

	public boolean isRSAKEMSupported() {
		return (this.getPkcs1Support() & RSAKEM_MASK) > 0;
	}

	public boolean isActivated() {
		return (this.getApplicationLifeCycle() == ACTIVATED);
	}

	public boolean isDeactivated() {
		return (this.getApplicationLifeCycle() == DEACTIVATED);
	}

	public boolean isLocked() {
		return (this.getApplicationLifeCycle() == LOCKED);
	}
}
