package ricercaOperativa.cotroller;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import javax.swing.JFileChooser;
import javax.swing.JFrame;

import ricercaOperativa.Views.RicercaOperativaPanel;
import ricercaOperativa.model.Mezzo;
import ricercaOperativa.model.Nodo;
import ricercaOperativa.model.Tappa;

public class RicercaOperativaController {

	private String filePath = "/Users/valer/Desktop/prova.txt"; // "/Users/NicoMac/Desktop/prova.txt"
	private double fuelTankCapacity;
	private double vehicleLoadCapacity;
	private double fuelConsumptionRate;
	private double inverseRefuelingRate;
	private double averageVelocity;
	private ArrayList<Mezzo> soluzioneMezzi;
	private RicercaOperativaPanel view;
	HashMap<Nodo, Double> distanzeClienti = new HashMap<Nodo, Double>();
	Tappa ultimaTappa;
	double funzioneObbiettivo = 0.0;
	double funzioneObbiettivoIniziale = 0.0;
	double percMiglioramento = 0.0;
	long elapsedTime = 0;

	public RicercaOperativaController(final RicercaOperativaPanel view, JFrame frame) {
		this.view = view;
		int count = 0;
		ArrayList<Nodo> depositi = new ArrayList<Nodo>();
		ArrayList<Nodo> clienti = new ArrayList<Nodo>();
		ArrayList<Nodo> distributori = new ArrayList<Nodo>();
		JFileChooser fileChooser = new JFileChooser();
		fileChooser.showOpenDialog(frame);
		File file = fileChooser.getSelectedFile();
		if (file != null) {
			try {
				// create a Buffered Reader object instance with a FileReader
				BufferedReader br = new BufferedReader(new FileReader(file));

				// read the first line from the text file
				String fileRead = br.readLine();
				fileRead = br.readLine();

				// loop until all lines are read
				while (fileRead != null) {
					if (!fileRead.isEmpty()) {
						// use string.split to load a string array with the values from each line of
						// the file, using a comma as the delimiter
						String[] tokenize = fileRead.split("/");
						String id = tokenize[0];
						String type = tokenize[1];
						double x = Double.parseDouble((tokenize[2]));
						double y = Double.parseDouble(tokenize[3]);
						double demand = Double.parseDouble(tokenize[4]);
						double readyTime = Double.parseDouble(tokenize[5]);
						double dueDate = Double.parseDouble(tokenize[6]);
						double serviceTime = Double.parseDouble(tokenize[7]);
						Nodo nodo = new Nodo(id, type, x, y, demand, readyTime, dueDate, serviceTime);
						if (type.equals("f")) {
							distributori.add(nodo);
						} else if (type.equals("c")) {
							clienti.add(nodo);
						} else {
							depositi.add(nodo);
						}

						fileRead = br.readLine();
					} else {
						fileRead = br.readLine();
						while (fileRead != null) {
							String[] tokenize = fileRead.split("/");
							switch (count) {
							case 0:
								fuelTankCapacity = Double.parseDouble(tokenize[1]);
								count++;
								break;
							case 1:
								vehicleLoadCapacity = Double.parseDouble(tokenize[1]);
								count++;
								break;
							case 2:
								fuelConsumptionRate = Double.parseDouble(tokenize[1]);
								count++;
								break;
							case 3:
								inverseRefuelingRate = Double.parseDouble(tokenize[1]);
								count++;
								break;
							case 4:
								averageVelocity = Double.parseDouble(tokenize[1]);
								count++;
								break;
							}
							fileRead = br.readLine();
						}
					}

				}
				// close file stream
				br.close();
			} catch (Exception ex) {
				ex.printStackTrace();
			}
		}
		trovaSoluzione(clienti, distributori, depositi);
	}

