package palvelin;

import java.io.IOException;
import java.io.InputStream;
import java.io.InterruptedIOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.Socket;

public class WorkDistributor {

	public static final int PORT = 3126;
	public static boolean verboseMode = true;

	public static void main(String[] args) throws Exception {
		if (args.length == 1 && args[0].equals("verbose")) {
			verboseMode = true;
		}
		DatagramSocket ds = new DatagramSocket(PORT);
		ds.setSoTimeout(500000); // 500 sekuntia
		try {
			while (true) {
				byte[] byteArea = new byte[256];
				DatagramPacket receivedPacket = new DatagramPacket(byteArea,
						byteArea.length);
				ds.receive(receivedPacket);
				if (verboseMode) {
					System.out.println("Connection from "
							+ receivedPacket.getAddress() + " port "
							+ receivedPacket.getPort());
				}
				// what was received?
				String message = new String(receivedPacket.getData(), 0,
						receivedPacket.getLength());
				int contactPort = -1;
				try {
					contactPort = Integer.parseInt(message.trim());
				} catch (NumberFormatException e) {
					System.err
							.println("UDP error, message should represent number, message = "
									+ message);
				}
				if (contactPort < 1024 || contactPort > 65535) {
					if (verboseMode) {
						System.out.println("Errorneous suggestion for port '"
								+ message + "'");
						System.out.println("Contact attempt from "
								+ ds.getInetAddress() + " ignored.");
					}
					continue; // jump over the rest
				}
				new WorkDistributor.WorkDistributionHandler(
						receivedPacket.getAddress(), contactPort).start();
			} // while
		} catch (InterruptedIOException e) {
		}
	} // main

	static class WorkDistributionHandler extends Thread {
		public static final int MAXCLIENTS = 10;
		private final int clientPort;
		private final InetAddress clientAddress;
		private final int[] portNumbers = new int[MAXCLIENTS];
		private final Socket[] calculators = new Socket[MAXCLIENTS];
		private final ObjectOutputStream[] numberStreams = new ObjectOutputStream[MAXCLIENTS];

		public WorkDistributionHandler(InetAddress a, int p) {
			clientPort = p;
			clientAddress = a;
		}

		@Override
		public void run() {
			try {
				if (verboseMode) {
					System.out.println("Spawning thread ...");
				}
				Thread.sleep(2000); // let the other side set itself up ...
				Socket s = new Socket(clientAddress, clientPort);
				s.setSoTimeout(3000);
				InputStream iS = s.getInputStream();
				OutputStream oS = s.getOutputStream();
				ObjectOutputStream oOut = new ObjectOutputStream(oS);
				ObjectInputStream oIn = new ObjectInputStream(iS);
				int clients = (int) (Math.random() * 9) + 2;
				if (verboseMode) {
					System.out.println("Writing " + clients + " to "
							+ clientAddress + " at port " + clientPort);
				}
				oOut.writeInt(clients);
				oOut.flush();
				boolean aborting = receivePortNumbers(oIn, clients);
				if (aborting) {
					if (verboseMode) {
						System.out.println("Closing connection to "
								+ clientAddress + " at port " + clientPort);
					}
				} else {
					// try to make the socket connection
					for (int i = 0; i < clients; i++) {
						if (verboseMode) {
							System.out.println("Trying to connect to "
									+ portNumbers[i]);
						}
						calculators[i] = new Socket(clientAddress,
								portNumbers[i]);
						numberStreams[i] = new ObjectOutputStream(
								calculators[i].getOutputStream());
						if (verboseMode) {
							System.out.println("Connection to " + i
									+ "'th adder created.");
						}
						sleep(100);
					}
					generateTraffic(numberStreams, clients, oOut, oIn);
					for (int i = 0; i < clients; i++) {
						numberStreams[i].close();
						calculators[i].close();
					}
					if (verboseMode) {
						System.out
								.println("Connections to calculators closing ...");
					}
				}
				oOut.writeInt(0);
				oOut.flush(); // ask the other side to close itself
				oOut.close();
				oIn.close();
				oS.close();
				iS.close();
				s.close();
			} catch (Exception e) {
				throw new Error(e.toString());
			}
			if (verboseMode) {
				System.out.println("... thread done.");
			}
		} // run

		private boolean makeTest(int question, int answer,
				ObjectOutputStream masterOut, ObjectInputStream masterIn)
				throws IOException {
			masterOut.writeInt(question);
			masterOut.flush();
			int answerRead = masterIn.readInt();
			if (answerRead == -1) {
				System.err.println("Client answered with -1 to question "
						+ question + " ... aborting.");
				// return true;
			}
			if (answer != answerRead) {
				System.err.println("Error in client: wrong answer to query ("
						+ question + "). Expecting " + answer + " got "
						+ answerRead + ".");
				return true;
			}
			return false;
		}

