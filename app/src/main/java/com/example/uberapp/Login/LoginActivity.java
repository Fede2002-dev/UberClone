package com.example.uberapp.Login;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.example.uberapp.DriverHomeActivity;
import com.example.uberapp.ClientHome.HomeActivity;
import com.example.uberapp.R;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class LoginActivity extends AppCompatActivity {
    //Variables
    private TextView tv_gotoregister;
    private EditText et_email, et_password;
    private Button btn_driver, btn_client, btn_login;
    private String userType ="";
    public static Activity A;

    //Variables firebase
    private FirebaseAuth mAuth;
    private FirebaseDatabase database;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        //Iniciando firebase

        mAuth = FirebaseAuth.getInstance();
        database = FirebaseDatabase.getInstance();

        //Declarando componentes
        A = this;
        tv_gotoregister = findViewById(R.id.tv_register);
        btn_driver = findViewById(R.id.btn_driver);
        btn_client = findViewById(R.id.btnClient);
        btn_login = findViewById(R.id.btn_login);
        et_email = findViewById(R.id.et_email_login);
        et_password = findViewById(R.id.et_password_login);

        //Metodos Onclick
        tv_gotoregister.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent i = new Intent(LoginActivity.this, SelectTypeActivity.class);
                startActivity(i);
            }
        });

        checkUserType();
        loginOnClick();
    }

    private void loginOnClick() {
        btn_login.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                btn_login.setEnabled(false);
                String email = et_email.getText().toString();
                String password = et_password.getText().toString();

                if (!userType.equals("")) {
                    if (!email.isEmpty() && !password.isEmpty()) {
                        mAuth.signInWithEmailAndPassword(email, password).addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                            @Override
                            public void onComplete(@NonNull Task<AuthResult> task) {
                                if (task.isSuccessful()) {
                                    if (userType.equals("client")) {
                                        btn_login.setText("Cargando...");
                                        FirebaseUser user = mAuth.getCurrentUser();
                                        DatabaseReference ref = database.getReference("Users").child("clients").child(user.getUid());
                                        ref.addListenerForSingleValueEvent(new ValueEventListener() {
                                            @Override
                                            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                                                if (dataSnapshot.exists()) {
                                                    goToHome();
                                                } else {
                                                    btn_login.setEnabled(true);
                                                    btn_login.setText("Iniciar sesion");
                                                    mAuth.signOut();
                                                    Toast.makeText(LoginActivity.this, "Usted no es un pasajero.", Toast.LENGTH_SHORT).show();
                                                }
                                            }

                                            @Override
                                            public void onCancelled(@NonNull DatabaseError databaseError) {
                                                mAuth.signOut();
                                                btn_login.setText("Iniciar sesion");
                                                btn_login.setEnabled(true);
                                            }
                                        });
                                    } else if (userType.equals("driver")) {
                                        btn_login.setText("Cargando...");
                                        FirebaseUser user = mAuth.getCurrentUser();
                                        DatabaseReference ref = database.getReference("Users").child("drivers").child(user.getUid());
                                        ref.addListenerForSingleValueEvent(new ValueEventListener() {
                                            @Override
                                            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                                                if (dataSnapshot.exists()) {
                                                    goToHomeDriver();
                                                } else {
                                                    btn_login.setText("Iniciar sesion");
                                                    btn_login.setEnabled(true);
                                                    mAuth.signOut();
                                                    Toast.makeText(LoginActivity.this, "Usted no es un conductor.", Toast.LENGTH_SHORT).show();
                                                }
                                            }

                                            @Override
                                            public void onCancelled(@NonNull DatabaseError databaseError) {
                                                btn_login.setText("Iniciar sesion");
                                                btn_login.setEnabled(true);
                                                mAuth.signOut();
                                            }
                                        });
                                    }
                                } else {
                                    btn_login.setEnabled(true);
                                    Toast.makeText(LoginActivity.this, "Revise su email y/o contraseña.", Toast.LENGTH_SHORT).show();
                                }
                            }
                        });//fin de OnCompleteListener
                    } else {
                        if (email.isEmpty()) {
                            btn_login.setEnabled(true);
                            et_email.setError("Ingrese su email.");
                        }
                        if (password.isEmpty()) {
                            btn_login.setEnabled(true);
                            et_password.setError("Ingrese su contraseña");
                        }
                    }
                }else{
                    btn_login.setEnabled(true);
                    Toast.makeText(LoginActivity.this, "Debe seleccionar su tipo de usuario.", Toast.LENGTH_SHORT).show();
                }
            }//fin del onclick
        });
    }

    private void goToHomeDriver() {
        Intent i = new Intent(LoginActivity.this, DriverHomeActivity.class);
        startActivity(i);
        finish();
    }


    private void checkUserType(){
        btn_driver.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(!userType.equals("driver")) {
                    btn_driver.setBackground(getResources().getDrawable(R.drawable.button));
                    btn_client.setBackground(getResources().getDrawable(R.drawable.btn_unselected));

                    btn_driver.setTextColor(getResources().getColor(R.color.colorWhite));
                    btn_client.setTextColor(getResources().getColor(R.color.colorText));

                    userType = "driver";
                }
            }
        });

        btn_client.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(!userType.equals("client")) {
                    btn_client.setBackground(getResources().getDrawable(R.drawable.button));
                    btn_driver.setBackground(getResources().getDrawable(R.drawable.btn_unselected));

                    btn_client.setTextColor(getResources().getColor(R.color.colorWhite));
                    btn_driver.setTextColor(getResources().getColor(R.color.colorText));

                    userType = "client";
                }
            }
        });
    }

    private void goToHome() {
        Intent i = new Intent(LoginActivity.this, HomeActivity.class);
        startActivity(i);
        finish();
    }
}
