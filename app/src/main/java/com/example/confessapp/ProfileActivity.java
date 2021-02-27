package com.example.confessapp;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class ProfileActivity extends AppCompatActivity {

    private FirebaseAuth mFirebaseAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        //Firebase
        mFirebaseAuth = FirebaseAuth.getInstance();

    }

    private void checkUserStatus() {
        FirebaseUser user = mFirebaseAuth.getCurrentUser();

        if(user != null) {

        }
        else {
            startActivity(new Intent(ProfileActivity.this, SignInActivity.class));
            finish();
        }
    }

    @Override
    protected void onStart() {
        checkUserStatus();
        super.onStart();
    }
}
