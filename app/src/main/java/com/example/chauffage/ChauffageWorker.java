package com.example.chauffage;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Objects;


public class ChauffageWorker extends Worker {
    private Handler handler;
    private Context context;
    private SharedPreferences preferences;
    String idCanal = "C1";
    CharSequence nomCanal = "Canal1";
    String descriptionCanal = "Canal de notification";
    int niveauImportance = NotificationManager.IMPORTANCE_DEFAULT;
    public ChauffageWorker(
            @NonNull Context context,
            @NonNull WorkerParameters parameters) {
        super(context, parameters);
        this.handler = new Handler(Looper.getMainLooper());
        this.context = context;
        this.preferences = context.getSharedPreferences("data", Context.MODE_PRIVATE);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel canal = new NotificationChannel(idCanal, nomCanal, niveauImportance);
            canal.setDescription(descriptionCanal);
            NotificationManager manager = context.getSystemService(NotificationManager.class);
            manager.createNotificationChannel(canal);
        }
    }

    @NonNull
    @Override
    public Result doWork() {

        long currentTimeMillis = System.currentTimeMillis();
        Date date = new Date(currentTimeMillis);
        SimpleDateFormat formatter = new SimpleDateFormat("HH:mm:ss");
        String time = formatter.format(date);
        Log.i("Worker", time);
        Log.i("Shared chau do", String.valueOf(preferences.getInt("chauffage", 3)));
        Log.i("Shared ac do", String.valueOf(preferences.getInt("ac", 3)));
        Log.i("Shared int do", String.valueOf(preferences.getInt("intensite", 3)));


        if (preferences.getBoolean("notifications", false)) {
            if (ContextCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.POST_NOTIFICATIONS)
                    == PackageManager.PERMISSION_GRANTED) {
                Log.i("GET", "GET");
                getData();
                return Result.success();
            } else {
                return Result.failure();
            }
        }
        return Result.failure();
    }

    private void envoyerNotification(String titre) {
        Log.i("Notification", "début");


        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Log.i("Notification", "milieu");
            NotificationManager notificationManager = (NotificationManager) getApplicationContext().getSystemService(Context.NOTIFICATION_SERVICE);
            // Créer un canal de notification
            NotificationChannel canal = new NotificationChannel(idCanal,
                    nomCanal, NotificationManager.IMPORTANCE_DEFAULT);
            notificationManager.createNotificationChannel(canal);
            // Envoyer la notification
             Notification notification = new Notification.Builder(getApplicationContext(), idCanal)
                    .setSmallIcon(android.R.drawable.star_on)
                    .setContentTitle(titre)
                    .setContentText("Modification des réglages du thermostate")
                    .build();
            notificationManager.notify(1, notification);
            Log.i("Notification", "fin");
        }
    }

    private void getData() {
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                Log.i("GET", "DEBUT");
                String ip = preferences.getString("ip", "");
                String port = preferences.getString("port", "");

                // Obtenir l'information de l'objet
                StringBuilder reponse = new StringBuilder();
                try {
                    // Établir la connexion
                    URL url = new URL("Http://" + ip + ":" + port + "/get");
                    HttpURLConnection connexion = (HttpURLConnection) url.openConnection();

                    // Au cas ou la connection échoue ou la requète
                    connexion.setConnectTimeout(4000);
                    connexion.setReadTimeout(4000);
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
                    // Notification connexion interompue
                    Log.i("Appel", "erreur");
                    envoyerNotification("Échec de connexion au thermostate");
                }

                // Obtenir les valeurs du Json
                Log.i("GET", "MILIEU");
                String text = reponse.toString();
                List<Integer> valeurs = new ArrayList<>();
                for (int i = 0; i < text.length(); i++) {
                    if (text.charAt(i) == ":".toCharArray()[0]) {
                        StringBuilder sValeur = new StringBuilder();
                        for (int n = i + 2; n < text.length(); n++) {
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
                Log.i("Shared chau", String.valueOf(preferences.getInt("chauffage", 3)));
                Log.i("Shared ac", String.valueOf(preferences.getInt("ac", 3)));
                Log.i("Shared int", String.valueOf(preferences.getInt("intensite", 3)));

                Log.i("GET chau", String.valueOf(valeurs.get(0)));
                Log.i("GET ac", String.valueOf(valeurs.get(1)));
                Log.i("GET int", String.valueOf(valeurs.get(2)));

                // Si toutes les valeurs ont bien été trouvé

                String titre = "";
                if (valeurs.size() == 3) {
                    SharedPreferences.Editor editor = preferences.edit();
                    if (valeurs.get(0) != preferences.getInt("chauffage", 0)) {
                        if (Objects.equals(valeurs.get(0), valeurs.get(1))) {
                            Log.i("Appel", "ferme");
                            titre = "Le thermostate est éteint";
                            editor.putInt("chauffage", valeurs.get(0));
                        } else if (valeurs.get(0) == 1) {
                            Log.i("Appel", "chauffage");
                            titre = "Le chauffage est allumé à " + valeurs.get(2) + "%";
                            editor.putInt("chauffage", valeurs.get(0));
                        }
                    } else if (valeurs.get(2) != preferences.getInt("intensite", 0)) {
                        Log.i("Appel", "intensite");
                        titre = "Le thermostate à été réglé à " + valeurs.get(2) + "%";
                        editor.putInt("intensite", valeurs.get(2));
                    }
                    if (valeurs.get(1) != preferences.getInt("ac", 0)) {
                        if (Objects.equals(valeurs.get(0), valeurs.get(1))) {
                            Log.i("Appel", "ferme");
                            titre = "Le thermostate est éteint";
                            editor.putInt("ac", valeurs.get(1));
                        } else if (valeurs.get(1) == 1) {
                            Log.i("Appel", "ac");
                            titre = "L'air climatisé est allumé à " + valeurs.get(2) + "%";
                            editor.putInt("ac", valeurs.get(1));
                        }
                    }
                    editor.apply();
                    if (!titre.isEmpty()) {
                        envoyerNotification(titre);
                    }
                    Log.i("GET", "FIN");
                }
            }
        });
        thread.start();
    }
}
