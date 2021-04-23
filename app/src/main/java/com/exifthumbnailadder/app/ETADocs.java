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
    int level;
    String excluded;

    public ETADocs(Context ctx, Object etaDocsRoot, int level, String excluded) {
        this.etaDocsRoot = etaDocsRoot;
        this.level = level;
        this.excluded = excluded;
        this.ctx = ctx;
    }

    public Object[] getDocsArray() {
        if (etaDocsRoot instanceof Uri) {
            DocumentFile baseDf = DocumentFile.fromTreeUri(ctx, (Uri) etaDocsRoot);
            boolean canRead = baseDf.canRead();

            ArrayList<DocumentFile> fileList = new ArrayList<DocumentFile>();
            listDocFilesToProcess_int(baseDf, 0, fileList, excluded);
            DocumentFile[] filesInDir = new DocumentFile[fileList.size()];
            filesInDir = fileList.toArray(filesInDir);
            return filesInDir; //DocumentFile[]
        }
        if (etaDocsRoot instanceof File) {
            // https://stackoverflow.com/a/27996686
            //File[] filesInDir = new File(srcPath).listFiles(new MyFilenameFilter(srcPath, true));
            //File[] filesInDir = listFilesRecursive(new File(srcPath));

            ArrayList<File> fileList = new ArrayList<File>();
            listDirectoryAsFile((File)etaDocsRoot, 0, fileList, new File(excluded));
            File[] filesInDir = new File[fileList.size()];
            filesInDir = fileList.toArray(filesInDir);
            if (MainApplication.enableLog) Log.i(MainApplication.TAG, "Array of filesInDir: " + filesInDir.toString());
            return filesInDir; //File[]
        }
        return null;
    }

    TreeSet<DocumentFile> getDocsSetOfDf() {
        TreeSet<DocumentFile> treeSet = new TreeSet<DocumentFile>(new DocumentFileComparator());
        DocumentFile[] docsArray = (DocumentFile[])getDocsArray();
        for (int i=0; i<docsArray.length; i++) {
            treeSet.add(docsArray[i]);
        }
        return treeSet;
    }

    class DocumentFileComparator implements Comparator<DocumentFile> {
        @Override
        public int compare(DocumentFile e1, DocumentFile e2) {
            return e1.getUri().toString().compareTo(e2.getUri().toString());
        }
    }

    private void listDocFilesToProcess_int(DocumentFile df, int level, ArrayList<DocumentFile> arrayList, String excluded) {
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
                        listDocFilesToProcess_int(aFile, level + 1, arrayList, excluded);
                    }
                } else {
                    //System.out.println(aFile.getName());
                    arrayList.add(aFile);
                }
            }
        }
    }

    public void listDirectory(File dir, int level) {
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

    private void listDirectoryAsFile(File dir, int level, ArrayList<File> arrayList, File excluded) {
        File[] firstLevelFiles = dir.listFiles();
        if (firstLevelFiles != null && firstLevelFiles.length > 0) {
            for (File aFile : firstLevelFiles) {
                if (!aFile.getPath().startsWith(excluded.getPath())) {
                    for (int i = 0; i < level; i++) {
                        //System.out.print("\t");
                    }
                    if (aFile.isDirectory()) {
                        //System.out.println("[" + aFile.getName() + "]");
                        listDirectoryAsFile(aFile, level + 1, arrayList, excluded);
                    } else {
                        //System.out.println(aFile.getName());
                        arrayList.add(aFile);
                    }
                }
            }
        }
    }

}
