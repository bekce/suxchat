import java.io.*;
import java.util.*;
import java.net.*;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

import static java.lang.System.out;

public class ChatClient extends JFrame implements ActionListener, KeyListener {
	String uname;
	PrintWriter pw;
	BufferedReader br;
	JTextArea taMessages;
	JTextField tfInput;
	JButton btnSend, btnExit;
	Socket client;

	public ChatClient(String uname, String servername) throws Exception {
		super(uname); // set title for frame
		this.uname = uname;
		client = new Socket(servername, 9999);
		br = new BufferedReader(new InputStreamReader(client.getInputStream()));
		pw = new PrintWriter(client.getOutputStream(), true);
		pw.println(uname); // send name to server
		buildInterface();
		taMessages.append("Welcome to SUXCHAT. Type U for current users list.\n");
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
				pw.println("EXIT");
				System.exit(0);
			}
		});
		setVisible(true);
		tfInput.requestFocus();
		pack();
	}

	public void actionPerformed(ActionEvent evt) {
		if (evt.getSource() == btnExit) {
			pw.println("EXIT"); // send end to server so that server know about
								// the termination
			System.exit(0);
		} else if (evt.getSource() == btnSend) {
			// send message to server
			pw.println(tfInput.getText());
			tfInput.setText("");
		}

	}

	public static void main(String... args) {

		// take username from user
		String name = JOptionPane.showInputDialog(null, "Enter your name :",
				"Username", JOptionPane.PLAIN_MESSAGE);
		if (name==null || name.equals(""))
			name = "user_"+UUID.randomUUID().toString().substring(0,8);
		String servername = "localhost";
		if(args.length>0)
			servername = args[0];
		try {
			new ChatClient(name, servername);
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
					taMessages.append(line + "\n");
				} // end of while
			} catch (Exception ex) {
			}
		}
	}

	@Override
	public void keyPressed(KeyEvent e) {
		// TODO Auto-generated method stub
		if (e.getKeyCode() == KeyEvent.VK_ENTER) {
			btnSend.doClick();
		}
	}

	@Override
	public void keyReleased(KeyEvent e) {
		// TODO Auto-generated method stub

	}

	@Override
	public void keyTyped(KeyEvent e) {
		// TODO Auto-generated method stub

	}
} // end of client