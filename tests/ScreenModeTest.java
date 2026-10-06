package dev.thor.fnaf;
public final class ScreenModeTest {
    static int checks;
    static void check(boolean value,String message){checks++;if(!value)throw new AssertionError(message);}
    public static void main(String[] args){
        check(!ScreenMode.useSecondDisplay(ScreenMode.AUTO,false),"Phone defaults to one screen");
        check(ScreenMode.useSecondDisplay(ScreenMode.AUTO,true),"Thor defaults to dual screens");
        check(!ScreenMode.useSecondDisplay(ScreenMode.SINGLE,true),"Forced single ignores secondary display");
        check(ScreenMode.useSecondDisplay(ScreenMode.DUAL,true),"Dual preference uses available display");
        check(!ScreenMode.useSecondDisplay(ScreenMode.DUAL,false),"Disconnect falls back without split preview");
        check(ScreenMode.valid(-1)==ScreenMode.AUTO&&ScreenMode.valid(100)==ScreenMode.AUTO,"Invalid saved setting recovers");
        check(ScreenMode.showCamera(true,0,false,false,0),"Monitor replaces office when raised");
        check(!ScreenMode.showCamera(false,0,false,false,0),"Lowering monitor restores office");
        for(int scare=1;scare<=6;scare++)check(!ScreenMode.showCamera(true,scare,false,false,0),"Jumpscare always occupies main screen");
        check(!ScreenMode.showCamera(true,0,true,false,0),"Death cannot be hidden behind camera");
        check(!ScreenMode.showCamera(true,0,false,true,0),"Victory cannot be hidden behind camera");
        for(int stage=1;stage<=4;stage++)check(!ScreenMode.showCamera(true,0,false,false,stage),"Power failure restores office");
        System.out.println("Screen modes: "+checks+" checks passed");
    }
}
