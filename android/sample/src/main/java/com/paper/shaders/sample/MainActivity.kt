package com.paper.shaders.sample

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.paper.shaders.PulsingBorder
import com.paper.shaders.PulsingBorderParams
import com.paper.shaders.PulsingBorderSpec

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContent {
      val params = PulsingBorderParams(
        colors = listOf("#0dc1fd", "#d915ef", "#ff3f2ecc"),
        colorBack = "#000000",
        roundness = 1f,
        thickness = 0f,
        softness = 0.75f,
        aspectRatio = PulsingBorderParams.AspectRatio.SQUARE,
        intensity = 0.2f,
        bloom = 0.45f,
        spots = 3,
        spotSize = 0.4f,
        pulse = 0.5f,
        smoke = 1f,
        smokeSize = 0f,
        speed = 1f,
        scale = 0.6f,
        marginLeft = 0f,
        marginRight = 0f,
        marginTop = 0f,
        marginBottom = 0f
      )

      Box(modifier = Modifier.fillMaxSize()) {
        PulsingBorder(
          spec = PulsingBorderSpec(width = 1280, height = 720, params = params),
          modifier = Modifier.fillMaxSize()
        )
      }
    }
  }
}
