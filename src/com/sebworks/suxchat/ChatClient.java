/* 
   Copyright 2012, Selim Eren Bekce, www.sebworks.com

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
 */
package com.sebworks.suxchat;

import static java.lang.System.out;

import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.nio.charset.Charset;
import java.util.Properties;
import java.util.UUID;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;

/**
 * SUXCHAT client part. 
 * @author selimerenbekce, www.sebworks.com
 * @version 3.7
 */
public class ChatClient extends JFrame implements ActionListener, KeyListener {
	private static final long serialVersionUID = 1L;
	OutputStreamWriter pw;
	BufferedReader br;
	JTextArea taMessages;
	JTextField tfInput;
	JButton btnSend, btnExit;
	Socket client;
	boolean activeWindow;
	static ChatClient instance;

	static String confFileName = "client.properties";
	static String serverAddress;
	static String userName;
	static int port;

	public static void main(String... args) throws Exception {
		Properties props = new Properties();
		props.load(ChatClient.class.getResourceAsStream(confFileName));
		
		port = Integer.parseInt(props.getProperty("client.port"));
		serverAddress = props.getProperty("client.default.server");

		serverAddress = (String) JOptionPane.showInputDialog(null, "Enter server address:", "Servername", JOptionPane.PLAIN_MESSAGE, null,
				null, serverAddress);
		
		if (serverAddress == null)
			System.exit(0);

		if (serverAddress.isEmpty())
			serverAddress = props.getProperty("client.default.server");
		
//		props.setProperty("client.default.server", serverAddress);
//		props.store(new FileOutputStream(ChatClient.class.getResource(confFileName).toString()),null);

		userName = (String) JOptionPane.showInputDialog(null, "Enter your name :", "Username", JOptionPane.PLAIN_MESSAGE, null,
				null, System.getProperty("user.name"));

		if (userName == null)
			System.exit(0);
		
		if (userName.isEmpty())
			userName = "user_" + UUID.randomUUID().toString().substring(0, 8);
		userName = userName.replaceAll(" ", "_");

		try {
			instance = new ChatClient();
		} catch (Exception ex) {
			out.println("Error --> " + ex.getMessage());
		}

	} // end of main

	public ChatClient() throws Exception {
		super(userName); // set title for frame
		client = new Socket(serverAddress, port);
		client.setKeepAlive(true);
		br = new BufferedReader(new InputStreamReader(client.getInputStream(), Charset.forName("UTF-8")));
		pw = new OutputStreamWriter(client.getOutputStream(), Charset.forName("UTF-8"));
		write(userName); // send name to server
		buildInterface();
		taMessages.append("Welcome to SUXCHAT. Type 'U' for logged users, 'P <user> <msg>' for private messaging.\n");
		Runtime.getRuntime().addShutdownHook(new ShutdownThread());
		new MessagesThread().start(); // create thread for listening for
										// messages
	}

	public void buildInterface() {
		btnSend = new JButton("Send");
		btnExit = new JButton("Exit");
		taMessages = new JTextArea();
		taMessages.setRows(10);
		taMessages.setColumns(50);
		taMessages.setEditable(false);
		taMessages.setAutoscrolls(true);
		tfInput = new JTextField(50);
		tfInput.addKeyListener(this);
		JScrollPane sp = new JScrollPane(taMessages, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
		add(sp, "Center");
		JPanel bp = new JPanel(new FlowLayout());
		bp.add(tfInput);
		bp.add(btnSend);
		bp.add(btnExit);
		add(bp, "South");
		btnSend.addActionListener(this);
		btnExit.addActionListener(this);
		setSize(500, 300);
		this.addWindowListener(new WindowAdapter() {
			@Override
			public void windowClosing(WindowEvent e) {
				System.exit(0);
			}

			@Override
			public void windowDeactivated(WindowEvent arg0) {
				activeWindow = false;
				super.windowDeactivated(arg0);
			}

			@Override
			public void windowActivated(WindowEvent e) {
				activeWindow = true;
				super.windowActivated(e);
			}

		});
		setVisible(true);
		activeWindow = true;
		tfInput.requestFocus();
		pack();
	}

	public void actionPerformed(ActionEvent evt) {
		if (evt.getSource() == btnExit) {
			System.exit(0);
		} else if (evt.getSource() == btnSend) {
			// send message to server
			try {
				String msg = tfInput.getText();
				write(msg);
				if(msg.startsWith("P ")){
					int index=msg.indexOf(" ", 2);
					tfInput.setText(msg.substring(0,index+1));
				}
				else
					tfInput.setText("");
			} catch (IOException e) {
				JOptionPane.showMessageDialog(instance, "Message send error. Try again. ");
			}
		}

	}

	// inner class for Messages Thread
	class MessagesThread extends Thread {
		public void run() {
			String line;
			try {
				while (true) {
					line = br.readLine();
					if (line.equals("EXIT")) {
						append("Server dropped connection. ");
						tfInput.setEnabled(false);
						btnSend.setEnabled(false);
						pw.close();
						break;
					}
					else if(line.equals("KEEPALIVE")){
						write("KEEPALIVE");
					}
					else{
						if (!activeWindow) {
							JDialog dialog = new JDialog(instance);
							dialog.setTitle("Alert");
							dialog.addWindowListener(new WindowAdapter() {
								public void windowOpened(WindowEvent e) {
									e.getWindow().dispose();
									super.windowOpened(e);
								}
							});
							dialog.setModal(true);
							dialog.setVisible(true);
						}
						append(line + "\n");
					}
				} // end of while
			} catch (Exception ex) {
			}
		}
	}

	class ShutdownThread extends Thread {
		public void run() {
			try {
				pw.write("EXIT");
				pw.flush();
				pw.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	public void keyPressed(KeyEvent e) {
		if (e.getKeyCode() == KeyEvent.VK_ENTER) {
			btnSend.doClick();
		}
	}

	public void keyReleased(KeyEvent e) {
	}

	public void keyTyped(KeyEvent e) {
	}
	
	public void write(String msg) throws IOException{
		pw.write(msg + "\n"); // send name to server
		pw.flush();
	}
	
	public void append(String msg){
		taMessages.append(msg);
		taMessages.select(taMessages.getDocument().getLength(), taMessages.getDocument().getLength());
	}
	
} // end of client