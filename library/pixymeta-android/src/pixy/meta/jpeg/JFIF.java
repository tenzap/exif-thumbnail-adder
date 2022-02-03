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
 * JFIF.java
 *
 * Who   Date       Description
 * ====  =======    ============================================================
 * WY    12Jul2015  Initial creation
 */

package pixy.meta.jpeg;

import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import android.graphics.Bitmap;
import pixy.meta.Metadata;
import pixy.meta.MetadataEntry;
import pixy.meta.MetadataType;
import pixy.io.IOUtils;
import pixy.util.ArrayUtils;
import pixy.util.MetadataUtils;

public class JFIF extends Metadata {
		
	private static void checkInput(int majorVersion, int minorVersion, int densityUnit, int xDensity, int yDensity) {
		if(majorVersion < 0 || majorVersion > 0xff) throw new IllegalArgumentException("Invalid major version number: " + majorVersion);
		if(minorVersion < 0 || minorVersion > 0xff) throw new IllegalArgumentException("Invalid minor version number: " + minorVersion);
		if(densityUnit < 0 || densityUnit > 2) throw new IllegalArgumentException("Density unit value " + densityUnit + " out of range [0-2]");
		if(xDensity < 0 || xDensity > 0xffff) throw new IllegalArgumentException("xDensity value " + xDensity + " out of range (0-0xffff]");
		if(yDensity < 0 || yDensity > 0xffff) throw new IllegalArgumentException("yDensity value " + xDensity + " out of range (0-0xffff]");
	}
	
	private int majorVersion;
	private int minorVersion;
	private int densityUnit;
	private int xDensity;
	private int yDensity;
	private int thumbnailWidth;
	private int thumbnailHeight;
	private boolean containsThumbnail;
	
	private JFIFThumbnail thumbnail;

	public JFIF(byte[] data) {
		super(MetadataType.JPG_JFIF, data);
		ensureDataRead();
	}
	
	public JFIF(int majorVersion, int minorVersion, int densityUnit, int xDensity, int yDensity) {
		this(majorVersion, minorVersion, densityUnit, xDensity, yDensity, null);
	}
	
	public JFIF(int majorVersion, int minorVersion, int densityUnit, int xDensity, int yDensity, JFIFThumbnail thumbnail) {
		super(MetadataType.JPG_JFIF);
		checkInput(majorVersion, minorVersion, densityUnit, xDensity, yDensity);
		this.majorVersion = majorVersion;
		this.minorVersion = minorVersion;
		this.densityUnit = densityUnit;
		this.xDensity = xDensity;
		this.yDensity = yDensity;
		
		if(thumbnail != null) {
			int thumbnailWidth = thumbnail.getWidth();
			int thumbnailHeight = thumbnail.getHeight();
			if(thumbnailWidth < 0 || thumbnailWidth > 0xff)
				throw new IllegalArgumentException("Thumbnail width " + thumbnailWidth + " out of range (0-0xff]");
			if(thumbnailHeight < 0 || thumbnailHeight > 0xff)
				throw new IllegalArgumentException("Thumbnail height " + thumbnailHeight + " out of range (0-0xff]");
			this.thumbnailWidth = thumbnailWidth;
			this.thumbnailHeight = thumbnailHeight;
			this.thumbnail = thumbnail;
			this.containsThumbnail = true;
		}
		
		isDataRead = true;
	}
	
	public boolean containsThumbnail() {
		return containsThumbnail;
	}
	
	public int getDensityUnit() {
		return densityUnit;
	}
	
	public int getMajorVersion() {
		return majorVersion;
	}
	
	public int getMinorVersion() {
		return minorVersion;
	}
	
	public JFIFThumbnail getThumbnail() {
		return new JFIFThumbnail(thumbnail);
	}
	
	public int getThumbnailHeight() {
		return thumbnailHeight;
	}
	
	public int getThumbnailWidth() {
		return thumbnailWidth;
	}

	public int getXDensity() {
		return xDensity;
	}
	
	public int getYDensity() {
		return yDensity;
	}
	
	public Iterator<MetadataEntry> iterator() {
		ensureDataRead();
		List<MetadataEntry> entries = new ArrayList<MetadataEntry>();
		String[] densityUnits = {"No units, aspect ratio only specified", "Dots per inch", "Dots per centimeter"};
		entries.add(new MetadataEntry("Version", majorVersion + "." + minorVersion));
		entries.add(new MetadataEntry("Density unit", (densityUnit <= 2)?densityUnits[densityUnit]:densityUnit + ""));
		entries.add(new MetadataEntry("XDensity", xDensity + ""));
		entries.add(new MetadataEntry("YDensity", yDensity + ""));
		entries.add(new MetadataEntry("Thumbnail width", thumbnailWidth + ""));
		entries.add(new MetadataEntry("Thumbnail height", thumbnailHeight + ""));
		
		return Collections.unmodifiableCollection(entries).iterator();
	}
	
	public void read() throws IOException {
		if(!isDataRead) {
			int expectedLen = 9;
			int offset = 0;
			
			if (data.length >= expectedLen) {
				majorVersion = data[offset++]&0xff;
				minorVersion = data[offset++]&0xff;
				densityUnit = data[offset++]&0xff;
				xDensity = IOUtils.readUnsignedShortMM(data, offset);
				offset += 2;
				yDensity = IOUtils.readUnsignedShortMM(data, offset);
				offset += 2;
				thumbnailWidth = data[offset++]&0xff;
				thumbnailHeight = data[offset]&0xff;
				if(thumbnailWidth != 0 && thumbnailHeight != 0) {
					containsThumbnail = true;
					// Extract the thumbnail
		    		//Create a Bitmap
		    		int size = 3*thumbnailWidth*thumbnailHeight;
					int[] colors = MetadataUtils.toARGB(ArrayUtils.subArray(data, expectedLen, size));
					thumbnail = new JFIFThumbnail(Bitmap.createBitmap(colors, thumbnailWidth, thumbnailHeight, Bitmap.Config.ARGB_8888));
				}
			}
			
		    isDataRead = true;
		}		
	}

	public void write(OutputStream os) throws IOException {
		ensureDataRead();
		IOUtils.write(os, majorVersion);
		IOUtils.write(os, minorVersion);
		IOUtils.write(os, densityUnit);
		IOUtils.writeShortMM(os, getXDensity());
		IOUtils.writeShortMM(os, getYDensity());
		IOUtils.write(os, thumbnailWidth);
		IOUtils.write(os, thumbnailHeight);
		if(containsThumbnail)
			thumbnail.write(os);
	}
}
