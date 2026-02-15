package com.packer;

import java.io.*;
import java.nio.file.*;
import java.util.zip.*;
import javax.xml.parsers.*;
import javax.xml.transform.*;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import org.w3c.dom.*;

public class SimplePacker {

    private static final byte KEY = 0x42;
    private static final String TEMP = "tmp_pack";

    public static void main(String[] args) throws Exception {
        if (args.length != 2) {
            System.out.println("Usage: java -jar SimplePacker.jar <input.apk> <output.apk>");
            System.exit(1);
        }

        String input = args[0];
        String output = args[1];

        System.out.println("[*] Packing: " + input);

        // Extract
        unzip(input, TEMP);

        // Encrypt original DEX
        byte[] originalDex = Files.readAllBytes(Paths.get(TEMP + "/classes.dex"));
        byte[] encrypted = xor(originalDex);

        // Save to assets
        new File(TEMP + "/assets").mkdirs();
        Files.write(Paths.get(TEMP + "/assets/p.dat"), encrypted);

        // Replace with stub
        Files.copy(Paths.get("stub.dex"), Paths.get(TEMP + "/classes.dex"),
                   StandardCopyOption.REPLACE_EXISTING);

        // Modify manifest
        modifyManifest(TEMP + "/AndroidManifest.xml");

        // Repackage
        zip(TEMP, output);

        // Cleanup
        deleteDir(new File(TEMP));

        System.out.println("[+] Created: " + output);
        System.out.println("[!] Sign with: apksigner sign --ks ~/.android/debug.keystore " + output);
    }

    static byte[] xor(byte[] data) {
        byte[] result = new byte[data.length];
        for (int i = 0; i < data.length; i++) {
            result[i] = (byte)(data[i] ^ KEY);
        }
        return result;
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
                zos.putNextEntry(new ZipEntry(path));
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

    static void modifyManifest(String path) throws Exception {
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        Document doc = dbf.newDocumentBuilder().parse(new File(path));
        Element app = (Element) doc.getElementsByTagName("application").item(0);
        app.setAttribute("android:name", "com.packer.StubApplication");

        TransformerFactory tf = TransformerFactory.newInstance();
        Transformer t = tf.newTransformer();
        t.transform(new DOMSource(doc), new StreamResult(new File(path)));
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
