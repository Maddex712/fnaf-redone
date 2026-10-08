package dev.thor.fnaf;
import java.io.*;
import java.net.URL;
import java.security.MessageDigest;
import org.json.*;

public final class AndroidUpdatesTest {
    static int checks;
    static void check(boolean b,String message){if(!b)throw new AssertionError(message);checks++;}
    interface Task {void run()throws Exception;}
    static void rejects(Task task,String message)throws Exception{boolean thrown=false;try{task.run();}catch(IOException e){thrown=true;}check(thrown,message);}
    static JSONObject release(String tag,String name,String digest)throws Exception{
        return new JSONObject().put("tag_name",tag).put("html_url",AndroidReleases.PAGE+"/tag/"+tag)
            .put("assets",new JSONArray().put(new JSONObject().put("name",name).put("size",100)
                .put("digest",digest).put("browser_download_url","https://github.com/Maddex712/fnaf-redone/releases/download/"+tag+"/"+name)));
    }
    public static void main(String[] args)throws Exception{
        String digest="sha256:"+new String(new char[64]).replace('\0','a');
        JSONObject android=release("v0.1.4-preview","FNaF-Redone-0.1.4-preview.apk",digest);
        JSONObject windows=release("v0.2.5-windows-preview","FNaF-Redone-0.2.5-Windows-x64-preview.zip",digest);
        String list=new JSONArray().put(windows).put(android).toString();
        AndroidReleases.Result result=AndroidReleases.select(list,"0.1.3-preview","Android");
        check(result.newer&&result.installable()&&result.tag.equals("v0.1.4-preview"),"Select Android despite newer Windows version");
        check(!AndroidReleases.select(list,"0.1.4-preview","Android").newer,"Current version up to date");
        check(!AndroidReleases.select(list,"0.1.5-preview","Android").installable(),"No downgrade");
        check(!AndroidReleases.select(new JSONArray().put(windows).toString(),"0.1.3-preview","Android").newer,"No Windows fallback");
        check(!AndroidReleases.select(new JSONArray().put(new JSONObject(android.toString()).put("draft",true)).toString(),"0.1.3-preview","Android").newer,"Ignore drafts");
        check(!AndroidReleases.select(new JSONArray().put(release("v0.1.4-preview","FNaF-Redone-0.1.4-preview.apk","" )).toString(),"0.1.3-preview","Android").installable(),"No install without checksum");
        result.assetUrl="https://example.org/update.apk";check(!result.installable(),"No external asset URL");
        check(AndroidReleases.compare("v0.1.10-preview","0.1.9-preview")>0,"Numeric version ordering");
        check(AndroidReleases.compare("v0.1.4","0.1.4-preview")>0,"Stable release supersedes preview");
        check(AndroidUpdateTransfer.trusted(new URL("https://release-assets.githubusercontent.com/file")),"GitHub download host allowed");
        for(String url:new String[]{"http://github.com/file","https://github.com.evil.example/file","https://someone@github.com/file","https://github.com:8080/file","https://example.com/file"})
            check(!AndroidUpdateTransfer.trusted(new URL(url)),"Reject untrusted redirect "+url);
        byte[] content="verified-apk-fixture".getBytes("UTF-8");String sha="sha256:"+AndroidUpdateTransfer.hex(MessageDigest.getInstance("SHA-256").digest(content));
        ByteArrayOutputStream out=new ByteArrayOutputStream();final long[] progress={0};
        new AndroidUpdateTransfer().copy(new ByteArrayInputStream(content),out,content.length,sha,(a,b)->progress[0]=a);
        check(java.util.Arrays.equals(content,out.toByteArray())&&progress[0]==content.length,"Verified bytes and progress");
        rejects(()->new AndroidUpdateTransfer().copy(new ByteArrayInputStream(content),new ByteArrayOutputStream(),content.length,digest,(a,b)->{}),"Checksum mismatch");
        rejects(()->new AndroidUpdateTransfer().copy(new ByteArrayInputStream(content),new ByteArrayOutputStream(),content.length+1,sha,(a,b)->{}),"Truncated response");
        rejects(()->new AndroidUpdateTransfer().copy(new ByteArrayInputStream(content),new ByteArrayOutputStream(),content.length-1,sha,(a,b)->{}),"Oversized response");
        AndroidUpdateTransfer cancelled=new AndroidUpdateTransfer();cancelled.cancel();
        rejects(()->cancelled.copy(new ByteArrayInputStream(content),new ByteArrayOutputStream(),content.length,sha,(a,b)->{}),"Cancellation");
        rejects(()->new AndroidUpdateTransfer().copy(new ByteArrayInputStream(content),new ByteArrayOutputStream(),AndroidUpdateTransfer.MAX_BYTES+1,sha,(a,b)->{}),"Download size cap");
        File cache=File.createTempFile("fnaf-update-",".apk");
        try{java.nio.file.Files.write(cache.toPath(),content);AndroidUpdateTransfer.verify(cache,content.length,sha);checks++;
            java.nio.file.Files.write(cache.toPath(),"changed".getBytes("UTF-8"));rejects(()->AndroidUpdateTransfer.verify(cache,content.length,sha),"Modified cache rejected");
        }finally{cache.delete();}
        System.out.println("Android updates: "+checks+" checks passed");
    }
}
