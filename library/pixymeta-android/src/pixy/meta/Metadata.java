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
 * Metadata.java
 *
 * Who   Date       Description
 * ====  =========  ======================================================
 * WY    26Sep2015  Added insertComment(InputStream, OutputStream, String}
 * WY    06Jul2015  Added insertXMP(InputSream, OutputStream, XMP)
 * WY    16Apr2015  Changed insertIRB() parameter List to Collection
 * WY    16Apr2015  Removed ICC_Profile related code
 * WY    13Mar2015  initial creation
 */

package pixy.meta;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import android.graphics.Bitmap;
import pixy.meta.Metadata;
import pixy.util.MetadataUtils;
import pixy.meta.MetadataReader;
import pixy.meta.MetadataType;
import pixy.meta.adobe._8BIM;
import pixy.meta.bmp.BMPMeta;
import pixy.meta.exif.Exif;
import pixy.meta.gif.GIFMeta;
import pixy.meta.iptc.IPTCDataSet;
import pixy.meta.jpeg.JPGMeta;
import pixy.meta.png.PNGMeta;
import pixy.meta.tiff.TIFFMeta;
import pixy.meta.xmp.XMP;
import pixy.image.ImageType;
import pixy.io.FileCacheRandomAccessInputStream;
import pixy.io.FileCacheRandomAccessOutputStream;
import pixy.io.PeekHeadInputStream;
import pixy.io.RandomAccessInputStream;
import pixy.io.RandomAccessOutputStream;

/**
 * Base class for image metadata.
 *  
 * @author Wen Yu, yuwen_66@yahoo.com
 * @version 1.0 01/12/2015
 */
public abstract class Metadata implements MetadataReader, Iterable<MetadataEntry> {
	public static final int IMAGE_MAGIC_NUMBER_LEN = 4;
	// Fields
	private MetadataType type;
	protected byte[] data;
	protected boolean isDataRead;
	
	// Obtain a logger instance
	private static final Logger LOGGER = LoggerFactory.getLogger(Metadata.class);		
	
	public static void  extractThumbnails(File image, String pathToThumbnail) throws IOException {
		FileInputStream fin = new FileInputStream(image);
		extractThumbnails(fin, pathToThumbnail);
		fin.close();
	}
	
	public static void extractThumbnails(InputStream is, String pathToThumbnail) throws IOException {
		// ImageIO.IMAGE_MAGIC_NUMBER_LEN bytes as image magic number
		PeekHeadInputStream peekHeadInputStream = new PeekHeadInputStream(is, IMAGE_MAGIC_NUMBER_LEN);
		ImageType imageType = MetadataUtils.guessImageType(peekHeadInputStream);		
		// Delegate thumbnail extracting to corresponding image tweaker.
		switch(imageType) {
			case JPG:
				JPGMeta.extractThumbnails(peekHeadInputStream, pathToThumbnail);
				break;
			case TIFF:
				RandomAccessInputStream randIS = new FileCacheRandomAccessInputStream(peekHeadInputStream);
				TIFFMeta.extractThumbnail(randIS, pathToThumbnail);
				randIS.shallowClose();
				break;
			case PNG:
				LOGGER.info("PNG image format does not contain any thumbnail");
				break;
			case GIF:
			case PCX:
			case TGA:
			case BMP:
				LOGGER.info("{} image format does not contain any thumbnails", imageType);
				break;
			default:
				peekHeadInputStream.close();
				throw new IllegalArgumentException("Thumbnail extracting is not supported for " + imageType + " image");				
		}
		peekHeadInputStream.shallowClose();
	}
	
	public static void extractThumbnails(String image, String pathToThumbnail) throws IOException {
		extractThumbnails(new File(image), pathToThumbnail);
	}
	
	public static void insertComment(InputStream is, OutputStream os, String comment) throws IOException {
		insertComments(is, os, Arrays.asList(comment));
	}
	
