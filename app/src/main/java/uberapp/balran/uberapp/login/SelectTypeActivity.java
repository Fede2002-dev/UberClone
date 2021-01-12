package uberapp.balran.uberapp.login;

import androidx.appcompat.app.AppCompatActivity;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.example.uberapp.R;

public class SelectTypeActivity extends AppCompatActivity implements View.OnClickListener {
    Button btn_selectDriver, btn_selecClient;
    public static Activity A;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_select_type);

        //Declarando componentes
        A = this;
        btn_selectDriver = findViewById(R.id.btn_selectDriver);
        btn_selecClient = findViewById(R.id.btn_selectClient);

        //Metodos Onclick

        btn_selectDriver.setOnClickListener(this);
        btn_selecClient.setOnClickListener(this);

    }

    @Override
    public void onClick(View v) {
        if(v.getId() == R.id.btn_selectDriver){
            Intent i = new Intent(SelectTypeActivity.this, DriverRegisterActivity.class);
            startActivity(i);
            Toast.makeText(this, "Driver", Toast.LENGTH_SHORT).show();
        }
        else if(v.getId() == R.id.btn_selectClient){
            Intent i = new Intent(SelectTypeActivity.this, RegisterActivity.class);
            startActivity(i);
            Toast.makeText(this, "Client", Toast.LENGTH_SHORT).show();
        }
    }

}
