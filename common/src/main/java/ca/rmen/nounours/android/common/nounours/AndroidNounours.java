/*
 *   Copyright (c) 2009 - 2015 Carmen Alvarez
 *
 *   This file is part of Nounours for Android.
 *
 *   Nounours for Android is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU General Public License as published by
 *   the Free Software Foundation, either version 3 of the License, or
 *   (at your option) any later version.
 *
 *   Nounours for Android is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU General Public License for more details.
 *
 *   You should have received a copy of the GNU General Public License
 *   along with Nounours for Android.  If not, see <http://www.gnu.org/licenses/>.
 */

package ca.rmen.nounours.android.common.nounours;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.os.Handler;
import android.util.Log;
import android.view.SurfaceHolder;

import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.atomic.AtomicBoolean;

import ca.rmen.nounours.Nounours;
import ca.rmen.nounours.NounoursAnimationHandler;
import ca.rmen.nounours.NounoursSoundHandler;
import ca.rmen.nounours.NounoursVibrateHandler;
import ca.rmen.nounours.android.common.Constants;
import ca.rmen.nounours.android.common.nounours.cache.ImageCache;
import ca.rmen.nounours.android.common.nounours.cache.NounoursResourceCache;
import ca.rmen.nounours.android.common.settings.NounoursSettings;
import ca.rmen.nounours.android.common.util.ThemeUtil;
import ca.rmen.nounours.common.R;
import ca.rmen.nounours.data.Image;
import ca.rmen.nounours.data.Theme;
import ca.rmen.nounours.io.StreamLoader;

/**
 * Implementation of the abstract Nounours class, containing logic specific to
 * Android.
 *
 * @author Carmen Alvarez
 */
public class AndroidNounours extends Nounours {

    private static final String TAG = Constants.TAG + AndroidNounours.class.getSimpleName();

    private final String mTag;
    private final Context mContext;
    private final Handler mUIHandler;
    private final NounoursSettings mSettings;
    private final SurfaceHolder mSurfaceHolder;
    private final ThemeLoadListener mListener;
    private int mViewWidth;
    private int mViewHeight;
    private final NounoursResourceCache mNounoursResourceCache;
    private final AtomicBoolean mOkToDraw = new AtomicBoolean(false);
    private final NounoursRenderer mRenderer;

    /**
     * Open the CSV data files and call the superclass
     * {@link Nounours#init(StreamLoader, NounoursAnimationHandler, NounoursSoundHandler, NounoursVibrateHandler, InputStream, InputStream, String)}
     * method.
     *
     * @param tag     used for logging, to distinguish between the lwp and app instances
     * @param context The android mContext.
     */
    public AndroidNounours(String tag,
                           Context context,
                           Handler uiHandler,
                           NounoursSettings settings,
                           SurfaceHolder surfaceHolder,
                           NounoursRenderer renderer,
                           NounoursResourceCache nounoursResourceCache,
                           NounoursSoundHandler soundHandler,
                           NounoursVibrateHandler vibrateHandler,
                           ThemeLoadListener listener) {

        mTag = "/" + tag;
        mContext = context;
        mUIHandler = uiHandler;
        mSettings = settings;
        mSurfaceHolder = surfaceHolder;
        mListener = listener;
        mNounoursResourceCache = nounoursResourceCache;
        mRenderer = renderer;
        StreamLoader streamLoader = new AssetStreamLoader(context);

        String themeId = mSettings.getThemeId();
        AnimationHandler animationHandler = new AnimationHandler(this);
        final InputStream propertiesFile = context.getResources().openRawResource(R.raw.nounours);
        final InputStream themesFile = context.getResources().openRawResource(R.raw.themes);
        mSurfaceHolder.addCallback(mSurfaceHolderCallback);

        try {
            init(streamLoader, animationHandler, soundHandler, vibrateHandler, propertiesFile,
                    themesFile, themeId);
            setEnableVibrate(mSettings.isSoundEnabled());
            setEnableSound(mSettings.isSoundEnabled());
            setIdleTimeout(mSettings.getIdleTimeout());
        } catch (final IOException e) {
            Log.e(TAG + mTag, "Error initializing nounours", e);
        }
    }

    @Override
    protected boolean cacheResources() {
        Theme theme = getCurrentTheme();
        return mNounoursResourceCache.loadImages(theme, mImageCacheListener)
                && mNounoursResourceCache.loadSounds(theme);
    }

