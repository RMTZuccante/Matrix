package it.rmtz.utils;

import com.fazecast.jSerialComm.SerialPort;
import it.rmtz.matrix.SerialConnector;

import java.util.Arrays;
import java.util.Scanner;

public class SerialTest {
    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        SerialConnector stm;

        if(args.length > 0) {
            stm = new SerialConnector(SerialPort.getCommPort(args[0]), 115200);
        }
        else {
            System.out.print("Port: ");
            stm = new SerialConnector(SerialPort.getCommPort(sc.nextLine()), 115200);
        }
        System.out.println("Port opened.");
        while(!stm.handShake()) System.out.println("Trying handshake...");
        System.out.println("Connected!");

        while (true) {
            System.out.print("Inserisci comando: ");
            switch (sc.nextLine().toLowerCase()) {
                case "rotate":
                    System.out.print("Angle: ");
                    stm.rotate(sc.nextInt());
                    sc.nextLine(); //consuming a nextline char
                    System.out.println("Rotate ended.");
                    break;
                case "go":
                    System.out.println("Go ended with code: "+stm.go());
                    break;
                case "getdistances":
                    System.out.println("Distances: "+ Arrays.toString(stm.getDistances()));
                    break;
                case "getcolor":
                    System.out.println("Color: "+stm.getColor());
                    break;
                case "gettemps":
                    System.out.println("Temperatures: "+Arrays.toString(stm.getTemps()));
                    break;
                case "victim":
                    System.out.print("Packets: ");
                    stm.victim(sc.nextInt());
                    sc.nextLine(); //consuming a nextline char
                    System.out.println("Victim ended.");
                    break;
                case "setdebug":
                    System.out.print("Level number: ");
                    sc.nextLine(); //consuming a nextline char
                    stm.setDebug(sc.nextByte());
                    break;
                case "setblack":
                    System.out.print("New black threshold: ");
                    sc.nextLine();
                    stm.setBlackThreshold(sc.nextByte());
                case "getconnectioninfo":
                    System.out.println(stm.getConnectionInfo());
                    break;
                case "exit":
                    System.exit(0);
                    break;
                default:
                    System.out.println("Unknown command!");
                    break;
            }
        }
    }
}
