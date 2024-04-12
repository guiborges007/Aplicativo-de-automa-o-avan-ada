package br.com.guilhermeborges;

import android.content.Context;
import android.util.Base64;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.security.Key;
import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

// Classe projetada para realizar operações de criptografia e descrição de forma assíncrona. Ela utiliza o algoritmo
// AES (Advanced Encryption Standard) para transformar dados em texto legível em texto cifrado e vice-versa, garantindo
// a confidencialidade dos dados. A classe gerencia a chave secreta usada para criptografia e descrição, armazenando-a
// de forma persistente no dispositivo e recuperando-a conforme necessário. Além disso, ela permite a execução de
// operações de criptografia e descrição em um thread separado, evitando bloquear a interface do usuário
public class Criptografia extends Thread {

    // Constantes para o nome do arquivo onde a chave secreta será armazenada e o tamanho da chave em bits.
    private static final String CHAVE_SECRETA_FILE = "chave_secreta";
    private static final int TAMANHO_CHAVE_BITS = 128;

    // Variáveis de instância para armazenar a chave secreta, indicar se a operação é de criptografia ou descriptografia,
    // e armazenar os dados JSON a serem criptografados ou descriptografados.
    private SecretKey chaveSecreta;
    private boolean criptografar;
    private String jsonCriptografado;
    private String jsonComum;
    private Context context;

    // Construtor que inicializa a classe com o contexto da aplicação, a string JSON a ser criptografada ou descriptografada,
    // e um booleano indicando se a operação é de criptografia.
    public Criptografia(Context context, String json, boolean criptografar) {
        this.context = context;
        this.criptografar = criptografar;
        if (criptografar) {
            this.jsonComum = json;
        } else {
            this.jsonCriptografado = json;
        }
        this.chaveSecreta = obterOuGerarChave();
    }

    // Método run() da Thread, que executa a operação de criptografia ou descriptografia de forma assíncrona.
    @Override
    public void run() {
        if (criptografar) {
            jsonCriptografado = criptografar();
        } else {
            jsonComum = descriptografar();
        }
    }

    // Método público para criptografar a string JSON.
    public String criptografar() {
        try {
            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");// Obtém uma instância do Cipher para o algoritmo AES/CBC/PKCS5Padding.
            byte[] iv = new byte[16];// Cria um vetor de inicialização (IV) de 16 bytes para o modo CBC.
            cipher.init(Cipher.ENCRYPT_MODE, chaveSecreta, new IvParameterSpec(iv));// Inicializa o Cipher no modo de criptografia com a chave secreta e o IV.
            byte[] bytesCriptografados = cipher.doFinal(jsonComum.getBytes());// Criptografa a string JSON em bytes e retorna os bytes criptografados.
            return Base64.encodeToString(bytesCriptografados, Base64.DEFAULT);// Converte os bytes criptografados para uma string Base64 para facilitar o armazenamento e transmissão.
        } catch (Exception e) {
            e.printStackTrace();// Imprime a pilha de rastreamento da exceção em caso de erro.
            return null; // Retorna null em caso de falha na criptografia.
        }
    }


    // Método público para descriptografar a string JSON criptografada.
    public String descriptografar() {
        try {
            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding"); // Obtém uma instância do Cipher com o algoritmo AES em modo CBC com padding PKCS5.
            // Cria um vetor de inicialização (IV) de 16 bytes, necessário para o modo CBC.
            byte[] iv = new byte[16]; // O mesmo vetor de inicialização usado para criptografia
            cipher.init(Cipher.DECRYPT_MODE, chaveSecreta, new IvParameterSpec(iv)); // Inicializa o Cipher no modo de descriptografia com a chave secreta e o IV.
            byte[] bytesDescriptografados = cipher.doFinal(Base64.decode(jsonCriptografado, Base64.DEFAULT)); // Decodifica a string JSON criptografada de Base64 para bytes e descriptografa.
            return new String(bytesDescriptografados); // Converte os bytes descriptografados de volta para uma string e retorna.
        } catch (Exception e) {
            e.printStackTrace(); // Em caso de erro, imprime a pilha de rastreamento e retorna null.
            return null;
        }
    }


    // Método para recuperar o resultado da operação de criptografia ou descriptografia.
    public String getResultado() {
        if (criptografar) {
            return jsonCriptografado;
        } else {
            return jsonComum;
        }
    }

           //-------------------------------------------------------------------------//
           //        MÉTODOS PARA LIDAR COM A CRIAÇÃO E TRATAMENTO DA CHAVE           //
           //-------------------------------------------------------------------------//

    // Método privado para obter ou gerar a chave secreta. Tenta ler a chave do armazenamento persistente.
    // Se a chave não estiver disponível, gera uma nova chave e a armazena no armazenamento persistente.
    private SecretKey obterOuGerarChave() {
        try {
            chaveSecreta = lerChaveSecreta();
            if (chaveSecreta == null) {
                chaveSecreta = gerarChave();
                salvarChaveSecreta(chaveSecreta);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return chaveSecreta;
    }

    // Método privado para gerar uma nova chave secreta usando o algoritmo AES.
    private SecretKey gerarChave() throws Exception {
        KeyGenerator geradorChave = KeyGenerator.getInstance("AES");
        geradorChave.init(TAMANHO_CHAVE_BITS);
        return geradorChave.generateKey();
    }

    // Método privado para salvar a chave secreta no armazenamento persistente.
    private void salvarChaveSecreta(Key chave) {
        try {
            FileOutputStream fos = context.openFileOutput(CHAVE_SECRETA_FILE, Context.MODE_PRIVATE);
            byte[] chaveBytes = chave.getEncoded();
            fos.write(chaveBytes);
            fos.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Método privado para ler a chave secreta do armazenamento persistente.
    private SecretKey lerChaveSecreta() {
        try {
            FileInputStream fis = context.openFileInput(CHAVE_SECRETA_FILE);
            byte[] chaveBytes = new byte[fis.available()];
            fis.read(chaveBytes);
            fis.close();
            return new SecretKeySpec(chaveBytes, "AES");
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }
}
