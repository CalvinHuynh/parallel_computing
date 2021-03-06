package nl.hva.pc.imagedenoiser;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.FileImageOutputStream;

public class Image {

    /**
     * Inspired by
     * http://kalanir.blogspot.com/2010/02/how-to-split-image-into-chunks-java.html
     * Splits the image by half
     * 
     * @param fileInputPath  folder of the images that you want to split
     * @param fileOutputPath output folder of the splitted images
     * @param rowSize        number of cells in a row
     * @param colSize        number of cells in a column
     * @param deleteOriginal delete the original file
     * @throws Exception
     */
    public void ImageSplitter(String fileInputPath, String fileOutputPath, int rowSize, int colSize,
            boolean deleteOriginal) throws Exception {
        FileUtility fileUtility = new FileUtility();
        fileUtility.CreateFolder(fileOutputPath);
        // Traverse the directory to retrieve the images.
        try (Stream<Path> paths = Files.walk(Paths.get(fileInputPath))) {
            paths.forEach((pathToImage) -> {
                File file = new File(pathToImage.toString());
                String nameOfFile = pathToImage.toString().substring(pathToImage.toString().lastIndexOf("/") + 1);
                if (nameOfFile.substring(nameOfFile.lastIndexOf(".") + 1).toLowerCase().matches("jpg|png")) {
                    String fileName = nameOfFile.substring(nameOfFile.lastIndexOf("/") + 1).substring(0,
                            nameOfFile.lastIndexOf("."));
                    String fileExtension = nameOfFile.substring(nameOfFile.lastIndexOf(".") + 1).trim();
                    FileInputStream fis = null;
                    BufferedImage image = null;
                    try {
                        fis = new FileInputStream(file);
                        image = ImageIO.read(fis);

                        int rows = rowSize;
                        int cols = colSize;
                        int chunks = rows * cols;

                        int chunkWidth = image.getWidth() / cols; // determines the chunk width and height
                        int chunkHeight = image.getHeight() / rows;
                        int count = 0;
                        BufferedImage imgs[] = new BufferedImage[chunks]; // Image array to hold image chunks
                        for (int x = 0; x < rows; x++) {
                            for (int y = 0; y < cols; y++) {
                                // Initialize the image array with image chunks
                                imgs[count] = new BufferedImage(chunkWidth, chunkHeight, image.getType());

                                // draws the image chunk
                                Graphics2D g2draw = imgs[count++].createGraphics();
                                g2draw.drawImage(image, 0, 0, chunkWidth, chunkHeight, chunkWidth * y, chunkHeight * x,
                                        chunkWidth * y + chunkWidth, chunkHeight * x + chunkHeight, null);
                                g2draw.dispose();
                            }
                        }

                        switch (fileExtension.toLowerCase()) {
                        case "jpg":
                        case "jpeg":
                            for (int i = 0; i < imgs.length; i++) {
                                ImageWriter writer = ImageIO.getImageWritersByFormatName("jpeg").next();
                                // Using ImageWriteParam to force the system to save jpegs using the highest
                                // quality setting
                                ImageWriteParam param = writer.getDefaultWriteParam();
                                param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT); // Needed see javadoc
                                param.setCompressionQuality(1.0F); // Highest quality
                                writer.setOutput(new FileImageOutputStream(
                                        new File(fileOutputPath + "/" + fileName + "_" + i + "." + fileExtension)));
                                writer.write(null, new IIOImage(imgs[i], null, null), param);
                            }
                            break;
                        default:
                            for (int i = 0; i < imgs.length; i++) {
                                ImageIO.write(imgs[i], fileExtension,
                                        new File(fileOutputPath + "/" + fileName + "_" + i + "." + fileExtension));
                            }
                        }

                        if (deleteOriginal) {
                            file.delete();
                        }
                    } catch (Exception e) {
                        System.out.println(e);
                    }
                }
            });
        }
    }

    /**
     * Inspired by
     * http://kalanir.blogspot.com/2010/02/how-to-merge-multiple-images-into-one.html
     * Merges the images created by the ImageSplitter function
     * 
     * @param fileInputPath  path to the folder containing the splitted images
     * @param fileOutputPath output path of the merged images
     * @param rowSize        number of cells in a row
     * @param colSize        number of cells in a column
     * @param deleteOriginal delete the original file
     * @throws IOException
     */
    public void ImageMerger(String fileInputPath, String fileOutputPath, int rowSize, int colSize,
            boolean deleteOriginal) throws IOException {
        FileUtility fileUtility = new FileUtility();
        fileUtility.CreateFolder(fileOutputPath);

        List<String> pathList = Files.walk(Paths.get(fileInputPath)).filter(Files::isRegularFile)
                .map(result -> result.toString()).collect(Collectors.toList());

        for (int i = 0; i < pathList.size(); i++) {
            String fileExtension = pathList.get(i).substring(pathList.get(i).lastIndexOf(".") + 1).trim();
            // remove the _{{ number }} part of the path
            pathList.set(i, pathList.get(i).replaceAll("_\\d+." + fileExtension, "." + fileExtension));
        }

        List<String> dedupedList = pathList.stream().distinct().collect(Collectors.toList());

        dedupedList.forEach((filePath) -> {
            try {
                int rows = rowSize;
                int cols = colSize;
                int chunks = rows * cols;
                String fileExtension = "";
                String fileName = "";

                int chunkWidth, chunkHeight;
                int type;
                // fetching image files
                File[] imgFiles = new File[chunks];
                for (int i = 0; i < chunks; i++) {
                    fileExtension = filePath.substring(filePath.lastIndexOf(".") + 1).trim();
                    // Construct the file path
                    imgFiles[i] = new File(
                            filePath.substring(0, filePath.lastIndexOf('.')) + "_" + i + "." + fileExtension);
                    fileName = imgFiles[i].getName().substring(imgFiles[i].getName().lastIndexOf("/") + 1).substring(0,
                            imgFiles[i].getName().lastIndexOf("_"));
                }
                fileName = fileName + "_Merged";

                // creating a bufferd image array from image files
                BufferedImage[] buffImages = new BufferedImage[chunks];
                for (int i = 0; i < chunks; i++) {
                    buffImages[i] = ImageIO.read(imgFiles[i]);
                }
                type = buffImages[0].getType();
                chunkWidth = buffImages[0].getWidth();
                chunkHeight = buffImages[0].getHeight();

                // Initializing the final image
                BufferedImage finalImg = new BufferedImage(chunkWidth * cols, chunkHeight * rows, type);

                int num = 0;
                for (int i = 0; i < rows; i++) {
                    for (int j = 0; j < cols; j++) {
                        finalImg.createGraphics().drawImage(buffImages[num], chunkWidth * j, chunkHeight * i, null);
                        num++;
                    }
                }

                switch (fileExtension.toLowerCase()) {
                case "jpg":
                case "jpeg":
                    ImageWriter writer = ImageIO.getImageWritersByFormatName("jpeg").next();
                    // Using ImageWriteParam to force the system to save jpegs using the highest
                    // quality setting
                    ImageWriteParam param = writer.getDefaultWriteParam();
                    param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT); // Needed see javadoc
                    param.setCompressionQuality(1.0F); // Highest quality
                    writer.setOutput(
                            new FileImageOutputStream(new File(fileOutputPath + "/" + fileName + "." + fileExtension)));
                    writer.write(null, new IIOImage(finalImg, null, null), param);

                    break;
                default:
                    ImageIO.write(finalImg, fileExtension,
                            new File(fileOutputPath + "/" + fileName + "." + fileExtension));
                }

                if (deleteOriginal) {
                    for (File imgFile : imgFiles) {
                        imgFile.delete();
                    }
                }
            } catch (Exception e) {
                System.out.println(e);
            }
        });
    }
}