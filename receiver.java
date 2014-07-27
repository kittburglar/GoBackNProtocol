import java.io.*;
import java.net.*;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.LinkedList;
import java.util.Queue;

public class receiver{
	InetAddress networkEmuHostname;
	int networkEmuPort;
	int receiverPort;
	String filename;
	DatagramSocket receiverSocket;
	int expectedSeqnum;
	int lastReceived = -1;
	Writer arrivalWriter;
	Writer fileWriter;
	Queue<packet> receivedPackets = new LinkedList<packet>();
	//PrintWriter writer = new PrintWriter("");
	//constrctor
	receiver(String networkHostname, int networkEmuPort, int receiverPort, String filename){
		//must be thrown
		try{
		this.networkEmuHostname = InetAddress.getByName(networkHostname);
		this.receiverSocket = new DatagramSocket(receiverPort);
		} catch (Exception e){
			System.err.println(e.getMessage());
		}
		this.networkEmuPort = networkEmuPort;
		this.receiverPort = receiverPort;
		this.filename = filename;
		//System.out.print(networkHostname + " " + networkEmuPort + " " + receiverPort + " " + filename + "\n");
		
	}

	public void receive(String filename) throws Exception {
		//int packetSize = packet.createPacket(0, new String( new char[500]) ).getUDPdata().length;
		byte[] ACKdata = new byte[packet.createACK(0).getUDPdata().length];
		DatagramPacket receiverPacketDatagram = new DatagramPacket(ACKdata, packet.createACK(0).getUDPdata().length);
		//receiverSocket.setSoTimeout(1000);
		while(true){
			//System.out.print("hello");
			receiverSocket.receive(receiverPacketDatagram);
			packet receiverPacket = packet.parseUDPdata(receiverPacketDatagram.getData());
			System.out.println("Received packet with seqnum: " + receiverPacket.getSeqNum());
			arrivalWriter.write(receiverPacket.getSeqNum() + "\n");
			//System.out.print(expectedSeqnum);
			if(receiverPacket.getSeqNum() == expectedSeqnum){
				//System.out.println("received expected seqnum " + receiverPacket.getSeqNum());
				lastReceived = receiverPacket.getSeqNum();
				packet ACK = packet.createACK(receiverPacket.getSeqNum());
				byte[] sendData = ACK.getUDPdata();
				DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, networkEmuHostname, networkEmuPort);
				receiverSocket.send(sendPacket);
				receivedPackets.add(receiverPacket);
				System.out.println("Sending ACK: " + receiverPacket.getSeqNum());
				expectedSeqnum = (expectedSeqnum + 1) % 32;
			}
			else if((receiverPacket.getType() == 2)){
				//packet EOTPacket = packet.createACK(lastReceived);
                                byte[] sendData = receiverPacket.getUDPdata();
                                DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, networkEmuHostname, networkEmuPort);
				receiverSocket.send(sendPacket);
				//System.out.println("sent EOT packet back");
				return;
			}
			else{
				//System.out.println("expected " + expectedSeqnum + "but got " + receiverPacket.getSeqNum());
				packet ACK = packet.createACK(lastReceived);
                                byte[] sendData = ACK.getUDPdata();
                                DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, networkEmuHostname, networkEmuPort);
                                receiverSocket.send(sendPacket);
                                //System.out.println("send ack: " + lastReceived);
                                //expectedSeqnum++;
			}
			
			
			//expectedSeqnum++;
			//System.out.print("hello");
		}

	}
	
	void writeToFile(String filename) throws Exception{
		for (packet receivedPacket : receivedPackets){
			String s = new String(receivedPacket.getData());
			fileWriter.write(s);
		}
	}
	public static void main(String[] args){
		if(args.length != 4){
			System.err.println("Usage:\n\tsender \t<host address of the network emulator>\n" +
					"\t\t<UDP port number used by the emulator to receive data from the sender>\n" +
					"\t\t<UDP port number used by the sender to receive ACKs from the emulator>\n" +
			"\t\t<name of the file to be transferred>\n");
			System.exit(1);
		}
		receiver r = new receiver(args[0], Integer.parseInt(args[1]), Integer.parseInt(args[2]), args[3]);
		try{
			//arrivalWriter = new BufferedWriter(new OutputStreamWriter(new FileOutputStream("arrival.log"), "utf-8"));
		
			//receiver r = new receiver(args[0], Integer.parseInt(args[1]), Integer.parseInt(args[2]), args[3]);
			r.arrivalWriter = new BufferedWriter(new OutputStreamWriter(new FileOutputStream("arrival.log"), "utf-8"));
			r.fileWriter = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(args[3]), "utf-8"));
			r.receive(args[3]);
			r.writeToFile(args[3]);
		}catch (Exception e){
			System.err.print(e.getMessage());
	
		}finally {
   		try {r.arrivalWriter.close(); r.fileWriter.close();} catch (Exception ex) {}
		}
	}

}
