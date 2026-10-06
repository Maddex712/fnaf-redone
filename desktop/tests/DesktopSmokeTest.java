package dev.thor.fnaf;

import android.content.SharedPreferences;
import android.graphics.*;
import android.view.KeyEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import javax.imageio.ImageIO;
import javax.swing.SwingUtilities;

/** Requires an owned Steam EXE; uses an isolated data directory supplied by the runner. */
public final class DesktopSmokeTest {
    static MainActivity app;static int checks;static File output;
    static void check(boolean ok,String reason){checks++;if(!ok)throw new AssertionError(reason);}
    static void edt(Runnable r)throws Exception{SwingUtilities.invokeAndWait(r);}
    static BufferedImage render(PanelView view,String name)throws Exception{
        BufferedImage b=new BufferedImage(view.getWidth(),view.getHeight(),BufferedImage.TYPE_INT_ARGB);
        for(int pass=0;pass<2;pass++){
            java.awt.Graphics2D g=b.createGraphics();view.paint(g);g.dispose();
            app.assets.worker.submit(()->{}).get(10,TimeUnit.SECONDS);
        }
        if(name!=null)ImageIO.write(b,"png",new File(output,name+".png"));return b;
    }
    static void key(int k,boolean down){app.handleKey(new KeyEvent(k,down,0));}
    public static void main(String[] args)throws Exception{
        if(args.length<2||args.length>3)throw new IllegalArgumentException("Steam EXE and private test output required; optional --scan");
        output=new File(args[1]);output.mkdirs();
        check(System.getProperty("fnaf.dataDir")!=null,"isolated test data directory required");
        BufferedImage dpiImage=new BufferedImage(200,200,BufferedImage.TYPE_INT_ARGB);
        java.awt.Graphics2D dpiGraphics=dpiImage.createGraphics();dpiGraphics.scale(2,2);
        Canvas dpiCanvas=new Canvas(dpiGraphics,100,100);dpiCanvas.translate(10,20);
        RectF hit=new RectF(0,0,5,5);dpiCanvas.getMatrix().mapRect(hit);
        check(hit.left==10&&hit.top==20&&hit.right==15,"HiDPI mouse hit boxes remain logical coordinates");dpiGraphics.dispose();
        edt(()->{
            app=new MainActivity();app.prefs=new SharedPreferences(app.getFilesDir());app.progress=new Progress(null);
            app.assets=new GameAssets(app);app.audio=new GameAudio(app.assets);app.primary=new PanelView(app,app,true);app.primary.setSize(1280,720);
        });
        CountDownLatch imported=new CountDownLatch(1);
        if(args.length==3&&args[2].equals("--scan")){
            check(!app.assets.ready,"scan integration requires a fresh isolated data directory");
            edt(()->{try{
                app.showHelp=true;render(app.primary,"steam-setup");
                javax.swing.Timer finish=new javax.swing.Timer(100,e->{
                    if(!app.assets.importing&&app.assets.ready){
                        for(java.awt.Window w:java.awt.Window.getWindows())if(w instanceof javax.swing.JDialog)w.dispose();
                        ((javax.swing.Timer)e.getSource()).stop();imported.countDown();
                    }
                });finish.start();
                key(KeyEvent.KEYCODE_ENTER,true);key(KeyEvent.KEYCODE_ENTER,false);
                check(app.scanningSteam,"Enter on setup starts background Steam scan");
            }catch(Exception e){throw new RuntimeException(e);}});
        }else edt(()->app.assets.importPack(new android.net.Uri(new File(args[0])),imported::countDown));
        check(imported.await(150,TimeUnit.SECONDS),"import completed before timeout");
        check(app.assets.ready,app.assets.status);
        check(new File(app.assets.directory,"Images").list().length==605,"605 images imported");
        check(new File(app.assets.directory,"Sounds").list().length==52,"52 sounds imported");
        edt(()->{try{
            render(app.primary,"title");
            app.selected=5;key(KeyEvent.KEYCODE_ENTER,true);key(KeyEvent.KEYCODE_ENTER,false);check(app.options,"menu keyboard opens Options");
            app.back();check(!app.options,"Back closes Options");
            app.openOptions();app.primary.setSize(1280,800);app.detectedDisplay="Display: 1280 x 800 / 16:10 / Auto fit";
            check(app.menuItems()[10].equals("Check for updates")&&app.menuActions()[10]==19,"update option routes correctly");
            check(app.menuActions()[11]==20&&app.menuActions()[12]==11,"rendering option and Back remain available");
            render(app.primary,"options-16-10");app.back();app.primary.setSize(1280,720);
            app.cycleRenderer();check(app.renderMode==1&&app.primary.renderReport().contains("Smooth"),"renderer switches to Smooth");
            check(app.prefs.getInt("render_mode",-1)==1,"rendering preference is saved");
            app.cycleRenderer();check(app.renderMode==2&&app.primary.renderReport().contains("Native"),"renderer switches to Native");
            app.cycleRenderer();check(app.renderMode==0&&app.primary.renderReport().contains("Handheld"),"renderer returns to Handheld");
            app.state.start(1);app.state.elapsed=1;app.state.sounds.clear();
            key(KeyEvent.KEYCODE_BUTTON_L1,true);check(app.state.leftDoor,"L1 closes left door");key(KeyEvent.KEYCODE_BUTTON_L1,false);
            key(KeyEvent.KEYCODE_BUTTON_L2,true);check(app.state.leftLight,"trigger holds left light");key(KeyEvent.KEYCODE_BUTTON_L2,false);check(!app.state.leftLight,"trigger release clears light");
            app.state.leftDoor=false;app.state.leftDoorTime=0;
            BufferedImage office=render(app.primary,"office");
            check((office.getRGB(640,350)&0xffffff)!=0,"office imagery rendered after asset load");
            key(KeyEvent.KEYCODE_BUTTON_X,true);key(KeyEvent.KEYCODE_BUTTON_X,false);check(app.state.monitor,"X opens monitor");app.state.monitorTime=0;
            int camera=app.state.camera;key(KeyEvent.KEYCODE_DPAD_RIGHT,true);key(KeyEvent.KEYCODE_DPAD_RIGHT,false);check(app.state.camera==camera+1,"D-pad moves exactly one camera");
            render(app.primary,"camera");
            app.back();check(app.state.paused,"Back pauses");render(app.primary,"pause");app.back();check(!app.state.paused,"Back resumes");
            app.state.monitor=false;
            DesktopController controller=new DesktopController(app);
            controller.api=(id,p)->{if(id!=0)return 1167;p.setInt(0,1);p.setShort(4,(short)0x200);p.setByte(6,(byte)255);p.setByte(7,(byte)0);p.setShort(8,(short)22000);p.setShort(10,(short)0);return 0;};
            controller.poll();check(app.state.rightDoor,"XInput right shoulder routed");check(app.state.leftLight,"XInput trigger routed");check(app.state.axis>.5f,"XInput stick routed");
            app.controller=controller;
            key(KeyEvent.KEYCODE_A,true);controller.poll();check(app.state.axis==-1,"keyboard look survives a connected controller poll");
            key(KeyEvent.KEYCODE_D,true);controller.poll();check(app.state.axis==0,"opposite held keys cancel");
            key(KeyEvent.KEYCODE_D,false);controller.poll();check(app.state.axis==-1,"releasing one look key preserves the other");
            key(KeyEvent.KEYCODE_A,false);controller.poll();check(app.state.axis>.5f,"stick resumes when keyboard look is released");
            controller.api=(id,p)->1167;controller.poll();check(app.state.paused&&!app.state.leftLight&&app.state.axis==0,"controller disconnect pauses and clears held input");
            check(!app.desktopLeft&&!app.desktopRight,"disconnect clears held keyboard look");
            app.state.paused=false;app.state.monitor=false;app.state.monitorTime=0;app.state.scare=0;app.state.elapsed=10;
            app.primary.setSize(2560,1440);render(app.primary,"office-1440p");
            PanelView.Hit monitorHit=app.primary.hits.stream().filter(h->h.id==4).findFirst().orElseThrow();
            float hitX=monitorHit.box.centerX(),hitY=monitorHit.box.centerY();
            check(hitX>600&&hitX<2000&&hitY>1200&&hitY<1440,"scaled hit box remains in logical component coordinates");
            app.primary.onTouchEvent(new android.view.MotionEvent(android.view.MotionEvent.ACTION_DOWN,hitX,hitY));
            app.primary.onTouchEvent(new android.view.MotionEvent(android.view.MotionEvent.ACTION_UP,hitX,hitY));
            check(app.state.monitor,"monitor click works at 2560x1440 with bounded composition");
            app.primary.setSize(1280,720);app.state.monitor=false;render(app.primary,null);
            check(app.primary.hits.stream().anyMatch(h->h.id==4&&h.box.centerY()<720),"hit boxes adapt after window resize");
            app.state.paused=false;app.state.running=false;app.openJournal(3);render(app.primary,"shop");app.closeSubmenu();
            app.dashboard=true;render(app.primary,"dashboard");app.dashboard=false;
            app.prefs.edit().putInt("smoke",42).apply();check(new SharedPreferences(app.getFilesDir()).getInt("smoke",0)==42,"atomic preference persistence");
            for(int scare=1;scare<=6;scare++){if(scare==4)continue;app.state.start(1);app.state.elapsed=1;app.state.scare=scare;app.state.scareTime=.1;render(app.primary,"scare-"+scare);}
            app.state.running=false;app.state.scare=0;app.state.dead=false;
            long start=System.nanoTime();for(int i=0;i<30;i++)render(app.primary,null);
            System.out.printf("Title render average: %.1f ms at 1280x720%n",(System.nanoTime()-start)/30e6);
        }catch(Exception e){throw new RuntimeException(e);}});
        // A failed replacement must preserve the previous imported game and saves.
        File invalid=new File(output,"invalid.exe");java.nio.file.Files.write(invalid.toPath(),new byte[]{'M','Z',0,0});
        CountDownLatch rejected=new CountDownLatch(1);edt(()->app.assets.importPack(new android.net.Uri(invalid),rejected::countDown));
        check(rejected.await(10,TimeUnit.SECONDS),"invalid import terminates");check(app.assets.ready&&app.assets.status.startsWith("Import failed"),"failed replacement preserves playable data");
        check(new File(app.assets.directory,"Images/00039.png").isFile(),"old pack intact");
        check(!new File(app.getCacheDir(),"steam-import.exe").exists(),"temporary EXE cleaned");
        edt(()->{app.audio.stopAll();app.assets.close();});
        System.out.println("Desktop integration: "+checks+" checks passed");System.exit(0);
    }
}
