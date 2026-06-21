package com.example.jogodaforca;

import android.Manifest;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.media.AudioAttributes;
import android.media.MediaPlayer;
import android.media.SoundPool;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

public class JogoActivity extends AppCompatActivity implements SensorEventListener {

    private static final String TAG = "JogoActivity";

    private TextView tvInfoJogador;
    private TextView tvCronometro;
    private TextView tvPalavraOculta;
    private TextView tvPontuacao;
    private ImageView ivForca;
    private ImageView ivAvatarJogo;
    private LinearLayout layoutTeclado;

    private BancoDadosHelper dbHelper;
    private String nickname;
    private int avatarId;
    private String palavraSorteada;
    private String categoriaSorteada;
    private StringBuilder progressoPalavra;
    private int errosCometidos = 0;
    private final int MAX_ERROS = 6;
    private int pontuacaoAtual = 0;

    private AlertDialog dialogoVisivel;
    private boolean dialogoEstaAberto = false;

    private Thread threadTempo;
    private Handler handlerTempo;
    private boolean tempoSendoContado = false;
    private int tempoRestanteSegundos = 180;
    private int tempoSalvoAoPausar = -1;
    private MediaPlayer mediaPlayerFundo;
    private SoundPool soundPoolEfeitos;
    private int somAcertoId, somErroId;
    private boolean sonsCarregados = false;

    private SensorManager sensorManager;
    private Sensor acelerometro;
    private Sensor sensorProximidade;
    private long ultimoTempoSensor = 0;
    private float ultimaCoordenadaX, ultimaCoordenadaY, ultimaCoordenadaZ;
    private static final int FORCA_CHACOALHAO = 800;

    private BroadcastReceiver receptorBateriaBaixa;
    private static final String CANAL_NOTIFICACAO_ID = "canal_jogo_forca";
    private static final int PERMISSAO_NOTIFICACAO_CODE = 101;

    @Override
    protected void onCreate(Bundle bundleSucedido) {
        super.onCreate(bundleSucedido);
        setContentView(R.layout.activity_jogo);

        dbHelper = new BancoDadosHelper(this);
        inicializarComponentes();
        configurarHandlerTempo();
        criarCanalNotificacao();
        verificarPermissaoNotificacao();
        inicializarAudio();

        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        if (sensorManager != null) {
            acelerometro = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
            sensorProximidade = sensorManager.getDefaultSensor(Sensor.TYPE_PROXIMITY);
        }

        configurarReceptorBateria();

        if (getIntent() != null && getIntent().hasExtra("PLAYER_NICK")) {
            nickname = getIntent().getStringExtra("PLAYER_NICK");
            avatarId = getIntent().getIntExtra("PLAYER_AVATAR", android.R.drawable.ic_menu_gallery);
        } else {
            nickname = "Jogador";
            avatarId = android.R.drawable.ic_menu_gallery;
        }

        if (tvInfoJogador != null) {
            tvInfoJogador.setText(String.format(Locale.getDefault(), "Jogador: %s", nickname));
        }
        if (ivAvatarJogo != null) {
            ivAvatarJogo.setImageResource(avatarId);
        }

        iniciarNovaPartida();
    }

    private void inicializarComponentes() {
        tvPalavraOculta = findViewById(R.id.tvPalavraOculta);
        ivForca = findViewById(R.id.ivForca);
        layoutTeclado = findViewById(R.id.llTecladoContainer);
        tvInfoJogador = findViewById(R.id.tvInfoJogador);
        ivAvatarJogo = findViewById(R.id.ivAvatarJogo);
        tvCronometro = findViewById(R.id.tvCronometro);
        tvPontuacao = findViewById(R.id.tvPontuacao);
        Button btnReiniciar = findViewById(R.id.btnReiniciar);
        Button btnSair = findViewById(R.id.btnSair);

        if (btnReiniciar != null) btnReiniciar.setOnClickListener(v -> {
            tempoSalvoAoPausar = -1; // Força resetar para 3 minutos ao clicar manualmente
            iniciarNovaPartida();
        });
        if (btnSair != null) btnSair.setOnClickListener(v -> finish());
    }

