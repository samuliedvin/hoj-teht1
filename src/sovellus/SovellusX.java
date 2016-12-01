package sovellus;

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.HashMap;
import java.util.Map;

public class SovellusX {
	public static final int PORT = 3127;
	
	public static void main(String[] args) throws Exception {
		ServerSocket serversocket = new ServerSocket(PORT);
		serversocket.setSoTimeout(60000);
		
		try{
			
			sendUDP("localhost", 3126, "3127");
			
			while (true) {
				Socket clientsocket = serversocket.accept();
				System.out.println("Connection from: " + clientsocket.getInetAddress() + " port " + clientsocket.getPort());
				
				new Handler(clientsocket).start();
			} //while
		}
		catch (SocketTimeoutException ste) {
			    System.out.println("I timed out!");
			    serversocket.close();
			    }
	} //main
	
	public static void sendUDP(String target, int port, String message) throws Exception {
		InetAddress targetAddr = InetAddress.getByName(target);
		DatagramSocket socket = new DatagramSocket();
		byte[] data = message.getBytes();
		DatagramPacket packet = new DatagramPacket(data, data.length,
				targetAddr, port);
		socket.send(packet);
	} //sendUDP
	
	
	static class Handler extends Thread {
		private final Socket client;
		private Map<Integer, Summauspalvelija> palvelijat;
		
		public Handler(Socket s) {
			client = s;
		}
		
		@Override
		public void run() {
			try {
				System.out.println("Spawning thread...");
				InputStream iS = client.getInputStream();
				OutputStream oS = client.getOutputStream();
				oS.flush();
				ObjectOutputStream oOut = new ObjectOutputStream(oS);
				ObjectInputStream oIn = new ObjectInputStream(iS);
				
				try {
				
					int foo = oIn.readInt();
					System.out.println("Luodaan " + foo + " summauspalvelijaa.");
					if (1 < foo && foo < 11) {
						
						//Tallennetaan summauspalvelijat Hashmappiin, saadaan porttinumeron kanssa sitten ulos...
						palvelijat = new HashMap<Integer, Summauspalvelija>();
						
						System.out.println("Sending ports...");
						for(int i = 0; i < foo; i++) {
							int port = 5000 + i;
							palvelijat.put(port, new Summauspalvelija(port)); 	//Lisätään s-palvelijat
							oOut.writeInt(port); 								//Lähetetään palvelimelle portit
							oOut.flush();
							System.out.println(palvelijat.get(port).toString());
						}
						for(Map.Entry<Integer, Summauspalvelija> entry : palvelijat.entrySet()) {
						    entry.getValue().start();
						}
						
					} else {
						oOut.writeInt(-1);
						oOut.flush();
						oIn.close();
						oOut.close();
						client.close();
					}
					
					while (true) {
						foo = oIn.readInt();
						Thread.sleep(500);
						if (foo == 1) {
							System.err.println(foo + ") Palvelin haluaa tietaa valitettyjen lukujen kokonaissumman.");
							int sum = countSum();
							System.err.println("No sehan on " + sum);
							
							oOut.writeInt(sum);
							oOut.flush();
						} else if (foo == 2) {
							System.err.println(foo + ") Mille summauspalvelijalle valitettyjen lukujen summa on suurin");
							int result = whatsBiggest();
							System.err.println("No sehan on " + result);
							
							oOut.writeInt(result);
							oOut.flush();
							
						} else if (foo == 3) {
							System.err.println(foo + ") Mika on tahan mennessa kaikille summauspalvelimille valitettyjen lukujen kokonaismaara?");
							int amount = getTotalAmount();
							System.err.println("No sehan on " + amount);
							
							oOut.writeInt(amount);
							oOut.flush();
						} else if(foo == 0){
							
						}
						
					} // while
					
				} catch (IOException e) {
					oIn.close();
					oOut.close();
					client.close();
				} //try
				
			} catch (Exception e) {
				e.printStackTrace();
			} // try
		} // run
		
		public int countSum() {
			int sum = 0;
			for(Map.Entry<Integer, Summauspalvelija> entry : palvelijat.entrySet()) {
			    
			    sum = sum + entry.getValue().getSum();
			}
			return sum;
		}
		
		public int whatsBiggest() {
			int biggest = Integer.MIN_VALUE;
			int server = -1;
			
			for(Map.Entry<Integer, Summauspalvelija> entry : palvelijat.entrySet()) {
			    if (entry.getValue().getSum() > biggest) {
			    	biggest = entry.getValue().getSum();
			    	server = entry.getKey() - 4999;
			    }
			}
			
			return server;
		}
		
		public int getTotalAmount() {
			int result = 0;
			for(Map.Entry<Integer, Summauspalvelija> entry : palvelijat.entrySet()) {
			    result = result + entry.getValue().getAmount();
			}
			return result;
		}
		
	} // class Handler
	
}
