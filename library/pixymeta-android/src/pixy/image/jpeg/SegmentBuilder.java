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

import pixy.util.Builder;

/**
 * Base builder for JPEG segments.
 *
 * @author Wen Yu, yuwen_66@yahoo.com
 * @version 1.0 10/11/2013
 */
public abstract class SegmentBuilder implements Builder<Segment> {
	//
	private final Marker marker;
	
	public SegmentBuilder(Marker marker) {
		this.marker = marker;
	}
		
	public final Segment build() {
		byte[] data = buildData();
		
		return new Segment(marker, data.length + 2, data);
	}
	
	protected abstract byte[] buildData();
}
