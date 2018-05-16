import java.io.*;
import java.net.*;
import java.util.*;

public class ChannelHandler extends Thread {

  private Socket channel;
  private Node process;
  private int majority;
  private boolean isLeader;

  private ObjectOutputStream writer = null;
  private ObjectInputStream reader = null;

  boolean exit = false;
  boolean acceptNotSent = true;
  boolean decisionNotSent = true;

  private ArrayList<Message> acks = new ArrayList<Message>();
  Block myVal;

  ReadHandler r = null;

  public ChannelHandler(Socket in, Node n) {
    channel = in;
    process = n;
    majority = 3;
    isLeader = false;
    myVal = null;
  }

  public void clearVars() {
    isLeader = false;
    acks.clear();
    myVal = null;
    boolean acceptNotSent = true;
    boolean decisionNotSent = true;
  }

  //will need to access vars of node here
  public void run() {
    try {
      writer = new ObjectOutputStream(channel.getOutputStream());
      System.out.println("writer connected");
      System.out.println("trying to open reader");
      reader = new ObjectInputStream(channel.getInputStream());
      System.out.println("reader connected");
    } catch(IOException e) {
      e.printStackTrace();
    }

    while(!exit) {
      // System.out.println("in thread loop");
      // try {
        if(process.sendPrepare){
          Ballot bal = process.ballotNum;
          bal.increaseSeqNum();
          Message send = new Message("prepare", bal, null, null);
          sendMessage(send);
          process.sendPrepare = false;
        }
        else if(process.getLeader() && acceptNotSent) { //only do this once, add bool
          System.out.println("sending accepts");
          acceptNotSent = false;
          //send accept
          System.out.println(acks.size());
          int i = getHighestAck();
          if(i == -1) {
            myVal = process.initialVal; //?
          } else {
            myVal = acks.get(i).v;
          }
          process.acceptVal = myVal;
          Message send = new Message("accept", process.ballotNum, null, myVal);
          sendMessage(send);
        }
        else if(process.getAcceptCount() >= majority && decisionNotSent) { //only do this once, add bool
          System.out.println("sending decision");
          decisionNotSent = false;
          Message m = new Message("decision", process.ballotNum, null, myVal);
          process.appendBlock(myVal);
          sendMessage(m);
          clearVars();
        }
        else {
          sendMessage(null);
        }

        // read message
      //   Object msgObj = reader.readObject();
      //   Message m = (Message) msgObj;
      //   handleMessage(m);
      // } catch(ClassNotFoundException e) {
      //   e.printStackTrace();
      // } catch(IOException e) {
      //   e.printStackTrace();
      // }

      if(r == null) {
        r = new ReadHandler();
        r.start();
      }
    }
  }

  public void handleMessage(Message m) {
    if(m.msgType.equals("prepare")) {
      System.out.println("got prepare");
      if(m.bal.compareTo(process.ballotNum) >= 0) {
        process.ballotNum = m.bal;
        Message send = new Message("ack", process.ballotNum, process.acceptNum, process.acceptVal);
        sendMessage(send);
      }
    }
    else if(m.msgType.equals("ack")) {
      System.out.println("got ack");
      //increment acks for node
      process.incrementAcks();
      acks.add(m);
      if(process.getAckCount() >= majority) {
        process.setLeader(true);
      }
    }
    else if(m.msgType.equals("accept")) {
      if(isLeader) {
        System.out.println("leader got accept");
        process.incrementAccepts();
        // System.out.println("acceptCount: " + process.acceptCount);
      }
      else if(m.bal.compareTo(process.ballotNum) >= 0) {
        System.out.println("acceptor got accept");
        process.acceptNum = m.a;
        process.acceptVal = m.v;
        Message send = new Message("accept", process.ballotNum, null, process.acceptVal);
        sendMessage(send);
      }
    }
    else if(m.msgType.equals("decision")) { //acceptor gets decision
      System.out.println("got decision");
      System.out.println(m.v);
      process.appendBlock(m.v);
      clearVars();
    }
  }

  public int getHighestAck() {
    Ballot highest = null;
    int highestIndex = -1;
    for(int i = 0; i < acks.size(); i++) {
      if(highest == null && acks.get(i).a != null) {
        highest = acks.get(i).a;
        highestIndex = i;
      }
      else if(highest != null && acks.get(i).a.compareTo(highest) > 0) {
        highest = acks.get(i).a;
        highestIndex = i;
      }
    }

    return highestIndex;
  }

  private void sendMessage(Message send) {
    try {
      try {
        Thread.sleep(2000);
      } catch(InterruptedException e) {
        e.printStackTrace();
      }
      writer.writeObject(send);
      writer.flush();
      // writer.reset();
    } catch(IOException e) {
      e.printStackTrace();
    }
  }

  private class ReadHandler extends Thread {
    public void run() {
      while(true) {
        try {
          // System.out.println("trying to read");
          Object msgObj = reader.readObject();
          // System.out.println("read");
          if(msgObj instanceof Message) {
            Message m = (Message) msgObj;
            handleMessage(m);
          }
        } catch(ClassNotFoundException e) {
          e.printStackTrace();
        } catch(IOException e) {
          e.printStackTrace();
        }
      }

    }
  }
}
