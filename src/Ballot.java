import java.util.*;
import java.io.*;
import java.net.*;

public class Ballot implements Comparable<Ballot>, Serializable {
  int seqNum;
  int procId;
  int depth;

  public Ballot(int seqNum, int procId, int depth) {
    this.seqNum = seqNum;
    this.procId = procId;
    this.depth = depth;
  }

  public void increaseSeqNum() {
    seqNum++;
  }

  public void increaseDepth() {
    depth++;
  }

  public int compareTo(Ballot b) { //negative if this < b
    if(this.depth < b.depth) { //b greater
      return -1;
    }
    if(b.depth < this.depth) { //b stale
      return 1;
    }
    if (b.seqNum == this.seqNum) {
      return Integer.compare(this.procId, b.procId);
    }
    return Integer.compare(this.seqNum, b.seqNum);
  }

  @Override
  public String toString() {
    return "<" + seqNum + ", " + procId + ", " + depth + ">";
  }
}
