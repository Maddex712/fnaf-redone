package dev.thor.fnaf;

import android.content.Context;
import android.graphics.*;
import android.view.MotionEvent;
import android.view.View;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Locale;

final class PanelView extends View {
    static final int BG=Color.BLACK,WHITE=Color.WHITE,GREEN=Color.rgb(150,193,73),MUTED=Color.rgb(185,185,185),RED=Color.rgb(236,104,91);
    final MainActivity app;final boolean primary;final Paint p=new Paint(Paint.ANTI_ALIAS_FLAG|Paint.FILTER_BITMAP_FLAG);
    final ArrayList<Hit> hits=new ArrayList<>();final HashMap<Integer,Integer> held=new HashMap<>();
    final HashMap<Integer,Float> pan=new HashMap<>();
    Canvas c;float scale=1,offsetX,offsetY;
    final Paint titlePaint=new Paint(Paint.FILTER_BITMAP_FLAG);
    final Paint cctvPaint=new Paint();
    final Shader contactShadow=new RadialGradient(0,0,1,new int[]{0xcc000000,0x65000000,0x00000000},new float[]{0,.4f,1},Shader.TileMode.CLAMP);
    final Paint dreamPaint=new Paint(Paint.ANTI_ALIAS_FLAG);
    final Shader dreamEdge=new RadialGradient(500,435,680,new int[]{0x00000000,0x00000000,0x22000000,0x9c000000},new float[]{0,.55f,.80f,1},Shader.TileMode.CLAMP);
    Bitmap[] titleNoise;
    final TitleEffects titleEffects=new TitleEffects();
    final Paint eyePaint=new Paint(Paint.ANTI_ALIAS_FLAG);
    final Paint handPaint=new Paint(Paint.FILTER_BITMAP_FLAG);
    final float[][] eyeCenters={{698f/1672*1000,369f/941*650},{1063f/1672*1000,342f/941*650},{1455f/1672*1000,344f/941*650}};
    final Shader eyeShader=new RadialGradient(0,0,6.3f,new int[]{0xffe9e7df,0xffe9e7df,0x85d5d5cf,0x00b8b8b0},new float[]{0,.25f,.50f,1},Shader.TileMode.CLAMP);

