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

package be.fedict.commons.eid.consumer.tlv;

import java.io.UnsupportedEncodingException;
import java.lang.reflect.Field;
import java.nio.ByteBuffer;

/**
 * Byte Array Fields Offset/Length Parser supports extraction of byte array
 * slices, unsigned 8 and 16-bit values from byte array integers
 * 
 * @author Frank Marien
 * 
 */
public class ByteArrayParser {

	private ByteArrayParser() {
		super();
	}

	/**
	 * Parses the given file using the meta-data annotations within the baClass
	 * parameter.
	 * 
	 * @param <T>
	 * @param file
	 * @param baClass
	 * @return
	 */
	public static <T> T parse(final byte[] file, final Class<T> baClass) {
		T t;
		try {
			t = parseThrowing(file, baClass);
		} catch (final Exception ex) {
			throw new RuntimeException("error parsing file: "
					+ baClass.getName(), ex);
		}
		return t;
	}

	private static <T> T parseThrowing(final byte[] data, final Class<T> baClass)
			throws InstantiationException, IllegalAccessException,
			DataConvertorException, UnsupportedEncodingException {
		final Field[] fields = baClass.getDeclaredFields();

		final T baObject = baClass.newInstance();
		for (Field field : fields) {
			final ByteArrayField baFieldAnnotation = field
					.getAnnotation(ByteArrayField.class);
			if (baFieldAnnotation != null) {
				final int offset = baFieldAnnotation.offset();
				final int length = baFieldAnnotation.length();
				if (field.getType().isArray()
						&& field.getType().getComponentType()
								.equals(byte.class)) {
					final byte[] byteArray = new byte[length];
					System.arraycopy(data, offset, byteArray, 0, length);
					field.set(baObject, byteArray);
				} else if (field.getType().equals(int.class)) {
					final ByteBuffer buff = ByteBuffer.wrap(data);
					switch (length) {
						case 1 :
							field.set(baObject, (int) buff.get(offset) & 0xff);
							break;

						case 2 :
							field.set(baObject,
									(int) buff.getShort(offset) & 0xffff);
							break;
					}

				}
			}
		}

		return baObject;
	}
}
