import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.StringTokenizer;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;

/**
 * Clasa ce reprezinta o solutie partiala pentru problema de rezolvat. Aceste
 * solutii partiale constituie task-uri care sunt introduse in workpool.
 */

class PartialSolution {

	String workingString;
	ArrayList<Character> delims;
	int index; // index-ul documentului (folosit pentru a cauta in vectorul de HashMap)
	ArrayList<HashMap<String, Integer>> wordCount;
	String mapOrReduce;

	public PartialSolution(String mapOrReduce, String workingString, ArrayList<Character> delims,
			ArrayList<HashMap<String, Integer>> wordCount, int index) {

		this.mapOrReduce = mapOrReduce;
		this.workingString= workingString;
		this.delims = delims;
		this.wordCount = wordCount;
		this.index = index;

	}

	@Override
	public String toString() {

		return "Working string is: " + workingString + "\n";

	}
}

/**
 * Clasa ce reprezinta un thread worker.
 */
class Worker extends Thread {
	WorkPool wp;
	CyclicBarrier barrier;

	public Worker(WorkPool workpool, CyclicBarrier barrier) {
		this.wp = workpool;
		this.barrier = barrier;
	}

	/**
	 * Procesarea unei solutii partiale. Aceasta poate implica generarea unor
	 * noi solutii partiale care se adauga in workpool folosind putWork().
	 * Daca s-a ajuns la o solutie finala, aceasta va fi afisata.
	 */
	void processPartialSolution(PartialSolution ps) {

		if (ps.mapOrReduce == "map") {

			String workString = ps.workingString;
			ArrayList<Character> delims = ps.delims;
			String delimsSt = "";
			for (Character ch : delims) {

				delimsSt += ch;

			}

			System.out.println("index: " + ps.index);

			StringTokenizer stTok = new StringTokenizer(workString, delimsSt);
			String word;

			while (stTok.hasMoreTokens()) {

				word = stTok.nextToken().toLowerCase();
				HashMap<String, Integer> currentMap = ps.wordCount.get(ps.index);

				synchronized (currentMap) {

					if (currentMap.containsKey(word)) {
						int no_of_appearances = currentMap.get(word);
						currentMap.remove(word);
						currentMap.put(word, no_of_appearances + 1);
					}
					else {
						currentMap.put(word, 1);
					}
				}
			}
		}



	}

	@Override
	public void run() {
		System.out.println("Thread-ul worker " + this.getName() + " a pornit...");
		while (true) {
			PartialSolution ps = wp.getWork();
			if (ps == null) {
				break;
			}

			processPartialSolution(ps);
		}
		try {
			barrier.await();
		} catch (InterruptedException | BrokenBarrierException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		System.out.println("Thread-ul worker " + this.getName() + " s-a terminat...");
	}


}


class ReplicatedWorkers {

	public static void main(String args[]) throws IOException, InterruptedException, BrokenBarrierException {

		int NT = Integer.parseInt(args[0]); // numarul de threaduri
		BufferedReader in = new BufferedReader(new FileReader(args[1]));
		BufferedWriter out = new BufferedWriter(new FileWriter(args[2]));

		String docVerified; // documentul pentru care se afla similaritatea
		int D; // dimensiunea (numarul de octeti) citita in buffer din fisier
		float similarity_limit; // pragul de similaritate
		int ND; // numarul de documente pentru care se verifica similaritatea
		String[] docsChecked; // documentele cu care se verifica similaritatea

		ArrayList<HashMap<String, Integer>> wordCount =
				new ArrayList<HashMap<String, Integer>>();

		ArrayList<Character> delims = new ArrayList<Character>();
		delims.add('\r');
		delims.add('\n');
		delims.add('\t');
		delims.add('\f');
		delims.add('.');
		delims.add(' ');
		delims.add('!');

		WorkPool wp = new WorkPool(NT);

		String line;
		line = in.readLine();
		StringTokenizer tok = new StringTokenizer(line);
		docVerified = tok.nextToken();

		line = in.readLine();
		D = Integer.parseInt(line);
		int D_CHARS = D / 2; // char-uri citite la un moment dat

		line = in.readLine();
		Float.parseFloat(line);

		line = in.readLine();
		ND = Integer.parseInt(line);

		docsChecked = new String[ND];
		for (int i = 0; i < ND; i++) {

			line = in.readLine();
			tok = new StringTokenizer(line);
			docsChecked[i] = tok.nextToken();
			wordCount.add(new HashMap<String, Integer>());

		}

		char[] buffer;
		CyclicBarrier barrier = new CyclicBarrier(NT, new Runnable() {

			@Override
			public void run() {
				// TODO Auto-generated method stub
				System.out.println("All threads arrived at barrier.");

			}
		});
		int n_workers = NT;

		for (int i = 0; i < ND; i++) {

			BufferedReader currentDoc = new BufferedReader(new FileReader(docsChecked[i]));
			buffer = new char[D_CHARS];
			int offset = 0;

			while (currentDoc.read(buffer, offset, D_CHARS) != -1) {

				String workingString = new String(buffer);
				char[] nextCh = new char[1];

				if (!delims.contains(buffer[buffer.length - 1])) {

					int ret = currentDoc.read(nextCh, offset, 1);

					while (ret != -1 && !delims.contains(nextCh[0])) {
						workingString += nextCh[0];
						nextCh = new char[1];
						ret = currentDoc.read(nextCh, offset, 1);
					}
				}

				wp.putWork(new PartialSolution("map", workingString, delims, wordCount, i));
				if (n_workers > 0) {
					Worker w = new Worker(wp, barrier);
					w.start();
					n_workers--;
				}

			}

			currentDoc.close();

		}
		barrier.await();

		for (int i = 0; i < ND; i++) {

			if (docVerified.compareTo(docsChecked[i]) != 0) {



			}

		}


		in.close();
		out.close();

	}

}

