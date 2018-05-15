import java.io.*;
import java.net.*;
import java.util.*;

public class Transaction {
  public int amount;
  public int debitNode;
  public int creditNode;

  public Transaction(int amount, int debitNode, int creditNode) {
    this.amount = amount;
    this.debitNode = debitNode;
    this.creditNode = creditNode;
  }
}
