package dev.thor.fnaf;

/** Foreground-only victory sequence. Event bits are emitted once at each boundary. */
final class NightOutro {
    static final double ROLL=3.0,CELEBRATE=3.4,ENDING=15;
    static final int CHEER=1,SHOW_ENDING=2,DONE=4;
    int stage,night; boolean campaign,cheered; double elapsed;
    void begin(int n,boolean normal){night=n;campaign=normal;stage=1;elapsed=0;cheered=false;}
    boolean active(){return stage!=0;}
    int endingImage(){return !campaign? -1:night==5?210:night==6?522:night==7?523:-1;}
    float roll(){return (float)Math.min(1,elapsed/ROLL);}
    void cancel(){stage=0;elapsed=0;}
    int tick(double dt){
        return tick(dt,true);
    }
    int tick(double dt,boolean victoryAudioFinished){
        if(!active())return 0;
        elapsed+=Math.max(0,Math.min(.25,dt));int events=0;
        if(stage==1){
            if(!cheered&&elapsed>=ROLL){cheered=true;events|=CHEER;}
            if(elapsed>=ROLL+CELEBRATE&&victoryAudioFinished){
                if(endingImage()>=0){stage=2;elapsed=0;events|=SHOW_ENDING;}
                else{stage=0;events|=DONE;}
            }
        }else if(elapsed>=ENDING){stage=0;events|=DONE;}
        return events;
    }
    boolean skipEnding(){if(stage!=2||elapsed<1)return false;cancel();return true;}
    static int starMask(Progress progress,int unlocked){
        return ((progress.earned(4)||unlocked>=6)?1:0)|((progress.earned(5)||unlocked>=7)?2:0)|(progress.earned(9)?4:0);
    }
}
