/*
 * Copyright (c) 2014-2021 by Wen Yu
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * This Source Code may also be made available under the following Secondary
 * Licenses when the conditions for such availability set forth in the Eclipse
 * Public License, v. 2.0 are satisfied: GNU General Public License, version 2
 * or any later version.
 *
 * SPDX-License-Identifier: EPL-2.0 OR GPL-2.0-or-later
 */

package pixy.image.tiff;

import java.io.IOException;

import pixy.io.RandomAccessOutputStream;

public abstract class AbstractShortField extends TiffField<short[]> {

	public AbstractShortField(short tag, FieldType fieldType, short[] data) {
		super(tag, fieldType, data.length);
		this.data = data;	
	}
	
	public short[] getData() {
		return data.clone();
	}

	protected int writeData(RandomAccessOutputStream os, int toOffset) throws IOException {
		if (data.length <= 2) {
			dataOffset = (int)os.getStreamPointer();
			short[] tmp = new short[2];
			System.arraycopy(data, 0, tmp, 0, data.length);
			for (short value : tmp)
				os.writeShort(value);
		} else {
			dataOffset = toOffset;
			os.writeInt(toOffset);
			os.seek(toOffset);
			
			for (short value : data)
				os.writeShort(value);
			
			toOffset += (data.length << 1);
		}
		return toOffset;
	}
}
