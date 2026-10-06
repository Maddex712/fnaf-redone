package dev.thor.fnaf;
import android.media.MediaPlayer;
import java.io.File;
import javax.sound.sampled.*;
import javax.swing.SwingUtilities;
import java.util.concurrent.*;
/** Private synthesized fixture checks real Windows audio completion, pause and release. */
public final class AudioSmokeTest {
    public static void main(String[] args)throws Exception{
        File file=File.createTempFile("fnaf-audio-test", ".wav");
        AudioFormat format=new AudioFormat(22050,16,1,true,false);
        byte[] samples=new byte[22050*2];
        try(AudioInputStream in=new AudioInputStream(new java.io.ByteArrayInputStream(samples),format,22050)){AudioSystem.write(in,AudioFileFormat.Type.WAVE,file);}
        CountDownLatch prepared=new CountDownLatch(1),completed=new CountDownLatch(1);java.util.concurrent.atomic.AtomicReference<String> error=new java.util.concurrent.atomic.AtomicReference<>();
        MediaPlayer player=new MediaPlayer();player.setDataSource(file.getPath());player.setOnPreparedListener(m->{prepared.countDown();m.start();});player.setOnCompletionListener(m->completed.countDown());player.setOnErrorListener((m,w,e)->{error.set("Audio open failed");completed.countDown();return true;});
        SwingUtilities.invokeAndWait(player::prepareAsync);
        if(!prepared.await(5,TimeUnit.SECONDS))throw new AssertionError("Prepare failed: "+error.get());
        Thread.sleep(120);SwingUtilities.invokeAndWait(player::pause);
        if(completed.await(150,TimeUnit.MILLISECONDS))throw new AssertionError("Paused clip completed early");
        SwingUtilities.invokeAndWait(player::start);
        if(!completed.await(5,TimeUnit.SECONDS)||error.get()!=null)throw new AssertionError("Audio did not complete: "+error.get());
        SwingUtilities.invokeAndWait(player::release);file.delete();System.out.println("Audio: prepare, pause, resume, completion and release passed");System.exit(0);
    }
}
