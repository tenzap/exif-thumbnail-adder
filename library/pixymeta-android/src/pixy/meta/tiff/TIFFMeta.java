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
 * TIFFMeta.java
 *
 * Who   Date       Description
 * ====  =========  =====================================================================
 * WY    21Jun2019  Added code for removeMetadata to return the removed metadata as a map
 * WY    14May2019  Write IPTC to normal TIFF IPTC tag instead of PhotoShop IRB block
 * WY    06Jul2015  Added insertXMP(InputSream, OutputStream, XMP)
 * WY    15Apr2015  Changed the argument type for insertIPTC() and insertIRB()
 * WY    07Apr2015  Removed insertICCProfile() AWT related code
 * WY    07Apr2015  Merge Adobe IRB IPTC and TIFF IPTC data if both exist
 * WY    13Mar2015  Initial creation
 */

package pixy.meta.tiff;

import java.io.ByteArrayOutputStream;
import java.io.EOFException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;

import pixy.meta.Metadata;
import pixy.meta.MetadataType;
import pixy.meta.adobe.DDB;
import pixy.meta.adobe.IRB;
import pixy.meta.adobe.IRBThumbnail;
import pixy.meta.adobe.ThumbnailResource;
import pixy.meta.adobe.ImageResourceID;
import pixy.meta.adobe._8BIM;
import pixy.meta.exif.Exif;
import pixy.meta.exif.ExifTag;
import pixy.meta.exif.GPSTag;
import pixy.meta.exif.InteropTag;
import pixy.meta.icc.ICCProfile;
import pixy.meta.image.Comments;
import pixy.meta.iptc.IPTC;
import pixy.meta.iptc.IPTCDataSet;
import pixy.meta.iptc.IPTCTag;
import pixy.meta.xmp.XMP;
import pixy.image.jpeg.Marker;
import pixy.image.tiff.ASCIIField;
import pixy.image.tiff.ByteField;
import pixy.image.tiff.DoubleField;
import pixy.image.tiff.FieldType;
import pixy.image.tiff.FloatField;
import pixy.image.tiff.IFD;
import pixy.image.tiff.IFDField;
import pixy.image.tiff.LongField;
import pixy.image.tiff.RationalField;
import pixy.image.tiff.SRationalField;
import pixy.image.tiff.ShortField;
import pixy.image.tiff.Tag;
import pixy.image.tiff.TiffField;
import pixy.image.tiff.TiffFieldEnum;
import pixy.image.tiff.TiffTag;
import pixy.image.tiff.UndefinedField;
import pixy.image.tiff.TIFFImage;
import pixy.io.IOUtils;
import pixy.io.RandomAccessInputStream;
import pixy.io.RandomAccessOutputStream;
import pixy.io.ReadStrategy;
import pixy.io.ReadStrategyII;
import pixy.io.ReadStrategyMM;
import pixy.io.WriteStrategyII;
import pixy.io.WriteStrategyMM;
import pixy.string.StringUtils;
import pixy.string.XMLUtils;
import pixy.util.ArrayUtils;
import android.graphics.*;

public class TIFFMeta {
	// Offset where to write the value of the first IFD offset
	public static final int OFFSET_TO_WRITE_FIRST_IFD_OFFSET = 0x04;
	public static final int FIRST_WRITE_OFFSET = 0x08;
	public static final int STREAM_HEAD = 0x00;
	
	// Obtain a logger instance
	private static final Logger LOGGER = LoggerFactory.getLogger(TIFFMeta.class);
	
	private static int copyHeader(RandomAccessInputStream rin, RandomAccessOutputStream rout) throws IOException {		
		rin.seek(STREAM_HEAD);
		// First 2 bytes determine the byte order of the file, "MM" or "II"
	    short endian = rin.readShort();
	
		if (endian == IOUtils.BIG_ENDIAN) {
		    rin.setReadStrategy(ReadStrategyMM.getInstance());
		    rout.setWriteStrategy(WriteStrategyMM.getInstance());
		} else if(endian == IOUtils.LITTLE_ENDIAN) {
		    rin.setReadStrategy(ReadStrategyII.getInstance());
		    rout.setWriteStrategy(WriteStrategyII.getInstance());
		} else {
			rin.close();
			rout.close();
			throw new RuntimeException("Invalid TIFF byte order");
	    } 
		
		rout.writeShort(endian);
		// Read TIFF identifier
		rin.seek(0x02);
		short tiff_id = rin.readShort();
		
		if(tiff_id!=0x2a)//"*" 42 decimal
		{
		   rin.close();
		   rout.close();
		   throw new RuntimeException("Invalid TIFF identifier");
		}
		
		rout.writeShort(tiff_id);
		rin.seek(OFFSET_TO_WRITE_FIRST_IFD_OFFSET);
		
		return rin.readInt();
	}
	
	private static Collection<IPTCDataSet> copyIPTCDataSet(Collection<IPTCDataSet> iptcs, byte[] data) throws IOException {
		IPTC iptc = new IPTC(data);
		// Shallow copy the map
		Map<IPTCTag, List<IPTCDataSet>> dataSetMap = new HashMap<IPTCTag, List<IPTCDataSet>>(iptc.getDataSets());
		for(IPTCDataSet set : iptcs)
			if(!set.allowMultiple())
				dataSetMap.remove(set.getTagEnum());
		for(List<IPTCDataSet> iptcList : dataSetMap.values())
			iptcs.addAll(iptcList);
		
		return iptcs;
	}
	
	private static TiffField<?> copyJpegHufTable(RandomAccessInputStream rin, RandomAccessOutputStream rout, TiffField<?> field, int curPos) throws IOException
	{
		int[] data = field.getDataAsLong();
		int[] tmp = new int[data.length];
	
		for(int i = 0; i < data.length; i++) {
			rin.seek(data[i]);
			tmp[i] = curPos;
			byte[] htable = new byte[16];
			IOUtils.readFully(rin, htable);
			IOUtils.write(rout, htable);			
			curPos += 16;
			
			int numCodes = 0;
			
            for(int j = 0; j < 16; j++) {
                numCodes += htable[j]&0xff;
            }
            
            curPos += numCodes;
            
            htable = new byte[numCodes];
            IOUtils.readFully(rin, htable);
			IOUtils.write(rout, htable);
		}
		
		if(TiffTag.fromShort(field.getTag()) == TiffTag.JPEG_AC_TABLES)
			return new LongField(TiffTag.JPEG_AC_TABLES.getValue(), tmp);
	
		return new LongField(TiffTag.JPEG_DC_TABLES.getValue(), tmp);
	}
	
	private static void copyJpegIFByteCount(RandomAccessInputStream rin, RandomAccessOutputStream rout, int offset, int outOffset) throws IOException {		
		boolean finished = false;
		int length = 0;	
		short marker;
		Marker emarker;
		
		rin.seek(offset);
		rout.seek(outOffset);
		// The very first marker should be the start_of_image marker!	
		if(Marker.fromShort(IOUtils.readShortMM(rin)) != Marker.SOI) {
			return;
		}
		
		IOUtils.writeShortMM(rout, Marker.SOI.getValue());
		
		marker = IOUtils.readShortMM(rin);
			
		while (!finished) {	        
			if (Marker.fromShort(marker) == Marker.EOI) {
				IOUtils.writeShortMM(rout, marker);
				finished = true;
			} else { // Read markers
		  		emarker = Marker.fromShort(marker);
				
				switch (emarker) {
					case JPG: // JPG and JPGn shouldn't appear in the image.
					case JPG0:
					case JPG13:
				    case TEM: // The only stand alone mark besides SOI, EOI, and RSTn. 
				    	marker = IOUtils.readShortMM(rin);
				    	break;
				    case SOS:						
						marker = copyJpegSOS(rin, rout);
						break;
				    case PADDING:	
				    	int nextByte = 0;
				    	while((nextByte = rin.read()) == 0xff) {;}
				    	marker = (short)((0xff<<8)|nextByte);
				    	break;
				    default:
					    length = IOUtils.readUnsignedShortMM(rin);
					    byte[] buf = new byte[length - 2];
					    rin.read(buf);
					    IOUtils.writeShortMM(rout, marker);
					    IOUtils.writeShortMM(rout, length);
					    rout.write(buf);
					    marker = IOUtils.readShortMM(rin);					 
				}
			}
	    }
	}
	
	private static TiffField<?> copyJpegQTable(RandomAccessInputStream rin, RandomAccessOutputStream rout, TiffField<?> field, int curPos) throws IOException
	{
		byte[] qtable = new byte[64];
		int[] data = field.getDataAsLong();
		int[] tmp = new int[data.length];
		
		for(int i = 0; i < data.length; i++) {
			rin.seek(data[i]);
			tmp[i] = curPos;
			IOUtils.readFully(rin, qtable);
			IOUtils.write(rout, qtable);
			curPos += 64;
		}
		
		return new LongField(TiffTag.JPEG_Q_TABLES.getValue(), tmp);
	}
	
