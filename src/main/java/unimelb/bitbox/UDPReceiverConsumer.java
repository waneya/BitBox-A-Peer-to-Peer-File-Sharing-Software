package unimelb.bitbox;

import java.util.logging.Logger;

import unimelb.bitbox.Err.InvalidCommandException;
import unimelb.bitbox.util.Document;
import unimelb.bitbox.util.HostPort;

public class UDPReceiverConsumer extends Thread{
	ConnectionManager connectionManager;
	HostPort remoteHostPort;
	UDPReceive udpReceive;
	UDPSend udpSend;
	private static Logger log = Logger.getLogger(UDPReceiverConsumer.class.getName());
	
	public UDPReceiverConsumer(ConnectionManager connectionManager, HostPort remoteHostPort) {
		this.connectionManager = connectionManager;
		this.remoteHostPort = remoteHostPort;
		this.udpReceive = new UDPReceive();
		this.udpSend = new UDPSend();
	}
	
	public void run() {
		
		//As long as the connection is 
		//remembered, keep listening to
		// the connection.
		log.info("Started Receiving Packets From Peer.....: " + remoteHostPort.toString());
		while(connectionManager.activePeerHostPort.containsKey(remoteHostPort.toString())) {
			try {
				boolean isTemporarySuspended = connectionManager.temporaryUDPSuspendedConsmers.containsKey(remoteHostPort.toString());
				String message = null;
				// When sending a message to a specific remote Host
				// with time-out requirements, it is neccessary that
				// this continuous consumption of packets (without timeout)
				// is stopped. This if condition is used for temoprary
				// suspension of receiving packets for this peer.
				// It is not necessary to use it everytime. It can
				// be used during SEND if packet drop increases.
				if(!isTemporarySuspended) {
					
					
						//0 timeout means No Timeout
						// Because they are consumer thread
						// listening to remote peers, there 
						// is no need of timeout with them.					
						message = udpReceive.receiveBitBoxMessage(remoteHostPort, 0);
						System.out.println( ": Complete Message at Consumer Number: "+currentThread().getName()+ " is: " + message);
						
						
						
						/* In case UDPHandShakeServer previously send a HandShake Response 
						 * but it is lost and receiver does not receive it, receiver will
						 * send HandShake Again. However, after Sending HandShake Response
						 * Peer has remembered it and stop considering it as Unknown host and
						 * hence UDPHandShakeServer will not detect it. To cater for
						 * this, UDPReeciveronsumer should be able to understand HandShake
						 * and respond to it. The below try catch is doing the same. If 
						 * message is a HandShake, this block will respond HandShake, otherwise
						 * it should be added to Message Processing Queue.				 *  
						 */
						HostPort connectingPeer = null;
						try {
							connectingPeer =  Protocol.validateHS(Document.parse(message));//validate if message is HandShake
						} catch (InvalidCommandException e1) {
							connectingPeer = null;					
							log.info("Running UDPReceiverConsumer throw InvalidCommandException"
									+ " while validating HandShake Request. Now verifying the message for other commands. ");
						}
						
						
						if(connectingPeer != null) { // i.e. the command is a Handshake_Request					
							String responsOfHandShakeRequest = Protocol.createUDPHandShakeResponse(message, connectionManager);
							udpSend.sendBitBoxMessage(remoteHostPort, responsOfHandShakeRequest);
						} else {// command is something other than handshake request.
							
							
							
							//TODO GHD Check the type of message and place it in Event Processing Queue.
							// This is for receiving File System Messages
							// We also need to implemet FILE System SEND Events with time out, for whihch I have developed basic logic
							// 
						}
				} else {
					log.info(this.getName() + ": Continues Consumption with peer " +remoteHostPort.toString() + "has been temporarily suspended");
				}
	
				
			}catch(NullPointerException ne) {
				log.severe(this.getName()+ ": NullPointerException this Consumer....Perhaps default timeout is set"
						+ "in UDPReceiver where no TimeOut is expected...Continuing consuming");
				continue;
				
			}
			catch (Exception e) {
				log.severe(this.getName()+ ": General Exception in this Consumer....Continuing consuming");
				continue;
			}
		}
	}
}

/* Discard this for now!!!!!!! Do not discard packet!!! Send it to Message Queue
//Before suspension, a packet consumption
// might have started. We want to waste 
// that packet. If actual consumption
// has not started, sender may simply
// resend. Below if condition ensures this.
// So it is just a re-check before procesing.
// It is not needed for a timed out
// HandShake Process
//if(!isTemporarySuspended) {
	// Check the type of message and place it in Event Processing Queue.
}*/
