package dev.thor.fnaf;
import android.app.*;
import android.content.*;
import android.net.Uri;
import android.os.Bundle;
import java.io.*;
import java.security.MessageDigest;
import org.json.JSONObject;

/** Separate instrumentation APK only. Never included in the game. */
public final class UpdateProbe extends Instrumentation {
    Bundle arguments;int checks;
    @Override public void onCreate(Bundle args){arguments=args;start();}
    void check(boolean ok,String message){if(!ok)throw new AssertionError(message);checks++;}
    @Override public void onStart(){Bundle result=new Bundle();
        try{
            Context context=getTargetContext();String mode=arguments.getString("mode","prepare");
            if(mode.equals("network")){
                java.net.HttpURLConnection connection=(java.net.HttpURLConnection)new java.net.URL("https://api.github.com/repos/MjonirMade/fnaf-redone/releases?per_page=100").openConnection();
                connection.setConnectTimeout(10000);connection.setReadTimeout(10000);connection.setRequestProperty("User-Agent","FNaF-Redone-Test");
                int code=connection.getResponseCode();
                if(code!=200){result.putString("stream","GitHub HTTP "+code);finish(-1,result);return;}
                try(InputStream in=connection.getInputStream();ByteArrayOutputStream bytes=new ByteArrayOutputStream()){
                    byte[] buffer=new byte[8192];int n;while((n=in.read(buffer))!=-1)bytes.write(buffer,0,n);
                    AndroidReleases.Result live=AndroidReleases.select(new String(bytes.toByteArray(),"UTF-8"),"0.1.3-preview","Android");
                    result.putString("stream","HTTP "+code+"; "+live.message);finish(-1,result);return;
                }finally{connection.disconnect();}
            }
            MainActivity app=(MainActivity)startActivitySync(new Intent(context,MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
            if(mode.equals("options")){runOnMainSync(()->{app.openOptions();app.primary.invalidate();});result.putString("stream","Options open");finish(-1,result);return;}
            if(mode.equals("check")){runOnMainSync(()->app.checkForUpdates());result.putString("stream","Checking live release");finish(-1,result);return;}
            File marker=new File(context.getFilesDir(),"update-test-marker.txt");
            if(mode.equals("verify")){
                check(app.updater.version().equals("0.1.4-preview"),"Final release installed");
                check(new String(java.nio.file.Files.readAllBytes(marker.toPath()),"UTF-8").equals("preserved"),"Private data survived");
                check(context.getSharedPreferences("update-test",0).getInt("points",0)==98765,"Preferences survived");
                check(app.updater.readPending()==null,"Installed update cache cleared");
                check(!UpdateApkProvider.apk(context).exists(),"Installed APK cache removed");
                marker.delete();context.getSharedPreferences("update-test",0).edit().clear().commit();
                result.putString("stream",checks+" post-update checks passed");finish(-1,result);return;
            }
            java.nio.file.Files.write(marker.toPath(),"preserved".getBytes("UTF-8"));
            context.getSharedPreferences("update-test",0).edit().putInt("points",98765).commit();
            File source=new File(arguments.getString("apk"));File target=UpdateApkProvider.apk(context);target.getParentFile().mkdirs();
            java.nio.file.Files.copy(source.toPath(),target.toPath(),java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            MessageDigest hash=MessageDigest.getInstance("SHA-256");try(InputStream in=new FileInputStream(source)){byte[] b=new byte[65536];int n;while((n=in.read(b))!=-1)hash.update(b,0,n);}
            AndroidReleases.Result update=new AndroidReleases.Result("",AndroidReleases.PAGE,true);
            update.tag="v0.1.4-preview";update.size=source.length();update.digest="sha256:"+AndroidUpdateTransfer.hex(hash.digest());
            update.assetUrl="https://github.com/MjonirMade/fnaf-redone/releases/download/"+update.tag+"/FNaF-Redone-0.1.4-preview.apk";
            app.updater.validateApk(update);checks++;
            AndroidUpdateTransfer.verify(target,update.size,update.digest);checks++;
            update.tag="v9.9.9-preview";boolean refused=false;try{app.updater.validateApk(update);}catch(IOException e){refused=true;}
            check(refused,"Reject mismatched APK version");update.tag="v0.1.4-preview";
            Uri uri=UpdateApkProvider.uri(context);
            try(InputStream in=context.getContentResolver().openInputStream(uri)){check(in.read()=='P',"Read-only provider serves APK");}
            refused=false;try{context.getContentResolver().openOutputStream(uri).close();}catch(Exception e){refused=true;}
            check(refused,"Provider rejects writes");
            refused=false;try{context.getContentResolver().openInputStream(Uri.parse("content://"+context.getPackageName()+".updates/../files/update-test-marker.txt")).close();}catch(Exception e){refused=true;}
            check(refused,"Provider rejects unrelated private files");
            JSONObject data=new JSONObject().put("tag",update.tag).put("url",update.assetUrl).put("digest",update.digest).put("size",update.size);
            java.nio.file.Files.write(app.updater.metadata().toPath(),data.toString().getBytes("UTF-8"));
            check(app.updater.readPending()!=null,"Downloaded update can be retried");
            runOnMainSync(()->app.updater.prepareInstall(update));
            result.putString("stream",checks+" pre-update checks passed; Android installation flow opened");finish(-1,result);
        }catch(Throwable e){result.putString("stream",e.toString());finish(0,result);}
    }
}
