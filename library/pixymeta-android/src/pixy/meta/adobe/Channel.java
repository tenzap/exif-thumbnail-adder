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
 * Channel.java
 *
 * Who   Date       Description
 * ====  =========  =================================================================
 * WY    27Jul2015  initial creation
 */

package pixy.meta.adobe;

public class Channel {
	private int id;
	private int dataLen;
	
	private static final int RED = 0;
	private static final int GREEN = 1;
	private static final int BLUE = 2;
	
	private static final int TRANSPARENCY_MASK = -1;
	private static final int USER_SUPPLIED_LAYER_MASK = -2;
	private static final int REAL_USER_SUPPLIED_LAYER_MASK = -3;
		
	public Channel(int id, int len) {
		this.id = id;
		this.dataLen = len;
	}
	
	public int getDataLen() {
		return dataLen;
	}
	
	public int getID() {
		return id;
	}
	
	public String getType() {
		switch(id) {
			case RED:
				return "Red channel";
			case GREEN:
				return "Green channel";
			case BLUE:
				return "Blue channel";
			case TRANSPARENCY_MASK:
				return "Transparency mask";
			case USER_SUPPLIED_LAYER_MASK:
				return "User supplied layer mask";
			case REAL_USER_SUPPLIED_LAYER_MASK:
				return "real user supplied layer mask (when both a user mask and a vector mask are present)";
			default:
				return "Unknown channel (value " + id + ")";
		}
	}
}
