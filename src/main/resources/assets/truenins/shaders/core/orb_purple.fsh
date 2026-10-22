#version 150
// Adapted from Jujutsu Realms 0.102.5 by VFX Workshop; see META-INF/TRUENINS-THIRD-PARTY-NOTICES.txt.

// GLSL pow is undefined for negative bases, including pow(x, 2.0).
float vfxSquare(float x) { return x * x; }
vec2 vfxSquare(vec2 x) { return x * x; }
vec3 vfxSquare(vec3 x) { return x * x; }
vec4 vfxSquare(vec4 x) { return x * x; }
uniform sampler2D SceneDepth, SceneColor;
uniform mat4 InverseViewProjection, ViewProjection;
uniform vec3 CenterRelative, FusionRight, FusionUp;
uniform float Age, Radius, Seed, Visibility, FeedbackScale;
uniform vec2 ScreenSize;
in vec2 texCoord;
in vec4 tint;
out vec4 fragColor;

float hash(vec3 p){return fract(sin(dot(p,vec3(127.1,311.7,74.7)))*43758.5453);}
float noise(vec3 p){
    vec3 i=floor(p),f=fract(p);f=f*f*(3.0-2.0*f);
    return mix(mix(mix(hash(i),hash(i+vec3(1,0,0)),f.x),mix(hash(i+vec3(0,1,0)),hash(i+vec3(1,1,0)),f.x),f.y),
               mix(mix(hash(i+vec3(0,0,1)),hash(i+vec3(1,0,1)),f.x),mix(hash(i+vec3(0,1,1)),hash(i+vec3(1,1,1)),f.x),f.y),f.z);
}
mat2 turn(float angle){float s=sin(angle),c=cos(angle);return mat2(c,-s,s,c);}
vec4 field(vec3 local){
    float unit=1.; // Blast configuration must not inflate the two converging cores.
    if(Age<1.0){
        float gather=smoothstep(0.0,.82,Age),collapse=smoothstep(.68,1.0,Age);
        float orbit=(1.0-gather)*3.8*unit,orb=unit*(1.22+.38*gather)*(1.0-collapse*.55);
        // Start at the exact endpoints of the incoming morph; the orbit then continues
        // smoothly around the collision's shared basis for every observer.
        vec3 q=vec3(dot(local,FusionRight),dot(local,FusionUp),dot(local,cross(FusionUp,FusionRight)));
        q.xy=turn(Age*2.2)*q.xy;
        float blue=length(q-vec3(orbit,0,0))/orb,red=length(q+vec3(orbit,0,0))/orb;
        float cloud=noise(q/max(unit,.1)*2.9+vec3(Seed*.021,Age*3.0,-Age*2.1));
        float a=exp(-blue*blue*1.55),b=exp(-red*red*1.55);
        float narrow=exp(-dot(q.yz,q.yz)/(orb*orb*.11));
        float link=narrow*exp(-q.x*q.x/max(orbit*orbit+orb*orb,.01))*.55*gather;
        float strands=pow(.5+.5*sin(atan(q.z,q.y+.00001)*4.0+q.x*4.0/unit-Age*13.0),5.0);
        float sheath=exp(-vfxSquare((length(q)/(2.1*unit)-1.0)/.25))*strands*.15;
        float density=(a+b+link+sheath)*(1.0+cloud*.8);
        vec3 color=(vec3(.025,.62,2.8)*a+vec3(2.5,.018,.15)*b)/max(a+b,.001);
        float white=pow(max(a,b),5.0)*(.5+.5*cloud);
        color=mix(color,vec3(3.6,2.8,4.0),white*.80);
        color=mix(color,vec3(.065,.024,.24),collapse*.93);
        density*=1.0+collapse*1.2;
        return vec4(color,density);
    }
    float elapsed=Age-1.0,front=Radius*clamp(pow(max(elapsed,0.)/0.7,.68),.035,1.0);
    vec3 q=local/max(front,.10);float r=length(q);
    vec3 drift=q*5.7+vec3(Seed*.071,-elapsed*.70,elapsed*.43);
    float cloud=noise(drift)*.67+noise(drift*2.1+3.7)*.33;
    float ripples=.5+.5*sin(r*36.0-elapsed*9.0+cloud*9.0);
    float curl=vfxSquare(clamp((cloud-.31)*2.1,0.0,1.0));
    float outer=1.0-smoothstep(.70,1.05,r),inner=smoothstep(.08,.42,r);
    // Thin, torn gas sheets allow terrain and the new crater to remain visible after the pulse.
    float sheets=pow(max(0.,1.-abs(cloud-.52)*13.),3.);
    float density=(.008+.12*sheets)*outer*inner*(.12+.88*ripples);
    float fade=1.0-smoothstep(1.25,4.50,elapsed);
    density*=fade;
    float shell=exp(-vfxSquare((r-.93-cloud*.025)/.028));
    float torn=smoothstep(.32,.62,cloud);
    density+=shell*(.055+.58*torn)*exp(-elapsed*1.55);
    float hot=exp(-elapsed*7.5)*exp(-r*r*1.8);
    density+=hot*.58;
    float shock=exp(-vfxSquare((r-.98)/.018))*exp(-elapsed*2.5);
    float spokes=pow(.5+.5*sin(atan(q.z,q.x+.00001)*19.+cloud*6.),20.);
    density+=shock*(.2+spokes*.8);
    // Three delayed pressure sheets break apart instead of reading as one uniform ball.
    float angle=atan(q.z,q.x+.00001);
    float jagged=.5+.5*sin(angle*23.+q.y*19.+cloud*7.-elapsed*12.);
    float secondary=exp(-vfxSquare((r-(.62+.06*sin(angle*7.+elapsed*6.)))/.025));
    float fracture=pow(jagged,9.)*smoothstep(.25,.62,r)*(1.-smoothstep(.95,1.12,r));
    density+=(secondary*.28+fracture*.34)*exp(-elapsed*1.7);
    vec3 violet=mix(vec3(.035,.008,.16),vec3(.48,.025,1.05),curl);
    vec3 color=mix(violet,vec3(1.05,.24,1.80),shell*torn*.6);
    color=mix(color,vec3(2.9,2.55,3.4),clamp(hot+shock*.75,0.,1.));
    color+=vec3(1.8,.65,2.9)*(secondary*.4+fracture*.7)*exp(-elapsed*1.7);
    float filaments=pow(.5+.5*sin(atan(q.z,q.x+.00001)*13.+q.y*17.-elapsed*8.+cloud*5.),12.)
        *smoothstep(.5,.7,cloud);
    density+=filaments*outer*inner*.20*fade;
    color+=vec3(.55,.16,1.10)*filaments*outer*fade;
    return vec4(color,density);
}
void vfxMaterialMain(){
    fragColor=vec4(0);
    if(Age>=5.5)return;
    float unit=1.;
    float scale=Age<1.0?unit*2.1:max(.10,Radius*clamp(pow(max(Age-1.,0.)/.7,.68),.035,1.0));
    float bound=Age<1.0?unit*6.5:scale*1.09;
    vec4 farPoint=InverseViewProjection*vec4(texCoord*2.0-1.0,1,1);
    vec3 ray=normalize(farPoint.xyz*(farPoint.w<0.0?-1.0:1.0));
    float along=dot(ray,CenterRelative),disc=along*along+bound*bound-dot(CenterRelative,CenterRelative);
    if(disc<=0.0)return;
    float root=sqrt(disc),first=max(.025,along-root),last=along+root;
    float depth=texture(SceneDepth,texCoord).r;
    if(depth<.999999){
        vec4 scene=InverseViewProjection*vec4(texCoord*2.0-1.0,depth*2.0-1.0,1);
        last=min(last,dot(scene.xyz/scene.w,ray));
    }
    if(last<=first)return;
    // Preserve close-up density; distant blasts need fewer samples per covered pixel.
    float screenExtent=bound/max(length(CenterRelative),.01);
    int samples=screenExtent<.11?24:Age>2.8?32:48;
    float stepSize=(last-first)/float(samples),jitter=.35+.30*hash(vec3(gl_FragCoord.xy,0));
    vec4 sum=vec4(0);
    for(int i=0;i<48;i++){
        if(i>=samples)break;
        vec4 f=field(ray*(first+(float(i)+jitter)*stepSize)-CenterRelative);
        float a=1.0-exp(-f.a*stepSize*3.2/max(scale,.10));
        sum.rgb+=(1.0-sum.a)*f.rgb*a;sum.a+=(1.0-sum.a)*a;
        if(sum.a>.985)break;
    }
    fragColor=sum*Visibility*tint;
}

