/**
 * HTTPChatServer.java, receives information 
 * from a Python-based client program, parses and responds.
 * @author Sagnik Mukherjee, Michael Mandich
 * @version 1.0
 */

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class HTTPChatServer
{
	// Miscellaneous static vars which are shared across various methods.
	static boolean validLogin = false;
	static boolean hasCookie = false;
	static String currentUser = "";

	public static void main(String[] args) throws Exception
	{
		if (args.length != 1) {
			System.err.println("Usage: java Server <port number>");
			System.exit(1);
		}

		// Create server socket given port number.
		int portNumber = Integer.parseInt(args[0]);
		ServerSocket serverSocket = new ServerSocket(portNumber);
		System.out.println("Now connected to port " + portNumber);
		// Hold socket open until forced disconnection.
		while (true) {
			try (Socket client = serverSocket.accept()) {
				handleClient(client);
			}
		}
	}

	/**
	 * Reads in information from client socket, parses and responds.
	 * @param client Socket connection, opened for communication over TCP
	 */ 
	private static void handleClient(Socket client) throws IOException 
	{
		StringBuilder requestBuilder = readFromClient(client);

		//Process client message, breakdown.
		String request = requestBuilder.toString();
		System.out.printf("The request is: %s \r\n", request);

		String[] requestsLines = request.split("\r\n");
		String[] requestLine = requestsLines[0].split(" ");
		String method = requestLine[0];
		String path = requestLine[1];
		String version = requestLine[2];
		String host = requestsLines[1].split(" ")[1];
		
		// Parse client information and send response according to request method.
		if (method.equals("POST")) {
			parsePOST(requestsLines, requestLine, method, path, version, host, client);
			sendPOST(client, path);
		}

		else if (method.equals("GET")) {
			parseGET(requestsLines, requestLine, method, path, version, host, client);
			sendGET(client, path);
		}
	}

	/**
	 * Reads information from client socket, returns message received.
	 * @param client Socket connection, opened for communication over TCP
	 * @return StringBuilder representing message object of client information
	 */ 
	private static StringBuilder readFromClient(Socket client) throws IOException
	{
		BufferedReader br = new BufferedReader(
			new InputStreamReader(client.getInputStream()));
		StringBuilder requestBuilder = new StringBuilder();

		// Avoid empty incoming headers.
		while (br.ready()) {
			char c = (char) br.read();
			requestBuilder.append(c);
		}

		return requestBuilder;
	}

	/**
	 * Parses a GET-type client request.
	 * @param requestsLines String[] split information on "/r/n"
	 * @param requestLine String[] split first line of information on " "
	 * @param method String GET or POST for the scope of this project
	 * @param path String directory path in request
	 * @param version String of HTTP version
	 * @param host String representing address:port at opened server-client socket
	 * @param client Socket connection, opened for communication over TCP
	 */ 
	private static void parseGET(String[] requestsLines, String[] requestLine, String method,
		String path, String version, String host, Socket client) throws IOException
	{
		// Same actions for either path value for GET.
		if (path.equals("/chat/") || path.equals("/login/")) {
			List<String> headers = new ArrayList<>();
			for (int h = 2; h < requestsLines.length; h++) {
				String header = requestsLines[h];
				headers.add(header);
			}

			String accessLog = String.format("Client %s, method %s, "
				+ "path %s, version %s, host %s, headers %s\n",
				client.toString(), method, path, version, host, headers.toString());
			System.out.println(accessLog + "\n");
		}
	}

	/**
	 * Parses a POST-type client request.
	 * @param requestsLines String[] split information on "/r/n"
	 * @param requestLine String[] split first line of information on " "
	 * @param method String GET or POST for the scope of this project
	 * @param path String directory path in request
	 * @param version String of HTTP version
	 * @param host String representing address:port at opened server-client socket
	 * @param client Socket connection, opened for communication over TCP
	 */ 
	private static void parsePOST(String[] requestsLines, String[] requestLine, String method,
		String path, String version, String host, Socket client) throws IOException 
	{
		if (path.equals("/chat/"))
		{
			String cookie = "";
			String userID = "";

			List<String> headers = new ArrayList<>();
			for (int h = 2; h < requestsLines.length - 2; h++) {
				String header = requestsLines[h];

				if (header.contains("Cookie")) {
					cookie = header;
					String cookieSplit = cookie.split(" ")[1];
					userID = cookieSplit.split("=")[1];
					currentUser = userID;
				}

				headers.add(header);
			}

			String message = requestsLines[requestsLines.length - 1];
			String actualMsg = message.split("=")[1];

			String accessLog = String.format("Client %s, method %s, " 
				+ " path %s, version %s, host %s, headers %s ," 
				+ " body %s, userID %s\n",
				client.toString(), method, path, version, host,
				headers.toString(), message, userID);
			System.out.println(accessLog + "\n");

			if (!userID.equals(""))
			{
				//Post message to log
				validLogin = true;
				BufferedWriter out = null;

				try {
					// Use true option to append to file instead of overwriting.
					FileWriter fstream = new FileWriter("./chat/log.txt", true); 
					out = new BufferedWriter(fstream);
					out.write(currentUser + "," + actualMsg + "\n");
					out.close();
				} catch (IOException e) {
					// Nonexistent file, create first.
					final String fileName = "log.txt";
					final File file = new File("./chat/", fileName);
					file.createNewFile();

					FileWriter fstream = new FileWriter("/chat/log.txt", true);
					out = new BufferedWriter(fstream);
					out.write(currentUser + "," + actualMsg + "\n");
					out.close();
				}
			}
		}	//End POST-chat.

		else if (path.equals("/login/")) 
		{
			List<String> headers = new ArrayList<>();
			for (int h = 2; h < requestsLines.length - 2; h++) {
				String header = requestsLines[h];
				headers.add(header);
			}

			String body = requestsLines[requestsLines.length - 1];

			String accessLog = String.format("Client %s, method %s, " + 
				"path %s, version %s, host %s, headers %s, body %s\n",
				client.toString(), method, path, 
				version, host, headers.toString(), body);
			System.out.println(accessLog + "\n");

			String[] loginCreds = body.split("&");
			String[] user = loginCreds[0].split("=");
			String[] password = loginCreds[1].split("=");

			String credentialsPath = "login/credentials.txt";
			Scanner scanner = new Scanner(new File(credentialsPath));

			while (scanner.hasNextLine()) 
			{
				String line = scanner.nextLine();
				String[] currCreds = line.split(",");

				if (user[1].equals(currCreds[0])) 
				{
					if (password[1].equals(currCreds[1])) {
						validLogin = true;
						currentUser = user[1];
						break;
					}
				}
			}

			if (validLogin) {
				System.out.println("Successfully logged in.");
			}
			else {
				System.out.println("Sorry, invalid login.");
			}
		}	//End POST-login.
	}

	/**
	 * Sends a response to client for GET-type request.
	 * @param client Socket connection, opened for communication over TCP
	 * @param path String directory path in request
	 */ 
	private static void sendGET(Socket client, String path) throws IOException 
	{
		if (path.equals("/login/")) 
		{
			String loginPath = "/login/login.html";

			Path filePath = getFilePath(loginPath);
			if (Files.exists(filePath)) {
				String contentType = guessContentType(filePath);
				sendResponse(client, "200 OK", contentType, 
					Files.readAllBytes(filePath), "");
			} else {
				// 404
				byte[] notFoundContent = "<h1>Not found :(</h1>".getBytes();
				sendResponse(client, "404 Not Found", "text/html", notFoundContent, "");
			}
		}	//End GET-login.

		else if (path.equals("/chat/")) 
		{
			ByteArrayOutputStream output = new ByteArrayOutputStream();
			String logPath = "./chat/log.txt";
			Scanner scanner = new Scanner(new File(logPath));

			byte[] html1 = ("<html>\n<body>\n<h1>Chat Page for CS352</h1>\n<p>\n"
				+ "    Chat Space  :\n</p>\n<div id=\"chat-window\">\n").getBytes();
			output.write(html1);

			while (scanner.hasNextLine()) {
				String line = scanner.nextLine();
				String[] message = line.split(",");
				byte[] messageByte = (" <p>" + message[0] + ": " 
					+ message[1] + "</p>\n").getBytes();
				output.write(messageByte);
			}

			byte[] html2 = ("</div>\n<form action=\"/\" method=\"post\">\n"
				+ "   <p>Enter Message : </p>\n   <input type=\"text\" name=\"message\">\n"
				+ "    <p></p>\n   <input type=\"submit\" value=\"Enter\">\n</form>\n</body>\n"
				+ "</html>\n").getBytes();
			output.write(html2);
			byte[] out = output.toByteArray();
			sendResponse(client, "200 OK", "text/html", out, "");
		}	//End GET-chat.
	}

	/**
	 * Sends a response to client for POST-type request.
	 * @param client Socket connection, opened for communication over TCP
	 * @param path String directory path in request
	 */ 
	private static void sendPOST(Socket client, String path) throws IOException 
	{
		if (path.equals("/login/"))
		{
			if (validLogin) {
				String chatPath = "/chat/chat.html";
				Path filePath = getFilePath(chatPath);
				if (Files.exists(filePath)) {
					String contentType = guessContentType(filePath);
					hasCookie = true;
					sendResponse(client, "200 OK", contentType, 
					Files.readAllBytes(filePath), currentUser);
				}
			} 
			else {
				String errorPath = "/login/error.html";
				Path filePath = getFilePath(errorPath);
				if (Files.exists(filePath)) {
					String contentType = guessContentType(filePath);
					sendResponse(client, "200 OK", contentType, 
					Files.readAllBytes(filePath), "");
				}
			}
		}	//End SEND-login.

		else if(path.equals("/chat/")) 
		{
			if (validLogin) {
				String chatPath = "/chat/chat.html";
				Path filePath = getFilePath(chatPath);
				if (Files.exists(filePath)) {
					String contentType = guessContentType(filePath);
					hasCookie = true;
					sendResponse(client, "200 OK", contentType, 
						Files.readAllBytes(filePath), currentUser);
				}
			}
			else {
				String errorPath = "/login/error.html";
				Path filePath = getFilePath(errorPath);
				if (Files.exists(filePath)) {
					String contentType = guessContentType(filePath);
					sendResponse(client, "200 OK", contentType, 
						Files.readAllBytes(filePath), "");
				}
			}
		}	//END SEND-chat.
	}

	/**
	 * Writes output to client terminal.
	 * @param client Socket connection, opened for communication over TCP
	 * @param status String to represent validity status of information
	 * @param contentType String for type of media from current request
	 * @param content String messages represented in byte array format
	 * @param userID String username for login
	 */ 
	private static void sendResponse(Socket client, String status, 
		String contentType, byte[] content, String userID) throws IOException 
	{
		try {
			OutputStream toClient = client.getOutputStream();
			toClient.write(("HTTP/1.1 200 OK" + status + "\r\n").getBytes());
			if (hasCookie) {
				toClient.write(("Set-Cookie" + ": " + userID + "\r\n").getBytes());
			}
			toClient.write(("ContentType: " + contentType + "\r\n").getBytes());
			toClient.write("\r\n".getBytes());
			toClient.write(content);
			toClient.write("\r\n\r\n".getBytes());
			toClient.flush();
			client.close();
		} catch (IOException ex) {
			System.out.println(ex.getStackTrace());
		}
	}

	/**
	 * Return a formatted Path variable given a String filename.
	 * @param path String for directory location
	 * @return Path variable to appropriate directory, may or may not exist
	 */ 
	private static Path getFilePath(String path) {
		if ("/".equals(path)) {
			path = "/index.html";
		}

		return Paths.get("./", path);
	}

	/**
	 * Probe for the media type using built-in methods.
	 * @param filePath Path variable for given directory
	 * @return String representing media type of current request
	 */ 
	private static String guessContentType(Path filePath) throws IOException {
		return Files.probeContentType(filePath);
	}
}
