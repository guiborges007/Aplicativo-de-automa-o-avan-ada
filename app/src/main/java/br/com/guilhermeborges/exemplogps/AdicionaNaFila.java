package br.com.guilhermeborges.exemplogps;

import java.util.Queue;
import java.util.concurrent.Semaphore;

// Essa classe é uma implementação de Thread que adiciona uma região criptografada a uma fila ( Queue<Srting>)
// de forma thread-safe. Ela utiliza um Semaphorepara controlar o acesso à fila, garantindo que
// apenas um tópico possa adicionar uma região à fila por vez. Isso é crucial para evitar condições
// de execução e garantir a integridade dos dados na fila.
public class AdicionaNaFila extends Thread {
    private Queue<String> listaRegioes; // Fila para armazenar as regiões
    private Semaphore semaphore; // Semáforo para controlar o acesso à fila
    private String regiaoEncriptada; // Região candidata a ser adicionada à fila

    // Construtor para inicializar o gerenciador de fila de regiões
    public AdicionaNaFila(String regiaoEncriptada, Queue<String> listaDeRegioes) {
        listaRegioes = listaDeRegioes;
        semaphore = new Semaphore(1); // Inicializa o semáforo com 1 permit, permitindo apenas uma thread acessar a fila de uma vez
        this.regiaoEncriptada = regiaoEncriptada;
    }

    // Método run
    @Override
    public void run() {
        addRegiao();
    }

    // Método para adicionar uma região à fila
    public void addRegiao() {
        try {
            semaphore.acquire(); // Aguarda até que um permit esteja disponível
            listaRegioes.add(regiaoEncriptada); // Adicona refião encripitada
        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {
            semaphore.release(); // Libera o permit para que outras threads possam acessar a fila
        }
    }
}
