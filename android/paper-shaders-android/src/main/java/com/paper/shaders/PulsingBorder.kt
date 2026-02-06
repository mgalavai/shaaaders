package com.paper.shaders

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import kotlinx.coroutines.isActive
import kotlin.math.min

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
  val controller = remember { RuntimeShaderController(PULSING_BORDER_SHADER) }
  val noise = remember { NoiseTextureLoader.load() }

  val colorsPacked = remember(params.colors) { packColors(params.colors) }
  val colorsCount = min(params.colors.size, 5)
  val colorBack = remember(params.colorBack) { ColorParser.parse(params.colorBack) }
  val margins = params.resolveMargins()

  SideEffect {
    controller.shader.setFloatUniform("u_colorBack", colorBack)
    controller.shader.setFloatUniform("u_colors", colorsPacked)
    controller.shader.setFloatUniform("u_colorsCount", colorsCount.toFloat())
    controller.shader.setFloatUniform("u_roundness", params.roundness)
    controller.shader.setFloatUniform("u_thickness", params.thickness)
    controller.shader.setFloatUniform("u_marginLeft", margins.left)
    controller.shader.setFloatUniform("u_marginRight", margins.right)
    controller.shader.setFloatUniform("u_marginTop", margins.top)
    controller.shader.setFloatUniform("u_marginBottom", margins.bottom)
    controller.shader.setFloatUniform("u_aspectRatio", params.aspectRatio.value)
    controller.shader.setFloatUniform("u_softness", params.softness)
    controller.shader.setFloatUniform("u_intensity", params.intensity)
    controller.shader.setFloatUniform("u_bloom", params.bloom)
    controller.shader.setFloatUniform("u_spots", params.spots.toFloat())
    controller.shader.setFloatUniform("u_spotSize", params.spotSize)
    controller.shader.setFloatUniform("u_pulse", params.pulse)
    controller.shader.setFloatUniform("u_smoke", params.smoke)
    controller.shader.setFloatUniform("u_smokeSize", params.smokeSize)

    controller.shader.setFloatUniform("u_fit", params.fit.value)
    controller.shader.setFloatUniform("u_scale", params.scale)
    controller.shader.setFloatUniform("u_rotation", params.rotation)
    controller.shader.setFloatUniform("u_offsetX", params.offsetX)
    controller.shader.setFloatUniform("u_offsetY", params.offsetY)
    controller.shader.setFloatUniform("u_originX", params.originX)
    controller.shader.setFloatUniform("u_originY", params.originY)
    controller.shader.setFloatUniform("u_worldWidth", params.worldWidth)
    controller.shader.setFloatUniform("u_worldHeight", params.worldHeight)

    controller.shader.setInputShader("u_noiseTexture", noise.shader)
    controller.shader.setFloatUniform("u_noiseSize", noise.width.toFloat(), noise.height.toFloat())
  }

  var tick by remember { mutableIntStateOf(0) }

  LaunchedEffect(params.speed, params.frame) {
    controller.setMotion(params.speed, params.frame)
    if (params.speed == 0f) {
      tick += 1
      return@LaunchedEffect
    }
    while (isActive) {
      withFrameNanos { frameTime ->
        controller.updateTime(frameTime)
        tick += 1
      }
    }
  }

  val tickValue = tick
  Canvas(modifier = modifier) {
    controller.setResolution(size.width, size.height, density.density)
    drawRect(ShaderBrush(controller.shader))
  }
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
