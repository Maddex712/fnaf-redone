package android.view;
public class View extends javax.swing.JPanel { private android.content.Context context; public android.content.Context getContext(){return context;} public void setFocusableInTouchMode(boolean b){}
 private java.awt.image.BufferedImage frame;
 private final android.graphics.Canvas.LayerPool layers=new android.graphics.Canvas.LayerPool();
 private boolean bounded=Boolean.getBoolean("fnaf.softwareRenderer");
 private int slowFrames;
 public View(android.content.Context c){context=c;setOpaque(true);setBackground(java.awt.Color.BLACK);setFocusable(true);java.awt.event.MouseAdapter m=new java.awt.event.MouseAdapter(){void send(java.awt.event.MouseEvent e,int a){onTouchEvent(new MotionEvent(a,e.getX(),e.getY()));}public void mousePressed(java.awt.event.MouseEvent e){requestFocusInWindow();send(e,MotionEvent.ACTION_DOWN);}public void mouseReleased(java.awt.event.MouseEvent e){send(e,MotionEvent.ACTION_UP);}public void mouseDragged(java.awt.event.MouseEvent e){send(e,MotionEvent.ACTION_MOVE);}};addMouseListener(m);addMouseMotionListener(m);}
 public void setDefaultFocusHighlightEnabled(boolean b){}public void setContentDescription(String s){getAccessibleContext().setAccessibleName(s);}protected void onDraw(android.graphics.Canvas c){}public void invalidate(){super.invalidate();repaint();}public boolean onTouchEvent(MotionEvent e){return false;}public boolean performClick(){return true;}
 protected void paintComponent(java.awt.Graphics graphics){
  int w=getWidth(),h=getHeight();if(w<=0||h<=0)return;
  if(!bounded){
   // Keep direct GPU drawing where Java2D accelerates it. Fall back after sustained
   // expensive frames (e.g. an unaccelerated high-DPI driver), not a single asset load.
   long start=System.nanoTime();java.awt.Graphics2D nativeFrame=(java.awt.Graphics2D)graphics.create();
   try{onDraw(new android.graphics.Canvas(nativeFrame,w,h,layers));}finally{nativeFrame.dispose();}
   if(System.nanoTime()-start>20_000_000L)slowFrames++;else slowFrames=Math.max(0,slowFrames-1);
   if(slowFrames>=6){bounded=true;layers.clear();}
   return;
  }
  // Compose the game's 720p artwork once; native/HiDPI output only scales this finished frame.
  // The logical coordinates stay in component pixels, preserving shared hit boxes and drag input.
  double scale=Math.min(1,Math.min(1280.0/w,720.0/h));
  int rw=Math.max(1,(int)Math.round(w*scale)),rh=Math.max(1,(int)Math.round(h*scale));
  if(frame==null||frame.getWidth()!=rw||frame.getHeight()!=rh){frame=new java.awt.image.BufferedImage(rw,rh,java.awt.image.BufferedImage.TYPE_INT_RGB);layers.clear();}
  java.awt.Graphics2D scene=frame.createGraphics();
  try{scene.scale((double)rw/w,(double)rh/h);onDraw(new android.graphics.Canvas(scene,w,h,layers));}finally{scene.dispose();}
  java.awt.Graphics2D output=(java.awt.Graphics2D)graphics.create();
  try{output.setRenderingHint(java.awt.RenderingHints.KEY_INTERPOLATION,java.awt.RenderingHints.VALUE_INTERPOLATION_BILINEAR);output.drawImage(frame,0,0,w,h,null);}finally{output.dispose();}
 }
}
