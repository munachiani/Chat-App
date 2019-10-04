package chat;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.*;

public class Chat {

	 public static void main(String[] arg){


	        if(arg != null && arg.length > 0){

	            try{

	                int listenPort = Integer.parseInt(arg[0]);
	                Chat chatApp = new Chat(listenPort);
	                chatApp.startChat();

	            }catch(NumberFormatException nfe){
	                System.out.println("Invalid Argument for the port");
	            }

	        }else{
	            System.out.println("Invalid Args : java chat.Chat <PORT>");
	        }

	    }

    private int myPort;
    private InetAddress myIP;
    private Map<Integer, Destination> destinationsHosts = new TreeMap<>();
    private int clientCounter = 1;
    private Server messageReciever ;


    private Chat(int myPort) {
        this.myPort = myPort;
    }

    private String getmyIP(){
        return myIP.getHostAddress();
    }

    private int getmyPort(){
        return myPort;
    }


    private void help(){
        System.out.println("  --> Command manual <--");
        System.out.println("terminate <connection_id> ......... close the connection for the selected id");
        System.out.println("connect <Destination> <dest-port> . Connect with destination IP and Port");
        System.out.println("send <connection_id> <message> .... Send message using connection id");
        System.out.println("myip ...... Display the ip Address of this process");
        System.out.println("myport .... Displays the port listening for incoming connections");
        System.out.println("list ...... Display all connection with detsination hosts");
        System.out.println("exit ..... closes all connections and terminate the process");
        System.out.println("\n");

    }

    private void sendMessage(String[] commandArg){
        if(commandArg.length > 2){
            try{
                int id = Integer.parseInt(commandArg[1]);
                Destination destinationHost = destinationsHosts.get(id);
                System.out.println("id===="+destinationsHosts.get(id));
                if(destinationHost != null){
                    StringBuilder message = new StringBuilder();
                    for(int i = 2 ; i < commandArg.length ; i++){
                        message.append(commandArg[i]);
                        message.append(" ");
                    }
                    destinationHost.sendMessage(message.toString());
                    System.out.println("Mesage send successfully");
          }else
          System.out.println(
					"No Connection available with provided connection id,kindly check list command");

            }catch(NumberFormatException ne){
                System.out.println("Invalid Connection id ,check list command");
            }
        }else{
        System.out.println("Invalid command format , Kindly follow : send <connection id.> <message>");
        }
    }

    private void listDestinations(){
        System.out.println("Id:\tIP Address\tPort");
        if(destinationsHosts.isEmpty()){
            System.out.println("No Destinations available");
        }else{
            for(Integer id : destinationsHosts.keySet()){
                Destination destinationHost = destinationsHosts.get(id);
                System.out.println(id+"\t"+destinationHost.toString());
            }
        }
        System.out.println();
    }

    private void connect(String[] commandArg){

        if(commandArg != null && commandArg.length == 3){
            try {
                InetAddress remoteAddress = InetAddress.getByName(commandArg[1]);
                int remotePort = Integer.parseInt(commandArg[2]);
                Destination destinationHost = new Destination(remoteAddress,remotePort);
                if(destinationHost.initConnections()){
                	destinationsHosts.put(clientCounter, destinationHost);
                    clientCounter++;
                    System.out.println("Connected successfully");

                }else{

                    System.out.println("Unable to establish connection, try again");
                }
            }catch(NumberFormatException ne){
                System.out.println("Invalid Remote Host Port, unable to connect");
            }catch (UnknownHostException e) {
                System.out.println("Invalid Remote Host Address, unable to connect");
            }
        }else{
            System.out.println("Invalid command format , Kindly follow : connect <destination> <port no>");
        }

    }

    private void terminate(String[] commandArg){


    }

