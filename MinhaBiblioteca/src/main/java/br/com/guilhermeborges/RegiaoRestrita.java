package br.com.guilhermeborges;

import android.location.Location;

// Classe RegiaoRestrita que estende a classe Regiao, representando uma região restrita.
public class RegiaoRestrita extends Regiao {

    private Regiao regiaoPrincipal; // Variável privada para armazenar a região principal associada à região restrita.

    private boolean restrita; // Variável booleana para indicar se a região é restrita.

    public RegiaoRestrita(){} // Construtor padrão sem parâmetros.

    // Construtor que inicializa a região restrita com nome, coordenadas, usuário, timestamp e indicação de restrição.
    public RegiaoRestrita(String name, double latitude, double longitude, int usuario, long timestamp, boolean restrita){
        super(name, latitude, longitude, usuario, timestamp);
        this.regiaoPrincipal = null;
        this.restrita = restrita;
    }

    // Método sobrescrito para calcular a distância entre duas coordenadas geográficas.
    @Override
    public double calcularDistancia(double lat1, double lon1, double lat2, double lon2) {
        float[] results = new float[3];
        Location.distanceBetween(lat1, lon1, lat2, lon2, results);
        return results[0];
    }

    // Métodos getter e setter para acessar e modificar a região principal associada e o estado de restrição.//

    public Regiao getRegiaoPrincipal(){return regiaoPrincipal;}
    public boolean getRestrita(){return restrita;}
    public void setRegiaoPrincipal(Regiao regiaoPrincipal){this.regiaoPrincipal = regiaoPrincipal;}
    public void setRestrita(boolean restrita){this.restrita = restrita;}
}
