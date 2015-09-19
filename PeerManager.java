//Rick McEwan
//Sean Fleming
//Xiao Liu

import java.io.*;
import java.util.ArrayList;

public class PeerManager implements Runnable {

	//will manage timers and throughput? for each peer? - possible with dynamic # of peers?
	//array_list of timers - add to, and have loop with limit arraylist_timers.size() as end condition?
	//throughput - track number of bytes sent (upload) and received(download) per second
	
 	public static RandomAccessFile ourimage;
 	public static boolean[] pieceCheck;
 	public static int num_pieces_acquired;
 	public static int[] rarest_piece_count;
 	public static int num_chunks_needed;
 	public static ArrayList<Peer> list_possible_peers;
	public static TorrentInfo torrent;
	
	public static byte[] event;
        public final static byte[] started = new byte[]{'s', 't', 'a', 'r', 't', 'e', 'd'};
        public final static byte[] stopped = new byte[]{'s', 't', 'o', 'p', 'p', 'e', 'd'};
	public final static byte[] completed = new byte[]{'c', 'o', 'm', 'p', 'l', 'e', 't', 'e', 'd'};

 	public PeerManager(ArrayList<Peer> pl, TorrentInfo tdata) {
 		list_possible_peers = pl;
		torrent = tdata;
		pieceCheck = new boolean[torrent.piece_hashes.length];
		rarest_piece_count = new int[torrent.piece_hashes.length];
		event = started;
 	}

	public void run()  {
		int i=0;

     	for (Peer peer : list_possible_peers) {
     		Thread peer_thread = new Thread(peer);
     		peer_thread.setName(Integer.toString(i++));
     		peer_thread.start();
     	}
		
	}


/*	public boolean setEvent(byte[] eventStatus){
		if(eventStatus == stopped || eventStatus == completed || eventStatus == started){
			event = eventStatus;
			return true;
		}
		else 
			return false;
	}*/

	public static boolean isDownloadDone(){
        	for(int i=0; i < pieceCheck.length; i++){
        		if(pieceCheck[i] == false){
                		return false;
            		}
        	}
		System.out.println("Download complete");
//        	Manager.eventstatus = completed;
        	return true;
    	}


	public static int bytesLeft(){
		int have = 0;
		for(int i=0 ; i < pieceCheck.length - 1; i++){
			if(pieceCheck[i]){
				have += torrent.piece_length;
			}
		}
		if(pieceCheck[pieceCheck.length-1]){
			have += torrent.file_length % torrent.piece_length;
		}
		return torrent.file_length - have;
	}

}
