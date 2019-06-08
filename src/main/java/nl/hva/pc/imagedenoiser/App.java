package nl.hva.pc.imagedenoiser;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

/**
 * Thread and lock implementation of an image denoiser
 * 
 * @author Calvin Huynh 500661383
 * @author Ramon Gill 500733062
 */
public class App {

    public static void main(String[] args) throws Exception {

        final Pattern PATTERN = Pattern.compile("(\\D*)(\\d*)");
        final int ROW_SIZE = 3;
        final int COL_SIZE = 3;
        final int NUMBER_OF_RUNS = 2;

        Image image = new Image();
        FileUtility fileHelper = new FileUtility();

        fileHelper.Unzip("image_dataset_10.zip", "resources/", false);

        image.ImageSplitter("resources/image_dataset_10/input_images", "resources/image_dataset_10/splitted_images",
                ROW_SIZE, COL_SIZE, false);

        // TODO implement file download to keep the project size small
        // FileDownload fileDownload = new FileDownload();
        // fileDownload.DownloadWithJavaNIO("https://drive.google.com/uc?export=download&confirm=YDo9&id=1invDcT-fqGNWRQI4R2b8MwoZa78T2JGK",
        // "downloaded.zip");

        for (int i = 0; i < NUMBER_OF_RUNS; i++) {
            System.out.println("Currently on run " + (i + 1));
            HashMap<String, Long> hashMap = image.RunDenoiser("resources/image_dataset_10/splitted_images",
                    "resources/image_dataset_10/denoised_images", false);
            Map<String, Long> sortedMap = new TreeMap<>(new NumberAwareComparator(PATTERN));
            sortedMap.putAll(hashMap);
            long totalTimeTaken = 0l;
            for (Entry<String, Long> entry : sortedMap.entrySet()) {
                String id = entry.getKey();
                long timeTaken = TimeUnit.MILLISECONDS.convert(entry.getValue(), TimeUnit.NANOSECONDS);
                totalTimeTaken = totalTimeTaken + timeTaken;
                System.out.println(id + "\t" + timeTaken);
            }
            System.out.println("Total time taken: " + totalTimeTaken + " milliseconds.");
        }

        image.ImageMerger("resources/image_dataset_10/denoised_images", "resources/image_dataset_10/output_images",
                ROW_SIZE, COL_SIZE, false);
    }
}
