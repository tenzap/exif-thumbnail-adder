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

package pixy.image.png;

import pixy.util.Builder;

/**
 * PNG tRNS chunk builder
 *  
 * @author Wen Yu, yuwen_66@yahoo.com
 * @version 1.0 05/03/2013
 */
public class TRNSBuilder extends ChunkBuilder implements Builder<Chunk> {

	private int colorType = 0;
	private byte[] alpha;
	
	public TRNSBuilder(int colorType) {
		super(ChunkType.TRNS);
		this.colorType = colorType;
	}
	
	public TRNSBuilder alpha(byte[] alpha) {
		this.alpha = alpha;
		return this;
	}

	@Override
	protected byte[] buildData() {
		switch(colorType)
		{			
			case 0:	
			case 2:				
			case 3:			
				break;
			case 4:
			case 6:		
			default:
				throw new IllegalArgumentException("Invalid color type: " + colorType);
		}
		
		return alpha;
	}
}
