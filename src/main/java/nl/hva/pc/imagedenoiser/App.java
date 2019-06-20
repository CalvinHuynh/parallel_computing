package nl.hva.pc.imagedenoiser;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
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
public class App {

    // TODO: Add commandline arguments to run it as a jar.
    // Required arguments would be
    // - NUMBER_OF_THREADS // maybe use a method to check for threads available on
    // the system
    public static void main(String[] args) throws Exception {

        final Pattern NUMBER_COMPARATOR_PATTERN = Pattern.compile("(\\D*)(\\d*)");
        final Pattern NUMBERS = Pattern.compile("\\d+");
        final String ZIP_SOURCE = "image_dataset_10.zip";
        final String ZIP_DESTINATION = "resources/";
        final String INPUT_IMAGES_FOLDER = "resources/image_dataset_10/input_images";
        final String SPLITTED_IMAGES_FOLDER = "resources/image_dataset_10/splitted_images";
        final String DENOISED_IMAGES_FOLDER = "resources/image_dataset_10/denoised_images";
        // ROW_SIZE and COL_SIZE form the raster to split the images
        int ROW_SIZE = 3;
        int COL_SIZE = 3;
        int NUMBER_OF_RUNS = 2;
        // Let 1 thread handle the IO requests
        // When running multiple producers, multiple copies of the same file will be added to the queue
        int NUMBER_OF_PRODUCERS = 1;
        int NUMBER_OF_CONSUMERS = 3;
        int TOTAL_NUMBER_OF_THREADS = NUMBER_OF_PRODUCERS + NUMBER_OF_CONSUMERS;
        // int QUEUE_LIMIT = ROW_SIZE * COL_SIZE * NUMBER_OF_IMAGES;
        int QUEUE_LIMIT = 10;

        // Used to store the results of the denoising
        HashMap<Integer, TreeMap<String, Long>> statisticsMap = new HashMap<>();
        HashMap<String, Long> summaryMap = new HashMap<>();
        long startProcessingTime = 0l;
        long totalProcessingTime = 0l;
        if (TOTAL_NUMBER_OF_THREADS > Runtime.getRuntime().availableProcessors()) {
            System.out.println("WARNING...\n"
                    + "You are trying to run the application with more cores than the maximum available cores");
        }
        Image image = new Image();
        FileUtility fileHelper = new FileUtility();

        fileHelper.Unzip(ZIP_SOURCE, ZIP_DESTINATION, false);

        image.ImageSplitter(INPUT_IMAGES_FOLDER, SPLITTED_IMAGES_FOLDER, ROW_SIZE, COL_SIZE, false);

        // Outer loop for running the test multiple times.
        // The outer loop is placed here, because we are only measuring the time it
        // takes to denoise the image.
        for (int i = 0; i < NUMBER_OF_RUNS; i++) {
            LinkedBlockingQueue<String> pathsQueue = new LinkedBlockingQueue<>(QUEUE_LIMIT);

            ExecutorService executorService = Executors.newFixedThreadPool(TOTAL_NUMBER_OF_THREADS);
            System.out.println("Currently on run " + (i + 1));

            List<Producer> producerList = new ArrayList<>();
            List<CallableDenoiser> consumerList = new ArrayList<>();
            // Spawn the number of producers
            for (int j = 0; j < NUMBER_OF_PRODUCERS; j++) {
                Producer producer = new Producer(j, SPLITTED_IMAGES_FOLDER, pathsQueue, true);
                producerList.add(producer);
            }

            // Start the producer
            for (Producer producer : producerList) {
                executorService.execute(producer);
            }

            // Spawn the maxumum number of threads
            // If the producer has finished it's task, it will return to the pool and start running
            // the callable denoiser task.
            for (int k = 0; k < TOTAL_NUMBER_OF_THREADS; k++) {
                CallableDenoiser callableDenoiser = new CallableDenoiser("thread_" + k, pathsQueue,
                        DENOISED_IMAGES_FOLDER, false);
                consumerList.add(callableDenoiser);
            }

            startProcessingTime = System.nanoTime();
            // Start all consumers
            List<Future<HashMap<String, Long>>> futureHashMaps = executorService.invokeAll(consumerList);
            HashMap<String, Long> futuresResolvedMap = new HashMap<>();
            for (Future<HashMap<String, Long>> futureHashMap : futureHashMaps) {
                futuresResolvedMap.putAll(futureHashMap.get());
            }

            executorService.shutdown();
            if (!executorService.isTerminated()) {
                System.out.println("Waiting for termination...");
                executorService.awaitTermination(10, TimeUnit.MILLISECONDS);
            }
            if (executorService.isTerminated()) {
                System.out.println("Executor service has been terminated");
                totalProcessingTime = System.nanoTime() - startProcessingTime;
            }
            // Sort the hashmap
            Map<String, Long> sortedMap = new TreeMap<>(new NumberAwareComparator(NUMBER_COMPARATOR_PATTERN));
            sortedMap.putAll(futuresResolvedMap);

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

        image.ImageMerger("resources/image_dataset_10/denoised_images", "resources/image_dataset_10/output_images",
                ROW_SIZE, COL_SIZE, false);

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

        String threadForm = NUMBER_OF_CONSUMERS >= 2 ? "threads" : "thread";
        System.out.println("Summarized values from all runs with " + NUMBER_OF_CONSUMERS + " " + threadForm + " are:");
        for (Map.Entry<String, Long> entry : summaryMap.entrySet()) {
            System.out.println(entry.getKey() + "\t" + (entry.getValue() / NUMBER_OF_RUNS));
        }
    }
}
