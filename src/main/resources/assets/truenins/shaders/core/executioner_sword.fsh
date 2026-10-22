#version 150
// Original sword silhouette; uses the existing Blue/Red scene-depth composition pipeline.
uniform sampler2D SceneDepth;
uniform mat4 InverseViewProjection;
uniform vec3 CenterRelative, Axis, Right, Normal;
uniform vec2 ScreenSize;
uniform float Time, Age, Formation, Visibility;
in vec2 texCoord;
in vec4 tint;
out vec4 fragColor;

// Distance from this pixel's visible ray to a finite, tapered luminous strand.
// Core and halo share the exact same endpoints, so the tip remains sharp when moving.
vec2 strand(vec3 ray, vec3 a, vec3 b, float widthA, float widthB, float depth) {
    vec3 delta=b-a;
    float dd=dot(delta,delta),rd=dot(ray,delta),ra=dot(ray,a),da=dot(delta,a);
    float u=clamp((rd*ra-da)/max(.000001,dd-rd*rd),0.,1.);
    vec3 point=a+delta*u;
    float t=dot(point,ray);
    if(t<.025||t>depth)return vec2(0);
    float dist=length(point-ray*t);
    float width=mix(widthA,widthB,u),aa=max(.002,t/max(ScreenSize.y,1.)*.7);
    float core=1.-smoothstep(max(0.,width-aa),width+aa,dist);
    float outer=max(0.,dist-width);
    float halo=exp(-outer*outer/max(.00001,(width*.6+.035)*(width*.6+.035)))*.45
        +exp(-outer*outer/.045)*.065;
    // The clamped segment already gives finite end caps, including near-parallel views.
    return vec2(core,halo);
}
vec3 point(float x,float y,float z){return CenterRelative+Right*x+Axis*y+Normal*z;}
vec3 lightBlade(vec3 ray,vec3 a,vec3 b,float wa,float wb,float depth){
    vec2 light=strand(ray,a,b,wa,wb,depth);
    return vec3(1.7,1.46,.58)*light.x+vec3(1.,.69,.055)*light.y;
}
void main(){
    vec4 fp=InverseViewProjection*vec4(texCoord*2.-1.,1,1);
    vec3 ray=normalize(fp.xyz*(fp.w<0.?-1.:1.));
    float along=dot(ray,CenterRelative);
    if(along+5.<0.||length(CenterRelative-ray*along)>5.)discard;
    float depth=texture(SceneDepth,texCoord).r,worldDepth=100000.;
    if(depth<.999999){vec4 wp=InverseViewProjection*vec4(texCoord*2.-1.,depth*2.-1.,1);worldDepth=dot(wp.xyz/wp.w,ray);}
    float lengthBlade=3.65*Formation;
    float pulse=.96+.04*sin(Time*18.);
    vec3 light=lightBlade(ray,point(0,0,0),point(0,lengthBlade,0),.145,.002,worldDepth);
    // Long luminous pommel and forked, needle-like guard, as in the supplied image.
    light+=lightBlade(ray,point(0,-1.35,0),point(0,0,0),.002,.065,worldDepth);
    for(int side=-1;side<=1;side+=2){
        float s=float(side);
        light+=lightBlade(ray,point(0,-.05,0),point(s*.64*Formation,-.23,0),.075,.002,worldDepth);
        light+=lightBlade(ray,point(s*.12,0,0),point(s*.29*Formation,.83*Formation,0),.07,.002,worldDepth);
    }
    // Fine gold fibres travel from the grip into the blade, rather than orbiting the sword.
    for(int i=0;i<9;i++){
        float n=float(i),phase=fract(Time*1.3+n*.618034),y=phase*max(.01,lengthBlade-.3);
        float a=n*2.39996+Time*2.;
        float radius=.16*(1.-y/3.9);
        vec3 from=point(cos(a)*radius,y,sin(a)*radius);
        vec3 to=point(cos(a+.25)*radius,y+.23,sin(a+.25)*radius);
        light+=lightBlade(ray,from,to,.007,.002,worldDepth)*.22;
    }
    // A narrow swept fan fades behind the blade; no solid rectangular trail geometry.
    if(Age>201.&&Age<221.){
        for(int i=1;i<=7;i++){
            float lag=float(i)*.07,angle=-lag*2.30;
            vec3 trailAxis=Axis*cos(angle)-Right*sin(angle);
            vec3 tip=CenterRelative+trailAxis*3.45;
            light+=lightBlade(ray,CenterRelative,tip,.035,.001,worldDepth)
                *(1.-float(i)/8.)*.14*(1.-smoothstep(213.,221.,Age));
        }
    }
    light*=Visibility*pulse;
    if(max(light.r,max(light.g,light.b))<.001)discard;
    // Premultiplied energy over the already composed court, like the other volume materials.
    float alpha=clamp(max(light.r,max(light.g,light.b))*.55,0.,.95);
    fragColor=vec4(light,alpha)*tint;
    if(any(isnan(fragColor))||any(isinf(fragColor)))discard;
}
