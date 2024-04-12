package br.com.guilhermeborges;

import com.google.gson.Gson;

// Essa classe é uma ferramenta para converter objetos Regiaoem strings JSON e vice-versa, utilizando a
// biblioteca Gson. Ela pode ser usada em um thread separado para evitar bloqueios na interface do usuário.
// A classe tem dois construtores: um para converter um objeto Regiaoem JSON e outro para converter uma
// string JSON em um objeto Regiao. A conversão é realizada pelos métodos converterRegiaoParaJsone
// converterJsonParaRegiao, que utilizam a biblioteca Gson para serialização e desserialização. A classe também
// identifica o tipo sonoro do objeto Regiaoreconstruído, verificando a presença de strings específicas na
// string JSON. Além disso, fornece métodos para recuperar o resultado da conversão e o tipo sonoro do objeto Regiaoreconstruído.
public class ConversorJSON extends Thread {
    private Regiao regiao; // Obejto regiao que armazena o objeto recebido, ou é usado para armazenar o objeto a ser retornado
    private String json; // String JSON que armazena a string recebida, ou é usada para armazenar a string a ser retornada
    private boolean regiaoParaJson; // Indica o tipo de conversão a ser feita
    private String tipoDeRegiao; // Usado como retorno para identificação do tipo dinâmico dos objetos reconstruidos

    // Construtor usando quando é necessário converter um objeto Regiao para uma string JSON
    public ConversorJSON(Regiao regiao) {
        this.regiao = regiao;
        this.regiaoParaJson = true;
    }

    // Construtor usando quando é necessário converter uma string JSON para um objeto Região
    public ConversorJSON(String json) {
        this.json = json;
        this.regiaoParaJson = false;
    }

    // Método executado quando a Thread é startada
    @Override
    public void run() {
        if (regiaoParaJson) {
            json = converterRegiaoParaJson(regiao);
        } else {
            regiao = converterJsonParaRegiao(json);
        }
    }

    // Converte objeto Regiao para uma string JSON
    private String converterRegiaoParaJson(Regiao regiao) {
        Gson gson = new Gson();
        return gson.toJson(regiao);
    }

    // Converte string JSON para um obejeto do tipo Regiao e identifica seu tipo dinâmico.
    private Regiao converterJsonParaRegiao(String json) {
        Gson gson = new Gson();
        if (json.contains("restrita")){                     // Caso a string contenha o conjunto de caracteres "restrita", o tipo dinâmico será RegiaoRestrita
            setTipoDeRegiao("Regiao Restrita");
            return gson.fromJson(json, RegiaoRestrita.class);
        } else if (json.contains("Principal")) {            // Caso a string contenha o conjunto de caracteres "principal", o tipo dinâmico será SubRegiao
            setTipoDeRegiao("Sub Regiao");
            return gson.fromJson(json, SubRegiao.class);
        }else{                                              // Caso contrário, o tipo dinâmico é Regiao
            setTipoDeRegiao("Regiao");
            return gson.fromJson(json, Regiao.class);
        }
    }

    public Object getResultado() {
        if (regiaoParaJson) {
            return json;
        } else {
            return regiao;
        }
    }


    // Retorna o tipo dinâmico da região reconstruída
    public String getTipoDeRegiao(){
        return tipoDeRegiao;
    }

    // Usado para atualizar o tipo dinâmico da região reconstruída
    private void setTipoDeRegiao(String tipoDeRegiao){this.tipoDeRegiao = tipoDeRegiao;}
}
