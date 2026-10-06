package dev.thor.fnaf;

/** Saved preference; absent/disconnected secondary displays always use one viewport. */
final class ScreenMode {
    static final int AUTO=0,SINGLE=1,DUAL=2;
    static final String[] LABELS={"Auto","Single screen","Dual screen"};
    static int valid(int mode){return mode>=AUTO&&mode<=DUAL?mode:AUTO;}
    static boolean useSecondDisplay(int mode,boolean available){return valid(mode)!=SINGLE&&available;}
    static boolean showCamera(boolean monitor,int scare,boolean dead,boolean won,int powerStage){
        return monitor&&scare==0&&!dead&&!won&&powerStage==0;
    }
}
