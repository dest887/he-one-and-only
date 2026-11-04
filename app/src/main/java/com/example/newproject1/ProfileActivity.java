package com.example.newproject1;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

public class ProfileActivity extends AppCompatActivity {

    private ImageView imgProfile;
    private Button btnGallery, btnCamera, btnBackToGame;
    private TextView txtProfileUsername, txtProfileEmail;
    private SharedPreferences preferences;
    private FirebaseAuth mAuth;

    // Launcher for picking an image from the gallery
    private final ActivityResultLauncher<String> galleryLauncher = registerForActivityResult(
            new ActivityResultContracts.GetContent(),
            uri -> {
                if (uri != null) {
                    imgProfile.setImageURI(uri);
                    preferences.edit().putString("profileImageUri", uri.toString()).apply();
                    Toast.makeText(this, "Profile picture updated from Gallery!", Toast.LENGTH_SHORT).show();
                }
            }
    );

    // Launcher for taking a photo with the camera
    private final ActivityResultLauncher<Void> cameraLauncher = registerForActivityResult(
            new ActivityResultContracts.TakePicturePreview(),
            bitmap -> {
                if (bitmap != null) {
                    imgProfile.setImageBitmap(bitmap);
                    // Save bitmap to internal storage so it persists across restarts
                    String savedImagePath = saveImageToInternalStorage(bitmap);
                    if (savedImagePath != null) {
                        preferences.edit().putString("profileImageUri", savedImagePath).apply();
                    }
                    Toast.makeText(this, "Photo captured successfully!", Toast.LENGTH_SHORT).show();
                }
            }
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        mAuth = FirebaseAuth.getInstance();
        preferences = getSharedPreferences("GamePrefs", MODE_PRIVATE);

        imgProfile = findViewById(R.id.imgProfile);
        btnGallery = findViewById(R.id.btnGallery);
        btnCamera = findViewById(R.id.btnCamera);
        btnBackToGame = findViewById(R.id.btnBackToGame);
        txtProfileUsername = findViewById(R.id.txtProfileUsername);
        txtProfileEmail = findViewById(R.id.txtProfileEmail);

        // Display Firebase User Details
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            String email = currentUser.getEmail();
            String username = currentUser.getDisplayName();

            if (username == null || username.isEmpty()) {
                username = preferences.getString("playerName", "Player");
            }

            txtProfileUsername.setText("Username: " + username);
            txtProfileEmail.setText("Email: " + (email != null ? email : "Not available"));
        }

        // Load saved profile image (Gallery URI or Camera File path)
        String savedImageUri = preferences.getString("profileImageUri", null);
        if (savedImageUri != null) {
            if (savedImageUri.startsWith("content://") || savedImageUri.startsWith("file://")) {
                imgProfile.setImageURI(Uri.parse(savedImageUri));
            } else {
                // Internal storage file path from camera
                File imgFile = new File(savedImageUri);
                if (imgFile.exists()) {
                    Bitmap myBitmap = BitmapFactory.decodeFile(imgFile.getAbsolutePath());
                    imgProfile.setImageBitmap(myBitmap);
                }
            }
        }

        // Pick from Gallery click
        btnGallery.setOnClickListener(v -> galleryLauncher.launch("image/*"));

        // Take Photo with Camera click
        btnCamera.setOnClickListener(v -> cameraLauncher.launch(null));

        // Back to game click
        btnBackToGame.setOnClickListener(v -> finish());
    }

    // Helper method to save camera bitmap to internal app storage
    private String saveImageToInternalStorage(Bitmap bitmap) {
        File directory = new File(getFilesDir(), "profile_images");
        if (!directory.exists()) {
            directory.mkdir();
        }
        File file = new File(directory, "profile_pic.png");
        try (FileOutputStream fos = new FileOutputStream(file)) {
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, fos);
            return file.getAbsolutePath();
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }
}
