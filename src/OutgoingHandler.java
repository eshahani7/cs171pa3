import java.io.*;
import java.net.*;
import java.util.*;
import javafx.util.Pair;

public class OutgoingHandler extends Thread {
  private ArrayList< Pair<String, Integer> > connections;
  private Socket out;
  Node processor;

  public OutgoingHandler(ArrayList< Pair<String, Integer> > c, Node n) {
    connections = c;
    processor = n;
  }

  public void run() {
    int start = processor.num + 1;
    System.out.println("node num + 1: " + start);
    if(!processor.recovered){
      for(int i = processor.num + 1; i < connections.size(); i++) {
        try {
          out = new Socket(connections.get(i).getKey(), connections.get(i).getValue());
          System.out.println("Connected to: " + connections.get(i).getKey() + " on port: " + connections.get(i).getValue());
          ChannelHandler c = new ChannelHandler(out, processor);
          processor.channels.add(c);
          processor.linkStatus.put(i, true);
          c.start();
        } catch(IOException e) {
          e.printStackTrace();
        }
      }
    }

    else {
      for(int i = 0; i < 5; i++){
        try {
          if (i != processor.num){
            out = new Socket(connections.get(i).getKey(), connections.get(i).getValue());
            System.out.println("Connected to: " + connections.get(i).getKey() + " on port: " + connections.get(i).getValue());
            ChannelHandler c = new ChannelHandler(out, processor);
            processor.channels.add(c);
            processor.linkStatus.put(i, true);
            c.start();
          }
        } catch(IOException e) {
          // e.printStackTrace();
        }
      }
    }

    /*while(true){
      for(int i = 0; i < processor.channels.size(); i++){
        if (processor.channels.get(i).checkChannel() == false){
          try {
            out = new Socket(connections.get(processor.num + 1 + i).getKey(), connections.get(processor.num + 1 + i).getValue());
            System.out.println("Connected to: " + connections.get(processor.num + 1 + i).getKey() + " on port: " + connections.get(processor.num + 1 + i).getValue());
            ChannelHandler c = new ChannelHandler(out, processor);
            processor.channels.set(i,c);
            c.start();
          } catch(IOException e) {
            e.printStackTrace();
          }
        }
      }
    }*/

  }
}
