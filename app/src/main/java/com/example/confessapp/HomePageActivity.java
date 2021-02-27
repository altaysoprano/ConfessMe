package com.example.confessapp;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import android.os.Bundle;

public class HomePageActivity extends AppCompatActivity {

    Toolbar homePageToolbar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home_page);

        homePageToolbar = findViewById(R.id.home_page_toolbar);
        setSupportActionBar(homePageToolbar);
    }
}
