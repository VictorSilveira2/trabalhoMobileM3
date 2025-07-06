package com.univali.mobile.m3_extensao;

import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class HistoryActivity extends AppCompatActivity {

    private DBHelper dbHelper;
    private TextView textViewTotal;
    private TextView textViewSequencia;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_history);

        dbHelper = new DBHelper(this);
        ListView listView = findViewById(R.id.listViewHistory);
        textViewTotal = findViewById(R.id.textViewTotal);
        textViewSequencia = findViewById(R.id.textViewSequencia);

        List<String> history = dbHelper.getAllActivities();

        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_list_item_1,
                history
        );
        listView.setAdapter(adapter);

        // Atualiza os contadores no topo
        atualizarInformacoes(history);
    }

    private void atualizarInformacoes(List<String> history) {
        textViewTotal.setText(getString(R.string.total_atividades, history.size()));
        textViewSequencia.setText(getString(R.string.sequencia_dias, calcularSequenciaAtual()));
    }

    private int calcularSequenciaAtual() {
        List<String> datas = dbHelper.getUltimosDias(30); // datas em formato yyyy-MM-dd
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        Calendar cal = Calendar.getInstance();

        int sequencia = 0;
        while (true) {
            String hoje = sdf.format(cal.getTime());
            if (datas.contains(hoje)) {
                sequencia++;
                cal.add(Calendar.DAY_OF_YEAR, -1);
            } else {
                break;
            }
        }
        return sequencia;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (dbHelper != null) {
            dbHelper.close();
        }
    }
}
