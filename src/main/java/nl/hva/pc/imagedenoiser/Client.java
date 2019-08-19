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
		int numberOfRuns = Server.TOTAL_NUMBER_OF_RUNS;
		int currentRun = 0;
		String host = Server.HOST_NAME;
		int port = Server.SERVER_PORT_NUMBER;
		String clientIdentifier = "";
		// save the downloaded images to the download folder
		String clientPath = "/home/calvin/Downloads/";
		String clientOutputPath = clientPath + "denoised_images/";
		// download images from this path
		String serverResourcesPath = "/home/calvin/Projects/Parallel-Computing/parallel_computing/resources/image_dataset_10/splitted_images/";
		// upload denoised image to this path
		String serverOutputPath = "/home/calvin/Projects/Parallel-Computing/parallel_computing/resources/image_dataset_10/denoised_images/";

		boolean removeFileAfterDenoising = true;
		boolean showAllOutput = false;

		switch (args.length) {
		case 3:
			clientIdentifier = args[0];
			serverOutputPath = args[1];
			clientPath = args[2];
			break;
		case 1:
			clientIdentifier = args[0];
			break;
		default:
			System.out.println("Running with default variables");
		}

		while (currentRun < numberOfRuns) {
			try {
				Registry registry = LocateRegistry.getRegistry(host, port);
				ServiceInterface stub = (ServiceInterface) registry.lookup(Server.SERVICE_NAME);

				ArrayList<Object> resultArrayList = new ArrayList<>();
				HashMap<String, Long> resultMap = new HashMap<>();

				while (stub.getServerRunNumber() != currentRun) {
					if (showAllOutput) {
						System.out.println("Server run number is " + stub.getServerRunNumber() + "\n"
								+ "Current run number is " + currentRun);
					}
					Thread.sleep(500);
				}

				while (stub.checkServerStatus() == false) {
					if (showAllOutput) {
						System.out.println("Is the server ready to accecpt clients? " + stub.checkServerStatus());
					}
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
					out.flush();
					out.close();
					// #endregion

					// Denoise image and write result to a generic array list
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

				currentRun++;
				Thread.sleep(2500);

			} catch (Exception e) {
				e.printStackTrace();
				System.out.println("An error has occured." + "\n" + e);
				break;
			}
		}
	}
}