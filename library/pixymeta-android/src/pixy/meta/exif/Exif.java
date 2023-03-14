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
 * Exif.java
 *
 * Who   Date       Description
 * ====  =======    =================================================
 * WY    10Apr2015  Moved data loaded checking to ExifReader
 * WY    31Mar2015  Fixed bug with getImageIFD() etc
 * WY    13Mar2015  Initial creation
 */

package pixy.meta.exif;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import android.graphics.Bitmap;
import pixy.meta.Metadata;
import pixy.meta.MetadataEntry;
import pixy.meta.MetadataType;
import pixy.meta.Thumbnail;
import pixy.meta.tiff.TIFFMeta;
import pixy.string.StringUtils;
import pixy.image.tiff.FieldType;
import pixy.image.tiff.IFD;
import pixy.image.tiff.Tag;
import pixy.image.tiff.TiffField;
import pixy.image.tiff.TiffTag;
import pixy.io.FileCacheRandomAccessInputStream;
import pixy.io.FileCacheRandomAccessOutputStream;
import pixy.io.IOUtils;
import pixy.io.RandomAccessInputStream;
import pixy.io.RandomAccessOutputStream;

/**
 * EXIF wrapper
 *  
 * @author Wen Yu, yuwen_66@yahoo.com
 * @version 1.0 03/13/2014
 */
public abstract class Exif extends Metadata {
	protected IFD imageIFD;
	protected IFD exifSubIFD;
	protected IFD gpsSubIFD;
	protected IFD interopSubIFD;
	protected ExifThumbnail thumbnail;
	protected short preferedEndian = IOUtils.BIG_ENDIAN;
	
	private boolean containsThumbnail;
	private boolean isThumbnailRequired;
	
	public static final int FIRST_IFD_OFFSET = 0x08;
	
	// Obtain a logger instance
	private static final Logger LOGGER = LoggerFactory.getLogger(Exif.class);
	
	public Exif() {
		super(MetadataType.EXIF);
		isDataRead = true;
	}
	
	public Exif(byte[] data) {
		super(MetadataType.EXIF, data);
		ensureDataRead();
	}
	
	public Exif(IFD imageIFD) {
		this();
		setImageIFD(imageIFD);
	}
	
	public Exif(InputStream is) throws IOException {
		this(IOUtils.inputStreamToByteArray(is));
	}
	
	public void addExifField(ExifTag tag, FieldType type, Object data) {
		if(exifSubIFD == null)
			exifSubIFD = new IFD();
		TiffField<?> field = FieldType.createField(tag, type, data);
		if(field != null)
			exifSubIFD.addField(field);
		else
			throw new IllegalArgumentException("Cannot create required EXIF TIFF field");
	}
	
	public void addGPSField(GPSTag tag, FieldType type, Object data) {
		if(gpsSubIFD == null)
			gpsSubIFD = new IFD();
		TiffField<?> field = FieldType.createField(tag, type, data);
		if(field != null)
			gpsSubIFD.addField(field);
		else
			throw new IllegalArgumentException("Cannot create required GPS TIFF field");
	}
	
	public void addInteropField(InteropTag tag, FieldType type, Object data) {
		if(interopSubIFD == null)
			interopSubIFD = new IFD();
		TiffField<?> field = FieldType.createField(tag, type, data);
		if(field != null)
			interopSubIFD.addField(field);
		else
			throw new IllegalArgumentException("Cannot create required interop TIFF field");
	}
	
	public void addImageField(TiffTag tag, FieldType type, Object data) {
		if(imageIFD == null)
			imageIFD = new IFD();
		TiffField<?> field = FieldType.createField(tag, type, data);
		if(field != null)
			imageIFD.addField(field);
		else
			throw new IllegalArgumentException("Cannot create required Image TIFF field");
	}
	
	public boolean containsThumbnail() {
		if(containsThumbnail)
			return true;
		if(thumbnail != null)
			return true;
		return false;
	}
	
	public IFD getExifIFD() {
		if(exifSubIFD != null) {
			return new IFD(exifSubIFD);
		}
		
		return null;
	}
	
	public IFD getGPSIFD() {
		if(gpsSubIFD != null) {
			return new IFD(gpsSubIFD);
		} 

		return null;
	}
	
	public IFD getInteropIFD() {
		if(interopSubIFD != null) {
			return new IFD(interopSubIFD);
		} 

		return null;
	}
	
	public IFD getImageIFD() {
		if(imageIFD != null) {
			return new IFD(imageIFD);
		}
		
		return null;		
	}
	
