package uberapp.balran.uberapp.login

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.uberapp.R

class SelectTypeActivity : AppCompatActivity(), View.OnClickListener {
    private var btnSelectDriver: Button? = null
    private var btnSelectClient: Button? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_select_type)

        //Declarando componentes
        A = this
        btnSelectDriver = findViewById(R.id.btn_selectDriver)
        btnSelectClient = findViewById(R.id.btn_selectClient)

        //Metodos Onclick
        btnSelectDriver!!.setOnClickListener(this)
        btnSelectClient!!.setOnClickListener(this)
    }

    override fun onClick(v: View) {
        if (v.id == R.id.btn_selectDriver) {
            val i = Intent(this@SelectTypeActivity, DriverRegisterActivity::class.java)
            startActivity(i)
        } else if (v.id == R.id.btn_selectClient) {
            val i = Intent(this@SelectTypeActivity, RegisterActivity::class.java)
            startActivity(i)
        }
    }

    companion object {
        var A: Activity? = null
    }
}