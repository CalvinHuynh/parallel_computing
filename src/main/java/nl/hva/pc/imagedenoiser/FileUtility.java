package nl.hva.pc.imagedenoiser;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class FileUtility {

    /**
     * Creates a folder at the root of this directory
     * 
     * @param folderName name of the folder
     */
    public void CreateFolder(String folderName) {
        File f = new File(folderName);
        try {
            if (f.mkdir())
                System.out.println("Successfully created directory " + folderName);
            else
                System.out.println("Directory " + folderName + " already exist.");
        } catch (Exception e) {
            System.out.println(e);
        }
    }

    /**
     * Function to unzip a zip file to a destination
     * Code is taken from the following source:
     * https://howtodoinjava.com/java/io/unzip-file-with-subdirectories/
     * @param zipPath name and location of the zip file
     * @param destination destination of the unzipped content
     */
    public void Unzip(String zipPath, String destination) {
        if (destination == null || destination.trim().isEmpty()) {
            destination = "resources/";
        }
        try (ZipFile file = new ZipFile(zipPath)) {
            FileSystem fileSystem = FileSystems.getDefault();
            // Get file entries
            Enumeration<? extends ZipEntry> entries = file.entries();

            // We will unzip files in this folder
            String uncompressedDirectory = destination;
            try {
                Files.createDirectory(fileSystem.getPath(uncompressedDirectory));
            } catch (Exception e) {
                System.out.println(e);
            }

            // Iterate over entries
            while (entries.hasMoreElements()) {
                ZipEntry entry = entries.nextElement();
                // If directory then create a new directory in uncompressed folder
                if (entry.isDirectory()) {
                    System.out.println("Creating Directory: " + uncompressedDirectory + entry.getName());
                    Files.createDirectories(fileSystem.getPath(uncompressedDirectory + entry.getName()));
                }
                // Else create the file
                else {
                    InputStream is = file.getInputStream(entry);
                    BufferedInputStream bis = new BufferedInputStream(is);
                    String uncompressedFileName = uncompressedDirectory + entry.getName();
                    Path uncompressedFilePath = fileSystem.getPath(uncompressedFileName);
                    try {
                        Files.createFile(uncompressedFilePath);
                        FileOutputStream fileOutput = new FileOutputStream(uncompressedFileName);
                        while (bis.available() > 0) {
                            fileOutput.write(bis.read());
                        }
                        fileOutput.close();
                        System.out.println("Written: " + entry.getName());
                    } catch (Exception e) {
                        System.out.println(e);
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
