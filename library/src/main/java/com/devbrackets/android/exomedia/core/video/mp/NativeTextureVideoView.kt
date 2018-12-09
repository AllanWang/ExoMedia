/*
 * Copyright (C) 2016 - 2019 ExoMedia Contributors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.devbrackets.android.exomedia.core.video.mp

import android.annotation.TargetApi
import android.content.Context
import android.graphics.SurfaceTexture
import android.media.MediaPlayer
import android.net.Uri
import android.os.Build
import android.support.annotation.FloatRange
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.Surface
import android.view.TextureView
import android.view.View
import android.widget.MediaController

import com.devbrackets.android.exomedia.ExoMedia
import com.devbrackets.android.exomedia.core.ListenerMux
import com.devbrackets.android.exomedia.core.api.VideoViewApi
import com.devbrackets.android.exomedia.core.exoplayer.WindowInfo
import com.devbrackets.android.exomedia.core.listener.CaptionListener
import com.devbrackets.android.exomedia.core.video.ResizingTextureView
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.drm.MediaDrmCallback
import com.google.android.exoplayer2.source.MediaSource
import com.google.android.exoplayer2.source.TrackGroupArray

/**
 * A "Native" VideoView implementation using the [android.view.TextureView]
 * as a backing instead of the older [android.view.SurfaceView].  This
 * resolves issues with the SurfaceView because the TextureView is an actual
 * View that follows the normal drawing paths; allowing the view to be animated,
 * scaled, etc.
 * <br></br><br></br>
 * NOTE: This does remove some of the functionality from the VideoView including:
 *
 *  * The [MediaController]
 *
 */
class NativeTextureVideoView : ResizingTextureView, NativeVideoDelegate.Callback, VideoViewApi {
    protected var touchListener: View.OnTouchListener? = null
    protected val delegate: NativeVideoDelegate by lazy { NativeVideoDelegate(context, this, this) }

    override val duration: Long
        get() = delegate.duration

    override val currentPosition: Long
        get() = delegate.currentPosition

    override val isPlaying: Boolean
        get() = delegate.isPlaying

    override var volume: Float = delegate.getVolume()

    override val bufferedPercent: Int
        get() = delegate.bufferPercentage

    override val windowInfo: WindowInfo?
        get() = delegate.windowInfo

    override val playbackSpeed: Float
        get() = delegate.playbackSpeed

    override val availableTracks: Map<ExoMedia.RendererType, TrackGroupArray>?
        get() = null

