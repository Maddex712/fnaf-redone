package dev.thor.fnaf;
import java.util.Random;
public final class TitleEffectsTest {
    static int checks;
    static void check(boolean v,String s){checks++;if(!v)throw new AssertionError(s);}
    static void near(float v,float e,String s){check(Math.abs(v-e)<.002,s);}
    public static void main(String[] args){
        near(TitleEffects.goldenOpacity(29999),0,"Initially hidden");
        near(TitleEffects.goldenOpacity(30000),0,"Entrance starts invisible");
        near(TitleEffects.goldenOpacity(30250),.5f,"Entrance midpoint");
        near(TitleEffects.goldenOpacity(30500),1,"Entrance completes");
        near(TitleEffects.fredbearMix(32000),0,"Golden held before dissolve");
        near(TitleEffects.fredbearMix(32250),.5f,"Variant dissolve midpoint");
        near(TitleEffects.fredbearMix(32500),1,"Fredbear fully visible");
        for(long t=32500;t<=33500;t+=100){near(TitleEffects.goldenOpacity(t),1,"Fredbear one-second hold");near(TitleEffects.fredbearMix(t),1,"Fredbear pose held");}
        near(TitleEffects.goldenOpacity(33750),.5f,"Exit midpoint");
        near(TitleEffects.goldenOpacity(34000),0,"Gone after four seconds");
        near(TitleEffects.goldenOpacity(60250),.5f,"Repeats at 30 seconds");
        near(TitleEffects.randomEyeVisibility(29200),1,"Pre-arrival fade starts smoothly");
        near(TitleEffects.randomEyeVisibility(29600),.5f,"Pre-arrival fade midpoint");
        near(TitleEffects.randomEyeVisibility(30000),0,"Other eyes dark at arrival");
        for(long t=30000;t<=32000;t+=10)for(int eye=0;eye<3;eye++)
            near(TitleEffects.eyeOpacity(t,eye,eye,1),0,"No random white eyes during Golden Freddy");
        near(TitleEffects.groupEyeOpacity(32000),0,"Group fade starts with Fredbear");
        near(TitleEffects.groupEyeOpacity(32250),.5f,"Group fade-in midpoint");
        near(TitleEffects.groupEyeOpacity(32500),1,"Group fully visible at hold");
        near(TitleEffects.groupEyeOpacity(33499),1,"Full one-second hold");
        near(TitleEffects.groupEyeOpacity(33750),.5f,"Group fades out with Fredbear");
        near(TitleEffects.groupEyeOpacity(34000),0,"Group exits smoothly");
        near(TitleEffects.randomEyeVisibility(34400),.5f,"Random eyes ease back in");
        near(TitleEffects.groupEyeOpacity(2500),0,"No premature group event");
        near(TitleEffects.groupEyeOpacity(62250),.5f,"Group fade repeats at 30 seconds");
        for(int eye=0;eye<3;eye++){
            float previous=0;
            for(long t=28000;t<36000;t++){
                float value=TitleEffects.eyeOpacity(t,eye,1,.7f);
                if(t>28000)check(Math.abs(value-previous)<.004,"No hard cuts across apparition boundaries");
                previous=value;
            }
        }
        near(TitleEffects.goldenOpacity(45000),0,"No old fifteen-second appearance");
        for(int seed=0;seed<30;seed++){
            TitleEffects e=new TitleEffects(new Random(seed));int last=-1,mask=0;long start=-1,fullStart=-1;int fullSamples=0,events=0;boolean nonLinear=false;int[] history=new int[4];
            for(long t=0;t<240000;t+=10){
                float v=e.update(t);
                if(e.eye!=last){
                    if(last>=0){check(fullSamples==301,"Exactly three full seconds at full intensity");if(e.eye!=(last+1)%3)nonLinear=true;}
                    last=e.eye;fullSamples=0;history[events%4]=e.eye;events++;mask|=1<<e.eye;
                    if(events>=4){int recent=0;for(int socket:history)recent|=1<<socket;check(recent==7,"Every four events include all sockets");}
                }
                if(v==1)fullSamples++;
                if(v<0||v>1)throw new AssertionError("Invalid opacity");
            }
            check(mask==7,"All sockets reachable");check(nonLinear,"Random order, not fixed rotation");check(events>20,"Repeated events exercised");
            near(e.update(999999),0,"Return after clock jump fades in");
        }
        System.out.println("Title effects: "+checks+" checks passed");
    }
}

