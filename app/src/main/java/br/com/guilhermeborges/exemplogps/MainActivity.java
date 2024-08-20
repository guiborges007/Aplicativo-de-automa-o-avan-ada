package br.com.guilhermeborges.exemplogps;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.FrameLayout;
import android.widget.Switch;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.LinkedList;
import java.util.Objects;
import java.util.Queue;
import java.util.concurrent.atomic.AtomicBoolean;
import br.com.guilhermeborges.ConversorJSON;
import br.com.guilhermeborges.Criptografia;
import br.com.guilhermeborges.Regiao;
import br.com.guilhermeborges.RegiaoRestrita;
import br.com.guilhermeborges.SubRegiao;

// Essa é a classe principal do projeto, responsável por manipular os elementos da UI e também lançar as Threads usadas.
public class MainActivity extends AppCompatActivity implements OnMapReadyCallback {

    private ColetaLocalizacao coletaLocalizacao; // Objeto da classe ColetaLocalizacao, que é responsável por coletar a localização via GPS
    private GoogleMap mMap; // Obejeto da classe GoogleMap que fornece acesso aos dados e visualização do mapa
    private Marker mCurrentLocationMarker; // Objeto da classe Marker, que é usada para identificar locais no mapa
    private Queue<String> listaRegioes; // Declara uma fila de regiões
    private FirebaseFirestore bancoDeDados; // Objeto da classe FirebaseFirestore que permite armazenar e sincronizar dados entre os usuários em tempo real.
    private ConsultaDB consultaDB; // Obejeto da classe ConsultaDB, que é responsável por varrer o banco de dados e verificar a condição de 30 metros
    private ConsultaLista consultaLista; // Obejeto da classe ConsultaLista, que é responsável por varrer Fila de regiões e verificar a condição de 30 metros
    private AtomicBoolean existeRegiaoProxima; // Objeto da classe AtomicBoolean, que permite a leitura e escrita (de booleanos) segura em cenários de programação concorrente


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //----------------------------RECONCILIAÇÃO DE DADOS_____________________________________//

        double[] y = new double[] { 33441.36667, 31465.36667, 29637.66667, 27852.56667, 25825.46667}; //vetor de medidas

        double[] v = new double[] { 3497.024513, 3497.024513, 3427.605401, 3425.283675, 13378.44903}; //vetor de desvio padrões

        double[][] A = new double[][] {  // Matriz de incidência
              {-1, 1, 0, 0, 0},
              {0, -1, 1, 0, 0},
              {0, 0, -1, 1, 0},
              {0, 0, 0, -1, 1}
        };

        Reconciliacao rec = new Reconciliacao(y, v, A);
        double[] y_reconciled = rec.getReconciledFlow();

        // Exibir o resultado reconciliado no log
        String TAG = "RouteAnalysis";
        Log.d(TAG, "Dados Reconciliados:");
        for (int i = 0; i < y_reconciled.length; i++) {
            Log.d(TAG, "y^" + (i + 1) + "_reconciled: " + y_reconciled[i]);
        }

        listaRegioes = new LinkedList<>(); // Cria uma Fila de strings (criptografadas)
        bancoDeDados = FirebaseFirestore.getInstance(); // Cria uma única instância do FirebaseFirestore, que é o ponto central para interagir com o Cloud Firestore

        // Este trecho de código inicializa e configura um SupportMapFragmentno Android, possibilitando a incorporação do mapa no aplicativo.
        // Ele obtém uma referência ao fragmento de mapa adicionado ao layout da atividade e, em seguida, usa getMapAsync(this)para inicializar o
        // mapa de forma assíncrona, permitindo que a atividade configure o mapa e adicione funcionalidades de mapeamento interativos e dinâmicos.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        // Encontra os TextViews para exibir latitude e longitude e o TextView para coletar o nome de região digitado
        TextView latitude = findViewById(R.id.editLatitude);
        TextView longitude = findViewById(R.id.editLongitude);
        TextView nome = findViewById(R.id.editNomeRegiao);

        // Cria um objeto da classe ColetaLocalizacao e passa como parâmetros os TextView latitude e longitude.
        coletaLocalizacao = new ColetaLocalizacao(this, latitude, longitude, mMap);
        coletaLocalizacao.startLocationUpdates(); // Inícia a coleta de localização


        // Método exexcutado quando o evento click no botão "adicionar região" acontece
        Button botaoAdicionarRegiao = findViewById(R.id.botaoAdicionarRegiao);
        botaoAdicionarRegiao.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                //-------------------------------------------------------------------------//
                //     COLETA DE ATRIBUTOS INICIAIS PARA CRIAR UMA REGIAO GENÉRICA         //
                //-------------------------------------------------------------------------//

