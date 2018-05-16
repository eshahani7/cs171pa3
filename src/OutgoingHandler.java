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
    for(int i = processor.num + 1; i < connections.size(); i++) {
      try {
        out = new Socket(connections.get(i).getKey(), connections.get(i).getValue());
        System.out.println("Connected to: " + connections.get(i).getKey() + " on port: " + connections.get(i).getValue());
        ChannelHandler c = new ChannelHandler(out, processor);
        processor.channels.add(c);
        c.start();
      } catch(IOException e) {
        e.printStackTrace();
      }
    }
  }
}
