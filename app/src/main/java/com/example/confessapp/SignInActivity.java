package com.example.confessapp;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.widget.NestedScrollView;

import android.app.AlertDialog;
import android.app.ProgressDialog;
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
import android.widget.Toast;

import com.facebook.AccessToken;
import com.facebook.CallbackManager;
import com.facebook.FacebookCallback;
import com.facebook.FacebookException;
import com.facebook.FacebookSdk;

import com.facebook.login.LoginResult;
import com.facebook.login.widget.LoginButton;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FacebookAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class SignInActivity extends AppCompatActivity implements RecoverDialog.RecoverDialogListener {

    NestedScrollView nestedScrollView;
    LinearLayout editTextLayout;
    LinearLayout noAccountTextLinearLayout;
    Button buttonLogin;
    LinearLayout logoLayout;
    TextInputLayout emailTextInputLayout;
    TextInputEditText emailEditText;
    TextInputEditText passwordEditText;
    TextView signUpTextView;
    TextInputLayout passwordTextInputLayout;
    private FirebaseAuth mAuth;
    LoginButton facebookLoginButton;
    ProgressDialog progressDialog;
    TextView forgotPasswordText;

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
        passwordEditText = findViewById(R.id.password_edit_text);
        passwordTextInputLayout = findViewById(R.id.password_textfield);
        nestedScrollView.setBackgroundColor(Color.WHITE);
        facebookLoginButton = findViewById(R.id.facebook_login_button);
        signUpTextView = findViewById(R.id.sign_up_text);
        forgotPasswordText = findViewById(R.id.forgot_password_text);

        //ProgressDialog
        progressDialog = new ProgressDialog(this);

        //Firebase
        mAuth = FirebaseAuth.getInstance();

        signUpTextView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(SignInActivity.this, RegisterActivity.class);
                startActivity(intent);
                finish();
            }
        });

        forgotPasswordText.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showRecoverPasswordDialog();
            }
        });

        buttonLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String email = emailEditText.getText().toString().trim();
                String password = passwordEditText.getText().toString().trim();
                if(email == null || email.isEmpty()) {
                    emailEditText.setError("Email field cannot be left blank");
                    emailEditText.setFocusable(true);
                }
                else if(password == null || password.isEmpty()) {
                    passwordEditText.setError("Password field cannot be left blank");
                    passwordEditText.setFocusable(true);
                }
                else {
                    loginUser(email, password);
                }
            }
        });
    }

    private void showRecoverPasswordDialog() {
        RecoverDialog recoverDialog = new RecoverDialog();
        recoverDialog.show(getSupportFragmentManager(), "Recover Dialog");
    }

    private void loginUser(String email, String password) {

        progressDialog.setMessage("Logging In...");
        progressDialog.show();

        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            progressDialog.dismiss();
                            // Sign in success, update UI with the signed-in user's information
                            FirebaseUser user = mAuth.getCurrentUser();
                            startActivity(new Intent(SignInActivity.this, HomePageActivity.class));
                            finish();
                        } else {
                            progressDialog.dismiss();
                            // If sign in fails, display a message to the user.
                            Toast.makeText(SignInActivity.this, "Authentication failed.",
                                    Toast.LENGTH_SHORT).show();
                        }
                    }
                }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                progressDialog.dismiss();
                Toast.makeText(SignInActivity.this, "" + e.getMessage(), Toast.LENGTH_SHORT).show();
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

    @Override
    protected void onStart() {
        checkUserStatus();
        super.onStart();
    }

    private void checkUserStatus() {
        FirebaseUser user = mAuth.getCurrentUser();

        if(user != null) {
            startActivity(new Intent(SignInActivity.this, HomePageActivity.class));
            finish();
        }
    }

    //Email (RecoverDialog)
    @Override
    public void applyEmail(String email) {
        beginRecovery(email);
    }

    private void beginRecovery(String email) {

        progressDialog.setMessage("Email sending...");
        progressDialog.show();

        mAuth.sendPasswordResetEmail(email).addOnCompleteListener(new OnCompleteListener<Void>() {
            @Override
            public void onComplete(@NonNull Task<Void> task) {
                if(task.isSuccessful()) {
                    Toast.makeText(SignInActivity.this, "Email sent", Toast.LENGTH_SHORT).show();
                    progressDialog.dismiss();
                }
                else {
                    Toast.makeText(SignInActivity.this, "Failed", Toast.LENGTH_SHORT).show();
                    progressDialog.dismiss();
                }
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                Toast.makeText(SignInActivity.this, "" + e.getMessage(), Toast.LENGTH_SHORT).show();
                progressDialog.dismiss();
            }
        });
    }
}
