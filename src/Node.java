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

  Ballot ballotNum = new Ballot(0, num, 0);
  Ballot acceptNum = new Ballot(0, 0, 0);
  Block acceptVal = null;
  Block initialVal = null;

  ArrayList<ChannelHandler> tempChannels = new ArrayList<ChannelHandler>();
  ArrayList<ChannelHandler> channels = new ArrayList<ChannelHandler>();
  ArrayList<Message> acks = new ArrayList<Message>();

  ArrayList<Transaction> q = new ArrayList<Transaction>();
  LinkedList<Block> blockchain = new LinkedList<Block>();

  int balance = 100;
  private int ackCount = 1;
  private int acceptCount = 1;
  private int prepareCount = 0;
  int majority = 3;
  boolean sendPrepare = false;
  long delay = 0;
  long start_time = 0;
  long current_time = 0;

  Timer timer = new Timer();
  boolean inRound = false;
  boolean firstAddition = true;
  private boolean isLeader = false;

  public Node(int num) {
    config = new ArrayList< Pair<String, Integer> >();
    readConfigFile();
    PORT = config.get(num).getValue();
    this.num = num;
    try {
      serverSock = new ServerSocket(PORT);
      System.out.println("Server up on port " + PORT);
    } catch(IOException e) {
      e.printStackTrace();
    }
  }

  public void clearVars() {
    ackCount = 1;
    acceptCount = 1;
    sendPrepare = false;
    isLeader = false;
    ballotNum = new Ballot(0, num, 0);
    acceptNum = new Ballot(0, 0, 0);
    acceptVal = null;
    // initialVal = null;
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
    System.out.println("amount: " + amount + ", debit: " + debitNode + ", credit: " + creditNode);
    Transaction t = new Transaction(amount, debitNode, creditNode);
    q.add(t);
    if(firstAddition) {
      System.out.println("first run");
      initialVal = new Block(q, num);
      run();
      firstAddition = false;
    }
  }

  public void run(){
    clearVars();
    delay = current_time - start_time;
    delay += Math.random() * 6;
    start_time = System.nanoTime();
    // Timer timer = new Timer();
    timer.schedule(new startElection(),delay);
  }

  private class startElection extends TimerTask {
    public void run(){
      clearVars();
      if(q.size() != 0) {
        sendPrepare = true;
        initialVal = new Block(q, num);
        ballotNum.increaseSeqNum();
        ballotNum.increaseDepth();
        System.out.println("sendPrepare set to true");
      }
      // else {
      //   timer.cancel();
      //   run();
      // }
    }
  }

  public void applyTransactions(Block b) {
    //deduct or add money if you're debit or credit node
    ArrayList<Transaction> tList = b.getList();
    for(int i = 0; i < tList.size(); i++) {
      Transaction t = tList.get(i);
      if(num == t.debitNode) {
        balance -= t.amount;
      } else if(num == t.creditNode) {
        balance += t.amount;
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


//where to clear vars? idk do timer for reelection agh
  //------------------------------------PAXOS FUNCTIONS-----------------------//
  public synchronized void appendBlock(Block b) {
    if(blockchain.size() < acceptNum.depth) {
      System.out.println("appending: " + b);
      blockchain.add(b);
      System.out.println("block added");
      //clear queue if your block was added
      if(b.equals(initialVal)) {
        System.out.println("clearing queue");
        // q.clear();
        q = new ArrayList<Transaction>();
      }
      applyTransactions(b);
      // Timer timer = new Timer();
      // timer.cancel();
      // current_time = System.nanoTime();
      // run();
    }
  }

  public synchronized void leaderAccept() {
    Block high = getHighestAck();
    if(high == null) {
      System.out.println("highest ack null");
      acceptVal = new Block(q, num);
      initialVal = acceptVal;
    } else {
      acceptVal = high;
    }
    //accept your proposal
    acceptNum = ballotNum;
  }

  public Block getHighestAck() {
    System.out.println("get highest ack, acks size: " + acks.size());
    Ballot highest = null;
    int highestIndex = -1;
    for(int i = 0; i < acks.size(); i++) {
      if(highest == null && acks.get(i).a != null) {
        highest = acks.get(i).a;
        highestIndex = i;
      }
      else if((acks.get(i).a).compareTo(highest) > 0) {
        highest = acks.get(i).a;
        highestIndex = i;
      }
    }

    Block highBlock = null;
    if(highestIndex != -1) {
      highBlock = acks.get(highestIndex).v;
    }
    return highBlock;
  }

  public void incrementPrepares() {
    prepareCount++;
    // channels.get(0).test();
    if(prepareCount == 4) {
      sendPrepare = false;
    }
  }

  public synchronized void checkIfLeader() {
    if(ackCount >= majority && !isLeader) {
      isLeader = true;
      leaderAccept();
    }
  }

  public void incrementAcks() {
    ackCount++;
  }

  public int getAckCount() {
    return ackCount;
  }

  public void incrementAccepts() {
    acceptCount++;
  }

  public int getAcceptCount() {
    return acceptCount;
  }

  public boolean getLeader() {
    return isLeader;
  }
}