    private void inicializarAudio() {
        mediaPlayerFundo = MediaPlayer.create(this, R.raw.musica_fundo);
        if (mediaPlayerFundo != null) {
            mediaPlayerFundo.setLooping(true);
            mediaPlayerFundo.setVolume(0.4f, 0.4f);
        }

        AudioAttributes atributosAudio = new AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_GAME)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build();

        soundPoolEfeitos = new SoundPool.Builder()
                .setMaxStreams(5)
                .setAudioAttributes(atributosAudio)
                .build();

        somAcertoId = soundPoolEfeitos.load(this, R.raw.som_acerto, 1);
        somErroId = soundPoolEfeitos.load(this, R.raw.som_erro, 1);

        soundPoolEfeitos.setOnLoadCompleteListener((soundPool, sampleId, status) -> sonsCarregados = (status == 0));
    }

    private void tocarEfeitoSonoro(int somId) {
        if (sonsCarregados && soundPoolEfeitos != null) {
            soundPoolEfeitos.play(somId, 1.0f, 1.0f, 1, 0, 1.0f);
        }
    }

    private void configurarHandlerTempo() {
        handlerTempo = new Handler(Looper.getMainLooper()) {
            @Override
            public void handleMessage(@NonNull Message msg) {
                super.handleMessage(msg);

                int minutos = tempoRestanteSegundos / 60;
                int segundos = tempoRestanteSegundos % 60;
                String tempoFormatado = String.format(Locale.getDefault(), "%02d:%02d", minutos, segundos);

                if (tvCronometro != null) {
                    tvCronometro.setText(tempoFormatado);
                }

                if (tempoRestanteSegundos <= 0) {
                    pararTemporizador();
                    tocarEfeitoSonoro(somErroId);
                    mostrarDialogoFimDeJogo(false);
                }
            }
        };
    }

    private void iniciarTemporizador() {
        pararTemporizador();

        if (tempoSalvoAoPausar > 0) {
            tempoRestanteSegundos = tempoSalvoAoPausar;
            tempoSalvoAoPausar = -1;
        } else {
            tempoRestanteSegundos = 180;
        }

        tempoSendoContado = true;
        threadTempo = new Thread(() -> {
            while (tempoSendoContado && tempoRestanteSegundos > 0) {
                try {
                    Thread.sleep(1000);
                    tempoRestanteSegundos--;
                    handlerTempo.sendEmptyMessage(0);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        });
        threadTempo.start();
    }

    private void pararTemporizador() {
        tempoSendoContado = false;
        if (threadTempo != null && threadTempo.isAlive()) {
            threadTempo.interrupt();
        }
    }

    private void iniciarNovaPartida() {
        errosCometidos = 0;
        atualizarImagemForca();
        sortearPalavraDoBanco();

        if (palavraSorteada == null || palavraSorteada.isEmpty()) {
            palavraSorteada = "ANDROID";
            categoriaSorteada = "Tecnologia";
        }

        progressoPalavra = new StringBuilder();
        for (int i = 0; i < palavraSorteada.length(); i++) {
            progressoPalavra.append("_");
        }

        atualizarExibicaoPalavra();

        if (tvPontuacao != null) {
            tvPontuacao.setText(String.format(Locale.getDefault(), "Pontuação: %d  |  Tema: %s", pontuacaoAtual, categoriaSorteada));
        }

        gerarTecladoDinamico();
        iniciarTemporizador();
    }

    private void sortearPalavraDoBanco() {
        try {
            SQLiteDatabase db = dbHelper.getReadableDatabase();
            Cursor cursor = db.rawQuery("SELECT " + BancoDadosHelper.COLUNA_PALAVRA + ", "
                    + BancoDadosHelper.COLUNA_CATEGORIA + " FROM "
                    + BancoDadosHelper.TABELA_PALAVRAS
                    + " WHERE " + BancoDadosHelper.COLUNA_FOI_ACERTADA + " = 0"
                    + " ORDER BY RANDOM() LIMIT 1", null);

            if (cursor != null && cursor.moveToFirst()) {
                int indicePalavra = cursor.getColumnIndex(BancoDadosHelper.COLUNA_PALAVRA);
                int indiceCategoria = cursor.getColumnIndex(BancoDadosHelper.COLUNA_CATEGORIA);

                if (indicePalavra >= 0 && indiceCategoria >= 0) {
                    palavraSorteada = cursor.getString(indicePalavra).toUpperCase(Locale.getDefault()).trim();
                    categoriaSorteada = cursor.getString(indiceCategoria);
                }
                cursor.close();
            } else {
                if (cursor != null) cursor.close();

                if (pontuacaoAtual > 0) {
                    dbHelper.salvarOuAtualizarPontuacao(nickname, avatarId, pontuacaoAtual);
                }

                dbHelper.resetarTodasAsPalavras();

                new Handler(Looper.getMainLooper()).post(() -> {
                    com.google.android.material.dialog.MaterialAlertDialogBuilder conquistaDialog =
                            new com.google.android.material.dialog.MaterialAlertDialogBuilder(this);

                    conquistaDialog.setTitle("🎉 PARABÉNS! VOCÊ ZEROU O JOGO! 🎉");
                    conquistaDialog.setMessage(String.format(Locale.getDefault(),
                            "Incrível, %s!\n\nVocê desvendou absolutamente TODAS as palavras do nosso banco de dados!\n\nSua pontuação atual de %d pontos foi salva no topo do Ranking Geral. 🏆\n\nA lista de palavras foi renovada. Pronto para manter o seu reinado?",
                            nickname, pontuacaoAtual));

                    conquistaDialog.setCancelable(false);
                    conquistaDialog.setPositiveButton("Continuar Jogando 🔥", (dialog, which) -> {
                        tempoSalvoAoPausar = -1;
                        sortearPalavraDoBanco();
                    });

                    conquistaDialog.show();
                });
            }
        } catch (Exception e) {
            Log.e(TAG, "Erro ao buscar palavra aleatória ou exibir conquista", e);
        }
    }

    private void atualizarExibicaoPalavra() {
        StringBuilder exibicaoComEspacos = new StringBuilder();
        for (int i = 0; i < progressoPalavra.length(); i++) {
            exibicaoComEspacos.append(progressoPalavra.charAt(i)).append(" ");
        }
        tvPalavraOculta.setText(exibicaoComEspacos.toString().trim());
    }

    private void gerarTecladoDinamico() {
        if (layoutTeclado == null) return;

        layoutTeclado.removeAllViews();
        String[] linhasLetras = {"ABCDEFGH", "IJKLMNOPQ", "RSTUVWXYZ"};

        LinearLayout.LayoutParams parametrosLinha = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        parametrosLinha.setMargins(0, 4, 0, 4);

        LinearLayout.LayoutParams parametrosBotao = new LinearLayout.LayoutParams(0, 140, 1.0f);
        parametrosBotao.setMargins(4, 0, 4, 0);

        for (String staticLinha : linhasLetras) {
            LinearLayout containerLinha = new LinearLayout(this);
            containerLinha.setOrientation(LinearLayout.HORIZONTAL);
            containerLinha.setLayoutParams(parametrosLinha);

            for (int i = 0; i < staticLinha.length(); i++) {
                final char letra = staticLinha.charAt(i);
                Button btnLetra = new Button(this);
                btnLetra.setText(String.valueOf(letra));
                btnLetra.setLayoutParams(parametrosBotao);
                btnLetra.setBackgroundColor(android.graphics.Color.parseColor("#E0E0E0"));
                btnLetra.setTextColor(android.graphics.Color.parseColor("#4A3780"));
                btnLetra.setTextSize(16f);

                btnLetra.setOnClickListener(v -> {
                    v.setEnabled(false);
                    v.setBackgroundColor(android.graphics.Color.parseColor("#BDBDBD"));
                    processarPalpite(letra);
                });
                containerLinha.addView(btnLetra);
            }
            layoutTeclado.addView(containerLinha);
        }
    }

    private String removerAcentos(String texto) {
        String nfdNormalizedString = Normalizer.normalize(texto, Normalizer.Form.NFD);
        Pattern pattern = Pattern.compile("\\p{InCombiningDiacriticalMarks}+");
        return pattern.matcher(nfdNormalizedString).replaceAll("");
    }

    private void processarPalpite(char letraPalpite) {
        boolean acertou = false;
        String palavraSemAcento = removerAcentos(palavraSorteada);

        for (int i = 0; i < palavraSemAcento.length(); i++) {
            if (palavraSemAcento.charAt(i) == letraPalpite) {
                progressoPalavra.setCharAt(i, palavraSorteada.charAt(i));
                acertou = true;
            }
        }

        if (acertou) {
            tocarEfeitoSonoro(somAcertoId);
            atualizarExibicaoPalavra();
            if (!progressoPalavra.toString().contains("_")) {
                pararTemporizador();
                dbHelper.marcarComoAcertada(palavraSorteada);

                pontuacaoAtual += 100;
                if (tvPontuacao != null) {
                    tvPontuacao.setText(String.format(Locale.getDefault(), "Pontuação: %d  |  Tema: %s", pontuacaoAtual, categoriaSorteada));
                }
                dbHelper.salvarOuAtualizarPontuacao(nickname, avatarId, pontuacaoAtual);
                mostrarDialogoFimDeJogo(true);
            }
        } else {
            tocarEfeitoSonoro(somErroId);
            errosCometidos++;
            atualizarImagemForca();
            if (errosCometidos >= MAX_ERROS) {
                pararTemporizador();
                mostrarDialogoFimDeJogo(false);
            }
        }
    }

    private void mostrarDialogoFimDeJogo(boolean venceu) {
        if (dialogoEstaAberto) return;
        dialogoEstaAberto = true;
        bloquearTeclado();

        com.google.android.material.dialog.MaterialAlertDialogBuilder builder =
                new com.google.android.material.dialog.MaterialAlertDialogBuilder(this);

        String mensagemResultado;
        if (venceu) {
            builder.setTitle("🏆 Sobrevivência Garantida!");
            mensagemResultado = String.format(Locale.getDefault(), "Muito bem, %s!\nPalavra: %s\nSua pontuação nesta rodada: %d", nickname, palavraSorteada, pontuacaoAtual);
        } else {
            builder.setTitle("💀 Fim da Linha...");
            mensagemResultado = String.format(Locale.getDefault(), "A forca venceu!\nPalavra oculta: %s\nSua pontuação foi zerada.", palavraSorteada);
            pontuacaoAtual = 0;
            if (tvPontuacao != null) {
                tvPontuacao.setText(String.format(Locale.getDefault(), "Pontuação: %d  |  Tema: %s", pontuacaoAtual, categoriaSorteada));
            }
        }

        LinearLayout layoutDialogo = new LinearLayout(this);
        layoutDialogo.setOrientation(LinearLayout.VERTICAL);
        layoutDialogo.setPadding(45, 30, 45, 30);

        TextView tvMensagem = new TextView(this);
        tvMensagem.setText(mensagemResultado);
        tvMensagem.setTextSize(16f);
        tvMensagem.setTextColor(android.graphics.Color.parseColor("#1C1C1E"));
        layoutDialogo.addView(tvMensagem);

        TextView tvTituloRanking = new TextView(this);
        tvTituloRanking.setText("\n📊 Classificação Geral:");
        tvTituloRanking.setTextSize(15f);
        tvTituloRanking.setTypeface(null, android.graphics.Typeface.BOLD);
        tvTituloRanking.setTextColor(android.graphics.Color.parseColor("#4A3780"));
        layoutDialogo.addView(tvTituloRanking);

        ListView listViewRanking = new ListView(this);
        List<String> listaRanking = new ArrayList<>();

        Cursor cursor = dbHelper.obterTodosOsJogadores();
        if (cursor != null) {
            int posicao = 1;
            int idxNick = cursor.getColumnIndex(BancoDadosHelper.COLUNA_JOGADOR_NICK);
            int idxRecorde = cursor.getColumnIndex(BancoDadosHelper.COLUNA_JOGADOR_RECORDE);

            while (cursor.moveToNext() && posicao <= 5) {
                if (idxNick >= 0 && idxRecorde >= 0) {
                    String nickRanking = cursor.getString(idxNick);
                    int pontosRanking = cursor.getInt(idxRecorde);
                    listaRanking.add(String.format(Locale.getDefault(), "%dº. %s — %d pts", posicao, nickRanking, pontosRanking));
                    posicao++;
                }
            }
            cursor.close();
        }

        if (listaRanking.isEmpty()) {
            listaRanking.add("Nenhum recorde registrado ainda.");
        }

        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, listaRanking);
        listViewRanking.setAdapter(adapter);

        LinearLayout.LayoutParams lpLista = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 400);
        lpLista.setMargins(0, 15, 0, 0);
        listViewRanking.setLayoutParams(lpLista);
        layoutDialogo.addView(listViewRanking);

        builder.setView(layoutDialogo);
        builder.setCancelable(false);

        builder.setPositiveButton("Próxima Rodada", (dialog, which) -> {
            dialogoEstaAberto = false;
            tempoSalvoAoPausar = -1;
            iniciarNovaPartida();
        });

        builder.setNegativeButton("Sair ao Menu", (dialog, which) -> {
            dialogoEstaAberto = false;
            finish();
        });

        if (dialogoVisivel != null && dialogoVisivel.isShowing()) {
            dialogoVisivel.dismiss();
        }

        dialogoVisivel = builder.show();
    }

    private void bloquearTeclado() {
        if (layoutTeclado == null) return;
        for (int i = 0; i < layoutTeclado.getChildCount(); i++) {
            View childLinha = layoutTeclado.getChildAt(i);
            if (childLinha instanceof LinearLayout) {
                LinearLayout layoutLinha = (LinearLayout) childLinha;
                for (int j = 0; j < layoutLinha.getChildCount(); j++) {
                    layoutLinha.getChildAt(j).setEnabled(false);
                }
            }
        }
    }

    private void atualizarImagemForca() {
        if (ivForca == null) return;

        switch (errosCometidos) {
            case 0: ivForca.setImageResource(R.drawable.forca_0); break;
            case 1: ivForca.setImageResource(R.drawable.forca_1); break;
            case 2: ivForca.setImageResource(R.drawable.forca_2); break;
            case 3: ivForca.setImageResource(R.drawable.forca_3); break;
            case 4: ivForca.setImageResource(R.drawable.forca_4); break;
            case 5: ivForca.setImageResource(R.drawable.forca_5); break;
            case 6:
            default: ivForca.setImageResource(R.drawable.forca_6); break;
        }
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            long tempoAtual = System.currentTimeMillis();
            if ((tempoAtual - ultimoTempoSensor) > 100) {
                long diferencaTempo = (tempoAtual - ultimoTempoSensor);
                ultimoTempoSensor = tempoAtual;

                float x = event.values[0];
                float y = event.values[1];
                float z = event.values[2];

                float velocidade = Math.abs(x + y + z - ultimaCoordenadaX - ultimaCoordenadaY - ultimaCoordenadaZ) / diferencaTempo * 10000;

                if (velocidade > FORCA_CHACOALHAO) {
                    Toast.makeText(this, "Chacoalhão detectado! Reiniciando partida...", Toast.LENGTH_SHORT).show();
                    tempoSalvoAoPausar = -1;
                    iniciarNovaPartida();
                }

                ultimaCoordenadaX = x;
                ultimaCoordenadaY = y;
                ultimaCoordenadaZ = z;
            }
        }

        if (event.sensor.getType() == Sensor.TYPE_PROXIMITY) {
            float distancia = event.values[0];
            if (distancia < event.sensor.getMaximumRange()) {
                Toast.makeText(this, "💡 Dica de Mestre: Foque nas vogais primeiro!", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {}

    private void verificarPermissaoNotificacao() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.POST_NOTIFICATIONS}, PERMISSAO_NOTIFICACAO_CODE);
            }
        }
    }

    private void configurarReceptorBateria() {
        receptorBateriaBaixa = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (Intent.ACTION_BATTERY_LOW.equals(intent.getAction())) {
                    dispararNotificacaoLocal();
                }
            }
        };
    }

    private void criarCanalNotificacao() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel canal = new NotificationChannel(
                    CANAL_NOTIFICACAO_ID, "Alertas do Jogo", NotificationManager.IMPORTANCE_HIGH);
            canal.setDescription("Informa problemas de bateria");

            NotificationManager gerenciador = getSystemService(NotificationManager.class);
            if (gerenciador != null) {
                gerenciador.createNotificationChannel(canal);
            }
        }
    }

    private void dispararNotificacaoLocal() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        NotificationCompat.Builder construtorNotificacao = new NotificationCompat.Builder(this, CANAL_NOTIFICACAO_ID)
                .setSmallIcon(android.R.drawable.stat_sys_warning)
                .setContentTitle("Bateria Baixa!")
                .setContentText(String.format(Locale.getDefault(), "%s, conecte o carregador para não perder o jogo!", nickname))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true);

        try {
            NotificationManagerCompat.from(this).notify(1, construtorNotificacao.build());
        } catch (SecurityException e) {
            Toast.makeText(this, "Aviso: Bateria Baixa!", Toast.LENGTH_LONG).show();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (sensorManager != null) {
            if (acelerometro != null) {
                sensorManager.registerListener(this, acelerometro, SensorManager.SENSOR_DELAY_NORMAL);
            }
            if (sensorProximidade != null) {
                sensorManager.registerListener(this, sensorProximidade, SensorManager.SENSOR_DELAY_NORMAL);
            }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            registerReceiver(receptorBateriaBaixa, new IntentFilter(Intent.ACTION_BATTERY_LOW), Context.RECEIVER_EXPORTED);
        } else {
            registerReceiver(receptorBateriaBaixa, new IntentFilter(Intent.ACTION_BATTERY_LOW));
        }

        if (mediaPlayerFundo != null && !mediaPlayerFundo.isPlaying()) {
            mediaPlayerFundo.start();
        }

        iniciarTemporizador();
    }

    @Override
    protected void onPause() {
        super.onPause();
        tempoSalvoAoPausar = tempoRestanteSegundos;
        pararTemporizador();

        if (sensorManager != null) sensorManager.unregisterListener(this);
        unregisterReceiver(receptorBateriaBaixa);

        if (mediaPlayerFundo != null && mediaPlayerFundo.isPlaying()) {
            mediaPlayerFundo.pause();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        pararTemporizador();
        if (mediaPlayerFundo != null) {
            mediaPlayerFundo.release();
            mediaPlayerFundo = null;
        }
        if (soundPoolEfeitos != null) {
            soundPoolEfeitos.release();
            soundPoolEfeitos = null;
        }
    }
}