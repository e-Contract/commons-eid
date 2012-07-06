package test.integ.be.fedict.commons.eid.client.cardfactoryproxy;

import java.io.IOException;
import java.io.InputStream;
import javax.smartcardio.ATR;
import org.apache.commons.io.IOUtils;
import be.fedict.commons.eid.client.BeIDFileType;

public class SimulatedBeIDCard extends SimulatedCard {
	public SimulatedBeIDCard(String profile) {
		super(null);

		InputStream atrInputStream = SimulatedBeIDCard.class
				.getResourceAsStream("/" + profile + "_ATR.bin");

		try {
			setATR(new ATR(IOUtils.toByteArray(atrInputStream)));
		} catch (IOException e) {
			// missing _ATR file, set ATR from testcard
			setATR(new ATR(new byte[]{0x3b, (byte) 0x98, 0x13, 0x40, 0x0a,
					(byte) 0xa5, 0x03, 0x01, 0x01, 0x01, (byte) 0xad, 0x13,
					0x11}));
		}

		setFilesFromProfile(profile);
	}

	public SimulatedBeIDCard(ATR atr) {
		super(atr);
	}

	public SimulatedBeIDCard setFilesFromProfile(String profile) {
		for (BeIDFileType type : BeIDFileType.values()) {
			try {
				setFileFromProfile(type, profile);
			} catch (IOException iox) {
				System.err.println("Card Has No " + type);
			}
		}

		return this;
	}

	public SimulatedBeIDCard setFileFromProfile(BeIDFileType type,
			String profile) throws IOException {
		InputStream idInputStream = SimulatedBeIDCard.class
				.getResourceAsStream("/" + profile + "_" + type + ".tlv");
		setFile(type.getFileId(), IOUtils.toByteArray(idInputStream));
		return this;
	}
}
