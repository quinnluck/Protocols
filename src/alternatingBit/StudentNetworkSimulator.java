package alternatingBit;

import java.security.*;
import java.util.ArrayList;
import java.util.HashSet;
public class StudentNetworkSimulator extends NetworkSimulator
{
    /*
     * Predefined Constants (static member variables):
     *
     *   int MAXDATASIZE : the maximum size of the Message data and
     *                     Packet payload
     *
     *   int A           : a predefined integer that represents entity A
     *   int B           : a predefined integer that represents entity B
     *
     *
     * Predefined Member Methods:
     * ~~~~ ~~~~ ~~~~ ~~~~ ~~~~ ~~~~ ~~~~ ~~~~ ~~~~ ~~~~ ~~~~ ~~~~ ~~~~ ~~~~
     *  void stopTimer(int entity): 
     *       Stops the timer running at "entity" [A or B]
     *  void startTimer(int entity, double increment): 
     *       Starts a timer running at "entity" [A or B], which will expire in
     *       "increment" time units, causing the interrupt handler to be
     *       called.  You should only call this with A.
     *  void toLayer3(int callingEntity, Packet p)
     *       Puts the packet "p" into the network from "callingEntity" [A or B]
     *  void toLayer5(int entity, String dataSent)
     *       Passes "dataSent" up to layer 5 from "entity" [A or B]
     *  double getTime()
     *       Returns the current time in the simulator.  Might be useful for
     *       debugging.
     *  void printEventList()
     *       Prints the current event list to stdout.  Might be useful for
     *       debugging, but probably not.
     *
     *
     *  Predefined Classes:
     *
     *  ~~~~ Message: Used to encapsulate a message coming from layer 5 ~~~~
     *    Constructor:
     *      Message(String inputData): 
     *          creates a new Message containing "inputData"
     *    Methods:
     *      boolean setData(String inputData):
     *          sets an existing Message's data to "inputData"
     *          returns true on success, false otherwise
     *      String getData():
     *          returns the data contained in the message
     *  ~~~~ Packet: Used to encapsulate a packet ~~~~
     *    Constructors:
     *      Packet (Packet p):
     *          creates a new Packet that is a copy of "p"
     *      Packet (int seq, int ack, int check, String newPayload)
     *          creates a new Packet with a sequence field of "seq", an
     *          ack field of "ack", a checksum field of "check", and a
     *          payload of "newPayload"
     *      Packet (int seq, int ack, int check)
     *          chreate a new Packet with a sequence field of "seq", an
     *          ack field of "ack", a checksum field of "check", and
     *          an empty payload
     *    Methods:
     *      boolean setSeqnum(int n)
     *          sets the Packet's sequence field to "n"
     *          returns true on success, false otherwise
     *      boolean setAcknum(int n)
     *          sets the Packet's ack field to "n"
     *          returns true on success, false otherwise
     *      boolean setChecksum(int n)
     *          sets the Packet's checksum to "n"
     *          returns true on success, false otherwise
     *      boolean setPayload(String newPayload)
     *          sets the Packet's payload to "newPayload"
     *          returns true on success, false otherwise
     *      int getSeqnum()
     *          returns the contents of the Packet's sequence field
     *      int getAcknum()
     *          returns the contents of the Packet's ack field
     *      int getChecksum()
     *          returns the checksum of the Packet
     *      int getPayload()
     *          returns the Packet's payload
     *
     */

    // Add any necessary class variables here.  Remember, you cannot use
    // these variables to send messages error free!  They can only hold
    // state information for A or B.
    // Also add any necessary methods (e.g. checksum of a String)
	
	/*
	 * keeps track of the Ack number
	 */
	int Ack = 0;
	/*
	 * keeps track of the Sequence number of the packets
	 */
	int Seq = 0;
	/*
	 * keeps track of the last packet.  Used when loss occurs
	 */
	Packet lastPacket;
	/*
	 * these two variables keep track of where the lost packet comes from
	 */
	boolean fromA = false;
	boolean fromB = false;
	/*
	 * keeps track of an active timer.
	 */
	boolean timerActive = false;
	/*
	 * These are all statistics variables used when printing statistics.
	 */
	int lostPackets = 0;
	int corruptPackets = 0;
	int AcksSent = 0;
	int AcksRec = 0;
	int packetsSentApp = 0;
	int packetsRecApp = 0;
	int packetsSentProt = 0;
	int packetsRecProt = 0;
	int wrongAcks = 0;
	int wrongSeq = 0;
	
	// ~~~~ private variables for Go Back N implementation ~~~~
	
