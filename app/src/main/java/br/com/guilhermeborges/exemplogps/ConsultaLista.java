package br.com.guilhermeborges.exemplogps;

import android.content.Context;

import org.checkerframework.checker.index.qual.SubstringIndexBottom;

import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicBoolean;

import br.com.guilhermeborges.ConversorJSON;
import br.com.guilhermeborges.Criptografia;
import br.com.guilhermeborges.Regiao;
import br.com.guilhermeborges.RegiaoRestrita;
import br.com.guilhermeborges.SubRegiao;

// Essa classe é uma implementação de Thread para realizar uma verificação em uma lista de
// regiões geográficas ( Queue<Regiao>) para determinar se uma região candidata está dentro
// de um raio de 30 metros de qualquer região existente na lista. Ela utiliza um Semaphore
// para controlar o acesso à lista de regiões, garantindo que apenas um thread possa ser
// consultado por vez, e um AtomicBooleanpara indicar se uma região próxima foi encontrada de forma thread-safe.
public class ConsultaLista extends Thread {

    private Queue<String> listaRegioes; // Lista de regiões geográficas
    private Regiao regiaoCandidata; // Região candidata a ser verificada
    private Semaphore semaphore; // Semáforo para controlar o acesso à lista de regiões
    private AtomicBoolean existeRegiaoProxima; // Indica se existe uma região próxima
    private double distanciaMaxima; // Distancia a ser analisada na consulta
    private boolean procuraRegiaoMaisProxima;
    private Context contexto;

    private Queue<Regiao> listaRegioesRegioes;

    // Construtor da classe
    public ConsultaLista(Context contexto, Queue<String> listaRegioes, Regiao regiaoCandidata, double distanciaMaxima, AtomicBoolean existeRegiaoProxima, boolean procuraRegiaoMaisProxima) {
        this.listaRegioes = listaRegioes; // Inicializa a lista de regiões
        this.regiaoCandidata = regiaoCandidata; // Inicializa a região candidata
        semaphore = new Semaphore(1); // Inicializa o semáforo com um permit
        this.existeRegiaoProxima = existeRegiaoProxima; // Inicializa o indicador de existência de região próxima
        this.distanciaMaxima = distanciaMaxima;
        this.procuraRegiaoMaisProxima = procuraRegiaoMaisProxima;
        this.contexto = contexto;
        listaRegioesRegioes = new LinkedList<>();
    }


    // Método chamado quando a thread é startada
    @Override
    public void run() {
        reconstrucaoDaFila();
        consultaNaLista(); // Inicia a consulta na lista de regiões

        // Para subRegiões e regiões restritas, é necessário conferir qual a região mais próxima na fila,
        // para então determinar qual será sua região principal
        if(procuraRegiaoMaisProxima){
            atualizaRegiaoMaisProximaNaLista();
        }
    }


    // Método de consulta de distancia de regiões próximas. Pode ser usado tanto para distancia entre regiões (30m),
    // quanto para distância entre sub regiões e regiões restritas
    public void consultaNaLista() {
        try {
            semaphore.acquire(); // Aguarda até que um permit esteja disponível

            // Consulta en
            if (!(regiaoCandidata instanceof SubRegiao) && !(regiaoCandidata instanceof RegiaoRestrita)) { // Caso o tipo dinâmico da região seja "Regiao"
                for (Regiao regiaoExistente : listaRegioesRegioes) {
                    if(!(regiaoExistente instanceof SubRegiao) && !(regiaoExistente instanceof RegiaoRestrita)) {
                        double distancia = regiaoExistente.calcularDistancia(regiaoCandidata.getLatitude(), regiaoCandidata.getLongitude(), regiaoExistente.getLatitude(), regiaoExistente.getLongitude()); // Calcula a distância entre a região candidata e a existente
                        if (distancia <= distanciaMaxima) { // Se a distância for menor ou igual a 30 metros
                            existeRegiaoProxima.set(true); // Define que existe uma região próxima
                            break; // Interrompe o loop, pois já encontrou uma região próxima
                        }
                    }
                }
            }else {
                for (Regiao regiaoExistente : listaRegioesRegioes) {
                    if(regiaoExistente instanceof SubRegiao || regiaoExistente instanceof RegiaoRestrita) { // Caso o tipo dinâmico da região seja "SubRegiao" ou "RegiaoRestrita"
                        double distancia = regiaoExistente.calcularDistancia(regiaoCandidata.getLatitude(), regiaoCandidata.getLongitude(), regiaoExistente.getLatitude(), regiaoExistente.getLongitude()); // Calcula a distância entre a região candidata e a existente
                        if (distancia <= distanciaMaxima) {
                            existeRegiaoProxima.set(true);
                            break;
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

    public void reconstrucaoDaFila(){
        for (String regiaoExistenteString : listaRegioes){
            listaRegioesRegioes.add(reconstrucaoDeObjeto(regiaoExistenteString));
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
}
