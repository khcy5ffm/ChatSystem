package server;

import static org.junit.Assert.assertEquals;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.util.ArrayList;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import mockit.Mock;
import mockit.MockUp;
import mockit.integration.junit4.JMockit;

@RunWith(JMockit.class)
public class ConnectionTest {
	// all test cases will utilise a maximum of 1 server and 3 sockets
	// to initialise the required connection(s)
	Server fakess;
	Socket fakecs1, fakecs2, fakecs3;
	
	// substitutes list in class Server
	static ArrayList<Connection> cArray = new ArrayList<Connection>();
	Connection c1, c2, c3;
	
	Thread c1Thread, c2Thread, c3Thread;
	
	// substitutes input and output streams for in and out in class Connection
	static InputStream bais[] = new InputStream[3]; // holds String of test user input
	static OutputStream baos[] = new OutputStream[3]; // holds String of output (server messages) to test user input 
	
	String username1 = "marshmellow", username2 = "cupcake", username3 = "jellybean";
	
	static int ACTIVE_CONNECTION = 1; // holds identifier value of currently running (connection) thread
	
	// fake methods for substitution to eliminate dependency on class Server 
	public static final class fakeServer extends MockUp<Server> {
		@Mock
		public void $init(int port) {
		}
		
		@Mock
		public int getNumberOfUsers() {
			return cArray.size();
		}
		
		@Mock
		public ArrayList<String> getUserList() {
			ArrayList<String> userList = new ArrayList<String>();
			for( Connection c: cArray){
				if(c.getState() == Connection.STATE_REGISTERED) {
					userList.add(c.getUserName());
				}
			}
			return userList;
		}
		
		@Mock
		public boolean doesUserExist(String username) {
			boolean result = false;
			for( Connection c: cArray){
				if(c.getState() == Connection.STATE_REGISTERED) {
					if(result = c.getUserName().compareTo(username) == 0)
						break;
				}
			}
			return result;
		}
		
		@Mock
		public void broadcastMessage(String message){
			for( Connection c: cArray){
				c.messageForConnection(message + System.lineSeparator());	
			}
		}
		
		@Mock
		public boolean sendPrivateMessage(String message, String username) {
			for( Connection c: cArray) {
				if(c.getState() == Connection.STATE_REGISTERED) {
					if(c.getUserName().compareTo(username) == 0) {
						c.messageForConnection(message + System.lineSeparator());
						return true;
					}
				}
			}
			return false;
		}
		
		@Mock
		public void removeDeadUsers(){
			for (Connection c: cArray) {
				if(c.isRunning() == false) {
					cArray.remove(c);
					break;
				}
			}
		}
	}
	
	// fake methods for substitution to eliminate dependency on class Socket
	public static final class fakeSocket extends MockUp<Socket> {
		
		@Mock
		public InputStream getInputStream() throws IOException {
			return bais[ACTIVE_CONNECTION - 1];
		}
		
		@Mock
		public OutputStream getOutputStream() throws IOException {
			return baos[ACTIVE_CONNECTION - 1];
		}
	}
	
	public void setInputStream(String messageSource) {
		ConnectionTest.bais[ACTIVE_CONNECTION - 1] = new ByteArrayInputStream(messageSource.getBytes());
	}

	public void setOutputStream() {
		baos[ACTIVE_CONNECTION - 1] = new ByteArrayOutputStream();
	}

	public void setUpConnection(int connectionNum) {
		if(connectionNum == 1) {
			fakecs1 = new Socket();

			c1 = new Connection(fakecs1, fakess);
			cArray.add(c1);
			c1Thread = new Thread(c1);			
		} else if (connectionNum == 2) {
			fakecs2 = new Socket();
			
			c2 = new Connection(fakecs2, fakess);
			cArray.add(c2);
			c2Thread = new Thread(c2);
		} else {
			fakecs3 = new Socket();
			
			c3 = new Connection(fakecs3, fakess);
			cArray.add(c3);
			c3Thread = new Thread(c3);
		}
	}
	
	@Before
	public void setUp() throws Exception {
		// replaces Server and Socket methods with respective fake methods
		new fakeServer();
		new fakeSocket();
		
		fakess = new Server(0);
		
		// initialises one connection at beginning of each test case 
		setUpConnection(1);
	}

	@After
	public void tearDown() throws Exception {
		// empties array of connections for next test case
		cArray.remove(c1);
	}

	// tests connection establishment between server and client
	// expected output is welcome message from server
	@Test
	public void testConnection() {
		ACTIVE_CONNECTION = 1;
		
		setInputStream(""); // no user input
		setOutputStream();
		
		c1Thread.start();
		
		try {			
			Thread.sleep(100);
			
			String welcomeMessage = "OK Welcome to the chat server, there are currelty 1 user(s) online";
			
			String os = fakecs1.getOutputStream().toString();
			
			String line = os.trim();
			
			assertEquals(welcomeMessage, line);
		} catch (IOException e) {
			System.err.println("Error retrieving output stream");
		} catch (InterruptedException e) {
			System.err.println("Error sleeping thread interrupted");
		}
	}
	
