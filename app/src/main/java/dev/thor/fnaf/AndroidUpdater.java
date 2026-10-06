package dev.thor.fnaf;

import android.app.*;
import android.content.*;
import android.content.pm.*;
import android.net.Uri;
import android.os.Build;
import android.provider.Settings;
import java.io.*;
import java.util.*;
import org.json.JSONObject;

/** Manual update flow: GitHub -> checksum/signature verification -> Android confirmation. */
final class AndroidUpdater {
    static final int PERMISSION_REQUEST=52,INSTALL_REQUEST=53;
    final MainActivity app;
    volatile boolean destroyed,busy;
    AndroidUpdateTransfer transfer;
    AlertDialog dialog;
    boolean waitingPermission;
    AndroidReleases.Result pending;
    AndroidUpdater(MainActivity app){this.app=app;readPending();}
    String version(){try{return app.getPackageManager().getPackageInfo(app.getPackageName(),0).versionName;}catch(Exception e){return "unknown";}}
    void close(){destroyed=true;if(transfer!=null)transfer.cancel();if(dialog!=null)dialog.dismiss();}
    void ui(Runnable action){app.runOnUiThread(()->{if(!destroyed&&!app.isFinishing()&&!app.isDestroyed())action.run();});}
    void message(String title,String message){
        dialog=new AlertDialog.Builder(app).setTitle(title).setMessage(message).setPositiveButton("OK",null).show();
    }
    void releases(){try{app.startActivity(new Intent(Intent.ACTION_VIEW,Uri.parse(AndroidReleases.PAGE)));}catch(ActivityNotFoundException e){message("Releases","Visit github.com/MjonirMade/fnaf-redone/releases in a browser.");}}
    File metadata(){return new File(app.getCacheDir(),"updates/release.json");}
    void check(){
        if(busy)return;
        if(app.assets.importing){message("Import in progress","Wait for your Steam import to finish, then check again.");return;}
        app.state.pause();app.releaseInputs();app.audio.pause(true);busy=true;
        dialog=new AlertDialog.Builder(app).setTitle("Check for updates").setMessage("Checking GitHub...").setCancelable(false).show();
        new Thread(()->{
            AndroidReleases.Result result=AndroidReleases.check(version(),"Android");
            ui(()->{busy=false;dialog.dismiss();
                AlertDialog.Builder view=new AlertDialog.Builder(app).setTitle("FNaF Redone updates").setMessage(result.message).setNegativeButton("Close",null);
                if(result.installable())view.setPositiveButton("Download update",(d,w)->download(result));
                view.setNeutralButton("Release notes",(d,w)->releases());
                // A verified download survives cancellation of Android's installer or its permission screen.
                AndroidReleases.Result cached=readPending();
                if(cached!=null&&AndroidReleases.compare(cached.tag,version())>0&&(!result.newer||AndroidReleases.compare(cached.tag,result.tag)>=0))
                    view.setPositiveButton("Install downloaded update",(d,w)->prepareInstall(cached));
                dialog=view.show();
            });
        },"android-update-check").start();
    }
    AndroidReleases.Result readPending(){
        try{
            if(!UpdateApkProvider.apk(app).isFile()||metadata().length()>8192)return null;
            JSONObject data=new JSONObject(new String(java.nio.file.Files.readAllBytes(metadata().toPath()),java.nio.charset.StandardCharsets.UTF_8));
            AndroidReleases.Result r=new AndroidReleases.Result("",AndroidReleases.PAGE,true);
            r.tag=data.getString("tag");r.assetUrl=data.getString("url");r.digest=data.getString("digest");r.size=data.getLong("size");
            if(!r.installable())return null;
            if(AndroidReleases.compare(r.tag,version())<=0){metadata().delete();UpdateApkProvider.apk(app).delete();return null;}
            return r;
        }catch(Exception e){return null;}
    }
    void download(AndroidReleases.Result result){
        if(busy)return;busy=true;transfer=new AndroidUpdateTransfer();AndroidUpdateTransfer task=transfer;
        dialog=new AlertDialog.Builder(app).setTitle("Downloading "+result.tag).setMessage("Starting download...")
            .setNegativeButton("Cancel",(d,w)->task.cancel()).setOnCancelListener(d->task.cancel()).show();
        final long[] last={0};
        new Thread(()->{
            try{
                metadata().delete();
                task.download(result.assetUrl,UpdateApkProvider.apk(app),result.size,result.digest,(bytes,total)->{
                    long now=System.currentTimeMillis();if(now-last[0]>200||bytes==total){last[0]=now;ui(()->{if(!task.cancelled)dialog.setMessage("Downloading: "+(bytes*100/total)+"%\n"+String.format(Locale.US,"%.1f / %.1f MB",bytes/1048576.0,total/1048576.0));});}
                });
                validateApk(result);
                JSONObject data=new JSONObject().put("tag",result.tag).put("url",result.assetUrl).put("digest",result.digest).put("size",result.size);
                java.nio.file.Files.write(metadata().toPath(),data.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8));
                ui(()->{busy=false;dialog.dismiss();if(!task.cancelled)ready(result);});
            }catch(Exception e){ui(()->{busy=false;dialog.dismiss();if(!task.cancelled)message("Update not downloaded",e.getMessage()==null?"Check your connection and try again.":e.getMessage());});}
        },"android-update-download").start();
    }
    void ready(AndroidReleases.Result result){
        pending=result;
        dialog=new AlertDialog.Builder(app).setTitle("Update ready").setMessage(result.tag+" is verified and ready.\n\nAndroid will ask you to approve the update. Your saves, settings, and imported Steam files stay in place. Installing closes the game and ends any current night.")
            .setPositiveButton("Install update",(d,w)->prepareInstall(result)).setNegativeButton("Later",null).show();
    }
    void prepareInstall(AndroidReleases.Result result){
        if(busy)return;busy=true;pending=result;
        dialog=new AlertDialog.Builder(app).setTitle("Verify update").setMessage("Checking the downloaded APK...").setCancelable(false).show();
        new Thread(()->{
            try{
                AndroidUpdateTransfer.verify(UpdateApkProvider.apk(app),result.size,result.digest);validateApk(result);
                ui(()->{busy=false;dialog.dismiss();installVerified();});
            }catch(Exception e){ui(()->{busy=false;dialog.dismiss();metadata().delete();message("Update could not be installed",e.getMessage());});}
        },"android-update-verify").start();
    }
    void validateApk(AndroidReleases.Result result)throws Exception{
        PackageManager pm=app.getPackageManager();int flags=Build.VERSION.SDK_INT>=28?PackageManager.GET_SIGNING_CERTIFICATES:PackageManager.GET_SIGNATURES;
        PackageInfo installed=pm.getPackageInfo(app.getPackageName(),flags);
        PackageInfo archive=pm.getPackageArchiveInfo(UpdateApkProvider.apk(app).getAbsolutePath(),flags);
        if(archive==null||!app.getPackageName().equals(archive.packageName))throw new IOException("This APK is not an update for this app.");
        long oldCode=Build.VERSION.SDK_INT>=28?installed.getLongVersionCode():installed.versionCode;
        long newCode=Build.VERSION.SDK_INT>=28?archive.getLongVersionCode():archive.versionCode;
        if(newCode<=oldCode||!Objects.equals(archive.versionName,result.tag.replaceFirst("^v","")))throw new IOException("This APK is not a newer matching release.");
        Signature[] oldSign=signatures(installed),newSign=signatures(archive);
        if(oldSign==null||newSign==null||oldSign.length==0||newSign.length==0||!new HashSet<Signature>(Arrays.asList(oldSign)).equals(new HashSet<Signature>(Arrays.asList(newSign))))
            throw new IOException("The APK's signing certificate does not match your installed app. Your current installation has not changed.");
        if(archive.applicationInfo!=null&&archive.applicationInfo.minSdkVersion>Build.VERSION.SDK_INT)throw new IOException("This update requires a newer Android version.");
    }
    static Signature[] signatures(PackageInfo info){return Build.VERSION.SDK_INT>=28?(info.signingInfo==null?null:info.signingInfo.getApkContentsSigners()):info.signatures;}
    void installVerified(){
        if(!app.getPackageManager().canRequestPackageInstalls()){
            dialog=new AlertDialog.Builder(app).setTitle("Allow FNaF Redone updates").setMessage("On the next Android screen, enable Allow from this source, then go back. This permission lets FNaF Redone send its verified APK to Android's installer.")
                .setPositiveButton("Open settings",(d,w)->{
                    try{waitingPermission=true;app.startActivityForResult(new Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,Uri.parse("package:"+app.getPackageName())),PERMISSION_REQUEST);}
                    catch(ActivityNotFoundException|SecurityException e){waitingPermission=false;message("Permission unavailable","This device does not provide app-install permission settings. Use the GitHub APK download instead.");}
                }).setNegativeButton("Later",null).show();return;
        }
        app.saveProgress();app.state.pause();app.releaseInputs();app.audio.pause(true);
        Intent intent=new Intent(Intent.ACTION_VIEW).setDataAndType(UpdateApkProvider.uri(app),UpdateApkProvider.MIME)
            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        intent.setClipData(ClipData.newRawUri("FNaF Redone update",UpdateApkProvider.uri(app)));
        try{app.startActivityForResult(intent,INSTALL_REQUEST);}
        catch(ActivityNotFoundException|SecurityException e){message("Installer unavailable","Android couldn't open its package installer. The downloaded update is kept so you can try again.");}
    }
    boolean activityResult(int request){
        if(request==PERMISSION_REQUEST){
            waitingPermission=false;
            if(pending!=null&&app.getPackageManager().canRequestPackageInstalls())prepareInstall(pending);
            else message("Update not installed","Permission was not enabled. Your current game is unchanged. Check for updates again to retry the downloaded APK.");
            return true;
        }
        return request==INSTALL_REQUEST;
    }
}
