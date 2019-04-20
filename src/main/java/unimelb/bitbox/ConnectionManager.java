package unimelb.bitbox;

import java.net.*;
import java.util.*;
import java.util.concurrent.BlockingQueue;
import java.util.logging.Logger;
import unimelb.bitbox.util.*;
import unimelb.bitbox.util.Constants.PeerSource;
import unimelb.bitbox.util.FileSystemManager.FileSystemEvent;

public class ConnectionManager implements NetworkObserver {
	
	int MAX_NO_OF_CONNECTION;
	private static Logger log = Logger.getLogger(ConnectionManager.class.getName());
	//ArrayList<Connection> connectedPeers = new ArrayList<Connection>();
	Map<String, Connection> connectedPeers;
	HostPort serverHostPort;
	private BlockingQueue<Message> incomingMessagesQueue;

	public ConnectionManager(int maxNoOfConnections, HostPort serverHostPort, BlockingQueue<Message> MessageQueue)
	{
		this.MAX_NO_OF_CONNECTION = maxNoOfConnections;
		this.incomingMessagesQueue = MessageQueue;
		this.serverHostPort = serverHostPort;
		this.connectedPeers = new HashMap<String, Connection>(); //Collections.synchronizedMap(new HashMap<>());
	}
	
	public BlockingQueue<Message> getIncomingMessagesQueue() {
		return incomingMessagesQueue;
	}
	
	public void addConnection(Socket socket, PeerSource source)
	{
		Connection connection;
		 
		if (socket!=null)
		{
			connection = new Connection(socket, this, source);
			
			//TODO GHD: To discuss with Tariq: commented the validation that should ideally be done at the server side
			//     as otherwise this is not maintain the list of active connections from the client end!
			String[] message = new String[1];
			
			message[0] = this.serverHostPort.toDoc().toJson();
			connection.send(Protocol.createMessage(Constants.Command.HANDSHAKE_RESPONSE, message));
			new Thread(connection).start();
			connectedPeers.put(connection.peer.toString(),connection);
			
			//GHD: After connection successfully added we need to start SyncEvents
			//This is not the ideal way to do it, but just easy to continue in the same manner as external events
			messageReceived(connection.peer,"{\"command\":\"SYNC_EVENTS\"}");
			
			/*
			if (connection.isHSRReceived())
			{
				if (connectedPeers.size()<MAX_NO_OF_CONNECTION)
				{
					String[] message = new String[1];
					
					message[0] = this.serverHostPort.toDoc().toJson();
					connection.send(Protocol.createMessage(Constants.Command.HANDSHAKE_RESPONSE, message));
					new Thread(connection).start();
					connectedPeers.put(connection.peer.toString(),connection);
				}
				else
				{
					connection.send(getPeerList().toJson());
				}
				
			}
			else
			{
				String[] message = new String[1];
				message[0] = "Invalid protocol";
				connection.send(Protocol.createMessage(Constants.Command.INVALID_PROTOCOL, message));
				connection.close();
				
			}
			*/
		}
	}
	
	public Document getPeerList()
	{
		Document peerList = new Document();
		ArrayList<Document> peers = new ArrayList<Document>();
		for (Connection peer: connectedPeers.values())
		{
			peers.add(peer.peer.toDoc());
		}
		peerList.append("command", Constants.Command.CONNECTION_REFUSED.toString());
		peerList.append("message", "connection limit reached");
		peerList.append("peers", peerList);
		
		return peerList;
	}


	public void sendAllPeers(Message msg) {
		String jsonMessage = msg.getJsonMessage();
		sendAllPeers(jsonMessage);
	}
	
	public void sendAllPeers(String msg) {
		String jsonMessage = msg;
		if(connectedPeers.size()<=0)
			log.warning("No Active connected Peers available to send message... Dropped");
		for (Connection conn:connectedPeers.values())
		{
			try{
				log.info(String.format("Sending to %s, Message: %s",conn.peer.toString(),jsonMessage));
				conn.send(jsonMessage);
			}
			catch(Exception e){
				log.warning("Unable to send message to peer "+conn.peer.toString()+" message: "+jsonMessage);
			}
		}
	}
	
	public void sendToPeer(HostPort peer, Message msg) {
		String jsonMessage = msg.getJsonMessage();
		sendToPeer(peer, jsonMessage);
	}
	
	public void sendToPeer(HostPort peer, String jsonMessage) {
		//get the active connection to the specific peer
		Connection conn=connectedPeers.get(peer.toString());
		if (conn!=null)
		{
			try{
				log.info(String.format("Sending to %s, Message: %s",conn.peer.toString(),jsonMessage));
				conn.send(jsonMessage);
			}
			catch(Exception e){
				log.warning("Unable to send message to peer "+conn.peer.toString()+" message: "+jsonMessage);
			}
		}
		else {
			log.info(String.format("No active connection found with peer %s to send. could be dropped. Message: %s", peer.toString(), jsonMessage));
		}
	}
	
	@Override
	public void processNetworkEvent(FileSystemEvent fileSystemEvent) {
		// TODO Auto-generated method stub
		
	}

	/**
	 * This would be triggered by a connection incase the socket close or other exception occur.
	 *  Since the connection thread would be no longer running at this stage, remove the connection from the Active list
	 */
	@Override
	public void connectionClosed(HostPort connectionID) {
		try {
			connectedPeers.remove(connectionID.toString());
			log.info("Removing Peer from active connection list "+connectionID.toString());
		}
		catch(Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * Enqueues a message in InMessageQueue. This method will be invoked by
	 * Individual Connections as they received messages.
	 */
	@Override
	public void messageReceived(HostPort connectionID, String message) {
		Message inMsg;
		try {
			inMsg = new Message(message);
			inMsg.setFromAddress(connectionID);
			incomingMessagesQueue.put(inMsg);
		} catch (Exception e) {
			// TODO GHD Maybe add invalid protocol response?
			e.printStackTrace();
		}
	}
		
}
