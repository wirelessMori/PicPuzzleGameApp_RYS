package com.example.picpuzzleslider20_rys_jrm;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;

public class MenuActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_menu);

        Button btnNormal = findViewById(R.id.btn_normal);
        Button btnHard   = findViewById(R.id.btn_hard);
        Button btnExpert = findViewById(R.id.btn_expert);

        btnNormal.setOnClickListener(v -> launchGame(3));
        btnHard.setOnClickListener(v   -> launchGame(4));
        btnExpert.setOnClickListener(v -> launchGame(5));
    }

    private void launchGame(int boardSize) {
        Intent intent = new Intent(this, MainActivity.class);
        intent.putExtra(MainActivity.EXTRA_BOARD_SIZE, boardSize);
        startActivity(intent);
    }
}
