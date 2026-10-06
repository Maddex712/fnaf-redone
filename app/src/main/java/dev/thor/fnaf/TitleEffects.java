package dev.thor.fnaf;
import java.util.Random;
/** Menu effects in milliseconds, independent of game/save state. */
final class TitleEffects {
    private final Random random;
    private long next,start=-1;
    int eye=-1;
    private int previousEye=-1,olderEye=-1;
    static final long GOLDEN_PERIOD=30000;
    static final long EYE_FADE=800,EYE_HOLD=3000,EYE_TOTAL=EYE_HOLD+2*EYE_FADE;
    TitleEffects(){this(new Random());}
    TitleEffects(Random random){this.random=random;next=1200+random.nextInt(2500);}
    static float smooth(float x){x=Math.max(0,Math.min(1,x));return x*x*(3-2*x);}
    static float goldenOpacity(long elapsed){
        if(elapsed<GOLDEN_PERIOD)return 0;
        long p=elapsed%GOLDEN_PERIOD;
        if(p<500)return smooth(p/500f);
        if(p<3500)return 1;
        return p<4000?1-smooth((p-3500)/500f):0;
    }
    static float fredbearMix(long elapsed){
        if(elapsed<GOLDEN_PERIOD)return 0;
        long p=elapsed%GOLDEN_PERIOD;
        return smooth((p-2000)/500f);
    }
    static float groupEyeOpacity(long elapsed){
        // Rise with Fredbear's dissolve, hold for his full second, fade with his exit.
        return goldenOpacity(elapsed)*fredbearMix(elapsed);
    }
    static float randomEyeVisibility(long elapsed){
        long phase=elapsed%GOLDEN_PERIOD;
        if(phase>=GOLDEN_PERIOD-EYE_FADE)
            return 1-smooth((phase-(GOLDEN_PERIOD-EYE_FADE))/(float)EYE_FADE);
        if(elapsed<GOLDEN_PERIOD)return 1;
        if(phase<4000)return 0;
        return smooth((phase-4000)/(float)EYE_FADE);
    }
    static float eyeOpacity(long elapsed,int socket,int selected,float intensity){
        return Math.max(groupEyeOpacity(elapsed),socket==selected?intensity*randomEyeVisibility(elapsed):0);
    }
    float update(long elapsed){
        if(elapsed>=next){
            // Uniformly choose either of the two other sockets: random, never an immediate repeat.
            int choice=random.nextInt(eye<0?3:2);
            int selected=eye<0?choice:(eye+1+choice)%3;
            // Prevent a long A-B-A-B alternation too: every four events include all three.
            if(eye>=0&&eye==olderEye)selected=3-eye-previousEye;
            olderEye=previousEye;previousEye=eye;eye=selected;
            start=elapsed;next=start+EYE_TOTAL+1200+random.nextInt(4200);
        }
        if(start<0)return 0;
        long age=elapsed-start;
        if(age<EYE_FADE)return smooth(age/(float)EYE_FADE);
        if(age<EYE_FADE+EYE_HOLD)return 1;
        return 1-smooth((age-EYE_FADE-EYE_HOLD)/(float)EYE_FADE);
    }
}
