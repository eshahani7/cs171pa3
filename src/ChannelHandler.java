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
  }
  //will need to access vars of node here
  public void run() {
    try {
      writer = new ObjectOutputStream(channel.getOutputStream());
      System.out.println("writer connected");
      writer.flush();
      System.out.println("flushed stream");
      System.out.println("trying to open reader");
      reader = new ObjectInputStream(channel.getInputStream());
      System.out.println("reader connected");
    } catch(IOException e) {
      e.printStackTrace();
    }

    while(true) {
      try {

        if(sendPrepare){

        }
        else if(process.ackCount >= majority) {
          //send accept
        }
        else if(process.acceptCount >= majority){
          //send decide
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
}
