// Chat Server runs at port no. 9999
import static java.lang.System.out;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.Charset;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Vector;
import java.util.logging.Logger;

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


	public void broadcast(String message) throws IOException{
		for (HandleClient c : clients)
			c.sendMessage(message);
	}
	
	public void broadcast(String user, String message) throws IOException {
		// send message to all connected users
		String outputText = "["+sdf.format(Calendar.getInstance().getTime())+"] "+user + ": " + message;
		for (HandleClient c : clients)
			c.sendMessage(outputText);
		System.out.println(outputText);
	}
	
	class ShutdownThread extends Thread {
		public void run(){
			try {
				broadcast("EXIT");
			} catch (IOException e) {
				log.info(e.getMessage());
			}
		}
	}

	class HandleClient extends Thread {
		String name = "";
		BufferedReader input;
		OutputStreamWriter output;
		boolean operator;

		public HandleClient(Socket client) throws Exception {
			// get input and output streams
			input = new BufferedReader(new InputStreamReader(client.getInputStream(),Charset.forName("UTF-8"))); 
			output = new OutputStreamWriter(client.getOutputStream(),Charset.forName("UTF-8"));
			// read name
			name = input.readLine();
			users.add(name); // add to vector
			operator = false;
			start();
		}

		public void sendMessage(String msg) throws IOException {
			output.write(msg+"\n");
			output.flush();
		}

		public void run() {
			String line;
			try {
				while (true) {
					line = input.readLine();
					if (line.equals("EXIT")) {
						throw new Exception(name+" left chat room. ");
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
					else if(line.startsWith("P ")){
						boolean found = false;
						String rest = line.substring(2);
						System.out.println("Rest : " +rest);
						
						String msguser = rest.substring(0, rest.indexOf(' '));
						
						System.out.println("User Name : " + msguser);
						for (String user:users){
							if(msguser.equals(user)){
								users.remove(user);
								HandleClient cl =findClientByName(user); 
								cl.sendMessage("<< Private message from " + this.name+ " >>: " +rest.substring(msguser.length()));
								this.sendMessage("<< Private message to " + cl.name + " >>: " +    rest.substring(msguser.length()));
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
				try {
					broadcast("", "<< "+name+" has left chat room. Total of "+users.size()+" users online. >>");
					output.close();
				} catch (IOException e) {
					log.info(e.getMessage());
				} finally{
					clients.remove(this);
					users.remove(name);					
				}
			}
		} // end of run()
	} // end of inner class

} // end of Server