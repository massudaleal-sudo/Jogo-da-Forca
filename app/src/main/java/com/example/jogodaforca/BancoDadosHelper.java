package com.example.jogodaforca;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.text.Normalizer;
import java.util.Locale;


public class BancoDadosHelper extends SQLiteOpenHelper {

    // Configurações globais do arquivo físico do banco e sua versão atual
    private static final String NOME_BANCO = "JogoForca.db";
    private static final int VERSAO_BANCO = 6; // Incrementado para refletir mudanças estruturais

    // Definição das colunas da Tabela de Palavras
    public static final String TABELA_PALAVRAS = "palavras";
    public static final String COLUNA_ID = "id";
    public static final String COLUNA_PALAVRA = "palavra";
    public static final String COLUNA_CATEGORIA = "categoria";
    public static final String COLUNA_FOI_ACERTADA = "foi_acertada"; // Flag de estado para o fluxo de sorteio

    // Definição das colunas da Tabela de Perfis/Jogadores
    public static final String TABELA_JOGADORES = "jogadores";
    public static final String COLUNA_JOGADOR_ID = "id";
    public static final String COLUNA_JOGADOR_NICK = "nickname";
    public static final String COLUNA_JOGADOR_AVATAR = "avatar";
    public static final String COLUNA_JOGADOR_RECORDE = "pontuacao_maxima";

    // Construtor obrigatório passando o contexto da aplicação para inicializar o arquivo físico
    public BancoDadosHelper(Context context) {
        super(context, NOME_BANCO, null, VERSAO_BANCO);
    }

