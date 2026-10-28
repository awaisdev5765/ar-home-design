package com.arhomedesign.paintpreview.gles

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.opengl.GLES11Ext
import android.opengl.GLES20
import android.opengl.GLSurfaceView
import android.opengl.Matrix
import android.view.WindowManager
import com.google.ar.core.Coordinates2d
import com.google.ar.core.Frame
import com.google.ar.core.HitResult
import com.google.ar.core.Plane
import com.google.ar.core.Session
import com.google.ar.core.TrackingState
import com.google.ar.core.exceptions.CameraNotAvailableException
import com.google.ar.core.exceptions.UnavailableException
import com.arhomedesign.paintpreview.PaintSurfaceFilter
import com.arhomedesign.paintpreview.SurfacePaintKind
import com.arhomedesign.paintpreview.surfacePaintKind
import com.arhomedesign.paintpreview.isDrawableForPaintPreview
import com.arhomedesign.paintpreview.matchesFilter
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import java.util.concurrent.atomic.AtomicReference
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10

/** ARCore camera + plane-based paint overlay (no ML dependency). */
class PaintPreviewGlRenderer(
    private val context: Context,
) : GLSurfaceView.Renderer {
    private val mainHandler = Handler(Looper.getMainLooper())
    @Volatile
    private var sessionRunning: Boolean = false

    /**
     * Invoked on the **main** thread when tap-based paint focus (AI ROI and/or plane spotlight)
     * becomes active or inactive.
     */
    var paintFocusActiveListener: ((Boolean) -> Unit)? = null

    private var lastPaintFocusNotified: Boolean = false

    @Volatile
    var paintColorArgb: Int = 0x99286B9E.toInt()

    @Volatile
    var surfaceFilter: PaintSurfaceFilter = PaintSurfaceFilter.ALL

    @Volatile
    var session: Session? = null

    @Volatile
    var wallNormalThreshold: Float = 0.50f

    @Volatile
    var drawSurfaceOutlines: Boolean = true

    /** When true and a plane was hit, only that plane receives plane-based tint. */
    @Volatile
    var tapPlaneSpotlightEnabled: Boolean = true

    /** Independent wall / floor / ceiling colors (planes + wall ML uses wall slot). */
    @Volatile
    var studioSurfaceColors: Boolean = false

    @Volatile
    var wallPaintArgb: Int = 0x99286B9E.toInt()

    @Volatile
    var floorPaintArgb: Int = 0x994A6FA5.toInt()

    @Volatile
    var ceilingPaintArgb: Int = 0x99E8E0D5.toInt()

    @Volatile
    var accentPaintArgb: Int = 0x99C4B896.toInt()

    private var viewportWidth = 1
    private var viewportHeight = 1
    private var lastDisplayRotation = -1
    private var lastDisplayWidth = -1
    private var lastDisplayHeight = -1

    private val cameraTextureId = IntArray(1)

    private var bgProgram = 0
    private var bgPosAttrib = 0
    private var bgTexAttrib = 0
    private var bgSamplerUniform = 0

    private var planeProgram = 0
    private var planeMvpUniform = 0
    private var planePosAttrib = 0
    private var planeColorUniform = 0

    private val quadNdc = floatArrayOf(
        -1f, -1f,
        1f, -1f,
        1f, 1f,
        -1f, 1f,
    )
    private val quadTex = FloatArray(8)

    private val quadVertexBuffer: FloatBuffer = ByteBuffer.allocateDirect(quadNdc.size * 4)
        .order(ByteOrder.nativeOrder())
        .asFloatBuffer()
        .put(quadNdc)
        .apply { rewind() }

    private val quadTexBuffer: FloatBuffer = ByteBuffer.allocateDirect(quadTex.size * 4)
        .order(ByteOrder.nativeOrder())
        .asFloatBuffer()
        .apply { rewind() }

    private val projectionMatrix = FloatArray(16)
    private val viewMatrix = FloatArray(16)
    private val mvpMatrix = FloatArray(16)

    private val ndcScratch = floatArrayOf(
        -1f, -1f, 1f, -1f, 1f, 1f, -1f, 1f,
    )

    private val pendingTapPx = AtomicReference<Pair<Float, Float>?>(null)
    private val viewNormIn = FloatArray(2)
    private val tapTexOut = FloatArray(2)

    /** Last hit plane (GL thread); narrows plane tint when [tapPlaneSpotlightEnabled]. */
    private var lockedPlane: Plane? = null

    private var tapTexU: Float = 0.5f
    private var tapTexV: Float = 0.5f
    private var hasTapProjection: Boolean = false

    fun queueScreenTap(xPx: Float, yPx: Float) {
        if (viewportWidth > 0 && viewportHeight > 0) {
            pendingTapPx.set(xPx to yPx)
        }
    }

    /** Call on GL thread (e.g. via [GLSurfaceView.queueEvent]). */
    fun clearPaintFocus() {
        pendingTapPx.set(null)
        lockedPlane = null
        hasTapProjection = false
        if (lastPaintFocusNotified) {
            lastPaintFocusNotified = false
            paintFocusActiveListener?.let { lis -> mainHandler.post { lis(false) } }
        }
    }

    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
        GLES20.glClearColor(0f, 0f, 0f, 1f)
        GLES20.glGenTextures(1, cameraTextureId, 0)
        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, cameraTextureId[0])
        GLES20.glTexParameteri(
            GLES11Ext.GL_TEXTURE_EXTERNAL_OES,
            GLES20.GL_TEXTURE_WRAP_S,
            GLES20.GL_CLAMP_TO_EDGE,
        )
        GLES20.glTexParameteri(
            GLES11Ext.GL_TEXTURE_EXTERNAL_OES,
            GLES20.GL_TEXTURE_WRAP_T,
            GLES20.GL_CLAMP_TO_EDGE,
        )
        GLES20.glTexParameteri(
            GLES11Ext.GL_TEXTURE_EXTERNAL_OES,
            GLES20.GL_TEXTURE_MIN_FILTER,
            GLES20.GL_LINEAR,
        )
        GLES20.glTexParameteri(
            GLES11Ext.GL_TEXTURE_EXTERNAL_OES,
            GLES20.GL_TEXTURE_MAG_FILTER,
            GLES20.GL_LINEAR,
        )

        bgProgram = ShaderUtil.loadProgram(BG_VERTEX_SHADER, BG_FRAGMENT_SHADER)
        bgPosAttrib = GLES20.glGetAttribLocation(bgProgram, "a_Position")
        bgTexAttrib = GLES20.glGetAttribLocation(bgProgram, "a_TexCoord")
        bgSamplerUniform = GLES20.glGetUniformLocation(bgProgram, "sTexture")

        planeProgram = ShaderUtil.loadProgram(PLANE_VERTEX_SHADER, PLANE_FRAGMENT_SHADER)
        planeMvpUniform = GLES20.glGetUniformLocation(planeProgram, "u_MvpMatrix")
        planePosAttrib = GLES20.glGetAttribLocation(planeProgram, "a_Position")
        planeColorUniform = GLES20.glGetUniformLocation(planeProgram, "u_Color")

    }

    override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
        viewportWidth = width
        viewportHeight = height
        GLES20.glViewport(0, 0, width, height)
        lastDisplayRotation = -1
        lastDisplayWidth = -1
        lastDisplayHeight = -1
    }

    private fun displayRotation(): Int {
        val act = context.findActivity()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            act?.display?.rotation?.let { return it }
            context.display?.rotation?.let { return it }
        }
        @Suppress("DEPRECATION")
        return (act?.windowManager ?: context.getSystemService(Context.WINDOW_SERVICE) as WindowManager)
            .defaultDisplay
            .rotation
    }

    private fun Context.findActivity(): Activity? {
        var c: Context = this
        while (c is ContextWrapper) {
            if (c is Activity) return c
            c = c.baseContext
        }
        return null
    }

    override fun onDrawFrame(gl: GL10?) {
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT or GLES20.GL_DEPTH_BUFFER_BIT)

        val session = session ?: return
        if (!sessionRunning) return
        if (cameraTextureId[0] == 0 || viewportWidth <= 0 || viewportHeight <= 0) return

        val rot = displayRotation()
        if (rot != lastDisplayRotation || viewportWidth != lastDisplayWidth || viewportHeight != lastDisplayHeight) {
            session.setDisplayGeometry(rot, viewportWidth, viewportHeight)
            lastDisplayRotation = rot
            lastDisplayWidth = viewportWidth
            lastDisplayHeight = viewportHeight
        }

        session.setCameraTextureName(cameraTextureId[0])

        val frame = try {
            session.update()
        } catch (_: IllegalStateException) {
            return
        } catch (_: UnavailableException) {
            return
        } catch (_: CameraNotAvailableException) {
            return
        } catch (_: Throwable) {
            return
        }

        val camState = frame.camera.trackingState
        if (camState != TrackingState.TRACKING && camState != TrackingState.PAUSED) {
            drawBackgroundOnly(frame)
            return
        }

        processPendingTap(frame)
        validateLockedPlane(session)
        notifyPaintFocusIfChanged()

        if (!transformQuadTexCoords(frame)) {
            quadTex[0] = 0f
            quadTex[1] = 1f
            quadTex[2] = 1f
            quadTex[3] = 1f
            quadTex[4] = 1f
            quadTex[5] = 0f
            quadTex[6] = 0f
            quadTex[7] = 0f
            quadTexBuffer.clear()
            quadTexBuffer.put(quadTex)
            quadTexBuffer.rewind()
        }

        quadVertexBuffer.rewind()

        GLES20.glDisable(GLES20.GL_DEPTH_TEST)
        GLES20.glDepthMask(false)
        GLES20.glUseProgram(bgProgram)
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0)
        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, cameraTextureId[0])
        GLES20.glUniform1i(bgSamplerUniform, 0)
        GLES20.glVertexAttribPointer(bgPosAttrib, 2, GLES20.GL_FLOAT, false, 0, quadVertexBuffer)
        GLES20.glEnableVertexAttribArray(bgPosAttrib)
        GLES20.glVertexAttribPointer(bgTexAttrib, 2, GLES20.GL_FLOAT, false, 0, quadTexBuffer)
        GLES20.glEnableVertexAttribArray(bgTexAttrib)
        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_FAN, 0, 4)
        GLES20.glDisableVertexAttribArray(bgPosAttrib)
        GLES20.glDisableVertexAttribArray(bgTexAttrib)

        GLES20.glDepthMask(true)
        GLES20.glEnable(GLES20.GL_DEPTH_TEST)
        GLES20.glDepthFunc(GLES20.GL_LEQUAL)

        val camera = frame.camera
        camera.getProjectionMatrix(projectionMatrix, 0, 0.1f, 100f)
        camera.getViewMatrix(viewMatrix, 0)
        Matrix.multiplyMM(mvpMatrix, 0, projectionMatrix, 0, viewMatrix, 0)

        val filter = surfaceFilter
        val wallTh = wallNormalThreshold

        GLES20.glEnable(GLES20.GL_BLEND)
        GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA)
        GLES20.glDisable(GLES20.GL_CULL_FACE)

        GLES20.glUseProgram(planeProgram)
        GLES20.glUniformMatrix4fv(planeMvpUniform, 1, false, mvpMatrix, 0)

        val drawablePlanes = session.getAllTrackables(Plane::class.java)
            .filter { it.isDrawableForPaintPreview() && it.matchesFilter(filter, wallTh) }

        val spotlight = lockedPlane?.takeIf { lp ->
            tapPlaneSpotlightEnabled &&
                lp.subsumedBy == null &&
                lp.isDrawableForPaintPreview() &&
                drawablePlanes.any { it === lp }
        }

        val planeMeshes = drawablePlanes
            .filter { spotlight == null || it === spotlight }
            .mapNotNull { plane -> buildPlaneMesh(plane)?.let { mesh -> plane to mesh } }

        for ((plane, mesh) in planeMeshes) {
            val pa = paintArgbForPlane(plane, wallTh)
            val tint = unpackArgb(pa)
            GLES20.glUniform4f(planeColorUniform, tint.r, tint.g, tint.b, tint.a)
            drawPlaneFill(mesh)
        }

        if (drawSurfaceOutlines) {
            GLES20.glDepthMask(false)
            for ((_, mesh) in planeMeshes) {
                drawPlaneOutline(mesh)
            }
            GLES20.glLineWidth(1f)
            GLES20.glDepthMask(true)
        }

        GLES20.glDisable(GLES20.GL_BLEND)
    }

    private data class RgbaComponents(val r: Float, val g: Float, val b: Float, val a: Float)

    private fun unpackArgb(colorArgb: Int): RgbaComponents {
        val r = ((colorArgb shr 16) and 0xff) / 255f
        val g = ((colorArgb shr 8) and 0xff) / 255f
        val b = (colorArgb and 0xff) / 255f
        val aRaw = ((colorArgb shr 24) and 0xff) / 255f
        val a = if (aRaw < 0.02f) 0.45f else aRaw
        return RgbaComponents(r, g, b, a)
    }

    private fun paintArgbForPlane(plane: Plane, wallTh: Float): Int {
        if (!studioSurfaceColors) return paintColorArgb
        return when (plane.surfacePaintKind(wallTh)) {
            SurfacePaintKind.WALL -> wallPaintArgb
            SurfacePaintKind.FLOOR -> floorPaintArgb
            SurfacePaintKind.CEILING -> ceilingPaintArgb
            SurfacePaintKind.ACCENT -> accentPaintArgb
            SurfacePaintKind.UNKNOWN -> paintColorArgb
        }
    }

    private fun processPendingTap(frame: Frame) {
        val tap = pendingTapPx.getAndSet(null) ?: return
        val (x, y) = tap
        val w = viewportWidth
        val h = viewportHeight
        if (w <= 0 || h <= 0) return
        val hits = try {
            frame.hitTest(x, y)
        } catch (_: Throwable) {
            emptyList()
        }
        if (hits.isEmpty()) return

        var closestHit: HitResult? = null
        for (h in hits) {
            val pl = h.trackable as? Plane ?: continue
            if (!pl.isDrawableForPaintPreview()) continue
            val cur = closestHit
            if (cur == null || h.distance < cur.distance) closestHit = h
        }
        val bestPlane = closestHit?.trackable as? Plane
        if (tapPlaneSpotlightEnabled && bestPlane != null) {
            lockedPlane = bestPlane
        }

        viewNormIn[0] = (x / w.toFloat()).coerceIn(0f, 1f)
        viewNormIn[1] = (y / h.toFloat()).coerceIn(0f, 1f)
        hasTapProjection = try {
            frame.transformCoordinates2d(
                Coordinates2d.VIEW_NORMALIZED,
                viewNormIn,
                Coordinates2d.TEXTURE_NORMALIZED,
                tapTexOut,
            )
            tapTexU = tapTexOut[0]
            tapTexV = tapTexOut[1]
            true
        } catch (_: Throwable) {
            false
        }
    }

    private fun validateLockedPlane(session: Session) {
        val lp = lockedPlane ?: return
        if (lp.subsumedBy != null || !lp.isDrawableForPaintPreview()) {
            lockedPlane = null
            return
        }
        val alive = session.getAllTrackables(Plane::class.java).any { it === lp }
        if (!alive) lockedPlane = null
    }

    private fun paintFocusActive(): Boolean =
        hasTapProjection || (tapPlaneSpotlightEnabled && lockedPlane != null)

    private fun notifyPaintFocusIfChanged() {
        val active = paintFocusActive()
        if (active == lastPaintFocusNotified) return
        lastPaintFocusNotified = active
        paintFocusActiveListener?.let { lis -> mainHandler.post { lis(active) } }
    }

    private fun transformQuadTexCoords(frame: Frame): Boolean =
        try {
            frame.transformCoordinates2d(
                Coordinates2d.OPENGL_NORMALIZED_DEVICE_COORDINATES,
                ndcScratch,
                Coordinates2d.TEXTURE_NORMALIZED,
                quadTex,
            )
            quadTexBuffer.clear()
            quadTexBuffer.put(quadTex)
            quadTexBuffer.rewind()
            true
        } catch (_: Throwable) {
            false
        }

    private fun drawBackgroundOnly(frame: Frame) {
        if (!transformQuadTexCoords(frame)) {
            quadTex[0] = 0f
            quadTex[1] = 1f
            quadTex[2] = 1f
            quadTex[3] = 1f
            quadTex[4] = 1f
            quadTex[5] = 0f
            quadTex[6] = 0f
            quadTex[7] = 0f
            quadTexBuffer.clear()
            quadTexBuffer.put(quadTex)
            quadTexBuffer.rewind()
        }
        quadVertexBuffer.rewind()

        GLES20.glDisable(GLES20.GL_DEPTH_TEST)
        GLES20.glUseProgram(bgProgram)
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0)
        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, cameraTextureId[0])
        GLES20.glUniform1i(bgSamplerUniform, 0)
        GLES20.glVertexAttribPointer(bgPosAttrib, 2, GLES20.GL_FLOAT, false, 0, quadVertexBuffer)
        GLES20.glEnableVertexAttribArray(bgPosAttrib)
        GLES20.glVertexAttribPointer(bgTexAttrib, 2, GLES20.GL_FLOAT, false, 0, quadTexBuffer)
        GLES20.glEnableVertexAttribArray(bgTexAttrib)
        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_FAN, 0, 4)
        GLES20.glDisableVertexAttribArray(bgPosAttrib)
        GLES20.glDisableVertexAttribArray(bgTexAttrib)
    }

    private fun drawPlaneFill(mesh: PlaneMesh) {
        val buf = ByteBuffer.allocateDirect(mesh.fillVertices.size * 4)
            .order(ByteOrder.nativeOrder())
            .asFloatBuffer()
            .put(mesh.fillVertices)
            .apply { rewind() }

        GLES20.glVertexAttribPointer(planePosAttrib, 3, GLES20.GL_FLOAT, false, 0, buf)
        GLES20.glEnableVertexAttribArray(planePosAttrib)
        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_FAN, 0, mesh.vertexCount)
        GLES20.glDisableVertexAttribArray(planePosAttrib)
    }

    private fun drawPlaneOutline(mesh: PlaneMesh) {
        if (mesh.outlineVertices.size < 9) return

        GLES20.glUniform4f(planeColorUniform, 1f, 1f, 1f, 0.92f)
        GLES20.glLineWidth(4f)

        val buf = ByteBuffer.allocateDirect(mesh.outlineVertices.size * 4)
            .order(ByteOrder.nativeOrder())
            .asFloatBuffer()
            .put(mesh.outlineVertices)
            .apply { rewind() }

        GLES20.glVertexAttribPointer(planePosAttrib, 3, GLES20.GL_FLOAT, false, 0, buf)
        GLES20.glEnableVertexAttribArray(planePosAttrib)
        GLES20.glDrawArrays(GLES20.GL_LINE_LOOP, 0, mesh.outlineVertexCount)
        GLES20.glDisableVertexAttribArray(planePosAttrib)
    }

    private data class PlaneMesh(
        val fillVertices: FloatArray,
        val vertexCount: Int,
        val outlineVertices: FloatArray,
        val outlineVertexCount: Int,
    )

    private fun buildPlaneMesh(plane: Plane): PlaneMesh? {
        val poly = plane.polygon.duplicate()
        poly.rewind()
        val remaining = poly.remaining()
        if (remaining < 6) return null

        val coords = FloatArray(remaining)
        poly.get(coords)
        val n = coords.size / 2
        if (n < 3) return null

        var cx = 0f
        var cz = 0f
        for (i in 0 until n) {
            cx += coords[i * 2]
            cz += coords[i * 2 + 1]
        }
        cx /= n
        cz /= n

        val centerWorld = FloatArray(3)
        val cornerWorld = FloatArray(3)
        val local = FloatArray(3)
        local[0] = cx
        local[1] = 0f
        local[2] = cz
        plane.centerPose.transformPoint(local, 0, centerWorld, 0)

        val fill = FloatArray((n + 1) * 3)
        var idx = 0
        fill[idx++] = centerWorld[0]
        fill[idx++] = centerWorld[1]
        fill[idx++] = centerWorld[2]
        val outline = FloatArray(n * 3)
        var o = 0
        for (i in 0 until n) {
            local[0] = coords[i * 2]
            local[1] = 0f
            local[2] = coords[i * 2 + 1]
            plane.centerPose.transformPoint(local, 0, cornerWorld, 0)
            fill[idx++] = cornerWorld[0]
            fill[idx++] = cornerWorld[1]
            fill[idx++] = cornerWorld[2]
            outline[o++] = cornerWorld[0]
            outline[o++] = cornerWorld[1]
            outline[o++] = cornerWorld[2]
        }

        return PlaneMesh(
            fillVertices = fill,
            vertexCount = n + 1,
            outlineVertices = outline,
            outlineVertexCount = n,
        )
    }

    fun shutdownSegmentation() = Unit

    fun markSessionRunning(running: Boolean) {
        sessionRunning = running
    }

    fun releaseGlResources() {
        if (cameraTextureId[0] != 0) {
            GLES20.glDeleteTextures(1, cameraTextureId, 0)
            cameraTextureId[0] = 0
        }
        if (bgProgram != 0) {
            GLES20.glDeleteProgram(bgProgram)
            bgProgram = 0
        }
        if (planeProgram != 0) {
            GLES20.glDeleteProgram(planeProgram)
            planeProgram = 0
        }
    }

    companion object {
        private const val BG_VERTEX_SHADER = """
            attribute vec4 a_Position;
            attribute vec2 a_TexCoord;
            varying vec2 v_TexCoord;
            void main() {
              v_TexCoord = a_TexCoord;
              gl_Position = a_Position;
            }
        """

        private const val BG_FRAGMENT_SHADER = """
            #extension GL_OES_EGL_image_external : require
            precision mediump float;
            uniform samplerExternalOES sTexture;
            varying vec2 v_TexCoord;
            void main() {
              gl_FragColor = texture2D(sTexture, v_TexCoord);
            }
        """

        private const val PLANE_VERTEX_SHADER = """
            uniform mat4 u_MvpMatrix;
            attribute vec4 a_Position;
            void main() {
              gl_Position = u_MvpMatrix * a_Position;
            }
        """

        private const val PLANE_FRAGMENT_SHADER = """
            precision mediump float;
            uniform vec4 u_Color;
            void main() {
              gl_FragColor = u_Color;
            }
        """
    }
}
