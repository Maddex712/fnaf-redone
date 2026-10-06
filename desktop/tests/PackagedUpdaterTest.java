package dev.thor.fnaf;
import java.nio.file.*;
import java.util.concurrent.TimeUnit;
import java.util.jar.*;
/** Full native EXE -> external helper -> folder swap -> native EXE round trip, isolated from user data. */
public final class PackagedUpdaterTest {
 static void copy(Path source,Path target)throws Exception{try(var files=Files.walk(source)){for(Path p:files.toList()){Path dest=target.resolve(source.relativize(p));if(Files.isDirectory(p))Files.createDirectories(dest);else Files.copy(p,dest);}}}
 static void probe(Path app,Path fixture,String main)throws Exception{
  try(JarOutputStream out=new JarOutputStream(Files.newOutputStream(app.resolve("app/probe.jar")))){
   for(String cls:new String[]{"UpdaterParentProbe","UpdaterLaunchProbe"}){String name="dev/thor/fnaf/"+cls+".class";out.putNextEntry(new JarEntry(name));try(var in=PackagedUpdaterTest.class.getClassLoader().getResourceAsStream(name)){in.transferTo(out);}out.closeEntry();}
  }
  Path cfg=app.resolve("app/FNaF Redone.cfg");String text=Files.readString(cfg).replace("app.mainclass=dev.thor.fnaf.MainActivity","app.mainclass=dev.thor.fnaf."+main).replace("[JavaOptions]","app.classpath=$APPDIR\\probe.jar\n\n[JavaOptions]");
  Files.writeString(cfg,text+"\njava-options=-Dfnaf.updater.fixture="+fixture+"\n");
 }
 public static void main(String[] args)throws Exception{
  Path source=Path.of(args[0]).toAbsolutePath(),temp=Files.createTempDirectory("fnaf packaged update & test ");
  try{
   Path install=temp.resolve("Stable App"),candidate=temp.resolve("Stable App.update-next"),work=Files.createDirectory(temp.resolve("helper")),data=Files.createDirectory(temp.resolve("data"));
   copy(source,install);copy(source,candidate);probe(install,temp,"UpdaterParentProbe");probe(candidate,temp,"UpdaterLaunchProbe");
   Files.copy(Path.of("desktop/updater.ps1"),work.resolve("updater.ps1"));Files.writeString(data.resolve("save.txt"),"keep me");
   Process old=new ProcessBuilder(install.resolve("FNaF Redone.exe").toString()).directory(install.toFile()).redirectErrorStream(true).redirectOutput(work.resolve("parent.log").toFile()).start();
   if(!old.waitFor(20,TimeUnit.SECONDS)){old.destroyForcibly();throw new AssertionError("Old EXE did not exit");}
   if(old.exitValue()!=0)throw new AssertionError(Files.readString(work.resolve("parent.log")));
   long until=System.nanoTime()+15_000_000_000L;Path marker=temp.resolve("restarted.txt");while(!Files.exists(marker)&&System.nanoTime()<until)Thread.sleep(50);
   if(!Files.exists(marker))throw new AssertionError("EXE did not restart: "+Files.readString(work.resolve("helper.log")));
   String result=Files.readString(marker);
   if(!result.startsWith(ReleaseConfig.VERSION+"\n")||!result.toLowerCase().contains(install.resolve("FNaF Redone.exe").toString().toLowerCase()))throw new AssertionError("Wrong version/path: "+result);
   if(!Files.readString(data.resolve("save.txt")).equals("keep me"))throw new AssertionError("Save altered");
   // The marker is written just before the new native EXE exits; allow DLL handles to close.
   long relaunched=Long.parseLong(result.substring(result.lastIndexOf("\n")+1));
   var process=ProcessHandle.of(relaunched);if(process.isPresent())process.get().onExit().get(10,TimeUnit.SECONDS);
   Thread.sleep(300);
   System.out.println("Packaged updater: real EXE handoff, full runtime replacement, same-path restart and save preservation passed");
  }finally{for(int attempt=0;;attempt++){try{DesktopUpdater.removeCreatedTree(temp);break;}catch(java.io.IOException e){if(attempt>=20)throw e;Thread.sleep(200);}}}
 }
}
