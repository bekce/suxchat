import java.io.*;
import java.util.*;
import java.net.*;
import java.nio.charset.Charset;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

import static java.lang.System.out;

public class ChatClient extends JFrame implements ActionListener, KeyListener {
	private static final long serialVersionUID = 1L;
	String uname;
	OutputStreamWriter pw;
	BufferedReader br;
	JTextArea taMessages;
	JTextField tfInput;
	JButton btnSend, btnExit;
	Socket client;
	boolean activeWindow; 
	static ChatClient instance;

	public ChatClient(String uname, String servername) throws Exception {
		super(uname); // set title for frame
		this.uname = uname;
		client = new Socket(servername, 9999);
		br = new BufferedReader(new InputStreamReader(client.getInputStream()));
		pw = new OutputStreamWriter(client.getOutputStream(), Charset.forName("UTF-8"));
		pw.write(uname+"\n"); // send name to server
		pw.flush();
		buildInterface();
		taMessages.append("Welcome to SUXCHAT. Type U for current users list.\n");
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
		JScrollPane sp = new JScrollPane(taMessages,
				JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
				JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
		sp.getVerticalScrollBar().addAdjustmentListener(
				new AdjustmentListener() {
					public void adjustmentValueChanged(AdjustmentEvent e) {
						e.getAdjustable().setValue(
								e.getAdjustable().getMaximum());
					}
				});
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
//				pw.println("EXIT");
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
//			pw.println("EXIT"); // send end to server so that server know about
//								// the termination
			System.exit(0);
		} else if (evt.getSource() == btnSend) {
			// send message to server
			try {
				pw.write(tfInput.getText()+"\n");
				pw.flush();
				tfInput.setText("");
			} catch (IOException e) {
				JOptionPane.showMessageDialog(instance, "Message send error. Try again. ");
			}
		}

	}

	public static void main(String... args) {

		String servername;
		if(args.length>0)
			servername = args[0];
		else
			servername = JOptionPane.showInputDialog(null, "Enter server address :",
					"Servername", JOptionPane.PLAIN_MESSAGE);
			
		if(servername==null || servername.isEmpty())
			servername = "localhost";
		
		String name = JOptionPane.showInputDialog(null, "Enter your name :",
				"Username", JOptionPane.PLAIN_MESSAGE);
		
		if (name==null || name.equals(""))
			name = "user_"+UUID.randomUUID().toString().substring(0,8);
		
		try {
			instance = new ChatClient(name, servername);
		} catch (Exception ex) {
			out.println("Error --> " + ex.getMessage());
		}

	} // end of main

	// inner class for Messages Thread
	class MessagesThread extends Thread {
		public void run() {
			String line;
			try {
				while (true) {
					line = br.readLine();
					if(!activeWindow){
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
					if(line.equals("EXIT")){
						taMessages.append("Server dropped connection. ");
						tfInput.setEnabled(false);
						btnSend.setEnabled(false);
						pw.close();
						break;
					}
					else
						taMessages.append(line + "\n");
				} // end of while
			} catch (Exception ex) {
			}
		}
	}
	
	class ShutdownThread extends Thread{
		public void run(){
			try {
				pw.write("EXIT");
				pw.flush();
				pw.close();				
			} catch (IOException e) {
				e.printStackTrace();
			} 
		}
	}

	@Override
	public void keyPressed(KeyEvent e) {
		if (e.getKeyCode() == KeyEvent.VK_ENTER) {
			btnSend.doClick();
		}
	}

	@Override
	public void keyReleased(KeyEvent e) {}

	@Override
	public void keyTyped(KeyEvent e) {}
} // end of client