package com.packer;

import java.io.*;
import java.nio.file.*;
import java.util.zip.*;

public class SimplePacker {
   
   // const key for xoring bytes
    private static final byte KEY = 0x42;

    // temporrary directory for the decoded human readable apk and original apk (binary)
    private static final String TEMP = "tmp_pack";
    private static final String DECODED = "tmp_decode";

    public static void main(String[] args) throws Exception {
        if (args.length != 2) {
            System.out.println("Usage: java -jar SimplePacker.jar <input.apk> <output.apk>");
            System.exit(1);
        }

        String input = args[0];
        String output = args[1];


        // apk tool decodes the whole apk including the manifest, resources,
        // and bytecode which it turns into smali 
        runCommand("apktool d " + input + " -o " + DECODED + " -f");
        String manifestPath = DECODED + "/AndroidManifest.xml";
        // read the app class and the launchable activity from the manifest
        String realAppClass = readAppClass(manifestPath);
        String realEntryActivity = readLaunchableActivity(manifestPath);

        // patch the manifest to include both the stub application and launchable activity
        patchStubApplication(manifestPath);
        patchLauncherActivity(manifestPath);

        runCommand("apktool b " + DECODED + " -o tmp_rebuilt.apk");

        unzip("tmp_rebuilt.apk", TEMP);

        byte[] originalDex = Files.readAllBytes(Paths.get(TEMP + "/classes.dex"));
        byte[] encrypted = xor(originalDex);

        new File(TEMP + "/assets").mkdirs();
        Files.write(Paths.get(TEMP + "/assets/p.dat"), encrypted);
        Files.write(Paths.get(TEMP + "/assets/app.txt"), realAppClass.getBytes());
        Files.write(Paths.get(TEMP + "/assets/entry.txt"), realEntryActivity.getBytes());

        Files.copy(Paths.get("stub.dex"), Paths.get(TEMP + "/classes.dex"),
                   StandardCopyOption.REPLACE_EXISTING);

        zip(TEMP, output);

        deleteDir(new File(TEMP));
        deleteDir(new File(DECODED));
        new File("tmp_rebuilt.apk").delete();

    }

    static String readAppClass(String manifestPath) throws Exception {
        String content = new String(Files.readAllBytes(Paths.get(manifestPath)));
        int appIndex = content.indexOf("<application");
        if (appIndex == -1) throw new RuntimeException("No <application> tag found");
        int appTagEnd = content.indexOf(">", appIndex);
        if (appTagEnd == -1) throw new RuntimeException("Malformed <application> tag");
        String appTag = content.substring(appIndex, appTagEnd + 1);
        int nameIndex = appTag.indexOf("android:name=\"");
        if (nameIndex == -1) return "android.app.Application";
        int start = nameIndex + "android:name=\"".length();
        int end = appTag.indexOf("\"", start);
        return appTag.substring(start, end);
    }

    static void patchStubApplication(String manifestPath) throws Exception {
    String content = new String(Files.readAllBytes(Paths.get(manifestPath)));
    int appIndex = content.indexOf("<application");

    // looks for the application tag to find the entry point and replace it with the stub for the unpacking process
    if (appIndex == -1) throw new RuntimeException("No <application> tag found");
    int appTagEnd = content.indexOf(">", appIndex);
    if (appTagEnd == -1) throw new RuntimeException("Malformed <application> tag");

    String appTag = content.substring(appIndex, appTagEnd + 1);
    String patchedTag = appTag.replaceAll("android:name=\"[^\"]*\"", "");
    patchedTag = patchedTag.replace("<application", "<application android:name=\"com.packer.StubApplication\"");
    content = content.substring(0, appIndex) + patchedTag + content.substring(appTagEnd + 1);

    Files.write(Paths.get(manifestPath), content.getBytes());

    }


    static String readLaunchableActivity(String manifestPath) throws Exception {
        String content = new String(Files.readAllBytes(Paths.get(manifestPath)));
        int launcherIdx = content.indexOf("android.intent.category.LAUNCHER");
        if (launcherIdx == -1) throw new RuntimeException("No LAUNCHER activity found");

        int activityStart = content.lastIndexOf("<activity", launcherIdx);
        if (activityStart == -1) throw new RuntimeException("No <activity> tag found for LAUNCHER");

        int tagEnd = content.indexOf(">", activityStart);
        if (tagEnd == -1) throw new RuntimeException("Malformed <activity> tag");

        String activityTag = content.substring(activityStart, tagEnd + 1);
        int nameIndex = activityTag.indexOf("android:name=\"");
        if (nameIndex == -1) throw new RuntimeException("LAUNCHER activity missing android:name");

        int start = nameIndex + "android:name=\"".length();
        int end = activityTag.indexOf("\"", start);
        if (end == -1) throw new RuntimeException("Malformed android:name on LAUNCHER activity");
        return activityTag.substring(start, end);
    }