    constructor(context: Context) : super(context)
    constructor(context: Context, attrs: AttributeSet) : super(context, attrs)
    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int) : super(context, attrs, defStyleAttr)
    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int, defStyleRes: Int) : super(context, attrs, defStyleAttr, defStyleRes)

    init {
        surfaceTextureListener = TextureVideoViewSurfaceListener()

        isFocusable = true
        isFocusableInTouchMode = true
        requestFocus()

        updateVideoSize(0, 0)
    }

    override fun setDrmCallback(drmCallback: MediaDrmCallback?) {
        //Purposefully left blank
    }

    override fun start() {
        delegate.start()
        requestFocus()
    }

    override fun pause() {
        delegate.pause()
    }

    override fun seekTo(milliseconds: Long) {
        delegate.seekTo(milliseconds)
    }

    override fun videoSizeChanged(width: Int, height: Int) {
        if (updateVideoSize(width, height)) {
            requestLayout()
        }
    }

    override fun onTouchEvent(ev: MotionEvent): Boolean {
        return touchListener?.onTouch(this, ev) == true || super.onTouchEvent(ev)
    }

    override fun setOnTouchListener(listener: View.OnTouchListener?) {
        touchListener = listener
        super.setOnTouchListener(listener)
    }

    override fun setVideoUri(uri: Uri?) {
        setVideoUri(uri, null)
    }

    override fun setVideoUri(uri: Uri?, mediaSource: MediaSource?) {
        setVideoURI(uri)
    }

    override fun setVolume(@FloatRange(from = 0.0, to = 1.0) volume: Float): Boolean {
        return delegate.setVolume(volume)
    }

    /**
     * If the video has completed playback, calling `restart` will seek to the beginning of the video, and play it.
     *
     * @return `true` if the video was successfully restarted, otherwise `false`
     */
    override fun restart(): Boolean {
        return delegate.restart()
    }

    override fun stopPlayback(clearSurface: Boolean) {
        delegate.stopPlayback(clearSurface)
    }

    override fun release() {
        //Purposefully left blank
    }

    override fun setPlaybackSpeed(speed: Float): Boolean {
        return delegate.setPlaybackSpeed(speed)
    }

    override fun setCaptionListener(listener: CaptionListener?) {
        // Purposefully left blank
    }

    override fun trackSelectionAvailable(): Boolean {
        return false
    }

    fun setTrack(trackType: ExoMedia.RendererType, trackIndex: Int) {
        // Purposefully left blank
    }

    override fun setTrack(type: ExoMedia.RendererType, groupIndex: Int, trackIndex: Int) {
        // Purposefully left blank
    }

    override fun getSelectedTrackIndex(type: ExoMedia.RendererType, groupIndex: Int): Int {
        return -1
    }

    override fun clearSelectedTracks(type: ExoMedia.RendererType) {
        // Purposefully left blank
    }

    override fun setRendererEnabled(type: ExoMedia.RendererType, enabled: Boolean) {
        // Purposefully left blank
    }

    override fun isRendererEnabled(type: ExoMedia.RendererType): Boolean {
        return false
    }

    override fun setListenerMux(listenerMux: ListenerMux) {
        delegate.listenerMux = listenerMux
    }

    override fun onVideoSizeChanged(width: Int, height: Int, pixelWidthHeightRatio: Float) {
        if (updateVideoSize((width * pixelWidthHeightRatio).toInt(), height)) {
            requestLayout()
        }
    }

    override fun setRepeatMode(@Player.RepeatMode repeatMode: Int) {
        // Purposefully left blank
    }

    /**
     * Cleans up the resources being held.  This should only be called when
     * destroying the video view
     */
    override fun suspend() {
        delegate.suspend()
    }

    /**
     * Sets video URI using specific headers.
     *
     * @param uri The Uri for the video to play
     * @param headers The headers for the URI request.
     * Note that the cross domain redirection is allowed by default, but that can be
     * changed with key/value pairs through the headers parameter with
     * "android-allow-cross-domain-redirect" as the key and "0" or "1" as the value
     * to disallow or allow cross domain redirection.
     */
    @JvmOverloads
    fun setVideoURI(uri: Uri?, headers: Map<String, String>? = null) {
        delegate.setVideoURI(uri!!, headers)

        requestLayout()
        invalidate()
    }

    /**
     * Register a callback to be invoked when the media file
     * is loaded and ready to go.
     *
     * @param listener The callback that will be run
     */
    fun setOnPreparedListener(listener: MediaPlayer.OnPreparedListener?) {
        delegate.onPreparedListener = listener
    }

    /**
     * Register a callback to be invoked when the end of a media file
     * has been reached during playback.
     *
     * @param listener The callback that will be run
     */
    fun setOnCompletionListener(listener: MediaPlayer.OnCompletionListener?) {
        delegate.onCompletionListener = listener
    }

    /**
     * Register a callback to be invoked when the status of a network
     * stream's buffer has changed.
     *
     * @param listener the callback that will be run.
     */
    fun setOnBufferingUpdateListener(listener: MediaPlayer.OnBufferingUpdateListener?) {
        delegate.onBufferingUpdateListener = listener
    }

    /**
     * Register a callback to be invoked when a seek operation has been
     * completed.
     *
     * @param listener the callback that will be run
     */
    fun setOnSeekCompleteListener(listener: MediaPlayer.OnSeekCompleteListener?) {
        delegate.onSeekCompleteListener = listener
    }

    /**
     * Register a callback to be invoked when an error occurs
     * during playback or setup.  If no repeatListener is specified,
     * or if the repeatListener returned false, TextureVideoView will inform
     * the user of any errors.
     *
     * @param listener The callback that will be run
     */
    fun setOnErrorListener(listener: MediaPlayer.OnErrorListener?) {
        delegate.onErrorListener = listener
    }

    /**
     * Register a callback to be invoked when an informational event
     * occurs during playback or setup.
     *
     * @param listener The callback that will be run
     */
    fun setOnInfoListener(listener: MediaPlayer.OnInfoListener?) {
        delegate.onInfoListener = listener
    }

    protected inner class TextureVideoViewSurfaceListener : TextureView.SurfaceTextureListener {
        override fun onSurfaceTextureAvailable(surfaceTexture: SurfaceTexture, width: Int, height: Int) {
            delegate.onSurfaceReady(Surface(surfaceTexture))
        }

        override fun onSurfaceTextureSizeChanged(surface: SurfaceTexture, width: Int, height: Int) {
            delegate.onSurfaceSizeChanged(width, height)
        }

        override fun onSurfaceTextureDestroyed(surface: SurfaceTexture): Boolean {
            surface.release()
            suspend()
            return true
        }

        override fun onSurfaceTextureUpdated(surface: SurfaceTexture) {
            //Purposefully left blank
        }
    }
}