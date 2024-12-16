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
    private SharedPreferences preferences;
    String idCanal = "C1";
    CharSequence nomCanal = "Canal1";
    String descriptionCanal = "Canal de notification";
    public ChauffageWorker(
            @NonNull Context context,
            @NonNull WorkerParameters parameters) {
        super(context, parameters);
        this.preferences = context.getSharedPreferences("data", Context.MODE_PRIVATE);
    }

    @NonNull
    @Override
    public Result doWork() {
        Log.i("AAAAAAAAAAAAAAA", "Methode doWork");
        // Affiche l'heure à laquelle le Worker a travaillé
        long currentTimeMillis = System.currentTimeMillis();
        Date date = new Date(currentTimeMillis);
        SimpleDateFormat formatter = new SimpleDateFormat("HH:mm:ss");
        String time = formatter.format(date);
        Log.i("Worker", time);

        // Vérifie si les notifications sont activées et si les permissions sont accordées
        if (preferences.getBoolean("notifications", false)) {
            if (ContextCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.POST_NOTIFICATIONS)
                    == PackageManager.PERMISSION_GRANTED) {
                // get les données actuelles du serveur
                getData();
                Log.i("AAAAAAAAAAAAAAA", "work reussi");
                return Result.success();
            } else {
                Log.i("AAAAAAAAAAAAAAA", "work rate");
                return Result.failure();
            }
        }
        Log.i("AAAAAAAAAAAAAAA", "work rate");

        return Result.failure();
    }

    private void envoyerNotification(String titre) {
        Log.i("AAAAAAAAAAAAAAA", "envoyer notification");

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationManager notificationManager = (NotificationManager) getApplicationContext().getSystemService(Context.NOTIFICATION_SERVICE);
            // Créer un canal de notification
            NotificationChannel canal = new NotificationChannel(idCanal,
                    nomCanal, NotificationManager.IMPORTANCE_DEFAULT);
            canal.setDescription(descriptionCanal);
            notificationManager.createNotificationChannel(canal);
            // Envoyer la notification
            Notification notification = new Notification.Builder(getApplicationContext(), idCanal)
                    .setSmallIcon(android.R.drawable.star_on)
                    .setContentTitle(titre)
                    .setContentText("Modification des réglages du thermostate")
                    .build();
            notificationManager.notify(1, notification);
        }
    }

    private void getData() {
        Log.i("AAAAAAAAAAAAAAA", "work get");
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                // Obtiens la dernière valeur du ip et du port
                String ip = preferences.getString("ip", "");
                String port = preferences.getString("port", "");

                // Obtenir l'information de l'objet
                StringBuilder reponse = new StringBuilder();
                try {
                    // Établir la connexion
                    URL url = new URL("Http://" + ip + ":" + port + "/get");
                    HttpURLConnection connexion = (HttpURLConnection) url.openConnection();

                    // Au cas ou la connection ou la requète échoue
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
                    envoyerNotification("Échec de connexion au thermostate");
                }

                // Obtenir les valeurs du Json
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

                // Si toutes les valeurs ont bien été trouvé
                String titre = "";
                if (valeurs.size() == 3) {
                    // Si les valeurs ont changé, enregistre les nouvelles valeurs dans les sharedprefs et envoie une notification
                    SharedPreferences.Editor editor = preferences.edit();
                    if (valeurs.get(0) != preferences.getInt("chauffage", 0)) {
                        if (Objects.equals(valeurs.get(0), valeurs.get(1))) {
                            titre = "Le thermostate est éteint";
                            editor.putInt("chauffage", valeurs.get(0));
                        } else if (valeurs.get(0) == 1) {
                            titre = "Le chauffage est allumé à " + valeurs.get(2) + "%";
                            editor.putInt("chauffage", valeurs.get(0));
                        }
                    } else if (valeurs.get(2) != preferences.getInt("intensite", 0)) {
                        titre = "Le thermostate à été réglé à " + valeurs.get(2) + "%";
                        editor.putInt("intensite", valeurs.get(2));
                    }
                    if (valeurs.get(1) != preferences.getInt("ac", 0)) {
                        if (Objects.equals(valeurs.get(0), valeurs.get(1))) {
                            titre = "Le thermostate est éteint";
                            editor.putInt("ac", valeurs.get(1));
                        } else if (valeurs.get(1) == 1) {
                            titre = "L'air climatisé est allumé à " + valeurs.get(2) + "%";
                            editor.putInt("ac", valeurs.get(1));
                        }
                    }
                    editor.apply();
                    if (!titre.isEmpty()) {
                        envoyerNotification(titre);
                    }
                }
            }
        });
        thread.start();
    }
}