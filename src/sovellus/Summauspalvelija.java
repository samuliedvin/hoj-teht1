package sovellus;

import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;

public class Summauspalvelija extends Thread{

	private int port;
	private final String host = "127.0.0.1";
	private Socket cs;
	private int sum;
	int foo;
	private int amount = 0;
	private ServerSocket ss;
	
	
	public Summauspalvelija (int port) {
		this.port = port;
		try {
			System.out.println("Luodaan sokettia osoitteeseen " + host + ":" + port);
			ss = new ServerSocket(port);
			System.out.println("Soketti luotu osoitteeseen " + host + ":" + port);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	@Override	
	public void run() {
		try {
			cs = ss.accept();
			InputStream iS = cs.getInputStream();
			ObjectInputStream oIn = new ObjectInputStream(iS);
			
			try {
				while(true) {
					
					foo = oIn.readInt();
					System.out.println("Käytössä palvelin portissa " + port);
					System.out.println("Saatiin palvelimelta luku " + foo);
					if(foo != 0) {
						addSum(foo);
						amount++;
						
					} else {
						oIn.close();
						cs.close();
					}
					System.out.println("Summa on tällä hetkellä: " + sum);
					this.wait(NORM_PRIORITY);
				} // while
				
			} catch (Exception e) {
				oIn.close();
				cs.close();
			}
		} catch (Exception e) {
			
		}
	}
	
	public void addSum(int foo) {
		sum = sum + foo;
	}
	
	public int getSum() {
		return sum;
	}
	
	public int getAmount() {
		return amount;
	}
	
	
	public String getServerName() {
		return host + ":" + port;
	}
	
	@Override
	public String toString() {
		String result = "Summauspalvelija luotu porttiin: " + port;
		return result;
	}
	
	
	
}
