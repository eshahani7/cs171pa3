import java.io.*;
import java.net.*;
import java.util.*;
import javafx.util.Pair;

public class Node {
  public int num;

  private int PORT;
  private ServerSocket serverSock;
  private ArrayList< Pair<String, Integer> > config;
  private Socket in;
  private boolean connected = false;
  private boolean accepted = false;
  private BufferedReader br = null;

  Ballot ballotNum = new Ballot(0, num, 1);
  Ballot acceptNum = new Ballot(0, 0, 0);
  Block acceptVal = null;
  Block initialVal = null;

  ArrayList<ChannelHandler> tempChannels = new ArrayList<ChannelHandler>();
  ArrayList<ChannelHandler> channels = new ArrayList<ChannelHandler>();

  ArrayList<Transaction> q = new ArrayList<Transaction>();
  LinkedList<Block> blockchain = new LinkedList<Block>();

  int balance = 100;
  int ackCount = 1;
  int acceptCount = 1;
  int majority = 3;
  boolean sendPrepare = false;
  long delay = 0;
  long start_time = 0;
  long current_time = 0;

  Timer timer = new Timer();

  public Node(int num) {
    config = new ArrayList< Pair<String, Integer> >();
    readConfigFile();
    PORT = config.get(num).getValue();
    this.num = num;
    try {
      serverSock = new ServerSocket(PORT);
      System.out.println("Server up on port " + PORT);
      // config.remove(num);
    } catch(IOException e) {
      e.printStackTrace();
    }
  }

  public void clearVars() {
    ackCount = 1;
    acceptCount = 1;
    sendPrepare = false;
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

    OutgoingHandler o = new OutgoingHandler(config, this);
    o.start();

    try {
      while(tempChannels.size() < num) {
        in = serverSock.accept();
        ChannelHandler c = new ChannelHandler(in, this);
        tempChannels.add(c);
        // c.start();
      }

    } catch(IOException e) {
      e.printStackTrace();
    }
    //
    System.out.println(tempChannels.size());
    for(int i = 0; i < tempChannels.size(); i++) {
      tempChannels.get(i).start();
      System.out.println("thread started");
    }

    channels.addAll(tempChannels);
  }

  public void moneyTransfer(int amount, int debitNode, int creditNode) {
    Transaction t = new Transaction(amount, debitNode, creditNode);
    q.add(t);
  }

  private class startElection extends TimerTask {
    public void run(){
      //System.out.println("Depleted.");
      sendPrepare = true;
      initialVal = new Block(q, num);
    }
  }

  public void run(){
    delay = current_time - start_time;
    delay += Math.random() * 6;
    start_time = System.nanoTime();
    timer.schedule(new startElection(),delay);
  }

  public void sentPrepare() {
    sendPrepare = false;
  }

  public void appendBlock(Block b) {
    blockchain.add(b);
    //clear queue if your block was added
    if(b.equals(initialVal)) {
      q.clear();
    }
    applyTransactions(b);
    timer.cancel();
    current_time = System.nanoTime();
    run();
  }

  public void applyTransactions(Block b) {
    //deduct or add money if you're debit or credit node
    ArrayList<Transaction> tList = b.getList();
    for(int i = 0; i < tList.size(); i++) {
      Transaction t = tList.get(i);
      if(num == t.debitNode) {
        balance -= num;
      } else if(num == t.creditNode) {
        balance += num;
      }
    }
  }

  public void printBlockchain() {
    for(int i = 0; i < blockchain.size(); i++) {
      System.out.println(blockchain.get(i));
    }
  }

  public void printBalance() {
    System.out.println("Current balance: $" + balance);
  }

  public void printQueue() {
    Block queueBlock = new Block(q, num);
    System.out.println(queueBlock);
  }
}
