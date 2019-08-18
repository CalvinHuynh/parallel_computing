package nl.hva.pc.imagedenoiser;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.HashMap;
import java.util.Queue;

public interface ServiceInterface extends Remote {
    /**
     * Uploads the image to server
     * 
     * @param imageData  Image data in bytes
     * @param serverpath Upload path
     * @param length     Length of item
     * @throws RemoteException
     */
    void uploadImageToServer(byte[] imageData, String serverpath, int length) throws RemoteException;

    /**
     * Downloads the image from the server
     * 
     * @param serverPath Path to file
     * @return
     * @throws RemoteException
     */
    byte[] downloadImageFromServer(String serverPath) throws RemoteException;

    /**
     * Checks if the queue is empty
     * 
     * @return a boolean
     * @throws RemoteException
     */
    boolean checkIfQueueIsEmpty() throws RemoteException;

    /**
     * Tries to take an item from an atomic queue
     * 
     * @return item from queue
     * @throws InterruptedException
     */
    String takeItemFromQueue() throws InterruptedException;

    /**
     * Denoises the images that reside in the queue
     * 
     * @param identifier    Identifier of the client
     * @param imageQueue    Image queue
     * @param outputPath    Output path for the images
     * @param showAllOutput Shows debugging information
     * @return a summary of the time taken
     * @throws Exception
     */
    HashMap<String, Long> denoiseImage(String identifier, Queue<String> imageQueue, String outputPath,
            Boolean showAllOutput) throws Exception;

    /**
     * Pushes the result to the server
     * 
     * @param key   Identifier of the client
     * @param value Total time taken in Long
     */
    void pushResultToServer(String key, Long value);
}