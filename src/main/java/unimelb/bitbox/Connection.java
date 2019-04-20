package unimelb.bitbox;
import java.net.*;
import java.io.*;
import java.util.logging.Logger;
import unimelb.bitbox.util.*;
import unimelb.bitbox.util.Constants.*;

public class Connection implements Runnable {
	
	BufferedReader in;
	BufferedWriter out;
	Socket clientSocket;
	NetworkObserver networkObserver;
	//Seems risky to define it at class level
	//String inBuffer;
	PeerSource peerSource;
	String outBuffer;
	Logger log;
	HostPort peer;   
	private volatile boolean running = true;

	public Connection(Socket clientSocket, NetworkObserver connectionManager, PeerSource source) 
	{
		log = Logger.getLogger(Connection.class.getName());
		try
		{
			this.in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream(),"UTF8"));  
			this.out = new BufferedWriter(new OutputStreamWriter(clientSocket.getOutputStream(),"UTF8"));
			this.clientSocket = clientSocket;
			//GHD: Set socket read timeout and keep alive flags
			this.clientSocket.setKeepAlive(true);
			this.clientSocket.setSoTimeout(Constants.BITBOX_SOCKET_TIMEOUT);
			
			this.networkObserver = connectionManager;
			//connection ip/port
			this.peer = new HostPort(clientSocket.getInetAddress().getHostAddress(), clientSocket.getPort());
			this.peerSource = source;
			log.info("Active Connection maintained for: "+peer.toString());
		}
		
		catch (Exception e)
		{
			log.warning(e.getMessage());
		}
				
	}
	
	/**
	 * added in case we need to temporarily stop the thread from running
	 */
	public void stop() {
        running = false;
    }
	
	
	@Override
	public void run()
	{
		try
		{
			while (running)
			{
				if (this.outBuffer!="") {
					log.info("outBuffer content to send: "+outBuffer);
    				out.write(outBuffer);
    				//GHD: Clear buffer string after writing the related message to socket
    				out.flush();
    				outBuffer = "";
				}
				String inBuffer=receive();
				if (!(inBuffer==null))
				{
					try {
						log.info("inBuffer content received: "+inBuffer);
						networkObserver.messageReceived(peer, inBuffer); //add to inQueue that is used by Event Processor
					//this.connectionManager.enqueueMessage(inBuffer);
					}
					catch(Exception e) {
						log.severe("exception in message receive "+e.getMessage());
						e.printStackTrace();
					}
				}
				//GHD: Commented as this would be expected
				//else
				//	System.out.println("In buffer is null");
				
				Thread.sleep(Constants.BITBOX_THREAD_SLEEP_TIME);
			}
		}
		
		catch (Exception e)
		{
			//TODO should raise connection close event to the connection manager to remove this from the list
			networkObserver.connectionClosed(peer);
			//TODO close the socket and cleanup
			
			e.printStackTrace();
		}
	}
	
	/**
	 * Fills connection outgoing buffer with data to be sent to 
	 * @param message
	 */
	
	public boolean isHSRReceived()
	{
		boolean status = false;
		try
		{
			
			Document handshakeRequest = Document.parse(in.readLine());
			if (Protocol.validate(handshakeRequest))
			{
				status = true;
				Document host = (Document) handshakeRequest.get("hostPort");
				peer = new HostPort(host);
			}
				
			else
				status = false;
		}
		catch (SocketTimeoutException e)
		{
			log.warning("isHSRReceived timeout");
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
		
		return status;
	}
	
	//GHD: maybe sunchronized is required here
	public /*synchronized*/ void send(String message)
	{
		log.info(String.format("In connection.send, Peer: %s message: %s", peer.toString(),message));
		this.outBuffer=message;
	}
	
	public String receive()
	{
		String inBuffer = null;
		try
		{
			inBuffer = this.in.readLine();
		}
		catch(SocketTimeoutException ex)
		{
			//Do nothing, this is expected if no message arrived within the socket timeout set
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
		return inBuffer;
	}
	
	public void close()
	{
		try
		{
			if (this.clientSocket!=null)this.clientSocket.close();
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
	
	}

}
