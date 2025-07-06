package com.univali.mobile.m3_extensao;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class DBHelper extends SQLiteOpenHelper {

    private static final String DB_NAME = "atividade_diaria.db";
    private static final int DB_VERSION = 1;

    public DBHelper(Context context) {
        super(context, DB_NAME, null, DB_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        String CREATE_TABLE = "CREATE TABLE atividades (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "descricao TEXT," +
                "data_hora TEXT)";
        db.execSQL(CREATE_TABLE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldV, int newV) {
        db.execSQL("DROP TABLE IF EXISTS atividades");
        onCreate(db);
    }

    public void salvarAtividade(String descricao) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        String dataHora = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(new Date());
        values.put("descricao", descricao);
        values.put("data_hora", dataHora);
        db.insert("atividades", null, values);
        db.close();
    }

    public void registrarAtividade(String descricao) {
        salvarAtividade(descricao); // reuso
    }

    public boolean foiRealizadaHoje() {
        SQLiteDatabase db = this.getReadableDatabase();
        String dataHoje = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());

        Cursor cursor = db.rawQuery(
                "SELECT COUNT(*) FROM atividades WHERE data_hora LIKE ?",
                new String[]{dataHoje + "%"}
        );
        boolean resultado = false;
        if (cursor.moveToFirst()) {
            resultado = cursor.getInt(0) > 0;
        }

        cursor.close();
        db.close();
        return resultado;
    }

    public List<String> getUltimosDias(int dias) {
        List<String> datas = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();

        String query = "SELECT DISTINCT substr(data_hora, 1, 10) as data FROM atividades " +
                "ORDER BY data DESC LIMIT ?";
        Cursor cursor = db.rawQuery(query, new String[]{String.valueOf(dias)});

        if (cursor.moveToFirst()) {
            do {
                datas.add(cursor.getString(0));
            } while (cursor.moveToNext());
        }

        cursor.close();
        db.close();
        return datas;
    }

    public List<String> getAllActivities() {
        List<String> lista = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();

        Cursor cursor = db.rawQuery("SELECT * FROM atividades ORDER BY data_hora DESC", null);
        if (cursor.moveToFirst()) {
            do {
                String descricao = cursor.getString(cursor.getColumnIndexOrThrow("descricao"));
                String dataHora = cursor.getString(cursor.getColumnIndexOrThrow("data_hora"));
                lista.add(dataHora + " - " + descricao);
            } while (cursor.moveToNext());
        }

        cursor.close();
        db.close();
        return lista;
    }
}