	private static short copyJpegSOS(RandomAccessInputStream rin, RandomAccessOutputStream rout) throws IOException	{
		int len = IOUtils.readUnsignedShortMM(rin);
		byte buf[] = new byte[len - 2];
		IOUtils.readFully(rin, buf);
		IOUtils.writeShortMM(rout, Marker.SOS.getValue());
		IOUtils.writeShortMM(rout, len);
		rout.write(buf);		
		// Actual image data follow.
		int nextByte = 0;
		short marker = 0;	
		
		while((nextByte = IOUtils.read(rin)) != -1)	{
			rout.write(nextByte);
			
			if(nextByte == 0xff)
			{
				nextByte = IOUtils.read(rin);
			    rout.write(nextByte);
			    
				if (nextByte == -1) {
					throw new IOException("Premature end of SOS segment!");					
				}								
				
				if (nextByte != 0x00) {
					marker = (short)((0xff<<8)|nextByte);
					
					switch (Marker.fromShort(marker)) {										
						case RST0:  
						case RST1:
						case RST2:
						case RST3:
						case RST4:
						case RST5:
						case RST6:
						case RST7:
							continue;
						default:
					}
					break;
				}
			}
		}
		
		if (nextByte == -1) {
			throw new IOException("Premature end of SOS segment!");
		}

		return marker;
	}
	
	/**
	 * @param offset offset to write page image data
	 * 
	 * @return the position where to write the IFD for the current image page
	 */
	private static int copyPageData(IFD ifd, int offset, RandomAccessInputStream rin, RandomAccessOutputStream rout) throws IOException {
		int writeOffset = offset; // We copy the offset to a local variable to keep the original value
		int writeByteCount = 0; // To fix JPEG data double copy issue
		
		// Move stream pointer to the right place
		rout.seek(writeOffset);

		// Original image data start from these offsets.
		TiffField<?> stripOffSets = ifd.removeField(TiffTag.STRIP_OFFSETS);
		
		if(stripOffSets == null)
			stripOffSets = ifd.removeField(TiffTag.TILE_OFFSETS);
				
		TiffField<?> stripByteCounts = ifd.getField(TiffTag.STRIP_BYTE_COUNTS);
		
		if(stripByteCounts == null)
			stripByteCounts = ifd.getField(TiffTag.TILE_BYTE_COUNTS);	
		/* 
		 * Make sure this will work in the case when neither STRIP_OFFSETS nor TILE_OFFSETS presents.
		 * Not sure if this will ever happen for TIFF. JPEG EXIF data do not contain these fields. 
		 */
		if(stripOffSets != null) { 
			int[] counts = stripByteCounts.getDataAsLong();		
			int[] off = stripOffSets.getDataAsLong();
			int[] temp = new int[off.length];
			
			TiffField<?> tiffField = ifd.getField(TiffTag.COMPRESSION);
			
			// Uncompressed image with one strip or tile (may contain wrong StripByteCounts value)
			// Bug fix for uncompressed image with one strip and wrong StripByteCounts value
			if((tiffField == null ) || (tiffField != null && tiffField.getDataAsLong()[0] == 1)) { // Uncompressed data
				int planaryConfiguration = 1;
				
				tiffField = ifd.getField(TiffTag.PLANAR_CONFIGURATTION);		
				if(tiffField != null) planaryConfiguration = tiffField.getDataAsLong()[0];
				
				tiffField = ifd.getField(TiffTag.SAMPLES_PER_PIXEL);
				
				int samplesPerPixel = 1;
				if(tiffField != null) samplesPerPixel = tiffField.getDataAsLong()[0];
				
				// If there is only one strip/samplesPerPixel strips for PlanaryConfiguration = 2
				if((planaryConfiguration == 1 && off.length == 1) || (planaryConfiguration == 2 && off.length == samplesPerPixel)) {
					int[] totalBytes2Read = getBytes2Read(ifd);
				
					for(int i = 0; i < off.length; i++)
						counts[i] = totalBytes2Read[i];					
				}				
			} // End of bug fix
			
			writeByteCount = counts[0];
			
			// We are going to write the image data first
			rout.seek(writeOffset);
			
			// Copy image data from offset
			for(int i = 0; i < off.length; i++) {
				rin.seek(off[i]);
				byte[] buf = new byte[counts[i]];
				rin.readFully(buf);
				rout.write(buf);
				temp[i] = writeOffset;
				writeOffset += buf.length;
			}
						
			if(ifd.getField(TiffTag.STRIP_BYTE_COUNTS) != null)
				ifd.addField(new LongField(TiffTag.STRIP_OFFSETS.getValue(), temp));
			else
				ifd.addField(new LongField(TiffTag.TILE_OFFSETS.getValue(), temp));		
		}
		
		// Add software field.
		String softWare = "ICAFE - https://github.com/dragon66/icafe\0";
		ifd.addField(new ASCIIField(TiffTag.SOFTWARE.getValue(), softWare));
		
		/* The following are added to work with old-style JPEG compression (type 6) */		
		/* One of the flavors (found in JPEG EXIF thumbnail IFD - IFD1) of the old JPEG compression contains this field */
		TiffField<?> jpegIFOffset = ifd.removeField(TiffTag.JPEG_INTERCHANGE_FORMAT);
		if(jpegIFOffset != null) {
			TiffField<?> jpegIFByteCount = ifd.removeField(TiffTag.JPEG_INTERCHANGE_FORMAT_LENGTH);
			if(jpegIFOffset.getDataAsLong()[0] != stripOffSets.getDataAsLong()[0]) {
				try {
					if(jpegIFByteCount != null) {
						rin.seek(jpegIFOffset.getDataAsLong()[0]);
						byte[] bytes2Read = new byte[jpegIFByteCount.getDataAsLong()[0]];
						rin.readFully(bytes2Read);
						rout.seek(writeOffset);
						rout.write(bytes2Read);
						ifd.addField(jpegIFByteCount);
					} else {
						long startOffset = rout.getStreamPointer();					
						copyJpegIFByteCount(rin, rout, jpegIFOffset.getDataAsLong()[0], writeOffset);
						long endOffset = rout.getStreamPointer();
						ifd.addField(new LongField(TiffTag.JPEG_INTERCHANGE_FORMAT_LENGTH.getValue(), new int[]{(int)(endOffset - startOffset)}));
					}
					jpegIFOffset = new LongField(TiffTag.JPEG_INTERCHANGE_FORMAT.getValue(), new int[]{writeOffset});
					ifd.addField(jpegIFOffset);
				} catch (EOFException ex) {;};
			} else { // To fix the issue of double copy the JPEG data, we can safely re-assign the pointers.
				ifd.addField(new LongField(TiffTag.JPEG_INTERCHANGE_FORMAT.getValue(), new int[]{offset}));
				ifd.addField(new LongField(TiffTag.JPEG_INTERCHANGE_FORMAT_LENGTH.getValue(), new int[]{writeByteCount}));
			}
		}		
		/* Another flavor of the old style JPEG compression type 6 contains separate tables */
		TiffField<?> jpegTable = ifd.removeField(TiffTag.JPEG_DC_TABLES);
		if(jpegTable != null) {
			try {
				ifd.addField(copyJpegHufTable(rin, rout, jpegTable, (int)rout.getStreamPointer()));
			} catch(EOFException ex) {;}
		}
		
		jpegTable = ifd.removeField(TiffTag.JPEG_AC_TABLES);
		if(jpegTable != null) {
			try {
				ifd.addField(copyJpegHufTable(rin, rout, jpegTable, (int)rout.getStreamPointer()));
			} catch(EOFException ex) {;}
		}
	
		jpegTable = ifd.removeField(TiffTag.JPEG_Q_TABLES);
		if(jpegTable != null) {
			try {
				ifd.addField(copyJpegQTable(rin, rout, jpegTable, (int)rout.getStreamPointer()));
			} catch(EOFException ex) {;}
		}
		/* End of code to work with old-style JPEG compression */
		
		// Return the actual stream position (we may have lost track of it)  
		return (int)rout.getStreamPointer();	
	}
	
	// Copy a list of IFD and associated image data if any
	private static int copyPages(List<IFD> list, int writeOffset, RandomAccessInputStream rin, RandomAccessOutputStream rout) throws IOException {
		// Write the first page data
		writeOffset = copyPageData(list.get(0), writeOffset, rin, rout);
		// Then write the first IFD
		writeOffset = list.get(0).write(rout, writeOffset);
		// We are going to write the remaining image pages and IFDs if any
		for(int i = 1; i < list.size(); i++) {
			writeOffset = copyPageData(list.get(i), writeOffset, rin, rout);
			// Tell the IFD to update next IFD offset for the following IFD
			list.get(i-1).setNextIFDOffset(rout, writeOffset); 
			writeOffset = list.get(i).write(rout, writeOffset);
		}
		
		return writeOffset;
	}
	
