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
 * ThumbnailResource.java
 * <p>
 * Adobe thumbnail resource wrapper
 *
 * Who   Date       Description
 * ====  =========  ===================================================
 * WY    09Apr2016  Added new constructor
 * WY    14Apr2015  Fixed a bug with super() call, changed data to null 
 * WY    14Apr2015  Added new constructor
 * WY    13Apr2015  Initial creation
 */

package pixy.meta.adobe;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import android.graphics.Bitmap;
import pixy.io.IOUtils;
import pixy.meta.Thumbnail;
import pixy.util.ArrayUtils;
import pixy.util.MetadataUtils;

public class ThumbnailResource extends _8BIM {
	// Check to make sure id is either ImageResourceID.THUMBNAIL_RESOURCE_PS4
	// or ImageResourceID.THUMBNAIL_RESOURCE_PS5
	private static ImageResourceID validateID(ImageResourceID id) {
		if(id != ImageResourceID.THUMBNAIL_RESOURCE_PS4 && id != ImageResourceID.THUMBNAIL_RESOURCE_PS5)
			throw new IllegalArgumentException("Unsupported thumbnail ImageResourceID: " + id);
		
		return id;
	}	
	// Fields
	private int width;
	private int height;
	//Padded row bytes = (width * bits per pixel + 31) / 32 * 4.
	private int paddedRowBytes;
	// Total size = widthbytes * height * planes
	private int totalSize;
	// Size after compression. Used for consistency check.
	private int compressedSize;
	// Bits per pixel. = 24
	private int bitsPerPixel;
	// Number of planes. = 1
	private int numOfPlanes;
	private ImageResourceID id;
	private int dataType;
	// Thumbnail
	private IRBThumbnail thumbnail = new IRBThumbnail();
	
	public ThumbnailResource(Bitmap thumbnail) {
		this("THUMBNAIL_RESOURCE", thumbnail);
	}
		
	public ThumbnailResource(String name, Bitmap thumbnail) {
		super(ImageResourceID.THUMBNAIL_RESOURCE_PS5, name, null);
		try {
			this.thumbnail = createThumbnail(thumbnail);
		} catch (IOException e) {
			throw new RuntimeException("Unable to create IRBThumbnail from Bitmap");
		}
	}
	
	// id is either ImageResourceID.THUMBNAIL_RESOURCE_PS4 or ImageResourceID.THUMBNAIL_RESOURCE_PS5
	public ThumbnailResource(ImageResourceID id, int dataType, int width, int height, byte[] thumbnailData) {
		super(validateID(id), "THUMBNAIL_RESOURCE", null);
		// Initialize fields
		this.id = id;
		this.dataType = dataType;
		/** Sometimes, we don't have information about width and height */
		this.width = (width > 0)? width : 0; 
		this.height = (height > 0)? height : 0;
		// paddedRowBytes = (width * bitsPerPixel + 31) / 32 * 4.
		// totalSize = paddedRowBytes * height * numOfPlanes
		this.paddedRowBytes = (width * 24 + 31)/32 * 4;
		this.totalSize = paddedRowBytes * height * numOfPlanes;
		this.compressedSize = thumbnailData.length;
		this.bitsPerPixel = 24;
		this.numOfPlanes = 1;
		setThumbnailImage(id, dataType, width, height, totalSize, thumbnailData);
	}
	
	// id is either ImageResourceID.THUMBNAIL_RESOURCE_PS4 or ImageResourceID.THUMBNAIL_RESOURCE_PS5
	public ThumbnailResource(ImageResourceID id, byte[] data) {
		super(validateID(id), "THUMBNAIL_RESOURCE", data);
		this.id = id;
		read();
	}
	
	public ThumbnailResource(ImageResourceID id,Thumbnail thumbnail) {
		this(id, thumbnail.getDataType(), thumbnail.getWidth(), thumbnail.getHeight(), thumbnail.getCompressedImage());
	}
	
	private IRBThumbnail createThumbnail(Bitmap thumbnail) throws IOException {
		// Create memory buffer to write data
		ByteArrayOutputStream bout = new ByteArrayOutputStream();
		// Compress the thumbnail
		try {
			thumbnail.compress(Bitmap.CompressFormat.JPEG, 100, bout);
		} catch (Exception e) {
			e.printStackTrace();
		}
		byte[] data = bout.toByteArray();
		this.id = ImageResourceID.THUMBNAIL_RESOURCE_PS5;
		// Write thumbnail dimension
		this.width = thumbnail.getWidth();
		this.height = thumbnail.getHeight();
		// Padded row bytes = (width * bits per pixel + 31) / 32 * 4.
		this.bitsPerPixel = 24;
		this.numOfPlanes = 1;
		this.paddedRowBytes = (width*bitsPerPixel + 31)/32*4;
		// Total size = widthbytes * height * planes
		this.totalSize = paddedRowBytes*height*numOfPlanes;
		// Size after compression. Used for consistency check.
		this.compressedSize = data.length;
		this.dataType = Thumbnail.DATA_TYPE_KJpegRGB;
			
		return new IRBThumbnail(width, height, dataType, data);
	}
	
