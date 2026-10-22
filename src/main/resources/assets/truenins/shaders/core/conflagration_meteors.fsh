#version 150
uniform sampler2D SceneColor,SceneDepth;
uniform mat4 InverseViewProjection;
uniform vec3 CenterRelative;
uniform float Age,Time,FeedbackScale,BlastRadius;
uniform vec3 Breakthrough;
uniform int Count;
// Visibility is the whole-pass fade.  Without it the layer could only ever pop in and out
// at scene start/end even though every other scene pass fades.
uniform float Visibility;
uniform vec2 ScreenSize;
// Each slot is a bounded volume; uniform vectors avoid driver-dependent array uploads.
uniform vec4 Head0,Head1,Head2,Head3,Head4,Head5,Head6,Head7,Head8,Head9,Head10,Head11,Head12,Head13,Head14,Head15,Head16,Head17,Head18,Head19;
uniform vec4 Tail0,Tail1,Tail2,Tail3,Tail4,Tail5,Tail6,Tail7,Tail8,Tail9,Tail10,Tail11,Tail12,Tail13,Tail14,Tail15,Tail16,Tail17,Tail18,Tail19;
uniform float Seed0,Seed1,Seed2,Seed3,Seed4,Seed5,Seed6,Seed7,Seed8,Seed9,Seed10,Seed11,Seed12,Seed13,Seed14,Seed15,Seed16,Seed17,Seed18,Seed19;
in vec2 texCoord;
in vec4 tint;
out vec4 fragColor;
float hash(vec3 p){p=fract(p*.1031);p+=dot(p,p.yzx+33.33);return fract((p.x+p.y)*p.z);}
float noise(vec3 p){vec3 i=floor(p),f=fract(p);f=f*f*(3.-2.*f);return mix(mix(mix(hash(i),hash(i+vec3(1,0,0)),f.x),mix(hash(i+vec3(0,1,0)),hash(i+vec3(1,1,0)),f.x),f.y),mix(mix(hash(i+vec3(0,0,1)),hash(i+vec3(1,0,1)),f.x),mix(hash(i+vec3(0,1,1)),hash(i+vec3(1,1,1)),f.x),f.y),f.z);}
float fbm(vec3 p){return noise(p)*.6+noise(p*2.1+13.)*.28+noise(p*4.2-8.)*.12;}
vec2 sphere(vec3 ro,vec3 rd,float r){float b=dot(ro,rd),d=b*b-dot(ro,ro)+r*r;return d<0.?vec2(1e6,-1e6):vec2(-b-sqrt(d),-b+sqrt(d));}
// Pixel footprint of a world radius at a view distance.  Used only to spend fewer relaxation
// steps where a stone or wake covers a few pixels; nothing above ~140px is ever reduced.
float coverage(float radius,float dist){return radius/max(dist,.001)*ScreenSize.y*.72;}
vec3 heat(float h){return mix(vec3(.7,.045,.003),vec3(2.,.57,.045),smoothstep(.1,.65,h))+vec3(.55,.8,1.)*pow(clamp(h,0.,1.),6.);}
// Small stones use angular rock planes; the reference finale has a regular round cap.
// Keep silhouette relief shallow there and put the detail into fractured surface layers.
float rockPlanes(vec3 q){
    vec3 a=abs(q);
    float d=max(dot(a,vec3(.78,.49,.37)),dot(a,vec3(.35,.62,.76)));
    d=max(d,dot(a,vec3(.65,.29,.70)));
    d=max(d,dot(a,vec3(.20,.94,.27)));
    return max(d,max(a.x*.95,a.z*.96))-1.;
}
float rockField(vec3 p,float seed,bool giant,vec3 axis){
    if(giant){
        vec3 q=p/vec3(1.32,1.,1.26);
        float bank=noise(q*6.+seed*.017);
        float chips=noise(q*34.+seed*.11);
        return length(q)-1.-(bank-.5)*.045-(chips-.5)*.009;
    }
    vec3 q=(p-axis*dot(p,axis)*.26)*1.16;
    return rockPlanes(q)-(noise(q*4.5+seed*.017)-.5)*.20;
}
// Distance to a fracture between neighbouring rock plates. These form broken straight
// seams, unlike an isocontour of FBM, which reads as luminous scribbles on a balloon.
vec2 fractures(vec3 p){
    vec3 cell=floor(p),f=fract(p);float first=10.,second=10.,id=0.;
    for(int z=-1;z<=1;z++)for(int y=-1;y<=1;y++)for(int x=-1;x<=1;x++){
        vec3 offset=vec3(x,y,z),key=cell+offset;
        float h=hash(key);
        vec3 point=offset+vec3(h,hash(key+17.),hash(key-31.))*.8+.1-f;
        float d=dot(point,point);
        if(d<first){second=first;first=d;id=h;}else second=min(second,d);
    }
    return vec2(sqrt(second)-sqrt(first),id);
}
vec3 flameColor(float h){
    vec3 c=mix(vec3(.65,.065,.008),vec3(1.9,.72,.045),smoothstep(.08,.60,h));
    return mix(c,vec3(3.6,2.9,1.8),smoothstep(.66,.98,h));
}
void main(){
    vec4 heads[20]=vec4[20](Head0,Head1,Head2,Head3,Head4,Head5,Head6,Head7,Head8,Head9,Head10,Head11,Head12,Head13,Head14,Head15,Head16,Head17,Head18,Head19);
    vec4 tails[20]=vec4[20](Tail0,Tail1,Tail2,Tail3,Tail4,Tail5,Tail6,Tail7,Tail8,Tail9,Tail10,Tail11,Tail12,Tail13,Tail14,Tail15,Tail16,Tail17,Tail18,Tail19);
    float seeds[20]=float[20](Seed0,Seed1,Seed2,Seed3,Seed4,Seed5,Seed6,Seed7,Seed8,Seed9,Seed10,Seed11,Seed12,Seed13,Seed14,Seed15,Seed16,Seed17,Seed18,Seed19);
    vec4 fp=InverseViewProjection*vec4(texCoord*2.-1.,1,1);
    vec3 ray=normalize(fp.xyz*(fp.w<0.?-1.:1.));
    float depth=texture(SceneDepth,texCoord).r,world=100000.;
    if(depth<.999999){vec4 p=InverseViewProjection*vec4(texCoord*2.-1.,depth*2.-1.,1);world=max(0.,dot(p.xyz/p.w,ray));}
    vec3 base=texture(SceneColor,texCoord).rgb,emission=vec3(0);float opacity=0.,flash=0.,scorch=0.;
    float solidDepth=world;
    vec3 giantHead=vec3(0);float giantSize=0.;
    for(int k=0;k<20;k++)if(k<Count&&heads[k].w>10.&&tails[k].w>=0.){giantHead=heads[k].xyz;giantSize=heads[k].w;}
    for(int i=0;i<20;i++) {
        if(i>=Count)break;
        vec3 head=heads[i].xyz;float size=heads[i].w,seed=seeds[i];
        bool giant=size>10.;float phase=tails[i].w;
        if(phase<0.) {
            float dt=-phase-.001,life=giant?2.8:1.2;
            // Visual body size is independent of the unchanged ground explosion choreography.
            float blastSize=giant?BlastRadius*.72:size;
            float wave=dt*(giant?38.:13.);vec3 contact=head-vec3(0,giant?size:0.,0);
            float radius=blastSize*(giant?1.5:2.)+dt*(giant?13.:4.);
            float height=.55+dt*.3;
            // Enclose the whole plume, avoiding a sharply clipped translucent bubble.
            vec2 bound=sphere(-contact,ray,max(wave+blastSize*.8+3.,radius*max(1.,height)*1.2));
            if(bound.y<0.||bound.x>world||bound.x>bound.y)continue;
            // Ring lies in world space and disappears behind terrain / buildings.
            float plane=abs(ray.y)>.0001?(contact.y+.22)/ray.y:-1.;
            if(plane>0.&&plane<world+.5) {
                vec3 p=ray*plane-contact;float r=length(p.xz);
                float ring=exp(-pow((r-wave)/max(.4,blastSize*.10+dt*.7),2.))*(1.-dt/life);
                emission+=vec3(1.8,.65,.12)*ring*(giant?1.4:.7);
                // A second, faster and thinner front trails the first.  Analytic, so it costs one
                // exp instead of widening the march.
                float front=wave*1.62;
                float ring2=exp(-pow((r-front)/max(.28,blastSize*.055+dt*.42),2.))*(1.-smoothstep(0.,life*.9,dt));
                emission+=vec3(1.45,.92,.52)*ring2*(giant?1.15:.42);
                // Charred ground: outlives the flash, so a distant impact still reads as an impact
                // rather than as a puff of additive smoke over untouched grass.
                float scorchR=(giant?BlastRadius*.92:size*3.6)*(.42+dt*.34);
                float stain=1.-smoothstep(.30,1.,r/max(scorchR,.001)+(noise(vec3(p.xz*.16,seed*.01))-.5)*.55);
                scorch=max(scorch,stain*(1.-smoothstep(life*.5,life*1.5,dt))*(giant?.9:.55));
            }
            float t=max(.02,bound.x),stepSize=(min(bound.y,world)-t)/12.;
            if(stepSize<=0.)continue;
            vec3 smoke=vec3(0);float density=0.;
            for(int j=0;j<12;j++) {
                vec3 p=ray*(t+(float(j)+.5)*stepSize)-contact;
                float plume=length(p/vec3(1.,height,1.))/max(radius,.1);
                if(plume>1.1)continue;
                float n=fbm(p/max(blastSize*.35,.5)+vec3(seed,Age*1.4,0));
                float d=(1.-smoothstep(.3,1.,plume+n*.4))*exp(-dt*(giant?1.1:1.6));
                float a=1.-exp(-d*stepSize*.18);
                vec3 c=mix(vec3(.040,.028,.022),heat(n+exp(-dt*3.)*.7),exp(-dt*(giant?1.5:2.))*.8);
                smoke+=(1.-density)*a*c;density+=(1.-density)*a;
            }
            emission+=smoke;opacity=max(opacity,density*.85);
            if(giant&&dt<1.6){
                // A torn pressure shell expands through the air around the fireball.
                float shockRadius=BlastRadius*.22+dt*58.;
                vec2 shock=sphere(-(contact+vec3(0,BlastRadius*.16,0)),ray,shockRadius);
                float t=max(.02,shock.x);
                if(shock.y>t&&t<world){
                    vec3 normal=(ray*t-contact-vec3(0,BlastRadius*.16,0))/shockRadius;
                    float edge=pow(1.-abs(dot(normal,ray)),5.);
                    float torn=smoothstep(.25,.68,noise(normal*19.+Age*2.));
                    emission+=vec3(2.3,1.65,.65)*edge*torn*exp(-dt*3.2);
                }
            }
            if(giant&&dt<2.2) {
                // Six hot rock slabs peel out ballistically, then cool and fall.
                for(int k=0;k<6;k++) {
                    float a=float(k)*2.39996+seed;
                    vec3 fragment=contact+vec3(cos(a)*dt*18.,dt*23.-dt*dt*12.,sin(a)*dt*18.);
                    vec2 shard=sphere(-fragment,ray,max(.15,1.5-dt*.55));
                    if(shard.y>0.&&shard.x<world&&shard.x<shard.y) {
                        vec3 p=normalize(ray*max(.01,shard.x)-fragment);
                        float rough=noise(floor(p*12.)+seed);
                        emission+=mix(vec3(.09,.025,.015),vec3(1.8,.45,.035),rough*exp(-dt));
                        opacity=1.;
                    }
                }
            }
            if(giant)flash=max(flash,(exp(-dt*21.)+exp(-pow((dt-.14)/.065,2.))*.28)*clamp(1.-length(contact)/180.,0.,1.));
            continue;
        }
        vec3 tail=tails[i].xyz,axis=normalize(tail);float tailLength=length(tail);
        // Broad bound first, then raymarch only pixels intersecting the head / tapered wake.
        vec3 mid=head+tail*.4;float boundRadius=tailLength*.52+size*1.85;
        vec2 bound=sphere(-mid,ray,boundRadius);
        float start=max(.025,bound.x),end=min(world,bound.y);
        if(end<=start)continue;
        // Cheap ray/segment rejection before evaluating turbulent material.
        float ad=dot(axis,ray),denom=max(.0001,1.-ad*ad);
        float alongClosest=clamp((ad*dot(ray,head)-dot(axis,head))/denom,0.,tailLength);
        vec3 closest=head+axis*alongClosest;
        float rayClosest=max(0.,dot(closest,ray));
        if(length(ray*rayClosest-closest)>size*1.9)continue;
        if(!giant){
            // Tight finite-cylinder interval: sampling the large enclosing sphere skipped
            // thin wakes entirely at many view angles, leaving isolated round black heads.
            vec3 ro=-head;float ao=dot(ro,axis);
            float b=dot(ro,ray)-ao*ad,c=dot(ro,ro)-ao*ao-size*size*3.61;
            float disc=b*b-denom*c;
            if(disc<0.)continue;
            start=max(start,(-b-sqrt(disc))/denom);end=min(end,(-b+sqrt(disc))/denom);
            if(abs(ad)>.001){
                float cap0=(-size*1.6-ao)/ad,cap1=(tailLength+size-ao)/ad;
                start=max(start,min(cap0,cap1));end=min(end,max(cap0,cap1));
            }
            if(end<=start)continue;
        }
        vec2 core=sphere(-head,ray,size*(giant?1.95:1.65));
        vec3 col=vec3(0);float alpha=0.;
        if(core.y>0.&&core.x<world) {
            float t=max(.025,core.x);bool hit=false;
            // A stone covering a handful of pixels keeps the same silhouette but needs far fewer
            // relaxation steps to reach it.  Above ~140px the full 40-step budget is untouched.
            float corePx=coverage(size*1.85,max(core.x,.5));
            float coreFrac=corePx>140.?1.:corePx>60.?.78:corePx>26.?.58:.42;
            // Displace the actual silhouette, rather than only painting cracks onto a smooth ball.
            for(int k=0;k<40;k++) {
                if(float(k)>=40.*coreFrac)break;
                vec3 local=(ray*t-head)/size;
                float rock=rockField(local,seed,giant,axis);
                if(t>min(world,core.y))break;
                if(rock<.0006){hit=true;break;}
                t+=max(.001,rock*size*.65);
            }
            if(hit&&t<min(world,core.y)) {
            core.x=t;vec3 p=(ray*t-head)/size;
            solidDepth=min(solidDepth,t);
            // Continuous multiscale stone, with subtle pixel grain rather than giant
            // quantised crack blocks. Lit surface relief replaces the uniform neon rim.
            float e=giant?.009:.018;
            float field=rockField(p,seed,giant,axis);
            vec3 normal=normalize(vec3(rockField(p+vec3(e,0,0),seed,giant,axis),
                rockField(p+vec3(0,e,0),seed,giant,axis),rockField(p+vec3(0,0,e),seed,giant,axis))-field);
            vec3 q=p*(giant?6.4:3.5)+seed*.017;
            if(giant)q+=vec3(noise(q*1.7),noise(q*1.7+13.),noise(q*1.7-9.))*1.15;
            vec2 seam=fractures(q);
            float plate=fbm(q*2.3),grain=noise(floor(p*(giant?480.:90.))+seed);
            float cracks=1.-smoothstep(.006,.018,seam.x);
            float active=smoothstep(.33,.69,noise(q*.53));
            float light=max(0.,dot(normal,normalize(vec3(-.35,-.80,.40))));
            float edge=pow(1.-abs(dot(normal,ray)),3.);
            float under=1.-smoothstep(-.95,.25,p.y);
            vec3 stone=mix(vec3(.008,.009,.014),vec3(.050,.039,.043),seam.y*.50+plate*.50);
            col=stone*(.60+light*1.0)+vec3(.012,.012,.015)*grain;
            col+=flameColor(.60)*cracks*active*(giant?.10:.24);
            if(giant){
                // Branching hairline cracks cover the rock between the deeper main seams.
                // Main fractures have a red halo, a gold channel and a thin white-hot core.
                vec2 fine=fractures(p*25.+vec3(noise(p*31.),noise(p*31.+7.),noise(p*31.-4.))*1.2+seed*.023);
                float branch=(1.-smoothstep(.002,.006,fine.x))*(.14+active*.36);
                float halo=exp(-seam.x*31.)*(.20+active*.30);
                float channel=(1.-smoothstep(.0025,.008,seam.x))*(.22+active*.42);
                float core=(1.-smoothstep(.001,.0038,seam.x))*(.10+active*.28);
                col+=vec3(.30,.022,.005)*halo;
                col+=vec3(.82,.19,.024)*channel*(.22+under*.35);
                col+=vec3(1.45,.64,.18)*core*(.10+under*.30);
                col+=vec3(.27,.045,.009)*branch;
                // The bottommost section becomes incandescent as the round body presses down.
                float tip=pow(clamp((-p.y-.69)/.31,0.,1.),8.);
                float burn=tip*(.35+plate*.65);
                col+=vec3(1.15,.38,.095)*burn;
            }
            // Heat lives on exposed edges and a few opened seams; rock plates stay dark.
            col+=vec3(.74,.17,.022)*edge*(.2+plate*.8)*under;
            col+=vec3(.16,.035,.008)*under*active;
            alpha=1.;
            }
        }
        // Integrate turbulence around the core and through the long wake.
        // Same rule for the wake: fewer samples when it is only a few pixels wide, with the
        // integrated interval and every colour term unchanged.
        float wakePx=coverage(size*1.6,max(start,.5));
        float wakeFrac=wakePx>110.?1.:wakePx>48.?.74:wakePx>18.?.52:.30;
        float stepSize=(end-start)/(28.*wakeFrac);vec3 flames=vec3(0);float flameAlpha=0.;
        for(int j=0;j<28;j++) {
            if(float(j)>=28.*wakeFrac)break;
            float t=start+(float(j)+.5)*stepSize;
            if(alpha>.99&&t>core.x)break;
            vec3 p=ray*t-head;float along=dot(p,axis),u=clamp(along/tailLength,0.,1.);
            float width=size*(1.24-u*1.10);
            vec3 side=normalize(cross(axis,abs(axis.y)>.95?vec3(1,0,0):vec3(0,1,0)));
            vec3 across=cross(axis,side);
            float flutter=sin(along/max(size*.7,.4)-Age*19.+seed)*u;
            vec3 wake=p-axis*clamp(along,0.,tailLength)-side*flutter*width*.28;
            float radial=length(wake);
            if(radial>size*1.8&&length(p)>size*1.8)continue;
            float n=fbm(p/max(size*.42,.32)-axis*Age*12.+seed);
            float d,hot;
            if(giant){
                vec3 local=p/size;
                float shell=rockField(local,seed,true,axis);
                // Irregular incandescent skin and torn vapour in front of the dark mass.
                float skin=exp(-abs(shell-.020-(n-.5)*.022)*70.);
                float lower=1.-smoothstep(-.85,.20,local.y);
                float tongues=smoothstep(.76,.94,n);
                d=skin*(.002+tongues*.045)*(.05+lower*.50);
                hot=clamp(.30+n*.42+lower*.16,0.,1.);
            }else{
                float warp=(n-.5)*width*.75;
                d=1.-smoothstep(width*.10,width*1.2,radial+warp);
                d*=smoothstep(-size*1.35,-size*.15,along)*(1.-smoothstep(tailLength*.78,tailLength,along));
                float ribbons=.55+.45*sin(dot(wake,across)/max(width,.1)*15.+along*.7-Age*22.+seed);
                d*=mix(.3,1.,smoothstep(.25,.69,n))*ribbons;
                // A white-gold ablation hood partly hides the rock; the long tail breaks
                // into narrow fast filaments instead of ending in a flat red triangle.
                float hood=exp(-pow((length(p/vec3(1.12))-size*.98)/max(.1,size*.22),2.));
                d+=hood*.55*smoothstep(.3,.64,n);
                hot=clamp(1.02-radial/max(width,.1)*.49+n*.22-u*.12,0.,1.);
            }
            float a=1.-exp(-d*stepSize/max(.6,size*(giant?.34:.5)));
            flames+=(1.-flameAlpha)*flameColor(hot)*a*(giant?.20:1.3);
            // Ember filaments ride inside the wake.  This reuses the turbulence sample that was
            // already taken, so the tail reads hotter without an extra march.
            if(!giant)flames+=(1.-flameAlpha)*vec3(1.55,.44,.07)*a*pow(max(0.,n-.70)*3.3,2.)*.55;
            flameAlpha+=(1.-flameAlpha)*a;
        }
        vec3 combined=flames+(1.-flameAlpha)*col*alpha;
        float a=flameAlpha+(1.-flameAlpha)*alpha;
        // Selected slots arrive back to front; opaque rock hides earlier distant fireballs.
        emission=combined+(1.-a)*emission;opacity=a+(1.-a)*opacity;
    }
    base*=1.-clamp(scorch,0.,.88);
    vec3 color=base*(1.-opacity)+emission;
    // A physical cloud deck hides the enormous upper body. March only between camera and
    // nearest opaque surface, so the underside breaks THROUGH clouds rather than overpainting them.
    float fade=smoothstep(0.,3.2,Age)*(1.-smoothstep(12.4,15.,Age));
    // How far the giant has punched through the deck.  Widening the aperture is what lets the
    // sky's light spill down to the field instead of stopping at the cloud base.
    float breach=smoothstep(0.,1.2,Age-Breakthrough.x-.30);
    float lower=CenterRelative.y+43.,upper=CenterRelative.y+68.;
    if(abs(ray.y)>.001){
        float t0=lower/ray.y,t1=upper/ray.y;
        float begin=max(.05,min(t0,t1)),end=min(solidDepth,max(t0,t1));
        if(end>begin){
            float stride=(end-begin)/16.;vec3 cloudLight=vec3(0);float cloudAlpha=0.;
            // Fixed screen-space stratification breaks up the flat, repeated sampling planes
            // at grazing angles. No extra march samples or time-varying grain are required.
            float jitter=.15+.70*hash(vec3(floor(gl_FragCoord.xy),19.));
            for(int j=0;j<16;j++){
                vec3 samplePoint=ray*(begin+(float(j)+jitter)*stride);
                vec3 p=samplePoint-CenterRelative;
                float radius=length(p.xz);
                if(radius>380.)continue;
                // Large banks with smaller billows, rather than a uniformly tinted ceiling.
                // One warped FBM plus a single detail octave keeps cloud cost bounded.
                vec3 flow=vec3(p.x*.040+Age*.055,p.y*.083,p.z*.040-Age*.035);
                float bank=noise(flow*.42);
                float n=fbm(flow+bank*1.7);
                float detail=noise(flow*3.1+vec3(0,-Age*.11,0));
                float bottom=43.+(n-.5)*23.;
                float skin=smoothstep(bottom,bottom+10.,p.y);
                float d=skin*(1.-smoothstep(63.,68.,p.y));
                // A dense upper bank prevents the whole meteor appearing through random holes.
                d*=mix(.32,1.,smoothstep(.24,.68,n))*(1.-smoothstep(260.,380.,radius));
                // The rain falls through a ragged cloud eye. Its aperture is narrower than the
                // giant, leaving the enormous upper shell hidden behind the surrounding banks.
                float eye=smoothstep(22.-breach*12.,48.-breach*9.,radius+(n-.5)*18.);
                d*=mix(.065,1.,eye);
                float edgeLight=0.;
                if(giantSize>0.){
                    vec3 q=(samplePoint-giantHead)/vec3(1.32,1.,1.26);
                    float shell=length(q)-giantSize;
                    // Displace/evaporate clouds around the hot lower shell; gold light at the rim.
                    d*=smoothstep(-2.,5.,shell);
                    edgeLight=exp(-abs(shell)/16.)*smoothstep(0.,1.,(Age-9.)/2.)*(1.+breach*.55);
                }
                float a=(1.-exp(-d*stride*.36))*fade;
                float relief=clamp((n-.28)*1.5+(1.-skin)*.30,0.,1.);
                vec3 c=mix(vec3(.018,.022,.039),vec3(.145,.128,.180),relief);
                // Cool violet-grey relief, with gold confined to openings and the breach.
                float underlight=pow(1.-skin,2.)*(.35+detail*.65);
                float eyeRim=exp(-pow((radius-32.+(n-.5)*18.)/17.,2.));
                float sunward=exp(-radius/80.);
                c+=vec3(.075,.077,.125)*underlight*(.3+detail*.5);
                c+=vec3(.80,.49,.18)*eyeRim*(.10+underlight*.24)*(1.+breach*.5);
                c+=vec3(.32,.21,.10)*sunward*pow(detail,3.)*underlight;
                c+=vec3(1.35,.85,.27)*edgeLight*(.25+underlight*.6);
                cloudLight+=(1.-cloudAlpha)*a*c;cloudAlpha+=(1.-cloudAlpha)*a;
                if(cloudAlpha>.985)break;
            }
            color=color*(1.-cloudAlpha)+cloudLight;
        }
    }
    // Five soft sun shafts, evaluated as bounded world-space beams. Analytic closest
    // approach avoids both horizontal march bands and visible stochastic grain.
    if(ray.y>.025){
        vec3 lightAxis=normalize(vec3(-.28,-1.,.16));
        float ad=dot(lightAxis,ray),denom=max(.001,1.-ad*ad);
        for(int j=0;j<5;j++){
            float angle=float(j)*2.39996+Age*.009;
            float radius=29.+float(j)*7.;
            vec3 source=CenterRelative+vec3(cos(angle)*radius,43.,sin(angle)*radius);
            float along=clamp((ad*dot(ray,source)-dot(lightAxis,source))/denom,0.,40.);
            vec3 closest=source+lightAxis*along;
            float t=dot(ray,closest);
            if(t<=0.||t>=solidDepth)continue;
            float width=2.8+along*.085;
            float distance=length(ray*t-closest)/width;
            float beam=exp(-distance*distance*2.)*smoothstep(0.,5.,along)*(1.-smoothstep(24.,40.,along));
            color+=vec3(.16,.105,.038)*beam*fade*(1.+breach*.85);
        }
    }
    // Three staggered pressure fronts tear free of the cloud underside. Analytic plane
    // intersections keep the cost fixed; world depth and the rock surface occlude them.
    // Ragged vapour skirts surround a narrow hot lip, rather than solid neon hoops.
    if(giantSize>0.&&abs(ray.y)>.001){
        for(int j=0;j<3;j++){
            if(float(j)>=Breakthrough.z)break;
            float dt=Age-Breakthrough.x-float(j)*Breakthrough.y;
            if(dt<=0.||dt>=1.65)continue;
            float height=CenterRelative.y+43.-float(j)*5.-dt*6.;
            float t=height/ray.y;
            if(t<=0.||t>=solidDepth)continue;
            vec2 p=(ray*t-giantHead).xz;
            float radius=giantSize*.43+dt*78.;
            float radial=length(p);
            // Reject pixels away from the front before evaluating its torn edges.
            if(abs(radial-radius)>20.+dt*7.)continue;
            vec3 flow=vec3(p*.12,float(j)*19.+dt*1.5);
            float n=noise(flow),detail=noise(flow*3.7+11.);
            float offset=radial-radius+(n-.5)*9.+(detail-.5)*2.;
            float life=smoothstep(0.,.08,dt)*(1.-smoothstep(.60,1.65,dt));
            float rim=exp(-pow(offset/(.7+dt*1.7),2.));
            float vapour=exp(-pow((offset+3.)/(3.5+dt*3.5),2.));
            float torn=smoothstep(.22,.62,n*.75+detail*.25);
            float wisps=pow(detail,3.)*exp(-abs(offset+7.)/(4.+dt*4.));
            float veil=(vapour*.22+wisps*.12)*life*torn;
            color=mix(color,vec3(.29,.20,.17),veil);
            color+=mix(vec3(1.4,.32,.07),vec3(2.1,1.65,.88),detail)
                *(rim*.48+vapour*.09+wisps*.12)*torn*life;
        }
    }
    color=mix(color,vec3(1.15,1.03,.84),clamp(flash*.78*FeedbackScale,0.,.9));
    if(any(isnan(color))||any(isinf(color)))discard;
    // Alpha carries Visibility as well: the blend is (src, 1-srcAlpha), so this is what makes
    // the layer recede to the scene underneath instead of fading to black.
    fragColor=vec4(color,1)*tint*Visibility;
}