	/**
	 * Extracts ICC_Profile from certain page of TIFF if any
	 * 
	 * @param pageNumber page number from which to extract ICC_Profile
	 * @param rin RandomAccessInputStream for the input TIFF
	 * @return a byte array for the extracted ICC_Profile or null if none exists
	 * @throws Exception
	 */
	public static byte[] extractICCProfile(int pageNumber, RandomAccessInputStream rin) throws Exception {
		// Read pass image header
		int offset = readHeader(rin);
		// Read the IFDs into a list first
		List<IFD> ifds = new ArrayList<IFD>();
		readIFDs(null, null, TiffTag.class, ifds, offset, rin);
		
		if(pageNumber < 0 || pageNumber >= ifds.size())
			throw new IllegalArgumentException("pageNumber " + pageNumber + " out of bounds: 0 - " + (ifds.size() - 1));
		
		IFD workingPage = ifds.get(pageNumber);
		TiffField<?> f_iccProfile = workingPage.getField(TiffTag.ICC_PROFILE);
		if(f_iccProfile != null) {
			return (byte[])f_iccProfile.getData();
		}
		
		return null;
	}
	
	public static byte[] extractICCProfile(RandomAccessInputStream rin) throws Exception {
		return extractICCProfile(0, rin);
	}
	
	public static IRBThumbnail extractThumbnail(int pageNumber, RandomAccessInputStream rin) throws IOException {
		// Read pass image header
		int offset = readHeader(rin);
		// Read the IFDs into a list first
		List<IFD> ifds = new ArrayList<IFD>();
		readIFDs(null, null, TiffTag.class, ifds, offset, rin);
		
		if(pageNumber < 0 || pageNumber >= ifds.size())
			throw new IllegalArgumentException("pageNumber " + pageNumber + " out of bounds: 0 - " + (ifds.size() - 1));
		
		IFD workingPage = ifds.get(pageNumber);
		TiffField<?> f_photoshop = workingPage.getField(TiffTag.PHOTOSHOP);
		if(f_photoshop != null) {
			byte[] data = (byte[])f_photoshop.getData();
			IRB irb = new IRB(data);
			if(irb.containsThumbnail()) {
				IRBThumbnail thumbnail = irb.getThumbnail();
				return thumbnail;					
			}		
		}
		
		return null;
	}
	
	public static IRBThumbnail extractThumbnail(RandomAccessInputStream rin) throws IOException {
		return extractThumbnail(0, rin);
	}
	
	public static void extractThumbnail(RandomAccessInputStream rin, String pathToThumbnail) throws IOException {
		IRBThumbnail thumbnail = extractThumbnail(rin);				
		if(thumbnail != null) {
			String outpath = "";
			if(pathToThumbnail.endsWith("\\") || pathToThumbnail.endsWith("/"))
				outpath = pathToThumbnail + "photoshop_thumbnail.jpg";
			else
				outpath = pathToThumbnail.replaceFirst("[.][^.]+$", "") + "_photoshop_t.jpg";
			FileOutputStream fout = new FileOutputStream(outpath);
			if(thumbnail.getDataType() == IRBThumbnail.DATA_TYPE_KJpegRGB) {
				fout.write(thumbnail.getCompressedImage());
			} else {
				Bitmap bm = thumbnail.getRawImage();
				try {
					bm.compress(Bitmap.CompressFormat.JPEG, 100, fout);
				} catch (Exception e) {
					throw new IOException("Writing thumbnail failed!");
				}
			}
			fout.close();	
		}			
	}
	
	// Used to calculate how many bytes to read in case we have only one strip or tile
	private static int[] getBytes2Read(IFD ifd) {
		// Let's calculate how many bytes we are supposed to read
		TiffField<?> tiffField = ifd.getField(TiffTag.IMAGE_WIDTH);
		int imageWidth = tiffField.getDataAsLong()[0];
		tiffField = ifd.getField(TiffTag.IMAGE_LENGTH);
		int imageHeight = tiffField.getDataAsLong()[0];
		
		// For YCbCr image only
		int horizontalSampleFactor = 2; // Default 2X2
		int verticalSampleFactor = 2; // Not 1X1
		
		int photoMetric = ifd.getField(TiffTag.PHOTOMETRIC_INTERPRETATION).getDataAsLong()[0];
		
		// Correction for imageWidth and imageHeight for YCbCr image
		if(photoMetric == TiffFieldEnum.PhotoMetric.YCbCr.getValue()) {
			TiffField<?> f_YCbCrSubSampling = ifd.getField(TiffTag.YCbCr_SUB_SAMPLING);
			
			if(f_YCbCrSubSampling != null) {
				int[] sampleFactors = f_YCbCrSubSampling.getDataAsLong();
				horizontalSampleFactor = sampleFactors[0];
				verticalSampleFactor = sampleFactors[1];
			}
			imageWidth = ((imageWidth + horizontalSampleFactor - 1)/horizontalSampleFactor)*horizontalSampleFactor;
			imageHeight = ((imageHeight + verticalSampleFactor - 1)/verticalSampleFactor)*verticalSampleFactor;	
		}
		
		int samplesPerPixel = 1;
		
		tiffField = ifd.getField(TiffTag.SAMPLES_PER_PIXEL);
		if(tiffField != null) {
			samplesPerPixel = tiffField.getDataAsLong()[0];
		}				
		
		int bitsPerSample = 1;
		
		tiffField = ifd.getField(TiffTag.BITS_PER_SAMPLE);
		if(tiffField != null) {
			bitsPerSample = tiffField.getDataAsLong()[0];
		}
		
		int tileWidth = -1;
		int tileLength = -1;			
		
		TiffField<?> f_tileLength = ifd.getField(TiffTag.TILE_LENGTH);
		TiffField<?> f_tileWidth = ifd.getField(TiffTag.TILE_WIDTH);
		
		if(f_tileWidth != null) {
			tileWidth = f_tileWidth.getDataAsLong()[0];
			tileLength = f_tileLength.getDataAsLong()[0];
		}
		
		int rowsPerStrip = imageHeight;
		int rowWidth = imageWidth;
		
		TiffField<?> f_rowsPerStrip = ifd.getField(TiffTag.ROWS_PER_STRIP);
		if(f_rowsPerStrip != null) rowsPerStrip = f_rowsPerStrip.getDataAsLong()[0];					
		
		if(rowsPerStrip > imageHeight) rowsPerStrip = imageHeight;
		
		if(tileWidth > 0) {
			rowsPerStrip = tileLength;
			rowWidth = tileWidth;
		}
	
		int planaryConfiguration = 1;
		
		tiffField = ifd.getField(TiffTag.PLANAR_CONFIGURATTION);
		if(tiffField != null) planaryConfiguration = tiffField.getDataAsLong()[0];
		
		int[] totalBytes2Read = new int[samplesPerPixel];		
		
		if(planaryConfiguration == 1)
			totalBytes2Read[0] = ((rowWidth*bitsPerSample*samplesPerPixel + 7)/8)*rowsPerStrip;
		else
			totalBytes2Read[0] = totalBytes2Read[1] = totalBytes2Read[2] = ((rowWidth*bitsPerSample + 7)/8)*rowsPerStrip;
		
		if(photoMetric == TiffFieldEnum.PhotoMetric.YCbCr.getValue()) {
			if(samplesPerPixel != 3) samplesPerPixel = 3;
			
			int[] sampleBytesPerRow = new int[samplesPerPixel];
			sampleBytesPerRow[0] = (bitsPerSample*rowWidth + 7)/8;
			sampleBytesPerRow[1] = (bitsPerSample*rowWidth/horizontalSampleFactor + 7)/8;
			sampleBytesPerRow[2] = sampleBytesPerRow[1];
			
			int[] sampleRowsPerStrip = new int[samplesPerPixel];
			sampleRowsPerStrip[0] = rowsPerStrip;
			sampleRowsPerStrip[1] = rowsPerStrip/verticalSampleFactor;
			sampleRowsPerStrip[2]= sampleRowsPerStrip[1];
			
			totalBytes2Read[0] = sampleBytesPerRow[0]*sampleRowsPerStrip[0];
			totalBytes2Read[1] = sampleBytesPerRow[1]*sampleRowsPerStrip[1];
			totalBytes2Read[2] = totalBytes2Read[1];
		
			if(tiffField != null) planaryConfiguration = tiffField.getDataAsLong()[0];
		
			if(planaryConfiguration == 1)
				totalBytes2Read[0] = totalBytes2Read[0] + totalBytes2Read[1] + totalBytes2Read[2];			
		}
		
		return totalBytes2Read;
	}
	
	public static void insertComments(List<String> comments, RandomAccessInputStream rin, RandomAccessOutputStream rout) throws IOException {
		insertComments(comments, 0, rin, rout);
	}
		
	public static void insertComments(List<String> comments, int pageNumber, RandomAccessInputStream rin, RandomAccessOutputStream rout) throws IOException {
		int offset = copyHeader(rin, rout);
		// Read the IFDs into a list first
		List<IFD> ifds = new ArrayList<IFD>();
		readIFDs(null, null, TiffTag.class, ifds, offset, rin);
		
		if(pageNumber < 0 || pageNumber >= ifds.size())
			throw new IllegalArgumentException("pageNumber " + pageNumber + " out of bounds: 0 - " + (ifds.size() - 1));
		
		IFD workingPage = ifds.get(pageNumber);

		StringBuilder commentsBuilder = new StringBuilder();
		
		// ASCII field allows for multiple strings
		for(String comment : comments) {
			commentsBuilder.append(comment);
			commentsBuilder.append('\0');
		}
		
		workingPage.addField(new ASCIIField(TiffTag.IMAGE_DESCRIPTION.getValue(), commentsBuilder.toString()));
		
		offset = copyPages(ifds, offset, rin, rout);
		int firstIFDOffset = ifds.get(0).getStartOffset();	

		writeToStream(rout, firstIFDOffset);	
	}
	
