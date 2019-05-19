package nl.hva.pc.imagedenoiser;

import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.channels.ReadableByteChannel;

public class Download {

    /**
     * Downloads a file without buffering the data into the application memory.
     * Code is taken from the following source:
     * https://github.com/eugenp/tutorials/blob/master/core-java-modules/core-java-io/src/main/java/com/baeldung/download/FileDownload.java#L41
     * @param fileURL url of file to download
     * @param localFilename local name of downloaded file
     * @throws IOException
     */
    public void DownloadWithJavaNIO(String fileURL, String localFilename) throws IOException {
        URL url = new URL(fileURL);
        try (ReadableByteChannel readableByteChannel = Channels.newChannel(url.openStream());
                FileOutputStream fileOutputStream = new FileOutputStream(localFilename);
                FileChannel fileChannel = fileOutputStream.getChannel()) {
            fileChannel.transferFrom(readableByteChannel, 0, Long.MAX_VALUE);
        }
    }
}