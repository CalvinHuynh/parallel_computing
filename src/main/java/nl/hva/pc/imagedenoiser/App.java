package nl.hva.pc.imagedenoiser;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import org.opencv.core.Mat;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.photo.Photo;

/**
 * Thread and lock implementation of an image denoiser
 * 
 * @author Calvin Huynh 500661383
 * @author Ramon Gill 500733062
 */
public class App {
    public static void main(String[] args) throws IOException {
        FileHelper fileHelper = new FileHelper();

        // TODO implement file download to keep the project size small
        // FileDownload fileDownload = new FileDownload();
        // fileDownload.DownloadWithJavaNIO("https://drive.google.com/uc?export=download&confirm=YDo9&id=1invDcT-fqGNWRQI4R2b8MwoZa78T2JGK",
        // "downloaded.zip");

        /**
         * This function unzips the supplied image_dataset.zip to the location specified
         * at the second parameter. The zip contains the following: - control_images is
         * a folder that contains the images without noise - input_images is a folder
         * that contains the noisy images
         */
        fileHelper.Unzip("image_dataset.zip", "resources/");

        try {
            // These are the default parameters given by OpenCV for
            // fastNlMeansDenoisingColored
            int templateWindowSize = 7;
            int wsearchWindowSize = 21;
            String targetToReplace = "Real";
            String replacementText = "Denoised";

            // Loads in the OpenCV library
            nu.pattern.OpenCV.loadShared();
            // Creates a folder for the output
            fileHelper.CreateFolder("resources/image_dataset/output_images");

            // Traverse the directory to retrieve the images.
            try (Stream<Path> paths = Files.walk(Paths.get("resources/image_dataset/input_images"))) {
                Stopwatch stopwatch = new Stopwatch();
                paths.forEach((pathToImage) -> {
                    String nameOfImage = pathToImage.toString().substring(pathToImage.toString().lastIndexOf("/") + 1);
                    if (nameOfImage.substring(nameOfImage.lastIndexOf(".") + 1).toLowerCase().matches("jpg|png")) {
                        // Retrieve the correct file extension from input
                        String target = targetToReplace + "." + nameOfImage.substring(nameOfImage.lastIndexOf(".") + 1);
                        String replacement = replacementText + "_" + templateWindowSize + "_" + wsearchWindowSize + "."
                                + nameOfImage.substring(nameOfImage.lastIndexOf(".") + 1);

                        Mat source = Imgcodecs.imread(pathToImage.toString(), Imgcodecs.CV_LOAD_IMAGE_COLOR);
                        Mat destination = new Mat(source.rows(), source.cols(), source.type());
                        destination = source;
                        Photo.fastNlMeansDenoisingColored(source, destination, 3, 3, templateWindowSize,
                                wsearchWindowSize);
                        Imgcodecs.imwrite("resources/image_dataset/output_images/"
                                + nameOfImage.replace(target, replacement), destination);
                        System.out.println("It took the system "
                                + TimeUnit.SECONDS.convert(stopwatch.elapsedTime(), TimeUnit.NANOSECONDS)
                                + " seconds to denoise the image");
                    } else {
                        System.out.println(nameOfImage + " is not an image with the following extensions: .jpg | .png");
                    }
                });
            } catch (IOException e) {
                e.printStackTrace();
            }
        } catch (Exception e) {
        }
    }
}
