package com.example.confessapp;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.widget.NestedScrollView;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Rect;
import android.os.Bundle;
import android.util.Log;
import android.util.Patterns;
import android.view.MotionEvent;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.util.regex.Pattern;

public class RegisterActivity extends AppCompatActivity {

    NestedScrollView nestedScrollViewRegister;
    Button buttonRegister;
    LinearLayout logoLayoutRegister;
    TextInputLayout emailTextInputLayoutRegister;
    TextInputEditText emailEditTextRegister;
    TextInputEditText passwordEditTextRegister;
    TextInputEditText passwordAgainEditTextRegister;
    TextView signInTextViewRegister;
    TextInputLayout passwordTextInputLayoutRegister;
    TextInputLayout passwordAgainTextInputLayoutRegister;

    //ProgressDialog
    ProgressDialog progressDialog;

    //Firebase Instance
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        //Initialize
        nestedScrollViewRegister = findViewById(R.id.nested_scroll_view_register);
        buttonRegister = findViewById(R.id.register_button);
        logoLayoutRegister = findViewById(R.id.register_logo_layout);
        emailTextInputLayoutRegister = findViewById(R.id.register_email_textfield);
        emailEditTextRegister = findViewById(R.id.register_email_edit_text);
        passwordEditTextRegister = findViewById(R.id.register_password_edit_text);
        passwordAgainEditTextRegister = findViewById(R.id.register_password_again_edit_text);
        signInTextViewRegister = findViewById(R.id.register_sign_in_text);
        passwordTextInputLayoutRegister = findViewById(R.id.register_password_textfield);
        passwordAgainTextInputLayoutRegister = findViewById(R.id.register_password_again_textfield);

        //Firebase
        mAuth = FirebaseAuth.getInstance();

        progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("Registering User...");

        signInTextViewRegister.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(RegisterActivity.this, SignInActivity.class);
                startActivity(intent);
                finish();
            }
        });

        buttonRegister.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String email = emailEditTextRegister.getText().toString().trim();
                String password = passwordEditTextRegister.getText().toString().trim();
                String passwordAgain = passwordAgainEditTextRegister.getText().toString().trim();

                //validate
                if(!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                    emailEditTextRegister.setError("Invalid Email");
                    emailEditTextRegister.setFocusable(true);
                }
                else if(password.length()<6) {
                    passwordEditTextRegister.setError("Password length at least 6 characters");
                    passwordEditTextRegister.setFocusable(true);
                }
                else if(!password.equals(passwordAgain)) {
                    passwordAgainEditTextRegister.setError("Passwords do not match");
                    passwordAgainEditTextRegister.setFocusable(true);
                }
                else {
                    registerUser(email, password);
                }
            }
        });
    }

    private void registerUser(String email, String password) {

        progressDialog.show();

        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            // Sign in success, update UI with the signed-in user's information
                            progressDialog.dismiss();
                            FirebaseUser user = mAuth.getCurrentUser();
                            Toast.makeText(RegisterActivity.this, "Registered", Toast.LENGTH_SHORT).show();
                            Intent intent = new Intent(RegisterActivity.this, ProfileActivity.class);
                            startActivity(intent);
                            finish();
                        } else {
                            // If sign in fails, display a message to the user.
                            progressDialog.dismiss();
                            Toast.makeText(RegisterActivity.this, "Authentication failed.",
                                    Toast.LENGTH_SHORT).show();
                        }
                        // ...
                    }
                }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                progressDialog.dismiss();
                Toast.makeText(RegisterActivity.this, "" + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

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
