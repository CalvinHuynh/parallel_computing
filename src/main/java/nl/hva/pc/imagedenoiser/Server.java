package nl.hva.pc.imagedenoiser;

import java.io.Serializable;
import java.net.InetAddress;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Thread and lock implementation of an image denoiser
 * 
 * @author Calvin Huynh 500661383
 * @author Ramon Gill 500733062
 */
public class Server implements Serializable {

    /*
     * This class should be the server, since this holds the queue of all the
     * images. The client signs in/ tells the server that it is ready to denoise
     * images The server should send a part of the image over to the client The
     * client should either: 
     * 1. 
     * - Write the send image to disk
     * - Read the freshly written image 
     * - Denoise the image 
     * - Write the image to disk 
     * - Send the freshly denoised image back to server 
     * 2.
     * - Write image to buffer
     * - Denoise image from buffer 
     * - Send denoised buffer back to server 
     * - Server writes buffer to disk
     */

    private static final long serialVersionUID = -1666295165216198133L;

    public Server() throws RemoteException {
    }

    public static final String HOST_NAME = "localhost.localdomain";
    public static final String SERVICE_NAME = "ImageDenoiser";
    public static boolean serverIsReady = false;
    public static int serverPortNumber = 1234;
    public static int numberOfRuns = 2;
    public static LinkedBlockingQueue<String> pathsQueue;
    public static ConcurrentHashMap<String, Long> resultMap = new ConcurrentHashMap<>();

    private static void serverSetup() {
        try {
            String localHostname = InetAddress.getLocalHost().getHostName();

            // Initialize the Service implementation
            ServiceImplementation impl = new ServiceImplementation();
    
            // Creates the registry
            Registry registry = LocateRegistry.createRegistry(Server.serverPortNumber);

            // Bind the implementation to the registry
            registry.bind(Server.SERVICE_NAME, impl);

            System.out.println(Server.SERVICE_NAME + " running on " + localHostname);

        } catch (Exception ex) {
            System.out.println("Server failed to initialize: " + ex);
        }
    }

