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
          /*
          BallotNum = <BallotNum.num + 1, myID)
	        send(“prepare”, BallotNum) to all
          */
          Ballot bal = process.ballotNum;
          bal.increaseSeqNum();
          Message m = new Message("prepare", bal, null, null);
          writer.writeObject(m);
          writer.flush();
          writer.reset();
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
          Message m = new Message("accept", process.ballotNum, myVal);
          writer.writeObject(m);
          writer.flush();
          writer.reset();
          /*
          LEADER
          upon receiving (“ack”, BallotNum, b, v) from majority
          if v = null for all
            myVal = initial value
          else
            myVal = v with highest b
          send(“accept”, b, myVal) to all
          */
        }
        else if(process.acceptCount >= majority) { //only do this once, add bool
          //send decide
          /*
          upon receiving majority of accepts
          send(“decision”, b, v) to all
          //call method in Node that appends block
          //clear acks and accepts
          //clear isLeader
          */

          Message m = new Message("decision", process.ballotNum, myVal);
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
      /* ACCEPTOR RECEIVES MESSAGE
      if bal >= BallotNum
	     BallotNum = bal
	     send(“ack”, BallotNum, AcceptNum, AcceptVal) to i
      */
      if(m.bal >= process.ballotNum) {
        process.ballotNum = m.bal;
        Message m = new Message("ack", process.ballotNum, process.acceptNum, process.acceptVal);
        writer.writeObject(m);
        writer.flush();
        writer.reset();
      }
    }
    else if(m.msgType.equals("ack")) {
      //increment acks for node
      process.ackCount++;
      acks.add(m);
    }
    else if(m.msgType.equals("accept")) {
      //IF LEADER:
      //increment accepts for node
      /*
      IF ACCEPTOR
      upon receiving (“accept”, b, v)
      if b >= BallotNum
      	AcceptNum = b
        AcceptVal = v
        send(“accept”, b, v) to i OR to all
      */
      if(isLeader) {
        process.acceptCount++;
      }
      else if(m.bal >= process.ballotNum) {
        process.acceptNum = m.a;
        process.acceptVal = m.v;
        Message m = new Message("accept", process.ballotNum, process.acceptVal);
        writer.writeObject(m);
        writer.flush();
        writer.reset();
      }
    }
    else if(m.msgType.equals("decision")) { //acceptor gets decision
      //call method in Node that appends block
      //clear acks and accepts
      //clear isLeader
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
      else if(acks.get(i).a > highest) {
        highest = acks.get(i).a;
        highestIndex = i;
      }
    }

    return highestIndex;
  }
}
