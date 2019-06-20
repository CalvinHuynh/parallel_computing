package nl.hva.pc.imagedenoiser;

import java.util.HashMap;
import java.util.concurrent.Callable;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import org.opencv.core.Mat;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.photo.Photo;

/**
 * Callable task to denoise images
 */
public class CallableDenoiser implements Callable<HashMap<String, Long>> {

    private String threadIdentifier;
    private String fileOutputPath;
    private Boolean showAllOutput;
    private final LinkedBlockingQueue<String> queue;
    private int numberOfImagesDenoised = 0;

    /**
     * CallableDenoiser denoises the items that are given in the queue and writes it
     * to the specified output path
     * 
     * @param threadIdentifier Identifier to identify which thread denoised which
     *                         part of the image
     * @param imageQueue       Queue that contains the path to the images
     * @param fileOutputPath   Path to write the denoised images to
     * @param showAllOutput    Prints the intermediate output
     */
    public CallableDenoiser(String threadIdentifier, LinkedBlockingQueue<String> imageQueue, String fileOutputPath,
            Boolean showAllOutput) {
        this.threadIdentifier = threadIdentifier;
        this.queue = imageQueue;
        this.fileOutputPath = fileOutputPath;
        this.showAllOutput = showAllOutput;
    }

    @Override
    public HashMap<String, Long> call() throws Exception {

        FileUtility fileHelper = new FileUtility();
        HashMap<String, Long> idAndTimeMap = new HashMap<>();
        Stopwatch stopwatch = new Stopwatch();

        // These are the default parameters given by OpenCV for
        // fastNlMeansDenoisingColored
        int templateWindowSize = 7;
        int wsearchWindowSize = 21;
        String targetToReplace = "Real";
        String replacementText = "Denoised";

        // Loads in the OpenCV library
        nu.pattern.OpenCV.loadShared();
        // Creates a folder for the output
        fileHelper.CreateFolder(fileOutputPath);

        while (!queue.isEmpty()) {
            try {
                String pathToImage = queue.take();
                if (showAllOutput) {
                    System.out
                            .println("Consumer " + threadIdentifier + " took file " + pathToImage + " from the queue");
                }
                String nameOfImage = pathToImage.toString().substring(pathToImage.toString().lastIndexOf("/") + 1);
                if (nameOfImage.substring(nameOfImage.lastIndexOf(".") + 1).toLowerCase().matches("jpg|png")) {
                    // Retrieves the number of the image at the first underscore and last underscore
                    // to create an unique id
                    String idOfImage = nameOfImage.substring(0, nameOfImage.indexOf("_")) + "_" + nameOfImage
                            .substring(0, nameOfImage.indexOf(".")).substring(nameOfImage.lastIndexOf("_") + 1) + "_"
                            + threadIdentifier;
                    String outputImageName = nameOfImage.replace(targetToReplace, replacementText);

                    // Reset stopwatch
                    stopwatch.start();
                    // Loads image from path
                    Mat source = Imgcodecs.imread(pathToImage.toString(), Imgcodecs.CV_LOAD_IMAGE_COLOR);
                    Mat destination = new Mat(source.rows(), source.cols(), source.type());
                    destination = source;
                    Photo.fastNlMeansDenoisingColored(source, destination, 3, 3, templateWindowSize, wsearchWindowSize);
                    Imgcodecs.imwrite(fileOutputPath + "/" + outputImageName, destination);
                    // Stop the stopwatch after writing the image to the output folder
                    long elapsedTime = stopwatch.elapsedTime();
                    if (showAllOutput) {
                        System.out.println("It took thread " + threadIdentifier + " "
                                + TimeUnit.MILLISECONDS.convert(elapsedTime, TimeUnit.NANOSECONDS)
                                + " milliseconds to denoise the image " + nameOfImage
                                + ". Output image has been saved with the name " + outputImageName);
                    }
                    idAndTimeMap.put(idOfImage, elapsedTime);
                    numberOfImagesDenoised++;
                } else {
                    if (showAllOutput) {
                        System.out.println(nameOfImage + " is not an image with the following extensions: .jpg | .png");
                    }
                }
            } catch (Error e) {
                System.out.println("Error has occured: " + e);
            }
        }

        long totalTimeTaken = 0l;
        for (Long time : idAndTimeMap.values()) {
            totalTimeTaken += time;
        }
        if (showAllOutput) {
            System.out.println(
                    threadIdentifier + " took " + TimeUnit.MILLISECONDS.convert(totalTimeTaken, TimeUnit.NANOSECONDS)
                            + " milliseconds to denoise " + numberOfImagesDenoised + " images");
        }
        return idAndTimeMap;
    };
}
