package chat;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.Charset;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Properties;
import java.util.StringTokenizer;
import java.util.Timer;
import java.util.TimerTask;
import java.util.Vector;
import java.util.logging.Logger;

public class ChatServer {
	Vector<String> users = new Vector<String>();
	Vector<HandleClient> clients = new Vector<HandleClient>();
	Logger log = Logger.getAnonymousLogger();
	SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss");
	
	String confFileName = "server.properties";
	boolean stealthMode, entrancePasswordEnabled, logsEnabled;
	String entrancePassword, operatorPassword, serverMessage;
	int port;

	public static void main(String... args) throws Exception {
		final ChatServer instance = new ChatServer();
		instance.configure();
		instance.process();
	}
	
	public void configure() throws Exception{
		log.info("Loading external configuration from "+confFileName+".");
		Properties props = new Properties();
		props.load(ChatClient.class.getResourceAsStream(confFileName));
		stealthMode=Boolean.parseBoolean(props.getProperty("server.stealth"));
		entrancePasswordEnabled=Boolean.parseBoolean(props.getProperty("server.entrance.password.enabled"));
		logsEnabled=Boolean.parseBoolean(props.getProperty("server.logs.enabled"));
		entrancePassword=props.getProperty("server.entrance.password");
		operatorPassword=props.getProperty("server.operator.password");
		serverMessage=props.getProperty("server.message");
		port=Integer.parseInt(props.getProperty("server.port"));
	}

	public void process() throws Exception {
		ServerSocket server = new ServerSocket(port, 10);
		Runtime.getRuntime().addShutdownHook(new ShutdownThread());
		new Timer().scheduleAtFixedRate(new KeepAliveTimer(), 300000, 300000);
		log.info("ChatServer Started...");
		while (true) {
			Socket client = server.accept();
			client.setKeepAlive(true);
			log.info("Client accepted from "+client.getInetAddress().getHostName());
			new HandleClient(client);
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
		for (HandleClient c : clients)
			c.sendMessage(user + ": " + message,true);
		System.out.println(user + ": " + message);
	}
	
	class ShutdownThread extends Thread {
		public void run(){
			try {
				System.out.println("Shutting down ChatServer..");
				broadcast("EXIT");
				Thread.sleep(1000);
			} catch (Exception e) {
				log.info(e.getMessage());
			} 
		}
	}
	
	class KeepAliveTimer extends TimerTask{
		public void run() {
			try {
				broadcast("KEEPALIVE");
				//log.info("KEEPALIVE sent.");
			} catch (IOException e) {
				log.info(e.getMessage());
			}
		}
	}

	class HandleClient extends Thread {
		String name = "";
		BufferedReader input;
		OutputStreamWriter output;
		Socket clientSocket;
		boolean operator;

		public HandleClient(Socket clientSocket) throws Exception {
			this.clientSocket = clientSocket;
			operator = false;
			// get input and output streams
			input = new BufferedReader(new InputStreamReader(clientSocket.getInputStream(),Charset.forName("UTF-8"))); 
			output = new OutputStreamWriter(clientSocket.getOutputStream(),Charset.forName("UTF-8"));
			// read name
			name = input.readLine();
			if (users.contains(name)) {
				sendMessage("There is already a user with your name. Please relog with a different name.");
				sendMessage("EXIT");
				return;
			}
			if (entrancePasswordEnabled) {
				try{
					sendMessage("This is a private server. Please enter entrance password to login.");
					int retryCount = 2;
					for (; retryCount >= 0; retryCount--) {
						String pass = input.readLine();
						if (!entrancePassword.equals(pass)) {
							if (retryCount==0) {
								sendMessage("Sorry you're out.");
								sendMessage("EXIT");
							}
							else{
								sendMessage("Wrong password. You have "+(retryCount) + (retryCount==1?" retry":" retries")+" remaining");								
							}
						}
						else{
							sendMessage("Password accepted.");
							users.add(name); // add to vector
							start();
							clients.add(this);
							broadcast("", "<< "+name+" entered chat room. Total of "+users.size()+" users online. >>");
							break;
						}
					}
				}catch (Exception e) {
					//this client will be gc'ed.
				}
			}
			else{
				users.add(name); // add to vector				
				start();
				clients.add(this);
				broadcast("", "<< "+name+" entered chat room. Total of "+users.size()+" users online. >>");
			}
		}
		
		public void sendMessage(String msg, boolean includeTimeStamp) throws IOException {
			if(includeTimeStamp){
				sendMessage("["+sdf.format(Calendar.getInstance().getTime())+"] "+msg);
			}
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
					if(line.isEmpty()){
						continue;
					}
					StringTokenizer tokenizer = new StringTokenizer(line);
					String firstToken = tokenizer.nextToken();
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
					else if(firstToken.equals("AUTH")){
						if (operator) {
							sendMessage("You're already an operator.");
						}
						else if(tokenizer.nextToken().equals(operatorPassword)){
							operator = true;
							sendMessage("You're now an operator.");							
						}
						else{
							sendMessage("Wrong operator password. ");
						}
					}
					else if(firstToken.equals("K") && operator){
						boolean found = false;
						if (!tokenizer.hasMoreTokens()) {
							sendMessage("Kick user: K <user>");
							continue;
						}
						String token = tokenizer.nextToken();
						for (String user:users){
							if (token.equals(name)) {
								sendMessage("You cannot kick yourself.");
								found = true;
								break;
							}
							else if(token.equals(user)){
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
					else if(firstToken.equals("P")){
						boolean found = false;
						String msguser, rest;
						if(tokenizer.hasMoreTokens())
							msguser = tokenizer.nextToken();
						else{
							sendMessage("Private messaging: P <user> <msg>");
							continue;
						}
						if(tokenizer.hasMoreTokens())
							rest = tokenizer.nextToken("");
						else{
							sendMessage("Private messaging: P <user> <msg>");
							continue;
						}
						
						for (String user:users){
							if(msguser.equals(user)){
								HandleClient cl =findClientByName(user); 
								this.sendMessage("<< PM to " + cl.name + " >>: " + rest,true);
								cl.sendMessage("<< PM from " + this.name+ " >>: " +rest,true);
								found = true;
								break;
							}
						}
						if(!found){
							sendMessage("User not found. ");
						}
					}
					else if(line.equals("KEEPALIVE")){
						//ignore
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
					clients.remove(this);
					users.remove(name);
					broadcast("", "<< "+name+" has left chat room. Total of "+(users.size())+" users online. >>");
					clientSocket.close();
				} catch (IOException e) {
					log.info(e.getMessage());
				} finally{
				}
			}
		} // end of run()
	} // end of inner class

} // end of Server