package dev.thor.fnaf;

import android.content.SharedPreferences;
import java.awt.image.BufferedImage;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;
import javax.swing.SwingUtilities;

/** Uses a private imported pack; reports actual draw costs, not estimated FPS. */
public final class PerformanceProbe {
    static MainActivity app;
    static void edt(Runnable r)throws Exception{SwingUtilities.invokeAndWait(r);}
    static void paint(BufferedImage target){java.awt.Graphics2D g=target.createGraphics();app.primary.paint(g);g.dispose();}
    static void report(String name,long[] times){Arrays.sort(times);System.out.printf("%s: median %.2f ms, p95 %.2f ms, max %.2f ms%n",name,times[times.length/2]/1e6,times[(int)(times.length*.95)]/1e6,times[times.length-1]/1e6);}
    public static void main(String[] args)throws Exception{
        if(System.getProperty("fnaf.dataDir")==null)throw new IllegalArgumentException("Private imported data required");
        edt(()->{app=new MainActivity();app.prefs=new SharedPreferences(app.getFilesDir());app.progress=new Progress(null);app.assets=new GameAssets(app);app.audio=new GameAudio(app.assets);app.primary=new PanelView(app,app,true);app.assets.warm();});
        if(!app.assets.ready)throw new IllegalStateException("Import test pack first");
        app.assets.worker.submit(()->{}).get(30,TimeUnit.SECONDS);
        for(int width:new int[]{1280,1920,2560})for(boolean camera:new boolean[]{false,true}){
            int height=width*9/16;BufferedImage image=new BufferedImage(width,height,BufferedImage.TYPE_INT_RGB);
            edt(()->{app.primary.setSize(width,height);app.state.start(1);app.state.elapsed=10;app.state.monitor=camera;app.state.cameraStatic=0;app.state.monitorTime=0;app.state.sounds.clear();paint(image);});
            app.assets.worker.submit(()->{}).get(30,TimeUnit.SECONDS);
            long[] times=new long[80];
            edt(()->{for(int i=0;i<times.length+10;i++){app.state.look=(float)Math.sin(i*.08);app.state.ticks=i;long start=System.nanoTime();paint(image);if(i>=10)times[i-10]=System.nanoTime()-start;}});
            report((camera?"Camera":"Office")+" "+width,times);
        }
        edt(()->{DesktopController pad=new DesktopController(app);long[] times=new long[100];for(int i=0;i<times.length;i++){long t=System.nanoTime();pad.poll();times[i]=System.nanoTime()-t;}report("XInput poll (current connected devices)",times);});
        edt(()->{app.assets.close();app.audio.stopAll();});System.exit(0);
    }
}
