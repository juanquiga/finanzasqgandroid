package com.jdqg.finanzasqg

import android.content.Intent
import android.os.Bundle
import android.text.InputType
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase

class SignUp : AppCompatActivity() {

    private lateinit var editName    : EditText
    private lateinit var editEmail   : EditText
    private lateinit var editPassword: EditText
    private lateinit var btnSignUp   : Button
    private lateinit var btnVolver   : Button
    private lateinit var btnVerPass  : ImageButton
    private lateinit var mAuth       : FirebaseAuth
    private var mostrandoPass = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sign_up)

        mAuth        = FirebaseAuth.getInstance()
        editName     = findViewById(R.id.edt_Name)
        editEmail    = findViewById(R.id.edt_Email)
        editPassword = findViewById(R.id.edt_password)
        btnSignUp    = findViewById(R.id.btnSignUp)
        btnVolver    = findViewById(R.id.btnVolverLogin)
        btnVerPass   = findViewById(R.id.btnVerPassSignup)

        btnVerPass.setOnClickListener {
            mostrandoPass = !mostrandoPass
            editPassword.inputType = if (mostrandoPass)
                InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
            else
                InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
            editPassword.setSelection(editPassword.text.length)
        }

        btnSignUp.setOnClickListener {
            val name  = editName.text.toString().trim()
            val email = editEmail.text.toString().trim()
            val pass  = editPassword.text.toString().trim()
            when {
                name.isEmpty()  -> { editName.error = "Ingresa tu nombre"; editName.requestFocus() }
                email.isEmpty() -> { editEmail.error = "Ingresa tu correo"; editEmail.requestFocus() }
                pass.length < 6 -> { editPassword.error = "Mínimo 6 caracteres"; editPassword.requestFocus() }
                else -> registrar(name, email, pass)
            }
        }
        btnVolver.setOnClickListener { finish() }
    }

    private fun registrar(name: String, email: String, pass: String) {
        btnSignUp.isEnabled = false; btnSignUp.text = "Creando..."
        mAuth.createUserWithEmailAndPassword(email, pass)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    val uid = mAuth.currentUser?.uid ?: ""
                    FirebaseDatabase.getInstance().reference
                        .child("users").child(uid)
                        .setValue(mapOf(
                            "name"      to name,
                            "email"     to email,
                            "uid"       to uid,
                            "isNewUser" to true
                        ))
                        .addOnSuccessListener {
                            // Cerrar sesión → debe iniciar sesión manualmente
                            mAuth.signOut()
                            Toast.makeText(this,
                                "¡Cuenta creada! 🎉 Inicia sesión para continuar.",
                                Toast.LENGTH_LONG).show()
                            finish()
                        }
                } else {
                    val msg = when {
                        task.exception?.message?.contains("email") == true ->
                            "Correo inválido o ya registrado"
                        task.exception?.message?.contains("password") == true ->
                            "Contraseña muy débil"
                        else -> "Error: ${task.exception?.message}"
                    }
                    Toast.makeText(this, msg, Toast.LENGTH_LONG).show()
                    btnSignUp.isEnabled = true; btnSignUp.text = "CREAR CUENTA"
                }
            }
    }
}