import java.io.*;
import java.net.*;
import java.util.*;
import javafx.util.Pair;

public class Node {
  private int num;
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
  private BufferedReader br = null;

  
  public Node(int num) {
    config = new ArrayList< Pair<String, Integer> >();
    readConfigFile();
    PORT = config.get(num).getValue();
    try {
      serverSock = new ServerSocket(PORT);
      System.out.println("Server up on port " + PORT);
      config.remove(num);
    } catch(IOException e) {
      e.printStackTrace();
    }
  }
  
  public void readConfigFile() {
    String line = null;
    try {
      br = new BufferedReader(new FileReader("config.txt"));
    } catch(FileNotFoundException e){
      e.printStackTrace();
    }

    try{
      while ((line = br.readLine()) != null){
        String[] splitStr = line.split("\\s+");
        Pair<String, Integer> configPair = new Pair(splitStr[0],Integer.parseInt(splitStr[1]));
        config.add(configPair);
      }
    } catch (IOException ie){
      ie.printStackTrace();
    }
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
    Node n = new Node(Integer.parseInt(args[0]));
    n.setUp();
  }
}