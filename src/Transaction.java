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

  @Override
  public boolean equals(Object o) {
    if (o == this) {
        return true;
    }
    if (!(o instanceof Transaction)) {
        return false;
    }

    Transaction t = (Transaction) o;
    return t.amount == this.amount && t.debitNode == this.debitNode && t.creditNode == this.creditNode;
  }

  @Override
  public String toString() {
    return "amount: " + amount + ", debit_node: " + debitNode + ", credit_node: " + creditNode;
  }
}
