package nl.hva.pc.imagedenoiser;

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface ServiceInterface extends Remote {
    void uploadImageToServer(byte[] imageData, String serverpath, int length) throws RemoteException; // upload the denoised image to server
    byte[] downloadImageFromServer(String serverPath) throws RemoteException; // downloads the image from the server
    boolean checkIfQueueIsEmpty() throws RemoteException; // checks if the server has items in the queue.
}