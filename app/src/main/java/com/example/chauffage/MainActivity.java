package com.example.chauffage;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.DataOutput;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

public class MainActivity extends AppCompatActivity {

    private Handler handler;
    private SharedPreferences preferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        this.handler = new Handler(Looper.getMainLooper());
        this.preferences = getSharedPreferences("data", MODE_PRIVATE);

        getData();
        postData(1, 0, 35);

        PeriodicWorkRequest request = new PeriodicWorkRequest.Builder(ChauffageWorker.class,
                PeriodicWorkRequest.MIN_PERIODIC_INTERVAL_MILLIS,
                TimeUnit.MILLISECONDS).build();
        WorkManager.getInstance(this).enqueue(request);
    }

    private void getData () {
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                String ip = "10.4.173.33";
                String port = "8080";

                // Obtenir l'information de l'objet au demarrage de l'application
                StringBuilder reponse = new StringBuilder();
                Integer code;
                try {
                    URL url = new URL("Http://" + ip + ":" + port);
                    HttpURLConnection connexion = (HttpURLConnection) url.openConnection();
                    InputStream is = new BufferedInputStream(connexion.getInputStream());
                    byte[] donnees = new byte[1024];
                    int bytesRead = 0;
                    while ((bytesRead = is.read(donnees)) != -1) {
                        reponse.append(new String(donnees, 0, bytesRead));
                    }
                    connexion.disconnect();
                    code = connexion.getResponseCode();
                } catch (IOException e) {
                    Log.e("errrrrrrrrrrrrrrrrrrrrreur", e.toString());
                    throw new RuntimeException(e);
                }
                String text = reponse.toString();
                Log.i("aaaaaaaaaaaaaaaaaa", text);
                List<Integer> valeurs = new ArrayList<>();
                for (int i = 0; i < text.length(); i++) {
                    if (text.charAt(i) == ":".toCharArray()[0]) {
                        StringBuilder sValeur = new StringBuilder();
                        for (int n = i+2; n < text.length(); n++) {
                            if (text.charAt(n) == ",".toCharArray()[0] || text.charAt(n) == "}".toCharArray()[0]) {
                                break;
                            } else {
                                sValeur.append(text.charAt(n));
                            }
                        }
                        valeurs.add(Integer.valueOf(sValeur.toString()));
                    }
                }
                // Print current character
                for (Integer i : valeurs) {
                    Log.i("aaaaaaaaaaaaaaaaaa", i.toString());
                }
                TextView textView = findViewById(R.id.textViewIntensiteValeur);
                handler.post(() -> textView.setText("2"));

                if (code == 200 && valeurs.size() == 3) {
                    SharedPreferences.Editor editor = preferences.edit();
                    editor.putInt("chauffage", valeurs.get(0));
                    editor.putInt("ac", valeurs.get(1));
                    editor.putFloat("chauffage", valeurs.get(2));
                }
            }
        });
        thread.start();
    }

    private void postData(Integer chauffage, Integer ac, Integer intensite){
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                String ip = "10.4.173.33";
                String port = "8080";

                try {
                    URL url = new URL("Http://" + ip + ":" + port);
                    HttpURLConnection connexion = (HttpURLConnection) url.openConnection();
                    connexion.setRequestMethod("POST");
                    connexion.setRequestProperty("Content-Type", "application/json;charset=UTF-8");
                    connexion.setRequestProperty("Accept", "html/text");
                    connexion.setDoOutput(true);
                    connexion.setDoInput(true);
                    DataOutputStream os = new DataOutputStream(connexion.getOutputStream());
                    os.writeBytes("{\"chauffage\": " + chauffage + ", \"ac\": " + ac + ", \"intensite\": " + intensite + "}");
                    os.flush();
                    os.close();
                    Log.i("bbbbbbbbbbbbbb", String.valueOf(connexion.getResponseCode()));
                    Log.i("bbbbbbbbbbbbbb", connexion.getResponseMessage());

                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        });
        thread.start();
    }
}