	/*
	 * Holder for all the packets sent
	 */
	ArrayList<Packet> toBeSent = new ArrayList<Packet>();
	/*
	 * Window size, Fixed.
	 */
	int N = 50;
	/*
	 * Sent, not yet ACK'd
	 */
	int base = 0;
	/*
	 * Usable, not yet sent packets
	 */
	int nextSeq = 0;
	/*
	 * The expected sequence number coming from side A
	 */
	int expectedSeq = 0;
	
	
	/*
	 * We can assume in this method that the string is not null. returns a checksum value
	 * for a given string, if the string is corrupt, the checksum will be -1
	 * message parameter - payload of the packet
	 * Returns a valid value for the checksum
	 */
	public int checkSum(String message)
	{
			byte[] strBytes = message.getBytes();
			try 
			{ 
				MessageDigest md = MessageDigest.getInstance("MD5");
				md.update(strBytes);
				
				//convert it to a readable hex output checksum, then to an integer.
				StringBuffer builder = new StringBuffer();
				for(int i = 0; i < strBytes.length; i++)
				{
					builder.append(Integer.toString((strBytes[i] & 0xff) + 0x100, 16).substring(1));
				}
				
				try 
				{
					return md.digest(strBytes, 0, MAXDATASIZE);
				} 
				catch (DigestException e) 
				{
					System.out.println("Couldn't run the checksum for: " + message);
					e.printStackTrace();
					return -1;
				}	
			}
			catch(Exception e)
			{
				System.out.println("Couldn't run the checksum for: " + message);
				return -1;
			}
	}
	
	/*
	 * sets and returns the next sequence number in bytes.  Sequence numbers increment by 20
	 */
	public int nextSeqNumber(){
		int next = Seq;
		next += 20;
		return next;
	}
	
	/*
	 * Gives the last sequence number in bytes.  Sequence numbers increment by 20
	 */
	public int lastSeqNumber(){
		int last = Seq - 20;
		return last;
	}
	
	/*
	 * used to print out all the statistics in our network simulator.
	 */
	public void Stats() {
		System.out.println("~~~~~~ STATISTICS SO FAR ~~~~~~");
		System.out.println("LOST PACKETS: " + lostPackets);
		System.out.println("CORRUPT PACKETS: " + corruptPackets);
		System.out.println("PACKETS SENT BY APPLICATION: " + packetsSentApp);
		System.out.println("PACKETS RECEIVED BY APPLICATION: " + packetsRecApp);
		System.out.println("PACKETS SENT BY PROTOCOL: " + packetsSentProt);
		System.out.println("PACKETS RECEIVED BY PROTOCOL: " + packetsRecProt);
		System.out.println("ACKS SENT: " + AcksSent);
		System.out.println("ACKS RECEIVED: " + AcksRec);
		System.out.println("WRONG SEQUENCE NUMBER IN PACKET: " + wrongSeq);
		System.out.println("WRONG ACK IN PACKET: " + wrongAcks);
	}

    // This is the constructor.  Don't touch!
    public StudentNetworkSimulator(int numMessages,
                                   double loss,
                                   double corrupt,
                                   double avgDelay,
                                   int trace,
                                   long seed)
    {
        super(numMessages, loss, corrupt, avgDelay, trace, seed);
    }

    // This routine will be called whenever the upper layer at the sender [A]
    // has a message to send.  It is the job of your protocol to insure that
    // the data in such a message is delivered in-order, and correctly, to
    // the receiving upper layer.
    protected void aOutput(Message message)
    {
    	// Gets the message from the Application and checks the checkSum
    	String payload = message.getData();
    	int checkMessage = checkSum(payload);
    	packetsSentApp++;
		Packet pack = new Packet(nextSeq, 0, checkMessage, payload);

    	// check if buffer is full.
    	if(nextSeq < (base + N)){
    		// add to list, send it
    		toBeSent.add(pack);
    		this.toLayer3(A,  pack);
        	packetsSentProt++;
        	System.out.println("SENT TO LAYER 3 FROM A-OUTPUT");
    		System.out.println("SENT WITH SEQ NUMBER FROM A-OUTPUT: " + nextSeq);
    		// if we've sent something start the timer.
    		if(base == nextSeq){
    			this.startTimer(A, 1000);
    			System.out.println("STARTING TIMER FROM A-OUTPUT");
    		}
    		nextSeq++;
    	}
    	else{ /* do nothing */ }
    		
//    	if (timerActive == false) {
//    		// create a new packet to send 
//    		Packet pack = new Packet(Seq, Ack, checkMessage, payload);
//    		lastPacket = new Packet(pack);
//    	
//    		// send packet over network
//    		this.toLayer3(A, pack);
//    		packetsSentProt++;
//    		fromA = true;
//    		System.out.println("SENT TO LAYER 3 FROM A-OUTPUT");
//    		System.out.println("SENT WITH ACK FROM A-OUTPUT: " + Ack);
//    		this.startTimer(A, 1000);
//    		timerActive = true;
//        
//    		// if the Ack is 0, change it to a 1, if it is 1, change it to a 0
//    		if(Ack == 0){
//    			Ack++;
//    			this.nextSeqNumber();
//    			System.out.println("ACK INCREMENTED");
//    			System.out.println("NEXT SEQUENCE NUMBER: " + Seq);
//    		}
//    		else {
//    			Ack--;
//    			this.nextSeqNumber();
//    			System.out.println("ACK DECREMENTED");
//    			System.out.println("NEXT SEQUENCE NUMBER: " + Seq);
//    		}    	
//    	}
    }
    
