package nl.hva.pc.imagedenoiser;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Thread and lock implementation of an image denoiser
 * 
 * @author Calvin Huynh 500661383
 * @author Ramon Gill 500733062
 */
public class App {

    // TODO: Add commandline arguments to run it as a jar.
    // Required arguments would be 
    // - NUMBER_OF_THREADS // maybe use a method to check for threads available on the system
    public static void main(String[] args) throws Exception {

        final Pattern PATTERN = Pattern.compile("(\\D*)(\\d*)");
        // ROW_SIZE and COL_SIZE form the raster to split the images
        final int ROW_SIZE = 3;
        final int COL_SIZE = 3;
        final int NUMBER_OF_THREADS = 4;
        final int NUMBER_OF_RUNS = 1;
        ExecutorService executorService = Executors.newFixedThreadPool(NUMBER_OF_THREADS);
        int minItemsPerThread;
        int maxItemsPerThread;
        // number of threads that can use the maxItemsPerThread value
        int threadsWithMaxItems;
        int startOfList = 0;
        long startProcessingTime = 0l;
        long totalProcessingTime = 0l;
        Image image = new Image();
        FileUtility fileHelper = new FileUtility();

        fileHelper.Unzip("image_dataset_10.zip", "resources/", false);

        image.ImageSplitter("resources/image_dataset_10/input_images", "resources/image_dataset_10/splitted_images",
                ROW_SIZE, COL_SIZE, false);

        List<String> pathList = (Files.walk(Paths.get("resources/image_dataset_10/splitted_images"))
                .filter(Files::isRegularFile).map(result -> result.toString())).collect(Collectors.toList());

        minItemsPerThread = pathList.size() / NUMBER_OF_THREADS;
        System.out.println("minimum items per thread " + minItemsPerThread);
        maxItemsPerThread = minItemsPerThread + 1;
        System.out.println("maximum items per thread " + maxItemsPerThread);
        threadsWithMaxItems = pathList.size() - NUMBER_OF_THREADS * minItemsPerThread;
        System.out.println("threads with max items " + threadsWithMaxItems);
    
        // Outer loop for running the test multiple times.
        // The outer loop is placed here, because we are only measuring the time it takes to denoise the image.
        for (int i = 0; i < NUMBER_OF_RUNS; i++) {
            System.out.println("Currently on run " + (i + 1));
            List<CallableDenoiser> taskList = new ArrayList<>();
            for (int j = 0; j < NUMBER_OF_THREADS; j++) {
                System.out.println("determining the item count for thread " + j);
                int itemsCount = (j < threadsWithMaxItems ? maxItemsPerThread : minItemsPerThread);
                System.out.println("item count is " + itemsCount);
                System.out.println("start index is " + startOfList);
                int endOfList = startOfList + itemsCount;
                System.out.println("end index is " + endOfList);
                CallableDenoiser callableDenoiser = new CallableDenoiser("thread_" + j, pathList.subList(startOfList, endOfList),
                        "resources/image_dataset_10/denoised_images", true);
                taskList.add(callableDenoiser);
                startOfList = endOfList;
                System.out.println("new start index is " + startOfList);
            }

            startProcessingTime = System.nanoTime();
            List<Future<HashMap<String, Long>>> futureHashMaps = executorService.invokeAll(taskList);
            HashMap<String, Long> hashMap = new HashMap<>();
            for (Future<HashMap<String, Long>> futureHashMap : futureHashMaps) {
                hashMap.putAll(futureHashMap.get());
            }

            executorService.shutdown();
            if (!executorService.isTerminated()) {
                System.out.println("Waiting for termination...");
                executorService.awaitTermination(100, TimeUnit.MILLISECONDS);
            } 
            if (executorService.isTerminated()) {
                System.out.println("Executor service has been terminated");
                totalProcessingTime = System.nanoTime() - startProcessingTime;
            }
            Map<String, Long> sortedMap = new TreeMap<>(new NumberAwareComparator(PATTERN));

            sortedMap.putAll(hashMap);
            long totalTimeTaken = 0l;
            for (Entry<String, Long> entry : sortedMap.entrySet()) {
                String id = entry.getKey();
                long timeTaken = TimeUnit.MILLISECONDS.convert(entry.getValue(), TimeUnit.NANOSECONDS);
                totalTimeTaken = totalTimeTaken + timeTaken;
                System.out.println(id + "\t" + timeTaken);
            }
            System.out.println("Sum of total time taken by threads " + totalTimeTaken + " milliseconds.");
            System.out.println("Total processing time is: " + TimeUnit.MILLISECONDS.convert(totalProcessingTime, TimeUnit.NANOSECONDS)
                    + " milliseconds.");
        }

        image.ImageMerger("resources/image_dataset_10/denoised_images", "resources/image_dataset_10/output_images",
                ROW_SIZE, COL_SIZE, false);
    }
}
