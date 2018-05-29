import java.io.*;
import java.net.*;
import java.util.*;

public class ChannelHandler extends Thread {

  private Socket channel;
  private Node process;
  private int majority;
  private int linkedTo;

  private ObjectOutputStream writer = null;
  private ObjectInputStream reader = null;

  boolean exit = false;
  boolean sendAccept = false;
  boolean sendDecide = false;
  boolean sendPrepare = false;
  boolean sentAck = false;

  Ballot prepBallot;
  Block decideBlock;
  Ballot acceptBallot;

  ReadHandler r = null;


  public ChannelHandler(Socket in, Node n) {
    channel = in;
    process = n;
    majority = 3;
    linkedTo = in.getPort() % 3000;
  }

  public void setPrepare(Ballot bal) {
    if(process.linkStatus.get(linkedTo)) {
      sendPrepare = true;
      prepBallot = bal;
    }
  }

  public void setDecide(Block b) {
    if(process.linkStatus.get(linkedTo)) {
      sendDecide = true;
      decideBlock = b;
    }
  }

  public void setAccept(Ballot a) {
    if(process.linkStatus.get(linkedTo)) {
      sendAccept = true;
      acceptBallot = a;
    }
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
      if(process.linkStatus.get(linkedTo)) {
        if(sendPrepare){
          sendPrepare = false;
          Message send = new Message("prepare", prepBallot, null, null);
          System.out.println("sending prepare w/ bal: " + send.bal);
          sendMessage(send);
          prepBallot = null;
        }
        else if(sendAccept) { //only do this once
          System.out.println("sending accepts");
          sendAccept = false;
          Message send = new Message("accept", acceptBallot, null, process.acceptVal);
          // System.out.println(send.v);
          sendMessage(send);
          acceptBallot = null;
        }
        else if(sendDecide) { //only do this once
          if(decideBlock != null) {
            System.out.println("sending decision");
            sendDecide = false;
            Message send = new Message("decision", process.ballotNum, null, decideBlock); //need only one append per node
            System.out.println(decideBlock);
            sendMessage(send);
            decideBlock = null;
          }
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
  }

  public void handleMessage(Message m) {

    if(m.msgType.equals("prepare")) {
      System.out.println("got prepare: " + m.bal + ", accept: " + m.a);
      if(m.bal.compareTo(process.ballotNum) >= 0 && m.bal.depth != process.blockchain.size()) { //msg ballot >= my ballot
        process.setBallotNum(m.bal);
        Message send = new Message("ack", process.ballotNum, process.acceptNum, process.getAcceptVal());
        System.out.println("sending ack: " + process.ballotNum + " with val: " + send.v  + ", with a: " + send.a);
        sendMessage(send);
      }
      else {
        System.out.println("rejecting prepare, my ballot is: " + process.ballotNum);
      }
    }
    else if(m.msgType.equals("ack")) {
      System.out.println("got ack: " + m.bal + ", with val: " + m.v + ", and aNum: " + m.a + ", myBal: " + process.ballotNum);
      if(m.bal.compareTo(process.ballotNum) == 0) {
        process.acks.add(m);
        process.incrementAcks();
        process.checkIfLeader();
      }
    }
    else if(m.msgType.equals("accept")) {
      if(process.getLeader() && m.bal.compareTo(process.ballotNum) == 0) {
        System.out.println("leader got accept");
        process.incrementAccepts();
      }
      else if(m.bal.compareTo(process.ballotNum) >= 0 && !sendDecide && process.blockchain.size() < m.bal.depth) {
        System.out.println("acceptor got accept: " + m.bal + " , " + m.v);
        process.setAcceptNum(m.bal);
        process.setAcceptVal(m.v);
        Message send = new Message("accept", process.ballotNum, null, process.acceptVal);
        sendMessage(send);
        System.out.println("sent accept to leader");
        if(process.getFirstAck()) {
          process.setFirstAck(false);
        }
      }

    }
    else if(m.msgType.equals("decision")) { //acceptor gets decision
      System.out.println("got decision: " + m.v);
      process.appendBlock(m.v);
    }
  }

  private void sendMessage(Message send) {
    try {
      try {
        Thread.sleep(2000);
      } catch(InterruptedException e) {
        // e.printStackTrace();
      }
      writer.writeObject(send);
      writer.flush();
      // writer.reset();
    } catch(IOException e) {
      // e.printStackTrace();
    }
  }

  private class ReadHandler extends Thread {
    public void run() {
      while(true) {
        if(process.linkStatus.get(linkedTo)) {
          try {
            Object msgObj = reader.readObject();
            if(msgObj instanceof Message) {
              Message m = (Message) msgObj;
              handleMessage(m);
            }
          } catch(ClassNotFoundException e) {
            // e.printStackTrace();
          } catch(IOException e) {
            // e.printStackTrace();
          }
        }
      }
    }
  }

}
