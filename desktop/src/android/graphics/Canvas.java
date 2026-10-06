package android.graphics;
import java.awt.*;import java.awt.geom.*;import java.awt.image.BufferedImage;import java.util.ArrayDeque;
/** Java2D implementation of the drawing subset used by the shared Android renderer. */
public class Canvas {
 Graphics2D g;final int width,height,pixelWidth,pixelHeight;final AffineTransform base;final ArrayDeque<Frame> stack=new ArrayDeque<>();final LayerPool layers;float opacity=1;
 public static final class LayerPool{
  final ArrayDeque<BufferedImage> available=new ArrayDeque<>();
  public void clear(){available.clear();}
  BufferedImage acquire(int w,int h){BufferedImage b=available.pollFirst();if(b==null||b.getWidth()!=w||b.getHeight()!=h)b=new BufferedImage(w,h,BufferedImage.TYPE_INT_ARGB_PRE);Graphics2D g=b.createGraphics();g.setComposite(AlphaComposite.Clear);g.fillRect(0,0,w,h);g.dispose();return b;}
  void release(BufferedImage b){if(available.size()<3)available.addFirst(b);}
 }
 static class Frame{Graphics2D previous;BufferedImage layer;float alpha;Frame(Graphics2D p,BufferedImage b,float a){previous=p;layer=b;alpha=a;}}
 public Canvas(Graphics2D g,int w,int h){this(g,w,h,new LayerPool());}
 public Canvas(Graphics2D g,int w,int h,LayerPool pool){this.g=g;layers=pool;width=w;height=h;base=g.getTransform();pixelWidth=Math.max(1,(int)Math.round(w*Math.abs(base.getScaleX())));pixelHeight=Math.max(1,(int)Math.round(h*Math.abs(base.getScaleY())));}
 public void save(){stack.push(new Frame(g,null,opacity));g=(Graphics2D)g.create();}
 public void saveLayerAlpha(float l,float t,float r,float b,int alpha){BufferedImage layer=layers.acquire(pixelWidth,pixelHeight);stack.push(new Frame(g,layer,alpha/255f));Graphics2D next=layer.createGraphics();next.setTransform(g.getTransform());next.setClip(g.getClip());g=next;}
 public void restore(){Frame f=stack.pop();g.dispose();g=f.previous;if(f.layer!=null){Graphics2D v=(Graphics2D)g.create();v.setTransform(new AffineTransform());v.setComposite(AlphaComposite.SrcOver.derive(f.alpha));v.drawImage(f.layer,0,0,null);v.dispose();layers.release(f.layer);}else opacity=f.alpha;}
 public void translate(float x,float y){g.translate(x,y);}public void scale(float x,float y){g.scale(x,y);}public void rotate(float deg){g.rotate(Math.toRadians(deg));}public void skew(float x,float y){g.shear(x,y);}public Matrix getMatrix(){try{AffineTransform logical=base.createInverse();logical.concatenate(g.getTransform());return new Matrix(logical);}catch(NoninvertibleTransformException e){throw new IllegalStateException(e);}}
 public void clipRect(float l,float t,float r,float b){g.clip(new Rectangle2D.Float(l,t,r-l,b-t));}public void clipRect(RectF r){clipRect(r.left,r.top,r.right,r.bottom);}
 void setup(Paint p,boolean bitmap){g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,p.aa?RenderingHints.VALUE_ANTIALIAS_ON:RenderingHints.VALUE_ANTIALIAS_OFF);g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,p.aa?RenderingHints.VALUE_TEXT_ANTIALIAS_ON:RenderingHints.VALUE_TEXT_ANTIALIAS_OFF);g.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS,RenderingHints.VALUE_FRACTIONALMETRICS_ON);g.setRenderingHint(RenderingHints.KEY_INTERPOLATION,p.filter?RenderingHints.VALUE_INTERPOLATION_BILINEAR:RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);g.setComposite(AlphaComposite.SrcOver.derive(opacity*p.alpha/255f));g.setStroke(new BasicStroke(Math.max(.01f,p.stroke)));g.setPaint(p.shader!=null?p.shader.paint():new java.awt.Color(p.color&0xffffff));}
 void shape(Shape s,Paint p){setup(p,false);if(p.style==Paint.Style.STROKE)g.draw(s);else g.fill(s);}
 public void drawColor(int c){Paint p=new Paint();p.setColor(c);drawRect(0,0,width,height,p);}
 public void drawRect(float l,float t,float r,float b,Paint p){shape(new Rectangle2D.Float(l,t,r-l,b-t),p);}public void drawCircle(float x,float y,float radius,Paint p){shape(new Ellipse2D.Float(x-radius,y-radius,radius*2,radius*2),p);}
 public void drawLine(float x,float y,float xx,float yy,Paint p){setup(p,false);g.draw(new Line2D.Float(x,y,xx,yy));}
 public void drawText(String s,float x,float y,Paint p){setup(p,false);g.setFont(p.typeface.font.deriveFont(p.textSize));g.drawString(s,x,y);}
 public void drawBitmap(Bitmap b,float x,float y,Paint p){drawBitmap(b,null,new RectF(x,y,x+b.getWidth(),y+b.getHeight()),p);}
 public void drawBitmap(Bitmap b,Rect src,RectF dst,Paint p){if(b==null)return;setup(p,true);BufferedImage img=b.filtered(p.colorFilter);Graphics2D v=(Graphics2D)g.create();v.clip(new Rectangle2D.Float(dst.left,dst.top,dst.width(),dst.height()));int sx=src==null?0:src.left,sy=src==null?0:src.top,sw=src==null?b.getWidth():src.right-src.left,sh=src==null?b.getHeight():src.bottom-src.top;v.translate(dst.left,dst.top);v.scale(dst.width()/sw,dst.height()/sh);v.translate(-sx,-sy);v.drawImage(img,0,0,null);v.dispose();}
}
