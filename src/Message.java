import java.io.*;
import java.net.*;
import java.util.*;

public class Message implements Serializable {

  String msgType = null;
  Ballot bal = null;
  Ballot a = null;
  Block v = null;

  //access directly, set unecessary fields to null for different message types
  public Message(String msgType, Ballot bal, Ballot a, Block val) {
    this.msgType = msgType;
    this.bal = bal;
    this.a = a;
    this.v = val;
  }

  // public Message(String msgType, Ballot bal, Block val) {
  //   this.msgType = msgType;
  //   this.bal = bal;
  //   this.v = val;
  // }
}
