package nl.hva.pc.imagedenoiser;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.stream.Stream;

public class Producer implements Runnable {
    private Integer producerId;
    private String path;
    private LinkedBlockingQueue<String> pathsQueue;
    private Boolean showAllOutput;

    /**
     * Producer handles the file IO, it reads all the regular files from the given path and adds it to 
     * the LinkedBlockingQueue
     * 
     * @param producerId       Identifier used for debugging
     * @param path             Folder path to read
     * @param pathsQueue       LinkedBlockingQueue to add the values to
     * @param showAllOutput    Prints the intermediate output
     */
    public Producer(Integer producerId, String path, LinkedBlockingQueue<String> pathsQueue, Boolean showAllOutput) {
        this.producerId = producerId;
        this.path = path;
        this.pathsQueue = pathsQueue;
        this.showAllOutput = showAllOutput;
    }

    @Override
    public void run() {
        try (Stream<Path> paths = Files.walk(Paths.get(path))) {
            paths.filter(Files::isRegularFile).forEach(result -> {
                if (showAllOutput) {
                    System.out.println("Producer " + producerId + " is adding file " + result.toString() + " the queue");
                }
                try {
                    pathsQueue.put(result.toString());
                } catch (InterruptedException e) {        
                    e.printStackTrace();
                    System.out.println("Producer " + producerId + " got encountered error " + e);
                    Thread.currentThread().interrupt();
                }
            });
            if (showAllOutput) {
                System.out.println("Added all the items to the queue");   
            }
            Server.SERVER_IS_READY = true;
        } catch (IOException e) {
            e.printStackTrace();
            System.out.println("Producer " + producerId + " got encountered error " + e);
            Thread.currentThread().interrupt();
        }
    }
}