    private void startChat(){


        Scanner scanner = new Scanner(System.in);
        try{

        	 myIP = InetAddress.getLocalHost();
             messageReciever = new Server();
             new Thread(messageReciever).start();


            while(true){
                System.out.print("Enter the command :");
                String command = scanner.nextLine();
                if(command != null && command.trim().length() > 0){
                    command = command.trim();
										//common help args..
                    if(command.equalsIgnoreCase("help") || command.equalsIgnoreCase("/h") || command.equalsIgnoreCase("-h")){
                    	help();
                    }else if(command.equalsIgnoreCase("myip")){
                        System.out.println(getmyIP());
                    }else if(command.equalsIgnoreCase("myport")){
                        System.out.println(getmyPort());
                    }else if(command.startsWith("connect")){
                        String[] commandArg = command.split("\\s+");
                        connect(commandArg);
                    }
                    else if(command.equalsIgnoreCase("list")){
                    	 listDestinations();
                    }
                    else if(command.startsWith("terminate")){
                    	String[] args = command.split("\\s+");
                        terminate(args);
                    }
                    else if(command.startsWith("send")){
                    	String[] commandArg = command.split("\\s+");
                        sendMessage(commandArg);
                    }
                    else if(command.equalsIgnoreCase("exit")){
											
											  System.out.println("Closing connections...");
                        System.out.println("Chat Exited!");
                        closeAll();
                        System.exit(0);
                    }else{
                        System.out.println("Invalid command, try again!!!");
                        System.out.println();
                    }
                }else{
                    System.out.println("Invalid command, try again!!!");
                    System.out.println();
                }

            }
        }catch (UnknownHostException e) {
            e.printStackTrace();
        }finally{
            if(scanner != null)
                scanner.close();
            closeAll();
        }
    }
    private void closeAll(){
        for(Integer id : destinationsHosts.keySet()){
            Destination destinationHost = destinationsHosts.get(id);
            destinationHost.closeConnection();
        }
        destinationsHosts.clear();
        messageReciever.stopChat();
    }


		/*
				internal/helper object declarations below...
		*/

		/*
			Client class -
			connects to the specified tcp socket on a new thread (Runnable)
			listens for new messages from the connected client.
			server magages a list of these.

		*/


    private class Clients implements Runnable{

        private BufferedReader in = null;
        private Socket clientSocket = null;
        private boolean isStopped = false;
        private Clients(BufferedReader in,Socket ipAddress) {
            this.in = in;
            this.clientSocket = ipAddress;
        }

        @Override
        public void run() {

            while(!clientSocket.isClosed() && !this.isStopped)
            {
                String st;
                try {
                    st = in.readLine();
                    System.out.println("Message from "
										+clientSocket.getInetAddress().getHostAddress()
										+":"+clientSocket.getPort()+" : "+st);

                } catch (IOException e) {
                	e.printStackTrace();
                }
            }
        }

        public void stop(){

            if(in != null)
                try {
                    in.close();
                } catch (IOException e) {
                }

            if(clientSocket != null)
                try {
                    clientSocket.close();
                } catch (IOException e) {
                }
            isStopped = true;
            Thread.currentThread().interrupt();
        }

    } //end of client class

/*
	Server class -
	creates new tcp socket on a new thread (Runnable)
	this allows us to have multiple non blocking connections.

*/

    private class Server implements Runnable{

        BufferedReader in = null;
        Socket socket = null;
        boolean isStopped ;
        List<Clients> clientList = new ArrayList<Clients>();


        @Override
				public void run() {

            ServerSocket s;
            try {
                s = new ServerSocket(myPort);
                System.out.println("Server Waiting For The Client");
                while(!isStopped)
                {
                    try {
                        socket = s.accept();
                        in = new BufferedReader(new
                                InputStreamReader(socket.getInputStream()));
                        System.out.println(socket.getInetAddress().getHostAddress()
												+":"+socket.getPort()+" : client successfully connected.");

                        Clients clients = new Clients(in, socket);
                        new Thread(clients).start();
                        clientList.add(clients);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            } catch (IOException e1) {

            }

        }

        public void stopChat(){
            isStopped = true;
            for(Clients clients : clientList){
                clients.stop();
            }
            Thread.currentThread().interrupt();
        }

    }



}		//end of server class

/*
	Destination class
	wraps the socket and output stream of each client to make send messages easier.
	also help manage socket connection.

*/

class Destination{

    private InetAddress remoteHost;
    private int remotePort;
    private Socket connection;
    private PrintWriter out;
    private boolean isConnected;

    public Destination(InetAddress remoteHost, int remotePort) {
        super();
        this.remoteHost = remoteHost;
        this.remotePort = remotePort;
    }

    public boolean initConnections(){
        try {
            this.connection = new Socket(remoteHost, remotePort);
            this.out = new PrintWriter(connection.getOutputStream(), true);
            isConnected = true;
        } catch (IOException e) {

        }
        return isConnected;
    }
    public InetAddress getRemoteHost() {
        return remoteHost;
    }
    public void setRemoteHost(InetAddress remoteHost) {
        this.remoteHost = remoteHost;
    }
    public int getRemotePort() {
        return remotePort;
    }
    public void setRemotePort(int remotePort) {
        this.remotePort = remotePort;
    }

    public void sendMessage(String message){
        if(isConnected){
            out.println(message);
        }
    }
    public void closeConnection(){

        if(out != null)
            out.close();
        if(connection != null){
            try {
                connection.close();
            } catch (IOException e) {
            }
        }
        isConnected = false;
    }
    @Override
    public String toString() {
        return  remoteHost + "\t" + remotePort;
    }
}	//end of destination class
