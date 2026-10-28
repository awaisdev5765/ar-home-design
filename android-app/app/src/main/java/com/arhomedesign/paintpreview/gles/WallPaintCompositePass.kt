package com.arhomedesign.paintpreview.gles

import android.opengl.GLES11Ext
import android.opengl.GLES20
import java.nio.ByteBuffer

/**
 * Fullscreen pass: camera [samplerExternalOES] + wall mask + optional depth, with multiply/overlay
 * tint and light mask edge softening in the fragment shader.
 */
class WallPaintCompositePass {

    private var program = 0
    private var aPos = 0
    private var aTex = 0
    private var uCamera = 0
    private var uMask = 0
    private var uDepth = 0
    private var uTexToImgIx = 0
    private var uTexToImgIy = 0
    private var uImgSize = 0
    private var uDepthSize = 0
    private var uMaskSize = 0
    private var uPaintRgb = 0
    private var uPaintStrength = 0
    private var uBlendMultiply = 0
    private var uHasDepth = 0
    private var uWallDepthMm = 0
    private var uDepthUnpackMm = 0
    private var uOcclusionBandMm = 0
    private var uTapFocus = 0
    private var uTapUv = 0
    private var uTapSigma = 0

    private val maskTex = IntArray(1)
    private val depthTex = IntArray(1)
    private var maskW = 0
    private var maskH = 0
    private var depthW = 0
    private var depthH = 0
    var hasMaskTexture: Boolean = false
        private set

    /** Default unpack alignment is 4; 1-byte luminance rows (e.g. width 257) must be tightly packed or textures shear into vertical stripes. */
    private val unpackAlignScratch = IntArray(1)

    private inline fun withUnpackAlignment1(upload: () -> Unit) {
        GLES20.glGetIntegerv(GLES20.GL_UNPACK_ALIGNMENT, unpackAlignScratch, 0)
        GLES20.glPixelStorei(GLES20.GL_UNPACK_ALIGNMENT, 1)
        try {
            upload()
        } finally {
            GLES20.glPixelStorei(GLES20.GL_UNPACK_ALIGNMENT, unpackAlignScratch[0])
        }
    }

