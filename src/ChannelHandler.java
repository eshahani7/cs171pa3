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

  private ArrayList<Message> acks = new ArrayList<Message>();
  Block myVal;

  public ChannelHandler(Socket in, Node n) {
    channel = in;
    process = n;
    majority = 3;
    isLeader = false;
  }

  public void clearVars() {
    isLeader = false;
    acks.clear();
    myVal = null;
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

    while(true) {
      try {
        if(process.sendPrepare){
          Ballot bal = process.ballotNum;
          bal.increaseSeqNum();
          Message send = new Message("prepare", bal, null, null);
          sendMessage(send);
          process.sendPrepare = false;
        }
        else if(process.ackCount >= majority && !isLeader) { //only do this once, add bool
          //send accept
          isLeader = true;
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
        else if(process.acceptCount >= majority) { //only do this once, add bool
          Message m = new Message("decision", process.ballotNum, null, myVal);
          clearVars();
          process.appendBlock(myVal);
          writer.writeObject(m);
          writer.flush();
          writer.reset();
        }

        // read message
        Object msgObj = reader.readObject();
        Message m = (Message) msgObj;
        handleMessage(m);
      } catch(ClassNotFoundException e) {
        e.printStackTrace();
      } catch(IOException e) {
        e.printStackTrace();
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
      process.ackCount++;
      acks.add(m);
    }
    else if(m.msgType.equals("accept")) {
      System.out.println("got accept");
      if(isLeader) {
        process.acceptCount++;
      }
      else if(m.bal.compareTo(process.ballotNum) >= 0) {
        process.acceptNum = m.a;
        process.acceptVal = m.v;
        Message send = new Message("accept", process.ballotNum, null, process.acceptVal);
        sendMessage(send);
      }
    }
    else if(m.msgType.equals("decision")) { //acceptor gets decision
      System.out.println("got decision");
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
      else if(acks.get(i).a.compareTo(highest) > 0) {
        highest = acks.get(i).a;
        highestIndex = i;
      }
    }

    return highestIndex;
  }

  private void sendMessage(Message send) {
    try {
      writer.writeObject(send);
      writer.flush();
      writer.reset();
    } catch(IOException e) {
      e.printStackTrace();
    }
  }
}
