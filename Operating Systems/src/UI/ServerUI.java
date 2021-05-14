package UI;

import java.awt.BorderLayout;
import java.awt.EventQueue;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;
import me.alexpanov.net.FreePortFinder;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;

import javax.swing.JLabel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import java.awt.Font;
import javax.swing.JButton;
import javax.swing.JTextArea;
import javax.swing.JScrollPane;
import java.awt.Color;

public class ServerUI extends JFrame implements ActionListener {	

	// Fields for the socket
	public static SimpleDateFormat formatter = new SimpleDateFormat("[MM/hh/yy hh:mm a]"); // Field that will format the date and time
	private static HashMap<String, PrintWriter> connectedClients = new HashMap<>();
	private static final int MAX_CONNECTED = 50; // Initializes maximum users allowed, can be changed in the code
	private static int PORT;
	private static ServerSocket server;
	private static volatile boolean exit = false;

	// Fields used for Window Builder
	private JPanel contentPane;
	private JTextArea txtAreaLogs;
	private JButton btnStart;
	private JLabel lblChatServer;

	// Main method to launch the application
	public static void main(String[] args) {
		EventQueue.invokeLater(new Runnable() {
			public void run() {
				try {
					ServerUI frame = new ServerUI();
					UIManager.setLookAndFeel("com.sun.java.swing.plaf.windows.WindowsLookAndFeel");
					SwingUtilities.updateComponentTreeUI(frame);
					// Logs
					System.setOut(new PrintStream(new TextAreaOutputStream(frame.txtAreaLogs)));
					frame.setVisible(true);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});
	}

	// Constructor for the GUI
	public ServerUI() {
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		setBounds(150, 150, 720, 480); // Size of the window
		contentPane = new JPanel();
		contentPane.setBorder(new EmptyBorder(5, 5, 5, 5));
		contentPane.setBackground(new Color(21,32,43)); // Color of the border + upper side
		setContentPane(contentPane);
		contentPane.setLayout(new BorderLayout(0, 0));

		lblChatServer = new JLabel("USTP CHAT SERVER"); // Header text
		lblChatServer.setHorizontalAlignment(SwingConstants.CENTER);
		lblChatServer.setFont(new Font("Tahoma", Font.PLAIN, 40));
		lblChatServer.setForeground(Color.WHITE); // Color of the font
		contentPane.add(lblChatServer, BorderLayout.NORTH);

		btnStart = new JButton("START"); // Text for the "START" button
		btnStart.addActionListener(this);
		btnStart.setFont(new Font("Tahoma", Font.PLAIN, 30));
		contentPane.add(btnStart, BorderLayout.SOUTH);
		btnStart.setBackground(new Color(21,32,43)); // Color of the borders for the "START" button

		JScrollPane scrollPane = new JScrollPane();
		contentPane.add(scrollPane, BorderLayout.CENTER);

		txtAreaLogs = new JTextArea();
		txtAreaLogs.setBackground(new Color(208,181,41)); // Background color of the text/chats
		txtAreaLogs.setForeground(Color.BLACK); // Foreground color of the text/chats
		txtAreaLogs.setLineWrap(true);
		scrollPane.setViewportView(txtAreaLogs);
	}

	@Override
	public void actionPerformed(ActionEvent e) { // This method will be implemented if actions are triggered
		if(e.getSource() == btnStart) {
			if(btnStart.getText().equals("START")) {
				exit = false;
				getRandomPort(); // Calls the method for randomizing port
				start(); // Calls start() method
				btnStart.setText("STOP"); // Changes text to "STOP"
			}else {
				addToLogs("USTP Chat Server stopped..."); // Prints text
				exit = true;
				btnStart.setText("START"); // Changes text to "START"
			}
		}
		
		// Refresh UI
		refreshUIComponents();
	}
	
	public void refreshUIComponents() {
		lblChatServer.setText("USTP CHAT SERVER" + (!exit ? ": "+PORT:": OFFLINE")); // Header text when there is a status change
	}

	public static void start() {
		new Thread(new ServerHandler()).start(); // Calls ServerHandler class
	}

	public static void stop() throws IOException {
		if (!server.isClosed()) server.close();
	}

	private static void broadcastMessage(String message) {
		for (PrintWriter p: connectedClients.values()) {
			p.println(message);
		}
	}
	
	public static void addToLogs(String message) { // Prints out the date and time
		System.out.printf("%s %s\n", formatter.format(new Date()), message);
	}

	private static int getRandomPort() { // Method for randomizing port
		int port = FreePortFinder.findFreeLocalPort(); // Calls in the FreePortFinder class, then the findFreeLocalPort() method
		PORT = port;
		return port; // Returns the randomized port
	}
	
	private static class ServerHandler implements Runnable{ // This class will implement the Runnable class from the JRE library
		@Override
		public void run() {
			try {
				server = new ServerSocket(PORT);
				addToLogs("USTP Server started on port: " + PORT); // Text that pops up when started
				addToLogs("Now looking for students...");
				while(!exit) {
					if (connectedClients.size() <= MAX_CONNECTED){ // Accepts new accounts when the server doesn't exceed allowed users
						new Thread(new ClientHandler(server.accept())).start();
					}
				}
			}
			catch (Exception e) {
				addToLogs("\nError occured: \n");
				addToLogs(Arrays.toString(e.getStackTrace()));
				addToLogs("\nExiting...");
			}
		}
	}
	
	// Start of Client Handler class
	private static class ClientHandler implements Runnable { // This class will implement the Runnable class from the JRE library
		private Socket socket;
		private PrintWriter out;
		private BufferedReader in;
		private String name;
		
		public ClientHandler(Socket socket) {
			this.socket = socket;
		}
		
		@Override
		public void run(){
			addToLogs("Student connected: " + socket.getInetAddress());
			try {
				in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
				out = new PrintWriter(socket.getOutputStream(), true);
				for(;;) {
					name = in.readLine();
					if (name == null) {
						return;
					}
					synchronized (connectedClients) {
						if (!name.isEmpty() && !connectedClients.keySet().contains(name)) break; // Checks if the name is not empty and name does not exist elsewhere
						else out.println("INVALIDNAME"); // Text to show in-case of an empty and existing name
					}
				}
				out.println("Welcome to USTP chat group, " + name.toUpperCase() + "!"); // Welcoming message
				addToLogs(name.toUpperCase() + " has joined.");
				broadcastMessage("[SYSTEM] " + name.toUpperCase() + " has joined.");
				connectedClients.put(name, out);
				String message;
				out.println("You may join the chat now...");
				while ((message = in.readLine()) != null && !exit) {
					if (!message.isEmpty()) {
						if (message.toLowerCase().equals("/quit")) break;
						broadcastMessage(String.format("[%s] %s", name, message));
					}
				}
			} catch (Exception e) {
				addToLogs(e.getMessage());
			} finally {
				if (name != null) {
					addToLogs(name + " is leaving");
					connectedClients.remove(name);
					broadcastMessage(name + " has left");
				}
			}
		}
	}

}
