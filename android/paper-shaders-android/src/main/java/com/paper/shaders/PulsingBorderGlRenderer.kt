package com.paper.shaders

import android.opengl.GLES30
import android.opengl.GLSurfaceView
import android.opengl.GLUtils
import android.os.SystemClock
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10
import kotlin.math.min

internal class PulsingBorderGlRenderer : GLSurfaceView.Renderer {
  private var programId = 0
  private var positionAttribute = 0
  private var noiseTextureId = 0
  private var viewportWidth = 1
  private var viewportHeight = 1
  private val uniformLocations = mutableMapOf<String, Int>()

  private var uniformState = PulsingBorderUniformState.fromParams(PulsingBorderParams(), 1f)
  private var surfaceCreatedAtMs = 0L

  private val quadBuffer: FloatBuffer = ByteBuffer
    .allocateDirect(QUAD_VERTICES.size * 4)
    .order(ByteOrder.nativeOrder())
    .asFloatBuffer()
    .apply {
      put(QUAD_VERTICES)
      position(0)
    }

  fun setUniformState(state: PulsingBorderUniformState) {
    uniformState = state
  }

  override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
    GLES30.glDisable(GLES30.GL_DEPTH_TEST)

    uniformLocations.clear()
    programId = createProgram(PULSING_BORDER_VERTEX_SHADER, PULSING_BORDER_FRAGMENT_SHADER)
    GLES30.glUseProgram(programId)
    positionAttribute = GLES30.glGetAttribLocation(programId, "a_position")

    val noise = NoiseTextureLoader.load()
    noiseTextureId = createTexture(noise)
    noise.bitmap.recycle()

    GLES30.glActiveTexture(GLES30.GL_TEXTURE0)
    GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, noiseTextureId)
    GLES30.glUniform1i(uniform("u_noiseTexture"), 0)

    surfaceCreatedAtMs = SystemClock.elapsedRealtime()
  }

  override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
    viewportWidth = width.coerceAtLeast(1)
    viewportHeight = height.coerceAtLeast(1)
    GLES30.glViewport(0, 0, viewportWidth, viewportHeight)
  }

  override fun onDrawFrame(gl: GL10?) {
    GLES30.glClear(GLES30.GL_COLOR_BUFFER_BIT)
    GLES30.glUseProgram(programId)

    val state = uniformState
    val elapsedSeconds = (SystemClock.elapsedRealtime() - surfaceCreatedAtMs) / 1000f
    val time = state.frame + elapsedSeconds * state.speed

    GLES30.glActiveTexture(GLES30.GL_TEXTURE0)
    GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, noiseTextureId)

    GLES30.glUniform1f(uniform("u_time"), time)
    GLES30.glUniform2f(uniform("u_resolution"), viewportWidth.toFloat(), viewportHeight.toFloat())
    GLES30.glUniform1f(uniform("u_pixelRatio"), state.pixelRatio)

    GLES30.glUniform4fv(uniform("u_colorBack"), 1, state.colorBack, 0)
    GLES30.glUniform4fv(uniform("u_colors"), 5, state.colors, 0)
    GLES30.glUniform1f(uniform("u_colorsCount"), state.colorsCount)

    GLES30.glUniform1f(uniform("u_roundness"), state.roundness)
    GLES30.glUniform1f(uniform("u_thickness"), state.thickness)
    GLES30.glUniform1f(uniform("u_marginLeft"), state.marginLeft)
    GLES30.glUniform1f(uniform("u_marginRight"), state.marginRight)
    GLES30.glUniform1f(uniform("u_marginTop"), state.marginTop)
    GLES30.glUniform1f(uniform("u_marginBottom"), state.marginBottom)
    GLES30.glUniform1f(uniform("u_aspectRatio"), state.aspectRatio)
    GLES30.glUniform1f(uniform("u_softness"), state.softness)
    GLES30.glUniform1f(uniform("u_intensity"), state.intensity)
    GLES30.glUniform1f(uniform("u_bloom"), state.bloom)
    GLES30.glUniform1f(uniform("u_spots"), state.spots)
    GLES30.glUniform1f(uniform("u_spotSize"), state.spotSize)
    GLES30.glUniform1f(uniform("u_pulse"), state.pulse)
    GLES30.glUniform1f(uniform("u_smoke"), state.smoke)
    GLES30.glUniform1f(uniform("u_smokeSize"), state.smokeSize)

    GLES30.glUniform1f(uniform("u_fit"), state.fit)
    GLES30.glUniform1f(uniform("u_scale"), state.scale)
    GLES30.glUniform1f(uniform("u_rotation"), state.rotation)
    GLES30.glUniform1f(uniform("u_offsetX"), state.offsetX)
    GLES30.glUniform1f(uniform("u_offsetY"), state.offsetY)
    GLES30.glUniform1f(uniform("u_originX"), state.originX)
    GLES30.glUniform1f(uniform("u_originY"), state.originY)
    GLES30.glUniform1f(uniform("u_worldWidth"), state.worldWidth)
    GLES30.glUniform1f(uniform("u_worldHeight"), state.worldHeight)

    quadBuffer.position(0)
    GLES30.glEnableVertexAttribArray(positionAttribute)
    GLES30.glVertexAttribPointer(positionAttribute, 2, GLES30.GL_FLOAT, false, 0, quadBuffer)
    GLES30.glDrawArrays(GLES30.GL_TRIANGLES, 0, 6)
    GLES30.glDisableVertexAttribArray(positionAttribute)
  }

  fun release() {
    if (noiseTextureId != 0) {
      GLES30.glDeleteTextures(1, intArrayOf(noiseTextureId), 0)
      noiseTextureId = 0
    }
    if (programId != 0) {
      GLES30.glDeleteProgram(programId)
      programId = 0
    }
  }

  private fun uniform(name: String): Int {
    return uniformLocations.getOrPut(name) {
      GLES30.glGetUniformLocation(programId, name)
    }
  }

  private fun createTexture(noise: NoiseTexture): Int {
    val ids = IntArray(1)
    GLES30.glGenTextures(1, ids, 0)
    val id = ids[0]

    GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, id)
    GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_MIN_FILTER, GLES30.GL_LINEAR)
    GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_MAG_FILTER, GLES30.GL_LINEAR)
    GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_WRAP_S, GLES30.GL_REPEAT)
    GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_WRAP_T, GLES30.GL_REPEAT)

    GLUtils.texImage2D(GLES30.GL_TEXTURE_2D, 0, noise.bitmap, 0)
    return id
  }

  private fun createProgram(vertexShaderSource: String, fragmentShaderSource: String): Int {
    val vertexShader = compileShader(GLES30.GL_VERTEX_SHADER, vertexShaderSource)
    val fragmentShader = compileShader(GLES30.GL_FRAGMENT_SHADER, fragmentShaderSource)

    val program = GLES30.glCreateProgram()
    GLES30.glAttachShader(program, vertexShader)
    GLES30.glAttachShader(program, fragmentShader)
    GLES30.glLinkProgram(program)

    val linkStatus = IntArray(1)
    GLES30.glGetProgramiv(program, GLES30.GL_LINK_STATUS, linkStatus, 0)
    if (linkStatus[0] == 0) {
      val log = GLES30.glGetProgramInfoLog(program)
      GLES30.glDeleteShader(vertexShader)
      GLES30.glDeleteShader(fragmentShader)
      GLES30.glDeleteProgram(program)
      throw IllegalStateException("Failed to link GLES program: $log")
    }

    GLES30.glDeleteShader(vertexShader)
    GLES30.glDeleteShader(fragmentShader)
    return program
  }

  private fun compileShader(type: Int, source: String): Int {
    val shader = GLES30.glCreateShader(type)
    GLES30.glShaderSource(shader, source)
    GLES30.glCompileShader(shader)

    val compileStatus = IntArray(1)
    GLES30.glGetShaderiv(shader, GLES30.GL_COMPILE_STATUS, compileStatus, 0)
    if (compileStatus[0] == 0) {
      val log = GLES30.glGetShaderInfoLog(shader)
      GLES30.glDeleteShader(shader)
      throw IllegalStateException("Failed to compile shader: $log")
    }

    return shader
  }

  companion object {
    private val QUAD_VERTICES = floatArrayOf(
      -1f, -1f,
      1f, -1f,
      -1f, 1f,
      -1f, 1f,
      1f, -1f,
      1f, 1f
    )
  }
}

