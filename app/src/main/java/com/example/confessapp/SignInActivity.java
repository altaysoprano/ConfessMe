package com.example.confessapp;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.widget.NestedScrollView;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Rect;
import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.facebook.AccessToken;
import com.facebook.CallbackManager;
import com.facebook.FacebookCallback;
import com.facebook.FacebookException;
import com.facebook.FacebookSdk;

import com.facebook.login.LoginResult;
import com.facebook.login.widget.LoginButton;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FacebookAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class SignInActivity extends AppCompatActivity {

    NestedScrollView nestedScrollView;
    LinearLayout editTextLayout;
    LinearLayout noAccountTextLinearLayout;
    Button buttonLogin;
    LinearLayout logoLayout;
    TextInputLayout emailTextInputLayout;
    TextInputEditText emailEditText;
    TextView signUpTextView;
    TextInputLayout passwordTextInputLayout;
    private CallbackManager mCallbackManager;
    private FirebaseAuth mFirebaseAuth;
    LoginButton facebookLoginButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sign_in);

        //Initializing
        nestedScrollView = findViewById(R.id.nested_scroll_view);
        editTextLayout = findViewById(R.id.edit_text_layout);
        noAccountTextLinearLayout = findViewById(R.id.no_account_text_linear_layout);
        buttonLogin = findViewById(R.id.login_button);
        logoLayout = findViewById(R.id.logo_layout);
        emailTextInputLayout = findViewById(R.id.email_textfield);
        emailEditText = findViewById(R.id.email_edit_text);
        passwordTextInputLayout = findViewById(R.id.password_textfield);
        nestedScrollView.setBackgroundColor(Color.WHITE);
        facebookLoginButton = findViewById(R.id.facebook_login_button);
        signUpTextView = findViewById(R.id.sign_up_text);

        signUpTextView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(SignInActivity.this, RegisterActivity.class);
                startActivity(intent);
            }
        });


    }

    //Hide keyboard when click somewhere else
    @Override
    public boolean dispatchTouchEvent(MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            View v = getCurrentFocus();
            if ( v instanceof EditText) {
                Rect outRect = new Rect();
                v.getGlobalVisibleRect(outRect);
                if (!outRect.contains((int)event.getRawX(), (int)event.getRawY())) {
                    v.clearFocus();
                    InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                    imm.hideSoftInputFromWindow(v.getWindowToken(), 0);
                }
            }
        }
        return super.dispatchTouchEvent( event );
    }


}
