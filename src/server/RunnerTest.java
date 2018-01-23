package server;

import static org.junit.Assert.assertEquals;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.junit.Test;

public class RunnerTest {

	// tests construction of server on port 9000
	// expected output is initialisation message
	@Test
	public void test() {
		// sets System out print stream to test print stream
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		PrintStream ps = new PrintStream(baos);
		PrintStream systemOut = System.out;
		
		System.setOut(ps);
		
		ExecutorService es = Executors.newSingleThreadExecutor();
		boolean run = false;
		
		es.execute(new Runnable() {
			public void run() {
				Runner.main(null);				
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
		
		assertEquals("Server has been initialised on port 9000", baos.toString().trim());			
	}

}
