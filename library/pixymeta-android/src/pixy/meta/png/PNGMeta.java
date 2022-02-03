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
 * PNGMeta.java
 *
 * Who   Date       Description
 * ====  =========  =================================================
 * WY    30Mar2016  Added insertTextChunk()
 * WY    30Mar2016  Changed XMP trailing pi to "end='r'"
 * WY    06Jul2015  Added insertXMP(InputSream, OutputStream, XMP)
 * WY    30Mar2015  Added insertICCProfile()
 * WY    13Mar2015  Initial creation
 */

package pixy.meta.png;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.ArrayList;
import java.util.ListIterator;
import java.util.Map;
import java.util.Set;
import java.util.zip.InflaterInputStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;

import pixy.image.png.ICCPBuilder;
import pixy.meta.Metadata;
import pixy.meta.MetadataType;
import pixy.meta.icc.ICCProfile;
import pixy.meta.xmp.XMP;
import pixy.image.png.Chunk;
import pixy.image.png.ChunkType;
import pixy.image.png.TextBuilder;
import pixy.image.png.TextReader;
import pixy.image.png.UnknownChunk;
import pixy.io.IOUtils;
import pixy.string.XMLUtils;
/**
 * PNG image tweaking tool
 *
 * @author Wen Yu, yuwen_66@yahoo.com
 * @version 1.0 10/18/2012
 */
public class PNGMeta {
	
	/** PNG signature constant */
    private static final long SIGNATURE = 0x89504E470D0A1A0AL;
    
    // Obtain a logger instance
 	private static final Logger LOGGER = LoggerFactory.getLogger(PNGMeta.class);
	
   	public static void insertChunk(Chunk customChunk, InputStream is, OutputStream os) throws IOException {
  		insertChunks(is, os, customChunk);
  	}
  	
  	public static void insertChunks(InputStream is, OutputStream os, Chunk... chunks) throws IOException {
  		List<Chunk> list = readChunks(is);  		
        Collections.addAll(list, chunks);
    	
  		IOUtils.writeLongMM(os, SIGNATURE);
	
        serializeChunks(list, os);
  	}
  	
  	public static void insertChunks(List<Chunk> chunks, InputStream is, OutputStream os) throws IOException {
  		List<Chunk> list = readChunks(is);  		
        list.addAll(chunks);
    	
  		IOUtils.writeLongMM(os, SIGNATURE);
	
        serializeChunks(list, os);
  	}
  	
  	public static void insertComments(InputStream is, OutputStream os, List<String> comments) throws IOException {
  		// Build tEXt chunk
  		TextBuilder txtBuilder = new TextBuilder(ChunkType.TEXT);
  		int numOfComments = comments.size();
  		Chunk[] chunks = new Chunk[numOfComments];
  		for(int i = 0; i < numOfComments; i++) {
  			chunks[i] = txtBuilder.keyword("Comment").text(comments.get(i)).build();
  		}
  		insertChunks(is, os, chunks);
  	}
  	
  	public static void insertICCProfile(String profile_name, byte[] icc_profile, InputStream is, OutputStream os) throws IOException {
  		ICCPBuilder builder = new ICCPBuilder();
  		builder.name(profile_name);
  		builder.data(icc_profile);
  		insertChunk(builder.build(), is, os);
  	}
  	
  	public static void insertTextChunk(ChunkType type, String keyword, String text, InputStream is, OutputStream os) throws IOException {
  		if(type == null || keyword == null || text == null)
  			throw new IllegalArgumentException("Argument(s) are null");
  		
  		insertChunk(new TextBuilder(type).keyword(keyword).text(text).build(), is, os);
  	}
  	
  	public static void insertTextChunks(TextualChunks textualChunks, InputStream is, OutputStream os) throws IOException {
  		if(textualChunks == null) throw new IllegalArgumentException("Argument is null");
  		insertChunks(textualChunks.getChunks(), is, os);
  	}
  	
	public static void insertXMP(InputStream is, OutputStream os, XMP xmp) throws IOException {
  		insert(is, os, XMLUtils.serializeToString(xmp.getMergedDocument()));
  	}
  	
  	// Add leading and trailing PI
  	public static void insertXMP(InputStream is, OutputStream os, String xmp) throws IOException {
  		Document doc = XMLUtils.createXML(xmp);
		XMLUtils.insertLeadingPI(doc, "xpacket", "begin='' id='W5M0MpCehiHzreSzNTczkc9d'");
		XMLUtils.insertTrailingPI(doc, "xpacket", "end='r'");
		String newXmp = XMLUtils.serializeToString(doc); // DONOT use XMLUtils.serializeToStringLS()
  		insert(is, os, newXmp);
    }
  	
  	private static void insert(InputStream is, OutputStream os, String xmp) throws IOException {
  		// Read all the chunks first
  		List<Chunk> chunks = readChunks(is);
	    ListIterator<Chunk> itr = chunks.listIterator();
	    
	    // Remove old XMP chunk
	    while(itr.hasNext()) {
	    	Chunk chunk = itr.next();
	    	if(chunk.getChunkType() == ChunkType.ITXT) {
	    		TextReader reader = new TextReader(chunk);
				if(reader.getKeyword().equals("XML:com.adobe.xmp")); // We found XMP data
					itr.remove();
	    	}
	    }
	    
	    // Create XMP textual chunk
		Chunk xmpChunk = new TextBuilder(ChunkType.ITXT).keyword("XML:com.adobe.xmp").text(xmp).build();
		// Insert XMP textual chunk into image
	    chunks.add(xmpChunk);
	    
	    IOUtils.writeLongMM(os, SIGNATURE);
	    
        serializeChunks(chunks, os);
    }
  	
