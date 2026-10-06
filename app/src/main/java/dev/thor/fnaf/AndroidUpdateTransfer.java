package dev.thor.fnaf;

import java.io.*;
import java.net.*;
import java.security.MessageDigest;
import java.util.Locale;

/** Bounded HTTPS download. Only a verified complete APK replaces the staged file. */
final class AndroidUpdateTransfer {
    static final long MAX_BYTES=256L*1024*1024;
    interface Progress { void update(long bytes,long total); }
    volatile boolean cancelled;
    volatile HttpURLConnection connection;
    void cancel(){cancelled=true;HttpURLConnection c=connection;if(c!=null)c.disconnect();}
    static boolean trusted(URL url){
        String h=url.getHost().toLowerCase(Locale.ROOT);
        return url.getProtocol().equals("https")&&url.getUserInfo()==null&&(url.getPort()==-1||url.getPort()==443)&&
            (h.equals("github.com")||h.equals("release-assets.githubusercontent.com")||h.equals("objects.githubusercontent.com"));
    }
    void download(String address,File target,long size,String digest,Progress progress)throws Exception{
        URL url=new URL(address);File part=new File(target.getParentFile(),"download.part");
        if(size<=0||size>MAX_BYTES||!digest.matches("sha256:[0-9a-fA-F]{64}"))throw new IOException("The release has no usable checksum or size.");
        if(!target.getParentFile().isDirectory()&&!target.getParentFile().mkdirs())throw new IOException("Couldn't create update storage.");
        if(target.getParentFile().getUsableSpace()<size+16L*1024*1024)throw new IOException("Not enough free space for this update. Free some space and try again.");
        try{
            for(int redirects=0;redirects<6;redirects++){
                if(cancelled)throw new InterruptedIOException("Download cancelled.");
                if(!trusted(url))throw new IOException("The update redirected outside GitHub.");
                connection=(HttpURLConnection)url.openConnection();connection.setInstanceFollowRedirects(false);
                connection.setConnectTimeout(10000);connection.setReadTimeout(15000);connection.setRequestProperty("User-Agent","FNaF-Redone-Android");
                int status=connection.getResponseCode();
                if(status==301||status==302||status==303||status==307||status==308){
                    String location=connection.getHeaderField("Location");connection.disconnect();
                    if(location==null)throw new IOException("Missing download redirect.");url=new URL(url,location);continue;
                }
                if(status!=200)throw new IOException("GitHub download failed (HTTP "+status+"). Try again later.");
                try(InputStream in=connection.getInputStream();OutputStream out=new FileOutputStream(part)){
                    copy(in,out,size,digest,progress);
                }
                if(cancelled)throw new InterruptedIOException("Download cancelled.");
                java.nio.file.Files.move(part.toPath(),target.toPath(),java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                return;
            }
            throw new IOException("Too many download redirects.");
        }finally{if(connection!=null)connection.disconnect();connection=null;part.delete();}
    }
    void copy(InputStream in,OutputStream out,long size,String digest,Progress progress)throws Exception{
        if(size<=0||size>MAX_BYTES||!digest.matches("sha256:[0-9a-fA-F]{64}"))throw new IOException("Invalid update metadata.");
        MessageDigest sha=MessageDigest.getInstance("SHA-256");byte[] buffer=new byte[65536];long count=0;int n;
        while((n=in.read(buffer))!=-1){
            if(cancelled)throw new InterruptedIOException("Download cancelled.");
            count+=n;if(count>size||count>MAX_BYTES)throw new IOException("The download is larger than expected.");
            sha.update(buffer,0,n);out.write(buffer,0,n);progress.update(count,size);
        }
        if(cancelled)throw new InterruptedIOException("Download cancelled.");
        if(count!=size||!hex(sha.digest()).equalsIgnoreCase(digest.substring(7)))throw new IOException("The download did not pass verification. Please try again.");
    }
    static String hex(byte[] bytes){StringBuilder s=new StringBuilder();for(byte b:bytes)s.append(String.format(Locale.ROOT,"%02x",b&255));return s.toString();}
    static void verify(File file,long size,String digest)throws Exception{
        if(file.length()!=size)throw new IOException("The cached update is incomplete. Check for updates again.");
        try(InputStream in=new FileInputStream(file)){
            new AndroidUpdateTransfer().copy(in,new OutputStream(){public void write(int b){}public void write(byte[] b,int o,int n){}},size,digest,(a,b)->{});
        }
    }
}
