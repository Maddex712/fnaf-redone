using System;
using System.Diagnostics;
using System.Drawing;
using System.IO;
using System.Linq;
using System.Reflection;
using System.Runtime.InteropServices;
using System.Windows.Forms;
using Microsoft.Win32;
using FreddyFiles;
[assembly: System.Runtime.Versioning.TargetFramework(".NETFramework,Version=v4.8")]

class InstallerTests {
    static int checks;
    static void Check(bool condition, string message) {
        if (!condition) throw new Exception(message);
        checks++; Console.WriteLine("PASS: " + message);
    }
    static void Refuses(Action action, string message) {
        bool failed = false; try { action(); } catch (IOException) { failed = true; }
        Check(failed, message);
    }
    static string LinkTarget(string file) {
        Type type = Type.GetTypeFromProgID("WScript.Shell"); object shell = Activator.CreateInstance(type), link = null;
        try {
            link = type.InvokeMember("CreateShortcut", BindingFlags.InvokeMethod, null, shell, new object[] { file });
            return Convert.ToString(link.GetType().InvokeMember("TargetPath", BindingFlags.GetProperty, null, link, null));
        } finally { if (link != null) Marshal.FinalReleaseComObject(link); Marshal.FinalReleaseComObject(shell); }
    }
    static void CopyTree(string source, string target) {
        Directory.CreateDirectory(target);
        foreach (string file in Directory.GetFiles(source)) File.Copy(file, Path.Combine(target, Path.GetFileName(file)));
        foreach (string dir in Directory.GetDirectories(source)) CopyTree(dir, Path.Combine(target, Path.GetFileName(dir)));
    }
    [STAThread] static int Main(string[] args) {
        string root = Path.Combine(Path.GetFullPath(args[0]), "fixture-" + Guid.NewGuid().ToString("N"));
        Directory.CreateDirectory(root);
        string key = @"Software\Microsoft\Windows\CurrentVersion\Uninstall\FreddyFilesTest-" + Guid.NewGuid().ToString("N");
        var engine = new InstallEngine(key, Path.Combine(root, "Start Menu"), Path.Combine(root, "Desktop"));
        string target = Path.Combine(root, "Game & space's folder"), data = Path.Combine(root, "saved-data");
        Action<int, string> progress = delegate { };
        try {
            Directory.CreateDirectory(data); File.WriteAllText(Path.Combine(data, "save.json"), "preserve-save");
            Directory.CreateDirectory(Path.Combine(root, "unrelated")); File.WriteAllText(Path.Combine(root, "unrelated/keep.txt"), "keep");
            Refuses(() => InstallEngine.ValidateTarget(Path.Combine(root, "unrelated")), "Reject unrelated destination");
            Refuses(() => InstallEngine.ValidateTarget(Environment.GetFolderPath(Environment.SpecialFolder.UserProfile)), "Reject profile root");
            Refuses(() => InstallEngine.ValidateTarget(Path.Combine(Environment.GetFolderPath(Environment.SpecialFolder.LocalApplicationData), "FNaFRedone")), "Reject saved-data directory");
            string note = engine.Install(target, true, progress);
            Check(note == "", "Fresh install completes without warning"); InstallEngine.ValidateApp(target);
            Check(File.Exists(Path.Combine(target, "runtime/bin/server/jvm.dll")), "Bundled runtime installed");
            Check(File.ReadAllText(Path.Combine(target, "app/release-version.txt")).Trim() == BuildInfo.Version + "-windows-preview", "Installed version matches");
            Check(File.Exists(Path.Combine(InstallEngine.Maintenance(target), "uninstall.exe")), "Uninstaller outside app folder");
            using (RegistryKey registration = Registry.CurrentUser.OpenSubKey(key)) {
                Check(registration != null && Convert.ToString(registration.GetValue("InstallLocation")) == target, "Windows uninstall registration points at game");
                Check(Convert.ToString(registration.GetValue("UninstallString")) == "\"" + Path.Combine(InstallEngine.Maintenance(target), "uninstall.exe") + "\"", "Uninstall command quoted correctly");
            }
            string link = Path.Combine(engine.StartMenu, "FNaF Redone.lnk");
            Check(String.Equals(LinkTarget(link), Path.Combine(target, "FNaF Redone.exe"), StringComparison.OrdinalIgnoreCase), "Start menu shortcut points to stable EXE");
            Check(File.Exists(Path.Combine(engine.Desktop, "FNaF Redone.lnk")), "Optional desktop shortcut created");
            using (FileStream locked = new FileStream(Path.Combine(target, "runtime/bin/server/jvm.dll"), FileMode.Open, FileAccess.Read, FileShare.Read)) {
                Refuses(() => engine.Install(target, true, progress), "Running game refuses replacement");
                Refuses(() => engine.Uninstall(target), "Running game refuses uninstall before deleting files");
                Check(File.Exists(Path.Combine(target, "app/fnaf-redone.jar")), "Locked uninstall keeps app intact");
            }
            string version = Path.Combine(target, "app/release-version.txt");
            File.WriteAllText(version, "99.0.0-windows-preview");
            Refuses(() => engine.Install(target, true, progress), "Refuses downgrade of newer release");
            File.WriteAllText(version, BuildInfo.Version + "-windows-preview");
            string maintenanceExe = Path.Combine(InstallEngine.Maintenance(target), "uninstall.exe");
            using (FileStream locked = new FileStream(maintenanceExe, FileMode.Open, FileAccess.Read, FileShare.Read)) {
                Refuses(() => engine.Install(target, false, progress), "Maintenance write failure rolls back replacement");
                InstallEngine.ValidateApp(target);
                Check(!Directory.GetDirectories(root, "*.before-install-*").Any(), "Rollback restores original folder without a stranded backup");
            }
            File.WriteAllText(Path.Combine(target, "personal-note.txt"), "keep");
            note = engine.Install(target, false, progress);
            string backup = Directory.GetDirectories(root, "*.before-install-*").Single();
            Check(File.Exists(Path.Combine(backup, "personal-note.txt")), "Existing installation preserved as recovery folder");
            Check(note.Contains(backup), "Installer reports recovery location");
            Check(File.ReadAllText(Path.Combine(data, "save.json")) == "preserve-save", "Reinstall preserves separate save data");
            // Exercise the production updater's actual full-directory swap with an installed game.
            string staged = target + ".update-test"; CopyTree(target, staged);
            File.WriteAllText(Path.Combine(staged, "app/new-version-file.txt"), "new");
            string helper = Path.Combine(root, "updater.ps1"); File.Copy(args[1], helper);
            var start = new ProcessStartInfo("powershell.exe", "-NoProfile -NonInteractive -ExecutionPolicy Bypass -File \"" + helper + "\" -Install \"" + target + "\" -Staged \"" + staged + "\" -Data \"" + data + "\" -ParentId 2147483647 -NoRestart");
            start.UseShellExecute = false; start.CreateNoWindow = true;
            using (Process process = Process.Start(start)) {
                if (!process.WaitForExit(45000)) throw new Exception("Updater test timed out");
                Check(process.ExitCode == 0, "Production updater replaces installed app successfully");
            }
            Check(File.Exists(Path.Combine(target, "app/new-version-file.txt")), "New payload active at original path");
            Check(File.Exists(Path.Combine(InstallEngine.Maintenance(target), "uninstall.exe")), "Updater preserves external uninstaller");
            Check(String.Equals(LinkTarget(link), Path.Combine(target, "FNaF Redone.exe"), StringComparison.OrdinalIgnoreCase), "Shortcut unchanged after update");
            File.WriteAllText(Path.Combine(target, "keep-my-note.txt"), "leave");
            engine.Uninstall(target);
            Check(!File.Exists(Path.Combine(target, "FNaF Redone.exe")), "Uninstall removes game EXE");
            Check(!Directory.Exists(Path.Combine(target, "app")) && !Directory.Exists(Path.Combine(target, "runtime")), "Uninstall removes updated app and runtime");
            Check(File.ReadAllText(Path.Combine(target, "keep-my-note.txt")) == "leave", "Uninstall preserves unrelated root files");
            Check(File.ReadAllText(Path.Combine(data, "save.json")) == "preserve-save", "Uninstall preserves save data");
            Check(Registry.CurrentUser.OpenSubKey(key) == null, "Uninstall removes registration");
            Check(!File.Exists(link) && !File.Exists(Path.Combine(engine.Desktop, "FNaF Redone.lnk")), "Uninstall removes shortcuts");
            Check(File.Exists(Path.Combine(backup, "personal-note.txt")), "Recovery copy preserved on uninstall");
            Application.EnableVisualStyles();
            using (var window = new InstallerWindow()) {
                window.ShowInTaskbar = false; window.StartPosition = FormStartPosition.Manual;
                window.Location = new Point(-32000, -32000); window.Show(); Application.DoEvents();
                using (var image = new Bitmap(window.Width, window.Height)) {
                    window.DrawToBitmap(image, new Rectangle(0, 0, image.Width, image.Height));
                    image.Save(Path.Combine(args[0], "installer-preview.png"));
                }
                window.Close();
            }
            Console.WriteLine(checks + " installer checks passed. Fixture retained at " + root);
            return 0;
        } catch (Exception ex) { Console.Error.WriteLine(ex); return 1; }
        finally { Registry.CurrentUser.DeleteSubKeyTree(key, false); }
    }
}
