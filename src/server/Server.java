package server;

import java.io.IOException;
import java.net.ServerSocket;
import java.util.ArrayList;
import java.util.Iterator;


public class Server {

	private ServerSocket server;
	private ArrayList<Connection> list;
	
	public Server (int port) {
		try {
			server = new ServerSocket(port);
			System.out.println("Server has been initialised on port " + port);
		}
		catch (IOException e) {
			System.err.println("error initialising server");
			e.printStackTrace();
		}
		list = new ArrayList<Connection>();
		while(true) {
				Connection c = null;
				try {
					c = new Connection(server.accept(), this);
				}
				catch (IOException e) {
					System.err.println("error setting up new client conneciton");
					e.printStackTrace();
				}
				Thread t = new Thread(c);
				t.start();
				list.add(c);
		}
	}
	
	public ArrayList<String> getUserList() {
		ArrayList<String> userList = new ArrayList<String>();
		for( Connection clientThread: list){
			if(clientThread.getState() == Connection.STATE_REGISTERED) {
				userList.add(clientThread.getUserName());
			}
		}
		return userList;
	}
	
	public boolean doesUserExist(String newUser) {
		boolean result = false;
		for( Connection clientThread: list){
			if(clientThread.getState() == Connection.STATE_REGISTERED) {
				result = clientThread.getUserName().compareTo(newUser)==0;
			}
		}
		return result;
	}
	
	public void broadcastMessage(String theMessage){
		System.out.println(theMessage);
		for( Connection clientThread: list){
			clientThread.messageForConnection(theMessage + System.lineSeparator());	
		}
	}
	
	public boolean sendPrivateMessage(String message, String user) {
		for( Connection clientThread: list) {
			if(clientThread.getState() == Connection.STATE_REGISTERED) {
				if(clientThread.getUserName().compareTo(user)==0) {
					clientThread.messageForConnection(message + System.lineSeparator());
					return true;
				}
			}
		}
		return false;
	}
	
	public void removeDeadUsers(){
		Iterator<Connection> it = list.iterator();
		while (it.hasNext()) {
			Connection c = it.next();
			if(!c.isRunning())
				it.remove();
		}
	}
	
	public int getNumberOfUsers() {
		return list.size();
	}
	
	protected void finalize() throws IOException{
		server.close();
	}
		
}
