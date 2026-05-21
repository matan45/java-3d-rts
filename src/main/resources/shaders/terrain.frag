#version 460 core

in vec3 vWorldPos;
in vec3 vNormal;

uniform vec3 uLightDir;
uniform vec3 uAmbient;
uniform float uMaxHeight;

out vec4 fragColor;

const vec3 C_WATER = vec3(0.10, 0.25, 0.45);
const vec3 C_SAND  = vec3(0.78, 0.72, 0.50);
const vec3 C_GRASS = vec3(0.28, 0.50, 0.22);
const vec3 C_ROCK  = vec3(0.42, 0.38, 0.34);
const vec3 C_SNOW  = vec3(0.95, 0.95, 0.98);

float hash21(vec2 p) {
    p = fract(p * vec2(123.34, 456.21));
    p += dot(p, p + 45.32);
    return fract(p.x * p.y);
}

float valueNoise(vec2 p) {
    vec2 i = floor(p);
    vec2 f = fract(p);
    float a = hash21(i);
    float b = hash21(i + vec2(1.0, 0.0));
    float c = hash21(i + vec2(0.0, 1.0));
    float d = hash21(i + vec2(1.0, 1.0));
    vec2 u = f * f * (3.0 - 2.0 * f);
    return mix(mix(a, b, u.x), mix(c, d, u.x), u.y);
}

void main() {
    vec3 N = normalize(vNormal);

    float h01 = clamp(vWorldPos.y / uMaxHeight, 0.0, 1.0);

    vec3 base = C_WATER;
    base = mix(base, C_SAND,  smoothstep(0.18, 0.24, h01));
    base = mix(base, C_GRASS, smoothstep(0.26, 0.34, h01));
    base = mix(base, C_ROCK,  smoothstep(0.55, 0.70, h01));
    base = mix(base, C_SNOW,  smoothstep(0.80, 0.88, h01));

    float slope = 1.0 - clamp(N.y, 0.0, 1.0);
    float rockBlend = smoothstep(0.30, 0.65, slope) * step(0.18, h01);
    base = mix(base, C_ROCK, rockBlend);

    float n = valueNoise(vWorldPos.xz * 0.35) - 0.5;
    float n2 = valueNoise(vWorldPos.xz * 1.7) - 0.5;
    base += vec3(n * 0.08 + n2 * 0.03);

    float ndl = max(dot(N, uLightDir), 0.0);
    vec3 lit = base * (uAmbient + ndl * (1.0 - uAmbient));

    fragColor = vec4(lit, 1.0);
}
