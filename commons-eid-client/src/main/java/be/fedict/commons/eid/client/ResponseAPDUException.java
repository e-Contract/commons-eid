package be.fedict.commons.eid.client;

import javax.smartcardio.ResponseAPDU;

public class ResponseAPDUException extends RuntimeException {
	private static final long serialVersionUID = -3573705690889181394L;
	private ResponseAPDU apdu;

	public ResponseAPDUException(ResponseAPDU apdu) {
		super();
		this.apdu = apdu;
	}

	public ResponseAPDUException(String message, ResponseAPDU apdu) {
		super(message + " [" + Integer.toHexString(apdu.getSW()) + "]");
		this.apdu = apdu;
	}

	public ResponseAPDU getApdu() {
		return apdu;
	}
}
