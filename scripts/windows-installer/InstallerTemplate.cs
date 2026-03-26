using System;
using System.Diagnostics;
using System.IO;
using System.IO.Compression;
using System.Reflection;
using System.Text;
using System.Windows.Forms;
using Microsoft.Win32;

internal static class Program
{
    private const string AppName = "__APP_NAME__";
    private const string AppVersion = "__APP_VERSION__";
    private const string Publisher = "__PUBLISHER__";
    private const string MainExeName = "__MAIN_EXE__";
    private const string UninstallerExeName = "__UNINSTALLER_EXE__";
    private const string AppId = "__APP_ID__";
    private const string PayloadResourceName = "EstoqueTIInstaller.Payload.zip";

    private static bool _silent;
    private static bool _noShortcuts;
    private static bool _noRegistry;
    private static bool _noLaunch;
    private static bool _uninstall;
    private static string _installDir;
    private static string _logPath;

    [STAThread]
    private static int Main(string[] args)
    {
        try
        {
            ParseArguments(args);
            Log("Installer started.");

            if (_uninstall)
            {
                Uninstall();
            }
            else
            {
                Install();
            }

            return 0;
        }
        catch (Exception ex)
        {
            Log("ERROR: " + ex);
            if (_silent)
            {
                return 1;
            }

            MessageBox.Show(ex.Message, AppName, MessageBoxButtons.OK, MessageBoxIcon.Error);
            return 1;
        }
    }

    private static void ParseArguments(string[] args)
    {
        _installDir = GetDefaultInstallDirectory();

        foreach (var arg in args)
        {
            if (arg.Equals("/silent", StringComparison.OrdinalIgnoreCase))
            {
                _silent = true;
                continue;
            }

            if (arg.Equals("/no-shortcuts", StringComparison.OrdinalIgnoreCase))
            {
                _noShortcuts = true;
                continue;
            }

            if (arg.Equals("/no-registry", StringComparison.OrdinalIgnoreCase))
            {
                _noRegistry = true;
                continue;
            }

            if (arg.Equals("/no-launch", StringComparison.OrdinalIgnoreCase))
            {
                _noLaunch = true;
                continue;
            }

            if (arg.Equals("/uninstall", StringComparison.OrdinalIgnoreCase))
            {
                _uninstall = true;
                continue;
            }

            if (arg.StartsWith("/installDir=", StringComparison.OrdinalIgnoreCase))
            {
                _installDir = TrimQuotes(arg.Substring("/installDir=".Length));
                continue;
            }

            if (arg.StartsWith("/log=", StringComparison.OrdinalIgnoreCase))
            {
                _logPath = TrimQuotes(arg.Substring("/log=".Length));
            }
        }

        if (_uninstall && string.IsNullOrWhiteSpace(_installDir))
        {
            _installDir = Path.GetDirectoryName(Assembly.GetExecutingAssembly().Location);
        }
    }

    private static void Install()
    {
        var exePath = Assembly.GetExecutingAssembly().Location;
        var tempRoot = Path.Combine(Path.GetTempPath(), AppId + "-setup-" + Guid.NewGuid().ToString("N"));
        var extractedRoot = Path.Combine(tempRoot, "payload");

        try
        {
            Log("Install directory: " + _installDir);
            EnsureApplicationIsNotRunning(Path.Combine(_installDir, MainExeName));

            if (Directory.Exists(_installDir))
            {
                Log("Removing previous installation.");
                Directory.Delete(_installDir, true);
            }

            Directory.CreateDirectory(extractedRoot);
            ExtractPayload(extractedRoot);
            CopyDirectory(extractedRoot, _installDir);

            var installedExe = Path.Combine(_installDir, MainExeName);
            if (!File.Exists(installedExe))
            {
                throw new InvalidOperationException("Executavel principal nao encontrado apos a instalacao.");
            }

            var installedUninstaller = Path.Combine(_installDir, UninstallerExeName);
            File.Copy(exePath, installedUninstaller, true);

            if (!_noShortcuts)
            {
                CreateShortcuts(installedExe, installedUninstaller);
            }

            if (!_noRegistry)
            {
                RegisterUninstaller(installedExe, installedUninstaller);
            }

            Log("Installation completed successfully.");
            if (!_noLaunch)
            {
                Process.Start(new ProcessStartInfo
                {
                    FileName = installedExe,
                    WorkingDirectory = _installDir,
                    UseShellExecute = true
                });
            }

            if (!_silent)
            {
                MessageBox.Show("Instalacao concluida com sucesso.", AppName, MessageBoxButtons.OK, MessageBoxIcon.Information);
            }
        }
        finally
        {
            TryDeleteDirectory(tempRoot);
        }
    }