	public void trovaSoluzione(ArrayList<Nodo> clienti, ArrayList<Nodo> distributori, ArrayList<Nodo> depositi) {
		ArrayList<Mezzo> mezzi = new ArrayList<Mezzo>();
		boolean fullcapacity = false;
		while (!clienti.isEmpty()) {
			Tappa tappa = new Tappa(depositi.get(0), 0, 0);
			Mezzo mezzo = new Mezzo(fuelTankCapacity, vehicleLoadCapacity, tappa);
			ultimaTappa = mezzo.getTappe().get(mezzo.getTappe().size() - 1);
			// se impostato a true vuol significare che non è possibile servire più nessun'altro cliente per motivi di tempo
			boolean timeException = false;
			while (fullcapacity == false && timeException == false && !clienti.isEmpty()) {
				// calcolo le distanze dei vari clienti dall'ultima tappa
				calcolaDistanze(clienti);
				// ordino il set di clienti in base alla loro distanza crescente
				distanzeClienti = sortHashMapByValues(distanzeClienti);
				Set<Nodo> clientiDaVisitare = new HashSet<Nodo>();
				clientiDaVisitare.addAll(distanzeClienti.keySet());
				for (Nodo nodo : clientiDaVisitare) {
					double arrivalTime = distanzeClienti.get(nodo) * averageVelocity + ultimaTappa.getDepartureTime();
					if (arrivalTime < nodo.getDueDate()) {
						if (mezzo.getLivelloCarico() >= nodo.getDemand()) {
							double carburanteNecessario = distanzeClienti.get(nodo) * fuelConsumptionRate;
							fullcapacity = false;
							if (mezzo.getLivelloCarburante() > carburanteNecessario) {
								double departureTime;
								if (arrivalTime >= nodo.getReadyTime()) {
									departureTime = arrivalTime + nodo.getServiceTime();
								} else {
									departureTime = arrivalTime + (nodo.getReadyTime() - arrivalTime) + nodo.getServiceTime();
								}
								Tappa prossimaTappa = new Tappa(nodo, arrivalTime, departureTime);
								mezzo.getTappe().add(prossimaTappa);
								mezzo.setLivelloCarburante(mezzo.getLivelloCarburante() - carburanteNecessario);
								mezzo.setLivelloCarico(mezzo.getLivelloCarico() - nodo.getDemand());
								ultimaTappa = prossimaTappa;
								clienti.remove(nodo);
								timeException = false;
							} else {
								timeException = false;
								aggiungiDistributore(clienti, mezzo, distributori);
							}
						} else {
							fullcapacity = true;
						}
					} else {
						timeException = true;
					}
				}
			}
			boolean fineTurno = false;
			while (fineTurno == false) {
				// devo tornare al deposito
				double distanzaDeposito = calcolaDistanza(ultimaTappa.getNodoDaVisitare(), depositi.get(0));
				double arrivalTimeInDepot = ultimaTappa.getDepartureTime() + (distanzaDeposito * averageVelocity);
				if (arrivalTimeInDepot < depositi.get(0).getDueDate()) {
					double carburante = calcolaDistanza(ultimaTappa.getNodoDaVisitare(), depositi.get(0)) * fuelConsumptionRate;
					if (mezzo.getLivelloCarburante() > carburante) {
						Tappa tappaFinale = new Tappa(depositi.get(0), arrivalTimeInDepot, 0);
						mezzo.getTappe().add(tappaFinale);
						mezzo.setLivelloCarburante(mezzo.getLivelloCarburante() - carburante);
						fineTurno = true;
					} else {
						// se non mi basta il carburante devo fare rifornimento
						aggiungiDistributore(clienti, mezzo, distributori);
					}
				} else {
					// se arrivo troppo tardi devo rimuovere l'ultimo cliente
					Tappa tappaDaRimuovore = mezzo.getTappe().get(mezzo.getTappe().size() - 1);
					if(tappaDaRimuovore.getNodoDaVisitare().getType().equals("c")){
						clienti.add(tappaDaRimuovore.getNodoDaVisitare());
					}
					mezzo.getTappe().remove(tappaDaRimuovore);
					ultimaTappa = mezzo.getTappe().get(mezzo.getTappe().size() - 1);
					mezzo.setLivelloCarburante((calcolaDistanza(tappaDaRimuovore.getNodoDaVisitare(), ultimaTappa.getNodoDaVisitare()) * fuelConsumptionRate) + mezzo.getLivelloCarburante());
					if (tappaDaRimuovore.getNodoDaVisitare().getType().equals("c")) {
						mezzo.setLivelloCarico(mezzo.getLivelloCarico() + tappaDaRimuovore.getNodoDaVisitare().getDemand());
					}
				}
			}
			mezzi.add(mezzo);
		}
		for (Mezzo mezzo : mezzi) {
			calcolaDistanzaPercorsa(mezzo);
			funzioneObbiettivo = funzioneObbiettivo + (mezzo.getKmPercorsi() + mezzo.getKmInEccedenza() * 2);
		}
		soluzioneMezzi = mezzi;
		funzioneObbiettivoIniziale = funzioneObbiettivo;
		writeSolution(mezzi, "Soluzione pre-Simulated Annealing");
		simulatedAnnealing();
	}

