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

/**
 * Encapsulates a JPEG sample component
 *  
 * @author Wen Yu, yuwen_66@yahoo.com
 * @version 1.0 10/09/2013
 */
public class Component {
	//
	private byte id;
	private byte hSampleFactor;
	private byte vSampleFactor;
	private byte qTableNumber;
	
	private byte acTableNumber;
	private byte dcTableNumber;
	
	Component(byte id, byte hSampleFactor, byte vSampleFactor, byte qTableNumber) {
		this.id = id;
		this.hSampleFactor = hSampleFactor;
		this.vSampleFactor = vSampleFactor;
		this.qTableNumber = qTableNumber;
	}
	
	public byte getACTableNumber() {
		return acTableNumber;
	}
	
	public byte getDCTableNumber() {
		return dcTableNumber;
	}
	
	public byte getId() {
		return id;		
	}
	
	public byte getHSampleFactor() {
		return hSampleFactor;
	}
	
	public byte getVSampleFactor() {
		return vSampleFactor;
	}
	
	public byte getQTableNumber() {
		return qTableNumber;
	}
	
	public void setACTableNumber(byte acTableNumber) {
		this.acTableNumber = acTableNumber;
	}
	
	public void setDCTableNumber(byte dcTableNumber) {
		this.dcTableNumber = dcTableNumber;
	}
}
