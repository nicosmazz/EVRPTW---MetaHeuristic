package ricercaOperativa.model;

import java.util.ArrayList;

public class Mezzo {
	private double livelloCarburante;
	private double livelloCarico;
	private double kmInEccedenza = 0;
	private double kmPercorsi;
	private ArrayList<Tappa> tappe = new ArrayList<Tappa>();
	
	public Mezzo(double livelloCarburante, double livelloCarico, Tappa tappa) {
		this.livelloCarburante = livelloCarburante;
		this.livelloCarico = livelloCarico;
		tappe.add(tappa);
	}
	
	public double getKmPercorsi() {
		return kmPercorsi;
	}

	public void setKmPercorsi(double kmPercorsi) {
		this.kmPercorsi = kmPercorsi;
	}

	public double getKmInEccedenza() {
		return kmInEccedenza;
	}

	public void setKmInEccedenza(double kmInEccedenza) {
		this.kmInEccedenza = kmInEccedenza;
	}
	
	public double getLivelloCarburante() {
		return livelloCarburante;
	}

	public void setLivelloCarburante(double livelloCarburante) {
		this.livelloCarburante = livelloCarburante;
	}

	public double getLivelloCarico() {
		return livelloCarico;
	}

	public void setLivelloCarico(double livelloCarico) {
		this.livelloCarico = livelloCarico;
	}

	public ArrayList<Tappa> getTappe() {
		return tappe;
	}
	
	public void setMezzo (Mezzo mezzo){
		this.livelloCarburante = mezzo.getLivelloCarburante();
		this.livelloCarico = mezzo.getLivelloCarico();
		this.kmInEccedenza = mezzo.getKmInEccedenza();
		this.kmPercorsi = mezzo.getKmPercorsi();
		this.tappe.clear();
		this.tappe.addAll(mezzo.getTappe());
	}

	public Mezzo(double livelloCarburante, double livelloCarico, double kmInEccedenza, double kmPercorsi, ArrayList<Tappa> tappe) {
		this.livelloCarburante = livelloCarburante;
		this.livelloCarico = livelloCarico;
		this.kmInEccedenza = kmInEccedenza;
		this.kmPercorsi = kmPercorsi;
		this.tappe.addAll(tappe);
	}
	
}
