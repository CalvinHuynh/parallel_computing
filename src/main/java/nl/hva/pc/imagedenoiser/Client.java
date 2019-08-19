package nl.hva.pc.imagedenoiser;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.net.UnknownHostException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map.Entry;

public class Client {

	public static void main(String[] args) throws RemoteException, UnknownHostException {
		int numberOfRuns = Server.numberOfRuns;
		int currentRun = 0;
		String host = Server.HOST_NAME;
		int port = Server.serverPortNumber;
		String clientIdentifier = "";
		// save the downloaded images to the download folder
		String clientPath = "/home/calvin/Downloads/";
		String clientOutputPath = clientPath + "denoised_images/";
		// download images from this path
		String serverResourcesPath = "/home/calvin/Projects/Parallel-Computing/parallel_computing/resources/image_dataset_10/splitted_images/";
		// upload denoised image to this path
		String serverOutputPath = "/home/calvin/Projects/Parallel-Computing/parallel_computing/resources/image_dataset_10/denoised_images/";

		Boolean removeFileAfterDenoising = true;
		Boolean showAllOutput = true;

		while (currentRun < numberOfRuns) {
			currentRun++;
			System.out.println("Currently on run: " + currentRun);
			try {
				Registry registry = LocateRegistry.getRegistry(host, port);
				ServiceInterface stub = (ServiceInterface) registry.lookup(Server.SERVICE_NAME);
				switch (args.length) {
				case 3:
					clientIdentifier = args[0];
					serverOutputPath = args[1];
					clientPath = args[2];
				case 1:
					clientIdentifier = args[0];
				default:
					System.out.println("Running with default variables");
				}

				System.out.println("Client identifier is " + clientIdentifier);

				ArrayList<Object> resultArrayList = new ArrayList<>();
				HashMap<String, Long> resultMap = new HashMap<>();

				System.out.println("Checking server variables..." + "\n" + "Server is ready to accept clients? "
						+ stub.checkServerStatus());

				while (stub.checkServerStatus() == false) {
					System.out.println("Inside while loop" + System.currentTimeMillis());
					System.out.println("Checking server variables..." + "\n" + "Server is ready to accept clients? "
							+ stub.checkServerStatus());
					Thread.sleep(1000);
				}

				while (!stub.checkIfQueueIsEmpty()) {
					// #region Download image from server
					String itemFromQueue = stub.takeItemFromQueue();
					String nameOfImage = itemFromQueue.toString()
							.substring(itemFromQueue.toString().lastIndexOf("/") + 1);
					String oldNameOfImage = nameOfImage;
					byte[] data = stub.downloadImageFromServer(serverResourcesPath + nameOfImage);
					// Write image to local folder
					File clientPathFile = new File(clientPath + nameOfImage);
					FileOutputStream out = new FileOutputStream(clientPathFile);
					out.write(data);
					System.out.println("Downloaded image " + nameOfImage);
					out.flush();
					out.close();
					// #endregion

					// Denoise image and write result to a generic array list
					System.out.println("Denoising image...");
					resultArrayList = stub.denoiseImage(clientIdentifier, clientPath + nameOfImage, clientOutputPath,
							showAllOutput);

					// Retrieve the HashMap object form the array list
					if (resultArrayList.get(1) instanceof HashMap) {
						resultMap = (HashMap<String, Long>) resultArrayList.get(1);
					}

					// Retrieve the name from denoiseImage stub
					nameOfImage = resultArrayList.get(0).toString();

					// #region Upload denoised image to server
					clientPathFile = new File(clientOutputPath + nameOfImage);
					data = new byte[(int) clientPathFile.length()];
					FileInputStream in = new FileInputStream(clientPathFile);
					in.read(data, 0, data.length);
					stub.uploadImageToServer(data, serverOutputPath + nameOfImage, (int) clientPathFile.length());
					System.out.println("Uploading image " + nameOfImage);
					in.close();
					// #endregion

					// Iterate over the result map and push it to the server
					for (Entry<String, Long> result : resultMap.entrySet()) {
						stub.pushResultToServer(result.getKey(), result.getValue());
					}

					// Optionally remove the original and denoised image after uploading it to the
					// server
					if (removeFileAfterDenoising) {
						stub.removeDirectoryOrFile(clientPath + oldNameOfImage);
						stub.removeDirectoryOrFile(clientOutputPath + nameOfImage);
					}
				}

			} catch (Exception e) {
				e.printStackTrace();
				System.out.println("An error has occured." + "\n" + e);
			}
		}
	}
}