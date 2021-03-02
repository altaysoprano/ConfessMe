package com.example.confessapp;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class HomePageActivity extends AppCompatActivity {

    Toolbar homePageToolbar;
    private FirebaseAuth mFirebaseAuth;
    BottomNavigationView bottomNavigationView;
    TextView toolbarTextView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home_page);

        //Bottom Navigation Bar
        bottomNavigationView = findViewById(R.id.home_page_bottom);
        getSupportFragmentManager().beginTransaction().replace(R.id.fragment_layout, new HomeFragment()).commit();
        bottomNavigationView.setOnNavigationItemSelectedListener(new BottomNavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                Fragment selectedFragment = null;

                switch(item.getItemId()) {
                    case R.id.home :
                        selectedFragment = new HomeFragment();
                        toolbarTextView.setText("Home");
                        break;
                    case R.id.search :
                        selectedFragment = new SearchFragment();
                        toolbarTextView.setText("Search");
                        break;
                    case R.id.profile :
                        selectedFragment = new ProfileFragment();
                        toolbarTextView.setText("Profile");
                        break;
                    case R.id.inbox :
                        selectedFragment = new InboxFragment();
                        toolbarTextView.setText("Inbox");
                        break;
                    case R.id.notifications :
                        selectedFragment = new NotificationsFragment();
                        toolbarTextView.setText("Notifications");
                        break;
                }
                getSupportFragmentManager().beginTransaction()
                        .replace(R.id.fragment_layout, selectedFragment).commit();

                return true;
            }
        });

        //Toolbar
        homePageToolbar = findViewById(R.id.toolbar);
        setSupportActionBar(homePageToolbar);
        getSupportActionBar().setTitle("");
        toolbarTextView = findViewById(R.id.toolbar_textview);

        //Firebase
        mFirebaseAuth = FirebaseAuth.getInstance();


    }

    @Override
    protected void onStart() {
        checkUserStatus();
        super.onStart();
    }

    private void checkUserStatus() {
        FirebaseUser user = mFirebaseAuth.getCurrentUser();

        if(user != null) {

        }
        else {
            startActivity(new Intent(HomePageActivity.this, SignInActivity.class));
            finish();
        }
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();

        if(id == R.id.sign_out_item) {
            mFirebaseAuth.signOut();
            checkUserStatus();
        }
        return super.onOptionsItemSelected(item);
    }
}
