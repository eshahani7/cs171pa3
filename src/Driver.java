import java.io.*;
import java.net.*;
import java.util.*;

public class Driver {
  public static void main(String[] args) {
    Node n = new Node(Integer.parseInt(args[0]));
    n.setUp();
    n.run();
   }
}
