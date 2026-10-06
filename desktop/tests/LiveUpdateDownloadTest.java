package dev.thor.fnaf;
import java.nio.file.*;
/** Explicit opt-in network integration check; never executes downloaded code. */
public final class LiveUpdateDownloadTest {
 public static void main(String[] args)throws Exception{
  Path temp=Files.createTempDirectory("fnaf live update ");
  try{
   Path install=temp.resolve("App"),data=Files.createDirectory(temp.resolve("Data"));DesktopUpdaterTest.bundle(install,"old");Files.writeString(data.resolve("save"),"unchanged");
   ReleaseUpdates.Result result=ReleaseUpdates.check("0.2.4-windows-preview","Windows");
   if(!result.installable())throw new AssertionError(result.message);
   DesktopUpdater.Staged stage=DesktopUpdater.prepare(result,install,data,s->{},()->false);
   DesktopUpdater.validateBundle(stage.candidate(),result.tag);
   if(!Files.readString(install.resolve("FNaF Redone.exe")).equals("old")||!Files.readString(data.resolve("save")).equals("unchanged"))throw new AssertionError("Preparation changed installed game or saves");
   if(!Files.isRegularFile(stage.work().resolve("updater.ps1")))throw new AssertionError("No helper resource");
   System.out.println("Live update: GitHub download, SHA-256, full extraction, version verification and staging passed: "+result.tag);
  }finally{DesktopUpdater.removeCreatedTree(temp);}
 }
}
