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

    private EditText etNovaPalavra;
    private EditText etCategoria;
    private Button btnSalvarPalavra;
    private BancoDadosHelper dbHelper;

    public CadastroFragment() {
        // Construtor público obrigatório vazio
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_cadastro, container, false);

        dbHelper = new BancoDadosHelper(requireContext());

        etNovaPalavra = view.findViewById(R.id.etNovaPalavra);
        etCategoria = view.findViewById(R.id.etCategoria);
        btnSalvarPalavra = view.findViewById(R.id.btnSalvarPalavra);

        btnSalvarPalavra.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String palavra = etNovaPalavra.getText().toString().trim();
                String categoria = etCategoria.getText().toString().trim();

                if (palavra.isEmpty() || categoria.isEmpty()) {
                    Toast.makeText(requireContext(), getString(R.string.erro_cadastro_vazio), Toast.LENGTH_SHORT).show();
                    return;
                }

                boolean sucesso = dbHelper.inserirNovaPalavra(palavra, categoria);

                if (sucesso) {
                    Toast.makeText(requireContext(), "Palavra guardada com sucesso!", Toast.LENGTH_SHORT).show();
                    etNovaPalavra.setText("");
                    etCategoria.setText("");
                } else {
                    Toast.makeText(requireContext(), "Erro ao guardar no banco de dados.", Toast.LENGTH_SHORT).show();
                }
            }
        });

        return view;
    }
}