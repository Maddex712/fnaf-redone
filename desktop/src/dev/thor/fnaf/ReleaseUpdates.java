package dev.thor.fnaf;

import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import org.json.*;

/** Manual, public GitHub release lookup. No Steam data or credentials are transmitted. */
final class ReleaseUpdates {
    static final String PAGE="https://github.com/Maddex712/fnaf-redone/releases";
    static final class Result {
        final String message,url;final boolean newer;
        String tag="",assetUrl="",digest="";long size;
        boolean installable(){return newer&&size>0&&size<=DesktopUpdater.MAX_DOWNLOAD&&digest.matches("sha256:[0-9a-fA-F]{64}")&&assetUrl.startsWith("https://github.com/Maddex712/fnaf-redone/releases/download/"+tag+"/");}
        Result(String message,String url,boolean newer){this.message=message;this.url=url;this.newer=newer;}
    }
    static Result check(String current,String platform){
        HttpURLConnection c=null;
        try{
            c=(HttpURLConnection)new URL("https://api.github.com/repos/Maddex712/fnaf-redone/releases?per_page=100").openConnection();
            c.setConnectTimeout(6000);c.setReadTimeout(6000);
            c.setRequestProperty("Accept","application/vnd.github+json");
            c.setRequestProperty("User-Agent","FNaF-Redone/"+current);
            int status=c.getResponseCode();
            if(status==403||status==429)return new Result("GitHub's request limit was reached. Try again later or open Releases.",PAGE,false);
            if(status!=200)throw new IOException("GitHub returned HTTP "+status);
            try(InputStream in=c.getInputStream();ByteArrayOutputStream bytes=new ByteArrayOutputStream()){
                byte[] buf=new byte[8192];int n;
                while((n=in.read(buf))!=-1){if(bytes.size()+n>4*1024*1024)throw new IOException("Release response too large");bytes.write(buf,0,n);}
                return select(new String(bytes.toByteArray(),StandardCharsets.UTF_8),current,platform);
            }
        }catch(Exception e){return new Result("Couldn't check for updates. Check your connection or open Releases.",PAGE,false);}
        finally{if(c!=null)c.disconnect();}
    }
    static Result select(String json,String current,String platform)throws JSONException{
        JSONArray releases=new JSONArray(json);String best=null,url=PAGE;
        JSONObject bestAsset=null;
        boolean windows=platform.equals("Windows");
        for(int i=0;i<releases.length();i++){
            JSONObject r=releases.getJSONObject(i);if(r.optBoolean("draft",false))continue;
            String tag=r.optString("tag_name","");if(version(tag)==null)continue;
            JSONArray assets=r.optJSONArray("assets");JSONObject match=null;
            if(assets!=null)for(int j=0;j<assets.length();j++){
                String name=assets.getJSONObject(j).optString("name","").toLowerCase(java.util.Locale.ROOT);
                if(name.startsWith("fnaf-redone-")&&(windows?name.contains("windows-x64")&&name.endsWith(".zip"):name.endsWith(".apk")))match=assets.getJSONObject(j);
            }
            if(match!=null&&(best==null||compare(tag,best)>0)){
                bestAsset=match;
                best=tag;String page=r.optString("html_url",PAGE);
                url=page.startsWith(PAGE+"/tag/")?page:PAGE;
            }
        }
        if(best==null)return new Result("No "+platform+" download was found on GitHub yet.",PAGE,false);
        boolean newer=compare(best,current)>0;
        Result result=new Result(newer?"Update available: "+best+"\nInstalled: "+current+" ("+platform+")":"You're up to date.\nInstalled: "+current+" ("+platform+")",url,newer);
        result.tag=best;result.assetUrl=bestAsset.optString("browser_download_url","");result.digest=bestAsset.optString("digest","");result.size=bestAsset.optLong("size",0);return result;
    }
    static int[] version(String text){
        java.util.regex.Matcher m=java.util.regex.Pattern.compile("^v?(\\d+)\\.(\\d+)\\.(\\d+)(?:[-+].*)?$").matcher(text);
        if(!m.matches())return null;
        try{return new int[]{Integer.parseInt(m.group(1)),Integer.parseInt(m.group(2)),Integer.parseInt(m.group(3)),text.contains("preview")?0:1};}catch(NumberFormatException e){return null;}
    }
    static int compare(String a,String b){int[] x=version(a),y=version(b);if(x==null||y==null)return 0;for(int i=0;i<4;i++)if(x[i]!=y[i])return Integer.compare(x[i],y[i]);return 0;}
}
