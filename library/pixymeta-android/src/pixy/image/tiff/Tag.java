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

/**
 * Common interface for all TIFF related tag enumerations
 *
 * @author Wen Yu, yuwen_66@yahoo.com
 * @version 1.0 03/28/2014
 */
public interface Tag {
	public String getFieldAsString(Object value);
	public FieldType getFieldType();
	public String getName();
	public short getValue();
	public boolean isCritical();
}
