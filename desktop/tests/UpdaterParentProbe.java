package dev.thor.fnaf;
import java.nio.file.*;
/** Test-only replacement main for the real packaged EXE; no window or user-data access. */
public final class UpdaterParentProbe {
 public static void main(String[] args)throws Exception{
  Path root=Path.of(System.getProperty("fnaf.updater.fixture"));
  DesktopUpdater.handoff(new DesktopUpdater.Staged(DesktopUpdater.installation(),root.resolve("Stable App.update-next"),root.resolve("helper"),root.resolve("data")));
  System.exit(0);
 }
}
