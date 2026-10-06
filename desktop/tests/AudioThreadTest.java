package android.media;
import javax.sound.sampled.Clip;
import javax.swing.SwingUtilities;
import java.util.concurrent.*;
/** A slow native stop/close must not freeze camera drawing or menu input. */
public final class AudioThreadTest {
 public static void main(String[] args)throws Exception{
  CountDownLatch entered=new CountDownLatch(1),unblock=new CountDownLatch(1),closed=new CountDownLatch(1);
  MediaPlayer p=new MediaPlayer();
  p.clip=(Clip)java.lang.reflect.Proxy.newProxyInstance(Clip.class.getClassLoader(),new Class[]{Clip.class},(o,m,a)->{
   if(m.getName().equals("stop")){if(SwingUtilities.isEventDispatchThread())throw new AssertionError("Native stop on EDT");entered.countDown();unblock.await(3,TimeUnit.SECONDS);}
   if(m.getName().equals("close"))closed.countDown();
   if(m.getReturnType()==boolean.class)return false;if(m.getReturnType()==int.class)return 0;if(m.getReturnType()==long.class)return 0L;return null;
  });
  SwingUtilities.invokeAndWait(p::release);
  if(!entered.await(2,TimeUnit.SECONDS))throw new AssertionError("No native stop");
  long start=System.nanoTime();SwingUtilities.invokeAndWait(()->{});
  if(System.nanoTime()-start>100_000_000L)throw new AssertionError("Audio blocked UI");
  unblock.countDown();if(!closed.await(2,TimeUnit.SECONDS))throw new AssertionError("Clip not closed");
  System.out.println("Audio driver isolation: blocking stop/close leaves game thread responsive");System.exit(0);
 }
}
