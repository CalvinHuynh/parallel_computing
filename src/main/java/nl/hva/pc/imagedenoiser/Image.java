package nl.hva.pc.imagedenoiser;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.FileImageOutputStream;

import org.opencv.core.Mat;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.photo.Photo;

public class Image {

    // from
    // http://kalanir.blogspot.com/2010/02/how-to-split-image-into-chunks-java.html
    public void ImageSplitter(String fileInputPath, boolean deleteOriginal) throws Exception {
        // Traverse the directory to retrieve the images.
        try (Stream<Path> paths = Files.walk(Paths.get(fileInputPath))) {
            paths.forEach((pathToImage) -> {
                File file = new File(pathToImage.toString());
                String nameOfFile = pathToImage.toString().substring(pathToImage.toString().lastIndexOf("/") + 1);
                if (nameOfFile.substring(nameOfFile.lastIndexOf(".") + 1).toLowerCase().matches("jpg|png")) {
                    // String fileName = file.getName().substring(file.getName().lastIndexOf("/") + 1).substring(0,
                    //         file.getName().lastIndexOf("."));
                    String fileName = nameOfFile.substring(nameOfFile.lastIndexOf("/") + 1).substring(0,
                            nameOfFile.lastIndexOf("."));
                    // String fileExtension = file.getName().substring(file.getName().lastIndexOf(".") + 1).trim();
                    String fileExtension = nameOfFile.substring(nameOfFile.lastIndexOf(".") + 1).trim();
                    FileInputStream fis = null;
                    BufferedImage image = null;
                    try {
                        fis = new FileInputStream(file);
                        image = ImageIO.read(fis);

                        int rows = 1; // You should decide the values for rows and cols variables
                        int cols = 2;
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
                                writer.setOutput(
                                        new FileImageOutputStream(new File("resources/image_dataset_10/splitted_images/"
                                                + fileName + "_" + i + "." + fileExtension)));
                                writer.write(null, new IIOImage(imgs[i], null, null), param);
                            }
                            break;
                        default:
                            for (int i = 0; i < imgs.length; i++) {
                                ImageIO.write(imgs[i], fileExtension,
                                        new File("resources/image_dataset_10/splitted_images/" + fileName + "_" + i
                                                + "." + fileExtension));
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

    // from
    // http://kalanir.blogspot.com/2010/02/how-to-merge-multiple-images-into-one.html
    public void ImageMerger(String fileInputPath, boolean deleteOriginal) throws IOException {
        int rows = 1; // we assume the no. of rows and cols are known and each chunk has equal width
                      // and height
        int cols = 2;
        int chunks = rows * cols;
        String fileExtension = "";
        String fileName = "";

        int chunkWidth, chunkHeight;
        int type;
        // fetching image files
        File[] imgFiles = new File[chunks];
        for (int i = 0; i < chunks; i++) {
            imgFiles[i] = new File("resources/image_dataset_10/splitted_images/1_Canon5D2_bag_Real_" + i + ".JPG");
            fileExtension = imgFiles[i].getName().substring(imgFiles[i].getName().lastIndexOf(".") + 1).trim();
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
            writer.setOutput(new FileImageOutputStream(
                    new File("resources/image_dataset_10/input_images/" + fileName + "." + fileExtension)));
            writer.write(null, new IIOImage(finalImg, null, null), param);

            break;
        default:
            ImageIO.write(finalImg, fileExtension,
                    new File("resources/image_dataset_10/input_images/" + fileName + "." + fileExtension));
        }

        if (deleteOriginal) {
            for (File imgFile : imgFiles) {
                imgFile.delete();
            }
        }
    }

    /**
     * The one method that does all the work
     * 
     * @param inputPath     Path of the extracted images, the path starts with
     *                      "/resources/.."
     * @param outputPath    Path to write the denoised images to
     * @param showAllOutput Prints the intermediate output
     * @return
     */
    public HashMap<Integer, Long> RunDenoiser(String inputPath, String outputPath, Boolean showAllOutput) {

        FileUtility fileHelper = new FileUtility();
        HashMap<Integer, Long> idAndTimeMap = new HashMap<>();

        // These are the default parameters given by OpenCV for
        // fastNlMeansDenoisingColored
        int templateWindowSize = 7;
        int wsearchWindowSize = 21;
        String targetToReplace = "Real";
        String replacementText = "Denoised";

        // Loads in the OpenCV library
        nu.pattern.OpenCV.loadShared();
        // Creates a folder for the output
        fileHelper.CreateFolder(outputPath);

        // Traverse the directory to retrieve the images.
        try (Stream<Path> paths = Files.walk(Paths.get(inputPath))) {
            Stopwatch stopwatch = new Stopwatch();
            paths.forEach((pathToImage) -> {
                // Retrieves name of image file
                String nameOfImage = pathToImage.toString().substring(pathToImage.toString().lastIndexOf("/") + 1);
                if (nameOfImage.substring(nameOfImage.lastIndexOf(".") + 1).toLowerCase().matches("jpg|png")) {
                    int positionOfUnderscore = nameOfImage.indexOf("_");
                    // Retrieves the number of the image at the first underscore
                    int idOfImage = Integer.parseInt(nameOfImage.substring(0, positionOfUnderscore));
                    // Retrieve the correct file extension from input
                    String target = targetToReplace + "." + nameOfImage.substring(nameOfImage.lastIndexOf(".") + 1);
                    String replacement = replacementText + "_" + templateWindowSize + "_" + wsearchWindowSize + "."
                            + nameOfImage.substring(nameOfImage.lastIndexOf(".") + 1);
                    String outputImageName = nameOfImage.replace(target, replacement);

                    // Reset stopwatch
                    stopwatch.start();
                    // Loads image from path
                    Mat source = Imgcodecs.imread(pathToImage.toString(), Imgcodecs.CV_LOAD_IMAGE_COLOR);
                    Mat destination = new Mat(source.rows(), source.cols(), source.type());
                    destination = source;
                    Photo.fastNlMeansDenoisingColored(source, destination, 3, 3, templateWindowSize, wsearchWindowSize);
                    Imgcodecs.imwrite(outputPath + outputImageName, destination);
                    long elapsedTime = stopwatch.elapsedTime();
                    if (showAllOutput) {
                        System.out.println(
                                "It took the system " + TimeUnit.MILLISECONDS.convert(elapsedTime, TimeUnit.NANOSECONDS)
                                        + " milliseconds to denoise the image " + nameOfImage
                                        + ". Output image has been saved with the name " + outputImageName);
                    }
                    idAndTimeMap.put(idOfImage, elapsedTime);
                } else {
                    if (showAllOutput) {
                        System.out.println(nameOfImage + " is not an image with the following extensions: .jpg | .png");
                    }
                }
            });
        } catch (IOException e) {
            e.printStackTrace();
        }
        long totalTimeTaken = 0l;
        for (Long time : idAndTimeMap.values()) {
            totalTimeTaken += time;
        }
        System.out.println("Total time taken: " + TimeUnit.MILLISECONDS.convert(totalTimeTaken, TimeUnit.NANOSECONDS)
                + " milliseconds.");
        return idAndTimeMap;
    };
}