	public static void insertExif(RandomAccessInputStream rin, RandomAccessOutputStream rout, Exif exif, boolean update) throws IOException {
		insertExif(rin, rout, exif, 0, update);
	}
	
	/**
	 * Insert EXIF data with optional thumbnail IFD
	 * 
	 * @param rin input image stream
	 * @param rout output image stream
	 * @param exif EXIF wrapper instance
	 * @param pageNumber page offset where to insert EXIF (zero based)
	 * @param update True to keep the original data, otherwise false
	 * @throws Exception
	 */
	public static void insertExif(RandomAccessInputStream rin, RandomAccessOutputStream rout, Exif exif, int pageNumber, boolean update) throws IOException {
		int offset = copyHeader(rin, rout);
		// Read the IFDs into a list first
		List<IFD> ifds = new ArrayList<IFD>();
		readIFDs(null, null, TiffTag.class, ifds, offset, rin);
		
		if(pageNumber < 0 || pageNumber >= ifds.size())
			throw new IllegalArgumentException("pageNumber " + pageNumber + " out of bounds: 0 - " + (ifds.size() - 1));
		
		IFD imageIFD = ifds.get(pageNumber);
		IFD exifSubIFD = imageIFD.getChild(TiffTag.EXIF_SUB_IFD);
		IFD gpsSubIFD = imageIFD.getChild(TiffTag.GPS_SUB_IFD);
		IFD interopSubIFD = (exifSubIFD != null)? exifSubIFD.getChild(ExifTag.EXIF_INTEROPERABILITY_OFFSET) : null;
		IFD newImageIFD = exif.getImageIFD();
		IFD newExifSubIFD = exif.getExifIFD();
		IFD newGpsSubIFD = exif.getGPSIFD();
		IFD newInteropSubIFD = exif.getInteropIFD();
		
		if(newImageIFD != null) {
			Collection<TiffField<?>> fields = newImageIFD.getFields();
			for(TiffField<?> field : fields) {
				Tag tag = TiffTag.fromShort(field.getTag());
				if(imageIFD.getField(tag) != null && tag.isCritical())
					throw new RuntimeException("Override of TIFF critical Tag - " + tag.getName() + " is not allowed!");
				imageIFD.addField(field);
			}
		}
		
		if(update && exifSubIFD != null && newExifSubIFD != null) {
			exifSubIFD.addFields(newExifSubIFD.getFields());
			newExifSubIFD = exifSubIFD;
		}
		
		if(newExifSubIFD != null) {
			imageIFD.addField(new LongField(TiffTag.EXIF_SUB_IFD.getValue(), new int[]{0})); // Place holder
			imageIFD.addChild(TiffTag.EXIF_SUB_IFD, newExifSubIFD);
			if(update && interopSubIFD != null && newInteropSubIFD != null) {
				interopSubIFD.addFields(newInteropSubIFD.getFields());
				newInteropSubIFD = interopSubIFD;
			}
			if(newInteropSubIFD != null) {
				newExifSubIFD.addField(new LongField(ExifTag.EXIF_INTEROPERABILITY_OFFSET.getValue(), new int[]{0})); // Place holder
				newExifSubIFD.addChild(ExifTag.EXIF_INTEROPERABILITY_OFFSET, newInteropSubIFD);		
			}
		}
		
		if(update && gpsSubIFD != null && newGpsSubIFD != null) {
			gpsSubIFD.addFields(newGpsSubIFD.getFields());
			newGpsSubIFD = gpsSubIFD;
		}
		
		if(newGpsSubIFD != null) {
			imageIFD.addField(new LongField(TiffTag.GPS_SUB_IFD.getValue(), new int[]{0})); // Place holder
			imageIFD.addChild(TiffTag.GPS_SUB_IFD, newGpsSubIFD);		
		}
		
		int writeOffset = FIRST_WRITE_OFFSET;
		// Copy pages
		writeOffset = copyPages(ifds, writeOffset, rin, rout);
		int firstIFDOffset = ifds.get(0).getStartOffset();

		writeToStream(rout, firstIFDOffset);
	}
	
	public static void insertICCProfile(byte[] icc_profile, RandomAccessInputStream rin, RandomAccessOutputStream rout) throws IOException {
		insertICCProfile(icc_profile, 0, rin, rout);
	}
	
	/**
	 * Insert ICC_Profile into TIFF page
	 * 
	 * @param icc_profile byte array holding the ICC_Profile
	 * @param pageNumber page offset where to insert ICC_Profile
	 * @param rin RandomAccessInputStream for the input image
	 * @param rout RandomAccessOutputStream for the output image
	 * @throws Exception
	 */
	public static void insertICCProfile(byte[] icc_profile, int pageNumber, RandomAccessInputStream rin, RandomAccessOutputStream rout) throws IOException {
		int offset = copyHeader(rin, rout);
		// Read the IFDs into a list first
		List<IFD> ifds = new ArrayList<IFD>();
		readIFDs(null, null, TiffTag.class, ifds, offset, rin);
		
		if(pageNumber < 0 || pageNumber >= ifds.size())
			throw new IllegalArgumentException("pageNumber " + pageNumber + " out of bounds: 0 - " + (ifds.size() - 1));
		
		IFD workingPage = ifds.get(pageNumber);
		workingPage.addField(new UndefinedField(TiffTag.ICC_PROFILE.getValue(), icc_profile));
		
		offset = copyPages(ifds, offset, rin, rout);
		int firstIFDOffset = ifds.get(0).getStartOffset();	

		writeToStream(rout, firstIFDOffset);	
	}
	
	public static void insertIPTC(RandomAccessInputStream rin, RandomAccessOutputStream rout, Collection<IPTCDataSet> iptcs, boolean update) throws IOException {
		insertIPTC(rin, rout, 0, iptcs, update);
	}
	
	/**
	 * Insert IPTC data into TIFF image. If the original TIFF image contains IPTC data, we either keep
	 * or override them depending on the input parameter "update."
	 * <p>
	 * There is a possibility that IPTC data presents in more than one places such as a normal TIFF
	 * tag, or buried inside a Photoshop IPTC-NAA Image Resource Block (IRB), or even in a XMP block.
	 * Currently this method does the following thing: if no IPTC data was found from both Photoshop or 
	 * normal IPTC tag, we insert the IPTC data with a normal IPTC tag. If IPTC data is found both as
	 * a Photoshop tag and a normal IPTC tag, depending on the "update" parameter, we will either delete
	 * the IPTC data from both places and insert the new IPTC data into the Photoshop tag or we will
	 * synchronize the two sets of IPTC data, delete the original IPTC from both places and insert the
	 * synchronized IPTC data along with the new IPTC data into the Photoshop tag. In both cases, we
	 * will keep the other IRBs from the original Photoshop tag unchanged. 
	 * 
	 * @param rin RandomAccessInputStream for the original TIFF
	 * @param rout RandomAccessOutputStream for the output TIFF with IPTC inserted
	 * @param pageNumber page offset where to insert IPTC
	 * @param iptcs A list of IPTCDataSet to insert into the TIFF image
	 * @param update whether we want to keep the original IPTC data or override it
	 *        completely new IPTC data set
	 * @throws IOException
	 */
	public static void insertIPTC(RandomAccessInputStream rin, RandomAccessOutputStream rout, int pageNumber, Collection<IPTCDataSet> iptcs, boolean update) throws IOException {
		int offset = copyHeader(rin, rout);
		// Read the IFDs into a list first
		List<IFD> ifds = new ArrayList<IFD>();
		readIFDs(null, null, TiffTag.class, ifds, offset, rin);
		
		if(pageNumber < 0 || pageNumber >= ifds.size())
			throw new IllegalArgumentException("pageNumber " + pageNumber + " out of bounds: 0 - " + (ifds.size() - 1));
		
		IFD workingPage = ifds.get(pageNumber);
	
		ByteArrayOutputStream bout = new ByteArrayOutputStream();
		
		// See if we also have regular IPTC tag field
		TiffField<?> f_iptc = workingPage.removeField(TiffTag.IPTC);		
		TiffField<?> f_photoshop = workingPage.getField(TiffTag.PHOTOSHOP);
		if(f_photoshop != null) { // Read 8BIMs
			IRB irb = new IRB((byte[])f_photoshop.getData());
			// Shallow copy the map.
			Map<Short, _8BIM> bims = new HashMap<Short, _8BIM>(irb.get8BIM());
			_8BIM photoshop_iptc = bims.remove(ImageResourceID.IPTC_NAA.getValue());
			if(photoshop_iptc != null) { // If we have IPTC
				if(update) { // If we need to keep the old data, copy it
					if(f_iptc != null) {// We are going to synchronize the two IPTC data
						byte[] data = null;
						if(f_iptc.getType() == FieldType.LONG)
							data = ArrayUtils.toByteArray(f_iptc.getDataAsLong(), rin.getEndian() == IOUtils.BIG_ENDIAN);
						else
							data = (byte[])f_iptc.getData();
						copyIPTCDataSet(iptcs, data);
					}
					// Now copy the Photoshop IPTC data
					copyIPTCDataSet(iptcs, photoshop_iptc.getData());
					// Remove duplicates
					iptcs = new ArrayList<IPTCDataSet>(new HashSet<IPTCDataSet>(iptcs));
				}
			}
			for(_8BIM bim : bims.values()) // Copy the other 8BIMs if any
				bim.write(bout);
			// Add a new Photoshop tag field to TIFF
			workingPage.addField(new UndefinedField(TiffTag.PHOTOSHOP.getValue(), bout.toByteArray()));
		} else { // We don't have photoshop, copy the old IPTC data in the IPTC tag is any
			if(f_iptc != null && update) {
				byte[] data = null;
				if(f_iptc.getType() == FieldType.LONG)
					data = ArrayUtils.toByteArray(f_iptc.getDataAsLong(), rin.getEndian() == IOUtils.BIG_ENDIAN);
				else
					data = (byte[])f_iptc.getData();
				copyIPTCDataSet(iptcs, data);
			}
		}
		
		// Sort the IPTCDataSet collection
		List<IPTCDataSet> iptcList = new ArrayList<IPTCDataSet>(iptcs);
		Collections.sort(iptcList);
		// Write IPTCDataSet collection
		bout.reset();
		for(IPTCDataSet dataset : iptcList) {
			dataset.write(bout);
		}
		// Add IPTC to regular IPTC tag field
		workingPage.addField(new UndefinedField(TiffTag.IPTC.getValue(), bout.toByteArray()));
		
		offset = copyPages(ifds, offset, rin, rout);
		int firstIFDOffset = ifds.get(0).getStartOffset();	

		writeToStream(rout, firstIFDOffset);	
	}
	
