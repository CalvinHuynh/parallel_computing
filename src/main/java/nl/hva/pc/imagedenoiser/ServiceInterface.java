package nl.hva.pc.imagedenoiser;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.HashMap;
import java.util.Queue;

public interface ServiceInterface extends Remote {
    // upload the denoised image to server
    void uploadImageToServer(byte[] imageData, String serverpath, int length) throws RemoteException;

    // downloads the image from the server
    byte[] downloadImageFromServer(String serverPath) throws RemoteException;

    // checks if the server has items in the queue.
    boolean checkIfQueueIsEmpty() throws RemoteException;

    // denoise image in queue
    HashMap<String, Long> denoiseImage(String identifier, Queue<String> imageQueue, String outputPath,
            Boolean showAllOutput) throws Exception;
}