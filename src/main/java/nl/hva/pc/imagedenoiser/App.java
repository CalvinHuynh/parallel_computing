package nl.hva.pc.imagedenoiser;

import java.math.BigInteger;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;
import java.util.Map.Entry;
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

    public static void main(String[] args) throws Exception {

        final Pattern PATTERN = Pattern.compile("(\\D*)(\\d*)");
        final int ROW_SIZE = 4;
        final int COL_SIZE = 4;

        Image image = new Image();
        FileUtility fileHelper = new FileUtility();

        fileHelper.Unzip("image_dataset_10.zip", "resources/", false);

        image.ImageSplitter("resources/image_dataset_10/input_images", "resources/image_dataset_10/splitted_images",
                ROW_SIZE, COL_SIZE, false);

        // TODO implement file download to keep the project size small
        // FileDownload fileDownload = new FileDownload();
        // fileDownload.DownloadWithJavaNIO("https://drive.google.com/uc?export=download&confirm=YDo9&id=1invDcT-fqGNWRQI4R2b8MwoZa78T2JGK",
        // "downloaded.zip");

        for (int i = 0; i < 1; i++) {
            System.out.println("Currently on run " + (i + 1));
            HashMap<String, Long> hashMap = image.RunDenoiser("resources/image_dataset_10/splitted_images",
                    "resources/image_dataset_10/denoised_images", false);
            Map<String, Long> sortedMap = new TreeMap<>(new Comparator<CharSequence>() {
                @Override
                // Number aware comparator from https://codereview.stackexchange.com/a/37249
                public int compare(CharSequence s1, CharSequence s2) {
                    Matcher m1 = PATTERN.matcher(s1);
                    Matcher m2 = PATTERN.matcher(s2);

                    // The only way find() could fail is at the end of a string
                    while (m1.find() && m2.find()) {
                        // matcher.group(1) fetches any non-digits captured by the
                        // first parentheses in PATTERN.
                        int nonDigitCompare = m1.group(1).compareTo(m2.group(1));
                        if (0 != nonDigitCompare) {
                            return nonDigitCompare;
                        }

                        // matcher.group(2) fetches any digits captured by the
                        // second parentheses in PATTERN.
                        if (m1.group(2).isEmpty()) {
                            return m2.group(2).isEmpty() ? 0 : -1;
                        } else if (m2.group(2).isEmpty()) {
                            return +1;
                        }

                        BigInteger n1 = new BigInteger(m1.group(2));
                        BigInteger n2 = new BigInteger(m2.group(2));
                        int numberCompare = n1.compareTo(n2);
                        if (0 != numberCompare) {
                            return numberCompare;
                        }
                    }

                    // Handle if one string is a prefix of the other.
                    // Nothing comes before something.
                    return m1.hitEnd() && m2.hitEnd() ? 0 : m1.hitEnd() ? -1 : +1;
                }
            });
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
