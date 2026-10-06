package dev.thor.fnaf;
import java.util.Random;
public final class GameStateTest {
    static int assertions;
    static void check(boolean b,String message){assertions++;if(!b)throw new AssertionError(message);}
    static void advance(GameState s,double seconds){int n=(int)Math.round(seconds*60);for(int i=0;i<n;i++)s.tick(1.0/60);}
    static GameState safe(){GameState s=new GameState(new Random(){@Override public int nextInt(int b){return b-1;}});s.start();return s;}
    static boolean near(double a,double b){return Math.abs(a-b)<.00001;}
    public static void main(String[] args){
        for(int who=1;who<=5;who++){
            GameState paced=safe();paced.attack(who);
            check(near(paced.scareAnimationTime(),0),"Jumpscare starts on first frame");
            advance(paced,.5);double middle=paced.scareAnimationTime();
            check(middle>0&&middle<.51,"Jumpscares use the original animation clock");
            advance(paced,.55);check(!paced.dead&&near(paced.scareAnimationTime(),1.05),"Original-speed animation holds while the scream completes");
            advance(paced,.05);check(paced.dead,"Jumpscare completes after full 1.1 second window");
        }
        int[] scares=new int[7];
        for(int outcome=0;outcome<40;outcome++){
            final int fixed=outcome;
            GameState jump=new GameState(new Random(){@Override public int nextInt(int bound){return fixed%bound;}});
            check(!jump.triggerRandomJumpscare(),"Jumpscare shortcut inactive in menu");
            jump.start();check(jump.triggerRandomJumpscare(),"Shortcut starts active-night jumpscare");
            scares[jump.scare]++;check(!jump.triggerRandomJumpscare(),"Cannot repeat an ongoing jumpscare");
        }
        check(scares[6]==4&&scares[5]==0,"Exactly ten percent Golden Freddy; no power-out variant");
        for(int who=1;who<=4;who++)check(scares[who]==9,"Equal 22.5 percent share for each main character");
        NightSkipChord chord=new NightSkipChord();
        NightSkipChord jumpChord=new NightSkipChord(7);
        check(!jumpChord.button(4,true)&&!jumpChord.button(1,true)&&jumpChord.button(2,true),"Y and both stick clicks trigger");
        check(!jumpChord.button(2,true),"Held jumpscare chord cannot retrigger");
        jumpChord.reset();check(!jumpChord.button(2,true),"Jumpscare chord resets with input lifecycle");
        for(int bit=1;bit<=16;bit<<=1)check(!chord.button(bit,true),"Incomplete chord never fires");
        check(chord.button(32,true),"Six physical buttons fire");
        check(!chord.button(32,true)&&!chord.triggers(1,1),"Repeat and duplicate analog reports do not fire");
        chord.button(32,false);check(chord.button(32,true),"Release and repress rearms chord");
        chord.reset();check(!chord.triggers(1,1),"Analog triggers alone do not fire");
        chord.button(32,true);chord.button(16,true);chord.button(2,true);
        check(chord.button(1,true),"Analog triggers and four buttons work in reverse order");
        chord.reset();chord.button(1,true);chord.button(2,true);chord.button(16,true);chord.button(32,true);
        check(!chord.triggers(.4f,1)&&chord.triggers(1,1),"Analog trigger completing chord fires");
        chord.reset();check(!chord.button(32,true),"Lifecycle reset clears every held input");
        for(int night=1;night<=7;night++){
            GameState skip=safe();skip.start(night);double originalPower=skip.power;
            check(skip.skipToLastFiveSeconds()&&skip.hour==5&&skip.elapsed==530,"Skip advances night "+night);
            check(skip.power==originalPower&&!skip.won,"Skip preserves power and does not directly win");
            advance(skip,4);check(!skip.won&&!skip.skipToLastFiveSeconds()&&skip.elapsed==534,"Cannot rewind final seconds");
            advance(skip,1);check(skip.won&&skip.hour==6&&skip.sounds.contains("@win"),"Normal 6 AM event after five seconds");
        }
        GameState blocked=safe();blocked.pause();check(!blocked.skipToLastFiveSeconds(),"No skip while paused");
        blocked=safe();blocked.attack(1);check(!blocked.skipToLastFiveSeconds(),"No skip during jumpscare");
        blocked=new GameState();check(!blocked.skipToLastFiveSeconds(),"No skip from menu");
        GameState s=safe();advance(s,2);check(near(s.power,99.7),"Steam power drains one unit/second from 999 tenths");
        s.toggleDoor(true);s.toggleMonitor();check(s.monitor&&s.leftDoor&&s.usage()==3,"Monitor and door consume independent power");
        s.toggleDoor(false);s.light(false,true);check(!s.rightDoor&&!s.rightLight,"Office controls locked while monitor raised");
        s.selectCamera(-1);check(s.camera==10,"Camera wrapping backward");s.selectCamera(11);check(s.camera==0,"Camera wrapping forward");
        s.pause();double t=s.elapsed,p=s.power;advance(s,20);check(s.elapsed==t&&s.power==p,"Pause freezes simulation");
        s=safe();advance(s,89);check(s.hour==12,"First hour lasts 90 seconds");advance(s,1);check(s.hour==1,"1 AM transition");advance(s,89);check(s.hour==2&&s.ai[1]==1,"2 AM Bonnie increase");advance(s,89);check(s.hour==3&&s.ai[1]==2&&s.ai[2]==1&&s.ai[3]==1,"3 AM difficulty increase");advance(s,89);check(s.hour==4&&s.ai[1]==3&&s.ai[2]==2,"4 AM difficulty increase");advance(s,178);check(s.won&&s.hour==6,"6 AM win after 535 seconds");
        s=safe();s.bonnie=GameState.DOOR;s.leftDoor=true;s.moveBonnie();check(s.bonnie==1,"Closed left door sends Bonnie to dining");s.bonnie=GameState.DOOR;s.leftDoor=false;s.moveBonnie();check(s.bonnie==GameState.INSIDE,"Open door lets Bonnie enter");s.toggleDoor(true);check(!s.leftDoor,"Entered Bonnie jams the door");s.toggleMonitor();advance(s,.25);s.toggleMonitor();check(s.scare==1,"Bonnie attacks when monitor lowered after intrusion");
        s=safe();s.chica=GameState.DOOR;s.rightDoor=true;s.moveChica();check(s.chica==6,"Closed right door sends Chica to east hall");
        s=safe();s.leftDoor=true;s.foxAttack();check(near(s.power,98.9),"First Foxy block costs 1 percent");s.foxAttack();check(near(s.power,92.9),"Second Foxy block costs 6 percent");
        s=safe();s.freddy=7;s.freddyMode=2;s.monitor=true;s.camera=7;s.moveFreddy();check(s.freddy==7,"Watching east corner blocks Freddy");s.camera=0;s.moveFreddy();check(s.freddy==GameState.INSIDE,"Freddy enters from unwatched east corner");
        s=safe();s.power=.01;advance(s,1);check(s.powerStage>0&&!s.monitor&&!s.leftDoor,"Power loss disables equipment");
        s=safe();s.start(6);check(s.ai[0]==4&&s.ai[1]==10&&s.ai[2]==12&&s.ai[3]==6,"Night six initial AI");
        GameState a=new GameState(new Random(9)),b=new GameState(new Random(9));a.start(5);b.start(5);for(int i=0;i<3000;i++)a.tick(1.0/60);for(int i=0;i<6000;i++)b.tick(1.0/120);
        check(a.ticks==b.ticks&&a.bonnie==b.bonnie&&a.chica==b.chica&&near(a.power,b.power),"Simulation independent of refresh rate");
        s=safe();s.axis=1;advance(s,.25);check(s.look>.94f&&s.look<.96f,"Faster default look speed");advance(s,.5);check(s.look==1,"Look is clamped at office edge");
        s=safe();s.lookSpeed=1.6f;s.axis=1;advance(s,.25);check(s.look>.39f&&s.look<.41f,"Low sensitivity retains prior speed");s.toggleMonitor();float look=s.look;advance(s,.25);check(s.look==look,"Monitor locks panning at all sensitivities");
        s=safe();s.ticks=160;check(s.cameraPan()==160,"Camera sweeps one pixel per simulation frame");s.ticks=360;check(s.cameraPan()==320,"Camera pauses at right edge");s.ticks=580;check(s.cameraPan()==160,"Camera returns left");s.ticks=800;check(s.cameraPan()==0,"Camera pauses at left edge");s.ticks=840;check(s.cameraPan()==0,"Camera sweep loops");s.ticks=160;s.camera=5;check(s.cameraPan()==0,"Supply closet stays fixed");
        s=safe();check(near(s.pirateVolume(),.05f),"Foxy humming quiet in office");s.monitor=true;s.camera=2;check(near(s.pirateVolume(),.15f),"Foxy humming louder on Pirate Cove");s.camera=0;check(near(s.pirateVolume(),.05f),"Foxy humming quiet on other feeds");
        s.freddy=9;check(near(s.kitchenMusicVolume(),.05f),"Freddy kitchen music quiet remotely");s.camera=9;check(near(s.kitchenMusicVolume(),.5f),"Freddy kitchen music audible on kitchen feed");s.freddy=6;check(s.kitchenMusicVolume()==0,"Kitchen music silenced after Freddy leaves");
        s.chica=9;check(near(s.kitchenNoiseVolume(),.75f),"Kitchen noises on kitchen feed");s.camera=0;check(near(s.kitchenNoiseVolume(),.2f),"Kitchen noises on other feeds");s.monitor=false;check(near(s.kitchenNoiseVolume(),.1f),"Kitchen noises in office");s.chica=6;check(s.kitchenNoiseVolume()==0,"Kitchen noises silenced after Chica leaves");
        s=safe();s.toggleDoor(true);advance(s,.3);check(s.leftDoorTime>0&&s.leftDoor,"Door transition does not snap shut halfway");s.toggleDoor(true);check(s.leftDoor,"Repeated toggle cannot interrupt door animation");advance(s,.25);check(s.leftDoorTime==0&&s.leftDoor,"Full door closing sequence settles");s.toggleDoor(true);advance(s,.3);check(s.leftDoorTime>0&&!s.leftDoor,"Full opening sequence is retained");advance(s,.25);check(s.leftDoorTime==0&&!s.leftDoor,"Opening settles at open frame");
        System.out.println("PASS: "+assertions+" assertions covering simulation, door transitions, camera sweep, audio routing, sensitivity, and refresh-rate determinism.");
    }
}
