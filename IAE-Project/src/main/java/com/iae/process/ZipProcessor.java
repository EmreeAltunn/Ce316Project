package com.iae.process;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class ZipProcessor {

    public List<File> extractAll(File zipDirectory, File targetDirectory) throws IOException {
        List<File> extractedDirs = new ArrayList<>();

        if (!zipDirectory.exists() || !zipDirectory.isDirectory()) {
            throw new IOException("ZIP klasörü bulunamadı: " + zipDirectory.getAbsolutePath());
        }

        File[] zipFiles = zipDirectory.listFiles((dir, name) -> name.toLowerCase().endsWith(".zip"));

        if (zipFiles == null || zipFiles.length == 0) {
            System.out.println("Uyarı: " + zipDirectory + " klasöründe .zip dosyası bulunamadı.");
            return extractedDirs;
        }

        for (File zipFile : zipFiles) {
            try {
                File studentDir = extractSingle(zipFile, targetDirectory);
                extractedDirs.add(studentDir);
                System.out.println("✅ Başarıyla açıldı: " + zipFile.getName());
            } catch (Exception e) {
                System.err.println("❌ ZIP açılırken hata: " + zipFile.getName() + " - " + e.getMessage());
            }
        }

        return extractedDirs;
    }

    public File extractSingle(File zipFile, File targetDirectory) throws IOException {
        String studentId = getStudentIdFromZip(zipFile);
        File studentDir = new File(targetDirectory, studentId);
        studentDir.mkdirs();

        try (ZipInputStream zis = new ZipInputStream(new FileInputStream(zipFile))) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                if (entry.getName().contains("..")) {
                    System.err.println("Güvenlik uyarısı: " + entry.getName());
                    continue;
                }

                File outFile = new File(studentDir, entry.getName());

                if (entry.isDirectory()) {
                    outFile.mkdirs();
                } else {
                    outFile.getParentFile().mkdirs();
                    try (FileOutputStream fos = new FileOutputStream(outFile)) {
                        zis.transferTo(fos);
                    }
                }
            }
        }
        return studentDir;
    }

    public String getStudentIdFromZip(File zipFile) {
        String name = zipFile.getName();
        if (name.toLowerCase().endsWith(".zip")) {
            name = name.substring(0, name.length() - 4);
        }
        return name;
    }
}