package com.jdqg.finanzasqg

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.text.InputType
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.database.*

class Login : AppCompatActivity() {

    private lateinit var etEmail      : EditText
    private lateinit var etContrasena : EditText
    private lateinit var btnIngresar  : Button
    private lateinit var btnRegistrar : Button
    private lateinit var btnGoogle    : Button
    private lateinit var tvOlvide     : TextView
    private lateinit var checkRecordar: CheckBox
    private lateinit var btnVerPass   : ImageButton
    private lateinit var auth         : FirebaseAuth
    private lateinit var googleClient : GoogleSignInClient
    private val db = FirebaseDatabase.getInstance().reference

    private val PREFS     = "finanzasqg_prefs"
    private val KEY_REC   = "recordar_sesion"
    private val KEY_EMAIL = "email_guardado"
    private val RC_GOOGLE = 9001
    private var mostrandoPass = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        auth = FirebaseAuth.getInstance()

        val prefs    = getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val recordar = prefs.getBoolean(KEY_REC, false)
        if (recordar && auth.currentUser != null) { irAMain(); return }
        if (!recordar) auth.signOut()

        setContentView(R.layout.activity_login)

        etEmail       = findViewById(R.id.email)
        etContrasena  = findViewById(R.id.contrasena)
        btnIngresar   = findViewById(R.id.ingresar)
        btnRegistrar  = findViewById(R.id.registrar)
        btnGoogle     = findViewById(R.id.btnGoogle)
        tvOlvide      = findViewById(R.id.tvOlvideContrasena)
        checkRecordar = findViewById(R.id.checkRecordar)
        btnVerPass    = findViewById(R.id.btnVerContrasena)

        // Configurar Google Sign-In
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()
        googleClient = GoogleSignIn.getClient(this, gso)

        val emailGuardado = prefs.getString(KEY_EMAIL, "")
        if (!emailGuardado.isNullOrEmpty()) {
            etEmail.setText(emailGuardado)
            checkRecordar.isChecked = true
        }

        btnVerPass.setOnClickListener {
            mostrandoPass = !mostrandoPass
            etContrasena.inputType = if (mostrandoPass)
                InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
            else
                InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
            etContrasena.setSelection(etContrasena.text.length)
        }

        btnIngresar.setOnClickListener {
            val input = etEmail.text.toString().trim()
            val pass  = etContrasena.text.toString().trim()
            if (input.isEmpty() || pass.isEmpty()) {
                Toast.makeText(this, "Completa todos los campos", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (input.contains("@")) loginConEmail(input, pass)
            else buscarPorNombre(input, pass)
        }

        btnGoogle.setOnClickListener {
            googleClient.signOut().addOnCompleteListener {
                startActivityForResult(googleClient.signInIntent, RC_GOOGLE)
            }
        }

        intentarHuella()

        btnRegistrar.setOnClickListener {
            startActivity(Intent(this, SignUp::class.java))
        }

        tvOlvide.setOnClickListener { mostrarDialogoRecuperacion() }
    }

    @Suppress("DEPRECATION")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == RC_GOOGLE) {
            try {
                val account = GoogleSignIn
                    .getSignedInAccountFromIntent(data)
                    .getResult(ApiException::class.java)
                val credential = GoogleAuthProvider.getCredential(account.idToken, null)
                auth.signInWithCredential(credential)
                    .addOnCompleteListener(this) { task ->
                        if (task.isSuccessful) {
                            val user = auth.currentUser ?: return@addOnCompleteListener
                            manejarLoginSocial(
                                uid    = user.uid,
                                email  = user.email ?: "",
                                nombre = user.displayName ?: ""
                            )
                        } else {
                            Toast.makeText(this,
                                "Error: ${task.exception?.message}",
                                Toast.LENGTH_LONG).show()
                        }
                    }
            } catch (e: ApiException) {
                Toast.makeText(this,
                    "Google Sign-In falló (código ${e.statusCode})",
                    Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun manejarLoginSocial(uid: String, email: String, nombre: String) {
        db.child("users").child(uid)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snap: DataSnapshot) {
                    if (!snap.exists()) {
                        db.child("users").child(uid).setValue(
                            mapOf(
                                "name"      to nombre.ifEmpty {
                                    email.substringBefore("@")
                                },
                                "email"     to email,
                                "uid"       to uid,
                                "isNewUser" to true
                            )
                        ).addOnSuccessListener { irAOnboarding() }
                    } else {
                        val isNew = snap.child("isNewUser")
                            .getValue(Boolean::class.java) ?: false
                        if (isNew) irAOnboarding() else irAMain()
                    }
                }
                override fun onCancelled(e: DatabaseError) { irAMain() }
            })
    }

