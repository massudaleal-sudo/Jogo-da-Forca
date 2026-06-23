package com.example.jogodaforca;

import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;


public class CadastroActivity extends AppCompatActivity {

    // Declaração da referência do nosso gerenciador de banco de dados
    private BancoDadosHelper dbHelper;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Infla o layout XML (activity_cadastro.xml) transformando as tags gráficas em objetos de memória
        setContentView(R.layout.activity_cadastro);

        // Inicializa a nossa classe de persistência local passando o contexto desta tela
        dbHelper = new BancoDadosHelper(this);

        // Encontra os componentes visuais criados no XML através de seus respectivos IDs únicos
        EditText etPalavra = findViewById(R.id.etPalavra);
        EditText etCategoria = findViewById(R.id.etCategoria);
        Button btnSalvar = findViewById(R.id.btnSalvar);

        // Programação defensiva: verifica se o botão realmente existe antes de aplicar lógica, evitando NullPointerException
        if (btnSalvar != null) {

            // Captura o clique do usuário usando uma expressão Lambda para otimizar o código
            btnSalvar.setOnClickListener(v -> {

                // EXTRAÇÃO E TRATAMENTO DE TEXTO:
                // O .trim() é fundamental para remover espaços acidentais no início ou fim que quebrariam o jogo da forca
                String palavraDigitada = etPalavra.getText().toString().trim();
                String categoriaDigitada = etCategoria.getText().toString().trim();

                // VALIDAÇÃO DE CAMPOS (Regra de Interface):
                // Impede o envio de dados em branco para manter a integridade referencial do banco de dados
                if (palavraDigitada.isEmpty() || categoriaDigitada.isEmpty()) {
                    Toast.makeText(CadastroActivity.this, "Por favor, preencha todos os campos!", Toast.LENGTH_SHORT).show();
                    return; // Interrompe a execução do clique imediatamente
                }

                // Executa o métododentro do BancoDadosHelper e armazena se a transação SQL deu certo ou errado
                boolean sucesso = dbHelper.inserirNovaPalavra(palavraDigitada, categoriaDigitada);

                // TRATAMENTO DA RESPOSTA DO BANCO:
                if (sucesso) {
                    // Toast é um componente nativo de feedback visual rápido (UI/UX)
                    Toast.makeText(CadastroActivity.this, "Palavra registrada com sucesso!", Toast.LENGTH_SHORT).show();

                    // Limpa os campos de texto para resetar o estado visual da tela
                    etPalavra.setText("");
                    etCategoria.setText("");

                    // O método finish() encerra, destrói esta tela e remove ela da pilha de telas do Android.
                    // Isso faz o usuário retornar automaticamente para a MainActivity (Home).
                    finish();
                } else {
                    Toast.makeText(CadastroActivity.this, "Erro ao salvar no banco.", Toast.LENGTH_SHORT).show();
                }
            });
        }
    }
}