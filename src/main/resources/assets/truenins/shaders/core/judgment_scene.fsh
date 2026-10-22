#version 150
uniform sampler2D SceneColor, SceneDepth, StageColor, StageDepth;
uniform mat4 InverseViewProjection, ViewProjection;
uniform vec3 CenterRelative, CourtForward;
uniform float Age, Radius, Visibility, Cold;
in vec2 texCoord;
in vec4 tint;
out vec4 fragColor;

float hash(vec3 p){p=fract(p*.1031);p+=dot(p,p.yzx+33.33);return fract((p.x+p.y)*p.z);}
float noise(vec3 p){
    vec3 i=floor(p),f=fract(p);f=f*f*(3.-2.*f);
    return mix(mix(mix(hash(i),hash(i+vec3(1,0,0)),f.x),mix(hash(i+vec3(0,1,0)),hash(i+vec3(1,1,0)),f.x),f.y),
        mix(mix(hash(i+vec3(0,0,1)),hash(i+vec3(1,0,1)),f.x),mix(hash(i+vec3(0,1,1)),hash(i+vec3(1,1,1)),f.x),f.y),f.z);
}
// Anchored to the court, so looking around reveals the same invading space.
float blackInvasion(vec3 direction){
    if(Age>=12.)return 1.;
    vec3 right=vec3(CourtForward.z,0,-CourtForward.x);
    vec3 origin=normalize(-CourtForward*.18-right*.88-vec3(0,.34,0));
    float progress=smoothstep(0.,12.,Age);
    float tendrils=(noise(direction*12.+vec3(4.2,1.7,8.9))-.5)*.28
        +(noise(direction*31.)-.5)*.12+(noise(direction*73.)-.5)*.05;
    float field=dot(direction,origin)+tendrils-mix(1.32,-1.32,progress);
    return smoothstep(-.035,.025,field);
}

void main(){
    vec4 fp=InverseViewProjection*vec4(texCoord*2.-1.,1,1);
    vec3 ray=normalize(fp.xyz*(fp.w<0.?-1.:1.));
    float b=dot(ray,CenterRelative),disc=b*b-dot(CenterRelative,CenterRelative)+Radius*Radius;
    if(disc<=0.)discard;
    float root=sqrt(disc),nearWall=max(.025,b-root),farWall=b+root;
    if(farWall<.025)discard;
    bool inside=length(CenterRelative)<Radius;
    float wall=inside?farWall:nearWall;
    float depth=texture(SceneDepth,texCoord).r,worldDepth=100000.;
    if(depth<.999999){vec4 wp=InverseViewProjection*vec4(texCoord*2.-1.,depth*2.-1.,1);worldDepth=dot(wp.xyz/wp.w,ray);}
    if(!inside&&worldDepth<nearWall)discard;
    vec3 background=texture(SceneColor,texCoord).rgb;
    vec3 point=ray*wall-CenterRelative,normal=point/max(Radius,.025);
    float entry=inside?blackInvasion(ray):smoothstep(0.,8.,Age),reveal=smoothstep(24.,38.,Age);
    float cold=Cold;
    vec3 light=mix(vec3(1.16,.72,.34),vec3(.22,.42,.98),cold);
    // Fully enclosed black stage. A faint upper vault gives depth without a glowing forcefield.
    vec3 vault=vec3(.003,.004,.008)+vec3(.008,.009,.013)*pow(max(0.,normal.y),3.);
    float rim=pow(1.-abs(dot(normal,ray)),5.);
    if(!inside)vault=vec3(.0015,.0017,.0022)+vec3(.008,.009,.012)*rim;
    float stageDepth=texture(StageDepth,texCoord).r;
    float stageDistance=100000.;
    if(stageDepth<.999999){vec4 sp=InverseViewProjection*vec4(texCoord*2.-1.,stageDepth*2.-1.,1);stageDistance=dot(sp.xyz/sp.w,ray);}
    vec3 court=vault;
    if(inside&&stageDistance<wall){
        vec3 scene=ray*stageDistance-CenterRelative;
        vec3 local=vec3(dot(scene,vec3(CourtForward.z,0,-CourtForward.x)),scene.y,dot(scene,CourtForward));
        float pool=max(exp(-dot(local.xz-vec2(0,4),local.xz-vec2(0,4))*.12),
                       exp(-dot(local.xz+vec2(0,4),local.xz+vec2(0,4))*.12));
        // A restrained third pool keeps the charcoal judge readable against the vault.
        pool=max(pool,.72*exp(-dot(local.xz-vec2(0,9.4),local.xz-vec2(0,9.4))*.22));
        // Hard changes in overhead spotlights, dark blue ambient between amber pools.
        vec3 illumination=mix(vec3(.09,.15,.30),light,.25+.75*pool);
        float judgeLight=exp(-dot(local.xz-vec2(0,9.4),local.xz-vec2(0,9.4))*.32)
            *smoothstep(1.8,2.5,scene.y);
        illumination=mix(illumination,vec3(.78,.83,.96),judgeLight*.9);
        court=texture(StageColor,texCoord).rgb*illumination;
        float fog=(1.-exp(-stageDistance*.016))*.32;
        court=mix(court,vec3(.012,.014,.018),fog);
        court+=mix(vec3(.12,.055,.008),vec3(.012,.027,.085),cold)*pool*exp(-abs(scene.y-.04)*2.);
    }
    // An external observer sees the near, opaque shell, never the captured court inside it.
    if(!inside)court=vault;
    // Blackout covers the captured interior too, then the staged geometry emerges through it.
    court=mix(vec3(.0007,.0006,.001),court,reveal);
    if(inside&&Age<24.){
        float shot=smoothstep(4.,9.,Age)*(1.-smoothstep(20.,24.,Age));
        if(stageDepth<.999999)court=mix(court,texture(StageColor,texCoord).rgb*vec3(.9,.72,.5),shot);
        float impact=exp(-max(0.,Age-16.)*1.1)*step(16.,Age);
        court+=vec3(.13,.10,.06)*impact;
    }
    float fade=entry*Visibility;
    vec3 color=mix(background,court,fade);
    vec4 clip=ViewProjection*vec4(ray*wall,1);
    float barrier=clamp(clip.z/clip.w*.5+.5,0.,1.);
    // Interior must use the court depth. Keeping world depth makes trees/walls hide the sword.
    gl_FragDepth=fade>.98?(inside?min(stageDepth,barrier):barrier):depth;
    fragColor=vec4(color,1)*tint;
    if(any(isnan(fragColor))||any(isinf(fragColor)))discard;
}
