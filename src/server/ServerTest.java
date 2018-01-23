package server;

import static org.junit.Assert.assertEquals;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.junit.Test;

public class ServerTest {

	// tests construction of server
	// expected output is initialisation message
	@Test
	public void testServer() {
		// sets System out print stream to test print stream
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		PrintStream ps = new PrintStream(baos);
		PrintStream systemOut = System.out;
		
		System.setOut(ps);
		
		ExecutorService es = Executors.newCachedThreadPool();
		boolean run = false;
		
		// generates random port number from range 1000 to 9999
		Random r = new Random();
		int port = 1000 + r.nextInt(9999 - 1000) + 1;
		
		es.execute(new Runnable() {
			public void run() {
				Server s1 = new Server(port);	
			}
		});
		
		try {
			es.shutdown();
			
			run = es.awaitTermination(1000, TimeUnit.MILLISECONDS);
			
			if(!run)
				Thread.sleep(1000);
		} catch (InterruptedException e) {
			System.err.println("Error waiting thread interrupted");
		}
		
		es.shutdownNow();

		// resets System out print stream
		System.out.flush();
		System.setOut(systemOut);
		
		assertEquals("Server has been initialised on port " + port, baos.toString().trim());
	}
}
