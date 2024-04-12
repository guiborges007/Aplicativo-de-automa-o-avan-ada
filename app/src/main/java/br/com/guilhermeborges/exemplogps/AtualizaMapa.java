package br.com.guilhermeborges.exemplogps;

import static android.content.ContentValues.TAG;
import android.content.Context;
import android.graphics.Color;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import androidx.annotation.NonNull;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import br.com.guilhermeborges.ConversorJSON;
import br.com.guilhermeborges.Criptografia;
import br.com.guilhermeborges.Regiao;
import br.com.guilhermeborges.RegiaoRestrita;
import br.com.guilhermeborges.SubRegiao;

// Essa classe Thread atualiza a posição de um marcador em um mapa do Google Maps ( GoogleMap)
// com base nas coordenadas de latitude e longitude recebidas. Ela utiliza um Handler para enviar
// mensagens para o thread principal (UI thread) do Android, permitindo que a atualização da
// posição do marcador e a recentralização do mapa para a nova localização sejam realizadas
// de forma assíncrona e thread-safe. Isso é crucial para evitar o bloqueio da UI enquanto a
// localização é atualizada, mantendo um aplicativo responsivo. A classe também é responsável por
// inserir e controlar ícones e simbolos no mapa.
public class AtualizaMapa extends Thread {
    private Handler handler;
    private static GoogleMap mMap;
    private static Marker mCurrentLocationMarker;
    public static double latitudeRecebida;
    public static double longitudeRecebida;
    private static FirebaseFirestore bancoDeDados;
    private static boolean modoNavegacao;
    private static Context contexto;


