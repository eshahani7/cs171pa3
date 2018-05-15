public class Message {

  String msgType;
  Ballot bal;
  Ballot a;
  Block v;

  //access directly, set unecessary fields to null for different message types
  public Message(int msgType, Ballot bal, Ballot a, Block val) {
    this.msgType = msgType;
    this.bal = bal;
    this.a = a;
    this.v = v;
  }
}
