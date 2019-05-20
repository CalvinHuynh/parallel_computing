package nl.hva.pc.imagedenoiser;

import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;
import java.util.Map.Entry;
import java.util.concurrent.TimeUnit;

/**
 * Thread and lock implementation of an image denoiser
 * 
 * @author Calvin Huynh 500661383
 * @author Ramon Gill 500733062
 */
public class App {

    public static void main(String[] args) throws Exception {

        Image image = new Image();
        FileUtility fileHelper = new FileUtility();

        fileHelper.Unzip("image_dataset_10.zip", "resources/");

        image.ImageSplitter("resources/image_dataset_10/input_images", "resources/image_dataset_10/splitted_images", false);

        // TODO implement file download to keep the project size small
        // FileDownload fileDownload = new FileDownload();
        // fileDownload.DownloadWithJavaNIO("https://drive.google.com/uc?export=download&confirm=YDo9&id=1invDcT-fqGNWRQI4R2b8MwoZa78T2JGK",
        // "downloaded.zip");

        for (int i = 0; i < 1; i++) {
            System.out.println("Currently on run " + (i+1));
            HashMap<String, Long> hashMap = image.RunDenoiser(
                "resources/image_dataset_10/splitted_images", "resources/image_dataset_10/denoised_images", true);
            Map<String, Long> sortedMap = new TreeMap<>(new Comparator<String>() {
                public int compare(String o1, String o2) {
                    return o1.toLowerCase().compareTo(o2.toLowerCase());
                }
            });
            sortedMap.putAll(hashMap);
            for (Entry<String, Long> entry: sortedMap.entrySet()) {
                String id = entry.getKey();
                long timeTaken = TimeUnit.MILLISECONDS.convert(entry.getValue(), TimeUnit.NANOSECONDS);
                System.out.println(id + "\t" + timeTaken);
            }
        }

        image.ImageMerger("resources/image_dataset_10/denoised_images", "resources/image_dataset_10/output_images", false);
    }
}
