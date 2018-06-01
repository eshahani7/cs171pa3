import java.io.*;
import java.net.*;
import java.util.*;
import javafx.util.Pair;

public class Node implements Serializable{
  public int num;
  boolean recovered = false;
  int check = 0;

  private int PORT;
  transient ServerSocket serverSock;
  private ArrayList< Pair<String, Integer> > config;
  private boolean connected = false;
  private boolean accepted = false;
  private transient BufferedReader br = null;

  Ballot ballotNum;
  Ballot acceptNum;
  Block acceptVal = null;
  Block initialVal = null;

  transient ArrayList<ChannelHandler> tempChannels;
  transient ArrayList<ChannelHandler> channels;
  transient ArrayList<Message> acks;
  transient Hashtable<Integer, Boolean> linkStatus;

  ArrayList<Transaction> q = new ArrayList<Transaction>();
  LinkedList<Block> blockchain = new LinkedList<Block>();

  int balance = 100;
  int ackCount = 1;
  int acceptCount = 1;
  int prepareCount = 0;
  int majority = 3;
  boolean sendPrepare = false;
  boolean firstAck = true;
  long delay = 0;
  long start_time = 0;
  long current_time = 0;

  transient Timer timer = new Timer();
  boolean firstAddition = true;
  private boolean isLeader = false;


  public Node(int num) {
    tempChannels = new ArrayList<ChannelHandler>();
    channels = new ArrayList<ChannelHandler>();
    acks = new ArrayList<Message>();
    linkStatus = new Hashtable<Integer, Boolean>();
    timer = new Timer();

    config = new ArrayList< Pair<String, Integer> >();
    readConfigFile();
    PORT = config.get(num).getValue();
    this.num = num;
    ballotNum = new Ballot(0, num, 0);
    acceptNum = new Ballot(0, 0, 0);
    try {
      serverSock = new ServerSocket(PORT);
      System.out.println("Server up on port " + PORT);
    } catch(IOException e) {
      e.printStackTrace();
    }
    try {
      ObjectOutputStream os = new ObjectOutputStream(new FileOutputStream("Save"+num+".txt"));
      os.writeObject(this);
      os.close();
    } catch (IOException e){}
  }

  public void restore() {
    tempChannels = new ArrayList<ChannelHandler>();
    channels = new ArrayList<ChannelHandler>();
    acks = new ArrayList<Message>();
    linkStatus = new Hashtable<Integer, Boolean>();
    try {
      serverSock = new ServerSocket(PORT);
      System.out.println("Server up on port " + PORT);
    } catch(IOException e) {
      e.printStackTrace();
    }
    timer = new Timer();
    if (q.size() > 0){
      run();
    }
    else{
      firstAddition = true;
    }
  }

  public void pollBlockchain(){
    for (int i = 0; i < channels.size(); i++){
      channels.get(i).poll = true;
    }
  }

  public void clearVars() {
    ackCount = 1;
    acceptCount = 1;
    sendPrepare = false;
    isLeader = false;
    setFirstAck(true);
    ballotNum = new Ballot(0, num, blockchain.size());
    acceptNum = new Ballot(0, 0, 0);
    acceptVal = null;
    acks = new ArrayList<Message>();
    System.out.println("vars cleared");
    System.out.println("bal: " + ballotNum + ", accept: " + acceptNum + ", val: " + acceptVal);
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

    IncomingHandler ih = new IncomingHandler(this);
    ih.start();
  }

  public void setLink(int linkNum, boolean status) {
    linkStatus.put(linkNum, status);
  }

  public void moneyTransfer(int amount, int debitNode, int creditNode) {
    Transaction t = new Transaction(amount, debitNode, creditNode);
    if(debitNode != num) {
      System.out.println("Error, can only initiate transaction from debitNode");
    }
    else if(balance < amount) {
      System.out.println("Error, attempting to overdraw");
    }
    else {
      System.out.println("in money transfer");
      q.add(t);
      try {
          ObjectOutputStream os = new ObjectOutputStream(new FileOutputStream("Save"+num+".txt"));
          os.writeObject(this);
          os.close();
      } catch (IOException e){
      }
      if(firstAddition) {
        initialVal = new Block(q, num);
        run();
        firstAddition = false;
      }
    }
  }

