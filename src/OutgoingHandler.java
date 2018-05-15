import java.io.*;
import java.net.*;
import java.util.*;
import javafx.util.Pair;

public class OutgoingHandler extends Thread {
  private ArrayList< Pair<String, Integer> > connections;
  private Socket out;

  public OutgoingHandler(ArrayList< Pair<String, Integer> > c) {
    connections = c;
  }

  public void run() {
    for(int i = 0; i < connections.size(); i++) {
      try {
        out = new Socket(connections.get(i).getKey(), connections.get(i).getValue());
      } catch(IOException e) {
        e.printStackTrace();
      }
    }
  }
}
