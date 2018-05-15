import java.io.*;
import java.net.*;
import java.util.*;

public class ChannelHandler extends Thread {

  private Socket channel;
  private Node process;
  private boolean sendPrepare;
  private int majority;

  private ObjectOutputStream writer = null;
  private ObjectInputStream reader = null;

  public ChannelHandler(Socket in, Node n) {
    channel = in;
    process = n;
    sendPrepare = false;
    majority = 3;

    try {
      writer = new ObjectOutputStream(channel.getOutgoing().getOutputStream());
      reader = new ObjectInputStream(channel.getIncoming().getInputStream());
    } catch(IOException e) {
      e.printStackTrace();
    }
  }

  //will need to access vars of node here
  public void run() {
    while(true) {
      try {

        if(sendPrepare){

        }
        else if(n.ackCount >= majority) {
          //send accept
        }
        else if(n.acceptCount >= majority){
          //send decide
        }

        //read message
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

    }
    else if(m.msgType.equals("ack")) {

    }
    else if(m.msgType.equals("accept")) {

    }
    else if(m.msgType.equals("decision")) {

    }
  }

  public void prepare() {
    sendPrepare = true;
  }

  public void decide(){
    sendDecide = true;
  }
}