    private static void Uninstall()
    {
        var exePath = Assembly.GetExecutingAssembly().Location;
        if (string.IsNullOrWhiteSpace(_installDir))
        {
            _installDir = Path.GetDirectoryName(exePath);
        }

        Log("Uninstall directory: " + _installDir);
        EnsureApplicationIsNotRunning(Path.Combine(_installDir, MainExeName));
        RemoveShortcuts();
        RemoveRegistryEntry();

        var command = string.Format(
            "/c ping 127.0.0.1 -n 2 > nul & rmdir /s /q \"{0}\"",
            _installDir.Replace("\"", "\"\"")
        );

        Process.Start(new ProcessStartInfo
        {
            FileName = "cmd.exe",
            Arguments = command,
            CreateNoWindow = true,
            UseShellExecute = false,
            WindowStyle = ProcessWindowStyle.Hidden
        });

        Log("Uninstall scheduled.");
        if (!_silent)
        {
            MessageBox.Show("Desinstalacao iniciada. A pasta sera removida em seguida.", AppName, MessageBoxButtons.OK, MessageBoxIcon.Information);
        }
    }

    private static void ExtractPayload(string destinationDirectory)
    {
        var assembly = Assembly.GetExecutingAssembly();
        using (var stream = assembly.GetManifestResourceStream(PayloadResourceName))
        {
            if (stream == null)
            {
                throw new InvalidOperationException("Payload do instalador nao encontrado.");
            }

            using (var archive = new ZipArchive(stream, ZipArchiveMode.Read))
            {
                archive.ExtractToDirectory(destinationDirectory);
            }
        }
    }

    private static void CopyDirectory(string sourceDirectory, string destinationDirectory)
    {
        foreach (var directory in Directory.GetDirectories(sourceDirectory, "*", SearchOption.AllDirectories))
        {
            var relative = directory.Substring(sourceDirectory.Length).TrimStart(Path.DirectorySeparatorChar);
            Directory.CreateDirectory(Path.Combine(destinationDirectory, relative));
        }

        foreach (var file in Directory.GetFiles(sourceDirectory, "*", SearchOption.AllDirectories))
        {
            var relative = file.Substring(sourceDirectory.Length).TrimStart(Path.DirectorySeparatorChar);
            var destination = Path.Combine(destinationDirectory, relative);
            var destinationParent = Path.GetDirectoryName(destination);
            if (!string.IsNullOrEmpty(destinationParent))
            {
                Directory.CreateDirectory(destinationParent);
            }

            File.Copy(file, destination, true);
        }
    }

    private static void CreateShortcuts(string installedExe, string installedUninstaller)
    {
        var startMenuDirectory = Path.Combine(
            Environment.GetFolderPath(Environment.SpecialFolder.Programs),
            AppName
        );
        Directory.CreateDirectory(startMenuDirectory);

        CreateShortcut(Path.Combine(startMenuDirectory, AppName + ".lnk"), installedExe, _installDir, AppName);
        CreateShortcut(Path.Combine(startMenuDirectory, "Desinstalar " + AppName + ".lnk"), installedUninstaller, _installDir, "Desinstalar " + AppName);
        CreateShortcut(Path.Combine(Environment.GetFolderPath(Environment.SpecialFolder.DesktopDirectory), AppName + ".lnk"), installedExe, _installDir, AppName);
    }

    private static void RemoveShortcuts()
    {
        TryDeleteFile(Path.Combine(Environment.GetFolderPath(Environment.SpecialFolder.DesktopDirectory), AppName + ".lnk"));

        var startMenuDirectory = Path.Combine(Environment.GetFolderPath(Environment.SpecialFolder.Programs), AppName);
        TryDeleteFile(Path.Combine(startMenuDirectory, AppName + ".lnk"));
        TryDeleteFile(Path.Combine(startMenuDirectory, "Desinstalar " + AppName + ".lnk"));
        TryDeleteDirectory(startMenuDirectory);
    }

