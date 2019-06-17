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

    public Producer(Integer producerId, String path, LinkedBlockingQueue<String> pathsQueue) {
        this.producerId = producerId;
        this.path = path;
        this.pathsQueue = pathsQueue;
    }

    @Override
    public void run() {
        try (Stream<Path> paths = Files.walk(Paths.get(path))) {
            paths.filter(Files::isRegularFile).forEach(result -> {
                // System.out.println("Producer " + producerId + " is adding file " + result.toString() + " the queue");
				pathsQueue.offer(result.toString());
            });
            // System.out.println("queue size from producer " + producerId + " is " + pathsQueue.size());
        } catch (IOException e) {
            e.printStackTrace();
            System.out.println("Producer " + producerId + " got encountered error " + e);
            Thread.currentThread().interrupt();
        }
    }
}
