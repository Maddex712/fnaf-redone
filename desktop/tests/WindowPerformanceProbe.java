package dev.thor.fnaf;

import android.content.SharedPreferences;
import java.awt.*;
import java.util.*;
import java.util.concurrent.*;
import javax.swing.*;

/** Real presented frames and event-queue latency; no OS input or user save changes. */
public final class WindowPerformanceProbe {
    static MainActivity app;static JFrame window;static DesktopFramePump timer;
    static final java.util.List<Long> paints=new ArrayList<>(),latencies=new ArrayList<>();
    static long firstPaint,lastPaint;
    static void edt(Runnable r)throws Exception{SwingUtilities.invokeAndWait(r);}
    public static void main(String[] args)throws Exception{
        DesktopTiming timing=new DesktopTiming();if(args.length==0||!args[0].equals("--default-timer"))timing.active(true);
        if(System.getProperty("fnaf.dataDir")==null)throw new IllegalArgumentException("Private imported data required");
        edt(()->{
            app=new MainActivity();app.prefs=new SharedPreferences(app.getFilesDir());app.progress=new Progress(null);app.assets=new GameAssets(app);app.audio=new GameAudio(app.assets);app.primary=new PanelView(app,app,true);app.assets.warm();
            app.state.start(1);app.state.elapsed=10;app.state.ticks=600;app.state.monitor=true;app.state.cameraStatic=0;app.state.monitorTime=0;app.state.sounds.clear();app.progress.begin(app.state,-1);
            window=new JFrame("FNaF Redone — private performance check");app.window=window;window.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);window.setContentPane(app.primary);window.setSize(1920,1080);window.setLocation(0,0);window.setVisible(true);
        });
        app.assets.worker.submit(()->{}).get(30,TimeUnit.SECONDS);
        Thread.sleep(800);
        CountDownLatch done=new CountDownLatch(1);
        edt(()->{
            RepaintManager.setCurrentManager(new RepaintManager(){public void paintDirtyRegions(){long start=System.nanoTime();super.paintDirtyRegions();if(!isPaintingThread()&&System.nanoTime()-start<100_000)return;long end=System.nanoTime();if(firstPaint==0)firstPaint=start;lastPaint=end;paints.add(end-start);}boolean isPaintingThread(){return SwingUtilities.isEventDispatchThread();}});
            app.resumed=true;app.lastFrame=System.nanoTime();long begin=System.nanoTime();timer=new DesktopFramePump(()->{
                app.frame.run();
                if(System.nanoTime()-begin>5_000_000_000L){timer.close();done.countDown();}
            });
        });
        ScheduledExecutorService probe=Executors.newSingleThreadScheduledExecutor();
        probe.scheduleAtFixedRate(()->{long sent=System.nanoTime();SwingUtilities.invokeLater(()->latencies.add(System.nanoTime()-sent));},0,10,TimeUnit.MILLISECONDS);
        if(!done.await(20,TimeUnit.SECONDS))throw new AssertionError("Window unresponsive");probe.shutdownNow();
        edt(()->{
            PerformanceProbe.report("Presented frame cost",paints.stream().mapToLong(Long::longValue).toArray());
            PerformanceProbe.report("Event queue latency",latencies.stream().mapToLong(Long::longValue).toArray());
            System.out.printf("Presented %.1f FPS (%d frames), window %dx%d%n",paints.size()/((lastPaint-firstPaint)/1e9),paints.size(),app.primary.getWidth(),app.primary.getHeight());
            System.out.printf("Simulation advanced %.2f seconds during the 5-second check%n",app.state.elapsed-10);
            window.dispose();app.assets.close();app.audio.stopAll();app.timing.close();
        });timing.close();System.exit(0);
    }
}
