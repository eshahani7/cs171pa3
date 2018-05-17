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
  boolean sendPrepare = true;

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
    myVal = null;
    boolean acceptNotSent = true;
    boolean decisionNotSent = true;
  }

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
      if(process.sendPrepare && sendPrepare){
        Ballot bal = process.ballotNum;
        sendPrepare = false;
        process.incrementPrepares();
        Message send = new Message("prepare", bal, null, null);
        sendMessage(send);
      }
      else if(process.getLeader() && acceptNotSent) { //only do this once
        System.out.println("sending accepts");
        acceptNotSent = false;
        Message send = new Message("accept", process.ballotNum, process.acceptVal);
        System.out.println(send.v);
        sendMessage(send);
      }
      else if(process.getAcceptCount() >= majority && decisionNotSent) { //only do this once
        System.out.println("sending decision");
        decisionNotSent = false;
        Message m = new Message("decision", process.ballotNum, process.acceptVal); //need only one append per node
        process.appendBlock(process.acceptVal);
        System.out.println(process.acceptVal);
        sendMessage(m);
        clearVars();
      }
      else {
        sendMessage(null);
      }

      if(r == null) {
        r = new ReadHandler();
        r.start();
      }
    }
  }

  public void handleMessage(Message m) {
    if(m.msgType.equals("prepare")) {
      System.out.println("got prepare: " + m.bal);
      if(m.bal.compareTo(process.ballotNum) >= 0) {
        process.ballotNum = m.bal;
        Message send = new Message("ack", process.ballotNum, process.acceptNum, process.acceptVal);
        sendMessage(send);
      }
    }
    else if(m.msgType.equals("ack")) {
      System.out.println("got ack: " + m.a + ", with val: " + m.v);
      process.acks.add(m);
      process.incrementAcks();
      process.checkIfLeader();
    }
    else if(m.msgType.equals("accept")) {
      if(process.getLeader()) {
        System.out.println("leader got accept");
        process.incrementAccepts();
      }
      else if(m.bal.compareTo(process.ballotNum) >= 0) {
        System.out.println("acceptor got accept");
        process.acceptNum = m.bal;
        process.acceptVal = m.v;
        System.out.println("acceptNum: " + process.acceptNum + ", acceptVal: " + process.acceptVal);
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
