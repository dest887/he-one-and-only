package com.example.newproject1;

import android.content.Intent;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import java.util.Random;

public class MainActivity extends AppCompatActivity {

    private TextView txtScore;
    private TextView txtTimer;
    private EditText editNum;
    private Button btnSend;
    private TextView txtInfo;

    private int num;
    private int score = 0;
    private int wrongCount = 0;
    private final Random rand = new Random();
    private CountDownTimer countDownTimer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        txtScore = findViewById(R.id.txtScore);
        txtTimer = findViewById(R.id.txtTimer);
        editNum = findViewById(R.id.editNum);
        btnSend = findViewById(R.id.btnSend);
        txtInfo = findViewById(R.id.txtInfo);

        score = getIntent().getIntExtra("score", 0);
        txtScore.setText("Score: " + score);

        makeNum();
        startTimer();

        btnSend.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String input = editNum.getText().toString().trim();

                if (input.isEmpty()) {
                    txtInfo.setText("Enter a number");
                    return;
                }

                int guess;
                try {
                    guess = Integer.parseInt(input);
                } catch (Exception e) {
                    txtInfo.setText("Error");
                    editNum.setText("");
                    return;
                }

                if (guess < 1 || guess > 20) {
                    txtInfo.setText("Enter 1 to 20");
                } else if (guess < num) {
                    wrongCount++;
                    if (wrongCount >= 5) {
                        makeNum();
                        wrongCount = 0;
                        score = Math.max(0, score - 1);
                        txtScore.setText("Score: " + score);
                        txtInfo.setText("5 wrong tries! -1 Score & New number.");
                    } else {
                        txtInfo.setText("Higher (Wrong: " + wrongCount + "/5)");
                    }
                } else if (guess > num) {
                    wrongCount++;
                    if (wrongCount >= 5) {
                        makeNum();
                        wrongCount = 0;
                        score = Math.max(0, score - 1);
                        txtScore.setText("Score: " + score);
                        txtInfo.setText("5 wrong tries! -1 Score & New number.");
                    } else {
                        txtInfo.setText("Lower (Wrong: " + wrongCount + "/5)");
                    }
                } else {
                    score += 2;
                    txtScore.setText("Score: " + score);
                    wrongCount = 0;
                    makeNum();
                    txtInfo.setText("Correct! New number generated.");
                }

                editNum.setText("");
            }
        });
    }

    private void makeNum() {
        num = rand.nextInt(20) + 1;
    }

    private void startTimer() {
        if (countDownTimer != null) {
            countDownTimer.cancel();
        }

        countDownTimer = new CountDownTimer(30000, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                txtTimer.setText("Time: " + (millisUntilFinished / 1000) + "s");
            }

            @Override
            public void onFinish() {
                score = Math.max(0, score - 1);
                Intent intent = new Intent(MainActivity.this, MainActivity2.class);
                intent.putExtra("score", score);
                startActivity(intent);
                finish();
            }
        }.start();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (countDownTimer != null) {
            countDownTimer.cancel();
        }
    }
}