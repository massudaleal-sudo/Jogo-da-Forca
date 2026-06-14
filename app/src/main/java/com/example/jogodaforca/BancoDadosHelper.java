package com.example.jogodaforca;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import java.util.ArrayList;
import java.util.List;

public class BancoDadosHelper extends SQLiteOpenHelper {

    private static final String NOME_BANCO = "JogoForca.db";
    private static final int VERSAO_BANCO = 1;

    public static final String TABELA_PALAVRAS = "palabras";
    public static final String COLUNA_ID = "id";
    public static final String COLUNA_PALAVRA = "palavra";
    public static final String COLUNA_CATEGORIA = "categoria";

    public BancoDadosHelper(Context context) {
        super(context, NOME_BANCO, null, VERSAO_BANCO);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        String queryCriarTabela = "CREATE TABLE " + TABELA_PALAVRAS + " ("
                + COLUNA_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                + COLUNA_PALAVRA + " TEXT NOT NULL, "
                + COLUNA_CATEGORIA + " TEXT NOT NULL);";
        db.execSQL(queryCriarTabela);

        inserirPalavraInicial(db, "ANDROID", "Tecnologia");
        inserirPalavraInicial(db, "JAVA", "Tecnologia");
        inserirPalavraInicial(db, "KOTLIN", "Tecnologia");
        inserirPalavraInicial(db, "DESENVOLVEDOR", "Profissão");
        inserirPalavraInicial(db, "BANCO", "Estrutura");
        inserirPalavraInicial(db, "FORCA", "Jogo");
        inserirPalavraInicial(db, "THREADS", "Programação");
        inserirPalavraInicial(db, "SENSORES", "Hardware");
        inserirPalavraInicial(db, "NOTIFICACAO", "Sistema");
        inserirPalavraInicial(db, "BROADCAST", "Componente");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABELA_PALAVRAS);
        onCreate(db);
    }

    private void inserirPalavraInicial(SQLiteDatabase db, String palavra, String categoria) {
        ContentValues valores = new ContentValues();
        valores.put(COLUNA_PALAVRA, palavra.toUpperCase().trim());
        valores.put(COLUNA_CATEGORIA, categoria);
        db.insert(TABELA_PALAVRAS, null, valores);
    }

    public boolean inserirNovaPalavra(String palavra, String categoria) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues valores = new ContentValues();
        valores.put(COLUNA_PALAVRA, palavra.toUpperCase().trim());
        valores.put(COLUNA_CATEGORIA, categoria);

        long resultado = db.insert(TABELA_PALAVRAS, null, valores);
        db.close();
        return resultado != -1;
    }
}