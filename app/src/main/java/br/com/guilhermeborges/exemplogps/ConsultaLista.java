package br.com.guilhermeborges.exemplogps;

import android.content.Context;
import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicBoolean;
import br.com.guilhermeborges.ConversorJSON;
import br.com.guilhermeborges.Criptografia;
import br.com.guilhermeborges.Regiao;
import br.com.guilhermeborges.RegiaoRestrita;
import br.com.guilhermeborges.SubRegiao;

// Essa classe é uma implementação de Thread para realizar uma verificação em uma fila de
// regiões geográficas criptografadas( Queue<String>) para determinar se uma região candidata está dentro
// de um raio de 30 metros de qualquer região existente na fila. Ela utiliza um Semaphore
// para controlar o acesso à lista de regiões, garantindo que apenas um thread possa ser
// consultado por vez, e um AtomicBooleanpara indicar se uma região próxima foi encontrada de forma thread-safe.
public class ConsultaLista extends Thread {

    private Queue<String> listaRegioes; // Lista de regiões geográficas
    private Regiao regiaoCandidata; // Região candidata a ser verificada
    private Semaphore semaphore; // Semáforo para controlar o acesso à lista de regiões
    private AtomicBoolean existeRegiaoProxima; // Indica se existe uma região próxima
    private double distanciaMaxima; // Distancia a ser analisada na consulta
    private boolean procuraRegiaoMaisProxima; // Usado no caso de SubRegiões e RegiõesRestritas (Achar região principal mais próxima)
    private Context contexto; // Usado para mostrar avisos (Snackbar)
    private Queue<Regiao> listaRegioesRegioes; // Lista de regiões que é reconstruída sempre que a Thread é startada

    // Construtor da classe
    public ConsultaLista(Context contexto, Queue<String> listaRegioes, Regiao regiaoCandidata, double distanciaMaxima, AtomicBoolean existeRegiaoProxima, boolean procuraRegiaoMaisProxima) {
        this.listaRegioes = listaRegioes; // Inicializa a lista de regiões
        this.regiaoCandidata = regiaoCandidata; // Inicializa a região candidata
        semaphore = new Semaphore(1); // Inicializa o semáforo com um permit
        this.existeRegiaoProxima = existeRegiaoProxima; // Inicializa o indicador de existência de região próxima
        this.distanciaMaxima = distanciaMaxima; // Inicaliza o valor limite de distância
        this.procuraRegiaoMaisProxima = procuraRegiaoMaisProxima; // Inicializa o booleano para confirmar a operação de busca de região mais próxima
        this.contexto = contexto; // Inicializa o contexto
        listaRegioesRegioes = new LinkedList<>(); // Cria uma fila de obetos do tipo "Regiao" que será preenchida
    }


    // Método chamado quando a thread é startada
    @Override
    public void run() {
        reconstrucaoDaFila(); // Recontrói a fila para aproveitar a lógica do busca que já havia sido criada para fila de objetos do tipo "Regiao"
        consultaNaLista(); // Inicia a consulta na lista de regiões

        // Para subRegiões e regiões restritas, é necessário conferir qual a região mais próxima na fila,
        // para então determinar qual será sua região principal
        if(procuraRegiaoMaisProxima){
            atualizaRegiaoMaisProximaNaLista();
        }
    }


    // Método de consulta de distancia de regiões próximas. Pode ser usado tanto para distancia entre regiões (30m),
    // quanto para distância entre sub regiões e regiões restritas (5m)
    public void consultaNaLista() {
        try {
            semaphore.acquire(); // Aguarda até que um permit esteja disponível

            // Consulta en
            if (!(regiaoCandidata instanceof SubRegiao) && !(regiaoCandidata instanceof RegiaoRestrita)) { // Caso o tipo dinâmico da região seja "Regiao"
                for (Regiao regiaoExistente : listaRegioesRegioes) {
                    if(!(regiaoExistente instanceof SubRegiao) && !(regiaoExistente instanceof RegiaoRestrita)) { // Considera apenas outras "Regiões"
                        double distancia = regiaoExistente.calcularDistancia(regiaoCandidata.getLatitude(), regiaoCandidata.getLongitude(), regiaoExistente.getLatitude(), regiaoExistente.getLongitude()); // Calcula a distância entre a região candidata e a existente
                        if (distancia <= distanciaMaxima) {
                            existeRegiaoProxima.set(true); // Define que existe uma região próxima
                            break; // Interrompe o loop, pois já encontrou uma região próxima
                        }
                    }
                }
            }else {
                for (Regiao regiaoExistente : listaRegioesRegioes) {
                    if(regiaoExistente instanceof SubRegiao || regiaoExistente instanceof RegiaoRestrita) { // Considera apenas "SubRegiao" ou "RegiaoRestrita"
                        double distancia = regiaoExistente.calcularDistancia(regiaoCandidata.getLatitude(), regiaoCandidata.getLongitude(), regiaoExistente.getLatitude(), regiaoExistente.getLongitude()); // Calcula a distância entre a região candidata e a existente
                        if (distancia <= distanciaMaxima) {
                            existeRegiaoProxima.set(true); // Define que existe uma região próxima
                            break; // Interrompe o loop, pois já encontrou uma região próxima
                        }
                    }
                }
            }
        } catch (InterruptedException e) {
            e.printStackTrace(); // Trata a exceção de interrupção
        } finally {
            semaphore.release(); // Libera o permit para que outras threads possam acessar a fila
        }
    }

    // Método usado para calcular a região mais próxima da subRegiao ou RegiaoRestrita na fila
    private void atualizaRegiaoMaisProximaNaLista() {
        double distanciaStart = Double.MAX_VALUE;
        for (String regiaoEncriptada : listaRegioes){
            Regiao regiaoExistente = reconstrucaoDeObjeto(regiaoEncriptada);

            if (!(regiaoExistente instanceof RegiaoRestrita) && !(regiaoExistente instanceof SubRegiao)){
                double distancia = regiaoExistente.calcularDistancia(regiaoCandidata.getLatitude(), regiaoCandidata.getLongitude(), regiaoExistente.getLatitude(), regiaoExistente.getLongitude());
                if(distancia < distanciaStart){
                    regiaoCandidata.setRegiaoPrincipal(regiaoExistente);
                    distanciaStart = distancia;
                }
            }
        }
    }

    // Método que reconstroi a fila de objetos Regiao
    public void reconstrucaoDaFila(){
        for (String regiaoExistenteString : listaRegioes){
            listaRegioesRegioes.add(reconstrucaoDeObjeto(regiaoExistenteString));
        }
    }

    // Método que recebe uma string encriptada e retorna uma Região, subRegiao ou RegiaoRestrita
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
}
