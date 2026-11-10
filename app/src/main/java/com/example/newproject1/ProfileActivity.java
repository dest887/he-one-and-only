package com.example.newproject1;

import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class ProfileActivity extends AppCompatActivity {

    private ImageView imgProfile;
    private Button btnGallery, btnCamera, btnSaveProfile, btnBackToGame;
    private EditText etProfileName, etProfilePhone, etProfileCity, etProfilePassword;
    private TextView txtProfileEmail;

    private SharedPreferences preferences;
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    private final ActivityResultLauncher<String> galleryLauncher = registerForActivityResult(
            new ActivityResultContracts.GetContent(),
            uri -> {
                if (uri != null) {
                    imgProfile.setImageURI(uri);
                    preferences.edit().putString("profileImageUri", uri.toString()).apply();
                    saveImageUriToFirestore(uri.toString());
                    Toast.makeText(this, "Profile picture updated from Gallery!", Toast.LENGTH_SHORT).show();
                }
            }
    );

    private final ActivityResultLauncher<Void> cameraLauncher = registerForActivityResult(
            new ActivityResultContracts.TakePicturePreview(),
            bitmap -> {
                if (bitmap != null) {
                    imgProfile.setImageBitmap(bitmap);
                    String savedImagePath = saveImageToInternalStorage(bitmap);
                    if (savedImagePath != null) {
                        preferences.edit().putString("profileImageUri", savedImagePath).apply();
                        saveImageUriToFirestore(savedImagePath);
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
        db = FirebaseFirestore.getInstance();
        preferences = getSharedPreferences("GamePrefs", MODE_PRIVATE);

        imgProfile = findViewById(R.id.imgProfile);
        btnGallery = findViewById(R.id.btnGallery);
        btnCamera = findViewById(R.id.btnCamera);
        btnSaveProfile = findViewById(R.id.btnSaveProfile);
        btnBackToGame = findViewById(R.id.btnBackToGame);

        etProfileName = findViewById(R.id.etProfileName);
        etProfilePhone = findViewById(R.id.etProfilePhone);
        etProfileCity = findViewById(R.id.etProfileCity);
        etProfilePassword = findViewById(R.id.etProfilePassword);
        txtProfileEmail = findViewById(R.id.txtProfileEmail);

        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            String email = currentUser.getEmail();
            txtProfileEmail.setText("Email: " + (email != null ? email : "Not available"));
            loadUserDataFromFirestore(currentUser.getUid());
        }

        String savedImageUri = preferences.getString("profileImageUri", null);
        if (savedImageUri != null) {
            if (savedImageUri.startsWith("content://") || savedImageUri.startsWith("file://")) {
                imgProfile.setImageURI(Uri.parse(savedImageUri));
            } else {
                File imgFile = new File(savedImageUri);
                if (imgFile.exists()) {
                    Bitmap myBitmap = BitmapFactory.decodeFile(imgFile.getAbsolutePath());
                    imgProfile.setImageBitmap(myBitmap);
                }
            }
        }

        btnGallery.setOnClickListener(v -> galleryLauncher.launch("image/*"));
        btnCamera.setOnClickListener(v -> cameraLauncher.launch(null));
        btnSaveProfile.setOnClickListener(v -> saveProfileDetails());
        btnBackToGame.setOnClickListener(v -> finish());
    }

    private void loadUserDataFromFirestore(String userId) {
        // First, check local SharedPreferences so the newly updated password appears instantly
        String localPass = preferences.getString("savedPassword", "");
        if (!localPass.isEmpty()) {
            etProfilePassword.setText(localPass);
        }

        // Then fetch from Firestore for full sync
        DocumentReference docRef = db.collection("users").document(userId);
        docRef.get().addOnSuccessListener(documentSnapshot -> {
            if (documentSnapshot.exists()) {
                String name = documentSnapshot.getString("name");
                String phone = documentSnapshot.getString("phone");
                String city = documentSnapshot.getString("city");
                String dbPassword = documentSnapshot.getString("password");

                if (name != null) etProfileName.setText(name);
                if (phone != null) etProfilePhone.setText(phone);
                if (city != null) etProfileCity.setText(city);

                // If local didn't have it, load from Firestore (and if Firestore has a newer/different password, sync it)
                if (localPass.isEmpty() && dbPassword != null && !dbPassword.isEmpty()) {
                    etProfilePassword.setText(dbPassword);
                    preferences.edit().putString("savedPassword", dbPassword).apply();
                }
            }
        });
    }

    private void saveProfileDetails() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) return;

        String name = etProfileName.getText().toString().trim();
        String phone = etProfilePhone.getText().toString().trim();
        String city = etProfileCity.getText().toString().trim();
        String newPassword = etProfilePassword.getText().toString().trim();

        if (newPassword.isEmpty()) {
            Toast.makeText(ProfileActivity.this, "Password cannot be empty", Toast.LENGTH_SHORT).show();
            return;
        }

        if (newPassword.length() < 6) {
            Toast.makeText(ProfileActivity.this, "Password must be at least 6 characters", Toast.LENGTH_SHORT).show();
            return;
        }

        // 1. Save locally immediately so SharedPreferences is up to date
        preferences.edit()
                .putString("playerName", name)
                .putString("savedPassword", newPassword)
                .apply();

        // 2. Build Firestore Data Map
        Map<String, Object> userMap = new HashMap<>();
        userMap.put("name", name);
        userMap.put("phone", phone);
        userMap.put("city", city);
        userMap.put("password", newPassword); // <-- Ensures updated password is saved to Firestore
        userMap.put("email", currentUser.getEmail());

        // 3. Save to Firestore Database
        db.collection("users").document(currentUser.getUid())
                .set(userMap, SetOptions.merge())
                .addOnSuccessListener(aVoid -> {
                    // 4. Update Firebase Authentication password
                    currentUser.updatePassword(newPassword).addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            Toast.makeText(ProfileActivity.this, "Profile & Password updated successfully!", Toast.LENGTH_SHORT).show();
                        } else {
                            String errorMsg = task.getException() != null ? task.getException().getMessage() : "";
                            Toast.makeText(ProfileActivity.this, "Saved to Firestore & Local! Auth note: " + errorMsg, Toast.LENGTH_LONG).show();
                        }
                    });
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(ProfileActivity.this, "Firestore Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }

    private void saveImageUriToFirestore(String uriString) {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) return;

        db.collection("users").document(currentUser.getUid())
                .update("profileImageUri", uriString)
                .addOnFailureListener(e -> {
                    Map<String, Object> data = new HashMap<>();
                    data.put("profileImageUri", uriString);
                    db.collection("users").document(currentUser.getUid()).set(data, SetOptions.merge());
                });
    }

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