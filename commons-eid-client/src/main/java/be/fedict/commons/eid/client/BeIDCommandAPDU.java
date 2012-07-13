package be.fedict.commons.eid.client;

public enum BeIDCommandAPDU {
	SELECT_FILE(0x00, 0xA4, 0x08, 0x0C), READ_BINARY(0x00, 0xB0);

	private final int cla;
	private final int ins;
	private final int p1;
	private final int p2;

	private BeIDCommandAPDU(int cla, int ins, int p1, int p2) {
		this.cla = cla;
		this.ins = ins;
		this.p1 = p1;
		this.p2 = p2;
	}

	private BeIDCommandAPDU(int cla, int ins) {
		this.cla = cla;
		this.ins = ins;
		this.p1 = -1;
		this.p2 = -1;
	}

	public int getCla() {
		return cla;
	}

	public int getIns() {
		return ins;
	}

	public int getP1() {
		return p1;
	}

	public int getP2() {
		return p2;
	}
}
