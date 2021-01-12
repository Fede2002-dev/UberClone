package uberapp.balran.uberapp.login

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.uberapp.R
import com.example.uberapp.databinding.ActivityDataDriverBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import uberapp.balran.uberapp.DriverHomeActivity
import uberapp.balran.uberapp.login.DataDriverActivity
import uberapp.balran.uberapp.pojos.UserDriver

class DataDriverActivity : AppCompatActivity() {
    private lateinit var binding:ActivityDataDriverBinding

    private var mAuth: FirebaseAuth? = null
    private var database: FirebaseDatabase? = null
    private var ref: DatabaseReference? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding=ActivityDataDriverBinding.inflate(layoutInflater)
        setContentView(binding.root)
        A = this

        //Iniciando componentes de firebase
        mAuth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance()
        ref = database!!.getReference("Users").child("drivers")

        //Variables para el registro
        val email = intent.extras!!.getString("email")
        val password = intent.extras!!.getString("password")

        //Metodos Onclick
        binding.btnNext.setOnClickListener(View.OnClickListener {
            binding.btnNext.isEnabled = false
            val name = binding.etNombre.text.toString()
            val matricula = binding.etMatricula.text.toString()
            val dni = binding.etDni.text.toString()
            val phone = binding.etPhone.text.toString()

            if (name.isNotEmpty() && matricula.isNotEmpty() && dni.isNotEmpty() && phone.isNotEmpty()) {
                register(email, password, name, matricula, dni, phone)

            } else {
                if (name.isEmpty()) {
                    binding.btnNext.isEnabled = true
                    binding.etNombre.error = "Ingrese su nombre"
                    binding.btnNext.text="Siguiente"
                }
                if (matricula.isEmpty()) {
                    binding.btnNext.isEnabled = true
                    binding.etMatricula.error = "Ingrese su matricula"
                    binding.btnNext.text="Siguiente"
                }
                if (dni.isEmpty()) {
                    binding.btnNext.isEnabled = true
                    binding.etDni.error = "Ingrese su dni"
                    binding.btnNext.text="Siguiente"
                }
                if (phone.isEmpty()) {
                    binding.btnNext.isEnabled = true
                    binding.etPhone.error = "Ingrese su numero de telefono"
                    binding.btnNext.text="Siguiente"
                }
            }
        })
    } //Fin oncreate

    private fun register(email: String?, password: String?, name: String?, matricula: String?, dni: String?, phone: String?) {
        binding.btnNext.text="Registrando..."
        //Crear nuevo usuario
        mAuth!!.createUserWithEmailAndPassword(email!!, password!!).addOnCompleteListener { task ->
            if (task.isSuccessful) {
                val user = mAuth!!.currentUser
                val reference = ref!!.child(user!!.uid)
                val user1 = UserDriver(name, user.uid, email, "disconnected", matricula, phone, dni, "", "")
                reference.setValue(user1)
                goToHome()
            } else {
                Toast.makeText(this@DataDriverActivity, "Ha ocurrido un error. Intente nuevamente.", Toast.LENGTH_SHORT).show()
                binding.btnNext.isEnabled = true
                binding.btnNext.text="Siguiente"
            }
        }
    }

    private fun goToHome() {
        val i = Intent(this@DataDriverActivity, DriverHomeActivity::class.java)
        startActivity(i)
        finish()
    }

    companion object {
        var A: Activity? = null
    }
}