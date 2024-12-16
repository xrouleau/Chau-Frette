package com.example.chauffage;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import androidx.appcompat.app.AppCompatActivity;

public class ServeurActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_serveur);

        // récupère les edit text de mon activité
        EditText ETIp = findViewById(R.id.editTextIP);
        EditText ETPort = findViewById(R.id.editTextPort);

        // Méthode qui détecte et traite un click sur lè boutton retour
        Button btnRetour = findViewById(R.id.buttonRetour);
        btnRetour.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // Lance l'activité main
                mainActivity();
            }
        });

        // Méthode qui détecte et traite un click sur lè boutton sauvegarder
        Button btnSauvegarder = findViewById(R.id.buttonSauvegarder);
        btnSauvegarder.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // Enregistre les valeurs des 2 editText et lance l'activité main
                SharedPreferences preferences = getSharedPreferences("data", MODE_PRIVATE);
                SharedPreferences.Editor editor = preferences.edit();
                editor.putString("ip", ETIp.getText().toString());
                editor.putString("port", ETPort.getText().toString());
                editor.apply();
                mainActivity();
            }
        });
    }

    /***
     * Lance l'activité main
     */
    private void mainActivity() {
        Intent intent = new Intent(ServeurActivity.this, MainActivity.class);
        startActivity(intent);
    }
}