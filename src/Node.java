import java.io.*;
import java.net.*;
import java.util.*;
import javafx.util.Pair;

public class Node {
  private int num;

  private int PORT;
  private ServerSocket serverSock;
  private ArrayList< Pair<String, Integer> > config;
  private Socket in;
  private boolean connected = false;
  private boolean accepted = false;
  private BufferedReader br = null;

  private Ballot ballotNum = new Ballot(0, num, 0);
  private Ballot acceptNum = new Ballot(0, 0, 0);
  private Block acceptVal = NULL;

  ArrayList<ChannelHandler> channels = new ArrayList<ChannelHandler>();

  ArrayList<Transaction> q = new ArrayList<Transaction>();
  LinkedList<Block> blockchain = new LinkedList<Block>();

  int balance = 100;
  int ackCount = 0;
  int acceptCount = 1;
  int majority = 3;


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
        System.out.println("connection accepted");
      }
    } catch(IOException e) {
      e.printStackTrace();
    }
  }

  public void moneyTransfer(int amount, int debitNode, int creditNode) {
    //start leader election in here
    //add timer countdown; needs to be global?? execute for loop when it runs out
    //start phase 1 if queue empty --> iterate through channels and send "prepare"
    for(int i = 0; i < channels.size(); i++) {
      channels.get(i).prepare();
    }
    acceptVal = new Block(q, num);
  }

  public void appendBlock(Block b) {
    blockchain.add(b);
    //clear queue if your block was added
    if(b == acceptVal) {
      q.clear();
    }
  }

  public void printBlockchain() {

  }

  public void printBalance() {

  }

  public void printQueue() {

  }
}