	private void getMetadataEntries(IFD currIFD, Class<? extends Tag> tagClass, List<MetadataEntry> items) {
		// Use reflection to invoke fromShort(short) method
		Method method = null;
		try {
			method = tagClass.getDeclaredMethod("fromShort", short.class);
		} catch (NoSuchMethodException e) {
			throw new RuntimeException("Method 'fromShort' is not defined for class " + tagClass);
		} catch (SecurityException e) {
			throw new RuntimeException("The operation is not allowed by the current security setup");
		}
		
		Collection<TiffField<?>> fields = currIFD.getFields();
		MetadataEntry entry = null;
		
		if(tagClass.equals(TiffTag.class)) {
			entry = new MetadataEntry("IFD0", "Image Info", true);
		} else if(tagClass.equals(ExifTag.class)) {
			entry = new MetadataEntry("EXIF SubIFD", "EXIF Info", true);
		} else if(tagClass.equals(GPSTag.class)) {
			entry = new MetadataEntry("GPS SubIFD", "GPS Info", true);
		} else if(tagClass.equals(InteropTag.class)) {
			entry = new MetadataEntry("Interoperability SubIFD", "Interoperability Info", true);
		} else
			entry = new MetadataEntry("UNKNOWN", "UNKNOWN SubIFD", true);
		
		for(TiffField<?> field : fields) {
			short tag = field.getTag();
			Tag ftag = TiffTag.UNKNOWN;
			if(tag == ExifTag.PADDING.getValue()) {
				ftag = ExifTag.PADDING;
			} else if(tag == ExifTag.EXIF_INTEROPERABILITY_OFFSET.getValue()) {
				ftag = ExifTag.EXIF_INTEROPERABILITY_OFFSET;
			} else {
				try {
					ftag = (Tag)method.invoke(null, tag);
				} catch (IllegalAccessException e) {
					throw new RuntimeException("Illegal access for method: " + method);
				} catch (IllegalArgumentException e) {
					throw new RuntimeException("Illegal argument for method:  " + method);
				} catch (InvocationTargetException e) {
					throw new RuntimeException("Incorrect invocation target");
				}
			}	
			if (ftag == TiffTag.UNKNOWN)
				LOGGER.warn("Tag: {} [Value: 0x{}] (Unknown)", ftag, Integer.toHexString(tag&0xffff));
			
			FieldType ftype = field.getType();				
						
			String tagString = null;
			if(ftype == FieldType.SHORT || ftype == FieldType.SSHORT)
				tagString = ftag.getFieldAsString(field.getDataAsLong());
			else
				tagString = ftag.getFieldAsString(field.getData());
			if(StringUtils.isNullOrEmpty(tagString))
				entry.addEntry(new MetadataEntry(ftag.getName(), field.getDataAsString()));
			else
				entry.addEntry(new MetadataEntry(ftag.getName(), tagString));
		}
		
		items.add(entry); // Add the Entry (group) into the collection
		
		Map<Tag, IFD> children = currIFD.getChildren();
		
		if(children.get(TiffTag.EXIF_SUB_IFD) != null) {
			getMetadataEntries(children.get(TiffTag.EXIF_SUB_IFD), ExifTag.class, items);
		}
		
		if(children.get(ExifTag.EXIF_INTEROPERABILITY_OFFSET) != null) {
			getMetadataEntries(children.get(ExifTag.EXIF_INTEROPERABILITY_OFFSET), InteropTag.class, items);
		}
		
		if(children.get(TiffTag.GPS_SUB_IFD) != null) {
			getMetadataEntries(children.get(TiffTag.GPS_SUB_IFD), GPSTag.class, items);
		}		
	}

	public ExifThumbnail getThumbnail() {
		if(thumbnail != null)
			return new ExifThumbnail(thumbnail);
	
		return null;
	}
	
	public boolean isThumbnailRequired() {
		return isThumbnailRequired;
	}
	
	public short getPreferedEndian() {
		return preferedEndian;
	}
	
	public Iterator<MetadataEntry> iterator() {
		ensureDataRead();
		List<MetadataEntry> items = new ArrayList<MetadataEntry>();
		if(imageIFD != null)
			getMetadataEntries(imageIFD, TiffTag.class, items);
		if(containsThumbnail) {
			MetadataEntry thumbnailEntry = new MetadataEntry("IFD1", "Thumbnail Image", true);
			thumbnailEntry.addEntry(new MetadataEntry("Thumbnail format", (thumbnail.getDataType() == 1? "DATA_TYPE_KJpegRGB":"DATA_TYPE_TIFF")));
			thumbnailEntry.addEntry(new MetadataEntry("Thumbnail data length", "" + thumbnail.getCompressedImage().length));
			items.add(thumbnailEntry);
		}
	
		return Collections.unmodifiableList(items).iterator();
	}
	
