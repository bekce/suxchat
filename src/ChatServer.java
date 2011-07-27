// Chat Server runs at port no. 9999
import java.io.*;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.logging.Logger;
import java.net.*;
import static java.lang.System.out;

public class ChatServer {
	Vector<String> users = new Vector<String>();
	Vector<HandleClient> clients = new Vector<HandleClient>();
	Logger log = Logger.getAnonymousLogger();
	SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss");

	public void process() throws Exception {
		ServerSocket server = new ServerSocket(9999, 10);
		out.println("Server Started...");
		while (true) {
			Socket client = server.accept();
			log.info("Client accepted. "+client.getInetAddress().toString());
			HandleClient c = new HandleClient(client);
			clients.add(c);
			broadcast("", "<< "+c.name+" entered chat room. Total of "+users.size()+" users online. >>");
		} // end of while
	}

	public static void main(String... args) throws Exception {
		new ChatServer().process();
	} // end of main

	public void broadcast(String user, String message) {
		// send message to all connected users
		String outputText = "["+sdf.format(Calendar.getInstance().getTime())+"] "+user + ": " + message;
		for (HandleClient c : clients)
			c.sendMessage(outputText);
		System.out.println(outputText);
	}

	class HandleClient extends Thread {
		String name = "";
		BufferedReader input;
		PrintWriter output;

		public HandleClient(Socket client) throws Exception {
			// get input and output streams
			input = new BufferedReader(new InputStreamReader(
					client.getInputStream()));
			output = new PrintWriter(client.getOutputStream(), true);
			// read name
			name = input.readLine();
			users.add(name); // add to vector
			start();
		}

		public void sendMessage(String msg) {
			output.println(msg);
		}

		public String getUserName() {
			return name;
		}

		public void run() {
			String line;
			try {
				while (true) {
					line = input.readLine();
					if (line.equals("EXIT")) {
						clients.remove(this);
						users.remove(name);
						broadcast("", "<< "+name+" has left chat room. Total of "+users.size()+" users online. >>");
						break;
					}
					else if (line.equals("U")){
						StringBuilder str = new StringBuilder();
						for (String usr : users){
							str.append(usr+",");
						}
						sendMessage("Logged Users: " + str.toString());
					}
					else
						broadcast(name, line); // method of outer class - send
											// messages to all
				} // end of while
			} // try
			catch (Exception ex) {
				System.out.println(ex.getMessage());
			}
		} // end of run()
	} // end of inner class

} // end of Server