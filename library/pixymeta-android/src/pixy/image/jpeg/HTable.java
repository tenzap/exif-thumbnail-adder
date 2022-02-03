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

package pixy.image.jpeg;

public class HTable implements Comparable<HTable> {
	//
	public static final int DC_CLAZZ = 0;
	public static final int AC_CLAZZ = 1;
	
	private int clazz; // DC or AC
	private int id; // Table #
	private byte[] bits;
	private byte[] values;
	
	public HTable(int clazz, int id, byte[] bits, byte[] values) {
		this.clazz = clazz;
		this.id = id;
		this.bits = bits;
		this.values = values;
	}
	
	public int getClazz() {
		return clazz; 
	}
	
	public int getID() {
		return id;
	}
	
	public byte[] getBits() {
		return bits;
	}
	
	public byte[] getValues() {
		return values;
	}

	public int compareTo(HTable that) {
		return this.id - that.id;
	}
}
