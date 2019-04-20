package unimelb.bitbox;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.security.NoSuchAlgorithmException;
import java.util.logging.Logger;
import java.net.InetAddress;
import unimelb.bitbox.util.Configuration;
import unimelb.bitbox.util.FileSystemManager;
import unimelb.bitbox.util.Constants.PeerSource;
import unimelb.bitbox.util.*;


public class ServerMain implements Runnable {
	private static Logger log = Logger.getLogger(ServerMain.class.getName());
	protected FileSystemManager fileSystemManager;
	public ConnectionManager connectionManager;
    int connectionCount = 0;
    InetAddress serverAddress;
    ServerSocket serverSocket=null;
    
    //What is the point of having advertised name if we can't set it??
    private String serverName;
	private int serverPort;
	HostPort serverHostPort;

	
	public ServerMain(ConnectionManager connectionManager) throws NumberFormatException, IOException, NoSuchAlgorithmException {
		
		this.serverName = Configuration.getConfigurationValue("advertisedName");
		this.serverPort = Integer.parseInt(Configuration.getConfigurationValue("port"));
		this.connectionManager = connectionManager;
	}
  



	@Override
	public void run()
	{
		Socket clientSocket;
		try
		{
			this.serverSocket = new ServerSocket(this.serverPort);
			log.info("Peer Server started, listening at "+serverPort);
			while (true)
			{
				clientSocket = this.serverSocket.accept();
				//It should not just add the connection... 
				//it should check if max incoming connection is reached
				//then send either INVALID_PROTOCOL,CONNECTION_REFUSED or HANDSHAKE_RESPONSE
				//only after successful handshake it should add to the connection
				this.connectionManager.addConnection(clientSocket, PeerSource.SERVER);
				log.info(String.format("Connected to: %s, total number of established connections: %s\n",
						clientSocket.getInetAddress().getHostName(),
						this.connectionManager.connectedPeers.size()
						));
			}
		}
		catch (IOException ex) 
		{
			log.severe(ex.getMessage());
		}
	
		catch (Exception e)
		{
			e.printStackTrace();
		}
		
		finally
		{
			try
			{
				if (this.serverSocket!=null) this.serverSocket.close();
			}
			catch (Exception e)
			{
				System.out.println(e.getMessage());
			}
		}
	}
}
