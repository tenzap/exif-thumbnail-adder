/*
 * Copyright (C) 2021 Fab Stz <fabstz-it@yahoo.fr>
 *
 * This file is part of Exif Thumbnail Adder. An android app that adds
 * thumbnails in EXIF tags of your pictures that don't have one yet.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.exifthumbnailadder.app;

import android.content.Context;
import android.net.Uri;
import android.util.Log;

import androidx.documentfile.provider.DocumentFile;

import java.io.File;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.TreeSet;

public class ETADocs {

    Context ctx;
    Object etaDocsRoot;
    String excluded;

    public ETADocs(Context ctx, Object etaDocsRoot, String excluded) {
        this.etaDocsRoot = etaDocsRoot;
        this.excluded = excluded;
        this.ctx = ctx;
    }

    public Object getDocsSet() {
        if (etaDocsRoot instanceof Uri) {
            DocumentFile baseDf = DocumentFile.fromTreeUri(ctx, (Uri) etaDocsRoot);
            return docFilesToProcessList(baseDf, 0, excluded); // TreeSet<DocumentFile>
        }
        if (etaDocsRoot instanceof File) {
            return filesToProcessList((File)etaDocsRoot, 0, new File(excluded)); // TreeSet<File>
        }
        return null;
    }

    class DocumentFileComparator implements Comparator<DocumentFile> {
        @Override
        public int compare(DocumentFile e1, DocumentFile e2) {
            return e1.getUri().toString().compareTo(e2.getUri().toString());
        }
    }

    class FileComparator implements Comparator<File> {
        @Override
        public int compare(File e1, File e2) {
            return e1.getPath().compareTo(e2.getPath());
        }
    }

    private TreeSet<DocumentFile> docFilesToProcessList(DocumentFile df, int level, String excluded) {
        TreeSet<DocumentFile> treeSet = new TreeSet<DocumentFile>(new DocumentFileComparator());
        DocumentFile[] firstLevelFiles = df.listFiles();
        if (firstLevelFiles != null && firstLevelFiles.length > 0) {
            for (DocumentFile aFile : firstLevelFiles) {
                for (int i = 0; i < level; i++) {
                    //System.out.print("\t");
                }
                if (aFile.isDirectory()) {
                    if (aFile.getName().equals(excluded)) {
                        if (MainApplication.enableLog) Log.i(MainApplication.TAG, ctx.getString(R.string.frag1_log_skipping_excluded_dir, excluded, aFile.getUri().getPath()));
                    } else {
                        //System.out.println("[" + aFile.getName() + "]");
                        treeSet.addAll(docFilesToProcessList(aFile, level + 1, excluded));
                    }
                } else {
                    //System.out.println(aFile.getName());
                    treeSet.add(aFile);
                }
            }
        }
        return treeSet;
    }

    private void listDirectory(File dir, int level) {
        File[] firstLevelFiles = dir.listFiles();
        if (firstLevelFiles != null && firstLevelFiles.length > 0) {
            for (File aFile : firstLevelFiles) {
                for (int i = 0; i < level; i++) {
                    //System.out.print("\t");
                }
                if (aFile.isDirectory()) {
                    //System.out.println("[" + aFile.getName() + "]");
                    listDirectory(aFile, level + 1);
                } else {
                    //System.out.println(aFile.getName());
                }
            }
        }
    }

    private TreeSet<File> filesToProcessList(File dir, int level, File excluded) {
        TreeSet<File> treeSet = new TreeSet<File>(new FileComparator());
        File[] firstLevelFiles = dir.listFiles();
        if (firstLevelFiles != null && firstLevelFiles.length > 0) {
            for (File aFile : firstLevelFiles) {
                if (!aFile.getPath().startsWith(excluded.getPath())) {
                    for (int i = 0; i < level; i++) {
                        //System.out.print("\t");
                    }
                    if (aFile.isDirectory()) {
                        //System.out.println("[" + aFile.getName() + "]");
                        treeSet.addAll(filesToProcessList(aFile, level + 1, excluded));
                    } else {
                        //System.out.println(aFile.getName());
                        treeSet.add(aFile);
                    }
                }
            }
        }
        return treeSet;
    }

}
