import java.io.*;
import java.net.*;
import java.util.*;

public class Driver {
  public static void main(String[] args) {
    Node n = new Node(Integer.parseInt(args[0]));
    n.setUp();
    n.run();

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
