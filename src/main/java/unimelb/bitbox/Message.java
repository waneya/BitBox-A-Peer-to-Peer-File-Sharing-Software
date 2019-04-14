package unimelb.bitbox;

import java.io.IOException;
import java.io.ObjectInputStream.GetField;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.codec.binary.Base64;

import unimelb.bitbox.Err.InvalidCommandException;
import unimelb.bitbox.util.Constants.Command;
import unimelb.bitbox.util.Document;
import unimelb.bitbox.util.HostPort;

public class Message {
	
	private Command command;
	private String content;
	private String message;
	private byte[] binaryData;
	private String pathName;
	private boolean isSuccessStatus;
	private String md5;
	private String lastModified;

	private int fileSize;
	private int position;
	private int length;
	private List<HostPort> peersList = new ArrayList<>();
	private HostPort hostAddress;

	private static Logger log = Logger.getLogger(Message.class.getName());
	
	public static void main( String[] args ) throws IOException, NumberFormatException, NoSuchAlgorithmException
    {
        //Dummy testing
    	System.setProperty("java.util.logging.SimpleFormatter.format",
                "[%1$tc] %2$s %4$s: %5$s%n");
    	
    	String str="{\"command\":\"INVALID_PROTOCOL\",\"pathName\":\"dir/subdir/etc\"\"peers\": [{\"host\" : \"sunrise.cis.unimelb.edu.au\",\"port\" : 8111},{\"host\" : \"bigdata.cis.unimelb.edu.au\",\"port\" : 8500}]}";
    	
		log.info("message "+str);
    	
    	Message msg;
    	
		try {
			msg = new Message(str);
			log.info("test command " + msg.command);
			log.info("test "+msg.pathName);

			log.info(msg.getJsonMessage(Command.INVALID_PROTOCOL));
		} catch (Exception e) {
			// TODO Auto-generated catch block
			log.log(Level.SEVERE, e.toString());
		}
    }
	
	public Message(String jsonMessage) throws Exception {
		//parse jsonMessage
		this(Document.parse(jsonMessage));
	}
	
	public Message(Document doc) throws Exception {
		//Validate message
		String cmd = doc.getString("command");
		if (cmd == null || cmd.isEmpty())
				throw new InvalidCommandException("command cannot be empty.");
		
		command = Command.fromString(cmd);
		if (command == null)
			throw new InvalidCommandException("Invalid command.");
		
		parseMessage(command,doc);
	}
	
	private void parseMessage(Command cmd,Document doc) {
		//Read relivant content based on the command
		content = doc.getString("content");
		
		//Read and validate tags depending on the incoming command
		switch(cmd) {
		case DIRECTORY_CREATE_REQUEST:
			//TO DO
		case DIRECTORY_CREATE_RESPONSE:
			
		default:
				
		}
		
		message = doc.getString("message");
		if((content!=null) && !content.isEmpty())
			binaryData = Base64.decodeBase64(content);
		
		pathName = doc.getString("pathName");
		
		isSuccessStatus = doc.containsKey("isSuccessStatus") ? doc.getBoolean("isSuccessStatus") : false;
		position = doc.containsKey("position") ? doc.getInteger("position") : 0;
		length = doc.containsKey("length") ? doc.getInteger("length") : 0;
		
		if(doc.containsKey("fileDescriptor"))
		{
			Document fileDescriptor = (Document) doc.get("fileDescriptor");
			md5 = fileDescriptor.getString("md5");
			fileSize = fileDescriptor.getInteger("fileSize");
			lastModified = fileDescriptor.getString("lastModified");
		}
		
		if(doc.containsKey("hostPort")) {
			Document hostDoc = (Document) doc.get("hostPort");
			hostAddress = new HostPort(hostDoc);
		}
		
		if(doc.containsKey("peers")) {
			ArrayList<Document> peerDoc = (ArrayList<Document>) doc.get("peers");
			for (Document pd:peerDoc) {
				peersList.add(new  HostPort(pd));
			}
		}
	}
	
	public String getJsonMessage(Command cmd) {

        Document doc = new Document();
        doc.append("command", cmd.name());
        
        switch (cmd) {
	        case INVALID_PROTOCOL:
	        	doc.append("message", message);
	        	break;
	        case CONNECTION_REFUSED:
	        	doc.append("message", message);
	            ArrayList<Document> peerList = new ArrayList<Document>();
	            for (HostPort p:peersList) {
	                Document pd = new  Document();
		            pd.append("host", p.host);
		            pd.append("port", p.port);
		            peerList.add(pd);
	            }
	            doc.append("peers", peerList);
	            break;
	        case FILE_BYTES_REQUEST:
	            Document docFD = new Document();
	            docFD.append("md5",md5);
	            docFD.append("lastModified",lastModified);
	            docFD.append("fileSize",fileSize);
	            
	            doc.append("fileDescriptor", docFD);
	            break;
		default:
			//throw error
			break;
        }
        
		return doc.toJson();
	}
	
	public Command getCommand() {
		return command;
	}

	public void setCommand(Command command) {
		this.command = command;
	}

	public String getMessage() {
		return message;
	}

	public void setMessage(String message) {
		this.message = message;
	}

	public String getPathName() {
		return pathName;
	}

	public void setPathName(String pathName) {
		this.pathName = pathName;
	}

	public boolean isSuccessStatus() {
		return isSuccessStatus;
	}

	public void setSuccessStatus(boolean isSuccessStatus) {
		this.isSuccessStatus = isSuccessStatus;
	}

	public String getMd5() {
		return md5;
	}

	public void setMd5(String md5) {
		this.md5 = md5;
	}

	public String getLastModified() {
		return lastModified;
	}

	public void setLastModified(String lastModified) {
		this.lastModified = lastModified;
	}

	public int getFileSize() {
		return fileSize;
	}

	public void setFileSize(int fileSize) {
		this.fileSize = fileSize;
	}

	public int getPosition() {
		return position;
	}

	public void setPosition(int position) {
		this.position = position;
	}

	public int getLength() {
		return length;
	}

	public void setLength(int length) {
		this.length = length;
	}

	public List<HostPort> getPeersList() {
		return peersList;
	}

	public void setPeersList(List<HostPort> peersList) {
		this.peersList = peersList;
	}

	public String getContent() {
		return content;
	}
	
	public void setContent(byte[] binaryData) {
		content = Base64.encodeBase64String(binaryData);
		this.setBinaryData(binaryData);
	}

	public void setContent(String content) {
		this.content = content;
	}

	public byte[] getBinaryData() {
		return binaryData;
	}

	public void setBinaryData(byte[] binaryData) {
		this.binaryData = binaryData;
	}
}
