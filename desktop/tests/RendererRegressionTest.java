package dev.thor.fnaf;
import android.graphics.*;
import java.awt.image.BufferedImage;
/** Device-space crops and HiDPI hit boxes must survive cached pan rendering. */
public final class RendererRegressionTest {
 static void check(boolean b,String s){if(!b)throw new AssertionError(s);}
 public static void main(String[] args){
  BufferedImage source=new BufferedImage(100,40,BufferedImage.TYPE_INT_RGB);
  for(int y=0;y<40;y++)for(int x=0;x<100;x++)source.setRGB(x,y,x<50?0xffff0000:0xff00ff00);
  Bitmap bitmap=new Bitmap(source);Paint p=new Paint();p.setColor(0xffffffff);
  for(int pan=0;pan<40;pan++){
   BufferedImage target=new BufferedImage(200,160,BufferedImage.TYPE_INT_RGB);java.awt.Graphics2D g=target.createGraphics();g.scale(2,2);
   Canvas c=new Canvas(g,100,80);c.translate(10,10);c.clipRect(0,0,60,40);
   c.drawBitmap(bitmap,new Rect(pan,0,pan+60,40),new RectF(0,0,60,40),p);g.dispose();
   check((target.getRGB(19,40)&0xffffff)==0,"Left crop leaked");check((target.getRGB(140,40)&0xffffff)==0,"Right crop leaked");
   check((target.getRGB(22,40)&0xffffff)==0xff0000,"Wrong pan origin");check((target.getRGB(138,40)&0xffffff)==0x00ff00,"Wrong moving crop");
  }
  DesktopMenuNavigation nav=new DesktopMenuNavigation();
  check(nav.step(.25f,0)==0&&nav.step(.5f,100)==0,"Menu moved from small stick drift");
  check(nav.step(.8f,200)==1&&nav.step(.8f,201)==0&&nav.step(.8f,549)==0,"Initial menu repeat delay");
  check(nav.step(.8f,550)==1&&nav.step(.8f,729)==0&&nav.step(.8f,730)==1,"Held repeat pacing");
  check(nav.step(0,750)==0&&nav.step(-.9f,760)==-1,"Fresh press responds immediately");
  System.out.println("Renderer/input regression: moving crops, HiDPI clipping, drift threshold and repeats passed");
 }
}
