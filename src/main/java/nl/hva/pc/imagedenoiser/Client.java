package nl.hva.pc.imagedenoiser;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.net.UnknownHostException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

public class Client {

	public static void main(String[] args) throws RemoteException, UnknownHostException {
		// String serviceHost = Server.MasterNodeName;
		// int clientIdentifier = Server.numberOfClients + 1;
		String environment;
		String host = Server.HOST_NAME;
		int port = Server.PORT_NUMBER;
		String clientIdentifier = "";
		String clientPath = "~/Download/"; // save the downloaded images to the download folder
		String serverPath = "/home/calvin/Projects/Parallel-Computing/parallel_computing/resources/image_dataset_10/denoised_images";
		String upload = "upload";
		String download = "download";
		String start = "start";
		String dir = "dir";
		String mkdir = "mkdir";
		String rmdir = "rmdir";
		String rm = "rm";
		String shutdown = "shutdown";

		try {
			environment = System.getenv("SERVER_PORT");
			System.out.println(environment);

			host = environment.split(":")[0];

			port = Integer.parseInt(environment.split(":")[1]);
			System.out.println("seeking connection on:" + environment);

			Registry registry = LocateRegistry.getRegistry(host, port);
			ServiceInterface stub = (ServiceInterface) registry.lookup(Server.SERVICE_NAME);

			if (start.equals(args[0].toLowerCase())) {
				switch (args.length) {
					case 3:
						clientIdentifier = args[1];
						serverPath = args[2];
						clientPath = args[3];
					default:
						System.out.println("\"start\" command requires 3 arguments");
				}

				while(!stub.checkIfQueueIsEmpty()) {
					byte[] mydata = stub.downloadImageFromServer(stub.takeItemFromQueue());
					// System.out.println("downloading...");
					File clientPathFile = new File(clientPath);
					FileOutputStream out = new FileOutputStream(clientPathFile);
					out.write(mydata);
					out.flush();
					out.close();
				}
			}

			// to upload a file
			if (upload.equals(args[0])) {
				switch (args.length) {
				case 2:
					clientPath = args[1];
					serverPath = args[2];
					break;
				case 1:
					clientPath = args[1];
					break;
				default:
					break;
				}

				File clientPathFile = new File(clientPath);
				byte[] data = new byte[(int) clientPathFile.length()];
				FileInputStream in = new FileInputStream(clientPathFile);
				System.out.println("uploading to server...");
				in.read(data, 0, data.length);
				stub.uploadImageToServer(data, serverPath, (int) clientPathFile.length());

				in.close();
			}
			// to download a file
			if (download.equals(args[0])) {
				switch (args.length) {
				case 2:
					serverPath = args[1];
					clientPath = args[2];
					break;
				case 1:
					serverPath = args[1];
					break;
				default:
					break;
				}
				byte[] mydata = stub.downloadImageFromServer(serverPath);
				System.out.println("downloading...");
				File clientPathFiile = new File(clientPath);
				FileOutputStream out = new FileOutputStream(clientPathFiile);
				out.write(mydata);
				out.flush();
				out.close();
			}
		} catch (Exception e) {
			e.printStackTrace();
			System.out.println("error with connection or command. Check your hostname or command");
		}
	}
}