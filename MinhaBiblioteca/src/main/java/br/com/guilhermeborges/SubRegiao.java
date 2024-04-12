package br.com.guilhermeborges;

import android.location.Location;

// Classe SubRegiao que estende a classe Regiao, representando uma sub-região dentro de uma região principal.
public class SubRegiao extends Regiao {

    private Regiao regiaoPrincipal; // Variável privada para armazenar a região principal à qual a sub-região pertence.
    public SubRegiao(){} // Construtor padrão sem parâmetros.

    // Construtor que inicializa a sub-região com nome, coordenadas, usuário e timestamp.
    public SubRegiao(String name, double latitude, double longitude, int usuario, long timestamp){
        super(name, latitude, longitude, usuario, timestamp);
        regiaoPrincipal = null;
    }

    // Método sobrescrito para calcular a distância entre duas coordenadas geográficas.
    @Override
    public double calcularDistancia(double lat1, double lon1, double lat2, double lon2) {
        float[] results = new float[3];
        Location.distanceBetween(lat1, lon1, lat2, lon2, results);
        return results[0];
    }

    // Métodos getter e setter específicos para acessar e modificar a região principal associada à sub-região.//

    public Regiao getRegiaoPrincipal(){ return regiaoPrincipal; }
    public void setRegiaoPrincipal(Regiao regiaoPrincipal){this.regiaoPrincipal = regiaoPrincipal;}
}
