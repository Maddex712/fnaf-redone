package dev.thor.fnaf;

import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import javax.swing.SwingUtilities;

/** A stalled render must not leave a queue full of stale simulation/input callbacks. */
public final class DesktopFramePumpTest {
    public static void main(String[] args)throws Exception{
        AtomicInteger calls=new AtomicInteger();AtomicBoolean wrongThread=new AtomicBoolean();
        CountDownLatch first=new CountDownLatch(1),blocked=new CountDownLatch(1),release=new CountDownLatch(1);
        DesktopFramePump pump=new DesktopFramePump(()->{if(!SwingUtilities.isEventDispatchThread())wrongThread.set(true);calls.incrementAndGet();first.countDown();});
        if(!first.await(2,TimeUnit.SECONDS))throw new AssertionError("No scheduled frame");
        SwingUtilities.invokeLater(()->{blocked.countDown();try{release.await();}catch(InterruptedException e){Thread.currentThread().interrupt();}});
        if(!blocked.await(2,TimeUnit.SECONDS))throw new AssertionError("EDT did not enter fixture");
        int before=calls.get();Thread.sleep(180);
        if(calls.get()!=before)throw new AssertionError("Game state updated off the EDT");
        CountDownLatch drained=new CountDownLatch(1);
        SwingUtilities.invokeLater(()->{if(calls.get()>before+1)wrongThread.set(true);pump.close();drained.countDown();});
        release.countDown();if(!drained.await(2,TimeUnit.SECONDS))throw new AssertionError("EDT failed to recover");
        int after=calls.get();Thread.sleep(80);SwingUtilities.invokeAndWait(()->{});
        if(calls.get()!=after)throw new AssertionError("Frame executed after shutdown");
        if(wrongThread.get())throw new AssertionError("Wrong thread or queued frame backlog");
        System.out.println("Frame pump: EDT confinement, bounded backlog, recovery, and shutdown passed");System.exit(0);
    }
}
