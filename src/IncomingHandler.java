import java.io.*;
import java.net.*;
import java.util.*;
import javafx.util.Pair;

public class IncomingHandler extends Thread {
  	private Socket in;
  	Node processor;

  	public IncomingHandler(Node n) {
	    processor = n;
	}

	public void run() {

		System.out.println("Incoming thread started");
		if(!processor.recovered){
		  	try {
		      while(processor.tempChannels.size() < processor.num) {
		        in = processor.serverSock.accept();
		        ChannelHandler c = new ChannelHandler(in, processor);
		        processor.tempChannels.add(c);
		        System.out.println("Added temp");
		      }

		    } catch(IOException e) {
		      e.printStackTrace();
		    } //catch(NullPointerException){}
		  //
		  System.out.println(processor.tempChannels.size());
		  for(int i = 0; i < processor.tempChannels.size(); i++) {
		    processor.tempChannels.get(i).start();
		    System.out.println("thread started");
		  }

		  processor.channels.addAll(processor.tempChannels);
		}

	  while(true){
	  	try{
		  	in = processor.serverSock.accept();
			ChannelHandler c = new ChannelHandler(in, processor);
			processor.channels.add(c);
			c.start();
		} catch(IOException e) {
          e.printStackTrace();
        }
	  }

	}

}