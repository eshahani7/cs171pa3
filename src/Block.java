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

  @Override
  public boolean equals(Object o) {
    if (o == this) {
        return true;
    }
    if (!(o instanceof Block)) {
        return false;
    }

    Block b = (Block) b;
    return b.tList.equals(this.tList) && b.proposer == this.proposer;
  }
}
