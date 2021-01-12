package uberapp.balran.uberapp.login

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.uberapp.databinding.ActivityRegisterDriverBinding

class DriverRegisterActivity : AppCompatActivity() {
    private lateinit var binding:ActivityRegisterDriverBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding=ActivityRegisterDriverBinding.inflate(layoutInflater)
        setContentView(binding.root)

        //Declarando componentes
        A = this

        //Metodos Onclick
        binding.tvLoginDriver.setOnClickListener{
            val i = Intent(this@DriverRegisterActivity, LoginActivity::class.java)
            finish()
            if (DataDriverActivity.A != null) {
                DataDriverActivity.A!!.finish()
            }
            startActivity(i)
        }
        binding.buttonRegisterDriver.setOnClickListener{
            val email = binding.etEmailRegisterDriver.text.toString()
            val password = binding.etPasswordRegisterDriver.text.toString()
            val confirmpassword = binding.etConfirmpasswordDriver.text.toString()
            val i = Intent(this@DriverRegisterActivity, DataDriverActivity::class.java)

            if (email.isNotEmpty() && password.isNotEmpty() && confirmpassword.isNotEmpty()) {
                if (password == confirmpassword) {
                    if (password.length >= 8) {
                        i.putExtra("email", email)
                        i.putExtra("password", password)
                        startActivity(i)
                    } else {
                        binding.etPasswordRegisterDriver.error = "Las contraseñas deben tener por lo menos 8 caracteres."
                    }
                } else {
                    binding.etConfirmpasswordDriver.error = "Las contraseñas no coinciden"
                }
            } else {
                if (email.isEmpty()) {
                    binding.etEmailRegisterDriver.error = "Introduzca un email"
                } else if (password.isEmpty()) {
                    binding.etPasswordRegisterDriver.error = "Introduzca una contraseña"
                } else if (confirmpassword.isEmpty()) {
                    binding.etConfirmpasswordDriver.error = "Confirme la contraseña"
                }
            }
        }
    }

    companion object {
        var A: Activity? = null
    }
}