	public static void insertIRB(RandomAccessInputStream rin, RandomAccessOutputStream rout, Collection<_8BIM> bims, boolean update) throws IOException {
		insertIRB(rin, rout, 0, bims, update);
	}
	
	public static void insertIRB(RandomAccessInputStream rin, RandomAccessOutputStream rout, int pageNumber, Collection<_8BIM> bims, boolean update) throws IOException {
		int offset = copyHeader(rin, rout);
		// Read the IFDs into a list first
		List<IFD> ifds = new ArrayList<IFD>();
		readIFDs(null, null, TiffTag.class, ifds, offset, rin);
	
		if(pageNumber < 0 || pageNumber >= ifds.size())
			throw new IllegalArgumentException("pageNumber " + pageNumber + " out of bounds: 0 - " + (ifds.size() - 1));
		
		IFD workingPage = ifds.get(pageNumber);
		
		ByteArrayOutputStream bout = new ByteArrayOutputStream();
		
		if(update) {
			TiffField<?> f_irb = workingPage.getField(TiffTag.PHOTOSHOP);
			if(f_irb != null) {
				IRB irb = new IRB((byte[])f_irb.getData());
				// Shallow copy the map.
	    		Map<Short, _8BIM> bimMap = new HashMap<Short, _8BIM>(irb.get8BIM());
				for(_8BIM bim : bims) // Replace the original data
					bimMap.put(bim.getID(), bim);
				// In case we have two ThumbnailResource IRB, remove the Photoshop4.0 one
				if(bimMap.containsKey(ImageResourceID.THUMBNAIL_RESOURCE_PS4.getValue()) 
						&& bimMap.containsKey(ImageResourceID.THUMBNAIL_RESOURCE_PS5.getValue()))
					bimMap.remove(ImageResourceID.THUMBNAIL_RESOURCE_PS4.getValue());
				bims = bimMap.values();
			}
		}
		
		for(_8BIM bim : bims)
			bim.write(bout);
		
		workingPage.addField(new UndefinedField(TiffTag.PHOTOSHOP.getValue(), bout.toByteArray()));
		
		offset = copyPages(ifds, offset, rin, rout);
		int firstIFDOffset = ifds.get(0).getStartOffset();	

		writeToStream(rout, firstIFDOffset);	
	}
	
	/**
	 * Insert a thumbnail into PHOTOSHOP private tag field
	 *  
	 * @param rin RandomAccessInputStream for the input TIFF
	 * @param rout RandomAccessOutputStream for the output TIFF
	 * @param thumbnail a Bitmap to be inserted
	 * @throws Exception
	 */
	public static void insertThumbnail(RandomAccessInputStream rin, RandomAccessOutputStream rout, Bitmap thumbnail) throws IOException {
		// Sanity check
		if(thumbnail == null) throw new IllegalArgumentException("Input thumbnail is null");
		_8BIM bim = new ThumbnailResource(thumbnail);
		insertIRB(rin, rout, Arrays.asList(bim), true);
	}
	
	public static void insertXMP(XMP xmp, RandomAccessInputStream rin, RandomAccessOutputStream rout) throws IOException {
		insertXMP(xmp.getData(), rin, rout);
	}
	
	public static void insertXMP(XMP xmp, int pageNumber, RandomAccessInputStream rin, RandomAccessOutputStream rout) throws IOException {
		insertXMP(xmp.getData(), pageNumber, rin, rout);
	}
	
	public static void insertXMP(byte[] xmp, RandomAccessInputStream rin, RandomAccessOutputStream rout) throws IOException {
		insertXMP(xmp, 0, rin, rout);
	}
	
	/**
	 * Insert XMP data into TIFF image
	 * @param xmp byte array for the XMP data to be inserted
	 * @param pageNumber page offset where to insert XMP
	 * @param rin RandomAccessInputStream for the input image
	 * @param rout RandomAccessOutputStream for the output image
	 * @throws IOException
	 */
	public static void insertXMP(byte[] xmp, int pageNumber, RandomAccessInputStream rin, RandomAccessOutputStream rout) throws IOException {
		int offset = copyHeader(rin, rout);
		// Read the IFDs into a list first
		List<IFD> ifds = new ArrayList<IFD>();
		readIFDs(null, null, TiffTag.class, ifds, offset, rin);
		
		if(pageNumber < 0 || pageNumber >= ifds.size())
			throw new IllegalArgumentException("pageNumber " + pageNumber + " out of bounds: 0 - " + (ifds.size() - 1));
		
		IFD workingPage = ifds.get(pageNumber);
		workingPage.addField(new UndefinedField(TiffTag.XMP.getValue(), xmp));
		
		offset = copyPages(ifds, offset, rin, rout);
		int firstIFDOffset = ifds.get(0).getStartOffset();	

		writeToStream(rout, firstIFDOffset);	
	}
	
	public static void insertXMP(String xmp, RandomAccessInputStream rin, RandomAccessOutputStream rout) throws IOException {
		Document doc = XMLUtils.createXML(xmp);
		XMLUtils.insertLeadingPI(doc, "xpacket", "begin='' id='W5M0MpCehiHzreSzNTczkc9d'");
		XMLUtils.insertTrailingPI(doc, "xpacket", "end='w'");
		byte[] xmpBytes = XMLUtils.serializeToByteArray(doc);
		insertXMP(xmpBytes, rin, rout);
	}
	
	public static void printIFDs(Collection<IFD> list, String indent) {
		int id = 0;
		LOGGER.info("Printing IFDs ... ");
		
		for(IFD currIFD : list) {
			LOGGER.info("IFD #{}", id);
			printIFD(currIFD, TiffTag.class, indent);
			id++;
		}
	}
	
	public static void printIFD(IFD currIFD, Class<? extends Tag> tagClass, String indent) {
		StringBuilder ifd = new StringBuilder();
		print(currIFD, tagClass, indent, ifd);
		LOGGER.info("\n{}", ifd);
	}
	
