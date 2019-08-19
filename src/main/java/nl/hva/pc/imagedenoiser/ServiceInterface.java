package nl.hva.pc.imagedenoiser;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.ArrayList;

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
     * @return an item from queue
     * @throws InterruptedException
     */
    String takeItemFromQueue() throws RemoteException;

    /**
     * Denoises the images that reside in the queue
     * 
     * @param identifier    Identifier of the client
     * @param pathToImage   Path to image
     * @param outputPath    Output path for the images
     * @param showAllOutput Shows debugging information
     * @return an ArrayList that contains the denoised image name and a HashMap of the time taken denoising the image
     * @throws Exception
     */
    ArrayList<Object> denoiseImage(String identifier, String pathToImage, String outputPath,
            Boolean showAllOutput) throws RemoteException;

    /**
     * Pushes the result to the server
     * 
     * @param key   Identifier of the client
     * @param value Total time taken in Long
     */
    void pushResultToServer(String key, Long value) throws RemoteException;

    /**
     * Removes a directory or a file
     * 
     * @param serverPath Path to file or directory
     * @return a boolean whether the file or directory has been deleted
     * @throws RemoteException
     */
    boolean removeDirectoryOrFile(String serverPath) throws RemoteException;

    /**
     * Checks the status of the server to see if it is ready to accept clients.
     */
    boolean checkServerStatus() throws RemoteException;

    /**
     * Retrieved the current run number of the server
     * @return current run number
     * @throws RemoteException
     */
    int getServerRunNumber() throws RemoteException;
}