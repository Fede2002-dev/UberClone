package uberapp.balran.uberapp.login

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.res.ResourcesCompat
import com.example.uberapp.R
import com.example.uberapp.databinding.ActivityLoginBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import uberapp.balran.uberapp.DriverHomeActivity
import uberapp.balran.uberapp.clientHome.HomeActivity

class LoginActivity : AppCompatActivity() {
    //Variables
    private lateinit var binding:ActivityLoginBinding
    private var userType = ""

    //Variables firebase
    private var mAuth: FirebaseAuth? = null
    private var database: FirebaseDatabase? = null
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        //Iniciando firebase
        mAuth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance()

        //Declarando componentes
        A = this

        //Metodos Onclick
        binding.tvRegister.setOnClickListener(View.OnClickListener {
            val i = Intent(this@LoginActivity, SelectTypeActivity::class.java)
            startActivity(i)
        })
        checkUserType()
        loginOnClick()
    }

    private fun loginOnClick() {
        binding.btnLogin.setOnClickListener {
            binding.btnLogin.isEnabled = false
            val email = binding.etEmailLogin.text.toString()
            val password = binding.etPasswordLogin.text.toString()
            if (userType != "") {
                if (email.isNotEmpty() && password.isNotEmpty()) {
                    binding.btnLogin.text="Cargando..."
                    mAuth!!.signInWithEmailAndPassword(email, password).addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            if (userType == "client") {
                                val user = mAuth!!.currentUser
                                val ref = database!!.getReference("Users").child("clients").child(user!!.uid)
                                ref.addListenerForSingleValueEvent(object : ValueEventListener {
                                    override fun onDataChange(dataSnapshot: DataSnapshot) {
                                        if (dataSnapshot.exists()) {
                                            goToHome()
                                        } else {
                                            binding.btnLogin.isEnabled = true
                                            binding.btnLogin.text = "Iniciar sesion"
                                            mAuth!!.signOut()
                                            Toast.makeText(this@LoginActivity, "Usted no es un pasajero.", Toast.LENGTH_SHORT).show()
                                        }
                                    }

                                    override fun onCancelled(databaseError: DatabaseError) {
                                        mAuth!!.signOut()
                                        binding.btnLogin.text = "Iniciar sesion"
                                        binding.btnLogin.isEnabled = true
                                    }
                                })
                            } else if (userType == "driver") {
                                binding.btnLogin.text = "Cargando..."
                                val user = mAuth!!.currentUser
                                val ref = database!!.getReference("Users").child("drivers").child(user!!.uid)
                                ref.addListenerForSingleValueEvent(object : ValueEventListener {
                                    override fun onDataChange(dataSnapshot: DataSnapshot) {
                                        if (dataSnapshot.exists()) {
                                            goToHomeDriver()
                                        } else {
                                            binding.btnLogin.text = "Iniciar sesion"
                                            binding.btnLogin.isEnabled= true
                                            mAuth!!.signOut()
                                            Toast.makeText(this@LoginActivity, "Usted no es un conductor.", Toast.LENGTH_SHORT).show()
                                        }
                                    }

                                    override fun onCancelled(databaseError: DatabaseError) {
                                        binding.btnLogin.text = "Iniciar sesion"
                                        binding.btnLogin.isEnabled = true
                                        mAuth!!.signOut()
                                    }
                                })
                            }
                        } else {
                            val e = task.exception
                            binding.btnLogin.isEnabled = true
                            binding.btnLogin.text = "Iniciar sesion"
                            Toast.makeText(this@LoginActivity, "Revise su email y/o contraseña.", Toast.LENGTH_SHORT).show()
                        }
                    } //fin de OnCompleteListener
                } else {
                    if (email.isEmpty()) {
                        binding.btnLogin.isEnabled = true
                        binding.etEmailLogin.error = "Ingrese su email."
                    }
                    if (password.isEmpty()) {
                        binding.btnLogin.isEnabled = true
                        binding.etPasswordLogin.error = "Ingrese su contraseña"
                    }
                }
            } else {
                binding.btnLogin.isEnabled = true
                Toast.makeText(this@LoginActivity, "Debe seleccionar su tipo de usuario.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun goToHomeDriver() {
        val i = Intent(this@LoginActivity, DriverHomeActivity::class.java)
        startActivity(i)
        finish()
    }

    private fun checkUserType() {
        binding.btnDriver.setOnClickListener {
            if (userType != "driver") {
                binding.btnDriver.background = ResourcesCompat.getDrawable(resources, R.drawable.button,null)
                binding.btnClient.background = ResourcesCompat.getDrawable(resources,R.drawable.btn_unselected,null)
                binding.btnDriver.setTextColor(ResourcesCompat.getColor(resources, R.color.colorWhite,null))
                binding.btnClient.setTextColor(ResourcesCompat.getColor(resources, R.color.colorText,null))
                userType = "driver"
            }
        }
        binding.btnClient.setOnClickListener {
            if (userType != "client") {
                binding.btnClient.background = ResourcesCompat.getDrawable(resources, R.drawable.button,null)
                binding.btnDriver.background = ResourcesCompat.getDrawable(resources,R.drawable.btn_unselected,null)
                binding.btnClient.setTextColor(ResourcesCompat.getColor(resources, R.color.colorWhite,null))
                binding.btnDriver.setTextColor(ResourcesCompat.getColor(resources, R.color.colorText,null))
                userType = "client"
            }
        }
    }

    private fun goToHome() {
        val i = Intent(this@LoginActivity, HomeActivity::class.java)
        startActivity(i)
        finish()
    }

    companion object {
        var A: Activity? = null
    }
}