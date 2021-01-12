package uberapp.balran.uberapp.login;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import uberapp.balran.uberapp.clientHome.HomeActivity;
import com.example.uberapp.R;
import uberapp.balran.uberapp.pojos.User;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class RegisterActivity extends AppCompatActivity {
    private TextView tv_gotologin;
    private EditText et_email, et_password, et_confirmpassword, et_name;
    private Button btn_register;
    private FirebaseAuth mAuth;
    private FirebaseDatabase database;
    private DatabaseReference ref;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        //Iniciando componentes de firebase
        mAuth = FirebaseAuth.getInstance();
        database = FirebaseDatabase.getInstance();
        ref = database.getReference("Users").child("clients");

        //Declarando componentes
        tv_gotologin = findViewById(R.id.tv_login);
        et_email = findViewById(R.id.et_email_register);
        et_name = findViewById(R.id.et_name_register);
        et_password = findViewById(R.id.et_password_register);
        et_confirmpassword = findViewById(R.id.et_confirmpassword);
        btn_register = findViewById(R.id.button_register);


        //Metodos Onclick
        tv_gotologin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent i = new Intent(RegisterActivity.this, LoginActivity.class);
                finish();
                startActivity(i);
            }
        });

        btn_register.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                btn_register.setEnabled(false);

                final String email = et_email.getText().toString();
                final String name = et_name.getText().toString();
                String password = et_password.getText().toString();
                String confirmpassword = et_confirmpassword.getText().toString();

                if(!email.isEmpty() && !name.isEmpty() && !password.isEmpty() && !confirmpassword.isEmpty()){
                    if(password.equals(confirmpassword)){
                        if(password.length() >=8){
                            //Crear nuevo usuario
                            mAuth.createUserWithEmailAndPassword(email, password).addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                                @Override
                                public void onComplete(@NonNull Task<AuthResult> task) {
                                    if(task.isSuccessful()){
                                        FirebaseUser user = mAuth.getCurrentUser();
                                        DatabaseReference reference = ref.child(user.getUid());
                                        User user1 = new User(name, email, user.getUid(), "", "");
                                        reference.setValue(user1);

                                        goToHome();
                                    }else{
                                        Toast.makeText(RegisterActivity.this, "Ha ocurrido un error. Intente nuevamente.", Toast.LENGTH_SHORT).show();
                                        btn_register.setEnabled(true);
                                    }
                                }
                            });
                        }else{
                            et_password.setError("La contraseña debe tener al menos 8 caracteres");
                            btn_register.setEnabled(true);
                        }
                    }
                    else{
                        et_confirmpassword.setError("Las contraseñas no coinciden!");
                        btn_register.setEnabled(true);
                    }
                }
                else{
                    if(name.isEmpty()){
                        et_name.setError("Introduzca su nombre");
                        btn_register.setEnabled(true);
                    }
                    if(email.isEmpty()){
                        et_email.setError("Introduzca un email");
                        btn_register.setEnabled(true);
                    }
                    else if(password.isEmpty()){
                        et_password.setError("Introduzca una contraseña");
                        btn_register.setEnabled(true);
                    }
                    else{
                        et_confirmpassword.setError("Confirme la contraseña");
                        btn_register.setEnabled(true);
                    }
                }
            }
        });
    }

    @Override
    protected void onStart() {
        super.onStart();
        FirebaseUser currentUser = mAuth.getCurrentUser();

        if(currentUser!=null){
            goToHome();
        }
    }

    private void goToHome() {
        Intent i = new Intent(RegisterActivity.this, HomeActivity.class);
        startActivity(i);
        finish();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Toast.makeText(this, "Me muri", Toast.LENGTH_SHORT).show();
    }
}
