//Rick McEwan
//Sean Fleming
//Xiao Liu

import java.io.*;
import java.net.*;
import java.nio.ByteBuffer;
import java.util.*;

public class Manager implements Runnable {
	
	static TorrentInfo torrent;
	private byte[] peer_ID;
	int uploaded;
	int downloaded;
	private HttpURLConnection con;
	String fileName;
	byte[] eventstatus;

	public final static byte[] started = new byte[]{'s', 't', 'a', 'r', 't', 'e', 'd'};
	public final static byte[] stopped = new byte[]{'s', 't', 'o', 'p', 'p', 'e', 'd'};
	public final static byte[] completed = new byte[]{'c', 'o', 'm', 'p', 'l', 'e', 't', 'e', 'd'};
	
	public Manager(String fileName, TorrentInfo torrent, byte[] peer_ID_input) {
		this.peer_ID = peer_ID_input;
		this.torrent = torrent;
		this.fileName = fileName;
		con = null;
		int uploaded = 0;
		int downloaded = 0;
		
		this.eventstatus = started;
	}

	public void run(){

			ArrayList<Peer> list_desired_peers = getPeersFromTracker(); 
			if (list_desired_peers == null ) {
				System.out.println("Error connecting to tracker");
				return;
			}
			
			File file  = new File(fileName);
			final String FILEPATH = fileName;
		try{
			if(!file.exists()){
			
				System.out.println("Making a new file for you.");
				PeerManager.ourimage = new RandomAccessFile(fileName, "rw");
				PeerManager.ourimage.setLength(torrent.file_length);
				byte[] zByte = new byte[]{0,0,0,0,};
			
				for(int i = 0; i < torrent.file_length; i += torrent.piece_length)
				{
					PeerManager.ourimage.seek(i);
					PeerManager.ourimage.write(zByte);
				}
			}
	     	
	     		else{
				PeerManager.ourimage = new RandomAccessFile(FILEPATH, "rw");
			     	PeerManager.ourimage.setLength(torrent.file_length);
			}
		     	
		     	//calculates # pieces, always rounding up
		     	int num_pieces = (int)Math.ceil((double)torrent.file_length/(double)torrent.piece_length);
		     	
		     	//determines # chunks, and if last piece is one chunk or two
		     	double leftovers = ((double)torrent.file_length-(Manager.torrent.piece_length*(num_pieces-1)));
		     	//if leftovers > chunk size
		     	if (leftovers > 16384) {
		     		PeerManager.num_chunks_needed = num_pieces*2;
		     	} else {
		     		PeerManager.num_chunks_needed = ((num_pieces-1)*2) + 1;
		     	}
		     	
		     	//start peermanager thread. peer manager creates/monitors threads for each peer
				PeerManager our_pm = new PeerManager(list_desired_peers, torrent);
				Thread pm = new Thread(our_pm);
				pm.start();
			} catch (Exception e) {
				e.printStackTrace();
			}
	}
	
