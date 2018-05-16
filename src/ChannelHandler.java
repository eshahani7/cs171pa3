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
          process.sentPrepare(); //set bool to false;
        }
        else if(process.ackCount >= majority) { //only do this once, add bool
          //send accept
          //isLeader = true;
          /*
          LEADER process.ackCount >= majority
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
    }
    else if(m.msgType.equals("ack")) {
      //increment acks for node
    }
    else if(m.msgType.equals("accept")) {
      /*
      IF ACCEPTOR
      upon receiving (“accept”, b, v)
      if b >= BallotNum
      	AcceptNum = b
      AcceptVal = v
      send(“accept”, b, v) to i OR to all
      */

      //IF LEADER:
      //increment accepts for node
    }
    else if(m.msgType.equals("decision")) { //acceptor gets decision
      //call method in Node that appends block
      //clear acks and accepts
      //clear isLeader
    }
  }
}
