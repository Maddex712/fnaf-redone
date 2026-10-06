package dev.thor.fnaf;

/** Foreground-only intro clock. Gameplay is not started until this completes. */
final class NightIntro {
    static final int[] CARDS={453,454,472,473,474,446,538};
    int stage; // 0 idle, 1 newspaper, 2 night card, 3 black lead-in, 4 title fade
    boolean newspaper;
    double elapsed;
    void begin(boolean paper){newspaper=paper;stage=4;elapsed=0;}
    void beginNextNight(){newspaper=false;stage=2;elapsed=0;}
    boolean active(){return stage!=0;}
    void cancel(){stage=0;elapsed=0;}
    void next(){stage=stage==4?(newspaper?1:2):stage==3?0:stage+1;elapsed=0;}
    void skipPaper(){if(stage==1&&elapsed>=.45)elapsed=Math.max(elapsed,5.68);}
    float opacity(){
        if(stage==4)return (float)Math.max(0,1-elapsed/.35);
        if(stage==3||stage==0)return 0;
        double in=stage==1?.45:.12,out=.12,duration=stage==1?5.8:2.7;
        return (float)Math.max(0,Math.min(1,Math.min(elapsed/in,(duration-elapsed)/out)));
    }
    boolean tick(double dt){
        if(!active())return false;
        elapsed+=Math.max(0,Math.min(.25,dt));
        double duration=stage==4?.35:stage==1?5.8:stage==2?2.7:.04;
        if(elapsed>=duration){next();return !active();}
        return false;
    }
}

