package com.example.jogodaforca;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;


public class CadastroFragment extends Fragment {

    // Declaração dos componentes visuais e do manipulador do banco
    private EditText etNovaPalavra;
    private EditText etCategoria;
    private Button btnSalvarPalavra;
    private BancoDadosHelper dbHelper;

    public CadastroFragment() {
    }


    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {

        // INFLAR O LAYOUT:
        // O objeto 'inflater' pega o XML (fragment_cadastro.xml) e o transforma em um objeto View real do Java.
        View view = inflater.inflate(R.layout.fragment_cadastro, container, false);


        dbHelper = new BancoDadosHelper(requireContext());

        // MAPEAMENTO DOS COMPONENTES VISUAIS:
        // Como o escopo é do Fragment, precisamos buscar os IDs obrigatoriamente a partir da 'view' inflada acima.
        etNovaPalavra = view.findViewById(R.id.etNovaPalavra);
        etCategoria = view.findViewById(R.id.etCategoria);
        btnSalvarPalavra = view.findViewById(R.id.btnSalvarPalavra);

        // CONFIGURAÇÃO DO CLIQUE: Listener tradicional usando classe anônima
        btnSalvarPalavra.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Captura e limpeza de espaços invisíveis usando o .trim()
                String palavra = etNovaPalavra.getText().toString().trim();
                String categoria = etCategoria.getText().toString().trim();

                // VALIDAÇÃO DE INTERFACE:
                // Se algum campo estiver vazio, impede o avanço e exibe um erro mapeado nas Strings globais (R.string...)
                if (palavra.isEmpty() || categoria.isEmpty()) {
                    Toast.makeText(requireContext(), getString(R.string.erro_cadastro_vazio), Toast.LENGTH_SHORT).show();
                    return; // Interrompe a execução do clique
                }

                // COMUNICAÇÃO COM A CAMADA DATA:
                // Faz a chamada física enviando os dados tratados para salvar na tabela do SQLite
                boolean sucesso = dbHelper.inserirNovaPalavra(palavra, categoria);

                // FEEDBACK PARA O JOGADOR (UI/UX):
                if (sucesso) {
                    Toast.makeText(requireContext(), "Palavra guardada com sucesso!", Toast.LENGTH_SHORT).show();

                    // Limpa os campos após o sucesso para facilitar novos cadastros em sequência
                    etNovaPalavra.setText("");
                    etCategoria.setText("");
                } else {
                    Toast.makeText(requireContext(), "Erro ao guardar no banco de dados.", Toast.LENGTH_SHORT).show();
                }
            }
        });

        // Retorna a interface totalmente montada e configurada para ser exibida na tela
        return view;
    }
}