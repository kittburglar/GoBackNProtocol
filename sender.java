//package senderFiles;

import java.io.*;
import java.net.*;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Queue;
import java.util.List;
//import senderFiles.*;

public class sender {
	DatagramSocket clientSocket;
	int networkDataRecievePort;
	InetAddress networkEmuIPAddress;  //Name of the machine hosting the server
	String filename;
	int ACKSize;
	int expectedSeqnum = 0;
	long timerStart;
	long estimatedTime;
	boolean EOFReceived = false;
	Writer seqnumWriter;// = new PrintWriter("seqnum.log","UTF-8");
	Writer ackWriter;// = new PrintWriter("ack.log","UTF-8");
	Queue<packet> windowQueue = new LinkedList<packet>();
	Queue<packet> allPackets = new LinkedList<packet>();
	//int seqnum;
	//constructor
	sender(String networkEmuAddress, String networkDataRecievePort, String senderACKRecievePort, String filename){
		//must throw incase the program found an exception
		try{
		this.clientSocket = new DatagramSocket(Integer.parseInt(senderACKRecievePort));
		this.networkDataRecievePort = Integer.parseInt(networkDataRecievePort);
		this.networkEmuIPAddress = InetAddress.getByName(networkEmuAddress);
		this.filename = filename;
		this.ACKSize = packet.createACK(0).getUDPdata().length;
		//System.out.print ("" + networkEmuAddress + " " + networkDataRecievePort + " " + senderACKRecievePort + " " + filename);
		}catch(Exception e){
			//System.out.print("1");
			//return null;
		}
	}
	
	public void dataToPackets(String filename) throws Exception{
		int seqnum = 0;
		int inputChar;
		int charsRead = 0;
		String data = "";
		BufferedReader inFromUser = new BufferedReader(new FileReader(filename));
		while((inputChar = inFromUser.read()) != -1){
			//System.out.print("sup");
			if(charsRead == 500){
				
				//System.out.print("making packet with " + data + "\n");
				packet addedPacket = packet.createPacket(seqnum, data);
				allPackets.add(addedPacket);
				//System.out.print(addedPacket.getSeqNum() + "\n");
				charsRead = 0;
				data = "";	
				//seqnum = (seqnum + 1) % 32;
				seqnum++;
			}
			char c = (char) inputChar;
			data = data + c;
			//allPackets.add(packet(seqnum, data));
			//seqnum++;
			charsRead++;
		}
		//leftover data
		if(data.length() > 0){
			//System.out.print("making packet with " + data + "\n");
			//packet addedPacket = packet.createPacket(seqnum, data);
			allPackets.add(packet.createPacket(seqnum, data));
			//System.out.print(addedPacket.getSeqNum() + "\n");
			
		}
	}
	
	public void fillWindow() throws Exception{
		while(allPackets.size() > 0 && windowQueue.size() < 10){
			windowQueue.add(allPackets.poll());
			//System.out.print(windowQueue.peek().getData() + "\n");
		}
	}

	public void transmit(String filename) throws Exception{
		//byte[] senderData;
		/*
		int seqnum = 0;
		BufferedReader inFromUser = new BufferedReader(new FileReader(filename));
		//System.out.print(inFromUser);
		String data = "";
		int inputChar;
		byte[] senderData;
		//MAX data length is 500
		while ((inputChar = inFromUser.read()) != -1) {
        		seqnum = (seqnum + 1) % 32;
			char c = (char) inputChar;
			data += inputChar;
			System.out.println(c);
			//packet.createPacket(seqnum, line);
		}
		*/
		//dataToPackets(filename);
		fillWindow();
		//System.out.println("transmit function");
		for(packet windowPacket : this.windowQueue){
			//packet sentPacket = packet.createPacket(seqnum, data);
			System.out.print("Sending packet with seqnum: " + windowPacket.getSeqNum() + "\n");
			seqnumWriter.write(windowPacket.getSeqNum() + "\n");
			//System.out.println(packet);
			byte[] senderData = windowPacket.getUDPdata();
			DatagramPacket sendPacket = new DatagramPacket(senderData, senderData.length, this.networkEmuIPAddress, this.networkDataRecievePort);
			clientSocket.send(sendPacket);
			timerStart = System.currentTimeMillis();
		}
		//ACKlisten();
		//Wait for ACKs until EOF ACK is receieved
		
		/*
		byte[] senderData = windowQueue.poll().getUDPdata();
		DatagramPacket sendPacket = new DatagramPacket(senderData, senderData.length, networkEmuIPAddress, networkDataRecievePort);
                clientSocket.send(sendPacket);
		*/
	}	
	
	public void sendSingle(packet p) throws Exception{
		byte[] senderData = p.getUDPdata();
                DatagramPacket sendPacket = new DatagramPacket(senderData, senderData.length, this.networkEmuIPAddress, this.networkDataRecievePort);
                //System.out.println("sending single packet with seqnum " + p.getSeqNum());
		//seqnumWriter.write("test2");
		clientSocket.send(sendPacket);
		timerStart = System.currentTimeMillis();
	}
	
	void endTransmission() throws Exception{
		packet EOTPacket = packet.createEOT(0);
		sendSingle(EOTPacket);
		System.out.println("Done!");
		return;
	}