// Composite impact frames, radial pressure and the volume through the shared scene target.
void main() {
    vfxMaterialMain();
    if (any(isnan(fragColor)) || any(isinf(fragColor))) fragColor=vec4(0);
    vec4 volume=fragColor;
    vec3 background=texture(SceneColor,texCoord).rgb;
    vec4 clip=ViewProjection*vec4(CenterRelative,1);
    vec2 centerUV=clip.xy/max(clip.w,.00001)*.5+.5;
    float elapsed=max(0.,Age-1.);
    float response=FeedbackScale*Visibility*(1.-smoothstep(Radius,Radius*3.,length(CenterRelative)));
    if(clip.w>0.&&all(greaterThan(centerUV,vec2(0)))&&all(lessThan(centerUV,vec2(1)))){
        float centerDepth=clip.z/clip.w*.5+.5;
        response*=step(centerDepth-.00008,texture(SceneDepth,centerUV).r);
        vec2 aspect=vec2(ScreenSize.x/ScreenSize.y,1.);
        vec2 offset=(texCoord-centerUV)*aspect;
        float radial=length(offset),angle=atan(offset.y,offset.x+.00001);
        vec2 direction=offset/max(radial,.0001)/aspect;
        float pressure=Age<1.?smoothstep(.3,1.,Age)*.0009
            :exp(-vfxSquare((radial-elapsed*2.7)/.065))*.023*exp(-elapsed*2.4);
        pressure*=response;
        vec2 uv=clamp(texCoord-direction*pressure,vec2(.001),vec2(.999));
        // Keep displacement from dragging silhouettes across a depth discontinuity.
        if(abs(texture(SceneDepth,uv).r-texture(SceneDepth,texCoord).r)>.002)uv=mix(texCoord,uv,.04);
        background=texture(SceneColor,uv).rgb;
        if(Age>=1.&&elapsed<1.){
            float streak=pow(.5+.5*sin(angle*61.+sin(angle*17.)*3.),22.)
                *smoothstep(.12,.6,radial)*exp(-elapsed*7.)*response;
            volume.rgb+=vec3(.65,.37,1.1)*streak*.48;
        }
    }else response=0.;
    vec3 color=background*(1.-volume.a)+volume.rgb;
    if(Age>=1.&&elapsed<.8){
        // Brief white impact, a dark purple silhouette frame, then a softer exposure recovery.
        float flash=exp(-elapsed*12.)*.97;
        float inversion=exp(-vfxSquare((elapsed-.075)/.026))*.78;
        float recovery=exp(-vfxSquare((elapsed-.19)/.060))*.25;
        float distanceFromCore=length((texCoord-centerUV)*vec2(ScreenSize.x/ScreenSize.y,1.));
        flash*=.62+.38*exp(-distanceFromCore*distanceFromCore*2.5);
        color=mix(color,vec3(1.04,1.015,1.065),clamp((flash+recovery)*response,0.,.98));
        vec3 ink=mix(vec3(.004,.001,.018),vec3(.70,.40,1.),clamp(length(volume.rgb)*.38,0.,1.));
        color=mix(color,ink,inversion*response);
    }
    fragColor=vec4(color,1)*tint;
    if(any(isnan(fragColor))||any(isinf(fragColor)))discard;
}
