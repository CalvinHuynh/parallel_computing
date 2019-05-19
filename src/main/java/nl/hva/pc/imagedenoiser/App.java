package nl.hva.pc.imagedenoiser;

import java.io.IOException;
import java.util.Collections;
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

    public static void main(String[] args) throws IOException {

        Image image = new Image();
        FileUtility fileHelper = new FileUtility();

        fileHelper.Unzip("image_dataset_10.zip", "resources/");

        image.ImageSplitter();

        // TODO implement file download to keep the project size small
        // FileDownload fileDownload = new FileDownload();
        // fileDownload.DownloadWithJavaNIO("https://drive.google.com/uc?export=download&confirm=YDo9&id=1invDcT-fqGNWRQI4R2b8MwoZa78T2JGK",
        // "downloaded.zip");

        // for (int i = 0; i < 1; i++) {
        //     System.out.println("Currently on run " + (i+1));
        //     HashMap<Integer, Long> hashMap = image.RunDenoiser(
        //         "resources/image_dataset_10/input_images", "resources/image_dataset_10/output_images/", true);
        //     Map<Integer, Long> sortedMap = new TreeMap<>(Collections.reverseOrder());
        //     sortedMap.putAll(hashMap);
        //     for (Entry<Integer, Long> entry: sortedMap.entrySet()) {
        //         int id = entry.getKey();
        //         long timeTaken = TimeUnit.MILLISECONDS.convert(entry.getValue(), TimeUnit.NANOSECONDS);
        //         System.out.println(id + "\t" + timeTaken);
        //     }
        // }
    }
}
