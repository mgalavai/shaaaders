package com.paper.shaders

internal const val PULSING_BORDER_VERTEX_SHADER = """
#version 300 es
precision highp float;

layout(location = 0) in vec2 a_position;

uniform vec2 u_resolution;
uniform float u_pixelRatio;
uniform float u_originX;
uniform float u_originY;
uniform float u_worldWidth;
uniform float u_worldHeight;
uniform float u_fit;
uniform float u_scale;
uniform float u_rotation;
uniform float u_offsetX;
uniform float u_offsetY;

out vec2 v_responsiveUV;
out vec2 v_responsiveBoxGivenSize;
out vec2 v_patternUV;

vec3 getBoxSize(float boxRatio, vec2 givenBoxSize) {
  vec2 box = vec2(0.0);
  box.x = boxRatio * min(givenBoxSize.x / boxRatio, givenBoxSize.y);
  float noFitBoxWidth = box.x;
  if (u_fit == 1.0) {
    box.x = boxRatio * min(u_resolution.x / boxRatio, u_resolution.y);
  } else if (u_fit == 2.0) {
    box.x = boxRatio * max(u_resolution.x / boxRatio, u_resolution.y);
  }
  box.y = box.x / boxRatio;
  return vec3(box, noFitBoxWidth);
}

void main() {
  gl_Position = vec4(a_position, 0.0, 1.0);

  vec2 uv = gl_Position.xy * 0.5;
  vec2 boxOrigin = vec2(0.5 - u_originX, u_originY - 0.5);
  vec2 givenBoxSize = vec2(u_worldWidth, u_worldHeight);
  givenBoxSize = max(givenBoxSize, vec2(1.0)) * u_pixelRatio;

  float r = u_rotation * 3.14159265358979323846 / 180.0;
  mat2 graphicRotation = mat2(cos(r), sin(r), -sin(r), cos(r));
  vec2 graphicOffset = vec2(-u_offsetX, u_offsetY);

  v_responsiveBoxGivenSize = vec2(
    (u_worldWidth == 0.0) ? u_resolution.x : givenBoxSize.x,
    (u_worldHeight == 0.0) ? u_resolution.y : givenBoxSize.y
  );

  float responsiveRatio = v_responsiveBoxGivenSize.x / v_responsiveBoxGivenSize.y;
  vec2 responsiveBoxSize = getBoxSize(responsiveRatio, v_responsiveBoxGivenSize).xy;
  vec2 responsiveBoxScale = u_resolution.xy / responsiveBoxSize;

  v_responsiveUV = uv;
  v_responsiveUV *= responsiveBoxScale;
  v_responsiveUV += boxOrigin * (responsiveBoxScale - 1.0);
  v_responsiveUV += graphicOffset;
  v_responsiveUV /= u_scale;
  v_responsiveUV.x *= responsiveRatio;
  v_responsiveUV = graphicRotation * v_responsiveUV;
  v_responsiveUV.x /= responsiveRatio;

  vec2 patternBoxGivenSize = vec2(
    (u_worldWidth == 0.0) ? u_resolution.x : givenBoxSize.x,
    (u_worldHeight == 0.0) ? u_resolution.y : givenBoxSize.y
  );
  float patternBoxRatio = patternBoxGivenSize.x / patternBoxGivenSize.y;

  vec3 boxSizeData = getBoxSize(patternBoxRatio, patternBoxGivenSize);
  vec2 patternBoxSize = boxSizeData.xy;
  float patternBoxNoFitBoxWidth = boxSizeData.z;
  vec2 patternBoxScale = u_resolution.xy / patternBoxSize;

  v_patternUV = uv;
  v_patternUV += graphicOffset / patternBoxScale;
  v_patternUV += boxOrigin;
  v_patternUV -= boxOrigin / patternBoxScale;
  v_patternUV *= u_resolution.xy;
  v_patternUV /= u_pixelRatio;
  if (u_fit > 0.0) {
    v_patternUV *= (patternBoxNoFitBoxWidth / patternBoxSize.x);
  }
  v_patternUV /= u_scale;
  v_patternUV = graphicRotation * v_patternUV;
  v_patternUV += boxOrigin / patternBoxScale;
  v_patternUV -= boxOrigin;
  v_patternUV *= 0.01;
}
""".trimIndent()

