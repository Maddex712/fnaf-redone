package dev.thor.fnaf;

import java.util.ArrayDeque;
import java.util.Random;

/** Independent 60 Hz simulation informed by Steam build 284. Runtime parity remains unverified. */
public final class GameState {
    public static final double DOOR_DURATION=16.0/30.0;
    public static final String[] CAMERAS={"1A","1B","1C","2A","2B","3","4A","4B","5","6","7"};
    public static final String[] ROOMS={"Show stage","Dining area","Pirate Cove","West hall","W. hall corner","Supply closet","East hall","E. hall corner","Backstage","Kitchen","Restrooms"};
    public static final int DOOR=11, INSIDE=12;
    public boolean running,paused,monitor,leftDoor,rightDoor,leftLight,rightLight,won,dead,mutedCall;
    public int cameraMask;
    public int camera,night=1,hour=12,bonnie,chica,freddy,fox,scare,powerStage;
    public int[] ai=new int[4], custom={0,0,0,0}; // Freddy, Bonnie, Chica, Foxy
    public float look,axis;
    public float lookSpeed=3.8f;
    public double elapsed,power=99.9,scareTime,cameraStatic,leftDoorTime,rightDoorTime,monitorTime;
    public int picture=50,flicker=10,twitch=1,golden;
    final Random random;
    public final ArrayDeque<String> sounds=new ArrayDeque<>();
    int ticks,seconds,minute,foxCooldown,foxWait,foxHits,freddyMode,freddyWait,powerWait,goldenWait;
    int routeB=1,routeC=1,variantB=1,variantC=1;
    boolean bonnieArmed,chicaArmed,sawBonnie,sawChica;
    double accumulator;
    public GameState(){this(new Random());}
    public GameState(Random random){this.random=random;}
    int roll(int bound){return random.nextInt(bound);}
    public int usage(){return 1+(monitor?1:0)+(leftDoor?1:0)+(rightDoor?1:0)+(leftLight?1:0)+(rightLight?1:0);}
    public boolean active(){return running&&!paused&&!dead&&!won&&scare==0;}
    public int cameraPan(){if(camera==5)return 0;int phase=ticks%840;return phase<320?phase:phase<420?320:phase<740?740-phase:0;}
    public float pirateVolume(){return monitor&&camera==2?.15f:.05f;}
    public float kitchenMusicVolume(){return freddy!=9?0:monitor&&camera==9?.5f:.05f;}
    public float kitchenNoiseVolume(){return chica!=9?0:monitor?(camera==9?.75f:.2f):.1f;}
    public void start(){start(night);}
    public void start(int selectedNight){
        night=Math.max(1,Math.min(7,selectedNight));running=true;paused=monitor=leftDoor=rightDoor=leftLight=rightLight=won=dead=mutedCall=false;
        ticks=seconds=minute=foxCooldown=foxWait=foxHits=freddyMode=freddyWait=powerWait=goldenWait=0;
        cameraMask=0;camera=bonnie=chica=freddy=fox=scare=powerStage=golden=0;hour=12;look=axis=0;
        elapsed=accumulator=scareTime=cameraStatic=leftDoorTime=rightDoorTime=monitorTime=0;power=99.9;
        bonnieArmed=chicaArmed=sawBonnie=sawChica=false;picture=roll(100)+1;routeB=routeC=variantB=variantC=1;
        int[][] levels={{0,0,0,0},{0,3,1,1},{1,0,5,2},{1,2,4,6},{3,5,7,5},{4,10,12,6}};
        ai=(night==7?custom:levels[night-1]).clone();if(night==4)ai[0]=1+roll(2);
        sounds.clear();sound("@start");
        if(night==7&&custom[0]==1&&custom[1]==9&&custom[2]==8&&custom[3]==7)attack(6);
    }
    public void clearHeld(){leftLight=rightLight=false;axis=0;}
    public boolean skipToLastFiveSeconds(){
        // The first hour is 90 seconds, subsequent hours are 89 seconds.
        // Advance only the clock; preserve the current office, power and enemies.
        if(!active()||ticks>=530*60)return false;
        ticks=530*60;seconds=530;minute=85;hour=5;elapsed=530;accumulator=0;
        return true;
    }
    public void pause(){paused=true;clearHeld();}
    public void tick(double delta){
        if(!running||paused||dead||won)return;
        accumulator+=Math.min(.25,Math.max(0,delta));
        while(accumulator+1e-9>=1.0/60){accumulator-=1.0/60;step();if(dead||won){accumulator=0;break;}}
    }
    boolean every(double interval){return Math.floor((ticks/60.0+1e-7)/interval)>Math.floor(((ticks-1)/60.0+1e-7)/interval);}
    void step(){
        ticks++;elapsed=ticks/60.0;
        leftDoorTime=Math.max(0,leftDoorTime-1.0/60);rightDoorTime=Math.max(0,rightDoorTime-1.0/60);
        monitorTime=Math.max(0,monitorTime-1.0/60);cameraStatic=Math.max(0,cameraStatic-1.0/60);
        if(scare>0){scareTime+=1.0/60;if(scareTime>=scareDuration()){dead=true;sound("@dead");}return;}
        if(!monitor)look=Math.max(-1,Math.min(1,look+axis/60*lookSpeed));
        flicker=roll(10)+1;if(ticks%3==0)twitch=roll(30)+1;
        if(ticks%60==0){
            seconds++;minute++;
            if(minute>=90){minute=1;hour=hour==12?1:hour+1;if(hour==2)ai[1]++;if(hour==3||hour==4){ai[1]++;ai[2]++;ai[3]++;}}
            if(hour==6){won=true;clearHeld();monitor=false;sound("@win");return;}
            if(power>0)power-=usage()*.1;
            routeB=roll(2)+1;routeC=roll(2)+1;
            if(roll(100000)==1&&golden==0)golden=1;
            if(freddy==INSIDE&&!monitor&&powerStage==0&&roll(4)==1)attack(4);
        }
        if(night>=2&&every(night==2?6:night==3?5:night==4?4:3))power-=.1;
        power=Math.max(0,power);
        if(power<=0&&powerStage==0){powerStage=1;powerWait=0;leftDoor=rightDoor=monitor=false;clearHeld();sound("@power");}
        if(powerStage>0){powerSequence();return;}
        if(monitor&&ticks%6==0)foxCooldown=50+roll(1000);if(foxCooldown>0)foxCooldown--;
        if(every(4.97)&&roll(20)+1<=ai[1])moveBonnie();
        if(every(4.98)&&roll(20)+1<=ai[2])moveChica();
        if(every(3.02)&&roll(20)+1<=ai[0]&&!monitor)freddyMode=1;
        if(freddyMode==1){freddyWait++;if(monitor&&camera==freddy)freddyWait=0;if(freddyWait>=1000-ai[0]*100&&!monitor){freddyWait=0;freddyMode=2;}}
        if(freddyMode==2)moveFreddy();
        if(every(5.01)&&(!monitor||camera!=2)&&fox<3&&foxCooldown==0&&roll(20)+1<=ai[3]){fox++;foxWait=0;}
        if(fox==3){foxWait++;if(monitor&&camera==3){fox=4;foxWait=0;sound("run");}else if(foxWait>1500)foxAttack();}
        if(fox==4&&++foxWait>100)foxAttack();
        if(monitor){if(bonnie==INSIDE)bonnieArmed=true;if(chica==INSIDE)chicaArmed=true;}
        if(monitor&&every(30)&&(bonnie==INSIDE||chica==INSIDE))lowerMonitor();
        if(bonnie==INSIDE)leftLight=false;if(chica==INSIDE)rightLight=false;
        if(leftLight&&bonnie==DOOR&&!sawBonnie){sound("windowscare");sawBonnie=true;}if(rightLight&&chica==DOOR&&!sawChica){sound("windowscare");sawChica=true;}
        if(bonnie!=DOOR)sawBonnie=false;if(chica!=DOOR)sawChica=false;
        if(golden==2&&!monitor&&++goldenWait>=300)attack(6);
        if(every(4)&&chica==9){int kitchenRoll=roll(10);if(kitchenRoll<5)sound(new String[]{"OVEN-DRA_1_GEN-HDF18119","OVEN-DRA_2_GEN-HDF18120","OVEN-DRA_7_GEN-HDF18121","OVEN-DRA_7_GEN-HDF18121","OVEN-DRAWE_GEN-HDF18122"}[kitchenRoll]);}
        if(every(5)&&roll(30)==0)sound("circus");if(every(4)&&fox==0&&roll(30)==0)sound("pirate song2");
        if(every(300)&&freddy==9)sound("@kitchenMusic");
    }
    void moved(int from,int to){if(monitor&&(camera==from||camera==to)){cameraStatic=5;sound("garble"+(roll(3)+1));}else sound("deep steps");}
    void moveBonnie(){
        int old=bonnie;variantB=roll(2)+1;
        switch(bonnie){case 0:bonnie=routeB==1?8:1;break;case 8:case 1:bonnie=routeB==1?(old==8?1:8):3;break;
            case 3:bonnie=routeB==1?5:4;break;case 4:bonnie=routeB==1?5:DOOR;break;case 5:bonnie=routeB==1?DOOR:3;break;
            case DOOR:if(leftDoorTime>0)return;bonnie=leftDoor?1:INSIDE;break;default:return;}
        moved(old,bonnie);
    }
    void moveChica(){
        int old=chica;variantC=roll(2)+1;
        switch(chica){case 0:chica=1;break;case 1:chica=routeC==1?10:9;break;case 9:chica=routeC==1?10:6;break;case 10:chica=routeC==1?9:6;break;
            case 6:chica=routeC==1?1:7;break;case 7:chica=routeC==1?6:DOOR;break;case DOOR:if(rightDoorTime>0)return;chica=rightDoor?6:INSIDE;break;default:return;}
        moved(old,chica);
    }
    void moveFreddy(){
        int old=freddy;
        switch(freddy){case 0:if(bonnie==0||chica==0)return;freddy=1;break;case 1:freddy=10;break;case 10:freddy=9;break;case 9:freddy=6;break;
            case 6:if(rightLight)return;freddy=7;break;case 7:if(!monitor||camera==7||rightDoorTime>0)return;if(!rightDoor)freddy=INSIDE;else if(camera!=6)freddy=6;else return;break;default:return;}
        if(freddy!=old){freddyMode=0;sound("Laugh_Giggle_Girl_"+new String[]{"1d","2d","8d"}[roll(3)]);if(freddy==9)sound("@kitchenMusic");if(freddy==INSIDE)sound("whispering2");}
    }
    void foxAttack(){
        if(leftDoorTime>0)return;if(monitor)lowerMonitor();
        if(leftDoor){power=Math.max(0,power-(1+foxHits*5));foxHits++;fox=roll(2);foxWait=0;sound("knock2");}else attack(3);
    }
    void powerSequence(){
        powerWait++;
        if(powerStage==1&&((every(5)&&roll(5)==0)||every(20))){powerStage=2;powerWait=0;sound("music box");}
        else if(powerStage==2&&((every(5)&&roll(5)==0)||every(20))){powerStage=3;powerWait=0;sound("@blackout");}
        else if(powerStage==3&&powerWait>20){powerStage=4;powerWait=0;}
        else if(powerStage==4&&((every(2)&&roll(5)==0)||every(20)))attack(5);
    }
    void sound(String name){if(sounds.size()<64)sounds.add(name);}
    public void attack(int who){if(scare>0||won||dead)return;scare=who;scareTime=0;monitor=false;look=0;clearHeld();sound("@scare");}
    public boolean triggerRandomJumpscare(){
        if(!active())return false;
        // One of forty equally likely outcomes: four Golden, nine per main character.
        int choice=roll(40);
        attack(choice<4?6:1+(choice-4)/9);
        return true;
    }
    public double scareDuration(){return scare==6?1.5:1.1;}
    public double scareAnimationTime(){
        // Feed the original animation clock directly into the extracted FNaF
        // frame data. Short attacks finish naturally and hold their last frame
        // while the scream completes instead of being slowed to a shared pace.
        return scareTime;
    }
    public void selectCamera(int index){
        if(!monitor||!active()||powerStage>0)return;int next=Math.floorMod(index,CAMERAS.length);if(next==camera)return;
        camera=next;cameraMask|=1<<camera;sound("blip3");monitorTime=.12;if(camera==4&&golden==1){golden=2;sound("Laugh_Giggle_Girl_1");}
    }
    public void toggleMonitor(){
        if(!active()||powerStage>0||monitorTime>0)return;
        if(monitor)lowerMonitor();else{monitor=true;cameraMask|=1<<camera;leftLight=rightLight=false;monitorTime=.2;sound("CAMERA_VIDEO_LOA_60105303");if(goldenWait>0){golden=0;goldenWait=0;}}
    }
    void lowerMonitor(){monitor=false;monitorTime=.2;picture=roll(100)+1;sound("put down");if(bonnieArmed)attack(1);else if(chicaArmed)attack(2);}
    public void toggleDoor(boolean left){
        if(!active()||powerStage>0||monitor)return;
        if((left&&bonnie==INSIDE)||(!left&&chica==INSIDE)){sound("error");return;}
        if(left){if(leftDoorTime>0)return;leftDoor=!leftDoor;leftDoorTime=DOOR_DURATION;}else{if(rightDoorTime>0)return;rightDoor=!rightDoor;rightDoorTime=DOOR_DURATION;}sound("SFXBible_12478");
    }
    public void light(boolean left,boolean on){
        if(!on){if(left)leftLight=false;else rightLight=false;return;}if(!active()||powerStage>0||monitor)return;
        if((left&&bonnie==INSIDE)||(!left&&chica==INSIDE)){sound("error");return;}
        if(left){leftLight=true;rightLight=false;}else{rightLight=true;leftLight=false;}
    }
    public int officeAnimation(){
        if(scare==1)return 35;if(scare==2)return 44;if(scare==3)return 52;if(scare==4)return 65;
        if(powerStage>0)return powerStage==2&&flicker<=2?47:46;
        if(leftLight&&flicker>1)return bonnie==DOOR?34:18;if(rightLight&&flicker>1)return chica==DOOR?43:19;return 0;
    }
    public int cameraAnimation(){
        switch(camera){
            case 0:if(bonnie==0&&chica==0)return 17;if(bonnie!=0&&chica==0)return 27;if(bonnie==0)return 41;return freddy==0?(picture<=10?54:42):60;
            case 1:if(chica==1)return variantC==1?40:55;if(bonnie==1)return variantB==1?28:29;return freddy==1?61:13;
            case 2:return fox==0?26:fox==1?48:fox==2?49:picture<=10?73:50;
            case 3:if(fox==4)return 51;return flicker>3?14:bonnie==3?33:15;
            case 4:if(bonnie==4)return night<4||twitch<25?30:twitch<29?58:59;return golden>0?74:picture<2?75:20;
            case 5:return bonnie==5?31:24;
            case 6:if(chica==6)return variantC==1?39:45;if(freddy==6)return 63;return picture==99?67:picture==100?68:25;
            case 7:if(chica==7)return night<4||twitch<25?38:twitch<29?56:57;if(freddy==7)return 64;return picture>=97?69+picture-97:23;
            case 8:return bonnie==8?(picture<=10?66:32):(picture<=5?53:22);
            case 10:return chica==10?(variantC==1?36:37):freddy==10?62:21;default:return -1;
        }
    }
}