	public static void insertComments(InputStream is, OutputStream os, List<String> comments) throws IOException {
		// ImageIO.IMAGE_MAGIC_NUMBER_LEN bytes as image magic number
		PeekHeadInputStream peekHeadInputStream = new PeekHeadInputStream(is, IMAGE_MAGIC_NUMBER_LEN);
		ImageType imageType = MetadataUtils.guessImageType(peekHeadInputStream);		
		// Delegate IPTC inserting to corresponding image tweaker.
		switch(imageType) {
			case JPG:
				JPGMeta.insertComments(peekHeadInputStream, os, comments);
				break;
			case TIFF:
				RandomAccessInputStream randIS = new FileCacheRandomAccessInputStream(peekHeadInputStream);
				RandomAccessOutputStream randOS = new FileCacheRandomAccessOutputStream(os);
				TIFFMeta.insertComments(comments, randIS, randOS);
				randIS.shallowClose();
				randOS.shallowClose();
				break;
			case PNG:
				PNGMeta.insertComments(peekHeadInputStream, os, comments);
				break;
			case GIF:
				GIFMeta.insertComments(peekHeadInputStream, os, comments);
				break;
			case PCX:
			case TGA:
			case BMP:
				LOGGER.info("{} image format does not support comment data", imageType);
				break;
			default:
				peekHeadInputStream.close();
				throw new IllegalArgumentException("comment data inserting is not supported for " + imageType + " image");				
		}
		peekHeadInputStream.shallowClose();
	}
	
	/**
	 * @param is input image stream 
	 * @param os output image stream
	 * @param exif Exif instance
	 * @throws IOException 
	 */
	public static void insertExif(InputStream is, OutputStream os, Exif exif) throws IOException {
		insertExif(is, os, exif, false);
	}
	
	/**
	 * @param is input image stream 
	 * @param os output image stream
	 * @param exif Exif instance
	 * @param update true to keep the original data, otherwise false
	 * @throws IOException 
	 */
	public static void insertExif(InputStream is, OutputStream os, Exif exif, boolean update) throws IOException {
		// ImageIO.IMAGE_MAGIC_NUMBER_LEN bytes as image magic number
		PeekHeadInputStream peekHeadInputStream = new PeekHeadInputStream(is, IMAGE_MAGIC_NUMBER_LEN);
		ImageType imageType = MetadataUtils.guessImageType(peekHeadInputStream);		
		// Delegate EXIF inserting to corresponding image tweaker.
		switch(imageType) {
			case JPG:
				JPGMeta.insertExif(peekHeadInputStream, os, exif, update);
				break;
			case TIFF:
				RandomAccessInputStream randIS = new FileCacheRandomAccessInputStream(peekHeadInputStream);
				RandomAccessOutputStream randOS = new FileCacheRandomAccessOutputStream(os);
				TIFFMeta.insertExif(randIS, randOS, exif, update);
				randIS.shallowClose();
				randOS.shallowClose();
				break;
			case GIF:
			case PCX:
			case TGA:
			case BMP:
			case PNG:
				LOGGER.info("{} image format does not support EXIF data", imageType);
				break;
			default:
				peekHeadInputStream.close();
				throw new IllegalArgumentException("EXIF data inserting is not supported for " + imageType + " image");				
		}
		peekHeadInputStream.shallowClose();
	}
	
	public static void insertICCProfile(InputStream is, OutputStream out, byte[] icc_profile) throws IOException {
		// ImageIO.IMAGE_MAGIC_NUMBER_LEN bytes as image magic number
		PeekHeadInputStream peekHeadInputStream = new PeekHeadInputStream(is, IMAGE_MAGIC_NUMBER_LEN);
		ImageType imageType = MetadataUtils.guessImageType(peekHeadInputStream);		
		// Delegate ICCP inserting to corresponding image tweaker.
		switch(imageType) {
			case JPG:
				JPGMeta.insertICCProfile(peekHeadInputStream, out, icc_profile);
				break;
			case TIFF:
				RandomAccessInputStream randIS = new FileCacheRandomAccessInputStream(peekHeadInputStream);
				RandomAccessOutputStream randOS = new FileCacheRandomAccessOutputStream(out);
				TIFFMeta.insertICCProfile(icc_profile, 0, randIS, randOS);
				randIS.shallowClose();
				randOS.shallowClose();
				break;
			case GIF:
			case PCX:
			case TGA:
			case BMP:
				LOGGER.info("{} image format does not support ICCProfile data", imageType);
				break;
			default:
				peekHeadInputStream.close();
				throw new IllegalArgumentException("ICCProfile data inserting is not supported for " + imageType + " image");				
		}
		peekHeadInputStream.shallowClose();
	}