    static void patchLauncherActivity(String manifestPath) throws Exception {
        String content = new String(Files.readAllBytes(Paths.get(manifestPath)));

        // 1) Disable the original launcher intent-filter so only the stub shows up in the launcher.
        int launcherIdx = content.indexOf("android.intent.category.LAUNCHER");
        if (launcherIdx == -1) {
            throw new RuntimeException("No LAUNCHER activity found");
        }
        int intentFilterStart = content.lastIndexOf("<intent-filter", launcherIdx);
        int intentFilterEnd = content.indexOf("</intent-filter>", launcherIdx);
        if (intentFilterStart != -1 && intentFilterEnd != -1) {
            intentFilterEnd += "</intent-filter>".length();
            content = content.substring(0, intentFilterStart) + content.substring(intentFilterEnd);
        }

        // error checking before adding launch activity to manifest
        int appIndex = content.indexOf("<application");
        if (appIndex == -1) throw new RuntimeException("No <application> tag found");
        int appTagEnd = content.indexOf(">", appIndex);
        if (appTagEnd == -1) throw new RuntimeException("Malformed <application> tag");

        // launcher activity that gets appended to the manifest document in binary
        String stubLauncher =
            "\n        <activity android:name=\"com.packer.LaunchActivity\" android:exported=\"true\">\n" +
            "            <intent-filter>\n" +
            "                <action android:name=\"android.intent.action.MAIN\"/>\n" +
            "                <category android:name=\"android.intent.category.LAUNCHER\"/>\n" +
            "            </intent-filter>\n" +
            "        </activity>\n";

        content = content.substring(0, appTagEnd + 1) + stubLauncher + content.substring(appTagEnd + 1);

        Files.write(Paths.get(manifestPath), content.getBytes());
    }

    static void runCommand(String command) throws Exception {
        ProcessBuilder pb = new ProcessBuilder(command.split(" "));
        pb.inheritIO();
        Process process = pb.start();
        int exitCode = process.waitFor();
        if (exitCode != 0) throw new RuntimeException("Failed: " + command);
    }

    static byte[] xor(byte[] data) {
        byte[] result = new byte[data.length];
        for (int i = 0; i < data.length; i++) {
            result[i] = (byte)(data[i] ^ KEY);
        }
        return result;
    }

    static long computeCrc(File file) throws IOException {
        CRC32 crc = new CRC32();
        byte[] buffer = new byte[4096];
        try (FileInputStream fis = new FileInputStream(file)) {
            int len;
            while ((len = fis.read(buffer)) > 0) {
                crc.update(buffer, 0, len);
            }
        }
        return crc.getValue();
    }

    static void unzip(String zip, String dest) throws IOException {
        new File(dest).mkdirs();
        try (ZipInputStream zis = new ZipInputStream(new FileInputStream(zip))) {
            ZipEntry entry;
            byte[] buffer = new byte[4096];
            while ((entry = zis.getNextEntry()) != null) {
                File file = new File(dest, entry.getName());
                if (entry.isDirectory()) {
                    file.mkdirs();
                } else {
                    new File(file.getParent()).mkdirs();
                    try (FileOutputStream fos = new FileOutputStream(file)) {
                        int len;
                        while ((len = zis.read(buffer)) > 0) {
                            fos.write(buffer, 0, len);
                        }
                    }
                }
            }
        }
    }

    static void zip(String src, String dest) throws IOException {
        try (ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(dest))) {
            zipDir(new File(src), src, zos);
        }
    }

    static void zipDir(File dir, String base, ZipOutputStream zos) throws IOException {
        File[] files = dir.listFiles();
        if (files == null) return;
        byte[] buffer = new byte[4096];
        for (File file : files) {
            if (file.isDirectory()) {
                zipDir(file, base, zos);
            } else {
                String path = file.getPath().substring(base.length() + 1).replace(File.separatorChar, '/');
                ZipEntry entry = new ZipEntry(path);
                if (path.equals("resources.arsc")) {
                    entry.setMethod(ZipEntry.STORED);
                    entry.setSize(file.length());
                    entry.setCompressedSize(file.length());
                    entry.setCrc(computeCrc(file));
                }
                zos.putNextEntry(entry);
                try (FileInputStream fis = new FileInputStream(file)) {
                    int len;
                    while ((len = fis.read(buffer)) > 0) {
                        zos.write(buffer, 0, len);
                    }
                }
                zos.closeEntry();
            }
        }
    }

    static void deleteDir(File dir) {
        if (dir.exists()) {
            File[] files = dir.listFiles();
            if (files != null) {
                for (File f : files) {
                    if (f.isDirectory()) deleteDir(f);
                    else f.delete();
                }
            }
            dir.delete();
        }
    }
}