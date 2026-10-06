using System;
using System.Collections.Generic;
using System.Diagnostics;
using System.Drawing;
using System.IO;
using System.IO.Compression;
using System.Linq;
using System.Reflection;
using System.Runtime.InteropServices;
using System.Security.Cryptography;
using System.Threading.Tasks;
using System.Windows.Forms;
using Microsoft.Win32;

namespace FreddyFiles {
    public sealed class InstallEngine {
        public const string Game = "FNaF Redone";
        public const string RegistryPath = @"Software\Microsoft\Windows\CurrentVersion\Uninstall\FNaFRedone";
        public readonly string RegistryKey, StartMenu, Desktop;
        public InstallEngine() : this(RegistryPath,
            Path.Combine(Environment.GetFolderPath(Environment.SpecialFolder.Programs), Game),
            Environment.GetFolderPath(Environment.SpecialFolder.DesktopDirectory)) { }
        public InstallEngine(string key, string startMenu, string desktop) {
            RegistryKey = key; StartMenu = startMenu; Desktop = desktop;
        }
        public static string Maintenance(string target) { return target + ".freddyfiles"; }
        public static string Full(string path) { return Path.GetFullPath(path).TrimEnd(Path.DirectorySeparatorChar); }
        static bool Same(string a, string b) { return String.Equals(Full(a), Full(b), StringComparison.OrdinalIgnoreCase); }
        static bool Within(string path, string parent) {
            return Same(path, parent) || Full(path).StartsWith(Full(parent) + "\\", StringComparison.OrdinalIgnoreCase);
        }
        public static void NoLinks(string path) {
            for (string p = Full(path); !String.IsNullOrEmpty(p); p = Path.GetDirectoryName(p)) {
                if ((File.Exists(p) || Directory.Exists(p)) && (File.GetAttributes(p) & FileAttributes.ReparsePoint) != 0)
                    throw new IOException("Please use a folder without symbolic links or junctions.");
            }
        }
        public static void ValidateApp(string target) {
            foreach (string file in new[] { "FNaF Redone.exe", "app/fnaf-redone.jar", "app/FNaF Redone.cfg",
                "app/release-version.txt", "runtime/bin/java.exe", "runtime/bin/server/jvm.dll" })
                if (!File.Exists(Path.Combine(target, file))) throw new IOException("This is not a complete FNaF Redone installation.");
        }
        static void ValidateLocation(string target) {
            target = Full(target);
            if (String.IsNullOrEmpty(Path.GetDirectoryName(target)) || target.StartsWith(@"\\"))
                throw new IOException("Choose a local folder for FNaF Redone, not a drive or network root.");
            foreach (Environment.SpecialFolder folder in new[] { Environment.SpecialFolder.UserProfile,
                Environment.SpecialFolder.LocalApplicationData, Environment.SpecialFolder.ApplicationData,
                Environment.SpecialFolder.DesktopDirectory, Environment.SpecialFolder.MyDocuments,
                Environment.SpecialFolder.ProgramFiles, Environment.SpecialFolder.ProgramFilesX86,
                Environment.SpecialFolder.Windows, Environment.SpecialFolder.Programs }) {
                string reserved = Environment.GetFolderPath(folder);
                if (!String.IsNullOrEmpty(reserved) && Within(reserved, target))
                    throw new IOException("Choose a dedicated game folder, not an existing system or personal folder.");
            }
            string data = Path.Combine(Environment.GetFolderPath(Environment.SpecialFolder.LocalApplicationData), "FNaFRedone");
            if (Within(target, data) || Within(data, target)) throw new IOException("The game cannot be installed in its save-data folder.");
            if (Within(typeof(InstallEngine).Assembly.Location, target)) throw new IOException("Move FreddyFiles outside the folder you want to replace, then run it again.");
            NoLinks(target); NoLinks(Maintenance(target));
        }
        public static void ValidateTarget(string target) {
            target = Full(target); ValidateLocation(target);
            if (Directory.Exists(target) && Directory.EnumerateFileSystemEntries(target).Any()) {
                ValidateApp(target);
                foreach (string relative in new[] { "FNaF Redone.exe", "runtime/bin/server/jvm.dll" })
                    using (FileStream probe = new FileStream(Path.Combine(target, relative), FileMode.Open, FileAccess.Read, FileShare.None)) { }
                Version installed;
                string version = File.ReadAllText(Path.Combine(target, "app/release-version.txt")).Trim().Split('-')[0];
                if (!Version.TryParse(version, out installed)) throw new IOException("The installed version could not be read.");
                if (installed > new Version(BuildInfo.Version)) throw new IOException("A newer version is already installed. Use that version or choose another folder.");
            }
            string maintenance = Maintenance(target);
            if (Directory.Exists(maintenance) && Directory.EnumerateFileSystemEntries(maintenance).Any()) CheckRecord(target);
        }
        static void CheckRecord(string target) {
            string record = Path.Combine(Maintenance(target), "installation.txt");
            if (!File.Exists(record) || !Same(File.ReadAllText(record).Trim(), target))
                throw new IOException("The maintenance folder belongs to a different installation. Choose another folder.");
        }
        public static string DefaultTarget() {
            using (RegistryKey key = Registry.CurrentUser.OpenSubKey(RegistryPath)) {
                string saved = key == null ? null : key.GetValue("InstallLocation") as string;
                if (!String.IsNullOrEmpty(saved)) return saved;
            }
            return Path.Combine(Environment.GetFolderPath(Environment.SpecialFolder.LocalApplicationData), "Programs", Game);
        }
        static void Resource(string name, string destination) {
            using (Stream input = typeof(InstallEngine).Assembly.GetManifestResourceStream(name)) {
                if (input == null) throw new IOException("Installer resource missing: " + name);
                using (FileStream output = File.Create(destination)) input.CopyTo(output);
            }
        }
        public string Install(string target, bool desktopShortcut, Action<int, string> progress) {
            target = Full(target); ValidateTarget(target);
            string parent = Path.GetDirectoryName(target);
            Directory.CreateDirectory(parent);
            string stage = target + ".install-" + Guid.NewGuid().ToString("N");
            string backup = target + ".before-install-" + DateTime.Now.ToString("yyyyMMddHHmmss") + "-" + Guid.NewGuid().ToString("N").Substring(0, 6);
            string maintenance = Maintenance(target);
            bool movedOld = false, movedNew = false;
            Directory.CreateDirectory(stage);
            try {
                progress(3, "Verifying the bundled game...");
                string zipPath = Path.Combine(stage, "payload.zip");
                Resource("payload.zip", zipPath);
                using (SHA256 sha = SHA256.Create()) using (Stream zip = File.OpenRead(zipPath)) {
                    string actual = BitConverter.ToString(sha.ComputeHash(zip)).Replace("-", "").ToLowerInvariant();
                    if (actual != BuildInfo.PayloadHash) throw new IOException("The installer is damaged. Download FreddyFiles again.");
                }
                string unpacked = Path.Combine(stage, "unpacked"); Directory.CreateDirectory(unpacked);
                var files = new List<string>();
                using (ZipArchive archive = ZipFile.OpenRead(zipPath)) {
                    if (archive.Entries.Count > 8000) throw new IOException("Unexpected installer contents.");
                    long size = 0;
                    foreach (ZipArchiveEntry entry in archive.Entries) {
                        string name = entry.FullName.Replace('\\', '/');
                        if (!name.StartsWith(Game + "/", StringComparison.Ordinal)) throw new IOException("Unexpected archive root.");
                        string relative = name.Substring(Game.Length + 1);
                        if (relative.Length == 0) continue;
                        if (relative.Split('/').Any(p => p == ".." || p == "." || p.Contains(":"))) throw new IOException("Invalid archive path.");
                        string destination = Full(Path.Combine(unpacked, relative));
                        if (!Within(destination, unpacked) || Same(destination, unpacked)) throw new IOException("Invalid archive path.");
                        if (name.EndsWith("/")) { Directory.CreateDirectory(destination); continue; }
                        size += entry.Length;
                        if (size > 805306368L) throw new IOException("Unexpected installer size.");
                        Directory.CreateDirectory(Path.GetDirectoryName(destination));
                        using (Stream source = entry.Open()) using (FileStream output = new FileStream(destination, FileMode.CreateNew)) source.CopyTo(output);
                        files.Add(relative);
                        progress(10 + (files.Count * 65 / archive.Entries.Count), "Installing game files...");
                    }
                }
                ValidateApp(unpacked);
                if (File.ReadAllText(Path.Combine(unpacked, "app/release-version.txt")).Trim() != BuildInfo.Version + "-windows-preview")
                    throw new IOException("The bundled game version does not match this installer.");
                // Maintenance stays outside the app: the in-app updater replaces the whole app directory.
                Directory.CreateDirectory(maintenance);
                Resource("uninstall.exe", Path.Combine(stage, "uninstall.exe"));
                if (Directory.Exists(target)) { Directory.Move(target, backup); movedOld = true; }
                Directory.Move(unpacked, target); movedNew = true;
                File.Copy(Path.Combine(stage, "uninstall.exe"), Path.Combine(maintenance, "uninstall.exe"), true);
                File.WriteAllText(Path.Combine(maintenance, "installation.txt"), target);
                File.WriteAllLines(Path.Combine(maintenance, "files.txt"), files);
            } catch {
                // Keep the old folder intact if any installation step fails.
                if (movedNew) Directory.Move(target, Path.Combine(stage, "failed-install"));
                if (movedOld && !Directory.Exists(target)) Directory.Move(backup, target);
                throw;
            } finally { RemoveOwnedStage(stage, target + ".install-"); }
            progress(90, "Creating shortcuts...");
            string warning = "";
            try { Register(target, desktopShortcut); }
            catch (Exception ex) { warning = "The game is installed, but Windows could not create a shortcut or registration: " + ex.Message; }
            progress(100, "Ready to play.");
            if (movedOld) warning += "\nYour previous folder was kept at:\n" + backup;
            return warning.Trim();
        }
        static void RemoveOwnedStage(string folder, string prefix) {
            if (!Full(folder).StartsWith(Full(prefix), StringComparison.OrdinalIgnoreCase)) throw new IOException("Invalid staging folder.");
            if (!Directory.Exists(folder)) return;
            NoLinks(folder);
            foreach (string item in Directory.EnumerateFileSystemEntries(folder)) {
                NoLinks(item);
                if (Directory.Exists(item)) RemoveOwnedStage(item, prefix);
                else { File.SetAttributes(item, FileAttributes.Normal); File.Delete(item); }
            }
            Directory.Delete(folder);
        }
        static void Shortcut(string file, string target) {
            Directory.CreateDirectory(Path.GetDirectoryName(file));
            Type shellType = Type.GetTypeFromProgID("WScript.Shell");
            object shell = Activator.CreateInstance(shellType), shortcut = null;
            try {
                shortcut = shellType.InvokeMember("CreateShortcut", BindingFlags.InvokeMethod, null, shell, new object[] { file });
                Type type = shortcut.GetType();
                type.InvokeMember("TargetPath", BindingFlags.SetProperty, null, shortcut, new object[] { target });
                type.InvokeMember("WorkingDirectory", BindingFlags.SetProperty, null, shortcut, new object[] { Path.GetDirectoryName(target) });
                type.InvokeMember("Save", BindingFlags.InvokeMethod, null, shortcut, null);
            } finally { if (shortcut != null) Marshal.FinalReleaseComObject(shortcut); Marshal.FinalReleaseComObject(shell); }
        }
        void Register(string target, bool desktop) {
            using (RegistryKey key = Registry.CurrentUser.CreateSubKey(RegistryKey)) {
                key.SetValue("DisplayName", Game); key.SetValue("DisplayVersion", BuildInfo.Version);
                key.SetValue("Publisher", "FNaF Redone contributors"); key.SetValue("InstallLocation", target);
                key.SetValue("DisplayIcon", Path.Combine(target, "FNaF Redone.exe"));
                key.SetValue("UninstallString", "\"" + Path.Combine(Maintenance(target), "uninstall.exe") + "\"");
                key.SetValue("URLInfoAbout", "https://github.com/MjonirMade/fnaf-redone");
                key.SetValue("NoModify", 1); key.SetValue("NoRepair", 1);
            }
            Shortcut(Path.Combine(StartMenu, Game + ".lnk"), Path.Combine(target, Game + ".exe"));
            Shortcut(Path.Combine(StartMenu, "Uninstall FNaF Redone.lnk"), Path.Combine(Maintenance(target), "uninstall.exe"));
            if (desktop) Shortcut(Path.Combine(Desktop, Game + ".lnk"), Path.Combine(target, Game + ".exe"));
        }
        public void Uninstall(string target) {
            target = Full(target); ValidateLocation(target); CheckRecord(target);
            string maintenance = Maintenance(target);
            var files = new HashSet<string>(StringComparer.OrdinalIgnoreCase);
            foreach (string relative in File.ReadAllLines(Path.Combine(maintenance, "files.txt"))) {
                string file = Full(Path.Combine(target, relative));
                if (!Within(file, target) || Same(file, target)) throw new IOException("Invalid uninstall record.");
                files.Add(file);
            }
            // Updated runtime/app contents may differ from the original release manifest.
            foreach (string managed in new[] { "app", "runtime" }) {
                string folder = Path.Combine(target, managed);
                if (Directory.Exists(folder)) Collect(folder, files);
            }
            // Check every file before deleting anything: running Java or EXE files must be closed.
            foreach (string file in files.Where(File.Exists)) {
                NoLinks(file);
                using (FileStream probe = new FileStream(file, FileMode.Open, FileAccess.Read, FileShare.None)) { }
            }
            foreach (string file in files.Where(File.Exists)) { File.SetAttributes(file, FileAttributes.Normal); File.Delete(file); }
            var dirs = files.Select(Path.GetDirectoryName).Distinct().OrderByDescending(p => p.Length).ToList();
            foreach (string dir in dirs) RemoveEmptyParents(dir, target);
            if (Directory.Exists(target) && !Directory.EnumerateFileSystemEntries(target).Any()) Directory.Delete(target);
            using (RegistryKey key = Registry.CurrentUser.OpenSubKey(RegistryKey)) {
                if (key != null && Same(Convert.ToString(key.GetValue("InstallLocation")), target)) Registry.CurrentUser.DeleteSubKeyTree(RegistryKey, false);
            }
            DeleteShortcut(Path.Combine(StartMenu, Game + ".lnk"), Path.Combine(target, Game + ".exe"));
            DeleteShortcut(Path.Combine(StartMenu, "Uninstall FNaF Redone.lnk"), Path.Combine(maintenance, "uninstall.exe"));
            DeleteShortcut(Path.Combine(Desktop, Game + ".lnk"), Path.Combine(target, Game + ".exe"));
            if (Directory.Exists(StartMenu) && !Directory.EnumerateFileSystemEntries(StartMenu).Any()) Directory.Delete(StartMenu);
            // These are exact maintenance files only; user saves and recovery folders are untouched.
            File.Delete(Path.Combine(maintenance, "files.txt")); File.Delete(Path.Combine(maintenance, "installation.txt"));
        }
        static void Collect(string folder, HashSet<string> files) {
            NoLinks(folder);
            foreach (string path in Directory.EnumerateFileSystemEntries(folder)) {
                NoLinks(path); if (Directory.Exists(path)) Collect(path, files); else files.Add(path);
            }
        }
        static void RemoveEmptyParents(string path, string root) {
            while (Within(path, root) && !Same(path, root)) {
                if (Directory.Exists(path) && !Directory.EnumerateFileSystemEntries(path).Any()) Directory.Delete(path);
                path = Path.GetDirectoryName(path);
            }
        }
        static void DeleteShortcut(string path, string target) {
            if (!File.Exists(path)) return;
            Type type = Type.GetTypeFromProgID("WScript.Shell"); object shell = Activator.CreateInstance(type), link = null;
            try {
                link = type.InvokeMember("CreateShortcut", BindingFlags.InvokeMethod, null, shell, new object[] { path });
                string actual = Convert.ToString(link.GetType().InvokeMember("TargetPath", BindingFlags.GetProperty, null, link, null));
                if (Same(actual, target)) File.Delete(path);
            } finally { if (link != null) Marshal.FinalReleaseComObject(link); Marshal.FinalReleaseComObject(shell); }
        }
    }