	public static void insertIPTC(InputStream is, OutputStream out, Collection<IPTCDataSet> iptcs) throws IOException {
		insertIPTC(is, out, iptcs, false);
	}
	
	public static void insertIPTC(InputStream is, OutputStream out, Collection<IPTCDataSet> iptcs, boolean update) throws IOException {
		// ImageIO.IMAGE_MAGIC_NUMBER_LEN bytes as image magic number
		PeekHeadInputStream peekHeadInputStream = new PeekHeadInputStream(is, IMAGE_MAGIC_NUMBER_LEN);
		ImageType imageType = MetadataUtils.guessImageType(peekHeadInputStream);		
		// Delegate IPTC inserting to corresponding image tweaker.
		switch(imageType) {
			case JPG:
				JPGMeta.insertIPTC(peekHeadInputStream, out, iptcs, update);
				break;
			case TIFF:
				RandomAccessInputStream randIS = new FileCacheRandomAccessInputStream(peekHeadInputStream);
				RandomAccessOutputStream randOS = new FileCacheRandomAccessOutputStream(out);
				TIFFMeta.insertIPTC(randIS, randOS, iptcs, update);
				randIS.shallowClose();
				randOS.shallowClose();
				break;
			case PNG:
			case GIF:
			case PCX:
			case TGA:
			case BMP:
				LOGGER.info("{} image format does not support IPTC data", imageType);
				break;
			default:
				peekHeadInputStream.close();
				throw new IllegalArgumentException("IPTC data inserting is not supported for " + imageType + " image");				
		}
		peekHeadInputStream.shallowClose();
	}
	
	public static void insertIRB(InputStream is, OutputStream out, Collection<_8BIM> bims) throws IOException {
		insertIRB(is, out, bims, false);
	}
	
	public static void insertIRB(InputStream is, OutputStream os, Collection<_8BIM> bims, boolean update) throws IOException {
		// ImageIO.IMAGE_MAGIC_NUMBER_LEN bytes as image magic number
		PeekHeadInputStream peekHeadInputStream = new PeekHeadInputStream(is, IMAGE_MAGIC_NUMBER_LEN);
		ImageType imageType = MetadataUtils.guessImageType(peekHeadInputStream);		
		// Delegate IRB inserting to corresponding image tweaker.
		switch(imageType) {
			case JPG:
				JPGMeta.insertIRB(peekHeadInputStream, os, bims, update);
				break;
			case TIFF:
				RandomAccessInputStream randIS = new FileCacheRandomAccessInputStream(peekHeadInputStream);
				RandomAccessOutputStream randOS = new FileCacheRandomAccessOutputStream(os);
				TIFFMeta.insertIRB(randIS, randOS, bims, update);
				randIS.shallowClose();
				randOS.shallowClose();
				break;
			case PNG:
			case GIF:
			case PCX:
			case TGA:
			case BMP:
				LOGGER.info("{} image format does not support IRB data", imageType);
				break;
			default:
				peekHeadInputStream.close();
				throw new IllegalArgumentException("IRB data inserting is not supported for " + imageType + " image");				
		}
		peekHeadInputStream.shallowClose();
	}
	
	public static void insertIRBThumbnail(InputStream is, OutputStream out, Bitmap thumbnail) throws IOException {
		// ImageIO.IMAGE_MAGIC_NUMBER_LEN bytes as image magic number
		PeekHeadInputStream peekHeadInputStream = new PeekHeadInputStream(is, IMAGE_MAGIC_NUMBER_LEN);
		ImageType imageType = MetadataUtils.guessImageType(peekHeadInputStream);		
		// Delegate IRB thumbnail inserting to corresponding image tweaker.
		switch(imageType) {
			case JPG:
				JPGMeta.insertIRBThumbnail(peekHeadInputStream, out, thumbnail);
				break;
			case TIFF:
				RandomAccessInputStream randIS = new FileCacheRandomAccessInputStream(peekHeadInputStream);
				RandomAccessOutputStream randOS = new FileCacheRandomAccessOutputStream(out);
				TIFFMeta.insertThumbnail(randIS, randOS, thumbnail);
				randIS.shallowClose();
				randOS.shallowClose();
				break;
			case PNG:
			case GIF:
			case PCX:
			case TGA:
			case BMP:
				LOGGER.info("{} image format does not support IRB thumbnail", imageType);
				break;
			default:
				peekHeadInputStream.close();
				throw new IllegalArgumentException("IRB thumbnail inserting is not supported for " + imageType + " image");				
		}
		peekHeadInputStream.shallowClose();
	}
	