  public void run(){
    int rangeMin = 4;
    int rangeMax = 9;
    Random r = new Random();
    delay = (long)(rangeMin + (rangeMax - rangeMin) * r.nextDouble());
    delay *= 1000;
    timer.schedule(new startElection(), delay, delay*5);
  }

  //case 1: proposal failed, want to try again in same round
  //case 2: new round, want to be leader if queue not empty
  //2a: was prev leader, want to be leader again
  //2b: was prev not leader, want to be leader now
  private class startElection extends TimerTask {
    public void run() {
      if(!isLeader) {
          ackCount = 1;
          acceptCount = 1;
          elect();
      }
    }
  }

  private class startElection2 extends TimerTask {
    public void run() {
      if(!isLeader && blockchain.size() < ballotNum.depth) {
        ballotNum.procId = num;
        ackCount = 1;
        acceptCount = 1;
        elect();
      }
    }
  }

  private void elect() {
    if(q.size() != 0 || !firstAck) {
      if(q.size() != 0) {
        System.out.println("election: queue not empty");
      }
      if(!firstAck) {
        System.out.println("election: waiting for decision");
      }
      int old = ballotNum.seqNum;
      ballotNum = new Ballot(old+1, num, blockchain.size() + 1);
      Ballot prepBallot = ballotNum;
      System.out.println("starting election w/ ballot: " + ballotNum);
      for(int i = 0; i < channels.size(); i++) {
        if(channels.get(i) != null) {
          channels.get(i).setPrepare(prepBallot);
        }
      }
      initialVal = new Block(q, num);
    }
  }

  public synchronized void applyTransactions(Block b) {
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

  public synchronized void checkToClearQueue(Block b) {
    if(b.equals(q)) {
      q = new ArrayList<Transaction>();
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


  //------------------------------------PAXOS FUNCTIONS-----------------------//
  public synchronized void appendBlock(Block b) {
    if(blockchain.size() < acceptNum.depth) {
      System.out.println("appending: " + b);
      blockchain.add(b);
      System.out.println("block added");
      acceptVal = null;
      System.out.println("initial: " + initialVal);
      System.out.println("adding: " + b);
      //clear queue if your block was added
      if(b.equals(initialVal)) {
        System.out.println("clearing queue");
        q = new ArrayList<Transaction>();
      }
      applyTransactions(b);
      clearVars();
      try {
        ObjectOutputStream os = new ObjectOutputStream(new FileOutputStream("Save"+num+".txt"));
        os.writeObject(this);
        os.close();
    } catch (IOException e){}
    }
  }

  public synchronized void leaderAccept() {
    Block high = getHighestAck();
    if(high == null) {
      System.out.println("highest ack null");
      if(acceptVal == null) {
        acceptVal = new Block(q, num);
        initialVal = acceptVal;
      }
    } else {
      acceptVal = high;
    }
    //accept your proposal
    acceptNum = ballotNum;
  }

  public synchronized void setAcceptVal(Block v) {
    // System.out.println("setting accept val to: " + v);
    acceptVal = v;
  }

  public synchronized Block getAcceptVal() {
    return acceptVal;
  }

  public synchronized void setBallotNum(Ballot b) {
    ballotNum = b;
  }

  public synchronized void setAcceptNum(Ballot a) {
    acceptNum = a;
  }

  public synchronized void setFirstAck(boolean a) {
    firstAck = a;
  }

  public synchronized boolean getFirstAck() {
    return firstAck;
  }

  public Block getHighestAck() {
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

  public synchronized void checkIfLeader() {
    if(ackCount >= majority && !isLeader && blockchain.size() < ballotNum.depth) {
      Ballot a = ballotNum;
      isLeader = true;
      leaderAccept();
      for(int i = 0; i < channels.size(); i++) {
        if(channels.get(i) != null) {
          channels.get(i).setAccept(a);
        }
      }
    }
  }

  public synchronized void incrementAcks() {
    ackCount++;
  }

  public synchronized void incrementAccepts() {
    acceptCount++;
    if(acceptCount >= majority && blockchain.size() < acceptNum.depth) {
      Block d = acceptVal;
      appendBlock(d);
      for(int i = 0; i < channels.size(); i++) {
        if(channels.get(i) != null) {
          channels.get(i).setDecide(d);
        }
      }
    }
  }

  public synchronized boolean getLeader() {
    return isLeader;
  }
}