    private fun intentarHuella() {
        val prefs         = getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val emailGuardado = prefs.getString(KEY_EMAIL, "") ?: ""
        val huellaActiva  = prefs.getBoolean("huella_activa", false)
        if (emailGuardado.isEmpty() || !huellaActiva) return
        try {
            val executor = ContextCompat.getMainExecutor(this)
            val callback = object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(
                    result: BiometricPrompt.AuthenticationResult) { irAMain() }
                override fun onAuthenticationError(
                    errorCode: Int, errString: CharSequence) {}
                override fun onAuthenticationFailed() {
                    Toast.makeText(this@Login,
                        "Huella no reconocida", Toast.LENGTH_SHORT).show()
                }
            }
            val prompt = BiometricPrompt(this, executor, callback)
            val info   = BiometricPrompt.PromptInfo.Builder()
                .setTitle("FinanzasQG")
                .setSubtitle("Usa tu huella para ingresar")
                .setNegativeButtonText("Usar contraseña")
                .build()
            prompt.authenticate(info)
        } catch (e: Exception) { /* sin huella */ }
    }

    private fun buscarPorNombre(nombre: String, pass: String) {
        btnIngresar.isEnabled = false; btnIngresar.text = "Buscando..."
        db.child("users").addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snap: DataSnapshot) {
                var found: String? = null
                for (child in snap.children) {
                    val name = child.child("name").getValue(String::class.java) ?: ""
                    if (name.lowercase() == nombre.lowercase()) {
                        found = child.child("email").getValue(String::class.java)
                        break
                    }
                }
                if (found != null) loginConEmail(found, pass)
                else {
                    btnIngresar.isEnabled = true; btnIngresar.text = "INGRESAR"
                    Toast.makeText(this@Login,
                        "Usuario no encontrado", Toast.LENGTH_SHORT).show()
                }
            }
            override fun onCancelled(e: DatabaseError) {
                btnIngresar.isEnabled = true; btnIngresar.text = "INGRESAR"
            }
        })
    }

    private fun loginConEmail(correo: String, pass: String) {
        btnIngresar.isEnabled = false; btnIngresar.text = "Entrando..."
        auth.signInWithEmailAndPassword(correo, pass)
            .addOnCompleteListener(this) { task ->
                btnIngresar.isEnabled = true; btnIngresar.text = "INGRESAR"
                if (task.isSuccessful) {
                    val prefs = getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                    prefs.edit().apply {
                        putBoolean(KEY_REC, checkRecordar.isChecked)
                        putString(KEY_EMAIL,
                            if (checkRecordar.isChecked) correo else "")
                        apply()
                    }
                    val uid = auth.currentUser?.uid ?: ""
                    db.child("users").child(uid)
                        .addListenerForSingleValueEvent(object : ValueEventListener {
                            override fun onDataChange(snap: DataSnapshot) {
                                val isNew = snap.child("isNewUser")
                                    .getValue(Boolean::class.java) ?: false
                                if (isNew) irAOnboarding() else irAMain()
                            }
                            override fun onCancelled(e: DatabaseError) { irAMain() }
                        })
                } else {
                    Toast.makeText(this,
                        "Credenciales incorrectas", Toast.LENGTH_SHORT).show()
                }
            }
    }

    private fun irAMain() {
        startActivity(Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        })
        finish()
    }

    private fun irAOnboarding() {
        startActivity(Intent(this, OnboardingActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        })
        finish()
    }

    private fun mostrarDialogoRecuperacion() {
        val inflater = LayoutInflater.from(this)
        val vista    = inflater.inflate(R.layout.dialog_recuperar_contrasena, null)
        val dialog   = AlertDialog.Builder(this, R.style.DialogFinanzas)
            .setView(vista).create()
        dialog.window?.let { win ->
            win.setBackgroundDrawableResource(android.R.color.transparent)
            val w = (resources.displayMetrics.widthPixels * 0.90).toInt()
            win.setLayout(w, ViewGroup.LayoutParams.WRAP_CONTENT)
        }
        val etEmailRec = vista.findViewById<EditText>(R.id.etEmailRecuperar)
        vista.findViewById<Button>(R.id.btnEnviarRecuperar).setOnClickListener {
            val correo = etEmailRec.text.toString().trim()
            if (correo.isEmpty()) {
                etEmailRec.error = "Ingresa tu correo"; return@setOnClickListener
            }
            auth.sendPasswordResetEmail(correo)
                .addOnSuccessListener {
                    Toast.makeText(this, "Enlace enviado ✅", Toast.LENGTH_LONG).show()
                    dialog.dismiss()
                }
                .addOnFailureListener {
                    Toast.makeText(this, "Correo no encontrado", Toast.LENGTH_SHORT).show()
                }
        }
        vista.findViewById<Button>(R.id.btnCancelarRecuperar).setOnClickListener {
            dialog.dismiss()
        }
        dialog.show()
    }
}