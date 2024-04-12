package br.com.guilhermeborges.exemplogps;

import android.content.Context;

import com.google.android.gms.tasks.Tasks;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicBoolean;

import br.com.guilhermeborges.ConversorJSON;
import br.com.guilhermeborges.Criptografia;
import br.com.guilhermeborges.Regiao;
import br.com.guilhermeborges.RegiaoRestrita;
import br.com.guilhermeborges.SubRegiao;

// Essa classe é usada para realizar consultas ao Firestore em um thread separado. A classe consulta a
// coleção "Regiões" no Firestore, calcula a distância entre a região candidata e cada região na coleção, e
// interrompe a operação para encontrar uma região próxima (com distância menor ou igual a 30). Essa abordagem
// mantém a responsividade do aplicativo e garante que as operações de leitura e gravação sejam seguras em um
// ambiente de programação concorrente.
public class ConsultaDB extends Thread{

    private FirebaseFirestore bancoDeDados; // Instância do Firestore para acesso ao banco de dados
    private Regiao regiaoCandidata; // Região candidata a ser verificada
    private AtomicBoolean existeRegiaoProxima; // Indica se existe uma região próxima
    private double distanciaMaxima; // Distancia a ser analisada na consulta
    private boolean procuraRegiaoMaisProxima;
    private Context contexto;

    // Construtor da classe
    public ConsultaDB(Context contexto, FirebaseFirestore bancoDeDados , Regiao regiaoCandidata, double distanciaMaxima, AtomicBoolean existeRegiaoProxima, boolean procuraRegiaoMaisProxima){
        this.bancoDeDados = bancoDeDados;
        this.regiaoCandidata = regiaoCandidata;
        this.existeRegiaoProxima = existeRegiaoProxima;
        this.distanciaMaxima = distanciaMaxima;
        this.procuraRegiaoMaisProxima = procuraRegiaoMaisProxima;
        this.contexto = contexto;
    }

    @Override
    public void run() {
        consultaNoDB();
        if(procuraRegiaoMaisProxima){ atualizaRegiaoMaisProximaNoDB();}
    }

    public void consultaNoDB(){

        CollectionReference regioesRef = bancoDeDados.collection("Regiões");

        try {
            QuerySnapshot querySnapshot = Tasks.await(bancoDeDados.collection("Regiões").get()); // Realiza a consulta síncrona ao Firestore
            for (QueryDocumentSnapshot document : querySnapshot) {
                String regiaoEncriptada = document.getString("regiao");
                Regiao regiao = reconstrucaoDeObjeto(regiaoEncriptada);
                double distancia = 0.0;
                if (regiaoCandidata instanceof SubRegiao || regiaoCandidata instanceof RegiaoRestrita){
                    if (regiao instanceof SubRegiao || regiao instanceof RegiaoRestrita) {
                        distancia = regiao.calcularDistancia(regiaoCandidata.getLatitude(), regiaoCandidata.getLongitude(), regiao.getLatitude(), regiao.getLongitude());
                        if (distancia <= distanciaMaxima) { // Verifica se a distância é menor ou igual a 30
                            existeRegiaoProxima.set(true); // Define que existe uma região próxima
                            break; // Interrompe a consulta
                        }
                    }
                }else{
                    if ( !(regiao instanceof SubRegiao) && !(regiao instanceof RegiaoRestrita)) {
                        distancia = regiao.calcularDistancia(regiaoCandidata.getLatitude(), regiaoCandidata.getLongitude(), regiao.getLatitude(), regiao.getLongitude());
                        if (distancia <= distanciaMaxima) { // Verifica se a distância é menor ou igual a 30
                            existeRegiaoProxima.set(true); // Define que existe uma região próxima
                            break; // Interrompe a consulta
                        }
                    }
                }
            }
        } catch (ExecutionException | InterruptedException e) {
            // Trata exceções de execução ou interrupção
        }
    }

    public Regiao reconstrucaoDeObjeto(String regiaoEncripitada){

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

    public void atualizaRegiaoMaisProximaNoDB() {
        double distanciaStart = 0.0;
        if (regiaoCandidata.getRegiaoPrincipal() == null ){
            distanciaStart = Double.MAX_VALUE;
        }else {
            distanciaStart = regiaoCandidata.calcularDistancia(regiaoCandidata.getLatitude(), regiaoCandidata.getLongitude(), regiaoCandidata.getRegiaoPrincipal().getLatitude(), regiaoCandidata.getRegiaoPrincipal().getLongitude());
        }
        try {
            QuerySnapshot querySnapshot = Tasks.await(bancoDeDados.collection("Regiões").get()); // Realiza a consulta síncrona ao Firestore
            for (QueryDocumentSnapshot document : querySnapshot) {
                String regiaoEncriptada = document.getString("regiao");
                Regiao regiao = reconstrucaoDeObjeto(regiaoEncriptada);

                if (!(regiao instanceof SubRegiao) && !(regiao instanceof RegiaoRestrita)) {
                    double distancia = regiao.calcularDistancia(regiaoCandidata.getLatitude(), regiaoCandidata.getLongitude(), regiao.getLatitude(), regiao.getLongitude());
                    if (distancia < distanciaStart) {
                        regiaoCandidata.setRegiaoPrincipal(regiao);
                        distanciaStart = distancia;
                    }
                }
            }
        } catch (ExecutionException | InterruptedException e) {
            // Trata exceções de execução ou interrupção
        }
    }
}