    /**
     * BD CRIADO DO ZERO
     */
    @Override
    public void onCreate(SQLiteDatabase db) {
        // SQL de criação da tabela de Palavras
        String queryCriarTabela = "CREATE TABLE " + TABELA_PALAVRAS + " ("
                + COLUNA_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                + COLUNA_PALAVRA + " TEXT NOT NULL, "
                + COLUNA_CATEGORIA + " TEXT NOT NULL, "
                + COLUNA_FOI_ACERTADA + " INTEGER DEFAULT 0);";
        db.execSQL(queryCriarTabela);

        // SQL de criação da tabela de Jogadores
        // PONTO DE DEFESA: Sem restrição UNIQUE no nickname para permitir múltiplos perfis com o mesmo nome
        String queryCriarTabelaJogadores = "CREATE TABLE " + TABELA_JOGADORES + " ("
                + COLUNA_JOGADOR_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                + COLUNA_JOGADOR_NICK + " TEXT NOT NULL, "
                + COLUNA_JOGADOR_AVATAR + " INTEGER NOT NULL, "
                + COLUNA_JOGADOR_RECORDE + " INTEGER DEFAULT 0);";
        db.execSQL(queryCriarTabelaJogadores);

        // Carga inicial de dados para garantir que o jogo funcione imediatamente após abrir pela primeira vez
        inserirPalavraInicial(db, "FLAMINGO", "Animais");
        inserirPalavraInicial(db, "ESQUILO", "Animais");
        inserirPalavraInicial(db, "LASANHA", "Alimentos");
        inserirPalavraInicial(db, "ESPAGUETI", "Alimentos");
        inserirPalavraInicial(db, "AMEIXA", "Alimentos");
        inserirPalavraInicial(db, "CHILE", "Países");
        inserirPalavraInicial(db, "KUWAIT", "Países");
        inserirPalavraInicial(db, "COMPUTADOR", "Objetos");
        inserirPalavraInicial(db, "GELADEIRA", "Objetos");
        inserirPalavraInicial(db, "AVATAR", "Filmes");
        inserirPalavraInicial(db, "TITANIC", "Filmes");
    }


    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABELA_PALAVRAS);
        db.execSQL("DROP TABLE IF EXISTS " + TABELA_JOGADORES);
        onCreate(db); // Recria o banco atualizado
    }


    private String padronizarTexto(String texto) {
        if (texto == null) return "";
        String emMaiusculo = texto.toUpperCase(Locale.getDefault()).trim();
        return Normalizer.normalize(emMaiusculo, Normalizer.Form.NFD); // Decompõe acentos
    }

    // Método utilitário interno para alimentar os registros no onCreate utilizando a instância ativa do banco
    private void inserirPalavraInicial(SQLiteDatabase db, String palavra, String categoria) {
        ContentValues valores = new ContentValues(); // Mapeamento do tipo Chave/Valor para colunas
        valores.put(COLUNA_PALAVRA, padronizarTexto(palavra));
        valores.put(COLUNA_CATEGORIA, categoria);
        valores.put(COLUNA_FOI_ACERTADA, 0);
        db.insert(TABELA_PALAVRAS, null, valores);
    }


    public boolean inserirNovaPalavra(String palavra, String categoria) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues valores = new ContentValues();
        valores.put(COLUNA_PALAVRA, padronizarTexto(palavra));
        valores.put(COLUNA_CATEGORIA, categoria);
        valores.put(COLUNA_FOI_ACERTADA, 0);

        long resultado = db.insert(TABELA_PALAVRAS, null, valores);
        db.close(); // Fecha a conexão de escrita para evitar vazamento de recursos (memory leak)
        return resultado != -1; // Retorna falso se houver alguma falha interna na inserção
    }


    public void marcarComoAcertada(String palavra) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues valores = new ContentValues();
        valores.put(COLUNA_FOI_ACERTADA, 1);

        db.update(TABELA_PALAVRAS, valores, COLUNA_PALAVRA + " = ?", new String[]{padronizarTexto(palavra)});
        db.close();
    }


    public void resetarTodasAsPalavras() {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues valores = new ContentValues();
        valores.put(COLUNA_FOI_ACERTADA, 0);
        db.update(TABELA_PALAVRAS, valores, null, null); // Cláusulas WHERE nulas afetam todas as linhas
        db.close();
    }

    // MÉTODOS DE GERENCIAMENTO DE PERFIS (RANKING / HISTÓRICO)


    public Cursor obterTodosOsJogadores() {
        SQLiteDatabase db = this.getReadableDatabase(); // Leitura otimizada
        return db.rawQuery("SELECT * FROM " + TABELA_JOGADORES + " ORDER BY " + COLUNA_JOGADOR_RECORDE + " DESC", null);
    }


    public int obterMaiorPontuacaoGeral() {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT MAX(" + COLUNA_JOGADOR_RECORDE + ") FROM " + TABELA_JOGADORES, null);
        int maior = 0;
        if (cursor != null && cursor.moveToFirst()) {
            maior = cursor.getInt(0);
            cursor.close();
        }
        db.close();
        return maior;
    }


    public void salvarOuAtualizarPontuacao(String nickname, int idAvatar, int pontuacaoFinal) {
        SQLiteDatabase db = this.getWritableDatabase();

        // Busca o último registro correspondente a este par de nickname e avatar
        Cursor cursor = db.rawQuery("SELECT " + COLUNA_JOGADOR_ID + ", " + COLUNA_JOGADOR_RECORDE +
                " FROM " + TABELA_JOGADORES +
                " WHERE " + COLUNA_JOGADOR_NICK + " = ? AND " + COLUNA_JOGADOR_AVATAR + " = ?" +
                " ORDER BY " + COLUNA_JOGADOR_ID + " DESC LIMIT 1", new String[]{nickname, String.valueOf(idAvatar)});

        if (cursor != null && cursor.moveToFirst()) {
            int idRegistro = cursor.getInt(0);
            int recordeAtual = cursor.getInt(1);
            cursor.close();

            // Só atualiza se a performance atual ultrapassar o recorde deste registro
            if (pontuacaoFinal > recordeAtual) {
                ContentValues valores = new ContentValues();
                valores.put(COLUNA_JOGADOR_RECORDE, pontuacaoFinal);
                db.update(TABELA_JOGADORES, valores, COLUNA_JOGADOR_ID + " = ?", new String[]{String.valueOf(idRegistro)});
            }
        } else {
            // Cria um registro inédito se o par nickname/avatar não for localizado
            if (cursor != null) cursor.close();
            ContentValues valores = new ContentValues();
            valores.put(COLUNA_JOGADOR_NICK, nickname);
            valores.put(COLUNA_JOGADOR_AVATAR, idAvatar);
            valores.put(COLUNA_JOGADOR_RECORDE, pontuacaoFinal);
            db.insert(TABELA_JOGADORES, null, valores);
        }
        db.close();
    }
}