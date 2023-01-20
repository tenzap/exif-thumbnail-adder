/*
 * Copyright (C) 2021-2023 Fab Stz <fabstz-it@yahoo.fr>
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

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.Bitmap;
import android.media.ThumbnailUtils;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.util.Log;
import android.util.Size;

import com.exifthumbnailadder.app.exception.FfmpegHelperException;

class ThumbnailFactory {

    static final int FFMPEG_SERVICE_THUMBNAIL = 1;
    static final int FFMPEG_SERVICE_CRASH = 2;

    HandlerThread ht;
    static Bitmap ffmpeg_thumbnail;

    /**
     * Messenger for communicating with the service.
     */
    Messenger mService = null;

    /**
     * Flag indicating whether we have called bind on the service.
     */
    boolean bound;
    static boolean service_replied = false;
    static boolean doStopThread = false;
    final static Object sync = new Object();

    /**
     * Target we publish for clients to send messages to IncomingHandler.
     */
    final Messenger mMessenger;

    /**
     * Class for interacting with the main interface of the service.
     */
    private ServiceConnection mConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName className, IBinder service) {
            // This is called when the connection with the service has been
            // established, giving us the object we can use to
            // interact with the service.  We are communicating with the
            // service using a Messenger, so here we get a client-side
            // representation of that from the raw IBinder object.
            mService = new Messenger(service);
            bound = true;
        }

        public void onServiceDisconnected(ComponentName className) {
            // This is called when the connection with the service has been
            // unexpectedly disconnected -- that is, its process crashed.
            mService = null;
            bound = false;
        }
    };

    /**
     * Handler of incoming messages from service.
     */
    static class IncomingHandler extends Handler {

        public IncomingHandler(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case FFMPEG_SERVICE_THUMBNAIL:
                    synchronized (sync) {
                        if (MainApplication.enableLog) Log.i("ETA", "received thumbnail from Ffmpeg service");
                        service_replied = true;
                        ffmpeg_thumbnail = (Bitmap) msg.obj;
                        sync.notify();
                    }
                    break;
                case FFMPEG_SERVICE_CRASH:
                    synchronized (sync) {
                        if (MainApplication.enableLog)
                            Log.w("ETA", "received crash message concerning Ffmpeg service");
                        doStopThread = true;
                        sync.notify();
                    }
                    break;
                default:
                    super.handleMessage(msg);
            }
        }
    }

    public static Size getThumbnailTargetSize(int imageWidth, int imageHeight, int maxSize) {
        float imageRatio = ((float) Math.min(imageWidth, imageHeight) / (float) Math.max(imageWidth, imageHeight));
        int thumbnailWidth = (imageWidth < imageHeight) ? Math.round(maxSize * imageRatio) : maxSize;
        int thumbnailHeight = (imageWidth < imageHeight) ? maxSize : Math.round(maxSize * imageRatio);
//        if (imageWidth < imageHeight) {
//            // Swap thumbnail width and height to keep a relative aspect ratio
//            int temp = thumbnailWidth;
//            thumbnailWidth = thumbnailHeight;
//            thumbnailHeight = temp;
//        }
        if (imageWidth < thumbnailWidth) thumbnailWidth = imageWidth;
        if (imageHeight < thumbnailHeight) thumbnailHeight = imageHeight;

        return new Size(thumbnailWidth, thumbnailHeight);
    }

    ThumbnailFactory() {
        ht = new HandlerThread("MyHandlerThread");
        ht.start();
        mMessenger = new Messenger(new IncomingHandler(ht.getLooper()));
    }

    public Bitmap getThumbnailWithFfmpegService(Context ctx, ThumbnailProject tp) throws Exception {
        // Bind to the service
        ctx.bindService(new Intent(ctx, FfmpegSwscaleService.class), mConnection,
                Context.BIND_AUTO_CREATE);

        while (!bound) {
            // TODO: set max wait time
            Thread.sleep(100);
        }

        // Create a message to the service, using a supported 'what' value
        Message msg = Message.obtain(null, FfmpegSwscaleService.RUN_SWSCALE);
        //Message msg = Message.obtain(null, FfmpegSwscaleService.REQUEST_CRASH);

        // set replyTo to the messenger of this class
        msg.replyTo = mMessenger;

        // send the ThumbnailFactory object
        Bundle data = new Bundle();
        data.putParcelable("thumbnail_project", tp);
        msg.setData(data);

        doStopThread = false;
        Runnable serviceReplyTimer = () -> {
            int serviceDuration = 0;
            while (serviceDuration < 2000) {
                if (doStopThread)
                    return;
                serviceDuration = serviceDuration + 200;
                try {
                    Thread.sleep(200);
                    Log.e("ETA", "waiting service reply... " + serviceDuration);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            Message reply = Message.obtain(null, FFMPEG_SERVICE_CRASH);
            try {
                mMessenger.send(reply);
            } catch (Exception e) {
                e.printStackTrace();
            }
        };
        Thread t = new Thread(serviceReplyTimer);

        t.start();

        synchronized (sync) {
            service_replied = false;
        }
        Log.e("ETA", "ggg8");
        try {
            mService.send(msg);
            Log.e("ETA", "ggg10");
        } catch (RemoteException e) {
            e.printStackTrace();
        }

        synchronized (sync) {
            if (service_replied == false) {
                try {
                    sync.wait();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
        doStopThread = true;

        if (service_replied == false) {
            ctx.unbindService(mConnection);
            throw new FfmpegHelperException("ffmpeg timeout. Probably ffmpeg crashed.");
        }

        // Unbind from the service
        if (bound) {
            ctx.unbindService(mConnection);
            bound = false;
        }

        return ffmpeg_thumbnail;

    }

    public Bitmap getThumbnailWithThumbnailUtils(ThumbnailProject tp) throws Exception {
        int tmpWidth, tmpHeight;
        Bitmap thumbnail = null;

        tmpWidth = tp.imageWidth;
        tmpHeight = tp.imageHeight;

        thumbnail = tp.original;
        while (tmpWidth / tp.targetSize.getWidth() > 2 || tmpHeight / tp.targetSize.getHeight() > 2) {
            tmpWidth /= 2;
            tmpHeight /= 2;
            thumbnail = ThumbnailUtils.extractThumbnail(thumbnail, tmpWidth, tmpHeight, ThumbnailUtils.OPTIONS_RECYCLE_INPUT);
        }
        return ThumbnailUtils.extractThumbnail(thumbnail, tp.targetSize.getWidth(), tp.targetSize.getHeight(), ThumbnailUtils.OPTIONS_RECYCLE_INPUT);
    }

    public Bitmap getThumbnailWithCreateScaledBitmap(ThumbnailProject tp) throws Exception {
        int tmpWidth, tmpHeight;
        Bitmap thumbnail = null;

        tmpWidth = tp.imageWidth;
        tmpHeight = tp.imageHeight;
        thumbnail = tp.original;
        while (tmpWidth / tp.targetSize.getWidth() > 2 || tmpHeight / tp.targetSize.getHeight() > 2) {
            tmpWidth /= 2;
            tmpHeight /= 2;
            thumbnail = Bitmap.createScaledBitmap(thumbnail, tmpWidth, tmpHeight, true);
        }
        return Bitmap.createScaledBitmap(thumbnail, tp.targetSize.getWidth(), tp.targetSize.getHeight(), true);
    }
}
