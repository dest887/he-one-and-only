package com.example.newproject1;

import java.util.Random;

public class GuessGame {

    public enum Difficulty {
        EASY(50, 10, 30),
        MEDIUM(100, 7, 20),
        HARD(200, 5, 10);

        private final int maxNumber;
        private final int maxAttempts;
        private final int timeSeconds;

        Difficulty(int maxNumber, int maxAttempts, int timeSeconds) {
            this.maxNumber = maxNumber;
            this.maxAttempts = maxAttempts;
            this.timeSeconds = timeSeconds;
        }

        public int getMaxNumber() {
            return maxNumber;
        }

        public int getMaxAttempts() {
            return maxAttempts;
        }

        public int getTimeSeconds() {
            return timeSeconds;
        }
    }

    private int targetNumber;
    private int remainingAttempts;
    private boolean gameOver;
    private Difficulty currentDifficulty;

    private final Random random = new Random();

    public GuessGame(Difficulty difficulty) {
        resetGame(difficulty);
    }

    public void resetGame(Difficulty difficulty) {
        currentDifficulty = difficulty;
        targetNumber = random.nextInt(difficulty.getMaxNumber()) + 1;
        remainingAttempts = difficulty.getMaxAttempts();
        gameOver = false;
    }

    public String makeGuess(int userGuess) {
        if (gameOver) {
            return "Game over!";
        }

        if (userGuess < 1 || userGuess > currentDifficulty.getMaxNumber()) {
            return "Enter a number between 1 and " + currentDifficulty.getMaxNumber();
        }

        remainingAttempts--;

        if (userGuess == targetNumber) {
            gameOver = true;
            return "Correct! The number was " + targetNumber;
        }

        if (remainingAttempts <= 0) {
            gameOver = true;
            return "Game Over! The number was " + targetNumber;
        }

        if (userGuess < targetNumber) {
            return "Higher!";
        }

        return "Lower!";
    }

    public int getRemainingAttempts() {
        return remainingAttempts;
    }

    public boolean isGameOver() {
        return gameOver;
    }

    public Difficulty getCurrentDifficulty() {
        return currentDifficulty;
    }

    public void setGameOver() {
        this.gameOver = true;
        this.remainingAttempts = 0;
    }
}