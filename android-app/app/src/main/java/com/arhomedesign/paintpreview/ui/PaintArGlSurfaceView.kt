package com.arhomedesign.paintpreview.ui

import android.content.Context
import android.opengl.GLSurfaceView
import android.util.AttributeSet
import android.view.MotionEvent
import com.google.ar.core.Session
import com.arhomedesign.paintpreview.gles.PaintPreviewGlRenderer

/**
 * Full-screen GL view for ARCore camera + tinted planes. Host activity must forward lifecycle
 * [onHostResume] / [onHostPause] and call [closeSession] when leaving the screen.
 */
class PaintArGlSurfaceView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
) : GLSurfaceView(context, attrs) {

    val paintRenderer = PaintPreviewGlRenderer(context)

    init {
        preserveEGLContextOnPause = true
        setEGLContextClientVersion(2)
        // Default EGL config is more compatible across GPUs than forcing 8888; avoids black camera on some devices.
        setRenderer(paintRenderer)
        renderMode = RENDERMODE_CONTINUOUSLY
    }

    fun attachSession(session: Session) {
        paintRenderer.session = session
    }

    fun detachSession() {
        paintRenderer.session = null
    }

    /** Clears tap-based plane spotlight and ML region focus (GL thread). */
    fun clearPaintFocus() {
        queueEvent { paintRenderer.clearPaintFocus() }
    }

    fun onHostResume() {
        paintRenderer.markSessionRunning(true)
        onResume()
    }

    fun onHostPause() {
        paintRenderer.markSessionRunning(false)
        onPause()
    }

    /** Call after [onHostPause]; closes ARCore on the main thread. */
    fun closeSession(session: Session?) {
        paintRenderer.markSessionRunning(false)
        paintRenderer.session = null
        session?.close()
    }

    override fun onDetachedFromWindow() {
        paintRenderer.shutdownSegmentation()
        super.onDetachedFromWindow()
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (event.actionMasked == MotionEvent.ACTION_UP) {
            paintRenderer.queueScreenTap(event.x, event.y)
        }
        return true
    }
}
