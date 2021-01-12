package uberapp.balran.uberapp.login

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.uberapp.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.*
import uberapp.balran.uberapp.DriverHomeActivity
import uberapp.balran.uberapp.clientHome.HomeActivity
import uberapp.balran.uberapp.login.LoginActivity

class MainActivity : AppCompatActivity() {
    private var mAuth: FirebaseAuth? = null
    private var user: FirebaseUser? = null
    private var database: FirebaseDatabase? = null
    private var ref: DatabaseReference? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(R.style.SplashTheme)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)


        //Iniciando firebase
        mAuth = FirebaseAuth.getInstance()
        user = mAuth!!.currentUser
        database = FirebaseDatabase.getInstance()
        if (user != null) {
            ref = database!!.getReference("Users").child("clients").child(user!!.uid)
        }
        if (user == null) {
            val i = Intent(this@MainActivity, LoginActivity::class.java)
            i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK)
            finish()
            startActivity(i)
        } else {
            ref!!.addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(dataSnapshot: DataSnapshot) {
                    if (dataSnapshot.exists()) {
                        val i = Intent(this@MainActivity, HomeActivity::class.java)
                        i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK)
                        finish()
                        startActivity(i)
                    } else {
                        ref = database!!.getReference("Users").child("drivers")
                        ref!!.addListenerForSingleValueEvent(object : ValueEventListener {
                            override fun onDataChange(dataSnapshot: DataSnapshot) {
                                if (dataSnapshot.exists()) {
                                    val i = Intent(this@MainActivity, DriverHomeActivity::class.java)
                                    i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK)
                                    finish()
                                    startActivity(i)
                                }
                            }

                            override fun onCancelled(databaseError: DatabaseError) {}
                        })
                    }
                }

                override fun onCancelled(databaseError: DatabaseError) {}
            })
        }
    }
}