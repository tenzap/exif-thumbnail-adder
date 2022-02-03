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
 * TextualChunk.java
 *
 * Who   Date       Description
 * ====  =========  =================================================
 * WY    04Nov2015  Added chunk type check
 * WY    09Jul2015  Rewrote to work with multiple textual chunks
 * WY    05Jul2015  Added write support
 * WY    05Jul2015  Initial creation
 */

package pixy.meta.png;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;

import pixy.meta.Metadata;
import pixy.meta.MetadataEntry;
import pixy.meta.MetadataType;
import pixy.image.png.Chunk;
import pixy.image.png.ChunkType;
import pixy.image.png.TextReader;

public class TextualChunks extends Metadata {
	/* This queue is used to keep track of the unread chunks
	 * After it's being read, all of it's elements will be moved
	 * to chunks list
	 */
	private Queue<Chunk> queue;
	// We keep chunks and keyValMap in sync
	private List<Chunk> chunks;
	private Map<String, String> keyValMap;
	
	public TextualChunks() {
		super(MetadataType.PNG_TEXTUAL);
		this.queue = new LinkedList<Chunk>();
		this.chunks = new ArrayList<Chunk>();
		this.keyValMap = new HashMap<String, String>();		
	}
		
	public TextualChunks(Collection<Chunk> chunks) {
		super(MetadataType.PNG_TEXTUAL);
		validateChunks(chunks);
		this.queue = new LinkedList<Chunk>(chunks);
		this.chunks = new ArrayList<Chunk>();
		this.keyValMap = new HashMap<String, String>();
	}
	
	public List<Chunk> getChunks() {
		ArrayList<Chunk> chunkList = new ArrayList<Chunk>(chunks);
		chunkList.addAll(queue);		
		return chunkList;
	}
	
	public Map<String, String> getKeyValMap() {
		ensureDataRead();
		return Collections.unmodifiableMap(keyValMap);
	}
	
	public void addChunk(Chunk chunk) {
		validateChunkType(chunk.getChunkType());
		queue.offer(chunk);
	}
	
	public Iterator<MetadataEntry> iterator() {
		ensureDataRead();
		List<MetadataEntry> entries = new ArrayList<MetadataEntry>();
			
		for (Map.Entry<String, String> entry : keyValMap.entrySet()) {
		    entries.add(new MetadataEntry(entry.getKey(), entry.getValue()));
		}
		
		return Collections.unmodifiableCollection(entries).iterator();
	}
	
	public void read() throws IOException {
		if(queue.size() > 0) {
			TextReader reader = new TextReader();
			for(Chunk chunk : queue) {
				reader.setInput(chunk);
				String key = reader.getKeyword();
				String text = reader.getText();
				String oldText = keyValMap.get(key);
				keyValMap.put(key, (oldText == null)? text: oldText + "; " + text);
				chunks.add(chunk);
			}
			queue.clear();
		}
	}
	
	private static void validateChunks(Collection<Chunk> chunks) {
		for(Chunk chunk : chunks)
			validateChunkType(chunk.getChunkType());
	}
	
	private static void validateChunkType(ChunkType chunkType) {
		if((chunkType != ChunkType.TEXT) && (chunkType != ChunkType.ITXT) 
				&& (chunkType != ChunkType.ZTXT))
			throw new IllegalArgumentException("Expect Textual chunk!");
	}
}
