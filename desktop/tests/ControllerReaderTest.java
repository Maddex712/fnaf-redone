package dev.thor.fnaf;

import java.util.concurrent.*;
import javax.swing.SwingUtilities;

/** Slow driver calls must not block gameplay; short button transitions stay ordered. */
public final class ControllerReaderTest {
    public static void main(String[] args)throws Exception{
        java.nio.file.Path temp=java.nio.file.Files.createTempDirectory("fnaf-input-test-");
        System.setProperty("fnaf.dataDir",temp.toString());
        MainActivity app=new MainActivity();app.assets=new GameAssets(app);app.state.start(1);
        DesktopController pad=new DesktopController(app);
        CountDownLatch querying=new CountDownLatch(1),release=new CountDownLatch(1);
        pad.api=(id,p)->{querying.countDown();try{release.await(2,TimeUnit.SECONDS);}catch(InterruptedException e){Thread.currentThread().interrupt();}return 1167;};
        pad.startReader();if(!querying.await(2,TimeUnit.SECONDS))throw new AssertionError("Reader not started");
        long start=System.nanoTime();SwingUtilities.invokeAndWait(pad::poll);
        if(System.nanoTime()-start>100_000_000L)throw new AssertionError("Driver blocked EDT input");
        pad.close();release.countDown();
        if(!pad.reader.awaitTermination(3,TimeUnit.SECONDS))throw new AssertionError("Reader did not stop");
        DesktopController ordered=new DesktopController(app);
        // A reader marker uses queued input; no native call is made by poll().
        ordered.reader=Executors.newSingleThreadScheduledExecutor();
        byte[] down=new byte[17],up=new byte[17];down[5]=1; // L1, 0x100
        ordered.packets.offer(down);ordered.packets.offer(up);
        SwingUtilities.invokeAndWait(ordered::poll);
        if(!app.state.leftDoor||ordered.buttons!=0)throw new AssertionError("Lost or reordered short button transition");
        ordered.packets.offer(down);ordered.suspend();ordered.resume();SwingUtilities.invokeAndWait(ordered::poll);
        if(!app.state.leftDoor||!ordered.packets.isEmpty())throw new AssertionError("Replayed unfocused input");
        ordered.close();app.assets.close();java.nio.file.Files.delete(temp);
        System.out.println("Controller reader: nonblocking delivery, ordered edges, focus clearing and shutdown passed");System.exit(0);
    }
}
