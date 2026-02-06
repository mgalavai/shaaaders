package com.paper.shaders

import android.app.ActivityManager
import android.content.Context
import android.opengl.GLSurfaceView
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.viewinterop.AndroidView

@Composable
fun PulsingBorder(spec: PulsingBorderSpec, modifier: Modifier = Modifier) {
  val density = LocalDensity.current
  val widthDp: Dp? = spec.width?.let { with(density) { it.toDp() } }
  val heightDp: Dp? = spec.height?.let { with(density) { it.toDp() } }
  val sizeModifier = if (widthDp != null && heightDp != null) {
    modifier.then(Modifier.size(widthDp, heightDp))
  } else {
    modifier
  }
  PulsingBorder(params = spec.params, modifier = sizeModifier)
}

@Composable
fun PulsingBorder(params: PulsingBorderParams, modifier: Modifier = Modifier) {
  val density = LocalDensity.current
  val pixelRatio = density.density
  val state = remember(params, pixelRatio) {
    PulsingBorderUniformState.fromParams(params, pixelRatio)
  }

  AndroidView(
    modifier = modifier,
    factory = { context ->
      PulsingBorderGlView(context).apply {
        setUniformState(state)
      }
    },
    update = { view ->
      view.setUniformState(state)
    }
  )
}

private class PulsingBorderGlView(context: Context) : GLSurfaceView(context) {
  private val renderer = PulsingBorderGlRenderer()

  init {
    val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
    val supportsGles3 = (activityManager?.deviceConfigurationInfo?.reqGlEsVersion ?: 0) >= 0x30000
    check(supportsGles3) { "OpenGL ES 3.0 is required for PulsingBorder on API 32." }

    setEGLContextClientVersion(3)
    setRenderer(renderer)
    renderMode = RENDERMODE_CONTINUOUSLY
    preserveEGLContextOnPause = true
  }

  fun setUniformState(state: PulsingBorderUniformState) {
    queueEvent {
      renderer.setUniformState(state)
    }

    renderMode = if (state.speed == 0f) {
      RENDERMODE_WHEN_DIRTY
    } else {
      RENDERMODE_CONTINUOUSLY
    }

    if (state.speed == 0f) {
      requestRender()
    }
  }

  override fun onDetachedFromWindow() {
    queueEvent {
      renderer.release()
    }
    super.onDetachedFromWindow()
  }
}