	// tests validation of invalid commands
	// expected output are BAD messages from server
	@Test
	public void testBADMessages() {
		// initialises another connection to test username availability
		ACTIVE_CONNECTION = 2;
		
		setUpConnection(2);
		
		setInputStream("IDEN " + username1);
		setOutputStream();

		c2Thread.start();
		
		try {
			Thread.sleep(100);
			
			ACTIVE_CONNECTION = 1;
			
			String messageSource = "LIS\n" // command too short
					+ "LOST\n" // command not found
					+ "list\n" // command badly formatted (valid command format in uppercase)
					+ "LIST\n" // command only available after registration
					+ "HAIL Hello everyone\n" // command only available after registration
					+ "MESG Hi\n" // command only available after registration
					+ "IDEN " + username1 + "\n" // username taken
					+ "IDEN " + username2 + "\n" 
					+ "IDEN " + username2 + "\n" // user already registered
					+ "MESG Hi\n" // private message command badly formatted (no user specified)
					+ "MESG " + username1 + "\n" // private message command badly formatted (no message specified)
					+ "MESG " + username3 + " Hi\n"; // user does not exist
			
			setInputStream(messageSource);
			setOutputStream();
			
			c1Thread.start();
			
			Thread.sleep(100);
			
			String os = fakecs1.getOutputStream().toString();
			
			String[] messages = os.split("\n");
			String serverMessages[] = new String[messages.length];
						
			for(int i = 0; i < messages.length; i++) {
				serverMessages[i] = messages[i].trim();
			}
			
			assertEquals("BAD invalid command to server", serverMessages[1]);
			assertEquals("BAD command not recognised", serverMessages[2]);
			assertEquals("BAD command not recognised", serverMessages[3]);
			assertEquals("BAD You have not logged in yet", serverMessages[4]);
			assertEquals("BAD You have not logged in yet", serverMessages[5]);
			assertEquals("BAD You have not logged in yet", serverMessages[6]);
			assertEquals("BAD username is already taken", serverMessages[7]);
			assertEquals("BAD you are already registerd with username " + username2, serverMessages[9]);
			assertEquals("BAD Your message is badly formatted", serverMessages[10]);
			assertEquals("BAD Your message is badly formatted", serverMessages[11]);
			assertEquals("BAD the user does not exist", serverMessages[12]);
		} catch (IOException e) {
			System.err.println("Error comparing strings for testMessageForConnection()\nOutput stream cannot be retreived");
		} catch (InterruptedException e) {
			System.err.println("Error sleeping thread interrupted");
		}
	}
	
	// tests validation of valid commands
	// expected output are OK messages from server
	@Test
	public void testOKMessages() {
		// initialises another connection to test private messaging functionality
		ACTIVE_CONNECTION = 2;
		
		setUpConnection(2);
		
		setInputStream("IDEN " + username1);
		setOutputStream();

		c2Thread.start();
		
		try {
			Thread.sleep(100);
			
			ACTIVE_CONNECTION = 1;
			
			String messageSource = "STAT\n" // status check before register
					+ "IDEN " + username2 + "\n" // register
					+ "STAT\n" // status after register
					+ "LIST\n" // list of users online
					+ "HAIL Hello everyone\n" // message
					+ "MESG " + username1 + " Hi\n" // private message
					+ "QUIT\n"; // quit
			
			setInputStream(messageSource);
			setOutputStream();
			
			c1Thread.start();
			
			Thread.sleep(100);
			
			String os = fakecs1.getOutputStream().toString();
			
			String[] messages = os.split("\n");
			String serverMessages[] = new String[messages.length];
			
			for(int i = 0; i < messages.length; i++) {
				serverMessages[i] = messages[i].trim();
			}
			
			assertEquals("OK There are currently 2 user(s) on the server You have not logged in yet", serverMessages[1]);
			assertEquals("OK Welcome to the chat server " + username2, serverMessages[2]);
			assertEquals("OK There are currently 2 user(s) on the server You are logged im and have sent 0 message(s)", serverMessages[3]);
			assertEquals("OK " + username2 + ", " + username1 + ",", serverMessages[4]);
			assertEquals("Broadcast from " + username2 + ": Hello everyone", serverMessages[5]);
			assertEquals("OK your message has been sent", serverMessages[7]);
			assertEquals("OK thank you for sending 1 message(s) with the chat service, goodbye.", serverMessages[8]);
			
			ACTIVE_CONNECTION = 3;
			
			setUpConnection(3);
			
			setInputStream("QUIT\n"); // quit without registering
			setOutputStream();
			
			c3Thread.start();
			
			Thread.sleep(100);
			
			os = fakecs3.getOutputStream().toString();
			
			messages = os.split("\n");
			
			assertEquals("OK goodbye", messages[1].trim());
		} catch (IOException e) {
			System.err.println("Error comparing strings for testMessageForConnection()\nOutput stream cannot be retreived");
		} catch (InterruptedException e) {
			System.err.println("Error sleeping thread interrupted");
		}
	}
	
	// tests basic message passing from server/client to client
	// expected output is message sent
	@Test
	public void testMessageForConnection() {
		ACTIVE_CONNECTION = 1;
		
		setInputStream("");
		setOutputStream();
		
		c1Thread.start();
		
		String message = "This is a message sent over the connection.";

		try {			
			Thread.sleep(100);
			
			c1.messageForConnection(message);
			
			String os = fakecs1.getOutputStream().toString();
			
			String line = os.split("\n")[1].trim();
			
			assertEquals(message, line);
		} catch (IOException e) {
			System.err.println("Error retrieving output stream");
		} catch (InterruptedException e) {
			System.err.println("Error sleeping thread interrupted");
		}
	}

}