	public static void insertXMP(InputStream is, OutputStream out, XMP xmp) throws IOException {
		// ImageIO.IMAGE_MAGIC_NUMBER_LEN bytes as image magic number
		PeekHeadInputStream peekHeadInputStream = new PeekHeadInputStream(is, IMAGE_MAGIC_NUMBER_LEN);
		ImageType imageType = MetadataUtils.guessImageType(peekHeadInputStream);		
		// Delegate XMP inserting to corresponding image tweaker.
		switch(imageType) {
			case JPG:
				JPGMeta.insertXMP(peekHeadInputStream, out, xmp); // No ExtendedXMP
				break;
			case TIFF:
				RandomAccessInputStream randIS = new FileCacheRandomAccessInputStream(peekHeadInputStream);
				RandomAccessOutputStream randOS = new FileCacheRandomAccessOutputStream(out);
				TIFFMeta.insertXMP(xmp, randIS, randOS);
				randIS.shallowClose();
				randOS.shallowClose();
				break;
			case PNG:
				PNGMeta.insertXMP(peekHeadInputStream, out, xmp);
				break;
			case GIF:
				GIFMeta.insertXMPApplicationBlock(peekHeadInputStream, out, xmp);
				break;
			case PCX:
			case TGA:
			case BMP:
				LOGGER.info("{} image format does not support XMP data", imageType);
				break;
			default:
				peekHeadInputStream.close();
				throw new IllegalArgumentException("XMP inserting is not supported for " + imageType + " image");				
		}
		peekHeadInputStream.shallowClose();
	}
	
	public static void insertXMP(InputStream is, OutputStream out, String xmp) throws IOException {
		// ImageIO.IMAGE_MAGIC_NUMBER_LEN bytes as image magic number
		PeekHeadInputStream peekHeadInputStream = new PeekHeadInputStream(is, IMAGE_MAGIC_NUMBER_LEN);
		ImageType imageType = MetadataUtils.guessImageType(peekHeadInputStream);		
		// Delegate XMP inserting to corresponding image tweaker.
		switch(imageType) {
			case JPG:
				JPGMeta.insertXMP(peekHeadInputStream, out, xmp, null); // No ExtendedXMP
				break;
			case TIFF:
				RandomAccessInputStream randIS = new FileCacheRandomAccessInputStream(peekHeadInputStream);
				RandomAccessOutputStream randOS = new FileCacheRandomAccessOutputStream(out);
				TIFFMeta.insertXMP(xmp, randIS, randOS);
				randIS.shallowClose();
				randOS.shallowClose();
				break;
			case PNG:
				PNGMeta.insertXMP(peekHeadInputStream, out, xmp);
				break;
			case GIF:
				GIFMeta.insertXMPApplicationBlock(peekHeadInputStream, out, xmp);
				break;
			case PCX:
			case TGA:
			case BMP:
				LOGGER.info("{} image format does not support XMP data", imageType);
				break;
			default:
				peekHeadInputStream.close();
				throw new IllegalArgumentException("XMP inserting is not supported for " + imageType + " image");				
		}
		peekHeadInputStream.shallowClose();
	}
	
	public static Map<MetadataType, Metadata> readMetadata(File image) throws IOException {
		FileInputStream fin = new FileInputStream(image);
		Map<MetadataType, Metadata> metadataMap = readMetadata(fin);
		fin.close();
		
		return metadataMap; 
	}
	