	public double calcolaTempo(Nodo nodo1, Nodo nodo2) {
		double distance = calcolaDistanza(nodo1, nodo2);
		double timeDuration = distance / averageVelocity;
		return timeDuration;
	}

	public double calcolaDistanza(Nodo nodo1, Nodo nodo2) {
		double distance = Math.sqrt(Math.pow(nodo2.getX() - nodo1.getX(), 2) + Math.pow(nodo2.getY() - nodo1.getY(), 2));
		return distance;
	}

	public double costoDistributore(Nodo distributore, Nodo nodo1, Nodo nodo2) {
		double costo = calcolaDistanza(distributore, nodo2) + calcolaDistanza(nodo1, distributore) - calcolaDistanza(nodo1, nodo2);
		return costo;
	}

	public void aggiungiDistributore(ArrayList<Nodo> clienti, Mezzo mezzo, ArrayList<Nodo> distributori) {

		// se non ho carburante necessario per raggiungere il cliente più vicino vado al distributore più vicino
		// calcolo le distanze dei vari clienti
		HashMap<Nodo, Double> distanzeDistributori = new HashMap<Nodo, Double>();

		for (Nodo distributore : distributori) {
			double distanza = calcolaDistanza(ultimaTappa.getNodoDaVisitare(), distributore);
			distanzeDistributori.put(distributore, Double.valueOf(distanza));
		}
		// ordino il set di distributori in base alla loro distanza crescente
		distanzeDistributori = sortHashMapByValues(distanzeDistributori);
		Set<Nodo> DistributoriDaVisitare = new HashSet<Nodo>();
		DistributoriDaVisitare.addAll(distanzeDistributori.keySet());
		for (Nodo nodoDistributore : DistributoriDaVisitare) {
			double arrivalTimeDistributore = distanzeDistributori.get(nodoDistributore) * averageVelocity + ultimaTappa.getDepartureTime();
			if (arrivalTimeDistributore < nodoDistributore.getDueDate()) {
				double carburanteNecc = distanzeDistributori.get(nodoDistributore) * fuelConsumptionRate;
				if (mezzo.getLivelloCarburante() > carburanteNecc) {
					double tempoRifornimento = (fuelTankCapacity - mezzo.getLivelloCarburante() + carburanteNecc) * inverseRefuelingRate;
					double departureTime = arrivalTimeDistributore + tempoRifornimento;
					Tappa prossimaTappa = new Tappa(nodoDistributore, arrivalTimeDistributore, departureTime);
					double carburanteRestante = fuelTankCapacity;
					mezzo.getTappe().add(prossimaTappa);
					mezzo.setLivelloCarburante(carburanteRestante);
					ultimaTappa = prossimaTappa;
					break;
				} else {
					double kmInPiu = (carburanteNecc - mezzo.getLivelloCarburante()) * fuelConsumptionRate;
					mezzo.setKmInEccedenza(mezzo.getKmInEccedenza() + kmInPiu);
					double tempoRifornimento = fuelTankCapacity * inverseRefuelingRate;
					double departureTime = arrivalTimeDistributore + tempoRifornimento;
					Tappa prossimaTappa = new Tappa(nodoDistributore, arrivalTimeDistributore, departureTime);
					double carburanteRestante = fuelTankCapacity;
					mezzo.getTappe().add(prossimaTappa);
					mezzo.setLivelloCarburante(carburanteRestante);
					ultimaTappa = prossimaTappa;
					break;
				}

			}
		}
		calcolaDistanze(clienti);
	}

