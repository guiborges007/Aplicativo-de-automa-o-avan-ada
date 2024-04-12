package br.com.guilhermeborges.exemplogps;

import android.util.Log;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.HashMap;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.Semaphore;

// Essa classe é responsável por transferir dados de uma fila de regiões criptografadas para uma coleção no Firebase
// Firestore. Ela utiliza um Semaphorepara controlar o acesso ao fio, garantindo que apenas um fio
// possa realizar a transferência por vez. Isso é crucial para evitar condições de execução e garantir
// a integridade dos dados na fila. Além disso, a classe implementa uma interface Thread, permitindo
// que suas instâncias sejam executadas como threads separadas, facilitando a execução assíncrona da transferência de dados.
public class TransferenciaDados extends Thread{

    private Queue<String> listaDeRegioes; // Fila para armazenar as regiões criptografadas
    private Semaphore semaphore; // Semáforo para controlar o acesso à fila
    private FirebaseFirestore bancoDeDados; // Instância do Firebase Firestore para acessar o banco de dados

    // Construtor que inicializa a fila de regiões e o banco de dados Firestore
    public TransferenciaDados(Queue<String> listaDeRegioes, FirebaseFirestore bancoDeDados){
        this.listaDeRegioes = listaDeRegioes;
        this.bancoDeDados = bancoDeDados;
        semaphore = new Semaphore(1); // Inicializa o semáforo com 1 permit, permitindo apenas uma thread acessar a fila de uma vez
    }

    @Override
    public void run() {
        transfereDados(); // Inicia a transferência de dados
    }


    // Método para transferir dados da fila para o Firebase Firestore
    public void transfereDados() {
        try {
            semaphore.acquire(); // Aguarda até que um permit esteja disponível

            CollectionReference regioesRef = bancoDeDados.collection("Regiões");

            while (!listaDeRegioes.isEmpty()) { // Enquanto a fila não estiver vazia

                String regiaoEnriptada = listaDeRegioes.poll();

                Map<String, Object> dados = new HashMap<>(); // Cria um mapa para armazenar os dados
                dados.put("regiao", regiaoEnriptada); // Adiciona a string ao mapa

                regioesRef.add(dados) // Adiciona o mapa à coleção "Regiões"
                        .addOnSuccessListener(documentReference -> {
                            Log.e("SuaClasse", "Região adicionada com sucesso: " + documentReference.getId());  // Log de sucesso ao adicionar a região
                        })
                        .addOnFailureListener(e -> {
                            Log.e("SuaClasse", "Erro ao adicionar região", e); // Log de erro ao adicionar a região
                        });
            }
        } catch (InterruptedException e) {
            e.printStackTrace(); // Trata a exceção de interrupção
        } finally {
            semaphore.release(); // Libera o permit para que outras threads possam acessar a fila
        }
    }
}