    // Construtor da classe
    public AtualizaMapa(Context contexto, GoogleMap map, Marker currentLocationMarker, FirebaseFirestore bancoDeDados) {
        this.mMap = map;
        this.mCurrentLocationMarker = currentLocationMarker;
        this.bancoDeDados = bancoDeDados;
        this.contexto = contexto;
        modoNavegacao = false;



        // Inicializa o Handler para atualizar a UI a partir do Thread
        // O Handler é usado para enviar mensagens para a thread principal (UI thread)
        this.handler = new Handler(Looper.getMainLooper()) {
            @Override
            public void handleMessage(Message msg) {
                super.handleMessage(msg);

                // Atualiza a posição do marcador com a nova localização
                // A nova localização é enviada como um objeto LatLng
                LatLng newLocation = (LatLng) msg.obj;
                mCurrentLocationMarker.setPosition(newLocation);

                // Se o modo navegação estiver ativado, passa a centralizar a tela na localização atual
                if(modoNavegacao) {
                    mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(newLocation, 18));
                }
            }
        };
    }

    // Método executado quando a Thread é startada
    @Override
    public void run() {

        // Carrega os marcadores de regiçoes salvas no banco de dados
        carregaMarcadoresDeRegioesSalvasNoDB();

        // Loop infinito para atualizar a localização
        while (true) {
            try {
                // Simula a obtenção da localização atual
                // Substitua por sua lógica de obtenção de latitude e longitude
                double latitude = latitudeRecebida;
                double longitude = longitudeRecebida;
                LatLng newLocation = new LatLng(latitude, longitude);

                // Atualiza o marcador na UI usando o Handler
                // Envia a nova localização para a thread principal através do Handler
                handler.obtainMessage(0, newLocation).sendToTarget();

                // Simula um atraso para não sobrecarregar a UI
                // Ajuste o tempo conforme necessário
                Thread.sleep(1000);

            } catch (InterruptedException e) {
                // Log de erro em caso de interrupção da thread
                Log.e("LocationUpdateThread", "Thread interrupted", e);
                break; // Encerra o loop em caso de interrupção
            }
        }
    }

                        //----------------------------------------//
                        // METODOS PARA TRATAR MARCADORES NO MAPA //
                        //----------------------------------------//

    // Carrega os marcadores de regiões salvas do DB (quando o app é iniciado, as regiões salvas no DB precisam aparecer no mapa)
    public static void carregaMarcadoresDeRegioesSalvasNoDB(){
        bancoDeDados.collection("Regiões").get().addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
            @Override
            public void onComplete(@NonNull Task<QuerySnapshot> task) {
                if (task.isSuccessful()) {
                    for (QueryDocumentSnapshot document : task.getResult()) {
                        String regiaoEncriptada = document.getString("regiao");
                        Regiao regiao = reconstrucaoDeObjeto(regiaoEncriptada);

                        if (regiao instanceof SubRegiao){
                            insereMarcadoresNoMapa(regiao, "Sub Região", true, 5.0);
                        }else if(regiao instanceof RegiaoRestrita){
                            insereMarcadoresNoMapa(regiao, "Região Restrita", true, 5.0);
                        }else{
                            insereMarcadoresNoMapa(regiao, "Região Comum", true, 30.0);
                        }
                    }
                } else {
                    Log.d(TAG, "Falha na consulta: ", task.getException());
                }
            }
        });
    }


    // Remove marcadores do tipo região quando as regiões são salvadas no banco de dados e atualiza os marcadores
    public static void atualizaMarcadores_transferenciaParaDB(){

        // Limpa todos os marcsdores
        mMap.clear();

        // Adiciona o marcador de localização atual
        LatLng currentLocation = new LatLng(AtualizaMapa.getLatitudeRecebida(), AtualizaMapa.getLongitudeRecebida());
        mCurrentLocationMarker = mMap.addMarker(new MarkerOptions().position(currentLocation).title("Minha Localização"));

        // Carrega os marcadores de regiões salvas no banco de dados
        carregaMarcadoresDeRegioesSalvasNoDB();
    }


    // Método para configurar e inserir um marcador de localização no mapa
    public static void insereMarcadoresNoMapa(Regiao regiao, String tipoMarcador,  boolean salvarNoBanco, double raio){
        MarkerOptions markerOptions = new MarkerOptions()
                .position(new LatLng(regiao.getLatitude(), regiao.getLongitude()))
                .title(regiao.getNome());

        // Define o ícone do marcador
        switch (tipoMarcador) {
            case "Região Comum":
                markerOptions.icon(BitmapDescriptorFactory.fromResource(android.R.drawable.ic_menu_myplaces));
                break;
            case "Região Restrita":
                markerOptions.icon(BitmapDescriptorFactory.fromResource(android.R.drawable.ic_lock_idle_lock));
                break;
            case "Sub Região":
                markerOptions.icon(BitmapDescriptorFactory.fromResource(android.R.drawable.ic_menu_mylocation));
                break;
        }

        mMap.addMarker(markerOptions); // Adiciona o marcador ao mapa
        CircleOptions circleOptions = new CircleOptions();  // Instantiando CircleOptions para desenhar um círculo em torno do marcador
        circleOptions.center(new LatLng(regiao.getLatitude(), regiao.getLongitude())); // Especificando o centro do círculo
        circleOptions.radius(raio); // Raio em metros// Raio do círculo

        // Cor do círculo
        if (salvarNoBanco){
            circleOptions.strokeColor(Color.BLUE); // Salvas no DB
            circleOptions.fillColor(0x110000FF); // Azul com transparência
        }else{
            circleOptions.strokeColor(Color.RED); // Salvas na fila
            circleOptions.fillColor(0x11FF0000); // Azul com transparência
        }

        circleOptions.strokeWidth(3); // Largura da borda do círculo
        mMap.addCircle(circleOptions); // Adicionando o círculo ao GoogleMap
    }

    // O método de reconstruir obejto também é necessário aqui, uma vez que é necessário recuperar dados do banco de dados e reconstruir objetos
    public static Regiao reconstrucaoDeObjeto(String regiaoEncripitada){

        Criptografia criptografia = new Criptografia(contexto, regiaoEncripitada, false);
        criptografia.start();
        String jsonComum = null;
        try {
            criptografia.join(); // Aguarda a conclusão da thread
            jsonComum = criptografia.getResultado(); // Obtém o resultado da conversão
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        Regiao regiaoExistente = null;
        ConversorJSON conversorJSON = new ConversorJSON(jsonComum);
        conversorJSON.start();
        try {
            conversorJSON.join(); // Aguarda a conclusão da thread
            String qualTipoDeRegiao = conversorJSON.getTipoDeRegiao();

            if (qualTipoDeRegiao.equals("Sub Regiao")){
                regiaoExistente = (SubRegiao)conversorJSON.getResultado(); // Obtém o resultado da conversão
            }else if(qualTipoDeRegiao.equals("Regiao Restrita")){
                regiaoExistente = (RegiaoRestrita)conversorJSON.getResultado(); // Obtém o resultado da conversão
            }else
                regiaoExistente = (Regiao)conversorJSON.getResultado(); // Obtém o resultado da conversão
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return regiaoExistente;
    }


                             // MÉTODOS GETS //
    public static double getLatitudeRecebida() {return latitudeRecebida;}

    public static double getLongitudeRecebida() {return longitudeRecebida;}


                            // MÉTODOS SETS //
    public static void setModoNavegacao(boolean modoNavegacao_){modoNavegacao = modoNavegacao_;}

}