    // TODO: Add commandline arguments to override defaults.
    // Required arguments would be
    // - NUMBER_OF_THREADS // maybe use a method to check for threads available on
    // the system
    public static void main(String[] args) throws Exception {

        switch(args.length) {
            case 1:
                serverPortNumber = Integer.parseInt(args[0]);
                break;
            default:
        }

        final Pattern NUMBER_COMPARATOR_PATTERN = Pattern.compile("(\\D*)(\\d*)");
        final Pattern NUMBERS = Pattern.compile("\\d+");
        final String ZIP_SOURCE = "image_dataset_10.zip";
        final String ZIP_DESTINATION = "resources/";
        final String INPUT_IMAGES_FOLDER = "resources/image_dataset_10/input_images";
        final String SPLITTED_IMAGES_FOLDER = "resources/image_dataset_10/splitted_images";
        final String DENOISED_IMAGES_FOLDER = "resources/image_dataset_10/denoised_images";
        // rowSize and colSize form the raster to split the images
        int rowSize = 3;
        int colSize = 3;
        // Let 1 thread handle the IO requests
        // When running multiple producers, multiple copies of the same file will be added to the queue
        int numberOfProducers = 1;
        // int numberOfConsumers = 3;
        int totalNumberOfThreads = numberOfProducers /*+ numberOfConsumers*/;
        int totalNumberOfImages = rowSize * colSize * 10;

        // Used to store the results of the denoising
        HashMap<Integer, TreeMap<String, Long>> statisticsMap = new HashMap<>();
        HashMap<String, Long> summaryMap = new HashMap<>();
        long startProcessingTime = 0l;
        long totalProcessingTime = 0l;
        if (totalNumberOfThreads > Runtime.getRuntime().availableProcessors()) {
            System.out.println("WARNING...\n"
                    + "You are trying to run the application with more cores than the maximum available cores");
        }
        ImageUtility image = new ImageUtility();
        FileUtility fileHelper = new FileUtility();

        // Setup the server
        System.out.println("Setting up server");
        serverSetup();

        fileHelper.unzip(ZIP_SOURCE, ZIP_DESTINATION, false);

        image.splitImages(INPUT_IMAGES_FOLDER, SPLITTED_IMAGES_FOLDER, rowSize, colSize, false);
        fileHelper.createFolder(DENOISED_IMAGES_FOLDER);

        // Outer loop for running the test multiple times.
        // The outer loop is placed here, because we are only measuring the time it
        // takes to denoise the image.
        for (int i = 0; i < numberOfRuns; i++) {
            pathsQueue = new LinkedBlockingQueue<>(totalNumberOfImages);

            ExecutorService executorService = Executors.newFixedThreadPool(numberOfProducers);
            System.out.println("Currently on run " + (i + 1));

            List<Producer> producerList = new ArrayList<>();
            // Spawn the number of producers
            for (int j = 0; j < numberOfProducers; j++) {
                Producer producer = new Producer(j, SPLITTED_IMAGES_FOLDER, pathsQueue, true);
                producerList.add(producer);
            }

            // Start the producer
            for (Producer producer : producerList) {
                executorService.execute(producer);
            }
            startProcessingTime = System.nanoTime();

            executorService.shutdown();
            if (!executorService.isTerminated()) {
                System.out.println("Waiting for termination...");
                executorService.awaitTermination(10, TimeUnit.MILLISECONDS);
            }
            if (executorService.isTerminated()) {
                System.out.println("Executor service has been terminated");
            }

            serverIsReady = true;
            // Wait untill all the images have been denoised
            while(resultMap.size() != totalNumberOfImages) {
                System.out.println("Number of items left in queue " + pathsQueue.size());
                Thread.sleep(10);
            }

            // Sort the hashmap
            Map<String, Long> sortedMap = new TreeMap<>(new NumberAwareComparator(NUMBER_COMPARATOR_PATTERN));
            sortedMap.putAll(resultMap);
            // Clear the map
            resultMap.clear();

            long totalTimeTaken = 0l;
            String id = "";
            HashMap<String, Long> summedResultMap = new HashMap<>();
            for (Entry<String, Long> entry : sortedMap.entrySet()) {
                String identifier = entry.getKey();
                Matcher matcher = NUMBERS.matcher(identifier);
                if (matcher.find()) {
                    id = matcher.group(0);
                    if (summedResultMap.get(id) == null) {
                        summedResultMap.put(id, TimeUnit.MILLISECONDS.convert(entry.getValue(), TimeUnit.NANOSECONDS));
                    } else {
                        long value = summedResultMap.get(id);
                        summedResultMap.replace(id,
                                value + TimeUnit.MILLISECONDS.convert(entry.getValue(), TimeUnit.NANOSECONDS));
                    }
                }
                long timeTaken = TimeUnit.MILLISECONDS.convert(entry.getValue(), TimeUnit.NANOSECONDS);
                totalTimeTaken = totalTimeTaken + timeTaken;
                System.out.println(identifier + "\t" + timeTaken + "\t" + id);
            }
            TreeMap<String, Long> sortedSummedResultMap = new TreeMap<>(
                    new NumberAwareComparator(NUMBER_COMPARATOR_PATTERN));
            sortedSummedResultMap.putAll(summedResultMap);

            statisticsMap.put(i + 1, sortedSummedResultMap);
            System.out.println("Sum of total time taken by threads " + totalTimeTaken + " milliseconds.");
            System.out.println("Total processing time is: "
                    + TimeUnit.MILLISECONDS.convert(totalProcessingTime, TimeUnit.NANOSECONDS) + " milliseconds.");
        }

        image.mergeImages("resources/image_dataset_10/denoised_images", "resources/image_dataset_10/output_images",
                rowSize, colSize, false);

        for (Map.Entry<Integer, TreeMap<String, Long>> statisticEntry : statisticsMap.entrySet()) {
            int runNumber = statisticEntry.getKey();
            System.out.println("Summed result for run " + runNumber);
            for (Map.Entry<String, Long> result : statisticEntry.getValue().entrySet()) {
                if (summaryMap.get(result.getKey()) == null) {
                    summaryMap.put(result.getKey(), result.getValue());
                } else {
                    long value = summaryMap.get(result.getKey());
                    summaryMap.replace(result.getKey(), value + result.getValue());
                }
                System.out.println(result.getKey() + "\t" + result.getValue());
            }
        }

        TreeMap<String, Long> sortedSummaryMap = new TreeMap<>(new NumberAwareComparator(NUMBER_COMPARATOR_PATTERN));
        sortedSummaryMap.putAll(summaryMap);

        for (Map.Entry<String, Long> entry : summaryMap.entrySet()) {
            System.out.println(entry.getKey() + "\t" + (entry.getValue() / numberOfRuns));
        }
        // Reset server status
        serverIsReady = false;
    }
}
