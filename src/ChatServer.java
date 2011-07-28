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

	public static void main(String... args) throws Exception {
		new ChatServer().process();
	} // end of main

	public void process() throws Exception {
		ServerSocket server = new ServerSocket(9999, 10);
		Runtime.getRuntime().addShutdownHook(new ShutdownThread());
		out.println("Server Started...");
		while (true) {
			Socket client = server.accept();
			log.info("Client accepted. "+client.getInetAddress().toString());
			HandleClient c = new HandleClient(client);
			clients.add(c);
			broadcast("", "<< "+c.name+" entered chat room. Total of "+users.size()+" users online. >>");
		} // end of while
	}
	
	public HandleClient findClientByName(String name){
		for (HandleClient cl:clients){
			if(cl.name.equals(name)){
				return cl;
			}
		}
		return null;
	}


	public void broadcast(String message){
		for (HandleClient c : clients)
			c.sendMessage(message);
	}
	
	public void broadcast(String user, String message) {
		// send message to all connected users
		String outputText = "["+sdf.format(Calendar.getInstance().getTime())+"] "+user + ": " + message;
		for (HandleClient c : clients)
			c.sendMessage(outputText);
		System.out.println(outputText);
	}
	
	class ShutdownThread extends Thread {
		public void run(){
			broadcast("EXIT");
		}
	}

	class HandleClient extends Thread {
		String name = "";
		BufferedReader input;
		PrintWriter output;
		boolean operator;

		public HandleClient(Socket client) throws Exception {
			// get input and output streams
			input = new BufferedReader(new InputStreamReader(
					client.getInputStream()));
			output = new PrintWriter(client.getOutputStream(), true);
			// read name
			name = input.readLine();
			users.add(name); // add to vector
			operator = false;
			start();
		}

		public void sendMessage(String msg) {
			output.println(msg);
		}

		public void run() {
			String line;
			try {
				while (true) {
					line = input.readLine();
					if (line.equals("EXIT")) {
						throw new Exception(name+" left chat room. ");
//						clients.remove(this);
//						users.remove(name);
//						broadcast("", "<< "+name+" has left chat room. Total of "+users.size()+" users online. >>");
//						break;
					}
					else if (line.equals("U")){
						StringBuilder str = new StringBuilder();
						for (String usr : users){
							str.append(usr+",");
						}
						sendMessage("Logged Users: " + str.toString());
					}
					else if(line.startsWith("AUTH ")){
						if(line.endsWith("password")){
							operator = true;
							sendMessage("You're now an operator.");							
						}
						else{
							sendMessage("Wrong password. ");
						}
					}
					else if(line.startsWith("K ") && operator){
						boolean found = false;
						for (String user:users){
							if(line.endsWith(user)){
								users.remove(user);
								HandleClient cl =findClientByName(user); 
								cl.sendMessage("EXIT");
								cl.output.close();
								clients.remove(cl);
								sendMessage("User kicked. ");
								found = true;
								break;
							}
						}
						if(!found){
							sendMessage("User not found. ");
						}
					}
					else
						broadcast(name, line); // method of outer class - send
											// messages to all
				} // end of while
			} // try
			catch (Exception ex) {
				log.info(ex.getMessage());
			}
			finally {
				output.close();
				clients.remove(this);
				users.remove(name);
				broadcast("", "<< "+name+" has left chat room. Total of "+users.size()+" users online. >>");
			}
		} // end of run()
	} // end of inner class

} // end of Server