    void prepareTitleFilter(){
        if(titleNoise!=null)return;
        ColorMatrix washed=new ColorMatrix();washed.setSaturation(.62f);
        titlePaint.setColorFilter(new ColorMatrixColorFilter(washed));
        titleNoise=new Bitmap[4];int[] pixels=new int[640*360];
        java.util.Random random=new java.util.Random(1987);
        for(int frame=0;frame<titleNoise.length;frame++){
            for(int i=0;i<pixels.length;i++)pixels[i]=(random.nextInt(39)<<24)|0x00ffffff;
            titleNoise[frame]=Bitmap.createBitmap(pixels,640,360,Bitmap.Config.ARGB_8888);
        }
    }
    static final class Hit {final RectF box;final int id;Hit(RectF r,int i){box=r;id=i;}}
    static Typeface menuFont;
    static Typeface loadFont(Context context){
        if(menuFont!=null)return menuFont;
        java.io.File file=new java.io.File(context.getFilesDir(),"menu-font.ttf");
        if(file.isFile())try{return menuFont=Typeface.createFromFile(file);}catch(RuntimeException ignored){}
        return menuFont=Typeface.MONOSPACE;
    }
    PanelView(Context context,MainActivity app,boolean primary){super(context);this.app=app;this.primary=primary;setFocusable(true);setFocusableInTouchMode(true);setDefaultFocusHighlightEnabled(false);p.setTypeface(loadFont(context));setContentDescription(primary?"FNaF office and menus":"FNaF camera monitor");}
    void rect(float x,float y,float w,float h,int color){p.setColor(color);p.setStyle(Paint.Style.FILL);c.drawRect(x,y,x+w,y+h,p);}
    void line(float x,float y,float xx,float yy,int color,float width){p.setColor(color);p.setStrokeWidth(width);c.drawLine(x,y,xx,yy,p);}
    String points(int value){return String.format(java.util.Locale.US,"%,d",value);}
    void text(String s,float x,float y,float size,int color){p.setColor(color);p.setTextSize(size);p.setStyle(Paint.Style.FILL);c.drawText(s,x,y,p);}
    void lines(String s,float x,float y,float size,int color){for(String v:s.split("\n")){text(v,x,y,size,color);y+=size*1.6f;}}
    void hit(float x,float y,float w,float h,int id){
        Matrix m=c.getMatrix();RectF r=new RectF(x,y,x+w,y+h);m.mapRect(r);hits.add(new Hit(r,id));
    }
    void outline(float x,float y,float w,float h,int color,float width){
        p.setColor(color);p.setStrokeWidth(width);p.setStyle(Paint.Style.STROKE);c.drawRect(x,y,x+w,y+h,p);p.setStyle(Paint.Style.FILL);
    }
    void button(String s,float x,float y,float w,float h,int id,boolean active){
        rect(x,y,w,h,active?0xe0658036:0xc0222222);outline(x,y,w,h,WHITE,1.5f);
        text(s,x+10,y+h*.66f,Math.min(21,h*.39f),WHITE);hit(x,y,w,h,id);
    }
    void menuItem(String s,float x,float y,float w,float h,int id,boolean selected){
        text(selected?">>":"",x,y+h*.7f,26,WHITE);text(s,x+48,y+h*.7f,26,WHITE);hit(x,y,w,h,id);
    }
    void monitorBar(float h){
        outline(240,h-42,520,31,0xffaaaaaa,2);
        for(int x=385;x<620;x+=180){line(x,h-33,x+15,h-21,WHITE,3);line(x+15,h-21,x+30,h-33,WHITE,3);}
        hit(230,h-52,540,52,4);
    }
    @Override protected void onDraw(Canvas canvas){
        super.onDraw(canvas);c=canvas;hits.clear();c.drawColor(BG);
        if(app.outro.active()){drawOutro();return;}
        if(app.intro.active()){drawIntro();return;}
        if(!primary&&app.state.running&&app.state.scare>0){
            // Keep the companion display black for the entire jumpscare. The
            // pond begins its reveal only after the character's attack ends.
            if(app.state.dead)drawDeathCompanion();
            return;
        }
        if(app.menu()||app.diagnostic||app.showHelp||app.bindIndex>=0||app.customizing){
            if(primary&&app.dashboard)drawDashboard();
            else if(primary)drawMenu();else drawCompanion();drawNotice();drawMenuReveal();return;
        }
        if(primary&&app.presentation==null){
            float w=Math.min(getWidth(),getHeight()*16f/9f),h=w*9f/16f;
            GameState s=app.state;
            panel((getWidth()-w)/2,(getHeight()-h)/2,w,h,ScreenMode.showCamera(s.monitor,s.scare,s.dead,s.won,s.powerStage));
        }else panel(0,0,getWidth(),getHeight(),primary?app.swapped:!app.swapped);
        if(app.state.running&&!app.state.paused&&app.state.elapsed<.12)rect(0,0,getWidth(),getHeight(),((int)(255*(1-app.state.elapsed/.12)))<<24);
        drawNotice();
    }
    void panel(float x,float y,float w,float h,boolean camera){
        c.save();c.clipRect(x,y,x+w,y+h);c.translate(x,y);c.scale(w/1000,w/1000);float logicalHeight=h/w*1000;
        if(camera)drawCameras(logicalHeight);else drawOffice(logicalHeight);
        if(app.state.paused&&!primary){rect(0,0,1000,logicalHeight,0x99000000);text("PAUSED",42,logicalHeight/2,40,GREEN);}
        c.restore();
    }
    void bitmapFit(int id,RectF dst,boolean fill){
        Bitmap b=app.assets.bitmap(id);if(b==null)return;
        float factor=fill?Math.max(dst.width()/b.getWidth(),dst.height()/b.getHeight()):Math.min(dst.width()/b.getWidth(),dst.height()/b.getHeight());
        float w=b.getWidth()*factor,h=b.getHeight()*factor;RectF out=new RectF(dst.centerX()-w/2,dst.centerY()-h/2,dst.centerX()+w/2,dst.centerY()+h/2);
        c.save();c.clipRect(dst);p.setColor(Color.WHITE);p.setAlpha(255);c.drawBitmap(b,null,out,p);c.restore();
    }
    void scareFit(int id,RectF dst){
        Bitmap b=app.assets.scareFrame(id);if(b==null)return;
        float factor=Math.max(dst.width()/b.getWidth(),dst.height()/b.getHeight());
        float w=b.getWidth()*factor,h=b.getHeight()*factor;
        RectF out=new RectF(dst.centerX()-w/2,dst.centerY()-h/2,dst.centerX()+w/2,dst.centerY()+h/2);
        c.save();c.clipRect(dst);p.setColor(Color.WHITE);p.setAlpha(255);c.drawBitmap(b,null,out,p);c.restore();
    }
    void sprite(int object,int anim,double time,float x,float y,boolean once){
        int id=app.assets.image(object,anim,time,once);
        Bitmap b=app.state.scare>0?app.assets.scareFrame(id):app.assets.bitmap(id);
        if(b==null)return;int[] m=app.assets.meta[id];p.setColor(Color.WHITE);c.drawBitmap(b,x-m[2],y-m[3],p);
    }
    void drawOffice(float h){
        GameState s=app.state;rect(0,0,1000,h,BG);
        float viewportH=562.5f,yy=(h-viewportH)/2;
        c.save();c.clipRect(0,yy,1000,yy+viewportH);c.translate(0,yy);c.scale(1000f/1280,1000f/1280);
        float panX=160+s.look*160;c.translate(-panX,0);
        if(s.scare==5){c.translate(panX,0);scareFit(app.assets.image(152,0,s.scareAnimationTime(),true),new RectF(0,0,1280,720));}
        else if(s.scare==6){c.translate(panX,0);scareFit(548,new RectF(0,0,1280,720));}
        else if(s.powerStage<4||s.scare>0){
            sprite(44,s.officeAnimation(),s.scare>0?s.scareAnimationTime():0,0,0,s.scare>0);
            if(s.scare==0&&s.powerStage==0){
                sprite(58,0,s.elapsed,868,400,false);
                drawDeskPlushies();
                sprite(61,s.leftLight?(s.leftDoor?14:13):(s.leftDoor?12:0),0,48,390,false);
                sprite(65,s.rightLight?(s.rightDoor?14:13):(s.rightDoor?12:0),0,1546,400,false);
                sprite(59,s.leftDoorTime>0?(s.leftDoor?12:14):(s.leftDoor?13:0),s.leftDoorTime>0?GameState.DOOR_DURATION-s.leftDoorTime:0,72,-1,true);
                sprite(60,s.rightDoorTime>0?(s.rightDoor?12:14):(s.rightDoor?13:0),s.rightDoorTime>0?GameState.DOOR_DURATION-s.rightDoorTime:0,1270,-2,true);
                if(s.golden==2&&!s.monitor)sprite(146,0,0,660,478,true);
                if(!s.monitor){hit(662,224,32,32,22);hit(6,285,92,102,0);hit(6,387,92,107,2);hit(1497,285,92,102,1);hit(1497,387,92,107,3);}

            }
        }
        c.restore();
        if(!s.running)return;
        if(s.dead||s.won){result(h);return;}
        if(s.scare>0)return;
        text(s.hour+" AM",864,40,25,WHITE);text("Night "+s.night,864,68,18,WHITE);
        if(s.powerStage>0)return;
        text("Power left: "+(int)s.power+"%",25,h-94,18,WHITE);
        text("Usage:",25,h-65,18,WHITE);for(int i=0;i<s.usage();i++)rect(108+i*15,h-84,11,23,i>2?RED:GREEN);
        monitorBar(h);
        text("II",954,h-22,19,0xffaaaaaa);hit(932,h-65,68,65,20);
        if(s.elapsed<40&&!s.mutedCall){outline(26,24,135,31,0xffbbbbbb,1);text("Mute Call",36,46,18,WHITE);hit(20,15,155,50,21);}
    }
    void drawCameras(float h){
        GameState s=app.state;rect(0,0,1000,h,BG);
        boolean live=s.monitor&&s.powerStage==0;
        if(live&&s.cameraStatic<=0&&s.camera!=9){
            double t=s.fox==4?s.foxWait/60.0:0;
            Bitmap feed=app.assets.bitmap(app.assets.image(44,s.cameraAnimation(),t,false));
            if(feed!=null){
                // Preserve the original 1280-wide moving viewport, then fit it to the lower display.
                int cropWidth=Math.min(1280,feed.getWidth());
                int panX=Math.min(s.cameraPan(),feed.getWidth()-cropWidth);
                Rect src=new Rect(panX,0,panX+cropWidth,feed.getHeight());
                float zoom=Math.max(1000f/cropWidth,h/feed.getHeight());
                float w=cropWidth*zoom,hh=feed.getHeight()*zoom;
                p.setColor(WHITE);c.drawBitmap(feed,src,new RectF((1000-w)/2,(h-hh)/2,(1000+w)/2,(h+hh)/2),p);
            }
        }
        if(live){
            Bitmap noise=app.assets.bitmap(app.assets.image(42,0,s.elapsed,false));
            if(noise!=null){
                p.setColor(WHITE);p.setAlpha(18+s.flicker);
                c.drawBitmap(noise,null,new RectF(0,0,1000,h),p);
                p.setAlpha(255);
            }
            for(int i=0;i<h;i+=5)line(0,i,1000,i,0x25000000,1);
            if(s.cameraStatic>0||s.monitorTime>0){for(int i=0;i<80;i++){float yy=(i*47+s.ticks*31)%Math.max(1,(int)h);rect(0,yy,1000,2+(i%5),0x559b9b9b);}}
            outline(18,18,964,h-36,WHITE,2);
            if(s.ticks%60<35){p.setColor(RED);c.drawCircle(56,55,13,p);}
            if(s.camera==9)lines("-CAMERA DISABLED-\nAUDIO ONLY",265,180,28,WHITE);
            if(s.cameraStatic>0)text("SIGNAL INTERRUPTED",260,180,27,WHITE);
        }else{
            text("STANDBY",58,88,30,MUTED);text("X  Raise monitor",58,126,20,MUTED);
        }
        float mapY=h-470;
        c.save();c.translate(530,mapY+10);c.scale(1.05f,1.05f);
        bitmapFit(app.assets.image(74,0,0,false),new RectF(0,0,400,400),false);
        int[][] nodes={{116,28},{94,84},{63,162},{92,277},{92,317},{32,261},{221,279},{221,319},{0,111},{318,243},{327,111}};
        int[] labelImages={170,171,177,172,165,168,169,173,174,175,176};
        for(int i=0;i<nodes.length;i++){
            int x=nodes[i][0],y=nodes[i][1];rect(x,y,60,38,s.camera==i?0xff718b39:0xff454545);outline(x,y,60,38,WHITE,1.5f);
            Bitmap label=app.assets.bitmap(labelImages[i]);
            if(label!=null){p.setColor(WHITE);p.setFilterBitmap(false);c.drawBitmap(label,null,new RectF(x+6,y+5,x+6+label.getWidth(),y+5+label.getHeight()),p);p.setFilterBitmap(true);}
            else {text("CAM",x+5,y+13,12,WHITE);text(GameState.CAMERAS[i],x+5,y+30,17,WHITE);}
            hit(x-4,y-1,68,40,30+i);
        }
        c.restore();
        String[] roomLabels={"Show Stage","Dining Area","Pirate Cove","West Hall","W. Hall Corner","Supply Closet","East Hall","E. Hall Corner","Backstage","Kitchen","Restrooms"};
        text(roomLabels[s.camera],530,mapY-12,27,WHITE);
        monitorBar(h);hit(30,h-65,150,60,5);hit(820,h-65,150,60,6);
        text("<",48,h-24,26,WHITE);text(">",930,h-24,26,WHITE);
        if(primary&&app.presentation==null){
            text(s.hour+" AM / Night "+s.night,104,57,20,WHITE);
            text("Power: "+(int)s.power+"%",35,h-86,19,WHITE);
            text("II",940,57,24,WHITE);hit(905,18,70,65,20);
        }
        if(s.dead||s.won)result(h);
    }
    void drawOutro(){
        NightOutro o=app.outro;
        c.save();
        float fit=Math.min(getWidth()/1000f,getHeight()/562.5f);
        c.translate((getWidth()-1000*fit)/2,(getHeight()-562.5f*fit)/2);
        c.scale(fit,fit);
        rect(0,0,1000,562.5f,BG);
        if(primary){
            if(o.stage==2){
                bitmapFit(o.endingImage(),new RectF(0,0,1000,562.5f),false);
                float fade=(float)Math.min(1,Math.min(o.elapsed/.6,(NightOutro.ENDING-o.elapsed)/.6));
                rect(0,0,1000,562.5f,Math.round(255*(1-Math.max(0,fade)))<<24);
                if(o.elapsed>1)text("A / B  Continue",800,544,15,0xffbbbbbb);
                hit(0,0,1000,562.5f,300);
            }else{
                float shift=o.roll()*100;
                c.save();c.clipRect(416,231,467,313);
                bitmapFit(350,new RectF(422,237-shift,464,309-shift),false);
                bitmapFit(351,new RectF(422,337-shift,464,409-shift),false);c.restore();
                bitmapFit(352,new RectF(491,237,573,309),false);
                if(o.cheered){
                    float t=(float)(o.elapsed-NightOutro.ROLL);
                    int[] colors={0xffbca572,0xffcacaca,0xff7c8f9b,0xff98896d};
                    for(int i=0;i<100;i++){
                        float x=(i*97%1000)+(float)Math.sin(t*2+i)*20;
                        float y=-80-(i*43%180)+t*(145+i%65);
                        if(y>565)continue;
                        c.save();c.translate(x,y);c.rotate(i*31+t*65);
                        rect(-2,-3,3+i%3,6,colors[i%4]);c.restore();
                    }
                    if(app.presentation==null){
                        text("Shift complete  /  +"+points(app.runPoints)+" pts",315,402,23,WHITE);
                        text("Balance: "+points(app.progress.points)+" pts",370,441,20,MUTED);
                    }
                }
            }
        }else{
            text("SHIFT COMPLETE",60,100,35,WHITE);
            text("Night "+o.night+" survived",60,155,27,WHITE);
            text("Earned: +"+points(app.runPoints)+" pts",60,218,23,GREEN);
            text("Balance: "+points(app.progress.points)+" pts",60,259,23,WHITE);
            String status=o.campaign?(o.night==5?"6th Night unlocked":o.night==6?"Custom Night unlocked":o.night<5?"Next shift begins shortly":"Custom night complete"):"Assignment complete";
            text(status,60,328,23,MUTED);
            int stars=NightOutro.starMask(app.progress,app.unlocked);
            for(int i=0;i<3;i++)if((stars&(1<<i))!=0)bitmapFit(432,new RectF(60+i*60,380,100+i*60,420),false);
            if(o.stage==2&&o.elapsed>1)button("A / B  CONTINUE",60,468,880,55,300,false);
        }
        c.restore();
    }
    void drawDeathCompanion(){
        c.save();float fit=Math.min(getWidth()/1000f,getHeight()/870f);
        c.translate((getWidth()-1000*fit)/2,(getHeight()-870*fit)/2);c.scale(fit,fit);
        float reveal=TitleEffects.smooth((float)Math.min(1,app.deathElapsed/1.8));
        c.saveLayerAlpha(0,0,1000,870,Math.round(255*reveal));
        Bitmap scene=app.assets.deathScene;
        if(scene!=null){
            float unit=1000f/scene.getWidth(),sceneH=scene.getHeight()*unit,top=(870-sceneH)/2;
            p.setColor(WHITE);p.setFilterBitmap(false);
            c.drawBitmap(scene,null,new RectF(0,top,1000,top+sceneH),p);
            float bx=105,by=83,bw=790,bh=150;
            rect(bx,by,bw,bh,BG);outline(bx,by,bw,bh,0xffcccccc,2);
            c.save();c.translate(bx+14,by+45);c.scale(2,2);
            p.setAntiAlias(false);p.setTypeface(Typeface.DEFAULT_BOLD);
            String purple="he'll";text(purple,0,0,14,0xffa972cc);
            float word=p.measureText(purple);text(" pay soon.",word,0,14,WHITE);
            p.setTypeface(loadFont(getContext()));p.setAntiAlias(true);c.restore();
            p.setFilterBitmap(true);
        }
        drawDreamFog(reveal);
        c.restore();
        float fadeOut=TitleEffects.smooth((float)((app.deathElapsed-4.0)/1.0));
        if(fadeOut>0)rect(0,0,1000,870,Math.round(255*fadeOut)<<24);
        c.restore();
    }
    void drawDreamFog(float reveal){
        long now=android.os.SystemClock.uptimeMillis();
        float drift=(float)Math.sin(now/1100.0)*12;
        int mist=Math.round((72+10*(float)Math.sin(now/900.0))*reveal);
        float[][] clouds={
            {-35,70,210,145},{175+drift,-22,225,125},{470-drift,-28,245,130},{790+drift,-18,225,140},
            {1030,105,205,165},{1025,390+drift,190,225},{1020,720-drift,220,180},
            {790-drift,888,235,155},{500+drift,900,280,175},{190-drift,888,240,160},
            {-28,730+drift,220,190},{-35,430-drift,185,235}
        };
        for(int i=0;i<clouds.length;i++){
            float[] v=clouds[i];int alpha=Math.min(130,mist+(i%3)*12);
            drawFogCloud(v[0],v[1],v[2],v[3],alpha);
        }
        dreamPaint.setShader(dreamEdge);c.drawRect(0,0,1000,870,dreamPaint);
        dreamPaint.setShader(null);
    }
    void drawFogCloud(float x,float y,float rx,float ry,int alpha){
        c.save();c.translate(x,y);c.scale(rx,ry);
        int center=(alpha<<24)|0x00c5c9c1,mid=((alpha*2/3)<<24)|0x00aeb4ad;
        dreamPaint.setShader(new RadialGradient(0,0,1,new int[]{center,mid,0x00a0a69f},new float[]{0,.46f,1},Shader.TileMode.CLAMP));
        c.drawCircle(0,0,1,dreamPaint);c.restore();
    }
    void result(float h){
        if(app.state.dead){
            rect(0,0,1000,h,BG);
            p.setTextSize(36);String label="you died.";
            text(label,(1000-p.measureText(label))/2,h*.48f,36,WHITE);
            text(String.format(Locale.US,"%.1f",Math.max(0,5-app.deathElapsed)),915,h-25,16,0xff555555);
            float fadeOut=TitleEffects.smooth((float)((app.deathElapsed-4.0)/1.0));
            if(fadeOut>0)rect(0,0,1000,h,Math.round(255*fadeOut)<<24);return;
        }
        GameState s=app.state;rect(0,0,1000,h,0xef050807);
        text(s.won?"6 AM":"GAME OVER",100,h*.4f,80,s.won?GREEN:WHITE);
        String sub=s.won?"Night "+s.night+" complete.":"The night shift has ended.";
        text(sub,105,h*.4f+55,25,MUTED);
        if(s.won&&s.night>=5)text(s.night==5?"You've earned your paycheck.":s.night==6?"Overtime complete.":"Custom night complete.",105,h*.4f+95,23,WHITE);
        text("Earned this shift: "+app.runPoints+" pts   /   Balance: "+points(app.progress.points),105,h*.4f+130,20,GREEN);
        button("A  CONTINUE",105,h*.70f,440,65,300,true);
    }
    void drawTitleScene(float intensity,long elapsed,boolean active){
        RectF dst=new RectF(0,0,1000,650);
        titlePaint.setAlpha(255);c.drawBitmap(app.assets.title,null,dst,titlePaint);
        float opacity=active?TitleEffects.goldenOpacity(elapsed):0;
        if(opacity>0&&app.assets.titleVariants[1]!=null&&app.assets.titleVariants[2]!=null){
            float mix=TitleEffects.fredbearMix(elapsed),b=opacity*mix;
            float a=b>=1?0:opacity*(1-mix)/(1-b);
            titlePaint.setAlpha(Math.round(255*a));c.drawBitmap(app.assets.titleVariants[1],null,dst,titlePaint);
            titlePaint.setAlpha(Math.round(255*b));c.drawBitmap(app.assets.titleVariants[2],null,dst,titlePaint);
            titlePaint.setAlpha(255);
        }
        float handIntensity=active?TitleEffects.eyeOpacity(elapsed,2,titleEffects.eye,intensity):0;
        if(handIntensity>0&&app.assets.titleHandprint!=null){
            handPaint.setAlpha((int)(220*handIntensity));handPaint.setColorFilter(titlePaint.getColorFilter());
            c.drawBitmap(app.assets.titleHandprint,null,new RectF(0,0,1000,650),handPaint);
        }
        for(int eye=0;eye<eyeCenters.length;eye++){
            float brightness=active?TitleEffects.eyeOpacity(elapsed,eye,titleEffects.eye,intensity):0;
            if(brightness<=0)continue;
            float[] center=eyeCenters[eye];
            c.save();c.translate(center[0],center[1]);
            // Counter the logical menu's non-square scaling so the pinpoint stays circular.
            c.scale(1,650f/562.5f);
            eyePaint.setShader(eyeShader);eyePaint.setAlpha((int)(255*brightness));
            c.drawCircle(0,0,6.3f,eyePaint);c.restore();
        }
    }
    void drawTitleBackdrop(){
        if(app.assets.title==null)return;
        prepareTitleFilter();
        long clock=android.os.SystemClock.uptimeMillis(),elapsed=clock-app.menuEpoch;
        boolean active=!app.state.running&&app.journal==0&&!app.options&&!app.showHelp&&!app.diagnostic&&!app.customizing;
        float intensity=active?titleEffects.update(elapsed):0;
        drawTitleScene(intensity,elapsed,active);
        long phase=clock%3200;
        if(phase<220||(phase>480&&phase<600)){
            // Tear the complete composed scene, including every shoulder and the apparition.
            for(int i=0;i<9;i++){
                float sy=(clock/29*73+i*71)%650,sh=3+i*2;
                c.save();c.clipRect(0,sy,1000,sy+sh);c.translate((i%2==0?1:-1)*(4+i*2),0);
                drawTitleScene(intensity,elapsed,active);c.restore();
                if(i%3==0)line(0,sy+sh,1000,sy+sh,0x24e2e2e2,1);
            }
            rect(0,0,1000,650,0x0b000000);
        }
    }
    void drawTitleCrt(){
        prepareTitleFilter();
        long clock=android.os.SystemClock.uptimeMillis();
        c.save();c.clipRect(0,0,1000,650);
        c.drawBitmap(titleNoise[(int)((clock/65)%titleNoise.length)],null,new RectF(0,0,1000,650),cctvPaint);
        for(int y=0;y<650;y+=3)line(0,y,1000,y,0x38000000,1f);
        float band=(clock%8500)/8500f*730-80;
        rect(0,band,1000,42,0x16000000);
        line(0,band+43,1000,band+43,0x14ffffff,1);
        c.restore();
    }
    void drawMenu(){
        c.save();c.scale(getWidth()/1000f,getHeight()/650f);
        drawTitleBackdrop();
        boolean sub=app.journal!=0||app.options||app.bindIndex>=0||app.diagnostic||app.showHelp||app.customizing;
        if(sub){rect(0,0,1000,650,0xc0000000);text(app.options?"Options":"Five Nights at Freddy's",44,65,30,WHITE);}
        else if(app.state.running)text("Paused",45,110,48,WHITE);
        else bitmapFit(444,new RectF(42,35,245,265),false);
        if(app.journal!=0&&!app.showHelp){
            if(app.journal==3)drawShop(false);else drawJournal();
        }else if(app.bindIndex>=0){
            text("BUTTON REMAPPING",45,220,30,WHITE);text((app.bindIndex+1)+" / 7  •  "+MainActivity.ACTIONS[app.bindIndex],45,285,29,GREEN);
            lines("Press the button to use for this action.\nStart / Back cancels and restores your old mapping.\nStick and trigger axes retain their default roles.",45,345,20,MUTED);
            button("CANCEL",45,535,280,53,301,false);
        }else if(app.diagnostic){
            text("DISPLAY & CONTROLLER CHECK",45,205,26,WHITE);lines(app.diagnostics(),45,250,17,MUTED);
            text(app.lastInput,45,485,21,GREEN);button("B  BACK",45,545,280,53,301,false);
        }else if(app.showHelp){
            if(!app.assets.ready){
                text("BRING YOUR STEAM COPY",45,198,26,WHITE);
                lines("1. In Steam, browse FNaF 1's installed files.\n2. Copy FiveNightsatFreddys.exe to this device.\n3. Select it below. Setup happens here, offline.\n\nAn existing FNaF 1 data ZIP also works.\nNo Steam login or PC extraction tool is needed.",45,245,22,MUTED);
                button("A  SELECT STEAM FILE",45,530,540,55,640,true);
                button("B  BACK",650,530,280,55,301,false);
            }else{
            text("HOW TO PLAY",45,198,26,WHITE);
            lines("Left stick: look around the office\nL1 / R1: left / right door\nL2 / R2: hold left / right light\nX: raise / lower monitor • D-pad: previous / next camera\nY: mute phone call • Start: pause\nTouch controls work on both screens.\n\nThis alpha includes reconstructed gameplay and your local assets.\nNative dual-screen build. Original-game parity is still in progress.",45,238,19,MUTED);
            text(app.assets.status,45,550,18,GREEN);button("B  BACK",45,580,280,46,301,false);
            }
        }else if(app.customizing){
            text("CUSTOM NIGHT",45,205,30,WHITE);String[] labels={"FREDDY","BONNIE","CHICA","FOXY"};
            for(int i=0;i<4;i++){button(labels[i]+"     "+app.state.custom[i],45,235+i*61,420,50,450+i,app.customSelected==i);button("−",490,235+i*61,70,50,400+i*2,false);button("+",580,235+i*61,70,50,401+i*2,false);}
            button("START CUSTOM NIGHT",45,500,605,60,460,app.customSelected==4);button("BACK",720,550,210,50,301,false);
        }else{
            String[] labels=app.menuItems();float yy=app.options?115:290;float spacing=app.options?Math.min(51,480f/labels.length):43;
            for(int i=0;i<labels.length;i++)menuItem(labels[i],40,yy+i*spacing,650,spacing,100+i,app.selected==i);
            if(!app.assets.ready)text("Select New Game to import your Steam copy",45,620,17,WHITE);
            if(app.assets.importing){rect(0,105,1000,545,0xf0000000);text(app.assets.status,45,340,24,WHITE);}

        }
        if(!sub&&!app.state.running){
            int stars=NightOutro.starMask(app.progress,app.unlocked);
            for(int i=0;i<3;i++)if((stars&(1<<i))!=0)bitmapFit(432,new RectF(48+i*37,605,76+i*37,633),false);
        }
        drawTitleCrt();
        if(app.buyConfirm>=0)drawPurchase(650);
        c.restore();
    }
    void wrapped(String value,float x,float y,int max,float size,int color){
        for(String paragraph:value.split("\n")){
            String row="";
            for(String word:paragraph.split(" ")){
                if((row+word).length()>max){text(row,x,y,size,color);y+=size*1.5f;row="";}
                row+=word+" ";
            }
            text(row,x,y,size,color);y+=size*1.5f;
        }
    }
    void drawJournal(){
        boolean challenges=app.journal==1;
        text(challenges?"CHALLENGES":"ACHIEVEMENTS",45,125,36,WHITE);
        text(challenges?"Survive until 6 AM. Earn points on every completion.":app.progress.count()+" / "+Progress.TITLES.length+" unlocked  /  saved on this device",45,166,19,MUTED);
        String[] rows=app.menuItems();int first=Math.min(Math.max(0,app.selected-3),Math.max(0,rows.length-6));
        for(int i=first;i<Math.min(first+6,rows.length);i++)menuItem(rows[i],45,188+(i-first)*49,880,46,100+i,app.selected==i);
        if(app.selected<rows.length-1){
            String description=(challenges?Progress.BRIEFS[app.selected]:Progress.DETAILS[app.selected])+"  +"+(challenges?Progress.CHALLENGE_POINTS[app.selected]:Progress.ACHIEVEMENT_POINTS[app.selected])+" pts";
            wrapped(description.replace('\n',' '),90,518,65,18,MUTED);
        }
        text(challenges?"A  Start assignment     B  Back":"D-pad  Browse     *  Unlocked     B  Back",45,612,19,WHITE);
        hit(760,580,200,60,301);
    }
    void drawDashboard(){
        // A separate landscape page keeps lower-screen information readable on
        // phones instead of squeezing the portrait terminal beside the office.
        float fit=Math.min(getWidth()/1000f,getHeight()/650f);
        c.save();c.translate((getWidth()-1000*fit)/2,(getHeight()-650*fit)/2);c.scale(fit,fit);
        rect(12,12,976,626,0xff202020);outline(12,12,976,626,0xff555555,8);
        rect(32,32,936,51,0xff393939);
        text("FAZBEAR SHIFT MANAGER",48,65,25,WHITE);text("11/1993",835,64,21,MUTED);
        text(app.state.running?"SHIFT PAUSED":"NIGHT OPERATIONS",48,124,24,WHITE);
        String[] rows=app.menuItems();
        for(int i=0;i<rows.length;i++)menuItem(rows[i],44,158+i*70,410,58,100+i,app.selected==i);
        line(472,106,472,565,0xff555555,2);
        text("SHIFT "+app.state.night+(app.state.running?"  /  "+app.state.hour+" AM":""),500,125,23,WHITE);
        if(!app.state.running){
            for(int i=1;i<=7;i++){
                float x=500+(i-1)*65;String label=i==7?"CN":""+i;
                if(i<=app.unlocked)button(label,x,146,56,47,600+i,i==app.state.night);
                else{outline(x,146,56,47,0xff444444,1);text(label,x+10,177,18,0xff666666);}
            }
        }else text("Power remaining: "+(int)app.state.power+"%",500,174,22,MUTED);
        text("Balance: "+points(app.progress.points)+" pts",500,239,28,GREEN);
        text("Achievements: "+app.progress.count()+" / "+Progress.TITLES.length,500,277,22,MUTED);
        int next=app.nextObjective();text("NEXT ACHIEVEMENT",500,334,18,MUTED);
        wrapped(next<0?"All achievements complete":Progress.TITLES[next]+" / +"+Progress.ACHIEVEMENT_POINTS[next]+" pts",500,370,30,21,WHITE);
        text("YOUR DESK",500,442,18,MUTED);
        for(int i=0;i<5;i++){
            float x=500+i*91;outline(x,465,80,93,app.progress.wearing(i)?GREEN:0xff555555,1);
            plush(i,new RectF(x+9,472,x+71,541),false);hit(x,465,80,93,630+i);
        }
        text(app.state.running?"D-pad: navigate    A: select    B: settings":"Up/down: menu   Left/right: night   A: select   B: settings",48,610,20,MUTED);
        c.restore();
    }
    void drawCompanion(){
        c.save();c.scale(getWidth()/1000f,getHeight()/870f);
        rect(14,10,972,846,0xff202020);outline(14,10,972,846,0xff555555,10);
        outline(29,25,942,816,0xff101010,5);
        rect(38,35,924,45,0xff393939);line(39,79,960,79,0xff777777,2);
        text("FAZBEAR SHIFT MANAGER",55,66,22,WHITE);
        text("11/1993",845,66,20,0xffd0d0c4);
        text("FAZBEAR ENTERTAINMENT",60,107,21,WHITE);
        text("NIGHT OPERATIONS  /  SECURITY TERMINAL",60,136,17,MUTED);
        rect(40,145,920,681,0xff161616);outline(40,145,920,681,0xff8a8a8a,2);
        if(!app.options&&!app.showHelp&&!app.diagnostic&&app.bindIndex<0&&!app.customizing&&app.journal==0&&!app.state.running){
            rect(42,146,916,27,0xff343434);line(42,173,957,173,0xff858585,1);
            text("SHIFTS",60,168,17,WHITE);
            text("RECORDS",260,168,17,0xffc2c2b8);hit(250,146,160,28,501);
            text("ASSIGNMENTS",480,168,17,0xffc2c2b8);hit(470,146,215,28,500);
            text("DESK",762,168,17,0xffc2c2b8);hit(752,146,145,28,530);
        }
        if(app.options||app.showHelp||app.diagnostic||app.bindIndex>=0||app.customizing){
            text(app.customizing?"CUSTOM ASSIGNMENT":"SECURITY DESK",60,220,37,WHITE);
            lines("D-pad / Left stick  Navigate\nA  Select   /   B  Back\n\nYour progress saves automatically.\nUse the upper screen to change settings.",60,308,23,MUTED);
            if(app.bindIndex>=0)text("Remapping: "+MainActivity.ACTIONS[app.bindIndex],60,565,24,GREEN);
            button("B  BACK",60,750,880,58,301,false);
        }else if(app.journal==3){
            drawShop(true);
        }else if(app.journal!=0){
            boolean challenge=app.journal==1;
            String[] names=challenge?Progress.CHALLENGES:Progress.TITLES;
            text(challenge?"SPECIAL ASSIGNMENT":"PERSONNEL ACHIEVEMENT",60,194,22,MUTED);
            if(app.selected<names.length){
                int i=app.selected;text(names[i],60,247,32,WHITE);
                text(challenge?(app.progress.cleared(i)?"COMPLETED":"NOT YET COMPLETED"):app.progress.status(i),60,292,21,GREEN);
                wrapped(challenge?Progress.BRIEFS[i]:Progress.DETAILS[i],60,360,55,24,WHITE);
                if(challenge){
                    String[] who={"Freddy","Bonnie","Chica","Foxy"};
                    for(int a=0;a<4;a++){float xx=60+a*223;text(who[a],xx,507,22,MUTED);text("AI "+Progress.LEVELS[i][a],xx,550,30,WHITE);}
                    text("Reward: +"+Progress.CHALLENGE_POINTS[i]+" pts / every completion",60,606,22,GREEN);
                    button("A  START ASSIGNMENT",60,649,880,66,512,true);
                }else{
                    text("Reward: +"+Progress.ACHIEVEMENT_POINTS[i]+" pts / one-time unlock",60,594,21,GREEN);
                    text("Campaign and challenge shifts count",60,634,20,MUTED);
                    text("unless the objective specifies campaign.",60,665,20,MUTED);
                }
            }else{text("Return to the title screen",60,270,30,WHITE);button("A  BACK TO TITLE",60,649,880,66,512,false);}
            button("< PREVIOUS",60,750,290,58,510,false);button("NEXT >",366,750,270,58,511,false);button("B  BACK",652,750,288,58,301,false);
        }else if(app.state.running){
            text("SHIFT PAUSED",60,220,43,WHITE);
            text(app.activeChallenge>=0?Progress.CHALLENGES[app.activeChallenge]:"Night "+app.state.night,60,278,29,MUTED);
            text(app.state.hour+" AM     Power: "+(int)app.state.power+"%",60,350,33,WHITE);
            lines("X  Monitor   /   D-pad  Cameras\nL1 / R1  Doors   /   L2 / R2  Lights\nLeft stick  Look   /   Y  Mute call",60,436,23,MUTED);
            button("RESUME SHIFT",60,657,880,65,513,true);
            button("OPTIONS",60,750,880,58,502,false);
        }else{
            text("CHOOSE YOUR SHIFT",60,195,18,MUTED);
            for(int i=1;i<=7;i++){
                float x=60+(i-1)*127;boolean unlocked=i<=app.unlocked;
                rect(x,202,114,60,app.state.night==i?0xff55554b:0xff242424);
                outline(x,202,114,60,unlocked?WHITE:0xff444444,1);
                text(i==7?"CUSTOM":"N"+i,x+12,240,i==7?19:25,unlocked?WHITE:0xff666666);
                if(unlocked)hit(x,202,114,60,600+i);
            }
            rect(60,278,880,58,0xff4c4c47);outline(60,278,880,58,0xffd0d0c8,2);
            text(app.state.night==7?"CONFIGURE CUSTOM NIGHT":"START NIGHT "+app.state.night,72,316,22,WHITE);hit(60,278,880,58,620);
            int objective=app.nextObjective(),challenge=app.nextChallenge();
            text("NEXT ACHIEVEMENT",60,378,19,MUTED);
            if(objective>=0){
                text(Progress.TITLES[objective]+"  +"+Progress.ACHIEVEMENT_POINTS[objective]+" pts",60,413,24,WHITE);
                text(Progress.DETAILS[objective],60,445,18,MUTED);
            }else text("All achievements complete",60,413,24,GREEN);
            hit(60,350,880,104,621);text(">",912,414,26,WHITE);
            line(60,461,940,461,0xff555555,1);
            text("SPECIAL ASSIGNMENT",60,493,19,MUTED);
            text(Progress.CHALLENGES[challenge]+"  +"+Progress.CHALLENGE_POINTS[challenge]+" pts",60,531,24,WHITE);
            hit(60,473,880,76,622);text(">",912,531,26,WHITE);
            text("YOUR DESK  /  TAP TO PLACE OR STORE",60,584,19,MUTED);
            for(int i=0;i<5;i++){
                float x=60+i*178;
                outline(x,602,162,124,app.progress.wearing(i)?0xffb9b6a1:0xff555555,1);
                plush(i,new RectF(x+30,608,x+132,685),false);
                String label=app.progress.owns(i)?(app.progress.wearing(i)?"ON DESK":"STORED"):Progress.PRICES[i]+" pts";
                text(label,x+13,713,17,app.progress.wearing(i)?0xffc9c5a9:MUTED);hit(x,602,162,124,630+i);
            }
            button("SHOP / "+points(app.progress.points)+" pts",60,752,590,57,530,false);
            button("OPTIONS",668,752,272,57,502,false);
        }
        // Light screen grain and scanlines remain inside the thicker physical bezel.
        prepareTitleFilter();c.save();c.clipRect(40,145,960,826);
        cctvPaint.setAlpha(42);c.drawBitmap(titleNoise[(int)((android.os.SystemClock.uptimeMillis()/110)%titleNoise.length)],null,new RectF(40,145,960,826),cctvPaint);cctvPaint.setAlpha(255);
        for(int y=145;y<826;y+=4)line(40,y,960,y,0x1a000000,1);
        float sweep=(android.os.SystemClock.uptimeMillis()%7000)/7000f*720+115;
        rect(40,sweep,920,20,0x0cffffff);c.restore();
        if(app.buyConfirm>=0)drawPurchase(870);
        c.restore();
    }
    void plush(int i,RectF area,boolean office){
        Bitmap b=office?app.assets.officePlushies[i]:app.assets.plushies[i];if(b==null)return;
        float size=Math.min(area.width()/b.getWidth(),area.height()/b.getHeight());
        float w=b.getWidth()*size,h=b.getHeight()*size;
        p.setColor(WHITE);
        p.setColorFilter(null);
        c.drawBitmap(b,null,new RectF(area.centerX()-w/2,area.bottom-h,area.centerX()+w/2,area.bottom),p);
        p.setColorFilter(null);
    }
    void drawDeskPlushies(){
        // Individual surfaces: monitor stack, front desk, right desk, low monitor, rear monitor.
        float[][] spots={{650,349,72,86},{540,478,76,96},{994,477,77,87},{578,399,57,66},{970,350,62,75}};
        for(int i:new int[]{0,3,4,1,2})if(app.progress.wearing(i)){
            float[] v=spots[i];float x=v[0],bottom=v[1];
            Bitmap shadow=app.assets.plushShadows[i];
            if(shadow!=null){
                c.save();c.translate(x,bottom);c.scale(1,.18f);c.skew(x<800?.65f:-.65f,0);
                p.setColor(WHITE);p.setAlpha(150);
                c.drawBitmap(shadow,null,new RectF(-v[2]/2,-v[3],v[2]/2,0),p);p.setAlpha(255);c.restore();
            }
            c.save();c.translate(x,bottom);c.scale(v[2]*.46f,5.5f);
            eyePaint.setShader(contactShadow);eyePaint.setAlpha(255);
            c.drawCircle(0,0,1,eyePaint);c.restore();
            plush(i,new RectF(x-v[2]/2,bottom-v[3],x+v[2]/2,bottom),true);
        }
    }
    String shopActionLabel(int i){
        return app.progress.wearing(i)?"STORE PLUSH":app.progress.owns(i)?"PLACE ON DESK":app.progress.points>=Progress.PRICES[i]?"BUY - "+Progress.PRICES[i]+" pts":"NEED "+(Progress.PRICES[i]-app.progress.points)+" MORE POINTS";
    }
    void drawShop(boolean lower){
        int i=app.selected;
        if(lower){
            text("OFFICE COLLECTION",60,194,25,WHITE);
            text("Balance: "+points(app.progress.points)+" pts",60,236,26,GREEN);
            if(i<5){
                text(Progress.PLUSHIES[i]+" Plush",60,290,31,WHITE);
                plush(i,new RectF(310,310,690,578),false);
                text(app.progress.owns(i)?(app.progress.wearing(i)?"OWNED / ON YOUR DESK":"OWNED / IN STORAGE"):"PRICE: "+Progress.PRICES[i]+" pts",60,617,22,WHITE);
                button("A  "+shopActionLabel(i),60,646,880,62,512,app.progress.owns(i)||app.progress.points>=Progress.PRICES[i]);
            }else{text("Return to your shift record",60,330,30,WHITE);button("A  BACK",60,646,880,62,512,false);}
            if(!app.shopMessage.isEmpty()&&android.os.SystemClock.uptimeMillis()<app.shopMessageUntil)text(app.shopMessage,60,736,20,GREEN);
            button("< PREVIOUS",60,766,290,50,510,false);button("NEXT >",366,766,270,50,511,false);button("B  BACK",652,766,288,50,301,false);
        }else{
            text("OFFICE SHOP",45,125,36,WHITE);text("Balance: "+points(app.progress.points)+" pts",45,166,22,GREEN);
            String[] rows=app.menuItems();for(int j=0;j<rows.length;j++)menuItem(rows[j],45,192+j*48,630,44,100+j,i==j);
            if(i<5){
                plush(i,new RectF(715,192,965,461),false);
                text(shopActionLabel(i),90,523,20,GREEN);
                text("Cosmetic only. Place or store owned plushies for free.",90,560,18,MUTED);
            }
            text(!app.shopMessage.isEmpty()&&android.os.SystemClock.uptimeMillis()<app.shopMessageUntil?app.shopMessage:"A  Buy / Place / Store     B  Back",45,612,19,WHITE);
            hit(760,580,200,60,301);
        }
    }
    void drawPurchase(float height){
        int i=app.buyConfirm;hits.clear();rect(0,0,1000,height,0xef000000);
        text("CONFIRM PURCHASE",80,height*.3f,33,WHITE);
        text(Progress.PLUSHIES[i]+" Plush",80,height*.3f+60,30,WHITE);
        text("Price: "+Progress.PRICES[i]+" pts",80,height*.3f+111,25,GREEN);
        text("Balance after: "+points(app.progress.points-Progress.PRICES[i])+" pts",80,height*.3f+152,23,MUTED);
        button("A  BUY & PLACE",80,height*.3f+199,490,65,531,true);
        button("B  CANCEL",590,height*.3f+199,330,65,532,false);
    }
    void drawIntro(){
        if(app.intro.stage==4){
            if(primary)drawMenu();else drawCompanion();hits.clear();
            rect(0,0,getWidth(),getHeight(),((int)(255*(1-app.intro.opacity())))<<24);return;
        }
        if(app.intro.stage==3||(!primary&&app.intro.stage==2))return;
        if(app.intro.stage==1&&primary){
            bitmapFit(574,new RectF(0,0,getWidth(),getHeight()),false);
            hit(0,0,getWidth(),getHeight(),520);
        }else{
            Bitmap card=app.assets.bitmap(NightIntro.CARDS[Math.max(0,Math.min(6,app.state.night-1))]);
            if(card!=null){
                int zoom=Math.max(1,(int)Math.min(getWidth()/640f,getHeight()/360f));
                float w=card.getWidth()*zoom,h=card.getHeight()*zoom;
                p.setColor(WHITE);p.setFilterBitmap(false);
                c.drawBitmap(card,null,new RectF((getWidth()-w)/2,(getHeight()-h)/2,(getWidth()+w)/2,(getHeight()+h)/2),p);
                p.setFilterBitmap(true);
            }
            if(app.intro.stage==1)hit(0,0,getWidth(),getHeight(),520);
        }
        rect(0,0,getWidth(),getHeight(),((int)(255*(1-app.intro.opacity())))<<24);
    }
    void drawNotice(){
        if(android.os.SystemClock.uptimeMillis()>app.noticeUntil)return;
        c.save();c.scale(getWidth()/1000f,getHeight()/650f);
        rect(180,12,640,66,0xf0101410);outline(180,12,640,66,GREEN,1);
        text(app.noticeTitle,202,37,16,GREEN);
        text(app.achievementNotice,202,64,23,WHITE);c.restore();
    }
    void drawMenuReveal(){
        if(app.menuReveal<=0)return;
        float black=1-app.menuRevealProgress();
        if(black>0)rect(0,0,getWidth(),getHeight(),Math.round(255*black)<<24);
    }
    void action(int id,boolean down){
        if(app.assets.importing)return;
        if(app.buyConfirm>=0){if(down){if(id==531)app.confirmPurchase();else if(id==532)app.back();}return;}
        if(id==2||id==3){app.state.light(id==2,down);return;}if(!down)return;
        if(id>=100&&id<120)app.activate(id-100);
        else if(id>=30&&id<41)app.state.selectCamera(id-30);
        else if(id==20){app.state.pause();app.releaseInputs();}
        else if(id==21)app.state.mutedCall=true;
        else if(id==22)app.boop();
        else if(id>=601&&id<=607){if(id-600<=app.unlocked){app.state.night=id-600;app.audio.menuBlip();}}
        else if(id==620)app.activate(0);
        else if(id==621){int i=app.nextObjective();app.openJournal(2);app.selected=Math.max(0,i);app.audio.menuBlip();}
        else if(id==622){int i=app.nextChallenge();app.openJournal(1);app.selected=i;app.audio.menuBlip();}
        else if(id>=630&&id<=634){
            int i=id-630;app.audio.menuBlip();
            if(app.progress.owns(i)){app.progress.togglePlush(i);app.saveProgress();}
            else{app.openJournal(3);app.selected=i;}
        }
        else if(id==500){app.audio.menuBlip();app.openJournal(1);}
        else if(id==501){app.audio.menuBlip();app.openJournal(2);}
        else if(id==502){app.audio.menuBlip();app.openOptions();}
        else if(id==510)app.navigate(-1);
        else if(id==511)app.navigate(1);
        else if(id==512)app.activate(app.selected);
        else if(id==530){app.audio.menuBlip();app.openJournal(3);}
        else if(id==520)app.skipNewspaper();
        else if(id==640)app.chooseLocalFile(10);
        else if(id==513){app.state.paused=false;app.releaseInputs();}
        else if(id==300)app.finishResult();else if(id==301)app.back();
        else if(id>=400&&id<408){int i=(id-400)/2;app.state.custom[i]=Math.max(0,Math.min(20,app.state.custom[i]+(id%2==0?-1:1)));}
        else if(id>=450&&id<454){app.audio.menuBlip();app.customSelected=id-450;}
        else if(id==460){app.customizing=false;app.startNight();}
        else app.action(id);
    }
    @Override public boolean onTouchEvent(MotionEvent event){
        if(app.menuReveal>0)return true;
        int a=event.getActionMasked(),idx=event.getActionIndex(),pointer=event.getPointerId(idx);
        if(a==MotionEvent.ACTION_DOWN||a==MotionEvent.ACTION_POINTER_DOWN){
            float x=event.getX(idx),y=event.getY(idx);int hit=-1;
            for(int i=hits.size()-1;i>=0;i--)if(hits.get(i).box.contains(x,y)){hit=hits.get(i).id;break;}
            if(hit>=0){held.put(pointer,hit);action(hit,true);}else if(!app.menu()&&!app.state.monitor){pan.put(pointer,x);}return true;
        }
        if(a==MotionEvent.ACTION_MOVE){for(int i=0;i<event.getPointerCount();i++){int id=event.getPointerId(i);if(pan.containsKey(id)){float x=event.getX(i);app.state.look=Math.max(-1,Math.min(1,app.state.look+(pan.get(id)-x)/getWidth()*3));pan.put(id,x);}}return true;}
        if(a==MotionEvent.ACTION_UP||a==MotionEvent.ACTION_POINTER_UP){Integer hit=held.remove(pointer);if(hit!=null)action(hit,false);pan.remove(pointer);performClick();return true;}
        if(a==MotionEvent.ACTION_CANCEL){for(int hit:held.values())action(hit,false);held.clear();pan.clear();return true;}
        return true;
    }
    @Override public boolean performClick(){super.performClick();return true;}
}


