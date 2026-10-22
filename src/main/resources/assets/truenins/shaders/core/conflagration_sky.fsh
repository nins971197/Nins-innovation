#version 150
uniform sampler2D SceneColor,SceneDepth;
uniform mat4 InverseViewProjection;
uniform float Age,FeedbackScale;
// Whole-pass fade, driven by the scene; FeedbackScale keeps the accessibility slider working.
uniform float Visibility;
in vec2 texCoord;
in vec4 tint;
out vec4 fragColor;
float hash(vec3 p){p=fract(p*.1031);p+=dot(p,p.yzx+33.33);return fract((p.x+p.y)*p.z);}
float noise(vec3 p){vec3 i=floor(p),f=fract(p);f=f*f*(3.-2.*f);return mix(mix(mix(hash(i),hash(i+vec3(1,0,0)),f.x),mix(hash(i+vec3(0,1,0)),hash(i+vec3(1,1,0)),f.x),f.y),mix(mix(hash(i+vec3(0,0,1)),hash(i+vec3(1,0,1)),f.x),mix(hash(i+vec3(0,1,1)),hash(i+vec3(1,1,1)),f.x),f.y),f.z);}
float fbm(vec3 p){return .57*noise(p)+.28*noise(p*2.07+17.)+.15*noise(p*4.17-9.);}
void main(){
    vec3 scene=texture(SceneColor,texCoord).rgb;
    float depth=texture(SceneDepth,texCoord).r;
    vec4 fp=InverseViewProjection*vec4(texCoord*2.-1.,1,1);
    vec3 ray=normalize(fp.xyz*(fp.w<0.?-1.:1.));
    float fade=smoothstep(0.,3.2,Age)*(1.-smoothstep(12.4,15.,Age))*clamp(FeedbackScale,0.,1.);
    // Direction-space heavens: coherent across the dimension, independent of draw distance.
    vec3 axis=normalize(vec3(-.22,1.,.12));
    float polar=acos(clamp(dot(ray,axis),-1.,1.));
    vec3 east=normalize(cross(axis,vec3(0,0,1))),north=cross(axis,east);
    float angle=atan(dot(ray,north),dot(ray,east)),r=polar*1.28;
    float swirl=angle+r*2.1-Age*.10;
    vec3 q=vec3(cos(swirl)*r,sin(swirl)*r,r*.8)*3.2;
    float cloud=fbm(q+vec3(0,0,Age*.14));
    float folds=sin(r*6.-angle*2.+cloud*5.-Age*.3)*.5+.5;
    float bank=fbm(q*2.1+cloud*2.5+Age*.07);
    // Reference background: indigo / violet-grey banks around a warm central opening.
    // Broad illumination replaces the previous continuous yellow vortex ring.
    vec3 color=mix(vec3(.018,.021,.039),vec3(.105,.096,.143),smoothstep(.15,.82,cloud));
    color+=vec3(.052,.061,.094)*folds*smoothstep(.42,.78,bank);
    float opening=exp(-pow(r/.57,2.));
    float sun=exp(-pow(r/.24,2.));
    float silver=smoothstep(.58,.83,bank)*folds;
    color+=vec3(.16,.14,.20)*silver*(.3+opening*.7);
    color+=vec3(.95,.53,.17)*opening*(.35+bank*.35);
    color+=vec3(1.55,1.10,.45)*sun*(.55+folds*.45);
    float cuts=pow(max(0.,1.-abs(bank-.53)*18.),5.)*smoothstep(.48,.77,cloud);
    color+=vec3(.70,.45,.16)*cuts*opening*.20;
    float horizon=smoothstep(-.22,.14,ray.y);
    color=mix(vec3(.035,.032,.055),color,horizon);
    if(depth>.99999)scene=mix(scene,color,fade);
    else {
        // Keep architecture readable. The warm light is deliberately much weaker than the sky.
        vec3 warm=scene*vec3(1.08,.82,.71)+vec3(.026,.008,.002);
        scene=mix(scene,warm,fade*.52);
    }
    fragColor=vec4(scene,1)*tint*Visibility;
}
