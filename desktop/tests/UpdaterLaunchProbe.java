package dev.thor.fnaf;
import java.nio.file.*;
/** Test-only replacement main verifies that the helper relaunches the same EXE. */
public final class UpdaterLaunchProbe {
 public static void main(String[] args)throws Exception{
  Files.writeString(Path.of(System.getProperty("fnaf.updater.fixture")).resolve("restarted.txt"),ReleaseConfig.VERSION+"\n"+ProcessHandle.current().info().command().orElse("unknown")+"\n"+ProcessHandle.current().pid());
  System.exit(0);
 }
}
