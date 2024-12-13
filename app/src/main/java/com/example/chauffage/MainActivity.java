package com.example.chauffage;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.concurrent.TimeUnit;

public class MainActivity extends AppCompatActivity {

    private Handler handler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        this.handler = new Handler(Looper.getMainLooper());

        getData();

        PeriodicWorkRequest request = new PeriodicWorkRequest.Builder(ChauffageWorker.class,
                PeriodicWorkRequest.MIN_PERIODIC_INTERVAL_MILLIS,
                TimeUnit.MILLISECONDS).build();
        WorkManager.getInstance(this).enqueue(request);
    }

    private void getData () {
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                String ip = "10.4.129.34";
                String port = "1234";

                // Obtenir l'information de l'objet au demarrage de l'application
                StringBuilder reponse = new StringBuilder();
                try {
//                  URL url = new URL("Http://" + ip + ":" + port + "/");
                    URL url = new URL("https://www.google.com/");
                    HttpURLConnection connexion = (HttpURLConnection) url.openConnection();
                    InputStream is = new BufferedInputStream(connexion.getInputStream());
                    byte[] donnees = new byte[1024];
//                    TextView textView = findViewById(R.id.textViewIntensiteValeur);
//                    handler.post(() -> textView.setText("2"));
                    int bytesRead = 0;
                    while ((bytesRead = is.read(donnees)) != -1) {
                        reponse.append(new String(donnees, 0, bytesRead));
                    }
                    connexion.disconnect();
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
                Log.i("aaaaaaaaaaaaaaaaaa", reponse.toString());
            }
        });
        thread.start();
    }
}