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

package pixy.meta.iptc;

import java.util.Comparator;

public class IPTCTagComparator implements Comparator<IPTCTag> {

	public int compare(IPTCTag o1, IPTCTag o2) {	
		final int BEFORE = -1;
	    final int EQUAL = 0;
	    final int AFTER = 1;
	    
	    if(o1 == o2) return EQUAL;
	    
	    if (o1.getRecordNumber() < o2.getRecordNumber()) return BEFORE;
	    if (o1.getRecordNumber() > o2.getRecordNumber()) return AFTER;
	    if(o1.getRecordNumber() == o2.getRecordNumber()) {
	    	if (o1.getTag() < o2.getTag()) return BEFORE;
		    if (o1.getTag() > o2.getTag()) return AFTER;
		    return EQUAL;
	    }
	
		return EQUAL;
	}
}
