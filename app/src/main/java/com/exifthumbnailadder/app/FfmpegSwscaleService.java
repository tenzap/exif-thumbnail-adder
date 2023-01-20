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

import android.app.Service;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;

import com.exifthumbnailadder.app.exception.FfmpegHelperException;
import com.schokoladenbrown.Smooth;

public class FfmpegSwscaleService extends Service {
    /**
     * Command to the service to display a message
     */
    static final int RUN_SWSCALE = 1;

    /**
     * Handler of incoming messages from clients.
     */
    static class IncomingHandler extends Handler {
        Bitmap thumbnail;
        Messenger clientMessenger;

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case RUN_SWSCALE:
                    clientMessenger = msg.replyTo;

                    Bundle bundle = msg.getData();
                    bundle.setClassLoader(ThumbnailProject.class.getClassLoader());
                    ThumbnailProject tf = bundle.getParcelable("thumbnail_project");

                    Runnable doSwscale = () -> {
                        try {
                            if (tf.algo != null)
                                thumbnail = Smooth.rescale(tf.original, tf.targetSize.getWidth(), tf.targetSize.getHeight(), tf.algo);  // 3 is default width in ffmpeg.
                            else if (tf.algo1 != null)
                                thumbnail = Smooth.rescale(tf.original, tf.targetSize.getWidth(), tf.targetSize.getHeight(), tf.algo1, tf.p0);  // 3 is default width in ffmpeg.
                            else if (tf.algo2 != null)
                                thumbnail = Smooth.rescale(tf.original, tf.targetSize.getWidth(), tf.targetSize.getHeight(), tf.algo2, tf.p0, tf.p1);  // 3 is default width in ffmpeg.
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    };

                    Thread t = new Thread(doSwscale);

                    // Set UncaughtExceptionHandler to catch crash
                    t.setDefaultUncaughtExceptionHandler(new Thread.
                            UncaughtExceptionHandler() {
                        public void uncaughtException(Thread t, Throwable e) {
                            try {
                                throw new FfmpegHelperException("ffmpeg: Crash");
                            } catch (Exception e2) {
                                e2.printStackTrace();
                            }
                        }
                    });

                    t.start();

                    // Wait until thread is finished.
                    try {
                        t.join();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }

                    // Reply to client and send the thumbnail
                    Message reply = Message.obtain(null, ThumbnailFactory.FFMPEG_SERVICE_THUMBNAIL, thumbnail);
                    try {
                        clientMessenger.send(reply);
                    } catch (RemoteException e) {
                        e.printStackTrace();
                    }
                    break;
                default:
                    super.handleMessage(msg);
            }
        }
    }

    /**
     * Target we publish for clients to send messages to IncomingHandler.
     */
    Messenger mMessenger;

    /**
     * When binding to the service, we return an interface to our messenger
     * for sending messages to the service.
     */
    @Override
    public IBinder onBind(Intent intent) {
        mMessenger = new Messenger(new IncomingHandler());
        return mMessenger.getBinder();
    }
}
