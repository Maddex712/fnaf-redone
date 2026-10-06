package dev.thor.fnaf;
import java.util.Random;
public final class ProgressTest {
    static int checks;
    static void check(boolean ok,String message){checks++;if(!ok)throw new AssertionError(message);}
    static GameState game(int n){GameState s=new GameState(new Random(91));s.start(n);return s;}
    public static void main(String[] args){
        Progress p=new Progress();GameState s=game(1);p.begin(s,-1);
        check(p.update(s)==0&&p.wins==0,"Starting earns nothing");
        s.selectCamera(2);check(s.cameraMask==0,"Standby camera selection does not count");
        s.toggleMonitor();for(int i=0;i<11;i++)s.selectCamera(i);
        check(p.update(s)==1<<6,"All eleven channels earn surveillance achievement");
        check(p.update(s)==0,"Unlock fires only once");
        s.monitor=false;s.leftDoor=true;s.foxAttack();check(p.update(s)==1<<7,"A real blocked Foxy attack grants achievement");
        s.won=true;s.power=20;p.update(s);check(p.earned(0)&&p.earned(8)&&p.wins==1,"Campaign win and power threshold recorded");
        p.update(s);check(p.wins==1,"Repeated winning frames counted once");
        Progress reload=new Progress(p.save());check(reload.save().equals(p.save()),"Persistent progress roundtrip");
        s=game(2);p.begin(s,-1);s.dead=true;p.update(s);check(!p.earned(1)&&p.wins==1,"Death never grants completion");
        s=game(2);p.begin(s,-1);p.abandon();s.won=true;p.update(s);check(!p.earned(1),"Aborted run never grants completion");
        for(int i=0;i<4;i++){
            s=new GameState();s.custom=Progress.LEVELS[i].clone();s.start(7);p.begin(s,i);
            s.custom=new int[]{0,0,0,0};s.won=true;s.power=90;p.update(s);
            check(p.cleared(i),"Challenge completion "+i);
        }
        check(p.completed==15&&p.earned(10)&&p.earned(11)&&p.earned(9),"All assignments and captured maximum AI recognized");
        check(!p.earned(5)&&p.bestPower==20,"Challenges do not count as campaign wins or power records");
        s=game(7);p.begin(s,-1);s.ai=new int[]{20,20,20,20};s.won=true;Progress easy=new Progress();
        s=game(7);easy.begin(s,-1);s.ai=new int[]{20,20,20,20};s.won=true;easy.update(s);check(!easy.earned(9),"Later difficulty mutations cannot grant maximum AI achievement");
        s.cameraMask=2047;s.start(1);check(s.cameraMask==0,"Camera progress resets per shift");
        check(new Progress("1,bad,0,0,0").count()==0,"Corrupt save safely ignored");
        check(new Progress("1,4096,0,0,0").count()==0,"Out of range save safely ignored");
        p=new Progress();s=game(1);p.begin(s,-1);s.won=true;s.power=20;p.update(s);
        check(p.points==250&&p.lastPoints==250,"First shift and power saver pay once");
        p.update(s);check(p.points==250&&p.lastPoints==0,"Repeated win frames cannot farm points");
        check(!p.buy(4)&&p.points==250&&!p.owns(4),"Insufficient balance cannot purchase");
        check(p.buy(0)&&p.points==0&&p.owns(0)&&p.wearing(0),"Exact-balance purchase debits and equips");
        check(!p.buy(0)&&p.points==0,"Cannot purchase the same plush twice");
        check(p.togglePlush(0)&&!p.wearing(0)&&p.owns(0),"Storing preserves ownership");
        check(!p.togglePlush(1)&&!p.owns(1),"Cannot equip unowned plush");
        reload=new Progress(p.save());check(reload.owns(0)&&!reload.wearing(0)&&reload.points==0,"Ownership, storage, spent balance persist");
        reload.togglePlush(0);check(new Progress(reload.save()).wearing(0),"Equipped plush persists");
        Progress old=new Progress("1,257,1,2,25");check(old.points==350,"Legacy achievements and completed challenge receive retroactive points");
        check(new Progress(old.save()).points==350,"Migrated save cannot credit rewards twice");
        p=new Progress();int total=0;
        for(int i=0;i<4;i++){
            s=new GameState();s.custom=Progress.LEVELS[i].clone();s.start(7);p.begin(s,i);s.won=true;
            int awards=p.update(s);total+=Progress.CHALLENGE_POINTS[i]+Progress.achievementValue(awards);
            check(p.points==total,"Difficulty-scaled payout with achievement bonuses "+i);
            p.update(s);check(p.points==total,"Repeated result frame pays nothing "+i);
        }
        s=new GameState();s.custom=Progress.LEVELS[3].clone();s.start(7);p.begin(s,3);s.won=true;
        check(p.update(s)==0&&p.lastPoints==600&&p.points==total+600,"Replay pays assignment reward without duplicate achievements");
        int balance=p.points;s=game(7);p.begin(s,3);s.won=true;p.update(s);check(p.points==balance,"Mismatched AI preset cannot claim challenge reward");
        s=game(1);p.begin(s,-1);p.abandon();s.won=true;p.update(s);check(p.points==balance,"Abandoned run pays nothing");
        s=game(1);p.begin(s,-1);s.dead=true;p.update(s);check(p.points==balance,"Death pays no completion reward");
        check(!p.buy(-1)&&!p.buy(5),"Invalid purchase IDs rejected");
        check(new Progress("2,0,0,0,0,-1,0,0").points==0,"Negative wallet save rejected");
        check(new Progress("2,0,0,0,0,500,0,1").points==0,"Save cannot equip unowned item");
        check(Progress.PRICES[4]>Progress.PRICES[0]&&Progress.PRICES[4]>Progress.PRICES[3],"Golden Freddy is most expensive");
        System.out.println("Progress: "+checks+" checks passed");
    }
}
