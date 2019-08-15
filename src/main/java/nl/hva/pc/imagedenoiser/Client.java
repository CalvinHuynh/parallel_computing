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
		String clientpath = "~/Download/"; // save the downloaded images to the download folder
		String serverpath = "/home/calvin/Projects/Parallel-Computing/parallel_computing/resources/image_dataset_10/denoised_images";
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

			// to upload a file
			if (upload.equals(args[0])) {
				switch (args.length) {
				case 2:
					clientpath = args[1];
					serverpath = args[2];
					break;
				case 1:
					clientpath = args[1];
					break;
				default:
					break;
				}

				File clientpathfile = new File(clientpath);
				byte[] mydata = new byte[(int) clientpathfile.length()];
				FileInputStream in = new FileInputStream(clientpathfile);
				System.out.println("uploading to server...");
				in.read(mydata, 0, mydata.length);
				stub.uploadImageToServer(mydata, serverpath, (int) clientpathfile.length());

				in.close();
			}
			// to download a file
			if (download.equals(args[0])) {
				switch (args.length) {
				case 2:
					serverpath = args[1];
					clientpath = args[2];
					break;
				case 1:
					serverpath = args[1];
					break;
				default:
					break;
				}
				byte[] mydata = stub.downloadImageFromServer(serverpath);
				System.out.println("downloading...");
				File clientpathfile = new File(clientpath);
				FileOutputStream out = new FileOutputStream(clientpathfile);
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