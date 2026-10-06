package dev.thor.fnaf;
public final class NightIntroTest {
    static int checks;
    static void check(boolean ok,String message){checks++;if(!ok)throw new AssertionError(message);}
    static void advance(NightIntro n,double t){for(int i=0;i<(int)Math.round(t*100);i++)n.tick(.01);}
    public static void main(String[] args){
        NightIntro n=new NightIntro();n.begin(true);
        check(n.stage==4&&n.opacity()==1,"Intro starts with visible title for fade-out");
        n.tick(.175);check(n.opacity()>.49&&n.opacity()<.51,"Title fades smoothly");advance(n,.18);
        check(n.stage==1&&n.opacity()==0,"Newspaper begins black");n.skipPaper();check(n.elapsed==0,"Launch press cannot skip paper");
        advance(n,.5);check(n.opacity()==1,"Newspaper fades in");n.skipPaper();
        check(n.stage==1&&n.opacity()>.99,"Skip initiates fade, never cuts to next stage");advance(n,.06);
        check(n.stage==1&&n.opacity()<.6&&n.opacity()>0,"Skipped paper fades out");advance(n,.07);
        check(n.stage==2&&n.opacity()<.1,"Card begins with fade-in");advance(n,.14);check(n.opacity()==1,"Night card becomes readable");
        n.skipPaper();check(n.stage==2,"Repeated confirm cannot skip night card");advance(n,2.57);check(n.stage==3,"Card ends with black lead-in");
        advance(n,.05);check(!n.active()&&!n.tick(1),"Intro finishes once only");
        n.begin(false);advance(n,.36);check(n.stage==2,"Later nights bypass newspaper");
        n.tick(100);check(n.elapsed<.3,"Frame stalls cannot bypass intro");n.cancel();check(!n.tick(1)&&!n.active(),"Cancel does not start gameplay");
        n.begin(true);advance(n,5.3);check(n.stage==1&&n.opacity()==1,"Automatic newspaper keeps a readable hold");advance(n,1);check(n.stage==2,"Automatic newspaper advances after fade");
        check(NightIntro.CARDS.length==7,"Original card for every night");
        n.beginNextNight();check(n.stage==2&&n.opacity()==0,"Next night starts on black night card without drawing title");
        advance(n,.14);check(n.stage==2&&n.opacity()==1,"Next night card fades in normally");
        System.out.println("Night intro: "+checks+" checks passed");
    }
}


