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

package be.fedict.commons.eid.client;
import java.io.File;


public class LibJ2PCSCLinuxFix
{
	/**
	 * Finds .so.version file on GNU/Linux. avoid guessing all GNU/Linux
	 * distros' library path configurations on 32 and 64-bit when working around
	 * the buggy libj2pcsc.so implementation based on JRE implementations adding
	 * the native library paths to the end of java.library.path
	 */
	private static File findLinuxNativeLibrary(String baseName, int version)
	{
		String nativeLibraryPaths = System.getProperty("java.library.path");
		if (nativeLibraryPaths == null)
			return null;

		String libFileName = System.mapLibraryName(baseName) + "." + version;
		for (String nativeLibraryPath : nativeLibraryPaths.split(":"))
		{
			File libraryFile = new File(nativeLibraryPath, libFileName);
			if (libraryFile.exists())
				return libraryFile;
		}

		return null;
	}

	public static void fixLinuxNativeLibrary()
	{
		String osName = System.getProperty("os.name");
		if (osName.startsWith("Linux"))
		{
			/*
			 * Workaround for Linux. Apparently the libj2pcsc.so from the JRE
			 * links to libpcsclite.so instead of libpcsclite.so.1. This can
			 * cause linking problems on Linux distributions that don't have the
			 * libpcsclite.so symbolic link.
			 * 
			 * See also: http://bugs.debian.org/cgi-bin/bugreport.cgi?bug=529339
			 */

			File libPcscLite = findLinuxNativeLibrary("pcsclite", 1);
			if (libPcscLite != null)
				System.setProperty("sun.security.smartcardio.library",libPcscLite.getAbsolutePath());
		}
	}
}