    public sealed class InstallerWindow : Form {
        readonly TextBox destination = new TextBox(); readonly CheckBox desktop = new CheckBox();
        readonly Button install = new Button(), browse = new Button(); readonly ProgressBar progress = new ProgressBar();
        readonly Label status = new Label(); bool busy, finished; string installedPath;
        public InstallerWindow() {
            Text = "FreddyFiles"; ClientSize = new Size(620, 440); FormBorderStyle = FormBorderStyle.FixedDialog;
            MaximizeBox = false; StartPosition = FormStartPosition.CenterScreen; AutoScaleMode = AutoScaleMode.Dpi;
            BackColor = Color.FromArgb(25, 26, 28); ForeColor = Color.Gainsboro; Font = new Font("Segoe UI", 10);
            Label title = Label("FreddyFiles", 28, 24, 560, 46); title.Font = new Font("Segoe UI", 26, FontStyle.Bold);
            Label("FNaF Redone  " + BuildInfo.Version + "  |  Windows x64", 30, 78, 560, 28);
            Label("Install the game and its runtime. No Java setup required.\nYour own Steam copy of Five Nights at Freddy's is required to play.", 30, 118, 560, 52);
            Label("Install folder", 30, 190, 540, 22);
            destination.SetBounds(30, 218, 456, 28); destination.Text = InstallEngine.DefaultTarget(); Controls.Add(destination);
            browse.Text = "Browse..."; browse.SetBounds(496, 216, 94, 31); Controls.Add(browse);
            browse.Click += delegate { using (var picker = new FolderBrowserDialog()) {
                picker.Description = "Choose a dedicated game folder, or your existing FNaF Redone folder.";
                if (picker.ShowDialog(this) == DialogResult.OK) destination.Text = picker.SelectedPath;
            } };
            desktop.Text = "Create a desktop shortcut"; desktop.Checked = true; desktop.SetBounds(30, 266, 420, 28); Controls.Add(desktop);
            Label("Saves and imported Steam files stay separate and are kept on uninstall.", 30, 307, 560, 24).Font = new Font("Segoe UI", 9);
            progress.SetBounds(30, 350, 560, 8); Controls.Add(progress);
            status.SetBounds(30, 371, 400, 48); status.Text = "A Start menu shortcut is included."; Controls.Add(status);
            install.SetBounds(448, 376, 142, 36); install.Text = "Install"; install.BackColor = Color.FromArgb(224, 204, 155); install.ForeColor = Color.Black;
            Controls.Add(install); AcceptButton = install;
            install.Click += async delegate {
                if (finished) { Process.Start(new ProcessStartInfo(Path.Combine(installedPath, InstallEngine.Game + ".exe")) { UseShellExecute = true, WorkingDirectory = installedPath }); Close(); return; }
                try {
                    string target = InstallEngine.Full(destination.Text); bool shortcut = desktop.Checked;
                    InstallEngine.ValidateTarget(target);
                    busy = true; install.Enabled = browse.Enabled = destination.Enabled = desktop.Enabled = false;
                    string note = await Task.Run(() => new InstallEngine().Install(target, shortcut, (value, message) => {
                        BeginInvoke((Action)(() => { progress.Value = value; status.Text = message; }));
                    }));
                    installedPath = target; finished = true; install.Text = "Play"; status.Text = "Installed. Add FNaF Redone.exe to WinHanced once.";
                    if (note.Length > 0) MessageBox.Show(this, note, "FreddyFiles", MessageBoxButtons.OK, MessageBoxIcon.Information);
                } catch (Exception ex) {
                    status.Text = "Installation did not finish.";
                    MessageBox.Show(this, ex.Message + "\n\nClose FNaF Redone if it is running, then try again.", "FreddyFiles", MessageBoxButtons.OK, MessageBoxIcon.Error);
                    browse.Enabled = destination.Enabled = desktop.Enabled = true;
                } finally { busy = false; install.Enabled = true; }
            };
            FormClosing += delegate(object sender, FormClosingEventArgs args) { if (busy) args.Cancel = true; };
        }
        Label Label(string text, int x, int y, int width, int height) {
            var label = new Label { Text = text }; label.SetBounds(x, y, width, height); Controls.Add(label); return label;
        }
    }
    static class Program {
        [STAThread] static void Main() {
            Application.EnableVisualStyles(); Application.SetCompatibleTextRenderingDefault(false);
#if UNINSTALL
            try {
                string maintenance = Path.GetDirectoryName(Assembly.GetExecutingAssembly().Location);
                string target = File.ReadAllText(Path.Combine(maintenance, "installation.txt")).Trim();
                if (!String.Equals(InstallEngine.Full(maintenance), InstallEngine.Maintenance(InstallEngine.Full(target)), StringComparison.OrdinalIgnoreCase)) throw new IOException("Invalid installation record.");
                if (MessageBox.Show("Uninstall FNaF Redone?\n\nYour saves and imported Steam files will be kept.", "FreddyFiles", MessageBoxButtons.YesNo, MessageBoxIcon.Question) != DialogResult.Yes) return;
                new InstallEngine().Uninstall(target);
                MessageBox.Show("FNaF Redone was uninstalled. Your saves and imported Steam files were kept.\n\nThe small maintenance folder can now be deleted:\n" + maintenance, "FreddyFiles");
            } catch (Exception ex) { MessageBox.Show(ex.Message + "\n\nClose the game before uninstalling.", "FreddyFiles", MessageBoxButtons.OK, MessageBoxIcon.Error); }
#else
            Application.Run(new InstallerWindow());
#endif
        }
    }
}