		private void generateTraffic(ObjectOutputStream[] streams, int calcs,
				ObjectOutputStream masterOut, ObjectInputStream masterIn) {
			int table[] = new int[calcs];
			int sum = 0;
			int lkm = 0;
			int answer = 0;
			for (int i = 0; i < calcs; i++) {
				table[i] = 0;
			}
			int biggest = (int) (Math.random() * calcs);
			try {
				streams[biggest].writeInt(2);
				streams[biggest].flush();
				sum = 2;
				lkm = 1;
				table[biggest] = 2;
				// test 1
				if (verboseMode) {
					System.out.println("Making test 1 in set 1");
				}
				makeTest(1, sum, masterOut, masterIn);
				// test 2
				if (verboseMode) {
					System.out.println("Making test 2 in set 1");
				}
				makeTest(2, biggest + 1, masterOut, masterIn);
				// test 3
				if (verboseMode) {
					System.out.println("Making test 3 in set 1");
				}
				makeTest(3, lkm, masterOut, masterIn);
				for (int i = 0; i < 9; i++) {
					for (int j = 0; j < calcs; j++) {
						int number = (int) (Math.random() * 40) - 20;
						if (number == 0) {
							number++;
						}
						streams[j].writeInt(number);
						streams[j].flush();
						table[j] += number;
						lkm++;
						sum += number;
					}
				}
				for (int j = 0; j < calcs; j++) {
					streams[j].flush();
				}
				biggest = 0;
				for (int i = 1; i < calcs; i++) {
					if (table[i] > table[biggest]) {
						biggest = i;
					}
				}
				// test 4
				if (verboseMode) {
					System.out.println("Making test 1 in set 2");
				}
				makeTest(1, sum, masterOut, masterIn);
				// test 5
				if (verboseMode) {
					System.out.println("Making test 2 in set 2");
				}
				makeTest(2, biggest + 1, masterOut, masterIn);
				System.out.print("Table: ");
				for (int i = 0; i < calcs; i++) {
					System.out.print(" " + table[i]);
				}
				System.out.println("");
				for (int i = 0; i < calcs; i++) {
					if ((table[biggest] == table[i]) && (i != biggest)) {
						System.out
								.println("Tie with expected value " + (i + 1));
					}
				}
				// test 6
				if (verboseMode) {
					System.out.println("Making test 3 in set 2");
				}
				makeTest(3, lkm, masterOut, masterIn);
				streams[0].writeInt(0);
				streams[0].flush();
				if (calcs != 1) {
					streams[calcs - 1].writeInt(0);
					streams[calcs - 1].flush();
				}
				// test 7
				if (verboseMode) {
					System.out.println("Making test 1 in set 3");
				}
				makeTest(1, sum, masterOut, masterIn);
				// test 8
				if (verboseMode) {
					System.out.println("Making test 2 in set 3");
				}
				makeTest(2, biggest + 1, masterOut, masterIn);
				// test 9
				if (verboseMode) {
					System.out.println("Making test 3 in set 3");
				}
				makeTest(3, lkm, masterOut, masterIn);
			} catch (IOException e) {
				System.err
						.println("Received exception while testing ... aborting.");
				System.err.println("Exception: " + e);
				return;
			}
		} // generateTraffic

		public boolean receivePortNumbers(ObjectInputStream oIn, int clients) {
			boolean aborting = false;
			if (verboseMode) {
				System.out.println("Receiving port numbers.");
			}
			for (int i = 0; i < clients; i++) {
				int p;
				if (verboseMode) {
					System.out.println("Trying to receive " + i
							+ "th port number.");
				}
				try {
					p = oIn.readInt();
				} catch (IOException e) {
					System.out.println(e);
					if (verboseMode) {
						System.out
								.println("Error in reading portnumbers ... aborting.");
					}
					aborting = true;
					break;
				}
				if (i == 0 && p == -1) {
					// abort, the client didn't receive previous message in time
					if (verboseMode) {
						System.out
								.println("Received -1 from client ... aborting.");
					}
					aborting = true;
					break;
				}
				if (p < 1024 || p > 65535) {
					// illegal port number
					if (verboseMode) {
						System.out.println("Illegal port " + p
								+ " from client ... aborting.");
					}
					aborting = true;
					break;
				}
				portNumbers[i] = p;
				if (verboseMode) {
					System.out.println("Received " + i + "'th port number.");
				}
			} // for
			return aborting;
		} // receivePortNumbers

	} // class WorkDistributionHandler

} // class WorkDistributor