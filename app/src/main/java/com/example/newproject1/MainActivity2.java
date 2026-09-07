package com.example.newproject1;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

public class MainActivity2 extends AppCompatActivity {

    private TextView txtResult;
    private TextView txtGameOverReason;
    private TextView txtHighScore;

    private Button btnReset;
    private Button btnClose;

    private SharedPreferences preferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main2);

        txtResult = findViewById(R.id.txtResult);
        txtGameOverReason = findViewById(R.id.txtGameOverReason);
        txtHighScore = findViewById(R.id.txtHighScore);

        btnReset = findViewById(R.id.btnReset);
        btnClose = findViewById(R.id.btnClose);

        final int score = getIntent().getIntExtra("score", 0);
        String message = getIntent().getStringExtra("message");
        final boolean isWin = getIntent().getBooleanExtra("isWin", false);

        txtResult.setText("Score: " + score);

        if (message != null && !message.isEmpty()) {
            txtGameOverReason.setText(message);
        } else {
            txtGameOverReason.setVisibility(View.GONE);
        }

        if (isWin) {
            btnReset.setText("Next Round");
        } else {
            btnReset.setText("Play Again");
        }

        preferences = getSharedPreferences("GamePrefs", MODE_PRIVATE);

        int highestScore = preferences.getInt("highestScore", 0);
        String highestScoreName = preferences.getString("highestScoreName", "Nobody");

        txtHighScore.setText(
                "Highest Score: " + highestScore + "\nBy: " + highestScoreName
        );

        btnReset.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity2.this, MainActivity.class);
                intent.putExtra("score", isWin ? score : 0);
                startActivity(intent);
                finish();
            }
        });

        btnClose.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finishAffinity();
            }
        });
    }
}