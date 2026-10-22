#version 150
// The reference domain's bounded ray/depth approach, adapted to the sword-grave domain.
uniform sampler2D SceneColor, SceneDepth;
uniform mat4 InverseViewProjection;
uniform mat4 ViewProjection;
uniform vec3 CenterRelative;
uniform float Radius, Visibility, Time, Age, PulseAge, Expansion;
in vec2 texCoord;
in vec4 tint;
out vec4 fragColor;

float hash(vec3 p){p=fract(p*.1031);p+=dot(p,p.yzx+33.33);return fract((p.x+p.y)*p.z);}
float noise(vec3 p){
    vec3 i=floor(p),f=fract(p);f=f*f*(3.-2.*f);
    return mix(mix(mix(hash(i),hash(i+vec3(1,0,0)),f.x),mix(hash(i+vec3(0,1,0)),hash(i+vec3(1,1,0)),f.x),f.y),
               mix(mix(hash(i+vec3(0,0,1)),hash(i+vec3(1,0,1)),f.x),mix(hash(i+vec3(0,1,1)),hash(i+vec3(1,1,1)),f.x),f.y),f.z);
}
float clouds(vec3 q){return noise(q)*.57+noise(q*2.03+11.7)*.28+noise(q*4.13+25.3)*.15;}
void main(){
    vec4 fp=InverseViewProjection*vec4(texCoord*2.-1.,1,1);
    vec3 ray=normalize(fp.xyz*(fp.w<0.?-1.:1.));
    vec3 background=texture(SceneColor,texCoord).rgb;
    float b=dot(ray,CenterRelative),disc=b*b-dot(CenterRelative,CenterRelative)+Radius*Radius;
    if(disc<=0.)discard;
    float root=sqrt(disc),nearWall=max(.025,b-root),farWall=b+root;
    if(farWall<.025)discard;
    bool inside=length(CenterRelative)<Radius;
    float wall=inside?farWall:nearWall;
    float depth=texture(SceneDepth,texCoord).r,worldDepth=100000.;
    if(depth<.999999){vec4 wp=InverseViewProjection*vec4(texCoord*2.-1.,depth*2.-1.,1);worldDepth=dot(wp.xyz/wp.w,ray);}
    if(!inside&&worldDepth<nearWall)discard;
    vec3 p=ray*wall-CenterRelative,normal=p/max(Radius,.025);
    // Slowly advected pixel clouds, with a soft silhouette rather than stretched texture bands.
    vec3 pixel=floor(normal*150.)/150.;
    vec3 drift=vec3(Time*.018,Time*.004,-Time*.012);
    float bank=clouds(pixel*vec3(3.4,8.2,3.4)+drift);
    float detail=noise(pixel*25.-drift);
    vec3 sky=mix(vec3(.034,.008,.064),vec3(.19,.035,.26),smoothstep(.28,.71,bank));
    sky+=vec3(.14,.025,.19)*smoothstep(.60,.79,bank)*detail;
    sky=mix(sky,vec3(.24,.095,.29),exp(-abs(normal.y)*9.)*.44);
    float fade=Visibility;
    // An irregular rim follows the expanding / contracting sphere instead of a screen flash.
    float edge=pow(1.-abs(dot(normal,ray)),4.);
    float fracture=pow(max(0.,sin(normal.y*48.+bank*17.+Age*8.)),12.);
    if(!inside)sky=mix(vec3(.035,.009,.065),sky,.42)+vec3(.46,.10,.55)*edge*(.35+fracture);
    float alpha=worldDepth>=wall?fade:0.;
    vec4 wallClip=ViewProjection*vec4(ray*wall,1);
    float wallDepth=clamp(wallClip.z/wallClip.w*.5+.5,0.,1.);
    // Opaque barriers also occlude subsequent spells, while early entry / late release stays transparent.
    gl_FragDepth=fade>.98?min(depth,wallDepth):depth;
    vec3 color=mix(background,sky,alpha);
    // Shallow spatial mist: visible terrain and props are preserved, with distance-controlled haze.
    if(inside&&worldDepth<wall){
        vec3 scene=ray*worldDepth-CenterRelative;
        float mist=exp(-max(scene.y,0.)*.65)*(1.-exp(-worldDepth*.022))*fade;
        color=mix(color,vec3(.23,.11,.27),mist*.60);
        float wave=PulseAge>=0.&&PulseAge<.65?Radius*(.15+PulseAge*1.3):0.;
        float ribbon=exp(-pow((length(scene.xz)-wave)/.20,2.))*exp(-abs(scene.y-.12)*5.);
        color+=vec3(.34,.035,.22)*ribbon*max(0.,1.-PulseAge/.65)*fade;
    }
    float opening=1.-Expansion;
    color+=vec3(.29,.045,.34)*edge*fracture*opening*fade;
    fragColor=vec4(color,1)*tint;
    if(any(isnan(fragColor))||any(isinf(fragColor)))discard;
}
