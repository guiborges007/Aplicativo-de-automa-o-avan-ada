package br.com.guilhermeborges.exemplogps;

import android.content.Context;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.widget.TextView;
import com.google.android.gms.maps.GoogleMap;

// Essa classe é responsável por coletar e atualizar a localização do dispositivo usando
// o GPS e exibir essas coordenadas em TextView e em um mapa do Google Maps. Ela implementou
// uma interface LocationListener, permitindo que a classe receba atualizações de localização
// do sistema operacional. Além disso, a classe possui um LocationManager para gerenciar
// as restrições de localização e um GoogleMap para exibir a localização atualizada no mapa.
public class ColetaLocalizacao implements LocationListener {

    private LocationManager locationManager; // Gerenciador de localização do sistema
    private TextView latitude; // TextView para exibir a latitude
    private TextView longitude; // TextView para exibir a longitude
    private String latitudeString; // String para armazenar a latitude
    private String longitudeString; // String para armazenar a longitude
    private Double latitudeDouble; // Double para armazenar a latitude
    private Double longitudeDouble; // Double para armazenar a longitude
    private GoogleMap mMap; // Mapa do Google Maps


    // Construtor da classe que recebe o contexto da aplicação e os TextViews onde as coordenadas serão exibidas.
    public ColetaLocalizacao(Context context, TextView latitude , TextView longitude , GoogleMap mMap) {
        this.latitude = latitude;
        this.longitude = longitude;
        this.mMap = mMap;
        // Obtém o serviço de localização do sistema operacional.
        locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
    }

    // Método para iniciar a atualização da localização.
    public void startLocationUpdates() {
        try {
            // Solicita atualizações de localização do provedor GPS.
            // minTime é o tempo mínimo entre atualizações em milissegundos.
            // minDistance é a distância mínima entre atualizações em metros.
            // O último parâmetro é o listener que receberá as atualizações de localização.
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1000, 1, this); // Esse método roda na Thread principal

        } catch (SecurityException e) {
            e.printStackTrace();
        }
    }

    // Método chamado quando a localização do dispositivo muda.
    @Override
    public void onLocationChanged(Location location) {
        if (location != null) {

            // Obtém a latitude e a longitude da localização atual
            latitudeString = String.valueOf(location.getLatitude());
            longitudeString = String.valueOf(location.getLongitude());

            // Reduz a precisão das coordenadas para 9 casas decimais
            String latReduzida = latitudeString.length() > 11 ? latitudeString.substring(0, 11) : latitudeString;
            String lonReduzida = longitudeString.length() > 11 ? longitudeString.substring(0, 11) : longitudeString;

            // Atualiza os TextViews com as novas coordenadas
            latitude.setText("   LATITUDE:      " + latReduzida);
            longitude.setText("   LONGITUDE:   " + lonReduzida);

            // Converte as strings de latitude e longitude para doubles
            latitudeDouble = Double.parseDouble(latitudeString);
            longitudeDouble = Double.parseDouble(longitudeString);

            // Atualiza variáveis estáticas na classe AtualizaMapa para uso em outras partes do aplicativo.
            AtualizaMapa.latitudeRecebida = latitudeDouble;
            AtualizaMapa.longitudeRecebida = longitudeDouble;
        }
    }

    // Métodos abaixo são parte da interface LocationListener, mas não são utilizados neste app.
    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {}
    @Override
    public void onProviderEnabled(String provider) {}
}



