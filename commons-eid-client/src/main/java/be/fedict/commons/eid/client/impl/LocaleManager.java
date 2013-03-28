package be.fedict.commons.eid.client.impl;

import java.util.Locale;

public class LocaleManager {
	private static Locale locale;

	public static void setLocale(Locale newLocale) {
		LocaleManager.locale = newLocale;
	}

	public static Locale getLocale() {
		if (LocaleManager.locale == null) {
			LocaleManager.locale = Locale.getDefault();
		}
		return LocaleManager.locale;
	}
}