    /**
     * Load the new image set in a separate thread, showing the progress bar
     */
    @Override
    public void useTheme(final String id) {
        Log.v(TAG + mTag, "useTheme " + id);

        // Get the name of this theme.
        Theme theme = getThemes().get(id);
        CharSequence themeLabel = ThemeUtil.getThemeLabel(mContext, theme);

        // MEMORY
        mNounoursResourceCache.freeImages();
        mNounoursResourceCache.freeSounds();

        Thread themeLoader = new Thread() {
            @SuppressWarnings("synthetic-access")
            @Override
            public void run() {

                AndroidNounours.super.useTheme(id);

                runTask(new Runnable() {
                    public void run() {
                        mListener.onThemeLoadComplete();
                    }
                });
            }
        };
        mListener.onThemeLoadStart(theme.getImages().size(), mContext.getString(R.string.loading, themeLabel));
        themeLoader.start();
    }

    /**
     * Display a picture on the screen.
     *
     * @see ca.rmen.nounours.Nounours#displayImage(ca.rmen.nounours.data.Image)
     */
    @Override
    protected void displayImage(final Image image) {
        Log.v(TAG + mTag, "displayImage " + image);
        if (image == null) return;
        if (!mOkToDraw.get()) return;
        final Bitmap bitmap = mNounoursResourceCache.getDrawableImage(mContext, image);
        if (bitmap == null) return;

        Canvas c = mSurfaceHolder.lockCanvas();
        if (c != null) {
            mRenderer.render(mSettings, bitmap, c, mViewWidth, mViewHeight);
            mSurfaceHolder.unlockCanvasAndPost(c);
        }
    }

    public void redraw() {
        displayImage(getCurrentImage());
    }

    /**
     * Trace.
     */
    @Override
    protected void debug(final Object o) {
        if (o instanceof Throwable) {
            Throwable t = (Throwable) o;
            Log.w(TAG + mTag, t.getMessage(), t);
        } else {
            Log.v(TAG + mTag, "" + o);
        }
    }

    /**
     * UI threads should be run with an Android thread call.
     *
     * @see ca.rmen.nounours.Nounours#runTask(java.lang.Runnable)
     */
    @Override
    protected void runTask(final Runnable task) {
        mUIHandler.post(task);
    }

    /**
     * Cleanup.
     */
    public void onDestroy() {
        Log.v(TAG + mTag, "destroy");
        mNounoursResourceCache.freeImages();
        mNounoursResourceCache.freeSounds();
    }

    @Override
    protected int getDeviceHeight() {
        return mViewHeight;
    }

    @Override
    protected int getDeviceWidth() {
        return mViewWidth;
    }

    /**
     * Reread the shared preferences and apply the new app_settings.
     */
    public void reloadSettings() {
        if (mSettings.isSoundEnabled() && !isSoundEnabled()) {
            mNounoursResourceCache.loadSounds(getCurrentTheme());
        } else if (!mSettings.isSoundEnabled() && isSoundEnabled()) {
            mNounoursResourceCache.freeSounds();
        }

        setEnableSound(mSettings.isSoundEnabled());
        setEnableVibrate(mSettings.isSoundEnabled());
        setIdleTimeout(mSettings.getIdleTimeout());
        reloadThemeFromPreference();
    }

    private void reloadThemeFromPreference() {
        Log.v(TAG + mTag, "reloadThemeFromPreference");
        boolean nounoursIsBusy = isLoading();
        Log.v(TAG + mTag, "reloadThemeFromPreference, nounoursIsBusy = " + nounoursIsBusy);
        String themeId = mSettings.getThemeId();
        if (getCurrentTheme() != null && getCurrentTheme().getId().equals(themeId)) {
            return;
        }
        final Theme theme = getThemes().get(themeId);
        if (theme != null) {
            stopAnimation();
            useTheme(theme.getId());
        }
    }

    @SuppressWarnings("FieldCanBeLocal")
    private final SurfaceHolder.Callback mSurfaceHolderCallback = new SurfaceHolder.Callback() {
        @Override
        public void surfaceCreated(SurfaceHolder surfaceHolder) {
            Log.v(TAG + mTag, "surfaceCreated");
            mOkToDraw.set(true);
            redraw();
        }

        @Override
        public void surfaceChanged(SurfaceHolder surfaceHolder, int format, int width, int height) {
            Log.v(TAG + mTag, "surfaceChanged");
            mViewWidth = width;
            mViewHeight = height;
            redraw();
        }

        @Override
        public void surfaceDestroyed(SurfaceHolder surfaceHolder) {
            Log.v(TAG + mTag, "surfaceDestroyed");
            mOkToDraw.set(false);
        }
    };

    @SuppressWarnings("FieldCanBeLocal")
    private final ImageCache.ImageCacheListener mImageCacheListener = new ImageCache.ImageCacheListener() {
        @Override
        public void onImageLoaded(final Image image, int progress, int total) {
            Log.v(TAG + mTag, "onImageLoaded: " + progress + "/" + total);
            setImage(image);
            CharSequence themeName = ThemeUtil.getThemeLabel(mContext, getCurrentTheme());
            mListener.onThemeLoadProgress(progress, total, mContext.getString(R.string.loading, themeName));
        }
    };
}
