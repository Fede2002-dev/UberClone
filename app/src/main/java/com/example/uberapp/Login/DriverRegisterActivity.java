package com.example.uberapp.Login;

import androidx.appcompat.app.AppCompatActivity;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.example.uberapp.R;

public class DriverRegisterActivity extends AppCompatActivity {
    TextView tv_gotologin;
    Button btn_register;
    EditText et_email, et_password, et_confirmpassword;
    public static Activity A;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register_driver);

        //Declarando componentes
        A = this;

        tv_gotologin = findViewById(R.id.tv_login_driver);
        btn_register = findViewById(R.id.button_register_driver);
        et_email = findViewById(R.id.et_email_register_driver);
        et_password = findViewById(R.id.et_password_register_driver);
        et_confirmpassword = findViewById(R.id.et_confirmpassword_driver);

        //Metodos Onclick
        tv_gotologin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent i = new Intent(DriverRegisterActivity.this, LoginActivity.class);
                finish();
                if(DataDriverActivity.A != null) {
                    DataDriverActivity.A.finish();
                }
                startActivity(i);
            }
        });

        btn_register.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String email = et_email.getText().toString();
                String password = et_password.getText().toString();
                String confirmpassword = et_confirmpassword.getText().toString();
                Intent i = new Intent(DriverRegisterActivity.this, DataDriverActivity.class);

                if(!email.isEmpty() && !password.isEmpty() && !confirmpassword.isEmpty()){
                    if(password.equals(confirmpassword)) {
                        if(password.length() >=8) {
                            i.putExtra("email", email);
                            i.putExtra("password", password);
                            startActivity(i);
                        }else{
                            et_password.setError("Las contraseñas deben tener por lo menos 8 caracteres.");
                        }
                    }else{
                        et_confirmpassword.setError("Las contraseñas no coinciden");
                    }
                }else{
                    if(email.isEmpty()){
                        et_email.setError("Introduzca un email");
                    }
                    else if(password.isEmpty()){
                        et_password.setError("Introduzca una contraseña");
                    }
                    else if(confirmpassword.isEmpty()){
                        et_confirmpassword.setError("Confirme la contraseña");
                    }
                }
            }
        });
    }
}