    // This routine will be called whenever a packet sent from the B-side 
    // (i.e. as a result of a toLayer3() being done by a B-side procedure)
    // arrives at the A-side.  "packet" is the (possibly corrupted) packet
    // sent from the B-side.
    protected void aInput(Packet packet)
    {   	
    	// get info out of packet
    	String payload = packet.getPayload();
    	int arrivingCheck = packet.getChecksum();
    	// v v v  this is the Seqnum v v v 
    	int arrivingAck = packet.getAcknum();
    	packetsRecProt++;
    	
    	// calculate our own checkSum on the received packet
    	int check = checkSum(payload);
    	System.out.println("A-INPUT CHECKSUM: " + check + " A-INPUT ARRIVING CHECKSUM: " + arrivingCheck);
		System.out.println("ACK FROM A-INPUT: " + arrivingAck);
    	
		// if everything has been sent, stop the timer, If not, we want to restart the timer.
    	if(check == arrivingCheck){
    		base = arrivingAck + 1;
    		if(base == nextSeq){
    			this.stopTimer(A);
    		}
    		else{
    			this.stopTimer(A);
    			this.startTimer(A, 1000);
    		}
    	}
    	else { System.out.println("CORRUPT PACKET IN A-INPUT"); corruptPackets++; }
    	this.Stats();
	
//    	// pull out everything we need from the packet
//    	String payload = packet.getPayload();
//    	int arrivingCheck = packet.getChecksum();
//    	int arrivingSeq = packet.getSeqnum();
//    	int arrivingAck = packet.getAcknum();
//    	
//    	// calculate our own checkSum on the received packet
//    	int check = checkSum(payload);
//    	int last = this.lastSeqNumber();
//    	System.out.println("A-INPUT CHECKSUM " + check + " A-INPUT ARRIVING CHECKSUM " + arrivingCheck);
//		System.out.println("LAST SEQ FROM A-INPUT " + last + " A-INPUT ARRIVING SEQ " + arrivingSeq);
//		System.out.println("NEW ACK FROM A-INPUT " + Ack + " A-INPUT ARRIVING ACK " + arrivingAck);
//		packetsRecProt++;
//		
//		// make checks for Sequence number, Ack number, and the checkSum
//    	if(arrivingSeq == last){
//    		if(arrivingAck != Ack) {
//    			if(check == arrivingCheck) {
//    				fromB = false;
//    				AcksRec++;
//    				System.out.println("STOP TIMER FROM A INPUT INNITIATING");
//    				this.stopTimer(A);
//    				timerActive = false;
//    			}
//    			else { System.out.println("CORRUPT PACKET IN A-INPUT"); corruptPackets++; }
//    		}
//    		else { System.out.println("WRONG ACK FROM A-INPUT"); wrongAcks++; }
//    	}
//    	else { System.out.println("WRONG SEQUENCE NUMBER FROM A-INPUT"); wrongSeq++; }
//    	this.Stats();
    }
    
    // This routine will be called when A's timer expires (thus generating a 
    // timer interrupt). You'll probably want to use this routine to control 
    // the retransmission of packets. See startTimer() and stopTimer(), above,
    // for how the timer is started and stopped. 
    protected void aTimerInterrupt()
    {
    	System.out.println("TIMEOUT SOMEWHERE - FROM A TIMER INTERRUPT");
    	lostPackets++;
    	
    	this.startTimer(A, 1000);
    	
    	// send everything again!
    	for(int i = base; i < nextSeq; i++){
    		Packet pack = toBeSent.get(i);
    		System.out.println("RE-SEND PACKET: " + pack.toString());
    		this.toLayer3(A,  pack);
			packetsSentProt++;
    	}
  	
//    	timerActive = false;
    	
//    	// send the last packet to the correct destination, packets can get lost going both ways.
//    	if(fromB == true){
//    		this.toLayer3(B, lastPacket);
//    		packetsSentProt++;
//    	}
//    	else if(fromA == true){
//    		this.toLayer3(A, lastPacket);
//        	packetsSentProt++;
//            this.startTimer(A, 1000);
//            timerActive = true;
//    	}
    }
    
