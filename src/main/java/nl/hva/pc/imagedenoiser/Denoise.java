package nl.hva.pc.imagedenoiser;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import org.opencv.core.Mat;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.photo.Photo;

public class Denoise {
    /**
     * The one method that does all the work
     * 
     * @param zipPath       Path of the downloaded zipfolder
     * @param inputPath     Path of the extracted images, the path starts with
     *                      "/resources/.."
     * @param outputPath    Path to write the denoised images to
     * @param showAllOutput Prints the intermediate output
     * @return
     */
    public HashMap<Integer, Long> RunDenoiser(String zipPath, String inputPath, String outputPath,
            Boolean showAllOutput) {

        FileHelper fileHelper = new FileHelper();
        HashMap<Integer, Long> idAndTimeMap = new HashMap<>();

        /**
         * This function unzips the supplied image_dataset.zip to the location specified
         * at the second parameter. The zip contains the following: - control_images is
         * a folder that contains the images without noise - input_images is a folder
         * that contains the noisy images
         */
        fileHelper.Unzip(zipPath, "resources/");

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
                String nameOfImage = pathToImage.toString().substring(pathToImage.toString().lastIndexOf("/") + 1);
                if (nameOfImage.substring(nameOfImage.lastIndexOf(".") + 1).toLowerCase().matches("jpg|png")) {
                    int positionOfUnderscore = nameOfImage.indexOf("_");
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