import java.io.*;
import java.net.*;
import java.util.*;

public class Driver {
  public static void main(String[] args) {
    Node n = null;
    try {
      ObjectInputStream is = new ObjectInputStream(new FileInputStream("Save"+Integer.parseInt(args[0])+".txt"));
      System.out.println("Recovered from Save"+Integer.parseInt(args[0]));
      n = (Node) is.readObject();
      n.restore();
      n.clearVars();
      n.recovered = true;
      System.out.println("I recovered");
    } catch (IOException e){
        e.printStackTrace();
        n = new Node(Integer.parseInt(args[0]));
        n.check = 1;

    } catch (ClassNotFoundException e){
      e.printStackTrace();
    }
    
    n.setUp();

    while (true){
    	Scanner keyboard = new Scanner(System.in);
    	String cmd = keyboard.nextLine();
    	if (cmd.charAt(0) == 'm'){
    		String[] splitStr = cmd.split("\\s+");
    		int amount = Integer.parseInt(splitStr[1]);
    		int deb = Integer.parseInt(splitStr[2]);
    		int cred = Integer.parseInt(splitStr[3]);
    		n.moneyTransfer(amount,deb,cred);
    	}
    	else if(cmd.equals("printBlockchain")){
    		n.printBlockchain();
    	}
    	else if(cmd.equals("printBalance")){
    		n.printBalance();
    	}
    	else if(cmd.equals("printQueue")){
    		n.printQueue();
    	}
    }

  }
}
