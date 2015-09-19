//Rick McEwan
//Sean Fleming
//Xiao Liu

import java.io.*;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.util.*;
import java.security.*; 	

public class Peer implements Runnable {
	
	//message ids for all messages **keepalive given byte 99 randomly**
	public final byte keepalivebyte = (byte)99;
	public final byte chokebyte = (byte)0;
	public final byte unchokebyte = (byte)1;
	public final byte interestedbyte = (byte)2;
	public final byte uninterestedbyte = (byte)3;
	public final byte havebyte = (byte)4;
	public final byte bitfieldbyte = (byte)5;
	public final byte requestbyte = (byte)6;
	public final byte piecebyte = (byte)7;
	public final byte cancelbyte = (byte)8;
	public final byte portbyte= (byte)9;
	
	int port;
	int numPieces = 0;
	double downloaded;
	double downloadSpeed[] = new double[38];
	double avg_down_rate;
	double time;
	double realTime;
	String ip;
	byte[] peer_ID;
	TorrentInfo torrent;
	Socket socket;
	DataOutputStream toPeer;
	DataInputStream input;
	ArrayList<Integer> hasPiece;
	ArrayList<byte[]> pChunk;
	
	boolean handshakeConfirmed;
	//peer choking us
	boolean	peerChokingClient;
	boolean peerInterested;
	boolean isHandshake;
	//client choking peer
	boolean clientChokingPeer;
	boolean clientInterested;
	boolean isConnect;
	boolean amConnected;

 	//initialize image, set length to our file length - will change to read from torrent info
	public void run() {
		try {
			openSocket();
	     	//test for me
			this.toPeer = new DataOutputStream(this.socket.getOutputStream());
			sendHandshake(RUBTClient.peer_ID);
			this.input = new DataInputStream(this.socket.getInputStream());
			if(!receiveCorrectHandshake()) {
				System.out.println("Handshake failed!");
				input.close();
				toPeer.close();
				socket.close();
			}
			
			//sends messages and gets messages until file is done
	     	while(PeerManager.num_pieces_acquired < PeerManager.num_chunks_needed) {
		     	sendMessage();

	//		time = Calendar.getInstance().getTimeInMillis();
		     	getMessage();

	     	}
			PeerManager.ourimage.close();
			input.close();
			toPeer.close();
			
			socket.close();
		}
		catch(Exception e) {
			
		}
	}
 	
 	
	public Peer() {
		port = 0;
		ip = "";
		peerChokingClient = true;
		peerInterested = false;
		clientChokingPeer = true;
		clientInterested = false;
		isConnect = false;
		handshakeConfirmed = false;
	}

	public Peer(int port, String ip, byte[] peer_ID){
		this.port = port;
		this.ip = ip;
		this.peer_ID = peer_ID;
		
		peerChokingClient = true;
		clientInterested = false;
		clientChokingPeer = true;
		peerInterested = false;
		isConnect = false;
		handshakeConfirmed = false;
//		downloadSpeed = 
		hasPiece = new ArrayList<Integer>();
		pChunk = new ArrayList<byte[]>();
//		downloadSpeed;
	}
	public boolean getKeepAlive() {
		//*****UNFINISHED*****
		//reset timer with peer - timer still needs to be added
		return true;
	}
	
	public boolean getChoke() {
		peerChokingClient = true;
		return true;
	}
	
	public synchronized void decreasePieceNum(int index) {
		PeerManager.rarest_piece_count[index]--;
		if (PeerManager.rarest_piece_count[index] < 0) {
			PeerManager.rarest_piece_count[index] = 0;
		}
	}
	
	public synchronized void increasePieceNum(int index) {
		PeerManager.rarest_piece_count[index]++;
	}
	
	public boolean getUnchoke() {
		peerChokingClient = false;
     	System.out.println("unchoked!");
		return true;
	}
	
	public boolean getInterested() {
		peerInterested = true;
		return true;
	}
	
	public boolean getUninterested() {
		peerInterested = false;
		return true;
	}
	
	public boolean getHave() {
		return true;
	}
	
