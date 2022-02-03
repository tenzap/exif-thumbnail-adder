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
 *
 * Change History - most recent changes go on top of previous changes
 *
 * UserMask.java - Adobe Photoshop Document Data Block LMsk
 *
 * Who   Date       Description
 * ====  =========  =================================================
 * WY    28Jul2015  Initial creation
 */

package pixy.meta.adobe;

import java.util.Arrays;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import pixy.io.ReadStrategy;

public class UserMask extends DDBEntry {
	private int colorSpaceId;
	private int[] colors = new int[4];
	private int opacity;
	private int flag;
	
	// Obtain a logger instance
	private static final Logger LOGGER = LoggerFactory.getLogger(UserMask.class);
	
	public UserMask(int size, byte[] data, ReadStrategy readStrategy) {
		super(DataBlockType.LMsk, size, data, readStrategy);
		read();
	}
	
	public int[] getColors() {
		return colors.clone();
	}
	
	public int getOpacity() {
		return opacity;
	}
	
	public int getFlag() {
		return flag;
	}
	
	public int getColorSpace() {
		return colorSpaceId;
	}
	
	public ColorSpaceID getColorSpaceID() {
		return ColorSpaceID.fromInt(colorSpaceId);
	}
	
	public void print() {
		super.print();
		LOGGER.info("Color space: {}", getColorSpaceID());
		LOGGER.info("Color values: {}", Arrays.toString(colors));
		LOGGER.info("Opacity: {}", opacity);
		LOGGER.info("Flag: {}", flag);
	}
	
	private void read() {
		int i = 0;
		colorSpaceId = readStrategy.readShort(data, i);
		i += 2;
		colors[0] = readStrategy.readUnsignedShort(data, i);
		i += 2;
		colors[1] = readStrategy.readUnsignedShort(data, i);
		i += 2;
		colors[2] = readStrategy.readUnsignedShort(data, i);
		i += 2;
		colors[3] = readStrategy.readUnsignedShort(data, i);
		i += 2;
		opacity = readStrategy.readShort(data, i);
		i += 2;
		flag = data[i]&0xff; // 128
	}
}
