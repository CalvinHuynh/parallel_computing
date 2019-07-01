package nl.hva.pc.imagedenoiser;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.Serializable;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;

public class ServiceImplementation extends UnicastRemoteObject implements ServiceInterface, Serializable {

    protected ServiceImplementation() throws RemoteException {
        super();
    }

    private static final long serialVersionUID = -3959175628339711720L;

    @Override
    public void uploadImageToServer(byte[] imageData, String serverPath, int length) throws RemoteException {

        try {
            File serverpathfile = new File(serverPath);
            FileOutputStream out = new FileOutputStream(serverpathfile);
            byte[] data = imageData;

            out.write(data);
            out.flush();
            out.close();

        } catch (IOException e) {

            e.printStackTrace();
        }

        System.out.println("Done writing data...");
    }

    @Override
    public byte[] downloadImageFromServer(String serverPath) throws RemoteException {

        byte[] imageData;

        File serverpathfile = new File(serverPath);
        imageData = new byte[(int) serverpathfile.length()];
        FileInputStream in;
        try {
            in = new FileInputStream(serverpathfile);
            try {
                in.read(imageData, 0, imageData.length);
            } catch (IOException e) {
                e.printStackTrace();
            }
            try {
                in.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

        return imageData;
    }

    @Override
    public boolean checkIfQueueIsEmpty() throws RemoteException {
        return Server.pathsQueue.isEmpty();
    }
}