	public void read() throws IOException {
		if(!isDataRead) {
			RandomAccessInputStream exifIn = new FileCacheRandomAccessInputStream(new ByteArrayInputStream(data));
			List<IFD> ifds = new ArrayList<IFD>(3);
			TIFFMeta.readIFDs(ifds, exifIn);
			
			preferedEndian = exifIn.getEndian();
			
			if(ifds.size() > 0) {
				imageIFD = ifds.get(0);
				exifSubIFD = imageIFD.getChild(TiffTag.EXIF_SUB_IFD);
				if(exifSubIFD != null)
					interopSubIFD = exifSubIFD.getChild(ExifTag.EXIF_INTEROPERABILITY_OFFSET);
				gpsSubIFD = imageIFD.getChild(TiffTag.GPS_SUB_IFD);
			}
		    // We have thumbnail IFD
		    if(ifds.size() >= 2) {
		    	IFD thumbnailIFD = ifds.get(1);
		    	int width = -1;
		    	int height = -1;
		    	TiffField<?> field = thumbnailIFD.getField(TiffTag.IMAGE_WIDTH);
		    	if(field != null) 
		    		width = field.getDataAsLong()[0];
		    	field = thumbnailIFD.getField(TiffTag.IMAGE_LENGTH);
		    	if(field != null)
		    		height = field.getDataAsLong()[0];
		    	field = thumbnailIFD.getField(TiffTag.JPEG_INTERCHANGE_FORMAT);
		    	if(field != null) { // JPEG format, save as JPEG
		    		int thumbnailOffset = field.getDataAsLong()[0];
		    		field = thumbnailIFD.getField(TiffTag.JPEG_INTERCHANGE_FORMAT_LENGTH);
		    		int thumbnailLen = field.getDataAsLong()[0];
		    		exifIn.seek(thumbnailOffset);
		    		byte[] thumbnailData = new byte[thumbnailLen];
		    		exifIn.readFully(thumbnailData);
		    		thumbnail = new ExifThumbnail(width, height, Thumbnail.DATA_TYPE_KJpegRGB, thumbnailData, thumbnailIFD);
		    		containsThumbnail = true;				    
		    	} else { // Uncompressed TIFF
		    		field = thumbnailIFD.getField(TiffTag.STRIP_OFFSETS);
		    		if(field == null) 
		    			field = thumbnailIFD.getField(TiffTag.TILE_OFFSETS);
		    		if(field != null) {
		    			 exifIn.seek(0);
		    			 ByteArrayOutputStream bout = new ByteArrayOutputStream();
		    			 RandomAccessOutputStream tiffout = new FileCacheRandomAccessOutputStream(bout);
		    			 TIFFMeta.retainPages(exifIn, tiffout, 1);
		    			 tiffout.close(); // Auto flush when closed
		    			 thumbnail = new ExifThumbnail(width, height, Thumbnail.DATA_TYPE_TIFF, bout.toByteArray(), thumbnailIFD);
		    			 containsThumbnail = true;		    			    
		    		}
		    	}
		    }
		    exifIn.shallowClose();		
		    isDataRead = true;
		}
	}
	
	public void setExifIFD(IFD exifSubIFD) {
		this.exifSubIFD = exifSubIFD;
	}
	
	public void setGPSIFD(IFD gpsSubIFD) {
		this.gpsSubIFD = gpsSubIFD;
	}
	
	public void setInteropIFD(IFD interopSubIFD) {
		this.interopSubIFD = interopSubIFD;
	}
	
	public void setImageIFD(IFD imageIFD) {
		if(imageIFD == null)
			throw new IllegalArgumentException("Input image IFD is null");
		this.imageIFD = imageIFD;
		this.exifSubIFD = imageIFD.getChild(TiffTag.EXIF_SUB_IFD);
		this.gpsSubIFD = imageIFD.getChild(TiffTag.GPS_SUB_IFD);
	}
	
	/**
	 * @param thumbnail a Thumbnail instance. If null, a thumbnail
	 *        will be generated from the input image.
	 */	
	public void setThumbnail(ExifThumbnail thumbnail) {
		this.thumbnail = thumbnail;
	}
	
	public void setThumbnailImage(Bitmap thumbnail) {
		if(this.thumbnail == null)
			this.thumbnail = new ExifThumbnail(); 
		this.thumbnail.setImage(thumbnail);
	}
	
	public void setThumbnailRequired(boolean isThumbnailRequired) {
		this.isThumbnailRequired = isThumbnailRequired;
	}
	
	public void setPreferedEndian(short preferedEndian) {
		if(preferedEndian != IOUtils.BIG_ENDIAN && preferedEndian != IOUtils.LITTLE_ENDIAN)
			throw new IllegalArgumentException("Invalid Exif endian!");
		this.preferedEndian = preferedEndian;
	}
		
	public abstract void write(OutputStream os) throws IOException;
}
