package be.fedict.commons.eid.client;

public enum PINPurpose {
	PINTest("test"), AuthenticationSignature("authentication"), NonRepudiationSignature(
			"nonrepudiation");

	private final String type;

	private PINPurpose(String type) {
		this.type = type;
	}

	public String getType() {
		return this.type;
	}

	public static PINPurpose fromFileType(FileType fileType) {
		switch (fileType) {
			case AuthentificationCertificate :
				return AuthenticationSignature;
			case NonRepudiationCertificate :
				return NonRepudiationSignature;
			default :
				return PINTest;
		}
	}
}
