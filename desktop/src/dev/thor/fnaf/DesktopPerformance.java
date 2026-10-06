package dev.thor.fnaf;

/** Bounded, local timing samples. No telemetry or user/game-file paths. */
final class DesktopPerformance {
    static final class Samples {
        final long[] values=new long[120];int count,index;
        void add(long ns){values[index++%values.length]=ns;count=Math.min(values.length,count+1);}
        String describe(String label){if(count==0)return label+": no gameplay samples yet";long[] copy=java.util.Arrays.copyOf(values,count);java.util.Arrays.sort(copy);return String.format(java.util.Locale.US,"%s: median %.2f / p95 %.2f / max %.2f ms",label,copy[count/2]/1e6,copy[Math.min(count-1,(int)(count*.95))]/1e6,copy[count-1]/1e6);}
    }
    final Samples updates=new Samples(),input=new Samples(),audio=new Samples(),gaps=new Samples();
    long previousFrame;
    void frame(long now,boolean active){if(active&&previousFrame!=0)gaps.add(now-previousFrame);previousFrame=active?now:0;}
    String report(){return gaps.describe("Update interval")+"\n"+updates.describe("Game update")+"\n"+input.describe("Input delivery")+"\n"+audio.describe("Audio update");}
}