   	public static List<Chunk> readChunks(InputStream is) throws IOException {  		
  		List<Chunk> list = new ArrayList<Chunk>();
 		 //Local variables for reading chunks
        int data_len = 0;
        int chunk_type = 0;
        byte[] buf = null;
     
        long signature = IOUtils.readLongMM(is);

        if (signature != SIGNATURE) {
       	 	throw new RuntimeException("Invalid PNG signature");
        }   

        /** Read header */
        /** We are expecting IHDR */
        if ((IOUtils.readIntMM(is)!=13)||(IOUtils.readIntMM(is) != ChunkType.IHDR.getValue())) {
            throw new RuntimeException("Invalid PNG header");
        }     
        
        buf = new byte[13];
        IOUtils.readFully(is, buf, 0, 13);
  
        list.add(new Chunk(ChunkType.IHDR, 13, buf, IOUtils.readUnsignedIntMM(is)));         
      
        while (true) {
        	data_len = IOUtils.readIntMM(is);
	       	chunk_type = IOUtils.readIntMM(is);
	   
	       	if (chunk_type == ChunkType.IEND.getValue()) {
	        	 list.add(new Chunk(ChunkType.IEND, data_len, new byte[0], IOUtils.readUnsignedIntMM(is)));
	       		 break;
	       	} 
       		ChunkType chunkType = ChunkType.fromInt(chunk_type);
       		buf = new byte[data_len];
       		IOUtils.readFully(is, buf,0, data_len);
              
       		if (chunkType == ChunkType.UNKNOWN)
       			list.add(new UnknownChunk(data_len, chunk_type, buf, IOUtils.readUnsignedIntMM(is)));
       		else
       			list.add(new Chunk(chunkType, data_len, buf, IOUtils.readUnsignedIntMM(is)));
        }
        
        return list;
  	}
   	
	private static byte[] readICCProfile(byte[] buf) throws IOException {
		int profileName_len = 0;
		while(buf[profileName_len] != 0) profileName_len++;
		String profileName = new String(buf, 0, profileName_len, "UTF-8");
		
		InflaterInputStream ii = new InflaterInputStream(new ByteArrayInputStream(buf, profileName_len + 2, buf.length - profileName_len - 2));
		LOGGER.info("ICCProfile name: {}", profileName);
		 
		byte[] icc_profile = IOUtils.readFully(ii, 4096);
		LOGGER.info("ICCProfile length: {}", icc_profile.length);
	 		 
		return icc_profile;
 	}
  	
	public static Map<MetadataType, Metadata> readMetadata(InputStream is) throws IOException {
		Map<MetadataType, Metadata> metadataMap = new HashMap<MetadataType, Metadata>();
		List<Chunk> chunks = readChunks(is);
		Iterator<Chunk> iter = chunks.iterator();
		TextualChunks textualChunk = null;
		while (iter.hasNext()) {
			Chunk chunk = iter.next();
			ChunkType type = chunk.getChunkType();
			long length = chunk.getLength();
			if(type == ChunkType.ICCP)
				metadataMap.put(MetadataType.ICC_PROFILE, new ICCProfile(readICCProfile(chunk.getData())));
			else if(type == ChunkType.TEXT || type == ChunkType.ITXT || type == ChunkType.ZTXT) {
				if(textualChunk == null)
					textualChunk = new TextualChunks();
				textualChunk.addChunk(chunk);			
			} else if(type == ChunkType.TIME) {
				metadataMap.put(MetadataType.PNG_TIME, new TIMEChunk(chunk));
			}
			
			LOGGER.info("{} ({}) | {} bytes | 0x{} (CRC)", type.getName(), type.getAttribute(), length, Long.toHexString(chunk.getCRC()));
		}
		
		if(textualChunk != null) {
			metadataMap.put(MetadataType.PNG_TEXTUAL, textualChunk);
			
			// We may find XMP data inside iTXT
			Map<String, String> keyValMap = textualChunk.getKeyValMap();
			
			for (Map.Entry<String, String> entry : keyValMap.entrySet()) {
				if(entry.getKey().equals("XML:com.adobe.xmp"))
					metadataMap.put(MetadataType.XMP, new PngXMP(entry.getValue()));
			}
		}
			
		is.close();
		
		return metadataMap;
	}
  	
	public static List<Chunk> removeChunks(List<Chunk> chunks, ChunkType chunkType) {
  		
  		Iterator<Chunk> iter = chunks.listIterator();
   	
   		while(iter.hasNext()) {
   			
   			Chunk chunk = iter.next();
   		
   			if (chunk.getChunkType() == chunkType) {   				
   				iter.remove();
   			}   			
   		}
   		
   		return chunks;  		
  	}
   	
   	/**
   	 * Removes chunks which have the same ChunkType values from the chunkEnumSet.
   	 * 
   	 * @param chunks a list of chunks to be checked.
   	 * @param chunkEnumSet a set of ChunkType (better use a HashSet instead of EnumSet for performance).
   	 * @return a list of chunks with the specified chunks removed if any.
   	 */
   	
   	public static List<Chunk> removeChunks(List<Chunk> chunks, Set<ChunkType> chunkEnumSet) {
  		
  		Iterator<Chunk> iter = chunks.listIterator();
   	
   		while(iter.hasNext()) {
   			
   			Chunk chunk = iter.next();
   		
   			if (chunkEnumSet.contains(chunk.getChunkType())) {   				
   				iter.remove();
   			}   			
   		}
   		
   		return chunks;  		
  	}
  	
   	public static void serializeChunks(List<Chunk> chunks, OutputStream os) throws IOException {
  		
  		Collections.sort(chunks);
  	    
  		for(Chunk chunk : chunks) {
        	chunk.write(os);
        }
  	}
  	
  	private PNGMeta() {}
}
