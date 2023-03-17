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

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Timer;
import java.util.TimerTask;

import org.junit.rules.TestWatcher;
import org.junit.runner.Description;

// Inspired from https://medium.com/jumio/how-to-record-screen-captures-during-ui-tests-on-android-67ba5dedb7e1
public class TestDataCollectionRule extends TestWatcher {
    Timer timer = new Timer();

    TimerTask timerTask = new TimerTask() {
        @Override
        public void run() {
            restartScreenRecord();
        }
    };

    Description description;
    private LocalDateTime testStartTime;

    private void setTestStartDate() {
        if (testStartTime == null) {
            testStartTime = LocalDateTime.now();
        }
    }

    private LocalDateTime getTestStartDate() {
        return testStartTime;
    }

    private String formatDateForTestName(LocalDateTime date) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");
        return date.format(formatter);
    }

    private String formatDateForLogcat(LocalDateTime date) {
        // We can't use the date format with a space because executeShellCommand
        // doesn't support it.
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-ddHH:mm:ss.SSS");
        // Return the test start time minus 2sec to catch also what happens in @BeforeClass & @Before
        return date.minusSeconds(2).format(formatter);
    }

    private String getTestName(Description description) {
        String timeStamp = formatDateForTestName(getTestStartDate());
        return timeStamp + "_" + BuildConfig.FLAVOR + "_" + description.getMethodName();
    }

    private String getTestName(Description description, LocalDateTime date) {
        String timeStamp = formatDateForTestName(date);
        return timeStamp + "_" + BuildConfig.FLAVOR + "_" + description.getMethodName();
    }

    @Override
    protected void starting(Description description) {
        this.description = description;
        super.starting(description);
        setTestStartDate();
        // Start Screenrecord in a thread, because executeShellCommand
        // waits until the command is finished and screerecord is a blocking command
        new Thread() {
            public void run() {
                super.run();
                Log.d("ETATest", "ScreenRecord: starting");
                TestDataCollectionUtils.startScreenRecord(getTestName(description));
            }
        }.start();
        timer.schedule(timerTask, 160*1000, 160*1000);
    }

    @Override
    protected void finished(Description description) {
        super.finished(description);

        Log.d("ETATest", "ScreenRecord: stopping");
        TestDataCollectionUtils.stopScreenRecord();

        Log.d("ETATest", "save Logcat");
        TestDataCollectionUtils.storeLogcat(getTestName(description), formatDateForLogcat(getTestStartDate()));

        timer.cancel();
    }

    private void restartScreenRecord() {
        new Thread() {
            @Override
            public void run() {
                super.run();
                Log.d("ETATest", "ScreenRecord: restarting");
                TestDataCollectionUtils.stopScreenRecord();
                TestDataCollectionUtils.startScreenRecord(getTestName(description, LocalDateTime.now()));
            }
        }.start();
    }
}