	public boolean getBitfield(int sizeMsg) {
		try {
     	System.out.println("Test " + sizeMsg);
     	System.out.println(sizeMsg);
     		byte[] bitfield = new byte[sizeMsg-1];
     		input.readFully(bitfield);
     		
     		StringBuffer sb = new StringBuffer();
     		int pByte; 
     		int pBit;
     		byte valByte;
     		int numInt;

     		for(int bitIndex = 0; bitIndex < bitfield.length * 8 ; bitIndex++) {
     			pByte = bitIndex/8; 
     			pBit = bitIndex%8;
     			valByte = bitfield[pByte];
     			numInt = valByte>>(8-(pBit+1)) & 0x0001;
     			if(numInt == 1){
     				hasPiece.add(bitIndex);
     				increasePieceNum(bitIndex);
     			}
     		}
     		return true;

		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return false;
	}
	
	public boolean getRequest() {
		try {
			int index = input.readInt();
			int offset = input.readInt();
			int length = input.readInt();
			//sendpiece();
			return true;
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return false;
	}
	boolean isDone = false;
	public boolean getPiece(int lp) {

		try {
			int index = input.readInt();
			int offset = input.readInt();
			int pieceLen;
			byte[] piece = new byte[lp-9];
			input.readFully(piece);
			pChunk.add(piece);

			if(index == torrent.piece_hashes.length-1)
			{
				pieceLen = torrent.file_length % torrent.piece_length;
//				System.out.println(avgDownloadRate());
			}
			else {
				pieceLen = torrent.piece_length;
			}

			if(pChunk.size() * 16384 >= pieceLen){
				
				ByteBuffer bb;
				if (index != (torrent.piece_hashes.length - 1))
					bb = ByteBuffer.allocate(torrent.piece_length);
				else
					bb = ByteBuffer.allocate(torrent.file_length % torrent.piece_length);
				for (byte[] pm: pChunk)
				{
					bb.put(pm);
				}
				byte[] temp = bb.array();	
				boolean isDone = false;
				if(verifyPiece(temp, index)){
					PeerManager.ourimage.seek(index*torrent.piece_length);
					PeerManager.ourimage.write(temp);

//					realTime = Calendar.getInstance().getTimeInMillis() / 1000.00;
					PeerManager.isDownloadDone();
//					realTime = Calendar.getInstance().getTimeInMillis() / 1000.00;
					updateDownloaded(temp);
					System.out.println("Total left to download: " + PeerManager.bytesLeft());
				}
				
				pChunk = new ArrayList<byte[]>();
			}
			updateNumPieces();	     	
	     		return true;
		} catch (Exception e) {
			
		}
		return false;
	}
	
	public synchronized boolean updateNumPieces() {
		PeerManager.num_pieces_acquired++;
		return true;
	}
	
	public boolean getMessage() {
		try {
			int lp = input.readInt();
			//must be keep-alive message
			if (lp == 0) {
				return getKeepAlive();
			}
			//determines message based off message id
			int id;
			id = input.readByte();
			switch(id) {
				case 0 :
					return getChoke();
				case 1 :
					return getUnchoke();
				case 2 :
					return getInterested();
				case 3 :
					return getUninterested();
				case 4 :
					return getHave();
				case 5 :
					return getBitfield(lp);
				case 6 :
					return getRequest();
				case 7 :
					
//					time = Calendar.getInstance().getTimeInMillis() / 1000.00;
					return getPiece(lp);
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
		}
		
		return false;

	}
	
	//**UNFINISHED**
	public boolean hasDesiredPiece(){
		return true;
	}
	
	//**UNFINISHED**
	public int getRarestPiece() {
		Random r = new Random();
		int result = r.nextInt(torrent.piece_hashes.length);
		while (PeerManager.pieceCheck[result]==true) {	
			result = r.nextInt(torrent.piece_hashes.length);
		}
		updatePieceCheck(result, true);
		return result;
	}

	
	public boolean verifyPiece(byte[] piece, int index){
		ByteBuffer all_hashes = (ByteBuffer)torrent.info_map.get(torrent.KEY_PIECES);
	        byte[] all_hashes_array = all_hashes.array();
	        byte[] tBuff = new byte[20];
	        
		System.arraycopy(all_hashes_array,index*20,tBuff,0,20);
	        try {
        		MessageDigest digest = MessageDigest.getInstance("SHA-1");
 	                byte[] info_hash = digest.digest(piece);
        	        if (Arrays.equals(tBuff, info_hash)){
				System.out.println("Piece verified!");
                	        return true;
			}
            		else
            		{
                		return false;
            		}
        	} catch (NoSuchAlgorithmException e) {
            		e.printStackTrace();
        	}
        	
		return true;
	}
	
	//logically determines next message to send based on current conditions
	public boolean sendMessage() throws IOException {
			if (!handshakeConfirmed){
				sendHandshake(RUBTClient.peer_ID);
			}
			if (hasDesiredPiece()) {
				if (!clientInterested) {
					return sendInterested();
				}
			}
			else
				if (clientInterested) {
					return sendUninterested();
				}
			if (!peerChokingClient) {
				return sendBothRequests();
			}
			
			return sendKeepAlive();
		}
	
	public synchronized void updatePieceCheck(int index, boolean trueorfalse) {
		PeerManager.pieceCheck[index] = trueorfalse;
	}
	
	
	public boolean sendKeepAlive() throws IOException {
		ByteBuffer message_bb = ByteBuffer.allocate(4);
		message_bb.putInt(0);
		byte[] message = message_bb.array();
		toPeer.write(message);
		toPeer.flush();
		//*****UNFINISHED*****
		//needs to refresh our timer with peer - timer still needs to be added
		return true;
	}
	
	public boolean sendChoke() throws IOException{
		
		//4 byte length prefix, 1 byte message id
		ByteBuffer message_bb = ByteBuffer.allocate(5);
		
		//length prefix 1, only message id (1 byte) left
		message_bb.putInt(1);
		
		//choke message id is 0
		message_bb.put(chokebyte);
		
		byte[] message = message_bb.array();
		toPeer.write(message);
		toPeer.flush();
		clientChokingPeer = true;
		return true;
	}
	
	public boolean sendUnchoke() throws IOException {
		
		//4 byte length prefix, 1 byte message id
		ByteBuffer message_bb = ByteBuffer.allocate(5);
		
		//length prefix 1, only message id (1 byte) left
		message_bb.putInt(1);
		
		message_bb.put(unchokebyte);
		
		byte[] message = message_bb.array();
		toPeer.write(message);
		toPeer.flush();
		clientChokingPeer = false;
		return true;
	}
	
	public boolean sendInterested() throws IOException {
		
		//4 byte length prefix, 1 byte message id
		ByteBuffer byteBuf = ByteBuffer.allocate(5);
		byteBuf.putInt(1);
		
		//interested message id is 2
		byteBuf.put(interestedbyte);
		
		byte[] toSend = byteBuf.array();
		toPeer.write(toSend);
		toPeer.flush();
     	clientInterested = true;
		return true;
	}
	
	public boolean sendUninterested() throws IOException {
		//4 byte length prefix, 1 byte message id
		ByteBuffer byteBuf = ByteBuffer.allocate(5);
		byteBuf.putInt(1);
		
		//uninterested message id is 2
		byteBuf.put(uninterestedbyte);
		
		byte[] toSend = byteBuf.array();
		toPeer.write(toSend);
		toPeer.flush();
     	clientInterested = false;
		return true;
	}
	
	public boolean sendHave() {
		return true;
	}
	
	public boolean sendBitfield() {
		return true;
	}
	
	public boolean sendBothRequests() throws IOException {
		int index = getRarestPiece();
		int chunk_size;
		System.out.println("index is " + index);
		//replace 38 with variable - calculate length by file size
		if (index!= torrent.piece_hashes.length-1) {
			chunk_size = 16384;
			
			//request piece index chunk 1
			sendRequest(index, 0, chunk_size);
	     	getMessage();
			   
	     	//request piece index chunk 2
	     	sendRequest(index, chunk_size, chunk_size);
	     	//maybe take this out? and chunk two is received by while loop? depends on thread design
	     	return true;
		} else if (index==torrent.piece_hashes.length-1){
			chunk_size = torrent.file_length % 16384;
			sendRequest(index, 0, chunk_size);
	     	//maybe take this out? and this chunk is received by while loop? depends on thread design
			return true;
		}
		else
			return false;
	}
	
	public boolean sendRequest(int piece_index, int offset, int piece_length) throws IOException {
		ByteBuffer byteBuf = ByteBuffer.allocate(17);
		byteBuf.putInt(13);
		byteBuf.put(requestbyte);
		byteBuf.putInt(piece_index);
		byteBuf.putInt(offset);
		byteBuf.putInt(piece_length);
		byte[] toSend = byteBuf.array();
		toPeer.write(toSend);
		toPeer.flush();
		return true;
	}
		
	public boolean sendPiece() {
		return true;
	}
		
	public boolean openSocket(){
		try {
			socket = new Socket(ip, port);
			socket.setSoTimeout(100000);
			return true;
		} catch (Exception e) {
			e.printStackTrace();
			return false;
		}
	}

	public boolean sendHandshake(byte[] peer_id) {
		try {
			//send handshake to server
			this.toPeer.write(makeHandshake(peer_id), 0, 68);
			this.toPeer.flush();
			return true;
		}
		catch(Exception e) {
			System.out.println(e.getLocalizedMessage());
			return false;
		}
	}
	
	public boolean receiveCorrectHandshake() {
		try {
			byte[] handshake = new byte[68];	
			input.readFully(handshake, 0, 68);
			return verifyHandshake(handshake);
		}
		catch(Exception e) {
			System.out.println(e.getStackTrace());
			return false;
		}
	}
	
	//verifies this peer returned a proper handshake
	private boolean verifyHandshake(byte[] peer_hs) {
     	if (peer_hs[0] != (byte) 19)
     		return false;
		String protocol = "BitTorrent protocol";
		int i = 1;
		for (; i < 20; i++) {
			if (peer_hs[i] != (byte) protocol.charAt(i-1)){
				return false;
			}
		}
		for (; i < 28; i++) {
			if(peer_hs[i] != 0)
				return false;
		}
		for (Byte bt : torrent.info_hash.array()){
			if (peer_hs[i] != bt) 
				return false;
			i += 1;
		}
		for (Byte bt : "-RU1103".getBytes()){
			if (peer_hs[i] != bt) {
				return false;
			}
			i += 1;
		}
		System.out.println("Handshake verified!");
		handshakeConfirmed=true;
		return true;
	}
	
	
	//generates our handshake to peer
	private byte[] makeHandshake(byte[] ourpeerid) {
		byte[] hs = new byte[68];
		hs[0] = (byte) 19;
		String p = "BitTorrent protocol";
		int i = 1;
		for (; i < 20; i++)
			hs[i] = (byte) p.charAt(i-1);
		for (; i < 28; i++)
			hs[i] = (byte)0;
		for (Byte bt : torrent.info_hash.array()){
			hs[i] = bt;
			i += 1;
		}
		for (Byte bt : ourpeerid){
			hs[i] = bt;
			i += 1;
		}
		return hs;
	}
	
	public void disconnect(){
		try {
			socket.close();
			amConnected = false;
		} catch (IOException e) {
		} catch (NullPointerException npex) {
		}
	}
	
	public boolean verifyBitfield() {
		return false;
	}


	public void updateDownloaded(byte[] temp)
	{
		double thisPacket = 0;
//		realTime = Calendar.getInstance().getTimeInMillis() / 1000.00;
//		time = time /1000.00;
		double totalTime = realTime - time;
		numPieces++;
		for(byte b : temp)
			thisPacket++;
	
		downloaded += thisPacket;

//		downloadSpeed[numPieces-1] = thisPacket / totalTime;

		System.out.println("Downloaded bytes from Peer " + peer_ID + ": " + downloaded);
//		System.out.println("Download speed: " + downloadSpeed[numPieces-1]);

	}

/*	public double getDownloadSpeed()
	{
		return downloadSpeed;
	}*/
/*
	public double avgDownloadRate()
	{
		double total = 0;
		double i;
		for( i = 0; i < downloadSpeed.length; i++){
			total += downloadSpeed[(int)i];
		}

		this.avg_down_rate = (total / downloadSpeed.length);	

		return this.avg_down_rate;

	}*/	



}

