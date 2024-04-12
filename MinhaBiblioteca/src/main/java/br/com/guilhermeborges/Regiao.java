package br.com.guilhermeborges;

import android.location.Location;
import java.io.Serializable;

// Essa classe representa uma região geográfica. Ela implementa uma interface Serializable, o que significa que os
// objetos dessa classe podem ser serializados e desserializados, permitindo que seu estado seja convertido em uma
// sequência de bytes para armazenamento ou transmissão, e posteriormente reconstruído em um objeto Java. Isso é útil
// para armazenar o estado de um objeto Regiao em um arquivo ou banco de dados e recuperá-lo posteriormente.
public class Regiao implements Serializable{

    protected String name;
    protected double latitude;
    protected double longitude;
    protected int usuario;
    protected long timestamp;
    public Regiao(){}

    // Construtor da classe
    public Regiao(String name, double latitude, double longitude, int usuario, long timestamp){
        this.name = name;
        this.latitude = latitude;
        this.longitude = longitude;
        this.usuario = usuario;
        this.timestamp = timestamp;
    }

    public double calcularDistancia(double lat1, double lon1, double lat2, double lon2) {
        float[] results = new float[3];
        Location.distanceBetween(lat1, lon1, lat2, lon2, results);
        return results[0];
    }

                 // MÉTODOS GETS //

    // Método getter para obter o nome da região
    public String getNome(){ return name; }

    // Método getter para obter a latitude da região
    public double getLatitude() {return latitude; }

    // Método getter para obter a longitude da região
    public double getLongitude() { return longitude; }

    // Método getter para obter o número de usuário
    public int getUsuario() { return usuario; }/////////////

    // Método getter para obter o timeStamp
    public long getTimestamp() { return timestamp; }
    public Regiao getRegiaoPrincipal(){return null;}


    // MÉTODOS SETS //

    // Método setter para atualizar o nome da região
    public void setNome(String name) { this.name = name; }

    // Método setter para atualizar a latitude da região
    public void setLatitude(double latitude) { this.latitude = latitude; }

    // Método setter para atualizar a longitude da região
    public void setLongitude(double longitude) { this.longitude = longitude;}

    // Método setter para atualizar o numero de usuário
    public void setUsuario(int usuario) { this.usuario = usuario; }

    // Método setter para atualizar o timeStamp
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }

    public void setRegiaoPrincipal(Regiao regiaoMaisProxima){}
}






