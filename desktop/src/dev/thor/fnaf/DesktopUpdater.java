package dev.thor.fnaf;

import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.security.MessageDigest;
import java.util.*;
import java.util.function.*;
import java.util.zip.*;

/** Stages a verified full app beside the installation; a separate helper swaps it after exit. */
final class DesktopUpdater {
    static final long MAX_DOWNLOAD=256L*1024*1024,MAX_UNPACKED=768L*1024*1024;
    record Staged(Path install,Path candidate,Path work,Path data) {}
    static Path installation()throws Exception{
        Path jar=Paths.get(DesktopUpdater.class.getProtectionDomain().getCodeSource().getLocation().toURI());
        if(!jar.getFileName().toString().equals("fnaf-redone.jar"))throw new IOException("Automatic updates require the packaged Windows EXE.");
        Path root=jar.getParent().getParent().toRealPath();validateBundle(root,null);return root;
    }
    static void validateBundle(Path root,String tag)throws IOException{
        for(String name:new String[]{"FNaF Redone.exe","app/fnaf-redone.jar","app/FNaF Redone.cfg","runtime/bin/java.exe","runtime/bin/server/jvm.dll"})
            if(!Files.isRegularFile(root.resolve(name),LinkOption.NOFOLLOW_LINKS))throw new IOException("Incomplete Windows release: "+name);
        if(tag!=null){String version=Files.readString(root.resolve("app/release-version.txt")).trim();if(!version.equals(tag.replaceFirst("^v","")))throw new IOException("Release version does not match the download.");}
    }
    static void cancelled(BooleanSupplier cancel)throws IOException{if(cancel.getAsBoolean())throw new InterruptedIOException("Update cancelled.");}
    static HttpURLConnection connect(String url)throws IOException{
        URI uri=URI.create(url);
        for(int redirects=0;redirects<6;redirects++){
            String host=uri.getHost();
            if(!"https".equals(uri.getScheme())||uri.getUserInfo()!=null||uri.getPort()!=-1||!("github.com".equals(host)||"release-assets.githubusercontent.com".equals(host)||"objects.githubusercontent.com".equals(host)))throw new IOException("Unexpected release download host.");
            HttpURLConnection c=(HttpURLConnection)uri.toURL().openConnection();c.setInstanceFollowRedirects(false);c.setConnectTimeout(10000);c.setReadTimeout(15000);c.setRequestProperty("User-Agent","FNaF-Redone-updater");
            int status=c.getResponseCode();
            if(status==200)return c;
            String location=c.getHeaderField("Location");c.disconnect();
            if((status!=301&&status!=302&&status!=303&&status!=307&&status!=308)||location==null)throw new IOException("Download failed (HTTP "+status+").");
            uri=uri.resolve(location);
        }
        throw new IOException("Too many download redirects.");
    }
    static void copyVerified(InputStream in,Path zip,long expected,String digest,Consumer<String> progress,BooleanSupplier cancel)throws Exception{
        if(expected<=0||expected>MAX_DOWNLOAD||!digest.matches("sha256:[0-9a-fA-F]{64}"))throw new IOException("Release has no valid size/checksum.");
        MessageDigest hash=MessageDigest.getInstance("SHA-256");long total=0,last=-1;byte[] bytes=new byte[65536];int n;
        try(OutputStream out=Files.newOutputStream(zip,StandardOpenOption.CREATE_NEW)){
            while((n=in.read(bytes))!=-1){cancelled(cancel);total+=n;if(total>expected||total>MAX_DOWNLOAD)throw new IOException("Download exceeds expected size.");hash.update(bytes,0,n);out.write(bytes,0,n);long percent=total*100/expected;if(percent!=last){progress.accept("Downloading update: "+percent+"%");last=percent;}}
        }
        cancelled(cancel);
        if(total!=expected||!HexFormat.of().formatHex(hash.digest()).equalsIgnoreCase(digest.substring(7)))throw new IOException("Download verification failed. Your installed game has not changed.");
    }
    static void unpack(Path zip,Path target,BooleanSupplier cancel)throws IOException{
        long total=0;int count=0;HashSet<String> names=new HashSet<>();
        try(ZipInputStream in=new ZipInputStream(Files.newInputStream(zip))){ZipEntry entry;byte[] bytes=new byte[65536];
            while((entry=in.getNextEntry())!=null){
                cancelled(cancel);if(++count>8000)throw new IOException("Too many archive entries.");
                String name=entry.getName().replace('\\','/');
                if(!name.startsWith("FNaF Redone/"))throw new IOException("Unexpected archive root.");
                String relative=name.substring(12);if(relative.isEmpty())continue;
                for(String part:relative.split("/"))if(part.equals(".")||part.equals("..")||part.endsWith(".")||part.endsWith(" ")||part.matches("(?i)(CON|PRN|AUX|NUL|COM[0-9]|LPT[0-9])(?:\\..*)?")||part.matches(".*[:<>\"|?*].*"))throw new IOException("Unsafe archive path.");
                Path dest=target.resolve(relative).normalize();if(!dest.startsWith(target)||dest.equals(target)||!names.add(relative.toLowerCase(Locale.ROOT)))throw new IOException("Unsafe or duplicate archive entry.");
                if(entry.isDirectory()){Files.createDirectories(dest);continue;}
                Files.createDirectories(dest.getParent());
                try(OutputStream out=Files.newOutputStream(dest,StandardOpenOption.CREATE_NEW)){int n;while((n=in.read(bytes))!=-1){cancelled(cancel);total+=n;if(total>MAX_UNPACKED)throw new IOException("Expanded release is too large.");out.write(bytes,0,n);}}
            }
        }
    }
    static Staged prepare(ReleaseUpdates.Result release,Path install,Path data,Consumer<String> progress,BooleanSupplier cancel)throws Exception{
        if(!release.installable())throw new IOException("This release requires a manual download.");
        install=install.toRealPath();data=data.toRealPath();validateBundle(install,null);
        if(install.getParent()==null||data.startsWith(install)||data.startsWith(install.resolveSibling(install.getFileName()+".previous")))throw new IOException("Keep the application in its own folder, separate from its save-data folder.");
        Path work=Files.createTempDirectory(Files.createDirectories(data.resolve("updates")),"update-");
        Path candidate=null;
        try{
            candidate=Files.createTempDirectory(install.getParent(),install.getFileName()+".update-");
            progress.accept("Downloading verified Windows release...");HttpURLConnection c=connect(release.assetUrl);
            Path zip=work.resolve("release.zip");try(InputStream in=c.getInputStream()){copyVerified(in,zip,release.size,release.digest,progress,cancel);}finally{c.disconnect();}
            progress.accept("Checking and unpacking update...");unpack(zip,candidate,cancel);validateBundle(candidate,release.tag);Files.delete(zip);
            try(InputStream script=DesktopUpdater.class.getResourceAsStream("/updater.ps1")){if(script==null)throw new IOException("Update helper is missing.");Files.copy(script,work.resolve("updater.ps1"));}
            return new Staged(install,candidate,work,data);
        }catch(Exception e){if(candidate!=null)removeCreatedTree(candidate);removeCreatedTree(work);throw e;}
    }
    /** Only used for freshly created staging directories owned by this operation; never follows links. */
    static void removeCreatedTree(Path path)throws IOException{
        if(!Files.exists(path,LinkOption.NOFOLLOW_LINKS))return;
        try(var files=Files.walk(path)){for(Path p:files.sorted(Comparator.reverseOrder()).toList()){
            if(Files.isRegularFile(p,LinkOption.NOFOLLOW_LINKS)){
                var dos=Files.getFileAttributeView(p,java.nio.file.attribute.DosFileAttributeView.class,LinkOption.NOFOLLOW_LINKS);
                if(dos!=null)dos.setReadOnly(false);
            }
            Files.delete(p);
        }}
    }
    static void handoff(Staged stage)throws Exception{
        Path powershell=Paths.get(System.getenv("SystemRoot"),"System32","WindowsPowerShell","v1.0","powershell.exe");
        Process helper=new ProcessBuilder(powershell.toString(),"-NoProfile","-NonInteractive","-WindowStyle","Hidden","-ExecutionPolicy","Bypass","-File",stage.work.resolve("updater.ps1").toString(),"-Install",stage.install.toString(),"-Staged",stage.candidate.toString(),"-Data",stage.data.toString(),"-ParentId",Long.toString(ProcessHandle.current().pid())).directory(stage.work.toFile()).redirectErrorStream(true).redirectOutput(stage.work.resolve("helper.log").toFile()).start();
        Path ready=stage.work.resolve("ready");long deadline=System.nanoTime()+10_000_000_000L;
        while(System.nanoTime()<deadline&&helper.isAlive()){if(Files.exists(ready))return;Thread.sleep(50);}
        helper.destroyForcibly();helper.waitFor();throw new IOException("Update helper could not start. The current game is unchanged. See "+stage.work.resolve("helper.log"));
    }
}
