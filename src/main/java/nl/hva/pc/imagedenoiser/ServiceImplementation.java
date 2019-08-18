package nl.hva.pc.imagedenoiser;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.Serializable;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.TimeUnit;

import org.opencv.core.Mat;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.photo.Photo;

public class ServiceImplementation extends UnicastRemoteObject implements ServiceInterface, Serializable {

    protected ServiceImplementation() throws RemoteException {
        super();
    }

    private static final long serialVersionUID = -3959175628339711720L;

    @Override
    public void uploadImageToServer(byte[] imageData, String serverPath, int length) throws RemoteException {

        try {
            File serverpathfile = new File(serverPath);
            FileOutputStream out = new FileOutputStream(serverpathfile);
            byte[] data = imageData;

            out.write(data);
            out.flush();
            out.close();

        } catch (IOException e) {

            e.printStackTrace();
        }

        System.out.println("Done writing data...");
    }

    @Override
    public byte[] downloadImageFromServer(String serverPath) throws RemoteException {

        byte[] imageData;

        File serverpathfile = new File(serverPath);
        imageData = new byte[(int) serverpathfile.length()];
        FileInputStream in;
        try {
            in = new FileInputStream(serverpathfile);
            try {
                in.read(imageData, 0, imageData.length);
            } catch (IOException e) {
                e.printStackTrace();
            }
            try {
                in.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

        return imageData;
    }

    @Override
    public boolean checkIfQueueIsEmpty() throws RemoteException {
        return Server.pathsQueue.isEmpty();
    }

    @Override
    public ArrayList<Object> denoiseImage(String identifier, String pathToImage, String outputPath,
            Boolean showAllOutput) throws RemoteException {
        int numberOfImagesDenoised = 0;
        
        ArrayList<Object> resultarrayList = new ArrayList<>();
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
        fileHelper.createFolder(outputPath);

        while (pathToImage.trim() != null || !pathToImage.isEmpty()) {
            try {
                if (showAllOutput) {
                    System.out.println(identifier + " is trying to denoise " + pathToImage);
                }
                String nameOfImage = pathToImage.toString().substring(pathToImage.toString().lastIndexOf("/") + 1);
                if (nameOfImage.substring(nameOfImage.lastIndexOf(".") + 1).toLowerCase().matches("jpg|png")) {
                    // Retrieves the number of the image at the first underscore and last underscore
                    // to create an unique id
                    String idOfImage = nameOfImage.substring(0, nameOfImage.indexOf("_")) + "_" + nameOfImage
                            .substring(0, nameOfImage.indexOf(".")).substring(nameOfImage.lastIndexOf("_") + 1) + "_"
                            + identifier;
                    String outputImageName = nameOfImage.replace(targetToReplace, replacementText);

                    resultarrayList.add(outputImageName);

                    // Reset stopwatch
                    stopwatch.start();
                    // Loads image from path
                    Mat source = Imgcodecs.imread(pathToImage.toString(), Imgcodecs.CV_LOAD_IMAGE_COLOR);
                    Mat destination = new Mat(source.rows(), source.cols(), source.type());
                    destination = source;
                    Photo.fastNlMeansDenoisingColored(source, destination, 3, 3, templateWindowSize, wsearchWindowSize);
                    Imgcodecs.imwrite(outputPath + "/" + outputImageName, destination);
                    // Stop the stopwatch after writing the image to the output folder
                    long elapsedTime = stopwatch.elapsedTime();
                    if (showAllOutput) {
                        System.out.println("It took consumer" + identifier + " "
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
            System.out
                    .println(identifier + " took " + TimeUnit.MILLISECONDS.convert(totalTimeTaken, TimeUnit.NANOSECONDS)
                            + " milliseconds to denoise " + numberOfImagesDenoised + " images");
        }

        resultarrayList.add(idAndTimeMap);
        return resultarrayList;
    }

    @Override
    public void pushResultToServer(String key, Long value) {
        Server.resultMap.put(key, value);
    }

    @Override
    public String takeItemFromQueue() throws RemoteException {
        try {
            return Server.pathsQueue.take();
        } catch (InterruptedException e) {
            // e.printStackTrace();
            System.out.println("Error has occured: " + e);
        }
        return null;
    }

    @Override
    public boolean removeDirectoryOrFile(String serverPath) throws RemoteException {
        File serverPathDirectoryOrFile = new File(serverPath);
		return serverPathDirectoryOrFile.delete();
    }
}
