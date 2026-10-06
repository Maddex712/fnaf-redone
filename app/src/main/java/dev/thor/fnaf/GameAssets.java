package dev.thor.fnaf;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.util.LruCache;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

final class GameAssets {
    final Context context;
    final File directory;
    final ExecutorService worker=Executors.newSingleThreadExecutor();
    final HashSet<Integer> pending=new HashSet<>();
    final LruCache<Integer,Bitmap> images=new LruCache<Integer,Bitmap>(96*1024*1024){protected int sizeOf(Integer k,Bitmap v){return v.getAllocationByteCount();}};
    JSONObject catalog;
    Bitmap title;
    Bitmap deathScene;
    final Bitmap[] titleVariants=new Bitmap[3];
    Bitmap titleHandprint;
    final Bitmap[] plushies=new Bitmap[5],officePlushies=new Bitmap[5],plushShadows=new Bitmap[5];
    final HashSet<Integer> doorIds=new HashSet<>();
    final java.util.concurrent.ConcurrentHashMap<Integer,Bitmap> doorFrames=new java.util.concurrent.ConcurrentHashMap<>();
    final int[][] meta=new int[4096][4];
    volatile boolean ready,importing,closed;
    volatile String status="Import your Steam game data to begin.";
    GameAssets(Context context){
        this.context=context;directory=new File(context.getFilesDir(),"game");
        // Recover the previous pack if Android stopped the process between the
        // two directory renames of an import. Never discard a working install.
        File previous=new File(context.getFilesDir(),"previous-game");
        if(!directory.exists()&&previous.isDirectory())previous.renameTo(directory);
        try(InputStream in=context.getAssets().open("catalog.json");InputStream im=context.getAssets().open("image-meta.json")){
            catalog=new JSONObject(read(in));JSONArray m=new JSONArray(read(im));
            for(int i=0;i<m.length();i++){JSONObject v=m.getJSONObject(i);int id=v.getInt("Handle");if(id<meta.length)meta[id]=new int[]{v.getInt("Width"),v.getInt("Height"),v.getInt("HotspotX"),v.getInt("HotspotY")};}
            for(int object:new int[]{59,60}){JSONArray frames=catalog.getJSONObject(""+object).getJSONObject("animations").getJSONObject("12").getJSONArray("frames");for(int i=0;i<frames.length();i++)doorIds.add(frames.getInt(i));}
        }catch(Exception e){status="Asset catalog error: "+e.getMessage();}
        try(InputStream in=context.getAssets().open("title-lineup.png")){title=BitmapFactory.decodeStream(in);}catch(IOException ignored){}
        titleVariants[0]=title;
        try(InputStream in=context.getAssets().open("death-pond-clean.png")){deathScene=BitmapFactory.decodeStream(in);}catch(IOException ignored){}
        for(int i=1;i<3;i++)try(InputStream in=context.getAssets().open("title-golden-"+i+".png")){titleVariants[i]=BitmapFactory.decodeStream(in);}catch(IOException ignored){}
        try(InputStream in=context.getAssets().open("title-handprint.png")){titleHandprint=BitmapFactory.decodeStream(in);}catch(IOException ignored){}
        String[] plushNames={"freddy","bonnie","chica","foxy","golden"};
        for(int i=0;i<5;i++)try(InputStream in=context.getAssets().open("plush-"+plushNames[i]+".png")){plushies[i]=BitmapFactory.decodeStream(in);}catch(IOException ignored){}
        for(int i=0;i<5;i++){
            try(InputStream in=context.getAssets().open("plush-office-"+plushNames[i]+".png")){officePlushies[i]=BitmapFactory.decodeStream(in);}catch(IOException ignored){officePlushies[i]=plushies[i];}
            try(InputStream in=context.getAssets().open("plush-shadow-"+plushNames[i]+".png")){plushShadows[i]=BitmapFactory.decodeStream(in);}catch(IOException ignored){}
        }
        ready=new File(directory,"pack.json").isFile()&&new File(directory,"Images/00039.png").isFile();
        if(ready)status="Steam game data ready";
    }
    static String read(InputStream in)throws IOException{ByteArrayOutputStream b=new ByteArrayOutputStream();byte[] buf=new byte[8192];int n;while((n=in.read(buf))!=-1)b.write(buf,0,n);return b.toString(StandardCharsets.UTF_8.name());}
    int image(int object,int anim,double time,boolean once){
        try{JSONObject a=catalog.getJSONObject(""+object).getJSONObject("animations").getJSONObject(""+anim);JSONArray fs=a.getJSONArray("frames");
            double fps=Math.max(1,a.getInt("speed")*60.0/100);int frame=(int)(time*fps);frame=once?Math.min(fs.length()-1,frame):frame%fs.length();return fs.getInt(Math.max(0,frame));
        }catch(Exception e){return -1;}
    }
    int scaledFrame(int object,int anim,double fraction){try{JSONArray fs=catalog.getJSONObject(""+object).getJSONObject("animations").getJSONObject(""+anim).getJSONArray("frames");return fs.getInt(Math.max(0,Math.min(fs.length()-1,(int)(fraction*fs.length()))));}catch(Exception e){return -1;}}
    Bitmap bitmap(int id){
        if(!ready||id<0||closed)return null;
        Bitmap b=doorFrames.get(id);if(b==null)b=images.get(id);if(b!=null)return b;
        synchronized(pending){if(!pending.add(id))return null;}
        worker.execute(()->{try{BitmapFactory.Options o=new BitmapFactory.Options();o.inScaled=false;Bitmap decoded=BitmapFactory.decodeFile(new File(directory,String.format(Locale.US,"Images/%05d.png",id)).getPath(),o);if(decoded!=null&&!closed){if(doorIds.contains(id))doorFrames.put(id,decoded);else images.put(id,decoded);}}finally{synchronized(pending){pending.remove(id);}}});
        return null;
    }
    Bitmap scareFrame(int id){
        if(!ready||id<0||closed)return null;
        Bitmap cached=doorFrames.get(id);if(cached==null)cached=images.get(id);
        if(cached!=null)return cached;
        // Jumpscare frames can last less than 17 ms. Loading them through the
        // normal background queue leaves most frames blank or skipped.
        BitmapFactory.Options options=new BitmapFactory.Options();options.inScaled=false;
        Bitmap frame=BitmapFactory.decodeFile(new File(directory,String.format(Locale.US,"Images/%05d.png",id)).getPath(),options);
        if(frame!=null)images.put(id,frame);
        return frame;
    }
    void warm(){for(int id:new int[]{210,522,523,350,351,352,432,548})bitmap(id);bitmap(574);for(int id:NightIntro.CARDS)bitmap(id);for(int id:doorIds)bitmap(id);for(int a:new int[]{0,17,18,19,34,43,46,47})bitmap(image(44,a,0,false));for(int id:new int[]{58,59,60,61,65})bitmap(image(id,0,0,false));}
    void importPack(Uri uri,Runnable done){
        if(importing)return;importing=true;ready=false;images.evictAll();doorFrames.clear();status="Importing game data…";
        worker.execute(()->{
            File staging=new File(context.getFilesDir(),"importing");
            File executable=new File(context.getCacheDir(),"steam-import.exe");
            try{
                clear(staging);if(!staging.mkdirs())throw new IOException("Cannot create import directory");
                boolean isExecutable;
                try(InputStream probe=context.getContentResolver().openInputStream(uri)){
                    if(probe==null)throw new IOException("Cannot open selected file");
                    isExecutable=probe.read()=='M'&&probe.read()=='Z';
                }
                if(isExecutable){
                    status="Copying Steam file…";
                    try(InputStream input=context.getContentResolver().openInputStream(uri);OutputStream output=new FileOutputStream(executable)){
                        if(input==null)throw new IOException("Cannot open selected file");
                        byte[] buffer=new byte[65536];int n;long copied=0;
                        while((n=input.read(buffer))!=-1){copied+=n;if(copied>512L*1024*1024)throw new IOException("Steam file is too large");output.write(buffer,0,n);}
                    }
                    SteamImporter.extract(executable,staging,message->status=message);
                }else{
                long total=0;int count=0;boolean manifest=false;HashSet<String> names=new HashSet<>();
                try(InputStream input=context.getContentResolver().openInputStream(uri);ZipInputStream zip=new ZipInputStream(new BufferedInputStream(input))){
                    ZipEntry entry;byte[] buffer=new byte[65536];
                    while((entry=zip.getNextEntry())!=null){
                        String name=entry.getName();if(entry.isDirectory())continue;
                        if(!(name.matches("Images/[0-9]{5}\\.png")||name.matches("Sounds/[^/\\\\]{1,100}\\.wav")||name.equals("pack.json")))throw new IOException("Unexpected pack entry: "+name);
                        if(!names.add(name)||++count>800)throw new IOException("Invalid pack entries");
                        File out=new File(staging,name);out.getParentFile().mkdirs();long size=0;
                        try(OutputStream output=new FileOutputStream(out)){int n;while((n=zip.read(buffer))!=-1){size+=n;total+=n;if(size>64L*1024*1024||total>1024L*1024*1024)throw new IOException("Pack exceeds size limit");output.write(buffer,0,n);}}
                        if(name.equals("pack.json")){try(InputStream f=new FileInputStream(out)){JSONObject j=new JSONObject(read(f));manifest="fnaf1-steam-284".equals(j.optString("format"));}}
                        status="Importing game data • "+count+" files";
                    }
                }
                if(!manifest||!new File(staging,"Images/00039.png").isFile()||!new File(staging,"Sounds/XSCREAM.wav").isFile()||count<600)throw new IOException("Not a complete FNaF 1 data pack");
                }
                File old=new File(context.getFilesDir(),"previous-game");clear(old);
                if(directory.exists()&&!directory.renameTo(old))throw new IOException("Cannot replace current pack");
                if(!staging.renameTo(directory)){old.renameTo(directory);throw new IOException("Cannot activate pack");}
                clear(old);ready=true;status="Steam game data ready";
            }catch(Exception e){ready=new File(directory,"pack.json").isFile();status="Import failed: "+e.getMessage();}
            finally{
                try{clear(staging);clear(executable);}catch(IOException ignored){}
                importing=false;done.run();
            }
        });
    }
    static void clear(File f)throws IOException{if(!f.exists())return;if(f.isDirectory()){File[] fs=f.listFiles();if(fs==null)throw new IOException("Cannot read import directory");for(File child:fs)clear(child);}if(!f.delete())throw new IOException("Cannot clean import directory");}
    void close(){closed=true;worker.shutdown();images.evictAll();}
}
