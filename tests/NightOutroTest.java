package dev.thor.fnaf;
public final class NightOutroTest {
    static int checks;
    static void check(boolean yes,String message){checks++;if(!yes)throw new AssertionError(message);}
    public static void main(String[] args){
        for(int night=1;night<=7;night++){
            NightOutro o=new NightOutro();o.begin(night,true);int cheers=0,endings=0,done=0;
            check(!o.skipEnding(),"Celebration cannot be accidentally skipped");
            for(int frame=0;frame<1800;frame++){
                int event=o.tick(1.0/60);
                if((event&1)!=0){cheers++;check(o.elapsed>=3&&o.roll()==1,"Cheer waits for six");}
                if((event&2)!=0){endings++;check(o.endingImage()==(night==5?210:night==6?522:523),"Correct original ending");check(!o.skipEnding(),"Ending entry is protected");}
                if((event&4)!=0)done++;
            }
            check(cheers==1&&done==1,"One cheer and one completion per night");
            check(endings==(night>=5?1:0),"Only late nights have ending art");
        }
        NightOutro o=new NightOutro();o.begin(7,false);int e=0;for(int i=0;i<500;i++)e|=o.tick(.02);
        check(e==5,"Challenge celebrates without campaign firing ending");
        o.begin(5,true);while(o.stage==1)o.tick(.1);for(int i=0;i<12;i++)o.tick(.1);check(o.skipEnding()&&!o.active(),"Ending can be dismissed after guard");
        Progress p=new Progress();check(NightOutro.starMask(p,1)==0,"No unearned stars");
        for(int night:new int[]{1,5,6,7}){
            NightOutro held=new NightOutro();held.begin(night,true);int cheers=0;
            for(int i=0;i<1200;i++){int event=held.tick(.01,false);if((event&NightOutro.CHEER)!=0)cheers++;check((event&(NightOutro.DONE|NightOutro.SHOW_ENDING))==0,"Never cut victory audio for next scene");}
            check(held.stage==1&&cheers==1,"Waiting for audio holds six AM without repeating cheer");
            int event=held.tick(.01,true);check(event==(night==1?NightOutro.DONE:NightOutro.SHOW_ENDING),"Completion advances once audio finishes");
        }
        check(NightOutro.starMask(p,6)==1,"Existing Night 5 save migration");check(NightOutro.starMask(p,7)==3,"Existing Night 6 save migration");
        p.achievements=(1<<4)|(1<<5)|(1<<9);p=new Progress(p.save());check(NightOutro.starMask(p,7)==7,"Three stars survive save reload");
        System.out.println("Night outro: "+checks+" checks passed");
    }
}
