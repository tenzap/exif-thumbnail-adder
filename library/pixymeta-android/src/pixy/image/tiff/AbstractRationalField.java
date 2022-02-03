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

public abstract class AbstractRationalField extends TiffField<int[]> {

	public AbstractRationalField(short tag, FieldType fieldType, int[] data) {
		super(tag, fieldType, data.length>>1);
		this.data = data;
	}
	
	public int[] getData() {
		return data.clone();
	}
	
	public int[] getDataAsLong() {
		return getData();
	}

	protected int writeData(RandomAccessOutputStream os, int toOffset) throws IOException {
		//
		dataOffset = toOffset;
		os.writeInt(toOffset);
		os.seek(toOffset);
		
		for (int value : data)
			os.writeInt(value);
		
		toOffset += (data.length << 2);
		
		return toOffset;
	}
}
