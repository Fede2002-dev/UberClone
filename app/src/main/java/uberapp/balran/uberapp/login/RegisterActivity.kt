package uberapp.balran.uberapp.login

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.uberapp.R
import com.example.uberapp.databinding.ActivityRegisterBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import uberapp.balran.uberapp.clientHome.HomeActivity
import uberapp.balran.uberapp.pojos.User

class RegisterActivity : AppCompatActivity() {
    private lateinit var binding:ActivityRegisterBinding
    private var mAuth: FirebaseAuth? = null
    private var database: FirebaseDatabase? = null
    private var ref: DatabaseReference? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRegisterBinding.inflate(layoutInflater)
        setContentView(binding.root)

        //Iniciando componentes de firebase
        mAuth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance()
        ref = database!!.getReference("Users").child("clients")

        //Metodos Onclick
        binding.tvLogin.setOnClickListener(View.OnClickListener {
            val i = Intent(this@RegisterActivity, LoginActivity::class.java)
            finish()
            startActivity(i)
        })
        binding.buttonRegister.setOnClickListener{
            binding.buttonRegister.isEnabled = false
            val email = binding.etEmailRegister.text.toString()
            val name = binding.etNameRegister.text.toString()
            val password = binding.etPasswordRegister.text.toString()
            val confirmPassword = binding.etConfirmpassword.text.toString()

            if (email.isNotEmpty() && name.isNotEmpty() && password.isNotEmpty() && confirmPassword.isNotEmpty()) {
                if (password == confirmPassword) {
                    if (password.length >= 8) {
                        binding.buttonRegister.text = "Registrando..."
                        //Crear nuevo usuario
                        mAuth!!.createUserWithEmailAndPassword(email, password).addOnCompleteListener { task ->
                            if (task.isSuccessful) {
                                val user = mAuth!!.currentUser
                                val reference = ref!!.child(user!!.uid)
                                val user1 = User(name, email, user.uid, "", "")
                                reference.setValue(user1)
                                goToHome()
                            } else {
                                Toast.makeText(this@RegisterActivity, "Ha ocurrido un error. Intente nuevamente.", Toast.LENGTH_SHORT).show()
                                binding.buttonRegister.isEnabled = true
                                binding.buttonRegister.text = "Registrarme"
                            }
                        }
                    } else {
                        binding.etPasswordRegister.error = "La contraseña debe tener al menos 8 caracteres"
                        binding.buttonRegister.isEnabled = true
                        binding.buttonRegister.text = "Registrarme"
                    }
                } else {
                    binding.etConfirmpassword.error = "Las contraseñas no coinciden!"
                    binding.buttonRegister.isEnabled = true
                    binding.buttonRegister.text = "Registrarme"
                }
            } else {
                if (name.isEmpty()) {
                    binding.etNameRegister.error = "Introduzca su nombre"
                    binding.buttonRegister.isEnabled = true
                    binding.buttonRegister.text = "Registrarme"
                }
                if (email.isEmpty()) {
                    binding.etEmailRegister.error = "Introduzca un email"
                    binding.buttonRegister.isEnabled = true
                    binding.buttonRegister.text = "Registrarme"
                } else if (password.isEmpty()) {
                    binding.etPasswordRegister.error = "Introduzca una contraseña"
                    binding.buttonRegister.isEnabled = true
                    binding.buttonRegister.text = "Registrarme"
                } else {
                    binding.etConfirmpassword.error = "Confirme la contraseña"
                    binding.buttonRegister.isEnabled = true
                    binding.buttonRegister.text = "Registrarme"
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        val currentUser = mAuth!!.currentUser
        if (currentUser != null) {
            goToHome()
        }
    }

    private fun goToHome() {
        val i = Intent(this@RegisterActivity, HomeActivity::class.java)
        startActivity(i)
        finish()
    }
}