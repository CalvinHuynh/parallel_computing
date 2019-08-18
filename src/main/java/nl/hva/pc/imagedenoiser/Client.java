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
		// String serviceHost = Server.MasterNodeName;
		// int clientIdentifier = Server.numberOfClients + 1;
		String host = Server.HOST_NAME;
		int port = Server.PORT_NUMBER;
		String clientIdentifier = "";
		// save the downloaded images to the download folder
		String clientPath = "/home/calvin/Downloads/";
		String clientOutputPath = clientPath + "denoised_images/";
		// download images from this path
		String serverResourcesPath = "/home/calvin/Projects/Parallel-Computing/parallel_computing/resources/image_dataset_10/splitted_images/";
		// upload denoised image to this path
		String serverOutputPath = "/home/calvin/Projects/Parallel-Computing/parallel_computing/resources/image_dataset_10/denoised_images/";
		String upload = "upload";
		String download = "download";
		String start = "start";

		Boolean removeFileAfterDenoising = true;
		Boolean showAllOutput = false;
		// String dir = "dir";
		// String mkdir = "mkdir";
		// String rmdir = "rmdir";
		// String rm = "rm";
		// String shutdown = "shutdown";

		try {
			System.out.println("Connecting to:" + host + ":" + port);

			Registry registry = LocateRegistry.getRegistry(host, port);
			ServiceInterface stub = (ServiceInterface) registry.lookup(Server.SERVICE_NAME);

			if (start.equals(args[0].toLowerCase())) {
				switch (args.length) {
				case 3:
					clientIdentifier = args[1];
					serverOutputPath = args[2];
					clientPath = args[3];
				case 2:
					clientIdentifier = args[1];
				default:
					System.out.println("\"start\" command requires 4 arguments to overwrite default values");
				}

				System.out.println("Client identifier is " + clientIdentifier);

				ArrayList<Object> resultArrayList = new ArrayList<>();
				HashMap<String, Long> resultMap = new HashMap<>();

				while (!stub.checkIfQueueIsEmpty()) {

					//#region Download image from server
					String itemFromQueue = stub.takeItemFromQueue();
					System.out.println("Taken item from queue is " + itemFromQueue);
					String nameOfImage = itemFromQueue.toString()
							.substring(itemFromQueue.toString().lastIndexOf("/") + 1);
					System.out.println("Name of image is " + nameOfImage);
					System.out.println("Trying to download image from server");
					System.out.println("Path is: " + serverResourcesPath + nameOfImage);
					byte[] data = stub.downloadImageFromServer(serverResourcesPath + nameOfImage);
					// Write image to local folder
					File clientPathFile = new File(clientPath + nameOfImage);
					FileOutputStream out = new FileOutputStream(clientPathFile);
					out.write(data);
					out.flush();
					out.close();
					//#endregion

					// Denoise image and write result to a generic array list
					resultArrayList = stub.denoiseImage(clientIdentifier, clientPath + nameOfImage, clientOutputPath, showAllOutput);

					// Retrieve the HashMap object form the array list
					if (resultArrayList.get(1) instanceof HashMap) {
						resultMap = (HashMap<String,Long>) resultArrayList.get(1);
					}
					
					// Retrieve the name from denoiseImage stub
					nameOfImage = resultArrayList.get(0).toString();
					System.out.println("Name of file is " + nameOfImage);

					//#region Upload denoised image to server
					clientPathFile = new File(clientOutputPath + nameOfImage);
					data = new byte[(int) clientPathFile.length()];
					FileInputStream in = new FileInputStream(clientPathFile);
					in.read(data, 0, data.length);
					stub.uploadImageToServer(data, serverOutputPath + nameOfImage, (int) clientPathFile.length());
					in.close();
					//#endregion

					// Iterate over the result map and push it to the server
					for (Entry<String, Long> result : resultMap.entrySet()) {
						stub.pushResultToServer(result.getKey(), result.getValue());
					}
					
					// Optionally remove the original and denoised image after uploading it to the server
					if (removeFileAfterDenoising) {
						stub.removeDirectoryOrFile(clientPath + nameOfImage);
						stub.removeDirectoryOrFile(clientOutputPath + nameOfImage);
					}
				}
			}
		//#region other
		// 	// to upload a file
		// 	if (upload.equals(args[0])) {
		// 		switch (args.length) {
		// 		case 2:
		// 			clientPath = args[1];
		// 			serverOutputPath = args[2];
		// 			break;
		// 		case 1:
		// 			clientPath = args[1];
		// 			break;
		// 		default:
		// 			break;
		// 		}

		// 		File clientPathFile = new File(clientPath);
		// 		byte[] data = new byte[(int) clientPathFile.length()];
		// 		FileInputStream in = new FileInputStream(clientPathFile);
		// 		// System.out.println("uploading to server...");
		// 		in.read(data, 0, data.length);
		// 		stub.uploadImageToServer(data, serverOutputPath, (int) clientPathFile.length());

		// 		in.close();
		// 	}
		// 	// to download a file
		// 	if (download.equals(args[0])) {
		// 		switch (args.length) {
		// 		case 2:
		// 			serverResourcesPath = args[1];
		// 			clientPath = args[2];
		// 			break;
		// 		case 1:
		// 			serverResourcesPath = args[1];
		// 			break;
		// 		default:
		// 			break;
		// 		}
		// 		byte[] mydata = stub.downloadImageFromServer(serverResourcesPath);
		// 		System.out.println("downloading...");
		// 		File clientPathFiile = new File(clientPath);
		// 		FileOutputStream out = new FileOutputStream(clientPathFiile);
		// 		out.write(mydata);
		// 		out.flush();
		// 		out.close();
		// 	}
		//#endregion 
		} catch (Exception e) {
			e.printStackTrace();
			System.out.println("An error has occured." + "\n" + e);
		}
	}
}