	public LinkedHashMap<Nodo, Double> sortHashMapByValues(HashMap<Nodo, Double> passedMap) {
		List<Nodo> mapKeys = new ArrayList<Nodo>(passedMap.keySet());
		List<Double> mapValues = new ArrayList<Double>(passedMap.values());
		Collections.sort(mapValues);

		LinkedHashMap<Nodo, Double> sortedMap = new LinkedHashMap<Nodo, Double>();

		Iterator<Double> valueIt = mapValues.iterator();
		while (valueIt.hasNext()) {
			Double val = valueIt.next();
			Iterator<Nodo> keyIt = mapKeys.iterator();

			while (keyIt.hasNext()) {
				Nodo key = keyIt.next();
				Double comp1 = passedMap.get(key);
				Double comp2 = val;

				if (comp1.equals(comp2)) {
					keyIt.remove();
					sortedMap.put(key, val);
					break;
				}
			}
		}
		return sortedMap;
	}

	public void calcolaDistanze(ArrayList<Nodo> clienti) {
		distanzeClienti.clear();
		for (Nodo clienteDaVisitare : clienti) {
			double distanza = calcolaDistanza(ultimaTappa.getNodoDaVisitare(), clienteDaVisitare);
			distanzeClienti.put(clienteDaVisitare, Double.valueOf(distanza));

		}
	}

