package com.example.jogodaforca;

import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.database.Cursor;
import android.os.Bundle;
import android.text.InputFilter;
import android.text.InputType;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.card.MaterialCardView;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private int selectedAvatarId = R.drawable.shrek;
    private MaterialCardView cardAvatar1, cardAvatar2, cardAvatar3;
    private int primaryColor;
    private BancoDadosHelper dbHelper;

    // Listas de controle paralelo de perfis
    private List<String> listaNomesParaExibicao = new ArrayList<>();
    private List<String> listaNicknamesReais = new ArrayList<>();
    private List<Integer> listaAvatarsSalvos = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        dbHelper = new BancoDadosHelper(this);

        com.google.android.material.chip.Chip chipId = new com.google.android.material.chip.Chip(this);
        primaryColor = chipId.getTextColors().getDefaultColor();

        final AutoCompleteTextView etNickname = findViewById(R.id.etNickname);
        Button btnIniciar = findViewById(R.id.btnIniciar);

        if (etNickname != null) {
            etNickname.setFilters(new InputFilter[]{});
            etNickname.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);
        }

        cardAvatar1 = findViewById(R.id.cardAvatar1);
        cardAvatar2 = findViewById(R.id.cardAvatar2);
        cardAvatar3 = findViewById(R.id.cardAvatar3);

        ImageButton btnAvatar1 = findViewById(R.id.btnAvatar1);
        ImageButton btnAvatar2 = findViewById(R.id.btnAvatar2);
        ImageButton btnAvatar3 = findViewById(R.id.btnAvatar3);

        btnAvatar1.setOnClickListener(v -> updateAvatarSelection(R.drawable.shrek, cardAvatar1));
        btnAvatar2.setOnClickListener(v -> updateAvatarSelection(R.drawable.sapo, cardAvatar2));
        btnAvatar3.setOnClickListener(v -> updateAvatarSelection(R.drawable.zeca, cardAvatar3));

        View.OnLongClickListener adminLongClickListener = v -> {
            Toast.makeText(MainActivity.this, "Admin Mode", Toast.LENGTH_SHORT).show();
            Intent intentCadastro = new Intent(MainActivity.this, CadastroActivity.class);
            startActivity(intentCadastro);
            return true;
        };

        btnAvatar1.setOnLongClickListener(adminLongClickListener);
        btnAvatar2.setOnLongClickListener(adminLongClickListener);
        btnAvatar3.setOnLongClickListener(adminLongClickListener);

        // CARREGA E CONFIGURA O DROPDOWN DE PERFIS
        carregarPerfisCadastrados(etNickname);

        btnIniciar.setOnClickListener(v -> {
            String nick = etNickname.getText().toString().trim();

            if (nick.isEmpty()) {
                etNickname.setError("Required");
                return;
            }

            // Garante o registro inicial do perfil se for a primeira vez dele
            dbHelper.salvarOuAtualizarPontuacao(nick, selectedAvatarId, 0);

            Intent intent = new Intent(MainActivity.this, JogoActivity.class);
            intent.putExtra("PLAYER_NICK", nick);
            intent.putExtra("PLAYER_AVATAR", selectedAvatarId);
            startActivity(intent);
        });
    }

    private void carregarPerfisCadastrados(AutoCompleteTextView etNickname) {
        listaNomesParaExibicao.clear();
        listaNicknamesReais.clear();
        listaAvatarsSalvos.clear();

        int maiorPontuacaoGeral = dbHelper.obterMaiorPontuacaoGeral();
        Cursor cursor = dbHelper.obterTodosOsJogadores();

        if (cursor != null) {
            while (cursor.moveToNext()) {
                String nick = cursor.getString(cursor.getColumnIndexOrThrow(BancoDadosHelper.COLUNA_JOGADOR_NICK));
                int avatar = cursor.getInt(cursor.getColumnIndexOrThrow(BancoDadosHelper.COLUNA_JOGADOR_AVATAR));
                int recorde = cursor.getInt(cursor.getColumnIndexOrThrow(BancoDadosHelper.COLUNA_JOGADOR_RECORDE));

                listaNicknamesReais.add(nick);
                listaAvatarsSalvos.add(avatar);

                // Monta a string minimalista e profissional
                String textoExibicao = nick + " - Max: " + recorde;
                if (recorde == maiorPontuacaoGeral && maiorPontuacaoGeral > 0) {
                    textoExibicao += " 🏆";
                }
                listaNomesParaExibicao.add(textoExibicao);
            }
            cursor.close();
        }

        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, listaNomesParaExibicao);
        etNickname.setAdapter(adapter);

        // AÇÃO AO CLICAR EM UM PERFIL EXISTENTE DA LISTA
        etNickname.setOnItemClickListener((parent, view, position, id) -> {
            String itemSelecionado = (String) parent.getItemAtPosition(position);
            int index = listaNomesParaExibicao.indexOf(itemSelecionado);

            if (index != -1) {
                String nickReal = listaNicknamesReais.get(index);
                int avatarSalvo = listaAvatarsSalvos.get(index);

                // Limpa o texto inserindo apenas o nick puro sem as pontuações extras
                etNickname.setText(nickReal, false);

                // Seleciona o avatar do perfil de forma automatizada na interface
                if (avatarSalvo == R.drawable.shrek) updateAvatarSelection(R.drawable.shrek, cardAvatar1);
                else if (avatarSalvo == R.drawable.sapo) updateAvatarSelection(R.drawable.sapo, cardAvatar2);
                else if (avatarSalvo == R.drawable.zeca) updateAvatarSelection(R.drawable.zeca, cardAvatar3);
            }
        });
    }

    private void updateAvatarSelection(int drawableId, MaterialCardView selectedCard) {
        selectedAvatarId = drawableId;

        cardAvatar1.setStrokeColor(ColorStateList.valueOf(Color.TRANSPARENT));
        cardAvatar2.setStrokeColor(ColorStateList.valueOf(Color.TRANSPARENT));
        cardAvatar3.setStrokeColor(ColorStateList.valueOf(Color.TRANSPARENT));

        selectedCard.setStrokeColor(ColorStateList.valueOf(primaryColor));
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Recarrega a lista toda vez que voltar do jogo (caso haja recordes novos)
        AutoCompleteTextView etNickname = findViewById(R.id.etNickname);
        if (etNickname != null) {
            carregarPerfisCadastrados(etNickname);
        }
    }
}