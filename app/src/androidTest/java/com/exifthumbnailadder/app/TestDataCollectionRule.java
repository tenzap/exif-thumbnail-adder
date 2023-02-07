/*
 * Copyright (C) 2023 Fab Stz <fabstz-it@yahoo.fr>
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

import android.util.Log;

import java.util.Calendar;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.junit.rules.TestWatcher;
import org.junit.runner.Description;

// Inspired from https://medium.com/jumio/how-to-record-screen-captures-during-ui-tests-on-android-67ba5dedb7e1
public class TestDataCollectionRule extends TestWatcher {

    private Date testStartTime;

    private void setTestStartDate() {
        if (testStartTime == null) {
            testStartTime = Calendar.getInstance().getTime();
        }
    }

    private Date getTestStartDate() {
        return testStartTime;
    }

    private String formatDateForTestName(Date date) {
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyyMMdd_HHmmss");
        return simpleDateFormat.format(date);
    }

    private String formatDateForLogcat(Date date) {
        // We can't use the date format with a space because executeShellCommand
        // doesn't support it.
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-ddHH:mm:ss.SSS");
        return simpleDateFormat.format(date);
    }

    private String getTestName(Description description) {
        String timeStamp = formatDateForTestName(getTestStartDate());
        return timeStamp + "_" + BuildConfig.FLAVOR + "_" + description.getMethodName();
    }

    @Override
    protected void starting(Description description) {
        super.starting(description);
        setTestStartDate();
        // Start Screenrecord in a thread, because executeShellCommand
        // waits until the command is finished ans screerecord is a blocking command
        new Thread() {
            public void run() {
                super.run();
                Log.d("ETATest", "ScreenRecord: starting");
                TestDataCollectionUtils.startScreenRecord(getTestName(description));
            }
        }.start();
    }

    @Override
    protected void finished(Description description) {
        super.finished(description);

        Log.d("ETATest", "ScreenRecord: stopping");
        TestDataCollectionUtils.stopScreenRecord();

        Log.d("ETATest", "save Logcat");
        TestDataCollectionUtils.storeLogcat(getTestName(description), formatDateForLogcat(getTestStartDate()));
    }
}