	public void writeSolution(ArrayList<Mezzo> mezzi, String stringa) {
		try {
//			FileWriter fstream = new FileWriter("/Users/NicoMac/Desktop/prova.txt", true);
//TODO: Assegnare variabile filePath corretta all'inizio
			FileWriter fstream = new FileWriter(filePath, true);
			BufferedWriter fbw = new BufferedWriter(fstream);
			int i = 1;
			fbw.write(stringa + "\n");
			fbw.write("N.Corsa \t Nodo \t ArrivalTime \t DeparturTime \n ");
			for (Mezzo mezzo : mezzi) {
				for (Tappa tappa : mezzo.getTappe()) {
					fbw.write(i + ") \t" + tappa.getNodoDaVisitare().getId() + "\t" + tappa.getArrivalTime() + "\t" + tappa.getDepartureTime() + "\n");
				}
				i++;
				fbw.write(mezzo.getLivelloCarburante() + "\t" + mezzo.getLivelloCarico() + "\t" + "Eccedenza km: " + mezzo.getKmInEccedenza() + "\n");
			}
			fbw.write("I km totali percorsi sono:  " + funzioneObbiettivo + "\n" + "\n");
			if(elapsedTime != 0){
				fbw.write("I km percorsi sono diminuti del:  " + percMiglioramento + "% . Il tempo necessario per l'operazione di miglioramento è stato di: " + String.format("%d ore, %d min, %d sec, %d mill",
						TimeUnit.NANOSECONDS.toHours(elapsedTime),
						TimeUnit.NANOSECONDS.toMinutes(elapsedTime) -
							TimeUnit.HOURS.toMinutes(TimeUnit.NANOSECONDS.toMinutes(elapsedTime)),
                        TimeUnit.NANOSECONDS.toSeconds(elapsedTime) -
                                TimeUnit.MINUTES.toSeconds(TimeUnit.NANOSECONDS.toMinutes(elapsedTime)),
                        TimeUnit.NANOSECONDS.toMillis(elapsedTime) -
                        	TimeUnit.SECONDS.toMillis(TimeUnit.NANOSECONDS.toSeconds(elapsedTime))) + " \n" + "\n");
			}
			fbw.close();
		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}

	public void calcolaDistanzaPercorsa(Mezzo mezzo) {
		double distanzaPercorsa = 0;
		int numeroTappe = mezzo.getTappe().size();
		for (int i = 0; i < numeroTappe - 1; i++) {
			distanzaPercorsa = distanzaPercorsa + (calcolaDistanza(mezzo.getTappe().get(i).getNodoDaVisitare(), mezzo.getTappe().get(i + 1).getNodoDaVisitare()));
		}
		mezzo.setKmPercorsi(distanzaPercorsa);

	}

	
	/**
	 * Simulated Annealing: http://kursor.trunojoyo.ac.id/wp-content/uploads/2015/02/vol7no3_p1.pdf
	 */
	public void simulatedAnnealing() {
		Thread thread = new Thread(new Runnable() {
			public void run() {
				if (soluzioneMezzi.size() > 1) {
					long startTime = System.nanoTime();
					view.getProgressBar().setVisible(true);
					Random rand = new Random();
					double temp = 0.8;
					double finalTemp = 0.1;
					ArrayList<Mezzo> currentSolution = new ArrayList<Mezzo>();
					currentSolution.addAll(soluzioneMezzi);
					ArrayList<Mezzo> bestSolution = new ArrayList<Mezzo>();
					bestSolution.addAll(soluzioneMezzi);
					double valueOfCurrentSolution = funzioneObbiettivo;
					double valueOfBestSolution = funzioneObbiettivo;
					int k = 25;
					while (temp >= finalTemp) {
						for (int i = 1; i <= 6000; i++) {
							ArrayList<Mezzo> newSolution = new ArrayList<Mezzo>();
							newSolution.addAll(currentSolution);
							double valueOfNewSolution = 0;
							double prob = Math.random();
							if (prob <= 0.3) {
								int size = newSolution.size();
								int indice = rand.nextInt(size);
								Mezzo mezzoDaAggiornare = mossa1(newSolution.get(indice));
								newSolution.get(indice).setMezzo(mezzoDaAggiornare);
							} else if (prob <= 0.6) {
								int size = newSolution.size();
								int indice = rand.nextInt(size);
								int indice2;
								do {
									indice2 = rand.nextInt(size);
								} while (indice == indice2);

								ArrayList<Mezzo> mezziDaAggiornare = mossa2(newSolution.get(indice), newSolution.get(indice2));
								if (mezziDaAggiornare.get(0) == null) {
									newSolution.remove(indice);
									if(indice2 > indice){
										indice2 = indice2 - 1;
									}
								} else {
									newSolution.get(indice).setMezzo(mezziDaAggiornare.get(0));
								}
								newSolution.get(indice2).setMezzo(mezziDaAggiornare.get(1));

							} else if (prob <= 0.9) {
								int numeroTappe = 0;
								int indiceMezzo = 0;
								for (Mezzo mezzo : newSolution) {
									if (numeroTappe != 0) {
										if (mezzo.getTappe().size() < numeroTappe) {
											numeroTappe = mezzo.getTappe().size();
											indiceMezzo = newSolution.indexOf(mezzo);
										}
									} else {
										numeroTappe = mezzo.getTappe().size();
										indiceMezzo = newSolution.indexOf(mezzo);

									}
								}
								int indice;
								do {
									int size = newSolution.size();
									indice = rand.nextInt(size);
								} while (indice == indiceMezzo);
								ArrayList<Mezzo> mezziDaAggiornare = mossa2(newSolution.get(indiceMezzo), newSolution.get(indice));
								if (mezziDaAggiornare.get(0) == null) {
									newSolution.remove(indiceMezzo);
									if(indice > indiceMezzo){
										indice = indice - 1;
									}
								} else {
									newSolution.get(indiceMezzo).setMezzo(mezziDaAggiornare.get(0));
								}
								newSolution.get(indice).setMezzo(mezziDaAggiornare.get(1));
							} else {
								double kmPercorsi = 0.0;
								int indiceMezzo = 0;
								for (Mezzo mezzo : newSolution) {
									if (kmPercorsi != 0) {
										if (mezzo.getKmPercorsi() < kmPercorsi) {
											kmPercorsi = mezzo.getKmPercorsi();
											indiceMezzo = newSolution.indexOf(mezzo);
										}
									} else {
										kmPercorsi = mezzo.getKmPercorsi();
										indiceMezzo = newSolution.indexOf(mezzo);
									}
								}
								int indice;
								do {
									int size = newSolution.size();
									indice = rand.nextInt(size);
								} while (indice == indiceMezzo);
								ArrayList<Mezzo> mezziDaAggiornare = mossa2(newSolution.get(indiceMezzo), newSolution.get(indice));
								if (mezziDaAggiornare.get(0) == null) {
									newSolution.remove(indiceMezzo);
									if(indice > indiceMezzo){
										indice = indice - 1;
									}
								} else {
									newSolution.get(indiceMezzo).setMezzo(mezziDaAggiornare.get(0));
								}
								newSolution.get(indice).setMezzo(mezziDaAggiornare.get(1));
							}
							for (Mezzo mezzo : newSolution) {
								calcolaDistanzaPercorsa(mezzo);
								valueOfNewSolution = valueOfNewSolution + (mezzo.getKmPercorsi() + mezzo.getKmInEccedenza() * 2);
							}
							if (valueOfNewSolution < valueOfCurrentSolution) {
								currentSolution.clear();
								currentSolution.addAll(newSolution);
								valueOfCurrentSolution = valueOfNewSolution;
								if (valueOfNewSolution < valueOfBestSolution) {
									bestSolution.clear();
									bestSolution.addAll(newSolution);
									valueOfBestSolution = valueOfNewSolution;
								}
							} else {
								if (1 / (Math.pow(Math.E, ((valueOfNewSolution - valueOfCurrentSolution) * k) / (valueOfCurrentSolution * temp))) >= Math.random()) {
									currentSolution.clear();
									currentSolution.addAll(newSolution);
									valueOfCurrentSolution = valueOfNewSolution;
								}
							}
						}
						temp = temp * 0.995;
					}
					soluzioneMezzi.clear();
					soluzioneMezzi.addAll(bestSolution);
					funzioneObbiettivo = valueOfBestSolution;
					for(Mezzo mezzo : soluzioneMezzi){
						for(int i=0; i<mezzo.getTappe().size()-1; i++){
							if (mezzo.getTappe().get(i).getNodoDaVisitare().getId().equals(mezzo.getTappe().get(i+1).getNodoDaVisitare().getId())){
								mezzo.getTappe().remove(i+1);
							}
						}
						aggiornaPercorsoMezzo(mezzo);
						calcolaDistanzaPercorsa(mezzo);
					}
					long stopTime = System.nanoTime();
				    elapsedTime = stopTime - startTime;
				    percMiglioramento = -(((funzioneObbiettivo/funzioneObbiettivoIniziale)*100)-100);
				    writeSolution(soluzioneMezzi, "Soluzione post-Simulated Annealing");
					view.getProgressBar().setVisible(false);
					view.getLblLeRotteSono().setText("Le rotte sono state calcolate");
				} else {
					percMiglioramento = -(((funzioneObbiettivo/funzioneObbiettivoIniziale)*100)-100);
					writeSolution(soluzioneMezzi, "Soluzione post-Simulated Annealing");
					view.getProgressBar().setVisible(false);
					view.getLblLeRotteSono().setText("Le rotte sono state calcolate");
				}
			}
		});
		thread.start();
	}

	public boolean aggiornaPercorsoMezzo(Mezzo mezzo) {

		boolean valido = true;
		mezzo.setLivelloCarburante(fuelTankCapacity);
		mezzo.setLivelloCarico(vehicleLoadCapacity);
		mezzo.setKmInEccedenza(0);
		for (int i = 0; i < mezzo.getTappe().size() - 1; i++) {
			Nodo nodoDaAggiornare = mezzo.getTappe().get(i + 1).getNodoDaVisitare();
			double distanza = calcolaDistanza(mezzo.getTappe().get(i).getNodoDaVisitare(), nodoDaAggiornare);
			double livelloCarburante = mezzo.getLivelloCarburante() - (distanza * fuelConsumptionRate);
			double arrivalTime = mezzo.getTappe().get(i).getDepartureTime() + (distanza + averageVelocity);
			double departureTime = 0;
			double livelloCarico = mezzo.getLivelloCarico() - nodoDaAggiornare.getDemand();
			if (nodoDaAggiornare.getType().equals("f")) {
				if (arrivalTime < nodoDaAggiornare.getDueDate()) {
					if (livelloCarburante < 0) {
						departureTime = arrivalTime + (fuelTankCapacity * inverseRefuelingRate);
						double kmInEccedenza = (-livelloCarburante) * fuelConsumptionRate;
						mezzo.setKmInEccedenza(kmInEccedenza);
						mezzo.getTappe().get(i + 1).setArrivalTime(arrivalTime);
						mezzo.getTappe().get(i + 1).setDepartureTime(departureTime);
						mezzo.setLivelloCarburante(fuelTankCapacity);
					} else {
						double rifornimentoNecessario = fuelTankCapacity - livelloCarburante;
						departureTime = arrivalTime + (rifornimentoNecessario * inverseRefuelingRate);
						mezzo.getTappe().get(i + 1).setArrivalTime(arrivalTime);
						mezzo.getTappe().get(i + 1).setDepartureTime(departureTime);
						mezzo.setLivelloCarburante(fuelTankCapacity);
					}
				} else {
					valido = false;
					break;
				}
			} else {
				// carico veicolo
				if (arrivalTime < nodoDaAggiornare.getDueDate()) {
					if (livelloCarburante > 0) {
						if (livelloCarico >= 0) {
							if (arrivalTime > nodoDaAggiornare.getReadyTime()) {
								departureTime = arrivalTime + nodoDaAggiornare.getServiceTime();
								mezzo.getTappe().get(i + 1).setArrivalTime(arrivalTime);
								mezzo.getTappe().get(i + 1).setDepartureTime(departureTime);
								mezzo.setLivelloCarico(livelloCarico);
								mezzo.setLivelloCarburante(livelloCarburante);
							} else {
								departureTime = arrivalTime + (nodoDaAggiornare.getReadyTime() - arrivalTime) + nodoDaAggiornare.getServiceTime();
								mezzo.getTappe().get(i + 1).setArrivalTime(arrivalTime);
								mezzo.getTappe().get(i + 1).setDepartureTime(departureTime);
								mezzo.setLivelloCarico(livelloCarico);
								mezzo.setLivelloCarburante(livelloCarburante);
							}
						} else {
							valido = false;
							break;
						}
					} else {

						valido = false;
						break;
					}
				} else {
					valido = false;
					break;
				}
			}
		}
		return valido;
	}

	public Mezzo mossa1(Mezzo mezzoInput) {
		Mezzo mezzoOutput = new Mezzo(mezzoInput.getLivelloCarburante(), mezzoInput.getLivelloCarico(), mezzoInput.getKmInEccedenza(), mezzoInput.getKmPercorsi(), mezzoInput.getTappe());
		boolean valido = false;
		boolean fineMetodo = false;

		for (int i = 1; i < mezzoInput.getTappe().size() - 1; i++) {
			for (int y = i + 1; y < mezzoInput.getTappe().size() - 1; y++) {
				Mezzo mezzoAggiornato = new Mezzo(mezzoInput.getLivelloCarburante(), mezzoInput.getLivelloCarico(), mezzoInput.getKmInEccedenza(), mezzoInput.getKmPercorsi(), mezzoInput.getTappe());
				Tappa tappa = mezzoAggiornato.getTappe().get(i);
				mezzoAggiornato.getTappe().remove(i);
				mezzoAggiornato.getTappe().add(y, tappa);
				calcolaDistanzaPercorsa(mezzoAggiornato);
				valido = aggiornaPercorsoMezzo(mezzoAggiornato);
				if (mezzoAggiornato.getKmPercorsi() < mezzoInput.getKmPercorsi() && valido) {
					mezzoOutput.setMezzo(mezzoAggiornato);
					fineMetodo = true;
					break;
				}
			}
			if (fineMetodo) {
				break;
			}
		}
		return mezzoOutput;
	}

	public ArrayList<Mezzo> mossa2(Mezzo mezzo1, Mezzo mezzo2) {
		ArrayList<Mezzo> mezziInOutput = new ArrayList<Mezzo>();
		for (Tappa tappa : mezzo1.getTappe()) {
			if (tappa.getNodoDaVisitare().getType().equals("c")) {
				Mezzo mezzoDaAlleggerire = new Mezzo(mezzo1.getLivelloCarburante(), mezzo1.getLivelloCarico(), mezzo1.getKmInEccedenza(), mezzo1.getKmPercorsi(), mezzo1.getTappe());
				Tappa tappaRimossa = mezzoDaAlleggerire.getTappe().get(mezzoDaAlleggerire.getTappe().indexOf(tappa));
				mezzoDaAlleggerire.getTappe().remove(tappa);
				int countClienti = 0;
				for (Tappa controlloTappe : mezzoDaAlleggerire.getTappe()) {
					if (controlloTappe.getNodoDaVisitare().getType().equals("c")) {
						countClienti++;
					}
				}
				if (countClienti == 0) {
					mezzoDaAlleggerire = null;
					for (int i = 1; i <= mezzo2.getTappe().size() - 1; i++) {
						Mezzo mezzoDaAppesantire = new Mezzo(mezzo2.getLivelloCarburante(), mezzo2.getLivelloCarico(), mezzo2.getKmInEccedenza(), mezzo2.getKmPercorsi(), mezzo2.getTappe());
						mezzoDaAppesantire.getTappe().add(i, tappaRimossa);
						boolean valido = aggiornaPercorsoMezzo(mezzoDaAppesantire);
						if (valido) {
							mezziInOutput.add(mezzoDaAlleggerire);
							mezziInOutput.add(mezzoDaAppesantire);
							break;
						}
					}
					if (!mezziInOutput.isEmpty()) {
						break;
					}
				} else {
					boolean valido = aggiornaPercorsoMezzo(mezzoDaAlleggerire);
					if (valido) {
						for (int i = 1; i < mezzo2.getTappe().size() - 1; i++) {
							Mezzo mezzoDaAppesantire = new Mezzo(mezzo2.getLivelloCarburante(), mezzo2.getLivelloCarico(), mezzo2.getKmInEccedenza(), mezzo2.getKmPercorsi(), mezzo2.getTappe());
							mezzoDaAppesantire.getTappe().add(i, tappaRimossa);
							boolean valido2 = aggiornaPercorsoMezzo(mezzoDaAppesantire);
							if (valido2) {
								mezziInOutput.add(mezzoDaAlleggerire);
								mezziInOutput.add(mezzoDaAppesantire);
								break;
							}
						}
					}
					if (!mezziInOutput.isEmpty()) {
						break;
					}
				}

			}
		}
		if (mezziInOutput.isEmpty()) {
			Mezzo mezzoDaAlleggerire = new Mezzo(mezzo1.getLivelloCarburante(), mezzo1.getLivelloCarico(), mezzo1.getKmInEccedenza(), mezzo1.getKmPercorsi(), mezzo1.getTappe());
			mezziInOutput.add(mezzoDaAlleggerire);
			Mezzo mezzoDaAppesantire = new Mezzo(mezzo2.getLivelloCarburante(), mezzo2.getLivelloCarico(), mezzo2.getKmInEccedenza(), mezzo2.getKmPercorsi(), mezzo2.getTappe());
			mezziInOutput.add(mezzoDaAppesantire);
		}
		return mezziInOutput;
	}
}
