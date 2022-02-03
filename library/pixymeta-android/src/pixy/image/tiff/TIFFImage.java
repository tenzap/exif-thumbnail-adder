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

package pixy.image.tiff;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import pixy.io.RandomAccessInputStream;
import pixy.io.RandomAccessOutputStream;
import pixy.meta.tiff.TIFFMeta;

/**
 * TIFF image wrapper to manipulate pages and fields
 * 
 * @author Wen Yu, yuwen_66@yahoo.com
 * @version 1.0 05/23/2014
 */
public class TIFFImage implements Iterable<IFD> {
	// Define fields
	private int numOfPages;
	private int workingPage;
	private List<IFD> ifds;
	private RandomAccessInputStream rin;

	public TIFFImage(RandomAccessInputStream rin) throws IOException {
		ifds = new ArrayList<IFD>();
		this.rin = rin;
		TIFFMeta.readIFDs(ifds, rin);
		this.numOfPages = ifds.size();
		this.workingPage = 0;
	}
	
	public void addField(TiffField<?> field) {
		ifds.get(workingPage).addField(field);
	}
	
	public TiffField<?> getField(Tag tag) {
		return ifds.get(workingPage).getField(tag);
	}
	
	public List<IFD> getIFDs() {
		return Collections.unmodifiableList(ifds);
	}
	
	public RandomAccessInputStream getInputStream() {
		return rin;
	}
	
	public int getNumOfPages() {
		return numOfPages;
	}
	
	public TiffField<?> removeField(Tag tag) {
		return ifds.get(workingPage).removeField(tag);
	}
	
	public IFD removePage(int index) {
		IFD removed = ifds.remove(index);
		numOfPages--;
		
		return removed;
	}
	
	public void setWorkingPage(int workingPage) {
		if(workingPage >= 0 && workingPage < numOfPages)
			this.workingPage = workingPage;
		else
			throw new IllegalArgumentException("Invalid page number: " + workingPage);
	}
	
	public void write(RandomAccessOutputStream out) throws IOException {
		// Reset pageNumber if we have more than 1 pages
		if(numOfPages > 1) { 
			for(int i = 0; i < ifds.size(); i++) {
				ifds.get(i).removeField(TiffTag.PAGE_NUMBER);
				ifds.get(i).addField(new ShortField(TiffTag.PAGE_NUMBER.getValue(), new short[]{(short)i, (short)(numOfPages - 1)}));
			}
		}
		TIFFMeta.write(this, out);
	}

	public Iterator<IFD> iterator() {
		return ifds.iterator();
	}
}
