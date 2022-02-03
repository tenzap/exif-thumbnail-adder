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
import java.util.Arrays;

import pixy.io.RandomAccessOutputStream;

/**
 * TIFF FieldType.DOUBLE wrapper
 *
 * @author Wen Yu, yuwen_66@yahoo.com
 * @version 1.0 12/04/2014
 */
public class DoubleField extends TiffField<double[]> {

	public DoubleField(short tag, double[] data) {
		super(tag, FieldType.DOUBLE, data.length);
		this.data = data;
	}
	
	public double[] getData() {
		return data.clone();
	}
	
	public String getDataAsString() {
		return Arrays.toString(data);
	}

	@Override
	protected int writeData(RandomAccessOutputStream os, int toOffset)
			throws IOException {
		//
		dataOffset = toOffset;
		os.writeInt(toOffset);
		os.seek(toOffset);
		
		for (double value : data)
			os.writeDouble(value);
		
		toOffset += (data.length << 3);
		
		return toOffset;
	}
}
