package uberapp.balran.uberapp.login;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import uberapp.balran.uberapp.DriverHomeActivity;
import com.example.uberapp.R;
import uberapp.balran.uberapp.pojos.UserDriver;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class DataDriverActivity extends AppCompatActivity {
    private Button btn_next;
    private EditText et_matricula, et_nombre, et_dni, et_phone;
    private FirebaseAuth mAuth;
    private FirebaseDatabase database;
    private DatabaseReference ref;
    public static Activity A;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_data_driver);
        A = this;

        //Iniciando componentes de firebase
        mAuth = FirebaseAuth.getInstance();
        database = FirebaseDatabase.getInstance();
        ref = database.getReference("Users").child("drivers");

        //Declarando componentes
        btn_next = findViewById(R.id.btn_next);
        et_matricula = findViewById(R.id.et_matricula);
        et_nombre = findViewById(R.id.et_nombre);
        et_dni = findViewById(R.id.et_dni);
        et_phone = findViewById(R.id.et_phone);

        //Variables para el registro
        final String email = getIntent().getExtras().getString("email");
        final String password = getIntent().getExtras().getString("password");

        //Metodos Onclick
        btn_next.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                btn_next.setEnabled(false);
                String name = et_nombre.getText().toString();
                String matricula = et_matricula.getText().toString();
                String dni = et_dni.getText().toString();
                String phone = et_phone.getText().toString();

                if(!name.isEmpty() && !matricula.isEmpty() && !dni.isEmpty() && !phone.isEmpty()){
                    register(email, password, name, matricula, dni, phone);
                }else{
                    if(name.isEmpty()){
                        btn_next.setEnabled(true);
                        et_nombre.setError("Ingrese su nombre");
                    }
                    if(matricula.isEmpty()){
                        btn_next.setEnabled(true);
                        et_matricula.setError("Ingrese su matricula");
                    }
                    if(dni.isEmpty()){
                        btn_next.setEnabled(true);
                        et_dni.setError("Ingrese su dni");
                    }
                    if (phone.isEmpty()){
                        btn_next.setEnabled(true);
                        et_phone.setError("Ingrese su numero de telefono");
                    }
                }
            }
        });
    }//Fin oncreate

    public void register(final String email, String password, final String name, final String matricula, final String dni, final String phone){

        //Crear nuevo usuario
        mAuth.createUserWithEmailAndPassword(email, password).addOnCompleteListener(new OnCompleteListener<AuthResult>() {
            @Override
            public void onComplete(@NonNull Task<AuthResult> task) {
                if(task.isSuccessful()){
                    FirebaseUser user = mAuth.getCurrentUser();
                    DatabaseReference reference = ref.child(user.getUid());
                    UserDriver user1 = new UserDriver(name, user.getUid(), email, "disconnected", matricula, phone, dni, "", "");
                    reference.setValue(user1);

                    goToHome();
                }else{
                    Toast.makeText(DataDriverActivity.this, "Ha ocurrido un error. Intente nuevamente.", Toast.LENGTH_SHORT).show();
                    btn_next.setEnabled(true);
                }
            }
        });
    }

    private void goToHome() {
        Intent i = new Intent(DataDriverActivity.this, DriverHomeActivity.class);
        startActivity(i);
        finish();
    }

}
