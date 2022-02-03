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

public class QTable implements Comparable<QTable> {
	//
	private int precision;
	private int id;
	private int[] data;
	
	public QTable(int precision, int id, int[] data) {
		if(precision != 0 && precision != 1) 
			throw new IllegalArgumentException("Invalid precision value: " + precision);
		this.precision = precision;
		this.id = id;
		this.data = data;
	}
	
	public int getPrecision() {
		return precision; 
	}
	
	public int getID() {
		return id;
	}
	
	public int[] getData() {
		return data.clone();
	}

	public int compareTo(QTable that) {
		return this.id - that.id;
	}
}
