import java.io.*;
import java.net.*;
import java.util.*;
import javafx.util.Pair;

public class Node {
  private int balance = 100;
  
  Queue<Transaction> q = new LinkedList();
  LinkedList<Block> blockchain = new LinkedList<Block>();   
  ArrayList<ChannelHandler> channels = new ArrayList<ChannelHandler>();
  
  private int PORT = 5000;
  private ServerSocket serverSock;
  private ArrayList< Pair<String, Integer> > config;
  private Socket in;
  private boolean connected = false;
  private boolean accepted = false;
  
  public Node() {
    try {
      serverSock = new ServerSocket(PORT);
      config = new ArrayList< Pair<String, Integer> >();
      System.out.println("Server up on port " + PORT);
    } catch(IOException e) {
      e.printStackTrace();
    }
  }
  
  public void readConfigFile() {
    //fill in config ArrayList
  }
  
  public void setUp() {
    try {
      Thread.sleep(5000);
    } catch(InterruptedException e) {
      e.printStackTrace();
    }
    
    OutgoingHandler o = new OutgoingHandler(config);
    o.start();
    
    try {
      while(true) {
        in = serverSock.accept();
        ChannelHandler c = new ChannelHandler(in);
        channels.add(c);
        c.start();
      }
    } catch(IOException e) {
      e.printStackTrace();
    }
  }
  
  public static void main(String[] args) {
    Node n = new Node();
    n.setUp();
  }
}