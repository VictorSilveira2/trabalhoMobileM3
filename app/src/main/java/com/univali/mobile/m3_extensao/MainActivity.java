package com.univali.mobile.m3_extensao;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import org.json.JSONArray;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Random;

public class MainActivity extends AppCompatActivity {

    private TextView textViewAtividade, textViewCategoria;
    private String atividadeDoDia;
    private DBHelper dbHelper;

    private static final String URL_ATIVIDADES = "https://raw.githubusercontent.com/VictorSilveira2/trabalhoMobileM3/refs/heads/main/atividades.json";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        textViewAtividade = findViewById(R.id.textViewAtividade);
        textViewCategoria = findViewById(R.id.textViewCategoria);
        Button buttonConfirmar = findViewById(R.id.buttonConfirmar);
        Button buttonHistorico = findViewById(R.id.buttonHistorico);

        dbHelper = new DBHelper(this);

        atualizarCategoriaUsuario();

        buttonConfirmar.setOnClickListener(v -> {
            if (dbHelper.foiRealizadaHoje()) {
                Toast.makeText(this, R.string.atividade_ja_confirmada, Toast.LENGTH_SHORT).show();
            } else {
                dbHelper.registrarAtividade(atividadeDoDia);
                Toast.makeText(this, R.string.atividade_registrada, Toast.LENGTH_SHORT).show();
                atualizarCategoriaUsuario();
            }
        });

        buttonHistorico.setOnClickListener(v -> {
            Intent intent = new Intent(this, HistoryActivity.class);
            startActivity(intent);
        });

        createNotificationChannel();
    }

    @Override
    protected void onResume() {
        super.onResume();
        carregarAtividadeDoDia();
    }

    private void carregarAtividadeDoDia() {
        String dataHoje = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
        String dataSalva = getSharedPreferences("cache", MODE_PRIVATE).getString("atividade_data", null);
        String atividadeSalva = getSharedPreferences("cache", MODE_PRIVATE).getString("atividade_dia", null);

        if (dataHoje.equals(dataSalva) && atividadeSalva != null) {
            // Usa atividade salva se for do mesmo dia
            atividadeDoDia = atividadeSalva;
            textViewAtividade.setText(atividadeDoDia);
        } else {
            // Caso contrário, busca nova atividade do servidor (ou cache)
            new Thread(() -> {
                try {
                    JSONArray atividades = baixarAtividadesDoJson();
                    atividadeDoDia = escolherAtividadeAleatoria(atividades);

                    runOnUiThread(() -> textViewAtividade.setText(atividadeDoDia));

                    // Salva para usar no mesmo dia depois
                    getSharedPreferences("cache", MODE_PRIVATE)
                            .edit()
                            .putString("atividade_dia", atividadeDoDia)
                            .putString("atividade_data", dataHoje)
                            .apply();

                } catch (Exception e) {
                    runOnUiThread(() -> textViewAtividade.setText(getString(R.string.erro_carregar)));
                    Log.e("MainActivity", "Erro ao carregar atividade", e);
                }
            }).start();
        }
    }

    private JSONArray baixarAtividadesDoJson() throws Exception {
        try {
            URL url = new URL(URL_ATIVIDADES);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");

            InputStream inputStream = conn.getInputStream();
            String resultado = lerResposta(inputStream);

            // Salva cache local
            getSharedPreferences("cache", MODE_PRIVATE)
                    .edit()
                    .putString("json_atividades", resultado)
                    .apply();

            return new JSONArray(resultado);
        } catch (Exception e) {
            // Se falhar, tenta carregar cache local
            String cachedJson = getSharedPreferences("cache", MODE_PRIVATE)
                    .getString("json_atividades", null);

            if (cachedJson != null) {
                return new JSONArray(cachedJson);
            } else {
                throw new Exception("Erro ao baixar e nenhum JSON salvo.");
            }
        }
    }

    private String lerResposta(InputStream inputStream) throws Exception {
        BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
        StringBuilder resultado = new StringBuilder();
        String linha;
        while ((linha = reader.readLine()) != null) {
            resultado.append(linha);
        }
        return resultado.toString();
    }

    private String escolherAtividadeAleatoria(JSONArray atividades) throws Exception {
        int indice = new Random().nextInt(atividades.length());
        return atividades.getString(indice);
    }

    private void atualizarCategoriaUsuario() {
        List<String> ultimos10Dias = dbHelper.getUltimosDias(10);
        String categoria;
        int total = ultimos10Dias.size();

        if (total >= 10 && diasConsecutivos(ultimos10Dias)) {
            categoria = "Platina";
        } else if (total >= 7) {
            categoria = "Ouro";
        } else if (total >= 3) {
            categoria = "Prata";
        } else {
            categoria = "Bronze";
        }

        textViewCategoria.setText(getString(R.string.categoria_texto, categoria));
    }

    private boolean diasConsecutivos(List<String> datas) {
        if (datas.size() < 10) return false;

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        Calendar cal = Calendar.getInstance();

        for (int i = 0; i < 10; i++) {
            String esperado = sdf.format(cal.getTime());
            if (!datas.contains(esperado)) {
                return false;
            }
            cal.add(Calendar.DAY_OF_YEAR, -1);
        }
        return true;
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = "Lembretes";
            String description = "Canal de lembretes de atividades";
            int importance = NotificationManager.IMPORTANCE_DEFAULT;
            NotificationChannel channel = new NotificationChannel("atividades_channel", name, importance);
            channel.setDescription(description);
            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }
    }
}