	public int getBitsPerPixel() {
		return bitsPerPixel;
	}
	
	public int getCompressedSize() {
		return compressedSize;
	}
	
	public int getDataType() {
		return dataType;
	}
	
	public int getHeight() {
		return height;
	}
	
	public int getNumOfPlanes() {
		return numOfPlanes;
	}
	
	public int getPaddedRowBytes() {
		return paddedRowBytes;
	}
	
	public ImageResourceID getResouceID() {
		return id;
	}
	
	public IRBThumbnail getThumbnail() {
		return new IRBThumbnail(thumbnail);
	}
	
	public int getTotalSize() {
		return totalSize;		
	}
	
	public int getWidth() {
		return width;
	}
	
	private void setThumbnailImage(ImageResourceID id, int dataType, int width, int height, int totalSize, byte[] thumbnailData) {
		// JFIF data in RGB format. For resource ID 1033 (0x0409) the data is in BGR format.
		if(dataType == Thumbnail.DATA_TYPE_KJpegRGB) {
			thumbnail.setImage(width, height, dataType, thumbnailData);
		} else if(dataType == Thumbnail.DATA_TYPE_KRawRGB) {
			// kRawRGB - NOT tested yet!
			int[] colors = null;
			if(id == ImageResourceID.THUMBNAIL_RESOURCE_PS4)
				colors = MetadataUtils.bgr2ARGB(thumbnailData);
			else if(id == ImageResourceID.THUMBNAIL_RESOURCE_PS5)
				colors = MetadataUtils.toARGB(thumbnailData);
			thumbnail.setImage(Bitmap.createBitmap(colors, width, height, Bitmap.Config.ARGB_8888));
		} else
			throw new UnsupportedOperationException("Unsupported IRB thumbnail data type: " + dataType);
	}
	
	private void read() {
		this.dataType = IOUtils.readIntMM(data, 0); //1 = kJpegRGB. Also supports kRawRGB (0).
		this.width = IOUtils.readIntMM(data, 4);
		this.height = IOUtils.readIntMM(data, 8);
		// Padded row bytes = (width * bits per pixel + 31) / 32 * 4.
		this.paddedRowBytes = IOUtils.readIntMM(data, 12);
		// Total size = widthbytes * height * planes
		this.totalSize = IOUtils.readIntMM(data, 16);
		// Size after compression. Used for consistency check.
		this.compressedSize = IOUtils.readIntMM(data, 20);
		this.bitsPerPixel = IOUtils.readShortMM(data, 24); // Bits per pixel. = 24
		this.numOfPlanes = IOUtils.readShortMM(data, 26); // Number of planes. = 1
		byte[] thumbnailData = null;
		if(dataType == Thumbnail.DATA_TYPE_KJpegRGB)
			thumbnailData = ArrayUtils.subArray(data, 28, compressedSize);
		else if(dataType == Thumbnail.DATA_TYPE_KRawRGB)
			thumbnailData = ArrayUtils.subArray(data, 28, totalSize);
		setThumbnailImage(id, dataType, width, height, totalSize, thumbnailData);
	}
		
	public void write(OutputStream os) throws IOException {
		if(data == null) {			
			ByteArrayOutputStream bout = new ByteArrayOutputStream();
			thumbnail.write(bout);
			byte[] compressedData = bout.toByteArray();
			bout.reset();
			// Write thumbnail format
			IOUtils.writeIntMM(bout, dataType);
			IOUtils.writeIntMM(bout, width);
			IOUtils.writeIntMM(bout, height);
			IOUtils.writeIntMM(bout, paddedRowBytes);
			// Total size = widthbytes * height * planes
			IOUtils.writeIntMM(bout, totalSize);
			// Size after compression. Used for consistency check.
			IOUtils.writeIntMM(bout, compressedData.length);
			IOUtils.writeShortMM(bout, bitsPerPixel);
			IOUtils.writeShortMM(bout, numOfPlanes);
			bout.write(compressedData);
			data = bout.toByteArray();
			size = data.length;
		}
		super.write(os);
	}	
}
