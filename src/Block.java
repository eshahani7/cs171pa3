import java.io.*;
import java.net.*;
import java.util.*;

public class Block {
  private ArrayList<Transaction> tList;
  private int proposer;

  public Block(ArrayList<Transaction> tList, int p) {
    this.tList = tList;
    proposer = p;
  }

  public void addTransaction(Transaction t) {
    tList.add(t);
  }

  public ArrayList<Transaction> getList() {
    return tList;
  }

  @Override
  public boolean equals(Object o) {
    if (o == this) {
        return true;
    }
    if (!(o instanceof Block)) {
        return false;
    }

    Block b = (Block) o;
    return b.tList.equals(this.tList) && b.proposer == this.proposer;
  }

  @Override
  public String toString() {
    String blockStr = "";
    blockStr += "{";
    for(int i = 0; i < tList.size(); i++) {
      blockStr += tList.get(i).toString();
      if(i != tList.size() - 1) {
        blockStr += ", ";
      }
    }
    blockStr += "}";
    return blockStr;
  }
}
