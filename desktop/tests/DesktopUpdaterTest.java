package dev.thor.fnaf;

import java.io.*;
import java.nio.file.*;
import java.security.MessageDigest;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.zip.*;

/** Uses disposable installations and a non-restarting helper; never touches the installed game. */
public final class DesktopUpdaterTest {
    static int checks;
    static void check(boolean ok,String reason){checks++;if(!ok)throw new AssertionError(reason);}
    interface Action {void run()throws Exception;}
    static void rejects(Action action,String reason)throws Exception{boolean rejected=false;try{action.run();}catch(IOException e){rejected=true;}check(rejected,reason);}
    static void bundle(Path root,String version)throws IOException{
        for(String file:new String[]{"FNaF Redone.exe","app/fnaf-redone.jar","app/FNaF Redone.cfg","runtime/bin/java.exe","runtime/bin/server/jvm.dll","app/release-version.txt"}){
            Path p=root.resolve(file);Files.createDirectories(p.getParent());Files.writeString(p,version);
        }
    }
    static Path archive(Path dir,String name,String entry)throws IOException{
        Path zip=dir.resolve(name);try(ZipOutputStream out=new ZipOutputStream(Files.newOutputStream(zip))){out.putNextEntry(new ZipEntry(entry));out.write("test".getBytes());out.closeEntry();}return zip;
    }
    static Process helper(Path script,Path install,Path candidate,Path data,long parent,Path cwd)throws IOException{
        return new ProcessBuilder(Paths.get(System.getenv("SystemRoot"),"System32/WindowsPowerShell/v1.0/powershell.exe").toString(),"-NoProfile","-NonInteractive","-WindowStyle","Hidden","-ExecutionPolicy","Bypass","-File",script.toString(),"-Install",install.toString(),"-Staged",candidate.toString(),"-Data",data.toString(),"-ParentId",""+parent,"-NoRestart").directory(cwd.toFile()).redirectErrorStream(true).redirectOutput(script.resolveSibling("test-helper.log").toFile()).start();
    }
    static int finish(Process p)throws Exception{if(!p.waitFor(20,TimeUnit.SECONDS)){p.destroyForcibly();throw new AssertionError("Helper timed out");}return p.exitValue();}
    public static void main(String[] args)throws Exception{
        Path temp=Files.createTempDirectory("fnaf update & owner's test ");
        try{
            String json="[{\"tag_name\":\"v0.2.9-windows-preview\",\"assets\":[{\"name\":\"FNaF-Redone-0.2.9-Windows-x64-preview.zip\",\"size\":100,\"digest\":\"sha256:"+"a".repeat(64)+"\",\"browser_download_url\":\"https://github.com/Maddex712/fnaf-redone/releases/download/v0.2.9-windows-preview/update.zip\"}]}]";
            check(ReleaseUpdates.select(json,"0.2.5-windows-preview","Windows").installable(),"Verified release offers installation");
            check(!ReleaseUpdates.select(json.replace("sha256:","md5:"),"0.2.5-windows-preview","Windows").installable(),"Missing checksum prevents install");
            check(!ReleaseUpdates.select(json.replace("https://github.com/","https://evil.example/"),"0.2.5-windows-preview","Windows").installable(),"Foreign download refused");
            byte[] bytes="correct release bytes".getBytes();String digest="sha256:"+HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
            DesktopUpdater.copyVerified(new ByteArrayInputStream(bytes),temp.resolve("good.zip"),bytes.length,digest,s->{},()->false);
            check(Arrays.equals(bytes,Files.readAllBytes(temp.resolve("good.zip"))),"Download exact bytes");
            rejects(()->DesktopUpdater.copyVerified(new ByteArrayInputStream(bytes),temp.resolve("bad.zip"),bytes.length,"sha256:"+"0".repeat(64),s->{},()->false),"Corrupt download refused");
            rejects(()->DesktopUpdater.copyVerified(new ByteArrayInputStream(bytes),temp.resolve("truncated.zip"),bytes.length+1,digest,s->{},()->false),"Truncated download refused");
            rejects(()->DesktopUpdater.copyVerified(new ByteArrayInputStream(bytes),temp.resolve("cancelled.zip"),bytes.length,digest,s->{},()->true),"Cancellation stops download");
            for(String entry:new String[]{"FNaF Redone/../escape","FNaF Redone/app/../../escape","FNaF Redone/C:/escape","FNaF Redone/app/CON.txt","Other/app.jar"}){
                Path zip=archive(temp,"unsafe-"+checks+".zip",entry),target=Files.createTempDirectory(temp,"extract-");
                rejects(()->DesktopUpdater.unpack(zip,target,()->false),"Unsafe ZIP rejected: "+entry);
            }
            Path install=temp.resolve("Stable Game"),candidate=temp.resolve("Stable Game.update-one"),data=Files.createDirectory(temp.resolve("User Data"));
            Files.writeString(data.resolve("save.txt"),"saved progress");bundle(install,"old");bundle(candidate,"0.2.9-windows-preview");Files.writeString(install.resolve("obsolete.txt"),"old version only");
            DesktopUpdater.validateBundle(candidate,"v0.2.9-windows-preview");
            rejects(()->DesktopUpdater.validateBundle(candidate,"v0.3.0-windows-preview"),"Wrong archive version refused");
            Path work=Files.createDirectory(temp.resolve("helper")),script=work.resolve("updater.ps1");Files.copy(Path.of("desktop/updater.ps1"),script);
            Process parent=new ProcessBuilder("powershell.exe","-NoProfile","-WindowStyle","Hidden","-Command","Start-Sleep -Seconds 3").start();
            Process updater=helper(script,install,candidate,data,parent.pid(),work);
            long until=System.nanoTime()+2_000_000_000L;while(!Files.exists(work.resolve("ready"))&&System.nanoTime()<until)Thread.sleep(20);
            check(Files.readString(install.resolve("FNaF Redone.exe")).equals("old"),"Old app intact while parent runs");
            check(finish(updater)==0,"Helper installed successfully: "+Files.readString(work.resolve("test-helper.log")));
            check(Files.readString(install.resolve("FNaF Redone.exe")).equals("0.2.9-windows-preview"),"EXE replaced at same path");
            check(!Files.exists(install.resolve("obsolete.txt")),"Old program files removed from installation");
            Path backup=install.resolveSibling("Stable Game.previous");check(Files.readString(backup.resolve("FNaF Redone.exe")).equals("old"),"Previous version retained");
            check(Files.readString(data.resolve("save.txt")).equals("saved progress"),"External saves unchanged");
            // The helper's current directory locks the candidate on Windows, forcing post-backup rollback.
            Path locked=temp.resolve("Stable Game.update-locked");bundle(locked,"next");
            check(finish(helper(script,install,locked,data,2147483647,locked))!=0,"Locked candidate fails installation");
            check(Files.readString(install.resolve("FNaF Redone.exe")).equals("0.2.9-windows-preview"),"Failed replacement restores previous EXE");
            check(Files.readString(work.resolve("result.txt")).contains("Previous version restored"),"Rollback reported");
            check(Files.readString(data.resolve("save.txt")).equals("saved progress"),"Rollback leaves saves unchanged");
            // An unrelated sibling directory must never be deleted as an updater backup.
            bundle(backup,"not an updater backup");Path next=temp.resolve("Stable Game.update-two");bundle(next,"next");
            check(finish(helper(script,install,next,data,2147483647,work))!=0,"Unowned backup folder refused");
            check(Files.readString(backup.resolve("FNaF Redone.exe")).equals("not an updater backup"),"Unowned sibling preserved");
            System.out.println("Desktop updater: "+checks+" checks passed (hashes, ZIP safety, cancellation, same path, process exit, rollback, saves)");
        }finally{DesktopUpdater.removeCreatedTree(temp);}
    }
}