internal data class PulsingBorderUniformState(
  val speed: Float,
  val frame: Float,
  val pixelRatio: Float,
  val colorBack: FloatArray,
  val colors: FloatArray,
  val colorsCount: Float,
  val roundness: Float,
  val thickness: Float,
  val marginLeft: Float,
  val marginRight: Float,
  val marginTop: Float,
  val marginBottom: Float,
  val aspectRatio: Float,
  val softness: Float,
  val intensity: Float,
  val bloom: Float,
  val spots: Float,
  val spotSize: Float,
  val pulse: Float,
  val smoke: Float,
  val smokeSize: Float,
  val fit: Float,
  val scale: Float,
  val rotation: Float,
  val originX: Float,
  val originY: Float,
  val offsetX: Float,
  val offsetY: Float,
  val worldWidth: Float,
  val worldHeight: Float
) {
  companion object {
    fun fromParams(params: PulsingBorderParams, pixelRatio: Float): PulsingBorderUniformState {
      val margins = params.resolveMargins()
      return PulsingBorderUniformState(
        speed = params.speed,
        frame = params.frame,
        pixelRatio = pixelRatio,
        colorBack = ColorParser.parse(params.colorBack),
        colors = packColors(params.colors),
        colorsCount = min(params.colors.size, 5).toFloat(),
        roundness = params.roundness,
        thickness = params.thickness,
        marginLeft = margins.left,
        marginRight = margins.right,
        marginTop = margins.top,
        marginBottom = margins.bottom,
        aspectRatio = params.aspectRatio.value,
        softness = params.softness,
        intensity = params.intensity,
        bloom = params.bloom,
        spots = params.spots.toFloat(),
        spotSize = params.spotSize,
        pulse = params.pulse,
        smoke = params.smoke,
        smokeSize = params.smokeSize,
        fit = params.fit.value,
        scale = params.scale,
        rotation = params.rotation,
        originX = params.originX,
        originY = params.originY,
        offsetX = params.offsetX,
        offsetY = params.offsetY,
        worldWidth = params.worldWidth,
        worldHeight = params.worldHeight
      )
    }

    private fun packColors(colors: List<String>): FloatArray {
      val packed = FloatArray(5 * 4)
      val count = min(colors.size, 5)
      for (i in 0 until count) {
        val rgba = ColorParser.parse(colors[i])
        val base = i * 4
        packed[base] = rgba[0]
        packed[base + 1] = rgba[1]
        packed[base + 2] = rgba[2]
        packed[base + 3] = rgba[3]
      }
      return packed
    }
  }
}
