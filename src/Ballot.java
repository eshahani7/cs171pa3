import java.util.*;

public class Ballot implements Comparable<Ballot> {
  int seqNum;
  int procId;
  int depth;

  public Ballot(int seqNum, int procId, int depth) {
    this.seqNum = seqNum;
    this.procId = procId;
    this.depth = depth;
  }

  public int compareTo(Ballot b) { //negative if this < b
    if (b.seqNum == this.seqNum) {
      return Integer.compare(this.procId, b.procId);
    }
    return Integer.compare(this.seqNum, b.seqNum);
  }
}
