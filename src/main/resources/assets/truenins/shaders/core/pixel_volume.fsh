#version 150
#moj_import <fog.glsl>
uniform float FogStart;
uniform float FogEnd;
uniform vec4 FogColor;
in float vertexDistance;
uniform sampler2D Sampler0;
uniform vec4 ColorModulator;
in vec4 vertexColor;
in vec2 texCoord;
out vec4 fragColor;
void main() {
    fragColor = texture(Sampler0, texCoord) * vertexColor * ColorModulator;
    if (fragColor.a < 0.001) discard;
    fragColor = linear_fog(fragColor, vertexDistance, FogStart, FogEnd, FogColor);
}
