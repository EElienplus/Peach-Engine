package net.meowsers.Peach.Utils;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class SlangManager {
    public static final String SLANG_VERSION = "2026.17";
    private static String cachedExecutablePath = null;

    public static synchronized String getSlangcPath() {
        if (cachedExecutablePath != null && new File(cachedExecutablePath).exists()) {
            return cachedExecutablePath;
        }

        // Check System property
        String sysProp = System.getProperty("peach.slangc");
        if (sysProp == null) sysProp = System.getProperty("slangc.path");
        if (isValidExecutable(sysProp)) {
            cachedExecutablePath = new File(sysProp).getAbsolutePath();
            return cachedExecutablePath;
        }

        // Check Environment variables
        String envSlangc = System.getenv("SLANGC_PATH");
        if (isValidExecutable(envSlangc)) {
            cachedExecutablePath = new File(envSlangc).getAbsolutePath();
            return cachedExecutablePath;
        }

        String envSlangDir = System.getenv("SLANG_DIR");
        if (envSlangDir != null) {
            File binSlangc = new File(envSlangDir, isWindows() ? "bin/slangc.exe" : "bin/slangc");
            if (isValidExecutable(binSlangc.getAbsolutePath())) {
                cachedExecutablePath = binSlangc.getAbsolutePath();
                return cachedExecutablePath;
            }
        }

        // Check Project / Local directory
        String[] localPaths = {
                "tools/slang/bin/" + (isWindows() ? "slangc.exe" : "slangc"),
                ".peach/slang/bin/" + (isWindows() ? "slangc.exe" : "slangc"),
                "bin/" + (isWindows() ? "slangc.exe" : "slangc")
        };
        for (String lp : localPaths) {
            if (isValidExecutable(lp)) {
                cachedExecutablePath = new File(lp).getAbsolutePath();
                return cachedExecutablePath;
            }
        }

        // Check User Home Cache Directory (~/.peach/slang/v<version>/bin/slangc)
        File userHomeCache = new File(getUserSlangDir(), isWindows() ? "bin/slangc.exe" : "bin/slangc");
        if (isValidExecutable(userHomeCache.getAbsolutePath())) {
            cachedExecutablePath = userHomeCache.getAbsolutePath();
            return cachedExecutablePath;
        }

        // Check Standard System Installation Paths
        String[] systemPaths;
        if (isWindows()) {
            String programFiles = System.getenv("ProgramFiles");
            String localAppData = System.getenv("LOCALAPPDATA");
            systemPaths = new String[]{
                    (programFiles != null ? programFiles : "C:\\Program Files") + "\\Slang\\bin\\slangc.exe",
                    "C:\\Program Files (x86)\\Slang\\bin\\slangc.exe",
                    (localAppData != null ? localAppData : "") + "\\Programs\\Slang\\bin\\slangc.exe"
            };
        } else if (isMac()) {
            systemPaths = new String[]{
                    "/opt/homebrew/bin/slangc",
                    "/usr/local/bin/slangc",
                    "/usr/bin/slangc",
                    System.getProperty("user.home") + "/.local/bin/slangc"
            };
        } else {
            // Linux
            systemPaths = new String[]{
                    "/usr/local/bin/slangc",
                    "/usr/bin/slangc",
                    System.getProperty("user.home") + "/.local/bin/slangc"
            };
        }

        for (String sp : systemPaths) {
            if (isValidExecutable(sp)) {
                cachedExecutablePath = new File(sp).getAbsolutePath();
                return cachedExecutablePath;
            }
        }

        // Check PATH
        String pathEnv = System.getenv("PATH");
        if (pathEnv != null) {
            String[] dirs = pathEnv.split(PatternQuote(File.pathSeparator));
            String exeName = isWindows() ? "slangc.exe" : "slangc";
            for (String dir : dirs) {
                File f = new File(dir, exeName);
                if (isValidExecutable(f.getAbsolutePath())) {
                    cachedExecutablePath = f.getAbsolutePath();
                    return cachedExecutablePath;
                }
            }
        }

        // Auto-download and setup Slang release for current platform
        try {
            File downloadedSlangc = downloadAndInstallSlang();
            if (downloadedSlangc != null && isValidExecutable(downloadedSlangc.getAbsolutePath())) {
                cachedExecutablePath = downloadedSlangc.getAbsolutePath();
                return cachedExecutablePath;
            }
        } catch (Exception e) {
            System.err.println(ConsoleColors.YELLOW + "[Peach] Automatic download of Slang compiler failed: " + e.getMessage() + ConsoleColors.RESET);
        }

        return isWindows() ? "slangc.exe" : "slangc";
    }

    private static String PatternQuote(String s) {
        return java.util.regex.Pattern.quote(s);
    }

    public static File getUserSlangDir() {
        String userHome = System.getProperty("user.home");
        return new File(userHome, ".peach" + File.separator + "slang" + File.separator + "v" + SLANG_VERSION);
    }

    public static File downloadAndInstallSlang() throws IOException {
        String osName = getPlatformOS();
        String arch = getPlatformArch();
        String archiveName = "slang-" + SLANG_VERSION + "-" + osName + "-" + arch + ".zip";
        String downloadUrl = "https://github.com/shader-slang/slang/releases/download/v" + SLANG_VERSION + "/" + archiveName;

        File targetDir = getUserSlangDir();
        File slangcExe = new File(targetDir, isWindows() ? "bin/slangc.exe" : "bin/slangc");

        if (isValidExecutable(slangcExe.getAbsolutePath())) {
            return slangcExe;
        }

        System.out.println(ConsoleColors.CYAN_BOLD + "[Peach] Slang compiler not found on system. Downloading portable Slang v" + SLANG_VERSION + " for " + osName + "-" + arch + "..." + ConsoleColors.RESET);

        targetDir.mkdirs();
        File tempZip = File.createTempFile("slang-" + SLANG_VERSION, ".zip");
        try {
            downloadFile(downloadUrl, tempZip);
            System.out.println(ConsoleColors.CYAN + "[Peach] Extracting Slang compiler to " + targetDir.getAbsolutePath() + "..." + ConsoleColors.RESET);
            extractZip(tempZip, targetDir);

            if (!isWindows()) {
                File binDir = new File(targetDir, "bin");
                if (binDir.exists() && binDir.isDirectory()) {
                    File[] files = binDir.listFiles();
                    if (files != null) {
                        for (File f : files) {
                            f.setExecutable(true, false);
                            f.setReadable(true, false);
                        }
                    }
                }
            }

            if (isValidExecutable(slangcExe.getAbsolutePath())) {
                System.out.println(ConsoleColors.GREEN_BOLD + "[Peach] Portable Slang compiler successfully installed to " + slangcExe.getAbsolutePath() + ConsoleColors.RESET);
                return slangcExe;
            } else {
                throw new PeachException("Downloaded Slang archive did not contain executable at " + slangcExe.getAbsolutePath());
            }
        } finally {
            if (tempZip.exists()) {
                tempZip.delete();
            }
        }
    }

    private static void downloadFile(String urlString, File destination) throws IOException {
        URL url = URI.create(urlString).toURL();
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setInstanceFollowRedirects(true);
        connection.setRequestProperty("User-Agent", "Peach-Engine/1.0");
        connection.setConnectTimeout(15000);
        connection.setReadTimeout(30000);

        int status = connection.getResponseCode();
        // Handle HTTP 301 / 302 redirects
        if (status == HttpURLConnection.HTTP_MOVED_TEMP || status == HttpURLConnection.HTTP_MOVED_PERM || status == 307 || status == 308) {
            String newUrl = connection.getHeaderField("Location");
            connection = (HttpURLConnection) URI.create(newUrl).toURL().openConnection();
            connection.setRequestProperty("User-Agent", "Peach-Engine/1.0");
            connection.setConnectTimeout(15000);
            connection.setReadTimeout(30000);
            status = connection.getResponseCode();
        }

        if (status != HttpURLConnection.HTTP_OK) {
            throw new IOException("Failed to download from " + urlString + " (HTTP " + status + " " + connection.getResponseMessage() + ")");
        }

        try (InputStream in = new BufferedInputStream(connection.getInputStream());
             OutputStream out = new BufferedOutputStream(new FileOutputStream(destination))) {
            byte[] buffer = new byte[16384];
            int count;
            while ((count = in.read(buffer)) != -1) {
                out.write(buffer, 0, count);
            }
            out.flush();
        }
    }

    private static void extractZip(File zipFile, File destDir) throws IOException {
        byte[] buffer = new byte[16384];
        try (ZipInputStream zis = new ZipInputStream(new BufferedInputStream(new FileInputStream(zipFile)))) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                File targetFile = new File(destDir, entry.getName());

                // Prevent zip-slip
                String canonicalDest = destDir.getCanonicalPath();
                String canonicalTarget = targetFile.getCanonicalPath();
                if (!canonicalTarget.startsWith(canonicalDest + File.separator) && !canonicalTarget.equals(canonicalDest)) {
                    throw new IOException("Zip entry attempted directory traversal: " + entry.getName());
                }

                if (entry.isDirectory()) {
                    targetFile.mkdirs();
                } else {
                    File parent = targetFile.getParentFile();
                    if (parent != null && !parent.exists()) {
                        parent.mkdirs();
                    }
                    try (BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(targetFile))) {
                        int len;
                        while ((len = zis.read(buffer)) > 0) {
                            bos.write(buffer, 0, len);
                        }
                        bos.flush();
                    }
                    if (!isWindows() && (entry.getName().startsWith("bin/") || entry.getName().endsWith(".dylib") || entry.getName().endsWith(".so"))) {
                        targetFile.setExecutable(true, false);
                    }
                }
                zis.closeEntry();
            }
        }
    }

    public static boolean isValidExecutable(String path) {
        if (path == null || path.trim().isEmpty()) return false;
        File f = new File(path);
        return f.exists() && f.isFile() && (isWindows() || f.canExecute());
    }

    public static boolean isWindows() {
        return System.getProperty("os.name").toLowerCase().contains("win");
    }

    public static boolean isMac() {
        String os = System.getProperty("os.name").toLowerCase();
        return os.contains("mac") || os.contains("darwin");
    }

    public static boolean isLinux() {
        String os = System.getProperty("os.name").toLowerCase();
        return os.contains("linux") || os.contains("unix");
    }

    public static String getPlatformOS() {
        if (isWindows()) return "windows";
        if (isMac()) return "macos";
        return "linux";
    }

    public static void main(String[] args) {
        String path = getSlangcPath();
        System.out.println(ConsoleColors.GREEN_BOLD + "[Peach] Slang compiler is ready at: " + path + ConsoleColors.RESET);
    }

    public static String getPlatformArch() {
        String arch = System.getProperty("os.arch").toLowerCase();
        if (arch.contains("aarch64") || arch.contains("arm64") || arch.contains("armv8")) {
            return "aarch64";
        }
        return "x86_64";
    }
}
