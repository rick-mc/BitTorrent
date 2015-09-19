//Rick McEwan
//Sean Fleming
//Xiao Liu

import java.io.*;
import java.util.*;

public class RUBTClient{
	private TorrentInfo tdata;
	public static byte[] peer_ID;
	private String tPath;
	private String fileName;

	public final byte choke = (byte)0;
	public final byte unchoke = (byte)1;
	public final byte interested = (byte)2;
	public final byte uninterested = (byte)3;
	public final byte have = (byte)4;
	public final byte bitfield = (byte)5;
	public final byte request = (byte)6;
	public final byte piece = (byte)7;
	public final byte cancel = (byte)8;
	public final byte port = (byte)9;
//	public static  byte keep_alive = (byte)10;

	public static void main(String[] args) throws Exception{
		if(args.length != 2){
			System.err.println("You need two file names");
			return;
		}
		RUBTClient rubtclient = new RUBTClient(args[0], args[1]);
		rubtclient.go();
	}
	
	public RUBTClient(String tPath1, String fileName1){
		tPath = tPath1;
		fileName = fileName1;
	}

	private void go(){
		ourPID();
		if(!writeTAttributes(tPath))
			return;
		
		try{
			Manager manager = new Manager(fileName, tdata, peer_ID);
			Thread managerThread = new Thread(manager);
			Runtime.getRuntime().addShutdownHook(managerThread);
			managerThread.start();
			} catch (Exception e) {
			e.printStackTrace();
		}
		return;
	}

	private void ourPID(){
		Random r = new Random(System.currentTimeMillis());
		byte[] peerId = new byte[20];  
		for(int i = 0; i < 20; ++i)
			peerId[i] = (byte)('A' + r.nextInt(26));
		String peer_ID = new String(peerId);

		if (peer_ID.substring(0,4).equals("RUBT") || peer_ID.substring(0,6).equals("RU1103")){
			ourPID();
		}
		else {
			RUBTClient.peer_ID = peerId;
		}
	}
	
	public String generatePID() {
		return "";
	}

	private boolean writeTAttributes(String filename){
		File file = new File(filename);
		FileInputStream finstr;
		try {
			finstr = new FileInputStream(file);
		} catch (FileNotFoundException e) {
			System.err.println("Can't find that");
			e.printStackTrace();
			return false;
		}

		byte[] dataFromTorrentFile = new byte[(int)file.length()];

		try {
			new DataInputStream(finstr).readFully(dataFromTorrentFile);
			finstr.close();
		} catch (IOException e) {
			System.err.println("File unreadable.");
			e.printStackTrace();
			return false;
		}

		try {
			tdata = new TorrentInfo(dataFromTorrentFile);
		} catch (BencodingException e) {
			System.err.println(e.toString());
			e.printStackTrace();
			return false;
		}
		return true;
	} 

	public byte[] getPID() {
		return peer_ID;
	}

	public String getFName() {
		return fileName;
	}

	
}
