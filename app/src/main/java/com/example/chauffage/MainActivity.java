package com.example.chauffage;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts.RequestPermission;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;

import java.io.BufferedInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class MainActivity extends AppCompatActivity {
    private final String NOM_WORKER = "ChauFretteWorker";
    private Handler handler;
    private SharedPreferences preferences;
    private boolean shouldRefresh = true;

    @RequiresApi(api = Build.VERSION_CODES.TIRAMISU)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        this.handler = new Handler(Looper.getMainLooper());
        this.preferences = getSharedPreferences("data", MODE_PRIVATE);

        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
            // Si la permission n'est pas accordée, demander la permission
            requestPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS);
        } else {
            // Si la permission est déjà accordée, vous pouvez démarrer le worker
            PeriodicWorkRequest request = new PeriodicWorkRequest.Builder(ChauffageWorker.class,
                    15,
                    TimeUnit.MINUTES).build();
            WorkManager.getInstance(this).enqueueUniquePeriodicWork(NOM_WORKER, ExistingPeriodicWorkPolicy.CANCEL_AND_REENQUEUE, request);
        }


        @SuppressLint("UseSwitchCompatOrMaterialCode") Switch sNotif = findViewById(R.id.switchNotif);
        sNotif.setChecked(preferences.getBoolean("notifications", false));
        sNotif.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                SharedPreferences.Editor editor = preferences.edit();
                editor.putBoolean("notifications", sNotif.isChecked());
                editor.apply();
            }
        });

        @SuppressLint("UseSwitchCompatOrMaterialCode") Switch sChauffage = findViewById(R.id.switchChauffage);
        @SuppressLint("UseSwitchCompatOrMaterialCode") Switch sAC = findViewById(R.id.switchAC);
        Button enregistrer = findViewById(R.id.buttonIntensite);
        EditText editTextIntensite = findViewById(R.id.editTextNumber);
        TextView textViewIntensite = findViewById(R.id.textViewIntensiteValeur);
        sChauffage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (sChauffage.isChecked()) {
                    sAC.setChecked(false);
                }
                SharedPreferences.Editor editor = preferences.edit();
                editor.putInt("chauffage", sChauffage.isChecked() ? 1 : 0);
                editor.putInt("ac", 0);
                if (sAC.isChecked() == sChauffage.isChecked()) {
                    editor.putInt("intensite", 0);
                    textViewIntensite.setText("0");
                }
                if (sChauffage.isChecked() && preferences.getInt("intensite", 0) == 0) {
                    editor.putInt("intensite", 5);
                    textViewIntensite.setText("5");
                }
                editor.apply();
                postData();
            }
        });

        sAC.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (sAC.isChecked()) {
                    sChauffage.setChecked(false);
                }
                SharedPreferences.Editor editor = preferences.edit();
                editor.putInt("ac", sAC.isChecked() ? 1 : 0);
                editor.putInt("chauffage", 0);
                if (sAC.isChecked() == sChauffage.isChecked()) {
                    editor.putInt("intensite", 0);
                    textViewIntensite.setText("0");
                }
                if (sAC.isChecked() && preferences.getInt("intensite", 0) == 0) {
                    editor.putInt("intensite", 5);
                    textViewIntensite.setText("5");
                }
                editor.apply();
                postData();
            }
        });


        enregistrer.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                try {
                    int intensite = Integer.parseInt(editTextIntensite.getText().toString());
                    editTextIntensite.setText("");
                    SharedPreferences.Editor editor = preferences.edit();
                    if (intensite <= 0 || intensite > 100) {
                        throw new Exception();
                    } else if (intensite == 0) {
                        editor.putInt("chauffage", 0);
                        editor.putInt("ac", 0);
                        sAC.setChecked(false);
                        sChauffage.setChecked(false);
                    }
                    if (sAC.isChecked() != sChauffage.isChecked()) {
                        editor.putInt("intensite", intensite);
                        textViewIntensite.setText(String.valueOf(intensite));
                        editor.apply();
                    } else {
                        Toast.makeText(getApplicationContext(), "Il faut d'abord activer un mode", Toast.LENGTH_SHORT).show();
                    }
                    postData();
                } catch (NumberFormatException e) {
                    Toast.makeText(getApplicationContext(), "Doit être un nombre", Toast.LENGTH_SHORT).show();
                } catch (Exception e) {
                    Toast.makeText(getApplicationContext(), "Doit être entre 1 et 100 inclusivement", Toast.LENGTH_SHORT).show();
                }
            }
        });

        Button serveur = findViewById(R.id.buttonServeur);
        serveur.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                serveurActivity();
            }
        });
    }

    private final ActivityResultLauncher<String> requestPermissionLauncher =
            registerForActivityResult(new RequestPermission(), isGranted -> {
                if (isGranted) {
                    // Si la permission est accordée, démarrer le worker
                    PeriodicWorkRequest request = new PeriodicWorkRequest.Builder(ChauffageWorker.class,
                            PeriodicWorkRequest.MIN_PERIODIC_INTERVAL_MILLIS,
                            TimeUnit.MILLISECONDS).build();
                    WorkManager.getInstance(this).enqueue(request);
                } else {
                    // Si la permission est refusée, afficher un message
                    Toast.makeText(this, "Permission denied. Cannot send notifications.", Toast.LENGTH_SHORT).show();
                }
            });

    @Override
    protected void onResume() {
        super.onResume();
        refresh();
        shouldRefresh = true;
    }

    private void getData () {
        Thread thread = new Thread(new Runnable() {
            @SuppressLint("UseSwitchCompatOrMaterialCode")
            @Override
            public void run() {

                String ip = preferences.getString("ip", "");
                String port = preferences.getString("port", "");

                // Obtenir l'information de l'objet
                StringBuilder reponse = new StringBuilder();
                try {
                    // Établir la connexion
                    URL url = new URL("Http://" + ip + ":" + port + "/get");
                    HttpURLConnection connexion = (HttpURLConnection) url.openConnection();

                    // Au cas ou la connection échoue ou la requète
                    connexion.setConnectTimeout(2000);
                    connexion.setReadTimeout(2000);
                    if (connexion.getResponseCode() >= 300) {
                        throw new IOException();
                    }

                    // Si la connexion réussi lire les données reçues
                    InputStream is = new BufferedInputStream(connexion.getInputStream());
                    byte[] donnees = new byte[1024];
                    int bytesRead = 0;
                    while ((bytesRead = is.read(donnees)) != -1) {
                        reponse.append(new String(donnees, 0, bytesRead));
                    }
                    connexion.disconnect();
                } catch (IOException e) {
                    // Si la connexion échoue lancer l'activité serveur
                    handler.post(() -> Toast.makeText(getApplicationContext(), "Connexion au serveur impossible", Toast.LENGTH_LONG).show());
                    handler.post(() -> serveurActivity());
                }

                // Obtenir les valeurs du Json
                String text = reponse.toString();
                List<Integer> valeurs = new ArrayList<>();
                for (int i = 0; i < text.length(); i++) {
                    if (text.charAt(i) == ":".toCharArray()[0]) {
                        StringBuilder sValeur = new StringBuilder();
                        for (int n = i+2; n < text.length(); n++) {
                            if (Character.isDigit(text.charAt(n))) {
                                sValeur.append(text.charAt(n));
                            } else {
                                break;
                            }
                        }
                        try {
                            valeurs.add(Integer.valueOf(sValeur.toString()));
                        } catch (NumberFormatException e) {
                            Log.e("ERREUR", "Pas un int");
                        }
                    }
                }

                // Si toutes les valeurs ont bien été trouvé
                if (valeurs.size() == 3) {
                    // Stock les valeurs dans les shared prefs pour y avoir accès dans toutes les activités
                    SharedPreferences.Editor editor = preferences.edit();
                    editor.putInt("chauffage", valeurs.get(0));
                    editor.putInt("ac", valeurs.get(1));
                    editor.putInt("intensite", valeurs.get(2));
                    editor.apply();

                    // Afficher les valeurs sur l'application
                    Switch chauffage = findViewById(R.id.switchChauffage);
                    Switch ac = findViewById(R.id.switchAC);
                    TextView affichageIntensite = findViewById(R.id.textViewIntensiteValeur);
                    try {
                        handler.post(() -> chauffage.setChecked(valeurs.get(0) == 1));
                        handler.post(() -> ac.setChecked(valeurs.get(1) == 1));
                        handler.post(() -> affichageIntensite.setText(String.valueOf(valeurs.get(2))));
                    } catch (NullPointerException e) {
                        Log.w("Attention", "Pu de le meme activity");
                    }
                }
            }
        });
        thread.start();
    }

    private void postData(){
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                String ip = preferences.getString("ip", "");
                String port = preferences.getString("port", "");

                try {
                    // Établir la connexion
                    URL url = new URL("Http://" + ip + ":" + port + "/post");
                    HttpURLConnection connexion = (HttpURLConnection) url.openConnection();

                    // Au cas ou la connection échoue ou la requète
                    connexion.setConnectTimeout(4000);
                    connexion.setReadTimeout(4000);

                    // Si la connexion réussi envoyer la requête
                    connexion.setRequestMethod("POST");
                    connexion.setRequestProperty("Content-Type", "application/json;charset=UTF-8");
                    connexion.setRequestProperty("Accept", "html/text");
                    connexion.setDoOutput(true);
                    connexion.setDoInput(true);
                    DataOutputStream os = new DataOutputStream(connexion.getOutputStream());
                    Log.e("ICI", "ICI");
                    os.writeBytes("{\"chauffage\": " + preferences.getInt("chauffage", 0) + ", \"ac\": " + preferences.getInt("ac", 0) + ", \"intensite\": " + preferences.getInt("intensite", 0) + "}");
                    Log.e("ICI", "ICI");
                    os.flush();
                    Log.e("ICI", "ICI");
                    os.close();
                    Log.e("ICI", "ICI");
                    if (connexion.getResponseCode() >= 300) {
                        Log.e("ICI", String.valueOf(connexion.getResponseCode()));
                        throw new IOException();
                    }
                } catch (IOException e) {
                    // Si la connexion échoue lancer l'activité serveur
                    Log.e("ICI", "erreur");
                    handler.post(() -> Toast.makeText(getApplicationContext(), "Connexion au serveur impossible", Toast.LENGTH_LONG).show());
                    handler.post(() -> serveurActivity());
                }
            }
        });
        thread.start();
    }

    private void refresh() {
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                while (shouldRefresh) {
                    getData();
                    try {
                        Thread.sleep(10000);
                    } catch (InterruptedException e) {
                        Log.e("ERREUR", "Thread.sleep");
                    }
                }

            }
        });
        thread.start();
    }

    private void serveurActivity() {
        Intent intent = new Intent(MainActivity.this, ServeurActivity.class);
        startActivity(intent);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        shouldRefresh = false;
    }

    @Override
    protected void onStop() {
        super.onStop();
        shouldRefresh = false;
    }

    @Override
    protected void onPause() {
        super.onPause();
        shouldRefresh = false;
    }
}