    // This routine will be called once, before any of your other A-side 
    // routines are called. It can be used to do any required
    // initialization (e.g. of member variables you add to control the state
    // of entity A).
    protected void aInit()
    {
    	// initializing for A, not used in my implementation
    }
    
    // This routine will be called whenever a packet sent from the B-side 
    // (i.e. as a result of a toLayer3() being done by an A-side procedure)
    // arrives at the B-side.  "packet" is the (possibly corrupted) packet
    // sent from the A-side.
    protected void bInput(Packet packet)
    {
    	// get info out of packet
    	String payload = packet.getPayload();
    	int arrivingCheck = packet.getChecksum();
    	int arrivingSeq = packet.getSeqnum();
    	packetsRecProt++;
    	
    	// calculate our own checkSum on the received packet
    	int check = checkSum(payload);
    	System.out.println("B-INPUT CHECKSUM " + check + " B-INPUT ARRIVING CHECKSUM " + arrivingCheck);
		System.out.println("LAST SEQ FROM B-INPUT " + expectedSeq + " B-INPUT ARRIVING SEQ " + arrivingSeq);
    	
		// checks for the packet!
    	if(arrivingSeq == expectedSeq){
    		if(check == arrivingCheck){
    			this.toLayer5(B, payload);
    			System.out.println("PACKET DELIVERED UP TO LAYER 5 FROM B-INPUT");
				packetsRecApp++;
				
				// send a reply packet with ACK as the sequence number received.
				Packet reply = new Packet(0, arrivingSeq, check, payload);
				System.out.println("SENDING REPLY FROM B-INPUT");
		    	this.toLayer3(B, reply);
		    	packetsSentProt++;
		    	AcksSent++;
		    	expectedSeq++;
    		}
    		else { System.out.println("CORRUPT PACKET IN A-INPUT"); corruptPackets++; }
    	}
    	else { System.out.println("WRONG SEQUENCE NUMBER FROM A-INPUT"); wrongSeq++; }
    	

//    	// get everything we need out of the received packet
//    	String payload = packet.getPayload();
//    	int arrivingCheck = packet.getChecksum();
//    	int arrivingSeq = packet.getSeqnum();
//    	int arrivingAck = packet.getAcknum();
//    	
//    	// calculate our own checkSum
//    	int check = checkSum(payload);
//    	int lastSequence = this.lastSeqNumber();
//    	System.out.println("B-INPUT CHECKSUM " + check + " B-INPUT ARRIVING CHECKSUM " + arrivingCheck);
//		System.out.println("LAST SEQ FROM B-INPUT " + lastSequence + " B-INPUT ARRIVING SEQ " + arrivingSeq);
//		System.out.println("NEW ACK FROM B-INPUT " + Ack + " B-INPUT ARRIVING ACK " + arrivingAck);
//    	packetsRecProt++;
//    	
//    	// Check all necessary parts of the packet to make sure we have the correct one.
//    	if(arrivingSeq == lastSequence) {
//    		if(arrivingAck != Ack) {  // we don't care about this in this from aOutput...
//    			if(check == arrivingCheck) {
//    				this.toLayer5(B, payload);
//    				packetsRecApp++;
//    				fromA = false;
//    				System.out.println("PACKET DELIVERED UP TO LAYER 5 FROM B-INPUT");
//    				
//    				//send a reply to A
//    				Packet reply = new Packet(arrivingSeq, arrivingAck, check, payload);
//    				System.out.println("SENDING REPLY FROM B-INPUT");
//    		    	this.toLayer3(B, reply);
//    		    	packetsSentProt++;
//    		    	AcksSent++;
//    		    	fromB = true;
//    			}
//    			else { System.out.println("CORRUPT PACKET FROM B-INPUT"); corruptPackets++; }
//    		}
//    		else { System.out.println("WRONG ACK FROM B-INPUT"); wrongAcks++; }	
//    	}
//    	else { System.out.println("WRONG SEQUENCE NUMBER FROM B-INPUT"); wrongSeq++; }	
    	
    }
    
    // This routine will be called once, before any of your other B-side 
    // routines are called. It can be used to do any required
    // initialization (e.g. of member variables you add to control the state
    // of entity B).
    protected void bInit()
    {
    	// initializing for B, not used in my implementation
    }
}