    private static void RegisterUninstaller(string installedExe, string installedUninstaller)
    {
        using (var key = Registry.CurrentUser.CreateSubKey(@"Software\Microsoft\Windows\CurrentVersion\Uninstall\" + AppId))
        {
            if (key == null)
            {
                throw new InvalidOperationException("Nao foi possivel registrar o desinstalador.");
            }

            key.SetValue("DisplayName", AppName);
            key.SetValue("DisplayVersion", AppVersion);
            key.SetValue("Publisher", Publisher);
            key.SetValue("InstallLocation", _installDir);
            key.SetValue("DisplayIcon", installedExe);
            key.SetValue("UninstallString", string.Format("\"{0}\" /uninstall", installedUninstaller));
            key.SetValue("NoModify", 1, RegistryValueKind.DWord);
            key.SetValue("NoRepair", 1, RegistryValueKind.DWord);
        }
    }

    private static void RemoveRegistryEntry()
    {
        try
        {
            Registry.CurrentUser.DeleteSubKeyTree(@"Software\Microsoft\Windows\CurrentVersion\Uninstall\" + AppId, false);
        }
        catch
        {
        }
    }

    private static void CreateShortcut(string shortcutPath, string targetPath, string workingDirectory, string description)
    {
        var shellType = Type.GetTypeFromProgID("WScript.Shell");
        if (shellType == null)
        {
            throw new InvalidOperationException("WScript.Shell nao esta disponivel para criar atalhos.");
        }

        var shell = Activator.CreateInstance(shellType);
        try
        {
            var shortcut = shellType.InvokeMember("CreateShortcut", BindingFlags.InvokeMethod, null, shell, new object[] { shortcutPath });
            var shortcutType = shortcut.GetType();
            shortcutType.InvokeMember("TargetPath", BindingFlags.SetProperty, null, shortcut, new object[] { targetPath });
            shortcutType.InvokeMember("WorkingDirectory", BindingFlags.SetProperty, null, shortcut, new object[] { workingDirectory });
            shortcutType.InvokeMember("Description", BindingFlags.SetProperty, null, shortcut, new object[] { description });
            shortcutType.InvokeMember("IconLocation", BindingFlags.SetProperty, null, shortcut, new object[] { targetPath + ",0" });
            shortcutType.InvokeMember("Save", BindingFlags.InvokeMethod, null, shortcut, null);
        }
        finally
        {
            if (shell != null)
            {
                System.Runtime.InteropServices.Marshal.FinalReleaseComObject(shell);
            }
        }
    }

    private static void EnsureApplicationIsNotRunning(string targetExecutablePath)
    {
        var processName = Path.GetFileNameWithoutExtension(MainExeName);
        var expectedPath = Path.GetFullPath(targetExecutablePath);

        foreach (var process in Process.GetProcessesByName(processName))
        {
            try
            {
                string processPath;
                try
                {
                    processPath = process.MainModule == null ? null : process.MainModule.FileName;
                }
                catch
                {
                    continue;
                }

                if (string.IsNullOrWhiteSpace(processPath))
                {
                    continue;
                }

                if (string.Equals(Path.GetFullPath(processPath), expectedPath, StringComparison.OrdinalIgnoreCase))
                {
                    throw new InvalidOperationException("Feche o aplicativo antes de instalar ou desinstalar.");
                }
            }
            finally
            {
                process.Dispose();
            }
        }
    }

    private static string GetDefaultInstallDirectory()
    {
        return Path.Combine(
            Environment.GetFolderPath(Environment.SpecialFolder.LocalApplicationData),
            "Programs",
            AppName
        );
    }

    private static string TrimQuotes(string value)
    {
        if (string.IsNullOrWhiteSpace(value))
        {
            return value;
        }

        return value.Trim().Trim('"');
    }

    private static void TryDeleteDirectory(string path)
    {
        try
        {
            if (!string.IsNullOrWhiteSpace(path) && Directory.Exists(path))
            {
                Directory.Delete(path, true);
            }
        }
        catch
        {
        }
    }

    private static void TryDeleteFile(string path)
    {
        try
        {
            if (!string.IsNullOrWhiteSpace(path) && File.Exists(path))
            {
                File.Delete(path);
            }
        }
        catch
        {
        }
    }

    private static void Log(string message)
    {
        if (string.IsNullOrWhiteSpace(_logPath))
        {
            return;
        }

        var logDirectory = Path.GetDirectoryName(_logPath);
        if (!string.IsNullOrWhiteSpace(logDirectory))
        {
            Directory.CreateDirectory(logDirectory);
        }

        File.AppendAllText(_logPath, DateTime.Now.ToString("yyyy-MM-dd HH:mm:ss") + " " + message + Environment.NewLine, Encoding.UTF8);
    }
}