	private static void print(IFD currIFD, Class<? extends Tag> tagClass, String indent, StringBuilder ifds) {
		// Use reflection to invoke fromShort(short) method
		Method method = null;
		try {
			method = tagClass.getDeclaredMethod("fromShort", short.class);
		} catch (NoSuchMethodException e) {
			e.printStackTrace();
		} catch (SecurityException e) {
			e.printStackTrace();
		}
		Collection<TiffField<?>> fields = currIFD.getFields();
		int i = 0;
		
		for(TiffField<?> field : fields) {
			ifds.append(indent);
			ifds.append("Field #" + i + "\n");
			ifds.append(indent);
			short tag = field.getTag();
			Tag ftag = TiffTag.UNKNOWN;
			if(tag == ExifTag.PADDING.getValue()) {
				ftag = ExifTag.PADDING;
			} else {
				try {
					ftag = (Tag)method.invoke(null, tag);
				} catch (IllegalAccessException e) {
					LOGGER.error("IllegalAcessException", e);
				} catch (IllegalArgumentException e) {
					LOGGER.error("IllegalArgumentException", e);
				} catch (InvocationTargetException e) {
					LOGGER.error("InvocationTargetException", e);
				}
			}
			if (ftag == TiffTag.UNKNOWN) {
				LOGGER.warn("Tag: {} {}{}{} {}", ftag, "[Value: 0x", Integer.toHexString(tag&0xffff), "]", "(Unknown)");
			} else {
				ifds.append("Tag: " + ftag + "\n");
			}
			FieldType ftype = field.getType();				
			ifds.append(indent);
			ifds.append("Field type: " + ftype + "\n");
			int field_length = field.getLength();
			ifds.append(indent);
			ifds.append("Field length: " + field_length + "\n");
			ifds.append(indent);			
			
			String suffix = null;
			if(ftype == FieldType.SHORT || ftype == FieldType.SSHORT)
				suffix = ftag.getFieldAsString(field.getDataAsLong());
			else
				suffix = ftag.getFieldAsString(field.getData());			
			
			ifds.append("Field value: " + field.getDataAsString() + (StringUtils.isNullOrEmpty(suffix)?"":" => " + suffix) + "\n");
			
			i++;
		}
		
		Map<Tag, IFD> children = currIFD.getChildren();
		
		if(children.get(TiffTag.EXIF_SUB_IFD) != null) {
			ifds.append(indent + "--------- ");
			ifds.append("<<Exif SubIFD starts>>\n");
			print(children.get(TiffTag.EXIF_SUB_IFD), ExifTag.class, indent + "--------- ", ifds);
			ifds.append(indent + "--------- ");
			ifds.append("<<Exif SubIFD ends>>\n");
		}
		
		if(children.get(TiffTag.GPS_SUB_IFD) != null) {
			ifds.append(indent + "--------- ");
			ifds.append("<<GPS SubIFD starts>>\n");
			print(children.get(TiffTag.GPS_SUB_IFD), GPSTag.class, indent + "--------- ", ifds);
			ifds.append(indent + "--------- ");
			ifds.append("<<GPS SubIFD ends>>\n");
		}		
	}
	
	private static int readHeader(RandomAccessInputStream rin) throws IOException {
		int offset = 0;
	    // First 2 bytes determine the byte order of the file
		rin.seek(STREAM_HEAD);
	    short endian = rin.readShort();
	    offset += 2;
	
		if (endian == IOUtils.BIG_ENDIAN) {
		    rin.setReadStrategy(ReadStrategyMM.getInstance());
		} else if(endian == IOUtils.LITTLE_ENDIAN) {
		    rin.setReadStrategy(ReadStrategyII.getInstance());
		} else {		
			rin.close();
			throw new RuntimeException("Invalid TIFF byte order");
	    }
		
		// Read TIFF identifier
		rin.seek(offset);
		short tiff_id = rin.readShort();
		offset +=2;
		
		if(tiff_id!=0x2a) { //"*" 42 decimal
			rin.close();
			throw new RuntimeException("Invalid TIFF identifier");
		}
		
		rin.seek(offset);
		offset = rin.readInt();
			
		return offset;
	}
	
	private static int readIFD(IFD parent, Tag parentTag, Class<? extends Tag> tagClass, RandomAccessInputStream rin, List<IFD> list, int offset) throws IOException {	
		// Use reflection to invoke fromShort(short) method
		Method method = null;
		try {
			method = tagClass.getDeclaredMethod("fromShort", short.class);
		} catch (NoSuchMethodException e) {
			e.printStackTrace();
		} catch (SecurityException e) {
			e.printStackTrace();
		}
		IFD tiffIFD = new IFD();
		rin.seek(offset);
		int no_of_fields = rin.readShort();
		offset += 2;
		
		for (int i = 0; i < no_of_fields; i++) {
			rin.seek(offset);
			short tag = rin.readShort();
			Tag ftag = TiffTag.UNKNOWN;
			try {
				ftag = (Tag)method.invoke(null, tag);
			} catch (IllegalAccessException e) {
				e.printStackTrace();
			} catch (IllegalArgumentException e) {
				e.printStackTrace();
			} catch (InvocationTargetException e) {
				e.printStackTrace();
			}
			offset += 2;
			rin.seek(offset);
			short type = rin.readShort();
			FieldType ftype = FieldType.fromShort(type);
			offset += 2;
			rin.seek(offset);
			int field_length = rin.readInt();
			offset += 4;
			////// Try to read actual data.
			switch (ftype)
			{
				case BYTE:
				case UNDEFINED:
					byte[] data = new byte[field_length];
					rin.seek(offset);
					if(field_length <= 4) {						
						rin.readFully(data, 0, field_length);					   
					} else {
						rin.seek(rin.readInt());
						rin.readFully(data, 0, field_length);
					}					
					TiffField<byte[]> byteField = null;
					if(ftype == FieldType.BYTE)
						byteField = new ByteField(tag, data);
					else
						byteField = new UndefinedField(tag, data);
					tiffIFD.addField(byteField);
					offset += 4;					
					break;
				case ASCII:
					data = new byte[field_length];
					if(field_length <= 4) {
						rin.seek(offset);
						rin.readFully(data, 0, field_length);
					}						
					else {
						rin.seek(offset);
						rin.seek(rin.readInt());
						rin.readFully(data, 0, field_length);
					}
					TiffField<String> ascIIField = new ASCIIField(tag, new String(data, "UTF-8"));
					tiffIFD.addField(ascIIField);
					offset += 4;	
					break;
				case SHORT:
					short[] sdata = new short[field_length];
					if(field_length == 1) {
					  rin.seek(offset);
					  sdata[0] = rin.readShort();
					  offset += 4;
					} else if (field_length == 2) {
						rin.seek(offset);
						sdata[0] = rin.readShort();
						offset += 2;
						rin.seek(offset);
						sdata[1] = rin.readShort();
						offset += 2;
					} else {
						rin.seek(offset);
						int toOffset = rin.readInt();
						offset += 4;
						for (int j = 0; j  <field_length; j++){
							rin.seek(toOffset);
							sdata[j] = rin.readShort();
							toOffset += 2;
						}
					}
					TiffField<short[]> shortField = new ShortField(tag, sdata);
					tiffIFD.addField(shortField);
					break;
				case LONG:
					int[] ldata = new int[field_length];
					if(field_length == 1) {
					  rin.seek(offset);
					  ldata[0] = rin.readInt();
					  offset += 4;
					} else {
						rin.seek(offset);
						int toOffset = rin.readInt();
						offset += 4;
						for (int j=0;j<field_length; j++){
							rin.seek(toOffset);
							ldata[j] = rin.readInt();
							toOffset += 4;
						}
					}
					TiffField<int[]> longField = new LongField(tag, ldata);
					tiffIFD.addField(longField);
					
					if ((ftag == TiffTag.EXIF_SUB_IFD) && (ldata[0]!= 0)) {
						try { // If something bad happens, we skip the sub IFD
							readIFD(tiffIFD, TiffTag.EXIF_SUB_IFD, ExifTag.class, rin, null, ldata[0]);
						} catch(Exception e) {
							tiffIFD.removeField(TiffTag.EXIF_SUB_IFD);
							e.printStackTrace();
						}
					} else if ((ftag == TiffTag.GPS_SUB_IFD) && (ldata[0] != 0)) {
						try {
							readIFD(tiffIFD, TiffTag.GPS_SUB_IFD, GPSTag.class, rin, null, ldata[0]);
						} catch(Exception e) {
							tiffIFD.removeField(TiffTag.GPS_SUB_IFD);
							e.printStackTrace();
						}
					} else if((ftag == ExifTag.EXIF_INTEROPERABILITY_OFFSET) && (ldata[0] != 0)) {
						try {
							readIFD(tiffIFD, ExifTag.EXIF_INTEROPERABILITY_OFFSET, InteropTag.class, rin, null, ldata[0]);
						} catch(Exception e) {
							tiffIFD.removeField(ExifTag.EXIF_INTEROPERABILITY_OFFSET);
							e.printStackTrace();
						}
					} else if (ftag == TiffTag.SUB_IFDS) {						
						for(int ifd = 0; ifd < ldata.length; ifd++) {
							try {
								readIFD(tiffIFD, TiffTag.SUB_IFDS, TiffTag.class, rin, null, ldata[0]);
							} catch(Exception e) {
								tiffIFD.removeField(TiffTag.SUB_IFDS);
								e.printStackTrace();
							}
						}
					}				
					break;
				case FLOAT:
					float[] fdata = new float[field_length];
					if(field_length == 1) {
					  rin.seek(offset);
					  fdata[0] = rin.readFloat();
					  offset += 4;
					} else {
						rin.seek(offset);
						int toOffset = rin.readInt();
						offset += 4;
						for (int j=0;j<field_length; j++){
							rin.seek(toOffset);
							fdata[j] = rin.readFloat();
							toOffset += 4;
						}
					}
					TiffField<float[]> floatField = new FloatField(tag, fdata);
					tiffIFD.addField(floatField);
					
					break;
				case DOUBLE:
					double[] ddata = new double[field_length];
					rin.seek(offset);
					int toOffset = rin.readInt();
					offset += 4;
					for (int j=0;j<field_length; j++){
						rin.seek(toOffset);
						ddata[j] = rin.readDouble();
						toOffset += 8;
					}
					TiffField<double[]> doubleField = new DoubleField(tag, ddata);
					tiffIFD.addField(doubleField);
					
					break;
				case RATIONAL:
				case SRATIONAL:
					int len = 2*field_length;
					ldata = new int[len];	
					rin.seek(offset);
					toOffset = rin.readInt();
					offset += 4;					
					for (int j=0;j<len; j+=2){
						rin.seek(toOffset);
						ldata[j] = rin.readInt();
						toOffset += 4;
						rin.seek(toOffset);
						ldata[j+1] = rin.readInt();
						toOffset += 4;
					}
					TiffField<int[]> rationalField = null;
					if(ftype == FieldType.SRATIONAL) {
						rationalField = new SRationalField(tag, ldata);
					} else {
						rationalField = new RationalField(tag, ldata);
					}
					tiffIFD.addField(rationalField);
					
					break;
				case IFD:
					ldata = new int[field_length];
					if(field_length == 1) {
					  rin.seek(offset);
					  ldata[0] = rin.readInt();
					  offset += 4;
					} else {
						rin.seek(offset);
						toOffset = rin.readInt();
						offset += 4;
						for (int j=0;j<field_length; j++){
							rin.seek(toOffset);
							ldata[j] = rin.readInt();
							toOffset += 4;
						}
					}
					TiffField<int[]> ifdField = new IFDField(tag, ldata);
					tiffIFD.addField(ifdField);
					for(int ifd = 0; ifd < ldata.length; ifd++) {
						readIFD(tiffIFD, TiffTag.SUB_IFDS, TiffTag.class, rin, null, ldata[0]);
					}
								
					break;
				default:
					offset += 4;
					break;					
			}
		}
		// If this is a child IFD, add it to its parent
		if(parent != null)
			parent.addChild(parentTag, tiffIFD);
		else // Otherwise, add to the main IFD list
			list.add(tiffIFD);
		rin.seek(offset);
		
		return rin.readInt();
	}
	
