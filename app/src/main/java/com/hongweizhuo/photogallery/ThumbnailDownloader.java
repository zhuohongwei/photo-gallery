package com.hongweizhuo.photogallery;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.support.annotation.NonNull;
import android.util.Log;
import android.util.LruCache;

import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class ThumbnailDownloader<T> extends HandlerThread {

    private static final String TAG = "ThumbnailDownloader";

    private static final int MESSAGE_DOWNLOAD_IMAGE = 0;

    private LruCache<String, Bitmap> mImageCache = new LruCache<>(100);

    private boolean mHasQuit = false;

    private Handler mRequestHandler;

    private ConcurrentMap<T, String> mRequestMap = new ConcurrentHashMap<>();
    private ConcurrentMap<String, Boolean> mPreloadRequestMap = new ConcurrentHashMap<>();

    private Handler mResponseHandler;

    private ThumbnailDownloadListener<T> mThumbnailDownloadListener;

    public interface ThumbnailDownloadListener<T> {
        void onThumbnailDownloaded(T target, Bitmap thumbnail);
    }

    public void setThumbnailDownloadListener(ThumbnailDownloadListener<T> listener) {
        mThumbnailDownloadListener = listener;
    }

    public ThumbnailDownloader(Handler responseHandler) {
        super(TAG);
        mResponseHandler = responseHandler;
    }

    @Override
    protected void onLooperPrepared() {
        mRequestHandler = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                if (msg.what == MESSAGE_DOWNLOAD_IMAGE) {
                    T target = (T) msg.obj;
                    handleDownloadImageRequest(target);
                }
            }
        };
    }

    public void clearQueue() {
        mRequestHandler.removeMessages(MESSAGE_DOWNLOAD_IMAGE);
        mRequestMap.clear();
    }

    private void handleDownloadImageRequest(final T target) {
        try {
            final String url = mRequestMap.get(target);
            if (url == null) {
                return;
            }

            final Bitmap bitmap = getBitmap(url);

            mResponseHandler.post(new Runnable() {
                @Override
                public void run() {
                    if (mRequestMap.get(target) != url || mHasQuit) { return; }

                    mRequestMap.remove(target);
                    mThumbnailDownloadListener.onThumbnailDownloaded(target, bitmap);

                }

            });


        } catch (IOException ioe) {
            Log.e(TAG, "Error downloading image", ioe);
        }
    }

    private Bitmap getBitmap(@NonNull String url) throws IOException {

        Bitmap bitmap = mImageCache.get(url);

        if (bitmap == null) {
            byte[] bitmapBytes = new FlickrFetcher().getUrlBytes(url);
            bitmap = BitmapFactory.decodeByteArray(bitmapBytes, 0, bitmapBytes.length);
            mImageCache.put(url, bitmap);

            Log.i(TAG, "Bitmap created");

        } else {
            Log.i(TAG, "Bitmap served from cache");
        }

        return bitmap;

    }

    @Override
    public boolean quit() {
        mHasQuit = true;
        return super.quit();
    }

    public void queueThumnbnail(T target, String url) {
        if (url == null) {
            mRequestMap.remove(target);

        } else {
            mRequestMap.put(target, url);
            mRequestHandler.obtainMessage(MESSAGE_DOWNLOAD_IMAGE, target).sendToTarget();
        }
    }

    public void preloadThumbnail(final String url) {
        mRequestHandler.post(new Runnable() {
            @Override
            public void run() {

                if (url == null) { return; }

                if (mPreloadRequestMap.containsKey(url)) { return; }

                mPreloadRequestMap.put(url, true);

                try {
                    Log.i(TAG, "Preloading image");
                    getBitmap(url);

                } catch (IOException e) {
                    Log.e(TAG, "Error preloading image", e);

                } finally {
                    mPreloadRequestMap.remove(url);
                }

            }
        });
    }

}
