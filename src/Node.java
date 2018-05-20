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

  Ballot ballotNum;
  Ballot acceptNum;
  Block acceptVal = null;
  Block initialVal = null;

  ArrayList<ChannelHandler> tempChannels = new ArrayList<ChannelHandler>();
  ArrayList<ChannelHandler> channels = new ArrayList<ChannelHandler>();
  ArrayList<Message> acks = new ArrayList<Message>();

  ArrayList<Transaction> q = new ArrayList<Transaction>();
  LinkedList<Block> blockchain = new LinkedList<Block>();

  int balance = 100;
  int ackCount = 1;
  int acceptCount = 1;
  int prepareCount = 0;
  int majority = 3;
  boolean sendPrepare = false;
  long delay = 0;
  long start_time = 0;
  long current_time = 0;

  Timer timer = new Timer();
  boolean inRound = false;
  boolean firstAddition = true;
  private boolean isLeader = false;
  boolean dSent = false;

  public Node(int num) {
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
  }

  public void clearVars() {
    ackCount = 1;
    acceptCount = 1;
    sendPrepare = false;
    isLeader = false;
    ballotNum = new Ballot(0, num, blockchain.size());
    acceptNum = new Ballot(0, 0, 0);
    acceptVal = null;
    dSent = false;
    acks = new ArrayList<Message>();
    System.out.println("vars cleared");
    System.out.println("bal: " + ballotNum + ", accept: " + acceptNum + ", val: " + acceptVal);
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
    Transaction t = new Transaction(amount, debitNode, creditNode);
    q.add(t);
    if(firstAddition) {
      initialVal = new Block(q, num);
      run();
      firstAddition = false;
    }
  }

  public void run(){
    int rangeMin = 4;
    int rangeMax = 8;
    Random r = new Random();
    delay = (long)(rangeMin + (rangeMax - rangeMin) * r.nextDouble());
    delay *= 1000;
    timer.schedule(new startElection(), delay, delay);
  }

  //case 1: proposal failed, want to try again in same round
  //case 2: new round, want to be leader if queue not empty
  //2a: was prev leader, want to be leader again
  //2b: was prev not leader, want to be leader now
  private class startElection extends TimerTask {
    public void run() {
      // this doesn't work oops idk proposal failed, want to try again in same round
      // if(!isLeader && blockchain.size() < ballotNum.depth) {
      //   ballotNum.procId = num;
      //   ackCount = 1;
      //   acceptCount = 1;
      //   elect();
      // }
      //new round
      if(blockchain.size() == ballotNum.depth) {
        ballotNum.increaseDepth();
        elect();
      }
    }

    private void elect() {
      if(q.size() != 0) {
        ballotNum.increaseSeqNum();
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
      // if(!isLeader || (isLeader && acceptCount < majority)) {
      //   clearVars();
      // }
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
