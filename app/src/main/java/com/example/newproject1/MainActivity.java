package com.example.newproject1;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

    private TextView txtScore;
    private TextView txtTimer;
    private TextView txtAttempts;
    private TextView txtInfo;

    private EditText editName;
    private EditText editNum;

    private Button btnSend;
    private Spinner spinnerDifficulty;

    private GuessGame game;
    private CountDownTimer countDownTimer;

    private SharedPreferences preferences;

    private String playerName;
    private int score = 0;
    private boolean gameStarted = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        txtScore = findViewById(R.id.txtScore);
        txtTimer = findViewById(R.id.txtTimer);
        txtAttempts = findViewById(R.id.txtAttempts);
        txtInfo = findViewById(R.id.txtInfo);

        editName = findViewById(R.id.editName);
        editNum = findViewById(R.id.editNum);

        btnSend = findViewById(R.id.btnSend);
        spinnerDifficulty = findViewById(R.id.spinnerDifficulty);

        preferences = getSharedPreferences("GamePrefs", MODE_PRIVATE);

        score = getIntent().getIntExtra("score", 0);
        txtScore.setText("Score: " + score);

        playerName = preferences.getString("playerName", "");

        if (!playerName.isEmpty()) {
            editName.setText(playerName);
        }

        setupDifficultySpinner();

        editNum.setEnabled(false);

        btnSend.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                handleButtonClick();
            }
        });
    }

    private void setupDifficultySpinner() {
        String[] difficulties = {
                "EASY - 1 to 50",
                "MEDIUM - 1 to 100",
                "HARD - 1 to 200"
        };

        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_item,
                difficulties
        );

        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

        spinnerDifficulty.setAdapter(adapter);
    }

    private GuessGame.Difficulty getSelectedDifficulty() {
        switch (spinnerDifficulty.getSelectedItemPosition()) {
            case 1:
                return GuessGame.Difficulty.MEDIUM;
            case 2:
                return GuessGame.Difficulty.HARD;
            default:
                return GuessGame.Difficulty.EASY;
        }
    }

    private void handleButtonClick() {
        if (!gameStarted) {
            startGame();
        } else {
            submitGuess();
        }
    }

    private void startGame() {
        playerName = editName.getText().toString().trim();

        if (playerName.isEmpty()) {
            txtInfo.setText("Enter your name");
            return;
        }

        preferences.edit()
                .putString("playerName", playerName)
                .apply();

        GuessGame.Difficulty difficulty = getSelectedDifficulty();

        game = new GuessGame(difficulty);

        gameStarted = true;

        editName.setEnabled(false);
        spinnerDifficulty.setEnabled(false);
        editNum.setEnabled(true);

        btnSend.setText("Submit");

        updateAttempts();

        txtInfo.setText("Guess the number!");

        startTimer();

        editNum.requestFocus();
    }

    private void submitGuess() {
        String input = editNum.getText().toString().trim();

        if (input.isEmpty()) {
            txtInfo.setText("Enter a number");
            return;
        }

        int guess;

        try {
            guess = Integer.parseInt(input);
        } catch (NumberFormatException e) {
            txtInfo.setText("Enter a valid number");
            editNum.setText("");
            return;
        }

        String result = game.makeGuess(guess);

        updateAttempts();

        editNum.setText("");

        if (result.startsWith("Correct")) {
            score += 1;
            txtScore.setText("Score: " + score);
            txtInfo.setText(result + " (+1 pt)");
            saveHighScore();
            endRoundWithDelay(result, true);

        } else if (game.isGameOver()) {
            txtInfo.setText(result);
            saveHighScore();
            endRoundWithDelay(result, false);
        } else {
            txtInfo.setText(result);
        }
    }

    private void updateAttempts() {
        if (game == null) {
            return;
        }

        txtAttempts.setText("Attempts: " + game.getRemainingAttempts());
    }

    private void startTimer() {
        if (countDownTimer != null) {
            countDownTimer.cancel();
        }

        long time = game.getCurrentDifficulty().getTimeSeconds() * 1000L;

        countDownTimer = new CountDownTimer(time, 1000) {

            @Override
            public void onTick(long millisUntilFinished) {
                txtTimer.setText("Time: " + (millisUntilFinished / 1000) + "s");
            }

            @Override
            public void onFinish() {
                if (!game.isGameOver()) {
                    game.setGameOver();
                    txtTimer.setText("Time: 0s");
                    txtInfo.setText("Time is up!");
                    updateAttempts();
                    saveHighScore();
                    endRoundWithDelay("Time is up!", false);
                }
            }
        }.start();
    }

    private void endRoundWithDelay(final String message, final boolean isWin) {
        editNum.setEnabled(false);
        btnSend.setEnabled(false);

        if (countDownTimer != null) {
            countDownTimer.cancel();
        }

        new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
            @Override
            public void run() {
                finishRound(message, isWin);
            }
        }, 1500);
    }

    private void finishRound(String message, boolean isWin) {
        Intent intent = new Intent(MainActivity.this, MainActivity2.class);
        intent.putExtra("score", score);
        intent.putExtra("message", message);
        intent.putExtra("isWin", isWin);
        startActivity(intent);
        finish();
    }

    private void saveHighScore() {
        int highestScore = preferences.getInt("highestScore", 0);

        if (score > highestScore) {
            preferences.edit()
                    .putInt("highestScore", score)
                    .putString("highestScoreName", playerName)
                    .apply();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (countDownTimer != null) {
            countDownTimer.cancel();
        }
    }
}