    fun onGlContextCreated() {
        program = ShaderUtil.loadProgram(VERTEX_SHADER, FRAGMENT_SHADER)
        if (program == 0) return
        aPos = GLES20.glGetAttribLocation(program, "a_Position")
        aTex = GLES20.glGetAttribLocation(program, "a_TexCoord")
        uCamera = GLES20.glGetUniformLocation(program, "u_Camera")
        uMask = GLES20.glGetUniformLocation(program, "u_Mask")
        uDepth = GLES20.glGetUniformLocation(program, "u_Depth")
        uTexToImgIx = GLES20.glGetUniformLocation(program, "u_TexToImgIx")
        uTexToImgIy = GLES20.glGetUniformLocation(program, "u_TexToImgIy")
        uImgSize = GLES20.glGetUniformLocation(program, "u_ImgSize")
        uDepthSize = GLES20.glGetUniformLocation(program, "u_DepthSize")
        uMaskSize = GLES20.glGetUniformLocation(program, "u_MaskSize")
        uPaintRgb = GLES20.glGetUniformLocation(program, "u_PaintRgb")
        uPaintStrength = GLES20.glGetUniformLocation(program, "u_PaintStrength")
        uBlendMultiply = GLES20.glGetUniformLocation(program, "u_BlendMultiply")
        uHasDepth = GLES20.glGetUniformLocation(program, "u_HasDepth")
        uWallDepthMm = GLES20.glGetUniformLocation(program, "u_WallDepthMm")
        uDepthUnpackMm = GLES20.glGetUniformLocation(program, "u_DepthUnpackMm")
        uOcclusionBandMm = GLES20.glGetUniformLocation(program, "u_OcclusionBandMm")
        uTapFocus = GLES20.glGetUniformLocation(program, "u_TapFocus")
        uTapUv = GLES20.glGetUniformLocation(program, "u_TapUv")
        uTapSigma = GLES20.glGetUniformLocation(program, "u_TapSigma")

        GLES20.glGenTextures(1, maskTex, 0)
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, maskTex[0])
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE)
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE)
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR)
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR)

        GLES20.glGenTextures(1, depthTex, 0)
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, depthTex[0])
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE)
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE)
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR)
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR)
    }

    fun uploadMaskLuminance8(buffer: ByteBuffer, w: Int, h: Int) {
        if (w <= 0 || h <= 0) return
        buffer.rewind()
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, maskTex[0])
        withUnpackAlignment1 {
            if (hasMaskTexture && w == maskW && h == maskH && maskTex[0] != 0) {
                GLES20.glTexSubImage2D(
                    GLES20.GL_TEXTURE_2D,
                    0,
                    0,
                    0,
                    w,
                    h,
                    GLES20.GL_LUMINANCE,
                    GLES20.GL_UNSIGNED_BYTE,
                    buffer,
                )
            } else {
                GLES20.glTexImage2D(
                    GLES20.GL_TEXTURE_2D,
                    0,
                    GLES20.GL_LUMINANCE,
                    w,
                    h,
                    0,
                    GLES20.GL_LUMINANCE,
                    GLES20.GL_UNSIGNED_BYTE,
                    buffer,
                )
            }
        }
        maskW = w
        maskH = h
        hasMaskTexture = true
    }

    fun uploadDepthR8(buffer: ByteBuffer, w: Int, h: Int) {
        if (w <= 0 || h <= 0) return
        buffer.rewind()
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, depthTex[0])
        withUnpackAlignment1 {
            if (depthW == w && depthH == h && depthTex[0] != 0) {
                GLES20.glTexSubImage2D(
                    GLES20.GL_TEXTURE_2D,
                    0,
                    0,
                    0,
                    w,
                    h,
                    GLES20.GL_LUMINANCE,
                    GLES20.GL_UNSIGNED_BYTE,
                    buffer,
                )
            } else {
                GLES20.glTexImage2D(
                    GLES20.GL_TEXTURE_2D,
                    0,
                    GLES20.GL_LUMINANCE,
                    w,
                    h,
                    0,
                    GLES20.GL_LUMINANCE,
                    GLES20.GL_UNSIGNED_BYTE,
                    buffer,
                )
            }
        }
        depthW = w
        depthH = h
    }

    fun draw(
        externalCameraTexId: Int,
        quadVertexBuffer: java.nio.FloatBuffer,
        quadTexBuffer: java.nio.FloatBuffer,
        texToImgIx: FloatArray,
        texToImgIy: FloatArray,
        imageW: Float,
        imageH: Float,
        wallDepthMm: Float,
        hasDepthTexture: Boolean,
        paintR: Float,
        paintG: Float,
        paintB: Float,
        paintStrength: Float,
        blendMultiply: Boolean,
        tapFocusEnabled: Boolean,
        tapTexU: Float,
        tapTexV: Float,
        tapSigma: Float,
    ) {
        if (program == 0 || !hasMaskTexture || maskW <= 0 || maskH <= 0) return

        GLES20.glUseProgram(program)
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0)
        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, externalCameraTexId)
        GLES20.glUniform1i(uCamera, 0)

        GLES20.glActiveTexture(GLES20.GL_TEXTURE1)
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, maskTex[0])
        GLES20.glUniform1i(uMask, 1)

        GLES20.glActiveTexture(GLES20.GL_TEXTURE2)
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, depthTex[0])
        GLES20.glUniform1i(uDepth, 2)

        GLES20.glUniform3fv(uTexToImgIx, 1, texToImgIx, 0)
        GLES20.glUniform3fv(uTexToImgIy, 1, texToImgIy, 0)
        GLES20.glUniform2f(uImgSize, imageW, imageH)
        GLES20.glUniform2f(uDepthSize, depthW.toFloat(), depthH.toFloat())
        GLES20.glUniform2f(uMaskSize, maskW.toFloat(), maskH.toFloat())
        GLES20.glUniform3f(uPaintRgb, paintR, paintG, paintB)
        GLES20.glUniform1f(uPaintStrength, paintStrength)
        GLES20.glUniform1f(uBlendMultiply, if (blendMultiply) 1f else 0f)
        val hd = hasDepthTexture && depthW > 0 && depthH > 0
        GLES20.glUniform1f(uHasDepth, if (hd) 1f else 0f)
        GLES20.glUniform1f(uWallDepthMm, wallDepthMm)
        GLES20.glUniform1f(uDepthUnpackMm, 12_000f)
        GLES20.glUniform1f(uOcclusionBandMm, 200f)
        GLES20.glUniform1f(uTapFocus, if (tapFocusEnabled) 1f else 0f)
        GLES20.glUniform2f(uTapUv, tapTexU, tapTexV)
        GLES20.glUniform1f(uTapSigma, tapSigma.coerceIn(0.04f, 0.55f))

        quadVertexBuffer.rewind()
        quadTexBuffer.rewind()
        GLES20.glVertexAttribPointer(aPos, 2, GLES20.GL_FLOAT, false, 0, quadVertexBuffer)
        GLES20.glEnableVertexAttribArray(aPos)
        GLES20.glVertexAttribPointer(aTex, 2, GLES20.GL_FLOAT, false, 0, quadTexBuffer)
        GLES20.glEnableVertexAttribArray(aTex)
        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_FAN, 0, 4)
        GLES20.glDisableVertexAttribArray(aPos)
        GLES20.glDisableVertexAttribArray(aTex)

        GLES20.glActiveTexture(GLES20.GL_TEXTURE0)
    }

    fun releaseGl() {
        if (maskTex[0] != 0) {
            GLES20.glDeleteTextures(1, maskTex, 0)
            maskTex[0] = 0
        }
        if (depthTex[0] != 0) {
            GLES20.glDeleteTextures(1, depthTex, 0)
            depthTex[0] = 0
        }
        if (program != 0) {
            GLES20.glDeleteProgram(program)
            program = 0
        }
        hasMaskTexture = false
        maskW = 0
        maskH = 0
        depthW = 0
        depthH = 0
    }

    companion object {
        private const val VERTEX_SHADER = """
            attribute vec4 a_Position;
            attribute vec2 a_TexCoord;
            varying vec2 v_TexCoord;
            void main() {
              v_TexCoord = a_TexCoord;
              gl_Position = a_Position;
            }
        """

        private const val FRAGMENT_SHADER = """
            #extension GL_OES_EGL_image_external : require
            precision mediump float;
            uniform samplerExternalOES u_Camera;
            uniform sampler2D u_Mask;
            uniform sampler2D u_Depth;
            uniform vec3 u_TexToImgIx;
            uniform vec3 u_TexToImgIy;
            uniform vec2 u_ImgSize;
            uniform vec2 u_DepthSize;
            uniform vec2 u_MaskSize;
            uniform vec3 u_PaintRgb;
            uniform float u_PaintStrength;
            uniform float u_BlendMultiply;
            uniform float u_HasDepth;
            uniform float u_WallDepthMm;
            uniform float u_DepthUnpackMm;
            uniform float u_OcclusionBandMm;
            uniform float u_TapFocus;
            uniform vec2 u_TapUv;
            uniform float u_TapSigma;
            varying vec2 v_TexCoord;

            float maskSoft(vec2 uv) {
              if (uv.x < 0.001 || uv.x > 0.999 || uv.y < 0.001 || uv.y > 0.999) {
                return texture2D(u_Mask, clamp(uv, 0.001, 0.999)).r;
              }
              vec2 px = 1.0 / u_MaskSize;
              float s = 0.0;
              s += texture2D(u_Mask, uv).r;
              s += texture2D(u_Mask, uv + vec2(px.x, 0.0)).r;
              s += texture2D(u_Mask, uv - vec2(px.x, 0.0)).r;
              s += texture2D(u_Mask, uv + vec2(0.0, px.y)).r;
              s += texture2D(u_Mask, uv - vec2(0.0, px.y)).r;
              return s * 0.2;
            }

            void main() {
              vec4 cam = texture2D(u_Camera, v_TexCoord);
              float ix = u_TexToImgIx.x * v_TexCoord.x + u_TexToImgIx.y * v_TexCoord.y + u_TexToImgIx.z;
              float iy = u_TexToImgIy.x * v_TexCoord.x + u_TexToImgIy.y * v_TexCoord.y + u_TexToImgIy.z;
              float side = min(u_ImgSize.x, u_ImgSize.y);
              float dx = (u_ImgSize.x - side) * 0.5;
              float dy = (u_ImgSize.y - side) * 0.5;
              vec2 muv = vec2((ix - dx) / side, (iy - dy) / side);
              float mRaw = maskSoft(muv);
              // Suppress speckle flicker; keeps edges reasonably soft
              float m = smoothstep(0.07, 0.38, mRaw);

              vec2 dUv = vec2((ix + 0.5) / u_ImgSize.x, (iy + 0.5) / u_ImgSize.y);
              float zNorm = texture2D(u_Depth, dUv).r;
              float zMm = zNorm * u_DepthUnpackMm;
              if (u_HasDepth > 0.5 && u_WallDepthMm > 150.0 && zMm > 80.0 && zMm < u_WallDepthMm - u_OcclusionBandMm) {
                m = 0.0;
              }

              float tapRoi = 1.0;
              if (u_TapFocus > 0.5) {
                vec2 d = v_TexCoord - u_TapUv;
                float dist2 = dot(d, d);
                tapRoi = exp(-dist2 / (2.0 * u_TapSigma * u_TapSigma + 1e-6));
              }
              float a = m * u_PaintStrength * tapRoi;
              vec3 base = cam.rgb;
              vec3 paint = u_PaintRgb;
              vec3 mult = base * paint;
              vec3 low = 2.0 * base * paint;
              vec3 high = 1.0 - 2.0 * (1.0 - base) * (1.0 - paint);
              vec3 ovl = mix(low, high, step(0.5, base));
              vec3 tinted = mix(ovl, mult, step(0.5, u_BlendMultiply));
              gl_FragColor = vec4(mix(base, tinted, a), 1.0);
            }
        """
    }
}