	/**
	 * Reads all metadata associated with the input image
	 *
	 * @param is InputStream for the image
	 * @return a list of Metadata for the input stream
	 * @throws IOException
	 */
	public static Map<MetadataType, Metadata> readMetadata(InputStream is) throws IOException {
		// Metadata map for all the Metadata read
		Map<MetadataType, Metadata> metadataMap = new HashMap<MetadataType, Metadata>();
		// ImageIO.IMAGE_MAGIC_NUMBER_LEN bytes as image magic number
		PeekHeadInputStream peekHeadInputStream = new PeekHeadInputStream(is, IMAGE_MAGIC_NUMBER_LEN);
		ImageType imageType = MetadataUtils.guessImageType(peekHeadInputStream);		
		// Delegate metadata reading to corresponding image tweakers.
		switch(imageType) {
			case JPG:
				metadataMap = JPGMeta.readMetadata(peekHeadInputStream);
				break;
			case TIFF:
				RandomAccessInputStream randIS = new FileCacheRandomAccessInputStream(peekHeadInputStream);
				metadataMap = TIFFMeta.readMetadata(randIS);
				randIS.shallowClose();
				break;
			case PNG:
				metadataMap = PNGMeta.readMetadata(peekHeadInputStream);
				break;
			case GIF:
				metadataMap = GIFMeta.readMetadata(peekHeadInputStream);
				break;
			case BMP:
				metadataMap = BMPMeta.readMetadata(peekHeadInputStream);
				break;
			default:
				peekHeadInputStream.close();
				throw new IllegalArgumentException("Metadata reading is not supported for " + imageType + " image");
				
		}	
		peekHeadInputStream.shallowClose();
		
		return metadataMap;
	}
	
	public static Map<MetadataType, Metadata> readMetadata(String image) throws IOException {
		return readMetadata(new File(image));
	}
	
	/**
	 * Remove meta data from image
	 * 
	 * @param is InputStream for the input image
	 * @param os OutputStream for the output image
	 * @throws IOException
	 */
	public static Map<MetadataType, Metadata> removeMetadata(InputStream is, OutputStream os, MetadataType ...metadataTypes) throws IOException {
		// ImageIO.IMAGE_MAGIC_NUMBER_LEN bytes as image magic number
		PeekHeadInputStream peakHeadInputStream = new PeekHeadInputStream(is, IMAGE_MAGIC_NUMBER_LEN);
		ImageType imageType = MetadataUtils.guessImageType(peakHeadInputStream);
		// The map of removed metadata
		Map<MetadataType, Metadata> metadataMap = Collections.emptyMap();
		// Delegate meta data removing to corresponding image tweaker.
		switch(imageType) {
			case JPG:
				metadataMap = JPGMeta.removeMetadata(peakHeadInputStream, os, metadataTypes);
				break;
			case TIFF:
				RandomAccessInputStream randIS = new FileCacheRandomAccessInputStream(peakHeadInputStream);
				RandomAccessOutputStream randOS = new FileCacheRandomAccessOutputStream(os);
				metadataMap = TIFFMeta.removeMetadata(randIS, randOS, metadataTypes);
				randIS.shallowClose();
				randOS.shallowClose();
				break;
			case PCX:
			case TGA:
			case BMP:
				LOGGER.info("{} image format does not support meta data", imageType);
				break;
			default:
				peakHeadInputStream.close();
				throw new IllegalArgumentException("Metadata removing is not supported for " + imageType + " image");				
		}
		
		peakHeadInputStream.shallowClose();
		
		return metadataMap;
	}
	
	public Metadata(MetadataType type) {
		this.type = type;
	}
	
	public Metadata(MetadataType type, byte[] data) {
		if(type == null) throw new IllegalArgumentException("Metadata type must be specified");
		if(data == null) throw new IllegalArgumentException("Input data array is null");
		if(data.length == 0) isDataRead = true; // Allow for zero length data but disable read
		this.type = type;
		this.data = data;		
	}
	
	public void ensureDataRead() {
		if(!isDataRead) {
			try {
				read();
			} catch (IOException e) {
				e.printStackTrace();
			}			
		}
	}
	
	public byte[] getData() {
		if(data != null)
			return data.clone();
		
		return null;
	}
	
	public MetadataType getType() {
		return type;
	}
	
	public boolean isDataRead() {
		return isDataRead;
	}
	
	/**
	 * Writes the metadata out to the output stream
	 * 
	 * @param out OutputStream to write the metadata to
	 * @throws IOException
	 */
	public void write(OutputStream out) throws IOException {
		byte[] data = getData();
		if(data != null)
			out.write(data);
	}	
}
