package com.example.jogodaforca;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

    private EditText etNickname;
    private ImageButton btnAvatar;
    private Button btnIniciar;

    private int avatarSelecionadoId = android.R.drawable.ic_menu_gallery;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        setRequestedOrientation(android.content.pm.ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        etNickname = findViewById(R.id.etNickname);
        btnAvatar = findViewById(R.id.btnAvatar);
        btnIniciar = findViewById(R.id.btnIniciar);

        btnAvatar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                Toast.makeText(MainActivity.this, "Avatar selecionado com sucesso!", Toast.LENGTH_SHORT).show();
            }
        });

        btnAvatar.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                Intent intentCadastro = new Intent(MainActivity.this, CadastroActivity.class);
                startActivity(intentCadastro);
                return true;
            }
        });

        btnIniciar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String nickname = etNickname.getText().toString().trim();

                if (nickname.isEmpty()) {
                    etNickname.setError(getString(R.string.erro_nick_vazio));
                    return;
                }

                Intent intentJogo = new Intent(MainActivity.this, JogoActivity.class);
                intentJogo.putExtra("PLAYER_NICK", nickname);
                intentJogo.putExtra("PLAYER_AVATAR", avatarSelecionadoId);

                startActivity(intentJogo);
            }
        });
    }
}