	private static void readIFDs(IFD parent, Tag parentTag, Class<? extends Tag> tagClass, List<IFD> list, int offset, RandomAccessInputStream rin) throws IOException {
		// Read the IFDs into a list first	
		while (offset != 0)	{
			offset = readIFD(parent, parentTag, tagClass, rin, list, offset);
		}
	}
	
	public static void readIFDs(List<IFD> list, RandomAccessInputStream rin) throws IOException {
		int offset = readHeader(rin);
		readIFDs(null, null, TiffTag.class, list, offset, rin);
	}
	
	public static Map<MetadataType, Metadata> readMetadata(RandomAccessInputStream rin) throws IOException {
		return readMetadata(rin, 0);
	}
	
	public static Map<MetadataType, Metadata> readMetadata(RandomAccessInputStream rin, int pageNumber) throws IOException	{
		Map<MetadataType, Metadata> metadataMap = new HashMap<MetadataType, Metadata>();

		int offset = readHeader(rin);
		List<IFD> ifds = new ArrayList<IFD>();
		readIFDs(null, null, TiffTag.class, ifds, offset, rin);
		
		if(pageNumber < 0 || pageNumber >= ifds.size())
			throw new IllegalArgumentException("pageNumber " + pageNumber + " out of bounds: 0 - " + (ifds.size() - 1));
		
		IFD currIFD = ifds.get(pageNumber);
		TiffField<?> field = currIFD.getField(TiffTag.ICC_PROFILE); 
		if(field != null) { // We have found ICC_Profile
			metadataMap.put(MetadataType.ICC_PROFILE, new ICCProfile((byte[])field.getData()));
		}
		field = currIFD.getField(TiffTag.XMP);
		if(field != null) { // We have found XMP
			metadataMap.put(MetadataType.XMP, new TiffXMP((byte[])field.getData()));
		}
		field = currIFD.getField(TiffTag.PHOTOSHOP);
		if(field != null) { // We have found Photoshop IRB
			IRB irb = new IRB((byte[])field.getData());
			metadataMap.put(MetadataType.PHOTOSHOP_IRB, irb);
			_8BIM photoshop_8bim = irb.get8BIM(ImageResourceID.IPTC_NAA.getValue());
			if(photoshop_8bim != null) { // If we have IPTC data inside Photoshop, keep it
				IPTC iptc = new IPTC(photoshop_8bim.getData());
				metadataMap.put(MetadataType.IPTC, iptc);
			}
		}
		field = currIFD.getField(TiffTag.IPTC);
		if(field != null) { // We have found IPTC data
			IPTC iptc = (IPTC)(metadataMap.get(MetadataType.IPTC));
			byte[] iptcData = null;
			FieldType type = field.getType();
			if(type == FieldType.LONG)
				iptcData = ArrayUtils.toByteArray(field.getDataAsLong(), rin.getEndian() == IOUtils.BIG_ENDIAN);
			else
				iptcData = (byte[])field.getData();
			if(iptc != null) // If we have IPTC data from IRB, consolidate it with the current data
				iptcData = ArrayUtils.concat(iptcData, iptc.getData());
			metadataMap.put(MetadataType.IPTC, new IPTC(iptcData));
		}		
		field = currIFD.getField(TiffTag.EXIF_SUB_IFD);
		if(field != null) { // We have found EXIF SubIFD
			metadataMap.put(MetadataType.EXIF, new TiffExif(currIFD));
		}
		field = currIFD.getField(TiffTag.IMAGE_SOURCE_DATA);
		if(field != null) {
			boolean bigEndian = (rin.getEndian() == IOUtils.BIG_ENDIAN);
			ReadStrategy readStrategy = bigEndian?ReadStrategyMM.getInstance():ReadStrategyII.getInstance();
			metadataMap.put(MetadataType.PHOTOSHOP_DDB, new DDB((byte[])field.getData(), readStrategy));
		}
		field = currIFD.getField(TiffTag.IMAGE_DESCRIPTION);
		if(field != null) { // We have Comment
			Comments comments = new pixy.meta.image.Comments();
			comments.addComment(field.getDataAsString());
			metadataMap.put(MetadataType.COMMENT, comments);
		}
		
		return metadataMap;
	}
	
	/**
	 * Remove meta data from TIFF image
	 * 
	 * @param rin RandomAccessInputStream for the input image
	 * @param rout RandomAccessOutputStream for the output image
	 * @param pageNumber working page from which to remove metadata
	 * @param metadataTypes a variable length array of MetadataType to be removed
	 * @throws IOException
	 * @return A map of the removed metadata
	 */
	public static Map<MetadataType, Metadata> removeMetadata(int pageNumber, RandomAccessInputStream rin, RandomAccessOutputStream rout, MetadataType ... metadataTypes) throws IOException {
		return removeMetadata(new HashSet<MetadataType>(Arrays.asList(metadataTypes)), pageNumber, rin, rout);
	}
	
	/**
	 * Remove meta data from TIFF image
	 * 
	 * @param rin RandomAccessInputStream for the input image
	 * @param rout RandomAccessOutputStream for the output image
	 * @param metadataTypes a variable length array of MetadataType to be removed
	 * @throws IOException
	 * @return A map of the removed metadata
	 */
	public static Map<MetadataType, Metadata> removeMetadata(RandomAccessInputStream rin, RandomAccessOutputStream rout, MetadataType ... metadataTypes) throws IOException {
		return removeMetadata(0, rin, rout, metadataTypes);
	}
	
