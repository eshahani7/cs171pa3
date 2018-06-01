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

    if(!processor.recovered){
      try {
        while(processor.tempChannels.size() < processor.num) {
          in = processor.serverSock.accept();
          ChannelHandler c = new ChannelHandler(in, processor);
          processor.tempChannels.add(c);
          processor.linkStatus.put(in.getPort() % 3000, true);
        }

      } catch(IOException e) {
        e.printStackTrace();
      } //catch(NullPointerException){}
        //
        for(int i = 0; i < processor.tempChannels.size(); i++) {
          processor.tempChannels.get(i).start();
        }

        processor.channels.addAll(processor.tempChannels);
      }

      while(true){
        try{
          in = processor.serverSock.accept();
          ChannelHandler c = new ChannelHandler(in, processor);
          processor.channels.add(c);
          processor.linkStatus.put(in.getPort() % 3000, true);
          c.start();
        } catch(IOException e) {
          e.printStackTrace();
        }
      }

    }

  }