internal const val PULSING_BORDER_FRAGMENT_SHADER = """
#version 300 es
precision highp float;

uniform float u_time;

uniform vec4 u_colorBack;
uniform vec4 u_colors[5];
uniform float u_colorsCount;
uniform float u_roundness;
uniform float u_thickness;
uniform float u_marginLeft;
uniform float u_marginRight;
uniform float u_marginTop;
uniform float u_marginBottom;
uniform float u_aspectRatio;
uniform float u_softness;
uniform float u_intensity;
uniform float u_bloom;
uniform float u_spotSize;
uniform float u_spots;
uniform float u_pulse;
uniform float u_smoke;
uniform float u_smokeSize;

uniform sampler2D u_noiseTexture;

in vec2 v_responsiveUV;
in vec2 v_responsiveBoxGivenSize;
in vec2 v_patternUV;

out vec4 fragColor;

#define TWO_PI 6.28318530718
#define PI 3.14159265358979323846

float beat(float time) {
  float first = pow(abs(sin(time * TWO_PI)), 10.0);
  float second = pow(abs(sin((time - 0.15) * TWO_PI)), 10.0);

  return clamp(first + 0.6 * second, 0.0, 1.0);
}

float sst(float edge0, float edge1, float x) {
  return smoothstep(edge0, edge1, x);
}

float roundedBox(vec2 uv, vec2 halfSize, float distance, float cornerDistance, float thickness, float softness) {
  float borderDistance = abs(distance);
  float aa = 2.0 * fwidth(distance);
  float border = 1.0 - sst(min(mix(thickness, -thickness, softness), thickness + aa), max(mix(thickness, -thickness, softness), thickness + aa), borderDistance);
  float cornerFadeCircles = 0.0;
  cornerFadeCircles = mix(1.0, cornerFadeCircles, sst(0.0, 1.0, length((uv + halfSize) / thickness)));
  cornerFadeCircles = mix(1.0, cornerFadeCircles, sst(0.0, 1.0, length((uv - vec2(-halfSize.x, halfSize.y)) / thickness)));
  cornerFadeCircles = mix(1.0, cornerFadeCircles, sst(0.0, 1.0, length((uv - vec2(halfSize.x, -halfSize.y)) / thickness)));
  cornerFadeCircles = mix(1.0, cornerFadeCircles, sst(0.0, 1.0, length((uv - halfSize) / thickness)));
  aa = fwidth(cornerDistance);
  float cornerFade = sst(0.0, mix(aa, thickness, softness), cornerDistance);
  cornerFade *= cornerFadeCircles;
  border += cornerFade;
  return border;
}

vec2 randomGB(vec2 p) {
  vec2 uv = floor(p) / 100.0 + 0.5;
  return texture(u_noiseTexture, fract(uv)).gb;
}

float randomG(vec2 p) {
  vec2 uv = floor(p) / 100.0 + 0.5;
  return texture(u_noiseTexture, fract(uv)).g;
}

float valueNoise(vec2 st) {
  vec2 i = floor(st);
  vec2 f = fract(st);
  float a = randomG(i);
  float b = randomG(i + vec2(1.0, 0.0));
  float c = randomG(i + vec2(0.0, 1.0));
  float d = randomG(i + vec2(1.0, 1.0));
  vec2 u = f * f * (3.0 - 2.0 * f);
  float x1 = mix(a, b, u.x);
  float x2 = mix(c, d, u.x);
  return mix(x1, x2, u.y);
}

void main() {
  const float firstFrameOffset = 109.0;
  float t = 1.2 * (u_time + firstFrameOffset);

  vec2 borderUV = v_responsiveUV;
  float pulse = u_pulse * beat(0.18 * u_time);

  float canvasRatio = v_responsiveBoxGivenSize.x / v_responsiveBoxGivenSize.y;
  vec2 halfSize = vec2(0.5);
  borderUV.x *= max(canvasRatio, 1.0);
  borderUV.y /= min(canvasRatio, 1.0);
  halfSize.x *= max(canvasRatio, 1.0);
  halfSize.y /= min(canvasRatio, 1.0);

  float mL = u_marginLeft;
  float mR = u_marginRight;
  float mT = u_marginTop;
  float mB = u_marginBottom;
  float mX = mL + mR;
  float mY = mT + mB;

  if (u_aspectRatio > 0.0) {
    float shapeRatio = canvasRatio * (1.0 - mX) / max(1.0 - mY, 1e-6);
    float freeX = shapeRatio > 1.0 ? (1.0 - mX) * (1.0 - 1.0 / max(abs(shapeRatio), 1e-6)) : 0.0;
    float freeY = shapeRatio < 1.0 ? (1.0 - mY) * (1.0 - shapeRatio) : 0.0;
    mL += freeX * 0.5;
    mR += freeX * 0.5;
    mT += freeY * 0.5;
    mB += freeY * 0.5;
    mX = mL + mR;
    mY = mT + mB;
  }

  float thickness = 0.5 * u_thickness * min(halfSize.x, halfSize.y);

  halfSize.x *= (1.0 - mX);
  halfSize.y *= (1.0 - mY);

  vec2 centerShift = vec2(
    (mL - mR) * max(canvasRatio, 1.0) * 0.5,
    (mB - mT) / min(canvasRatio, 1.0) * 0.5
  );

  borderUV -= centerShift;
  halfSize -= mix(thickness, 0.0, u_softness);

  float radius = mix(0.0, min(halfSize.x, halfSize.y), u_roundness);
  vec2 d = abs(borderUV) - halfSize + radius;
  float outsideDistance = length(max(d, 0.0001)) - radius;
  float insideDistance = min(max(d.x, d.y), 0.0001);
  float cornerDistance = abs(min(max(d.x, d.y) - 0.45 * radius, 0.0));
  float distance = outsideDistance + insideDistance;

  float borderThickness = mix(thickness, 3.0 * thickness, u_softness);
  float border = roundedBox(borderUV, halfSize, distance, cornerDistance, borderThickness, u_softness);
  border = pow(border, 1.0 + u_softness);

  vec2 smokeUV = 0.3 * u_smokeSize * v_patternUV;
  float smoke = clamp(3.0 * valueNoise(2.7 * smokeUV + 0.5 * t), 0.0, 1.0);
  smoke -= valueNoise(3.4 * smokeUV - 0.5 * t);
  float smokeThickness = thickness + 0.2;
  smokeThickness = min(0.4, max(smokeThickness, 0.1));
  smoke *= roundedBox(borderUV, halfSize, distance, cornerDistance, smokeThickness, 1.0);
  smoke = 30.0 * smoke * smoke;
  smoke *= mix(0.0, 0.5, pow(u_smoke, 2.0));
  smoke *= mix(1.0, pulse, u_pulse);
  smoke = clamp(smoke, 0.0, 1.0);
  border += smoke;

  border = clamp(border, 0.0, 1.0);

  vec3 blendColor = vec3(0.0);
  float blendAlpha = 0.0;
  vec3 addColor = vec3(0.0);
  float addAlpha = 0.0;

  float bloom = 4.0 * u_bloom;
  float intensity = 1.0 + (1.0 + 4.0 * u_softness) * u_intensity;

  float angle = atan(borderUV.y, borderUV.x) / TWO_PI;

  for (int colorIdx = 0; colorIdx < 5; colorIdx++) {
    if (colorIdx >= int(u_colorsCount)) break;
    float colorIdxF = float(colorIdx);

    vec3 c = u_colors[colorIdx].rgb * u_colors[colorIdx].a;
    float a = u_colors[colorIdx].a;

    for (int spotIdx = 0; spotIdx < 4; spotIdx++) {
      if (spotIdx >= int(u_spots)) break;
      float spotIdxF = float(spotIdx);

      vec2 randVal = randomGB(vec2(spotIdxF * 10.0 + 2.0, 40.0 + colorIdxF));

      float time = (0.1 + 0.15 * abs(sin(spotIdxF * (2.0 + colorIdxF)) * cos(spotIdxF * (2.0 + 2.5 * colorIdxF)))) * t + randVal.x * 3.0;
      time *= mix(1.0, -1.0, step(0.5, randVal.y));

      float mask = 0.5 + 0.5 * mix(
        sin(t + spotIdxF * (5.0 - 1.5 * colorIdxF)),
        cos(t + spotIdxF * (3.0 + 1.3 * colorIdxF)),
        step(mod(colorIdxF, 2.0), 0.5)
      );

      float p = clamp(2.0 * u_pulse - randVal.x, 0.0, 1.0);
      mask = mix(mask, pulse, p);

      float atg1 = fract(angle + time);
      float spotSize = 0.05 + 0.6 * pow(u_spotSize, 2.0) + 0.05 * randVal.x;
      spotSize = mix(spotSize, 0.1, p);
      float sector = sst(0.5 - spotSize, 0.5, atg1) * (1.0 - sst(0.5, 0.5 + spotSize, atg1));

      sector *= mask;
      sector *= border;
      sector *= intensity;
      sector = clamp(sector, 0.0, 1.0);

      vec3 srcColor = c * sector;
      float srcAlpha = a * sector;

      blendColor += ((1.0 - blendAlpha) * srcColor);
      blendAlpha = blendAlpha + (1.0 - blendAlpha) * srcAlpha;
      addColor += srcColor;
      addAlpha += srcAlpha;
    }
  }

  vec3 accumColor = mix(blendColor, addColor, bloom);
  float accumAlpha = mix(blendAlpha, addAlpha, bloom);
  accumAlpha = clamp(accumAlpha, 0.0, 1.0);

  vec3 bgColor = u_colorBack.rgb * u_colorBack.a;
  vec3 color = accumColor + (1.0 - accumAlpha) * bgColor;
  float opacity = accumAlpha + (1.0 - accumAlpha) * u_colorBack.a;

  color += 1.0 / 256.0 * (fract(sin(dot(0.014 * gl_FragCoord.xy, vec2(12.9898, 78.233))) * 43758.5453123) - 0.5);

  fragColor = vec4(color, opacity);
}
""".trimIndent()
