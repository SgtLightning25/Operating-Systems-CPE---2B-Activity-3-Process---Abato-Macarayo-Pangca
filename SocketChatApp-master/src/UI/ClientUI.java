package UI;

import java.awt.BorderLayout;
import java.awt.EventQueue;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Date;
import javax.swing.JLabel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import java.awt.Font;
import javax.swing.JButton;
import javax.swing.JTextArea;
import javax.swing.JScrollPane;
import java.awt.Color;
import java.awt.FlowLayout;
import javax.swing.JTextField;

public class ClientUI extends JFrame implements ActionListener {	

	// Fields for the socket
	private static Socket clientSocket;
	private static int PORT;
	private PrintWriter out;

	// Fields used for Window Builder
	private JPanel contentPane;
	private JTextArea txtAreaLogs;
	private JButton btnStart;
	private JPanel panelNorth;
	private JLabel lblChatClient;
	private JPanel panelNorthSouth;
	private JLabel lblPort;
	private JLabel lblName;
	private JPanel panelSouth;
	private JButton btnSend;
	private JTextField txtMessage;
	private JTextField txtNickname;
	private JTextField txtPort;
	private String clientName;

	// Main method to launch the application
	public static void main(String[] args) {
		EventQueue.invokeLater(new Runnable() {
			public void run() {
				try { // To catch errors
					ClientUI frame = new ClientUI();
					UIManager.setLookAndFeel("com.sun.java.swing.plaf.windows.WindowsLookAndFeel");
					SwingUtilities.updateComponentTreeUI(frame);
					// Sets the output stream from OutputStream (standard) to TextAreaOutputStream
					System.setOut(new PrintStream(new TextAreaOutputStream(frame.txtAreaLogs)));
					frame.setVisible(true);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});
	}

	// Constructor for the GUI
	public ClientUI() {
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		setBounds(100, 100, 570, 400); // Size of the window
		contentPane = new JPanel();
		contentPane.setBorder(new EmptyBorder(5, 5, 5, 5));
		setContentPane(contentPane);
		contentPane.setLayout(new BorderLayout(0, 0));
		contentPane.setBackground(new Color(208,181,41)); // Color of the borders

		panelNorth = new JPanel();
		contentPane.add(panelNorth, BorderLayout.NORTH);
		panelNorth.setLayout(new BorderLayout(0, 0));
		panelNorth.setBackground(new Color(208,181,41)); // Color of the upper side of the window

		lblChatClient = new JLabel("USTP CHAT PORTAL"); // Header text
		lblChatClient.setHorizontalAlignment(SwingConstants.CENTER);
		lblChatClient.setFont(new Font("Tahoma", Font.PLAIN, 40));
		panelNorth.add(lblChatClient, BorderLayout.NORTH);

		panelNorthSouth = new JPanel();
		panelNorth.add(panelNorthSouth, BorderLayout.SOUTH);
		panelNorthSouth.setLayout(new FlowLayout(FlowLayout.CENTER, 5, 5));
		panelNorthSouth.setBackground(new Color(208,181,41)); // Color of the portion just below the upper side of the window

		lblName = new JLabel("Nickname");
		panelNorthSouth.add(lblName);

		txtNickname = new JTextField(); // JTextField of "Nickname"
		txtNickname.setColumns(10);
		panelNorthSouth.add(txtNickname);

		lblPort = new JLabel("Port");
		panelNorthSouth.add(lblPort);

		txtPort = new JTextField(); // JTextField of "Port"
		panelNorthSouth.add(txtPort);
		txtPort.setColumns(10);

		btnStart = new JButton("START"); // Text for the "START" button
		panelNorthSouth.add(btnStart);
		btnStart.addActionListener(this);
		btnStart.setFont(new Font("Tahoma", Font.PLAIN, 12));

		JScrollPane scrollPane = new JScrollPane();
		contentPane.add(scrollPane, BorderLayout.CENTER);

		txtAreaLogs = new JTextArea();
		txtAreaLogs.setBackground(new Color(21,32,43)); // Background color of the text/chats
		txtAreaLogs.setForeground(Color.WHITE); // Foreground color of the text/chats
		txtAreaLogs.setLineWrap(true);
		scrollPane.setViewportView(txtAreaLogs);

		panelSouth = new JPanel();
		FlowLayout fl_panelSouth = (FlowLayout) panelSouth.getLayout();
		fl_panelSouth.setAlignment(FlowLayout.CENTER);
		contentPane.add(panelSouth, BorderLayout.SOUTH);

		txtMessage = new JTextField(); // JTextField for the chat user input
		panelSouth.add(txtMessage);
		txtMessage.setColumns(50);
		panelSouth.setBackground(new Color(208,181,41)); // Color of the lower side of the window

		btnSend = new JButton("SEND"); // Text for the "SEND" button
		btnSend.addActionListener(this);
		btnSend.setFont(new Font("Tahoma", Font.PLAIN, 12));
		panelSouth.add(btnSend);
	}

	@Override
	public void actionPerformed(ActionEvent e) { // This method will be implemented if actions are triggered
		if(e.getSource() == btnStart) {
			if(btnStart.getText().equals("START")) { // IF clicked, it will run the method start() then change the text to "STOP"
				btnStart.setText("STOP");
				start();
			}else { // IF triggered, it will run the method stop() then change the text to "START"
				btnStart.setText("START");
				stop();
			}
		}else if(e.getSource() == btnSend) {
			String message = txtMessage.getText().trim();
			if(!message.isEmpty()) {
				out.println(message);
				txtMessage.setText("");
			}
		}
		// Refresh UI
		refreshUIComponents();
	}

	public void refreshUIComponents() {

	}

	public void start() {
		try {
			PORT = Integer.parseInt(txtPort.getText().trim());
			clientName = txtNickname.getText().trim();
			clientSocket = new Socket("localhost", PORT);
			out = new PrintWriter(clientSocket.getOutputStream(), true);
			new Thread(new Listener()).start();
			// Sends or Prints Nickname
			out.println(clientName);
		} catch (Exception err) {
			addToLogs("[ERROR] "+err.getLocalizedMessage());
		}
	}

	public void stop(){
		if(!clientSocket.isClosed()) {
			try {
				clientSocket.close();
			} catch (IOException e1) {}
		}
	}

	public static void addToLogs(String message) {
		System.out.printf("%s %s\n", ServerUI.formatter.format(new Date()), message); // Prints out the date and time
	}

	private static class Listener implements Runnable { // This class will implement the Runnable class from the JRE library
		private BufferedReader in;
		@Override
		public void run() {
			try {
				in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
				String read;
				for(;;) {
					read = in.readLine();
					if (read != null && !(read.isEmpty())) addToLogs(read);
				}
			} catch (IOException e) {
				return;
			}
		}

	}
}
