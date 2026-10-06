package dev.thor.fnaf;

/** Local progression. Only a run begun here can grant completion credit. */
public final class Progress {
    static final String[] TITLES={"First Shift","Getting Comfortable","Halfway There","One More Night","Payday","Overtime","Eyes Everywhere","Not Tonight, Foxy","Power Saver","Employee of the Month","Challenge Accepted","All Assignments"};
    static final String[] DETAILS={"Survive campaign Night 1.","Survive campaign Night 2.","Survive campaign Night 3.","Survive campaign Night 4.","Survive campaign Night 5.","Survive campaign Night 6.","Visit all 11 camera channels in one shift.","Block Foxy's attack with the left door.","Finish a campaign night with 20% power left.","Survive a custom night with all four AI at 20.","Complete any challenge assignment.","Complete all four challenge assignments."};
    static final String[] CHALLENGES={"Hall Patrol","Pirate Watch","Full House","20 / 20 / 20 / 20"};
    static final String[] BRIEFS={"Bonnie and Chica lead the shift.\nWatch both halls and manage the doors.","Foxy starts at AI 20.\nKeep Pirate Cove under surveillance.","Everyone starts at AI 10.\nBalance cameras, doors, and power.","All four start at maximum difficulty.\nSurvive the full shift to earn your place."};
    static final int[][] LEVELS={{0,10,10,0},{0,0,0,20},{10,10,10,10},{20,20,20,20}};
    static final int[] ACHIEVEMENT_POINTS={100,125,150,200,300,400,75,100,150,300,100,400};
    static final int[] CHALLENGE_POINTS={100,175,300,600};
    static final String[] PLUSHIES={"Freddy","Bonnie","Chica","Foxy","Golden Freddy"};
    static final int[] PRICES={250,300,300,400,1500};
    int achievements, completed, wins, bestPower, points, owned, equipped, lastPoints;
    static int achievementValue(int mask){int sum=0;for(int i=0;i<ACHIEVEMENT_POINTS.length;i++)if((mask&(1<<i))!=0)sum+=ACHIEVEMENT_POINTS[i];return sum;}
    boolean owns(int i){return i>=0&&i<5&&(owned&(1<<i))!=0;}
    boolean wearing(int i){return owns(i)&&(equipped&(1<<i))!=0;}
    boolean buy(int i){
        if(i<0||i>=5||owns(i)||points<PRICES[i])return false;
        points-=PRICES[i];owned|=1<<i;equipped|=1<<i;return true;
    }
    boolean togglePlush(int i){if(!owns(i))return false;equipped^=1<<i;return true;}
    private boolean tracking, credited;
    private int runNight, challenge=-1;
    private boolean maximum;
    Progress(){}
    Progress(String saved){
        if(saved==null)return;
        try{String[] v=saved.split(",");boolean legacy=v.length==5&&v[0].equals("1");
            if(!legacy&&!(v.length==8&&v[0].equals("2")))return;
            int a=Integer.parseInt(v[1]),c=Integer.parseInt(v[2]),w=Integer.parseInt(v[3]),p=Integer.parseInt(v[4]);
            if(a<0||a>=1<<TITLES.length||c<0||c>15||w<0||p<0||p>100)return;
            int balance=legacy?achievementValue(a):Integer.parseInt(v[5]);
            int purchases=legacy?0:Integer.parseInt(v[6]),visible=legacy?0:Integer.parseInt(v[7]);
            if(legacy)for(int i=0;i<4;i++)if((c&(1<<i))!=0)balance+=CHALLENGE_POINTS[i];
            if(balance<0||balance>1000000||purchases<0||purchases>31||visible<0||(visible&~purchases)!=0)return;
            achievements=a;completed=c;wins=w;bestPower=p;points=balance;owned=purchases;equipped=visible;
        }catch(NumberFormatException ignored){}
    }
    String save(){return "2,"+achievements+","+completed+","+wins+","+bestPower+","+points+","+owned+","+equipped;}
    boolean earned(int i){return (achievements&(1<<i))!=0;}
    boolean cleared(int i){return (completed&(1<<i))!=0;}
    int count(){return Integer.bitCount(achievements);}
    void begin(GameState s,int assignment){
        tracking=true;credited=false;runNight=s.night;challenge=assignment>=0&&assignment<LEVELS.length&&s.night==7&&java.util.Arrays.equals(s.ai,LEVELS[assignment])?assignment:-1;
        maximum=s.night==7;for(int a:s.ai)maximum&=a==20;
    }
    void abandon(){tracking=false;}
    int update(GameState s){
        lastPoints=0;if(!tracking||!s.running)return 0;
        int before=achievements;
        if(s.cameraMask==2047)achievements|=1<<6;
        if(s.foxHits>0)achievements|=1<<7;
        if(s.won&&!s.dead&&!credited){
            credited=true;wins++;
            if(challenge<0&&runNight<=6){achievements|=1<<(runNight-1);bestPower=Math.max(bestPower,(int)s.power);if(s.power>=20)achievements|=1<<8;}
            if(maximum)achievements|=1<<9;
            if(challenge>=0&&challenge<CHALLENGES.length){completed|=1<<challenge;lastPoints+=CHALLENGE_POINTS[challenge];achievements|=1<<10;if(completed==15)achievements|=1<<11;}
        }
        int awards=achievements&~before;lastPoints+=achievementValue(awards);points=Math.min(1000000,points+lastPoints);return awards;
    }
    String status(int i){
        if(earned(i))return "UNLOCKED";
        if(i==11)return Integer.bitCount(completed)+" / 4 assignments complete";
        return "LOCKED  -  "+(i<6?"Reach 6 AM":"See objective below");
    }
}
