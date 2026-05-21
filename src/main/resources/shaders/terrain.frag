#version 460 core

in vec3 vWorldPos;
flat in vec3 vColor;

uniform vec3 uLightDir;
uniform vec3 uAmbient;

out vec4 fragColor;

void main() {
    vec3 N = normalize(cross(dFdx(vWorldPos), dFdy(vWorldPos)));
    if (N.y < 0.0) N = -N;
    float ndl = max(dot(N, uLightDir), 0.0);
    vec3 lit = vColor * (uAmbient + ndl * (1.0 - uAmbient));
    fragColor = vec4(lit, 1.0);
}