	void slideWindow(int ack){
		//System.out.println("slinding window because we got ack " + ack);
		while((windowQueue.peek().getSeqNum() != ack)){
			windowQueue.poll();
			//System.out.println("sup1");
		}	
		if(windowQueue.peek() != null){
			//System.out.println("sup2");
			windowQueue.poll();
			//System.out.println("sup3");
		}
		
		//System.out.println("sup4");
	}
	int getIndex(int item){
		int i = 0;
		//Queue<packet> clonePackets = new LinkedList<packet>(windowQueue);
		List<packet> l = new ArrayList<packet>(windowQueue);
		for (i = l.size()-1; i > 0; i--) {
                
			if(l.get(i).getSeqNum() == item){
				return i;
			}
			//System.out.println(linkedList.get(i));
            	}
		return i;
	}
	void ACKlisten() throws Exception{
		
                int packetSize = packet.createPacket(0, new String( new char[500]) ).getUDPdata().length;
               	byte[] ACKdata = new byte[packetSize];
                DatagramPacket ACKDatagram = new DatagramPacket(ACKdata, packetSize);
                clientSocket.setSoTimeout(1000);
		//timer expires?
		while((System.currentTimeMillis() - timerStart) < 1000){
			try {//
			estimatedTime = System.currentTimeMillis() - timerStart;
		       	///System.out.println("first "+ estimatedTime);
			clientSocket.receive(ACKDatagram);
			//System.out.println("second "+ estimatedTime);
			//receiverSocket.receive(receiverPacketDatagram);
                        packet ACKPacket = packet.parseUDPdata(ACKDatagram.getData());
                	System.out.println("Received ACK: " + ACKPacket.getSeqNum());
			if(ACKPacket.getSeqNum() > -1)
			ackWriter.write(ACKPacket.getSeqNum() + "\n");
			if((windowQueue.peek() == null) && (allPackets.peek() == null)){
                                //System.out.println("no more in windowQueue or allPackets");
                                break;
                        }
			if(getIndex(windowQueue.peek().getSeqNum()) < getIndex(ACKPacket.getSeqNum())){
			//if(getIndex(windowQueue.peek().getSeqNum()) < getIndex(ACKPacket.getSeqNum())){
				slideWindow(ACKPacket.getSeqNum());
			}
			//if(windowQueue.peek() == null){
			if((windowQueue.peek() == null) && (allPackets.peek() == null)){
				//System.out.println("no more in windowQueue or allPackets");
				break;
			}
			//System.out.println("sup5");
			//if((s.windowQueue.peek() == null) && (s.allPackets.peek() == null)){
			if((windowQueue.peek().getSeqNum() == ACKPacket.getSeqNum())){
				//if(allPackets.peek() != null){
				//System.out.println("sliding window");
				expectedSeqnum++;
				windowQueue.poll();
				/*
				//allPackets.poll();
				//if(allPackets.peek() != null){
					packet nextToSend = allPackets.peek();
					//fillWindow();
					System.out.println("sliding window to: " + windowQueue.peek().getSeqNum());
					System.out.println("hello");
					if(nextToSend != null){
						sendSingle(nextToSend);
					}
					System.out.println("world");
					//timerStart = System.currentTimeMillis();
				//}
				if((allPackets.peek() == null) && (windowQueue.peek() == null)){
					endTransmission();
				}
				*/
				
			}
			else if(ACKPacket.getType() == 2){
				System.exit(0);
			}
			/*
			else{
				System.out.println("resending packets in window due to repeat ACK recieved");
				break;
				//transmit(filename);
			}
			*/
			}
			catch (SocketTimeoutException e1) {
				//System.out.println("Hello");
			}
		}
		//System.out.println("over 5000!");
	}
	
	public static void main(String [ ] args) {
		//Check if input is correct
		//seqnumWriter = new PrintWriter("seqnum.log","UTF-8");
        	//ackWriter = new PrintWriter("ack.log","UTF-8");
		if( args.length != 4) {
			System.err.println("Usage:\n\tsender \t<host address of the network emulator>\n" +
					"\t\t<UDP port number used by the emulator to receive data from the sender>\n" +
					"\t\t<UDP port number used by the sender to receive ACKs from the emulator>\n" +
					"\t\t<name of the file to be transferred>\n");
			System.exit(1);
		}
		//Make sender object
		sender s = new sender(args[0],args[1],args[2],args[3]);
		try{
		s.seqnumWriter = new BufferedWriter(new OutputStreamWriter(new FileOutputStream("seqnum.log"), "utf-8"));
		s.ackWriter = new BufferedWriter(new OutputStreamWriter(new FileOutputStream("ack.log"), "utf-8"));
		//s.seqnumWriter.write("test2");
		
		s.dataToPackets(args[3]);
                s.fillWindow();
                //ACKlisten();
		while(!((s.windowQueue.peek() == null) && (s.allPackets.peek() == null))){
			s.transmit(args[3]);
			s.ACKlisten();
		}
		s.endTransmission();
		} catch(Exception e) {
			System.err.println("Whoops! Something unexepceted happened. Please try again! :)");
		}finally {
  	 	try {s.seqnumWriter.close();s.ackWriter.close();} catch (Exception ex) {}
		}
	}

}
