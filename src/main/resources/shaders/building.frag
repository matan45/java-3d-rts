#version 460 core

in vec3 vWorldPos;

uniform vec4 uTint;
uniform vec3 uLightDir;
uniform vec3 uAmbient;

out vec4 fragColor;

void main() {
    vec3 N = normalize(cross(dFdx(vWorldPos), dFdy(vWorldPos)));
    float ndl = max(dot(N, uLightDir), 0.0);
    vec3 lit = uTint.rgb * (uAmbient + ndl * (1.0 - uAmbient));
    fragColor = vec4(lit, uTint.a);
}