                String inpNome = nome.getText().toString(); // Assim que o botão é acionado, coleta-se o conteúdo do campo de digitação de nome da região
                int usuario = 1; // Define o usuário fixo igual a 1
                long timestamp = System.nanoTime(); // Coleta o timestamp do momento do click

                if (inpNome.isEmpty()){  // Caso não tenha nada digitado no campo de nome

                      mostrarMensagemUsuario("Preencha o campo de nome!" ,"vermelho");

                } else {

                    //-------------------------------------------------------------------------//
                    //            PROCESSO DE CONSULTA NA FILA E NO BANCO DE DADOS             //
                    //-------------------------------------------------------------------------//

                    // Cria uma região, que ainda será avalidada
                    Regiao regiaoGenerica = new Regiao(inpNome, AtualizaMapa.getLatitudeRecebida(), AtualizaMapa.getLongitudeRecebida(), usuario, timestamp);

                    existeRegiaoProxima = new AtomicBoolean(false); // Define "existeRegiaoProxima" como false. A variavél será usada na verificação da restrição de 30 metros

                    // Cria e inicia uma thread para varrer a fila e verificar se a região candidata está a pelo menos 30 metros de alguma região na lista
                    consultaLista = new ConsultaLista(MainActivity.this, listaRegioes, regiaoGenerica, 30.0,  existeRegiaoProxima, false);
                    consultaLista.start();

                    // Cria e inicia uma thread para varrer o banco de dados e verificar se a região candidata está a pelo menos 30 metros de alguma região no banco de dados
                    consultaDB = new ConsultaDB(MainActivity.this, bancoDeDados, regiaoGenerica, 30.0, existeRegiaoProxima, false);
                    consultaDB.start();

                    // Aguarda até que ambas as threads concluam
                    try {
                        consultaLista.join();
                        consultaDB.join();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }

                    // As Threads consultaLista e consultaDB iram atribuir true para "existeRegiaoProxima" caso exista pelo menos uma região a menos de 30 metros da região candidata.
                    if (existeRegiaoProxima.get()){  // Caso exista regiões nesse raio

                        perguntaQualOTipoDeRegiaoSecundaria(regiaoGenerica); //Chama método para tratar cadastro de Sub regiões e Regiões restritas

                    }else { // Caso não existam regiões nesse raio

                        //-------------------------------------------------------------------------//
                        //     PROCESSO DE CONVERSÃO PARA JSON, ENCRIPTAMENTO E ADIÇÃO NA FILA     //
                        //-------------------------------------------------------------------------//

                        // Passa o objeto Regiao e retorna uma string encriptada
                        String jsonEncriptado = converteParaJSONeCripitografa(regiaoGenerica);

                        // Adiciona a string encriptada na fila
                        AdicionaNaFila adicionaNaFila = new AdicionaNaFila(jsonEncriptado, listaRegioes);
                        adicionaNaFila.start();

                        // Mostra aviso ao usuário
                        mostrarMensagemUsuario("REGIÃO cadastrada com sucesso!", "azul");

                        // Chama método para adicionar os marcadores do tipo região da fila
                        AtualizaMapa.insereMarcadoresNoMapa(regiaoGenerica, "Região Comum", false, 30.0);
                    }
                }
                nome.setText(""); // Limpa o campo de digitação de nome
            }
        });


        // Método exexcutado quando o evento click no botão "Salvar no DB" acontece
        Button botaoSalvarDB = findViewById(R.id.botaoSalvarDB);
        botaoSalvarDB.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if (listaRegioes.isEmpty()){ // Caso a fila esteja vazia
                    mostrarMensagemUsuario("Adicione uma região antes de salvar no banco de dados!" , "vermelho");
                }else {

                    // Cria um objeto da Thread TransferenciaDados e passa os parâmetros necessários para a transferência
                    TransferenciaDados transferenciaDados = new TransferenciaDados(listaRegioes , bancoDeDados);
                    transferenciaDados.start();

                    mostrarMensagemUsuario("Regiões salvas no banco de dados!" , "azul");

                    // Chama método para remover os marcadores do tipo região na fila e atualizar com marcadores de regiões salvas no DB
                    AtualizaMapa.atualizaMarcadores_transferenciaParaDB();
                }
            }
        });

        // Método exexcutado quando o evento click no botão "Salvar no DB" acontece
        Button TimeStamp = findViewById(R.id.TimeStamp);
        TimeStamp.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                // Obtém o timestamp atual em milissegundos
                long timestamp = System.currentTimeMillis();

                // Formata o timestamp como uma string
                String formattedTimestamp = Long.toString(timestamp);

                // Registra a mensagem no Logcat com o timestamp
                Log.i("Timestamp", "Timestamp: " + formattedTimestamp);
            }
        });


        // Método para tratar os estados da switch "Modo navegação"
        Switch switchMapa = (Switch) findViewById(R.id.switchMapa);
        switchMapa.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) { // Caso o modo esteja ativado
                    AtualizaMapa.setModoNavegacao(true); // Atribui true a variável "modo navegação" na thread atualiza mapa
                } else {
                    AtualizaMapa.setModoNavegacao(false); // Atribui false a variável "modo navegação" na thread atualiza mapa
                }
            }
        });
    }


    //-------------------------------------------------------------------------//
    //                 OUTROS MÉTODOS UTILIZADOS NO PROJETO                    //
    //-------------------------------------------------------------------------//


    // Método que abre uma caixa de pergunta para o usuário decidir qual o tipo de região secundária será cadastrada.
    // Dependendo da escolha do usuário, cadastra-se uma Sub região ou região restrita, verificando os requisitos de
    // distnância mínima.
    public void perguntaQualOTipoDeRegiaoSecundaria(Regiao regiao) {

        // Configura o construtor do AlertDialog
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Aqui não é possível cadastrar regiões comuns!");
        builder.setMessage("Como você deseja cadastrar essa localização?");

        // Botão "Cadastrar como nova região"
        builder.setPositiveButton("CADASTRAR COMO SUB REGIÃO", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {

                //-------------------------------------------------------------------------//
                //            PROCESSO DE CONSULTA NA FILA E NO BANCO DE DADOS             //
                //-------------------------------------------------------------------------//

                SubRegiao subRegiaoCandidata = new SubRegiao(regiao.getNome(), regiao.getLatitude(), regiao.getLongitude(), regiao.getUsuario(),regiao.getTimestamp());

                AtomicBoolean existeRegiaoProximaSub = new AtomicBoolean(false);

                ConsultaLista consultaListaSub = new ConsultaLista(MainActivity.this, listaRegioes, subRegiaoCandidata, 5.0, existeRegiaoProximaSub,true);
                consultaListaSub.start();
                try {consultaListaSub.join();} catch (InterruptedException e) {e.printStackTrace();} // Aguarda até que a threads conclua

                ConsultaDB consultaDBSub = new ConsultaDB(MainActivity.this, bancoDeDados, subRegiaoCandidata, 5.0, existeRegiaoProximaSub, true);
                consultaDBSub.start();
                try {consultaDBSub.join();} catch (InterruptedException e) {e.printStackTrace();} // Aguarda até que a thread conclua

                if (existeRegiaoProximaSub.get()){
                    mostrarMensagemUsuario("Já existem SUB REGIÕES ou REGIÕES RESTRITAS em um raio de 5 metros!" ,"vermelho");
                }
                else {

                    //-------------------------------------------------------------------------//
                    //     PROCESSO DE CONVERSÃO PARA JSON, ENCRIPTAMENTO E ADIÇÃO NA FILA//   //
                    //-------------------------------------------------------------------------//

                    String jsonEncriptado = converteParaJSONeCripitografa(subRegiaoCandidata);

                    // Cria um objeto da Thread AdicionaNafila e passa os parâmetros necessários para adiconar a região na fila
                    AdicionaNaFila adicionaNaFila = new AdicionaNaFila(jsonEncriptado, listaRegioes);
                    adicionaNaFila.start();

                    // Chama método para adicionar os marcadores do tipo região da fila
                    AtualizaMapa.insereMarcadoresNoMapa(subRegiaoCandidata, "Sub Região", false, 5.0);
                    mostrarMensagemUsuario("SUB REGIÃO cadastrada com sucesso!" , "azul");
                }
            }
        });

        // Botão "Cadastrar como região restrita"
        builder.setNegativeButton("CADASTRAR COMO REGIÃO RESTRITA", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {

                //-------------------------------------------------------------------------//
                //            PROCESSO DE CONSULTA NA FILA E NO BANCO DE DADOS             //
                //-------------------------------------------------------------------------//

                Regiao regiaoRestritaCandidata = new RegiaoRestrita(regiao.getNome(), regiao.getLatitude(), regiao.getLongitude(), regiao.getUsuario(),regiao.getTimestamp(), true);
                AtomicBoolean existeRegiaoProximaRestrita = new AtomicBoolean(false);

                ConsultaLista consultaListaRestrita = new ConsultaLista(MainActivity.this, listaRegioes, regiaoRestritaCandidata, 5.0, existeRegiaoProximaRestrita, true);
                consultaListaRestrita.start();
                try {consultaListaRestrita.join();} catch (InterruptedException e) {e.printStackTrace();} // Aguarda até que a thread conclua

                ConsultaDB consultaDBREstrita = new ConsultaDB(MainActivity.this, bancoDeDados, regiaoRestritaCandidata, 5.0, existeRegiaoProximaRestrita, true);
                consultaDBREstrita.start();
                try {consultaDBREstrita.join();} catch (InterruptedException e) {e.printStackTrace();} // Aguarda até que a thread conclua

                if (existeRegiaoProximaRestrita.get()){
                    mostrarMensagemUsuario("Já existem SUB REGIÕES ou REGIÕES RESTRITAS em um raio de 5 metros!" ,"vermelho");
                }
                else {

                    //-------------------------------------------------------------------------//
                    //     PROCESSO DE CONVERSÃO PARA JSON, ENCRIPTAMENTO E ADIÇÃO NA FILA//   //
                    //-------------------------------------------------------------------------//

                    String jsonEncriptado = converteParaJSONeCripitografa(regiaoRestritaCandidata);

                    // Cria um objeto da Thread AdicionaNafila e passa os parâmetros necessários para adiconar a região na fila
                    AdicionaNaFila adicionaNaFila = new AdicionaNaFila(jsonEncriptado, listaRegioes);
                    adicionaNaFila.start();

                    // Chama método para adicionar os marcadores do tipo região da fila
                    AtualizaMapa.insereMarcadoresNoMapa(regiaoRestritaCandidata, "Região Restrita", false, 5.0);
                    mostrarMensagemUsuario("REGIÃO RESTRITA cadastrada com sucesso!" , "azul");
                }
            }
        });

        // Cria e mostra o AlertDialog
        AlertDialog dialog = builder.create();
        dialog.show();
    }

    // Esse método é responsável por receber um objeo do tipo Região e então transforma-lo em uma string no formato
    // JSON. Logo em seguida ele encripta essa string, resultando em uma string criptografada. Para fazer isso, esse método
    // starta duas Threads: conversorJSON e Criptografia.
    public String converteParaJSONeCripitografa(Regiao regiao){
        ConversorJSON conversorJSON = new ConversorJSON(regiao);
        conversorJSON.start();
        String jsonComum = null;
        try {
            conversorJSON.join(); // Aguarda a conclusão da thread
            jsonComum = conversorJSON.getResultado().toString(); // Obtém o resultado da conversão
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        Criptografia criptografia = new Criptografia(MainActivity.this, jsonComum, true);
        criptografia.start();
        String jsonEncriptado = null;
        try {
            criptografia.join(); // Aguarda a conclusão da thread
            jsonEncriptado = criptografia.getResultado(); // Obtém o resultado da conversão
            return jsonEncriptado;
        } catch (InterruptedException e) {
            e.printStackTrace();
            return null;
        }
    }

    // Este método é chamado quando o mapa do Google Maps está pronto para ser usado, garantindo
    // que o objeto GoogleMapnão seja nulo. Ele inicializa uma variável mMapcom a instância do mapa fornecido, define uma
    // localização inicial para um marcador no mapa e inicia um tópico para atualizar a localização do marcador dinamicamente.
    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        // Define uma localização inicial para o marcador
        double latitude = 10;
        double longitude = 10;

        // Insere o marcador de localização atual
        LatLng currentLocation = new LatLng(latitude, longitude);
        mCurrentLocationMarker = mMap.addMarker(new MarkerOptions().position(currentLocation).title("Minha Localização"));

        // Inicia a thread de atualização de localização
        AtualizaMapa atualizaMapa = new AtualizaMapa(MainActivity.this, mMap, mCurrentLocationMarker, bancoDeDados);
        atualizaMapa.start();
    }

    // Método para exibir avisos na tela para o usúario
    public void mostrarMensagemUsuario(String mensagem, String cor) {
        View rootView = findViewById(android.R.id.content);
        Snackbar snackbar = Snackbar.make(rootView, mensagem, Snackbar.LENGTH_SHORT);

        if (Objects.equals(cor, "vermelho")) {
            snackbar.setBackgroundTint(Color.RED);
        } else if (Objects.equals(cor, "azul")) {
            snackbar.setBackgroundTint(Color.BLUE);
        } else if (Objects.equals(cor, "verde")) {
            snackbar.setBackgroundTint(Color.GREEN);
        } else if (Objects.equals(cor, "amarelo")) {
            snackbar.setBackgroundTint(Color.YELLOW);
        }

        View snackbarView = snackbar.getView();
        FrameLayout.LayoutParams params = (FrameLayout.LayoutParams) snackbarView.getLayoutParams();
        params.gravity = Gravity.TOP;
        snackbarView.setLayoutParams(params);
        snackbar.show();
    }
}