	public ArrayList<Peer> getPeersFromTracker() {
		try {
			
			ArrayList<Peer> list_desired_peers = new ArrayList<Peer>();
			
			//adds all needed info and url encodes it
			String finalurl = makeTrackerURL();
			
			//create url based of urlstring
			URL url = new URL (finalurl);
			
			//open connection with url
			con = (HttpURLConnection)url.openConnection();
			con.setDoOutput(true);
			con.setRequestMethod("GET");
			
			//Send Request,then flush and close stream
			DataOutputStream outstream = new DataOutputStream(con.getOutputStream());
			outstream.flush();
			outstream.close();

			//Get Response From Tracker
		    InputStream instream = con.getInputStream();
		    BufferedReader reader = new BufferedReader(new InputStreamReader(instream));
		    String instreamline;
		    StringBuffer response = new StringBuffer(); 
	     	while((instreamline = reader.readLine()) != null) {
		        response.append(instreamline);
		        response.append('\r');
		    }
	     	reader.close();
	     	
	     	//convert response back into bytes
	     	byte[] bencoded_bytes = String.valueOf(response).getBytes();
	     	
	     	//create keys for dictionaries
			ByteBuffer key_id = ByteBuffer.wrap(new byte[] {'p','e','e','r',' ','i','d'});
			ByteBuffer key_interval = ByteBuffer.wrap(new byte[] {'i','n','t','e','r','v','a','l'});
			ByteBuffer key_ip = ByteBuffer.wrap(new byte[] {'i','p'});
			ByteBuffer key_peers = ByteBuffer.wrap(new byte[] {'p','e','e','r','s'});
			ByteBuffer key_port = ByteBuffer.wrap(new byte[] {'p', 'o','r','t'});
			
			ByteBuffer key_fr = ByteBuffer.wrap("failure reason".getBytes());
			
			


			//decode response bytes and store in HashMap
	     	HashMap<ByteBuffer, Object> values = (HashMap<ByteBuffer, Object>) Bencoder2.decode(bencoded_bytes);
	     	
	     	//if size ==1, failure, print reason, exit
	     	if (values.size() == 1 ) {
	     		String fr = ByteToString((ByteBuffer)values.get(key_fr));
	     		System.out.println(fr);
	     		return null;
	     	}

	     	
	     	//get interval from values
	     	Integer interval = (Integer) values.get(key_interval);
			System.out.println("Interval is : " + interval);

	     	//get ArrayList of dictionaries of lists of peers
			ArrayList list_of_peers = (ArrayList)values.get(key_peers);
			
			
			HashMap<ByteBuffer, Object> peer_info;

			System.out.println("number possible peers : " + list_of_peers.size());
			Peer desired_peer = null;
			//loop through list, checking each peer to see if peer_id matches desired peer_id
			for (int i=0; i<list_of_peers.size();i++) {
				peer_info = (HashMap<ByteBuffer, Object>)list_of_peers.get(i);			
				//get hashmap of peer info for peer i in list

				//extract port, id, and peer_ID from dictionary
				Integer port = (Integer) peer_info.get(key_port);
				String ip = ByteToString((ByteBuffer)peer_info.get(key_ip));
				String peer_ID = ByteToString((ByteBuffer)peer_info.get(key_id));

//			System.out.println(ip);
				
				//if peer has desired peer_ID, return peer
				if (peer_ID.substring(0, 7).equals("-RU1103")) {
					System.out.println("Found!");
					desired_peer = new Peer(port, ip, peer_ID.getBytes());
					desired_peer.torrent = this.torrent;
					list_desired_peers.add(desired_peer);
					
					//if multiple peers - addtolist(desired_peer); - lets us create list of peers in loop	
				} else {
					System.out.println("NOPE!");
				}
			}
			
			return list_desired_peers;

		} catch (Exception ex) {
			ex.printStackTrace();
			return null;
		} finally {
			if(con != null) {
				con.disconnect(); 
			}
		}
	}
		
	
	public String getMapValue(Map<ByteBuffer, Object> map, ByteBuffer key) {
		ByteBuffer byteresult = (ByteBuffer) map.get(key);
		if (byteresult == null)
			return "null";
		else
			return ByteToString(byteresult);
	}
	
	public String ByteToString(ByteBuffer bytes) {
		return new String (bytes.array());
	}
	
	  public static String encode(byte bytes[]) {
		    byte ch = 0x00;
		    if (bytes == null || bytes.length <= 0)
		      return null;

		    String pseudo[] = { "0", "1", "2", "3", "4", "5", "6", "7", "8", "9",
		        "A", "B", "C", "D", "E", "F" };
		    StringBuffer out = new StringBuffer(bytes.length * 2);

		    for (int i=0; i < bytes.length; i++) {
		      // see if need to do anything
		      if ((bytes[i] >= '0' && bytes[i] <= '9')
		          || (bytes[i] >= 'a' && bytes[i] <= 'z')
		          || (bytes[i] >= 'A' && bytes[i] <= 'Z') || bytes[i] == '$'
		          || bytes[i] == '-' || bytes[i] == '_' || bytes[i] == '.'
		          || bytes[i] == '!') {
		        out.append((char) bytes[i]);
		      } else {
		        out.append('%');
		        ch = (byte) (bytes[i] & 0xF0); // get most important bit
		        ch = (byte) (ch >>> 4); // shift the bits down
		        ch = (byte) (ch & 0x0F); // must do this is high order bit is on!
		        out.append(pseudo[(int) ch]); // convert bit to char
		        ch = (byte) (bytes[i] & 0x0F); // get bit
		        out.append(pseudo[(int) ch]); // convert bit to char
		      }
		    }

		    String rslt = new String(out);

		    return rslt;

		  }

	
	
	public String makeTrackerURL() {
		
		StringBuilder finalurl = new StringBuilder();
		
		//append announce(base) url, info_hash, peer id, port of tracker, and # of bytes uploaded, downloaded, and still needed
		finalurl.append(torrent.announce_url);		
		
		finalurl.append("?info_hash=");
		
	    byte[]  bytes = new byte[torrent.info_hash.remaining()];
	    torrent.info_hash.get(bytes, 0, bytes.length);

		finalurl.append(encode(bytes));
				
		finalurl.append("&peer_id=");
		finalurl.append(encode(peer_ID));
		
		finalurl.append("&port=6881");
		
		finalurl.append("&uploaded=");
		finalurl.append(Integer.toString(uploaded));
		
		finalurl.append("&downloaded=");
		finalurl.append(Integer.toString(downloaded));
		
		
		finalurl.append("&left=");
		finalurl.append(torrent.file_length);
	

		if(eventstatus != null){
			finalurl.append("&event=");
			for (byte b: eventstatus){
				finalurl.append("%");
			  	finalurl.append(String.format("%02X", b));
			}
		}
	
		return finalurl.toString();
	}

}
