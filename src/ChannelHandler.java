import java.io.*;
import java.net.*;
import java.util.*;

public class ChannelHandler extends Thread {

  private Socket channel;
  private Node process;
  private int majority;
  public int linkedTo;

  private ObjectOutputStream writer = null;
  private ObjectInputStream reader = null;

  boolean exit = false;
  boolean sendAccept = false;
  boolean sendDecide = false;
  boolean sendPrepare = false;
  boolean sentLink = false;

  boolean poll = false;

  boolean sentAck = false;

  Ballot prepBallot;
  Block decideBlock;
  Ballot acceptBallot;

  ReadHandler r = null;


  public ChannelHandler(Socket in, Node n) {
    channel = in;
    process = n;
    majority = 3;
    linkedTo = -1;
  }

  public void setPrepare(Ballot bal) {
    if(process.linkStatus.get(linkedTo) && bal != null) {
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
      // System.out.println("trying to open reader");
      reader = new ObjectInputStream(channel.getInputStream());
      System.out.println("reader connected");
      Message m = new Message("port " + process.num, null, null, null);
      sendMessage(m);
    } catch(IOException e) {
      e.printStackTrace();
    }

    while(!exit) {
      if(r == null) {
        r = new ReadHandler();
        r.start();
      }

      // if(linkedTo != -1 && (linkedTo > 4 || linkedTo < 0)){
      //   System.out.println("Terminating " + linkedTo);
      //   exit = true;
      // }
      if(linkedTo != -1 && process.linkStatus.get(linkedTo)) {
        if(sendPrepare){
          sendPrepare = false;
          Message send = new Message("prepare", prepBallot, null, null);
          System.out.println("sending prepare w/ bal: " + send.bal + " to " + linkedTo);
          sendMessage(send);
          //prepBallot = null;
        }
        else if(sendAccept) { //only do this once
          System.out.println("sending accepts");
          sendAccept = false;
          Message send = new Message("accept", acceptBallot, null, process.acceptVal);
          // System.out.println(send.v);
          sendMessage(send);
          //acceptBallot = null;
        }
        else if(sendDecide) { //only do this once
          if(decideBlock != null) {
            System.out.println("sending decision");
            sendDecide = false;
            Message send = new Message("decision", process.ballotNum, null, decideBlock); //need only one append per node
            System.out.println(decideBlock);
            sendMessage(send);
            //decideBlock = null;
          }
        }
        else if(poll){
          poll = false;
          System.out.println("polling for blockchain");
          Message send = new Message("poll",null,null,null);
          sendMessage(send);
        }
      }
      else {
        sendMessage(null);
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
        Message send = new Message("stale",null,null,null);
        send.blockchain = process.blockchain;
        sendMessage(send);
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
          process.leaderTimeout();
        }
      }

    }
    else if(m.msgType.equals("decision")) { //acceptor gets decision
      System.out.println("got decision: " + m.v);
      if (m.bal.depth > process.blockchain.size() + 1){
        process.pollBlockchain();
      }
      else {
        process.appendBlock(m.v);
      }
    }

    else if(m.msgType.equals("poll")){
      System.out.println("got poll request");
      m.blockchain = process.blockchain;
      m.msgType = "pollReturn";
      sendMessage(m);
    }

    else if(m.msgType.equals("pollReturn")){
      System.out.println("got blockchain");
      if(m.blockchain.size() > process.blockchain.size()){
        for (int i = process.blockchain.size(); i < m.blockchain.size(); i++){
          process.applyTransactions(m.blockchain.get(i));
        }

        process.blockchain = m.blockchain;
        process.clearVars();

        try {
          ObjectOutputStream os = new ObjectOutputStream(new FileOutputStream("Save"+process.num+".txt"));
          os.writeObject(process);
          os.close();
        } catch (IOException e){
        }
      }
    }

    else if(m.msgType.equals("stale")){
      //System.out.println("stale " + m.blockchain.size() + " " + process.blockchain.size());
      if (m.blockchain.size() > process.blockchain.size()){
        for (int i = process.blockchain.size(); i < m.blockchain.size(); i++){
          process.applyTransactions(m.blockchain.get(i));
          process.checkToClearQueue(m.blockchain.get(i));
        }

        process.blockchain = m.blockchain;
        System.out.println("correcting stale");
        try {
          ObjectOutputStream os = new ObjectOutputStream(new FileOutputStream("Save"+process.num+".txt"));
          os.writeObject(process);
          os.close();
        } catch (IOException e){}
      }
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
      while(!exit) {
          try {
            Object msgObj = reader.readObject();
            if(msgObj instanceof Message) {
              Message m = (Message) msgObj;
              if(m.msgType.length() > 4 && (m.msgType.substring(0,4)).equals("port")){
                linkedTo = Integer.parseInt(m.msgType.substring(5));
                System.out.println("connected to node: " + linkedTo);
                process.linkStatus.put(linkedTo, true);
                process.deleteExtra();
              }
              else if(linkedTo != -1 && process.linkStatus.get(linkedTo)) {
                handleMessage(m);
              }
            }
          }
          catch(ClassNotFoundException e) {
            // e.printStackTrace();
          } catch(IOException e) {
            // e.printStackTrace();
          }
      }
    }
  }

}
