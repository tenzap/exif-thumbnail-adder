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

package pixy.meta;

public enum MetadataType {
	EXIF, // EXIF
	IPTC, // IPTC
	ICC_PROFILE, // ICC Profile
	XMP, // Adobe XMP
	PHOTOSHOP_IRB, // PHOTOSHOP Image Resource Block
	PHOTOSHOP_DDB, // PHOTOSHOP Document Data Block
	COMMENT, // General comment
	IMAGE, // Image specific information
	JPG_JFIF, // JPEG APP0 (JFIF)
	JPG_DUCKY, // JPEG APP12 (DUCKY)
	JPG_ADOBE, // JPEG APP14 (ADOBE)
	PNG_TEXTUAL, // PNG textual information
	PNG_TIME; // PNG tIME (last modified time) chunk
}
