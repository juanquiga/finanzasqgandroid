package com.jdqg.finanzasqg

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase

class OnboardingActivity : AppCompatActivity() {

    private var paginaActual = 0

    private val paginas = listOf(
        Triple("💰", "Bienvenido a FinanzasQG",
            "Tu app personal para controlar ingresos y gastos de forma fácil e inteligente."),
        Triple("📝", "Registra tus movimientos",
            "Agrega ingresos o egresos en segundos. Selecciona categoría, subcategoría y valor."),
        Triple("📊", "Visualiza tus finanzas",
            "Gráficas de barras y donut para ver exactamente en qué gastas más cada mes."),
        Triple("📅", "Resumen inteligente",
            "Filtra por Diario, Mensual o Anual. Ve solo Ingresos o Egresos según necesites."),
        Triple("🎯", "Personaliza tu app",
            "Crea tus propias categorías, subcategorías y cuentas para adaptarla a ti.")
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_onboarding)
        mostrarPagina()
    }

    private fun mostrarPagina() {
        val (emoji, titulo, texto) = paginas[paginaActual]
        val total = paginas.size

        findViewById<TextView>(R.id.tvOnboardingEmoji).text  = emoji
        findViewById<TextView>(R.id.tvOnboardingTitulo).text = titulo
        findViewById<TextView>(R.id.tvOnboardingTexto).text  = texto
        findViewById<TextView>(R.id.tvOnboardingPagina).text = "${paginaActual + 1} / $total"

        val btnSiguiente = findViewById<Button>(R.id.btnOnboardingSiguiente)
        btnSiguiente.text = if (paginaActual == total - 1) "¡PERSONALIZAR!" else "Siguiente →"
        btnSiguiente.setOnClickListener {
            if (paginaActual < total - 1) {
                paginaActual++
                mostrarPagina()
            } else {
                irASetup()
            }
        }

        val btnSaltar = findViewById<Button>(R.id.btnOnboardingSaltar)
        btnSaltar.visibility = if (paginaActual == total - 1)
            android.view.View.INVISIBLE else android.view.View.VISIBLE
        btnSaltar.setOnClickListener { irASetup() }
    }

    private fun irASetup() {
        startActivity(Intent(this, SetupCategoriasActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        })
        finish()
    }
}