	/**
	 * Remove meta data from TIFF image
	 * 
	 * @param pageNumber working page from which to remove EXIF and GPS data
	 * @param rin RandomAccessInputStream for the input image
	 * @param rout RandomAccessOutputStream for the output image
	 * @throws IOException
	 * @return A map of the removed metadata
	 */
	public static Map<MetadataType, Metadata> removeMetadata(Set<MetadataType> metadataTypes, int pageNumber, RandomAccessInputStream rin, RandomAccessOutputStream rout) throws IOException {
		int offset = copyHeader(rin, rout);
		// Read the IFDs into a list first
		List<IFD> ifds = new ArrayList<IFD>();
		readIFDs(null, null, TiffTag.class, ifds, offset, rin);
		
		// Create a map to hold all the metadata and thumbnails
		Map<MetadataType, Metadata> metadataMap = new HashMap<MetadataType, Metadata>();
	
		if(pageNumber < 0 || pageNumber >= ifds.size())
			throw new IllegalArgumentException("pageNumber " + pageNumber + " out of bounds: 0 - " + (ifds.size() - 1));
		
		IFD workingPage = ifds.get(pageNumber);
		
		TiffField<?> metadata = null;
		
		for(MetadataType metaType : metadataTypes) {
			switch(metaType) {
				case XMP:
					TiffField<?> xmpField = workingPage.removeField(TiffTag.XMP);
					if(xmpField != null) metadataMap.put(MetadataType.XMP, new TiffXMP((byte[])xmpField.getData()));
					metadata = workingPage.removeField(TiffTag.PHOTOSHOP);
					if(metadata != null) {
						byte[] data = (byte[])metadata.getData();
						// We only remove XMP and keep the other IRB data untouched.
						List<_8BIM> bims = removeMetadataFromIRB(workingPage, data, ImageResourceID.XMP_METADATA);
						if(bims.size() > 0 && xmpField == null) metadataMap.put(MetadataType.XMP, new TiffXMP(bims.get(0).getData()));
					}
					break;
				case IPTC:
					TiffField<?> iptcField = workingPage.removeField(TiffTag.IPTC);
					if(iptcField != null) metadataMap.put(MetadataType.IPTC, new IPTC((byte[])iptcField.getData()));
					metadata = workingPage.removeField(TiffTag.PHOTOSHOP);
					if(metadata != null) {
						byte[] data = (byte[])metadata.getData();
						// We only remove IPTC_NAA and keep the other IRB data untouched.
						List<_8BIM> bims = removeMetadataFromIRB(workingPage, data, ImageResourceID.IPTC_NAA);
						if(bims.size() > 0 && iptcField == null) metadataMap.put(MetadataType.IPTC, new IPTC(bims.get(0).getData()));
					}
					break;
				case ICC_PROFILE:
					TiffField<?>  iccField = workingPage.removeField(TiffTag.ICC_PROFILE);
					if(iccField != null) metadataMap.put(MetadataType.ICC_PROFILE, new ICCProfile((byte[])iccField.getData()));
					metadata = workingPage.removeField(TiffTag.PHOTOSHOP);
					if(metadata != null) {
						byte[] data = (byte[])metadata.getData();
						// We only remove ICC_PROFILE and keep the other IRB data untouched.
						List<_8BIM> bims = removeMetadataFromIRB(workingPage, data, ImageResourceID.ICC_PROFILE);
						if(bims.size() > 0 && iccField == null) metadataMap.put(MetadataType.ICC_PROFILE, new ICCProfile(bims.get(0).getData()));
					}
					break;
				case PHOTOSHOP_IRB:
					TiffField<?> irbField = workingPage.removeField(TiffTag.PHOTOSHOP);
					if(irbField != null) metadataMap.put(MetadataType.PHOTOSHOP_IRB, new IRB((byte[])irbField.getData()));
					break;
				case EXIF:
					TiffField<?> exifField = workingPage.removeField(TiffTag.EXIF_SUB_IFD);
					if(exifField != null) metadataMap.put(MetadataType.EXIF, new TiffExif(workingPage));
					workingPage.removeField(TiffTag.GPS_SUB_IFD);
					metadata = workingPage.removeField(TiffTag.PHOTOSHOP);
					if(metadata != null) {
						byte[] data = (byte[])metadata.getData();
						// We only remove EXIF and keep the other IRB data untouched.
						removeMetadataFromIRB(workingPage, data, ImageResourceID.EXIF_DATA1, ImageResourceID.EXIF_DATA3);
					}
					break;
				case COMMENT:
					TiffField<?> commentField = workingPage.removeField(TiffTag.IMAGE_DESCRIPTION);				
					if(commentField != null) {
						Comments comments = new Comments();
						comments.addComment(commentField.getDataAsString());
						metadataMap.put(MetadataType.COMMENT, comments);
					}
					break;
				default:
			}
		}
		
		offset = copyPages(ifds, offset, rin, rout);
		int firstIFDOffset = ifds.get(0).getStartOffset();	

		writeToStream(rout, firstIFDOffset);
		
		return metadataMap;
	}
	
	/**
	 * Remove meta data from TIFF image
	 * 
	 * @param metadataTypes a set of MetadataType to be removed
	 * @param rin RandomAccessInputStream for the input image
	 * @param rout RandomAccessOutputStream for the output image	 
	 * @throws IOException
	 * @return A map of the removed metadata
	 */
	public static Map<MetadataType, Metadata> removeMetadata(Set<MetadataType> metadataTypes, RandomAccessInputStream rin, RandomAccessOutputStream rout) throws IOException {
		return removeMetadata(metadataTypes, 0, rin, rout);
	}
	
	private static List<_8BIM> removeMetadataFromIRB(IFD workingPage, byte[] data, ImageResourceID ... ids) throws IOException {
		IRB irb = new IRB(data);
		// Shallow copy the map.
		Map<Short, _8BIM> bimMap = new HashMap<Short, _8BIM>(irb.get8BIM());
		List<_8BIM> bimList = new ArrayList<_8BIM>();
		// We only remove XMP and keep the other IRB data untouched.
		for(ImageResourceID id : ids) {
			_8BIM bim = bimMap.remove(id.getValue());
			if(bim != null) bimList.add(bim);
		}
		if(bimMap.size() > 0) {
		   	// Write back the IRB
			ByteArrayOutputStream bout = new ByteArrayOutputStream();
			for(_8BIM bim : bimMap.values())
				bim.write(bout);
			// Add new PHOTOSHOP field
			workingPage.addField(new ByteField(TiffTag.PHOTOSHOP.getValue(), bout.toByteArray()));
		}
		
		return bimList;
	}
	
	public static int retainPages(int startPage, int endPage, RandomAccessInputStream rin, RandomAccessOutputStream rout) throws IOException {
		if(startPage < 0 || endPage < 0)
			throw new IllegalArgumentException("Negative start or end page");
		else if(startPage > endPage)
			throw new IllegalArgumentException("Start page is larger than end page");
		
		List<IFD> list = new ArrayList<IFD>();
	  
		int offset = copyHeader(rin, rout);
		
		// Step 1: read the IFDs into a list first
		readIFDs(null, null, TiffTag.class, list, offset, rin);		
		// Step 2: remove pages from a multiple page TIFF
		int pagesRetained = list.size();
		List<IFD> newList = new ArrayList<IFD>();
		if(startPage <= list.size() - 1)  {
			if(endPage > list.size() - 1) endPage = list.size() - 1;
			for(int i = endPage; i >= startPage; i--) {
				newList.add(list.get(i)); 
			}
		}
		if(newList.size() > 0) {
			pagesRetained = newList.size();
			list.retainAll(newList);
		}
		// Reset pageNumber for the existing pages
		for(int i = 0; i < list.size(); i++) {
			list.get(i).removeField(TiffTag.PAGE_NUMBER);
			list.get(i).addField(new ShortField(TiffTag.PAGE_NUMBER.getValue(), new short[]{(short)i, (short)(list.size() - 1)}));
		}
		// End of removing pages		
		// Step 3: copy the remaining pages
		// 0x08 is the first write offset
		int writeOffset = FIRST_WRITE_OFFSET;
		offset = copyPages(list, writeOffset, rin, rout);
		int firstIFDOffset = list.get(0).getStartOffset();
		
		writeToStream(rout, firstIFDOffset);
		
		return pagesRetained;
	}
	
	// Return number of pages retained
	public static int retainPages(RandomAccessInputStream rin, RandomAccessOutputStream rout, int... pages) throws IOException {
		List<IFD> list = new ArrayList<IFD>();
	  
		int offset = copyHeader(rin, rout);
		// Step 1: read the IFDs into a list first
		readIFDs(null, null, TiffTag.class, list, offset, rin);		
		// Step 2: remove pages from a multiple page TIFF
		int pagesRetained = list.size();
		List<IFD> newList = new ArrayList<IFD>();
		Arrays.sort(pages);
		for(int i = pages.length - 1; i >= 0; i--) {
			if(pages[i] >= 0 && pages[i] < list.size())
				newList.add(list.get(pages[i])); 
		}
		if(newList.size() > 0) {
			pagesRetained = newList.size();
			list.retainAll(newList);
		}
		// End of removing pages
		// Reset pageNumber for the existing pages
		for(int i = 0; i < list.size(); i++) {
			list.get(i).removeField(TiffTag.PAGE_NUMBER);
			list.get(i).addField(new ShortField(TiffTag.PAGE_NUMBER.getValue(), new short[]{(short)i, (short)(list.size() - 1)}));
		}
		// Step 3: copy the remaining pages
		// 0x08 is the first write offset
		int writeOffset = FIRST_WRITE_OFFSET;
		offset = copyPages(list, writeOffset, rin, rout);
		int firstIFDOffset = list.get(0).getStartOffset();
		
		writeToStream(rout, firstIFDOffset);
		
		return pagesRetained;
	}
	
	public static void write(TIFFImage tiffImage, RandomAccessOutputStream rout) throws IOException {
		RandomAccessInputStream rin = tiffImage.getInputStream();
		int offset = writeHeader(rout);
		offset = copyPages(tiffImage.getIFDs(), offset, rin, rout);
		int firstIFDOffset = tiffImage.getIFDs().get(0).getStartOffset();	
	 
		writeToStream(rout, firstIFDOffset);
	}
	
	// Return stream offset where to write actual image data or IFD	
	private static int writeHeader(RandomAccessOutputStream rout) throws IOException {
		// Write byte order
		short endian = rout.getEndian();
		rout.writeShort(endian);
		// Write TIFF identifier
		rout.writeShort(0x2a);
		
		return FIRST_WRITE_OFFSET;
	}
		
	private static void writeToStream(RandomAccessOutputStream rout, int firstIFDOffset) throws IOException {
		// Go to the place where we should write the first IFD offset
		// and write the first IFD offset
		rout.seek(OFFSET_TO_WRITE_FIRST_IFD_OFFSET);
		rout.writeInt(firstIFDOffset);
		// Dump the data to the real output stream
		rout.seek(STREAM_HEAD);
		rout.writeToStream(rout.getLength());
		//rout.flush();
	}
}
