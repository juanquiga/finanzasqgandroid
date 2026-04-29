package com.jdqg.finanzasqg

import android.content.Context
import android.content.res.Configuration
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.fragment.app.Fragment
import com.google.android.material.bottomnavigation.BottomNavigationView
import java.util.Locale

class MainActivity : AppCompatActivity() {

    companion object {
        fun aplicarIdioma(context: Context): Context {
            val prefs  = context.getSharedPreferences("finanzasqg_prefs", Context.MODE_PRIVATE)
            val lang   = prefs.getString("idioma", "es") ?: "es"
            val locale = Locale(lang)
            Locale.setDefault(locale)
            val config = Configuration(context.resources.configuration)
            config.setLocale(locale)
            return context.createConfigurationContext(config)
        }
    }

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(aplicarIdioma(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        val prefs      = getSharedPreferences("finanzasqg_prefs", Context.MODE_PRIVATE)
        val modoOscuro = prefs.getBoolean("modo_oscuro", true)
        AppCompatDelegate.setDefaultNightMode(
            if (modoOscuro) AppCompatDelegate.MODE_NIGHT_YES
            else            AppCompatDelegate.MODE_NIGHT_NO
        )

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val bottomNav = findViewById<BottomNavigationView>(R.id.finanzasBottomNav)

        // Si viene de SetupCategoriasActivity con tab="config", abrir Configuración
        val tabExtra = intent.getStringExtra("tab")
        val fragmentoInicial: Fragment = if (tabExtra == "config") {
            bottomNav.selectedItemId = R.id.nav_cuenta
            CuentaFragment()
        } else {
            ResumenFragment()
        }

        cargarFragmento(fragmentoInicial)

        bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_resumen   -> cargarFragmento(ResumenFragment())
                R.id.nav_registrar -> cargarFragmento(RegistrarFragment())
                R.id.nav_graficas  -> cargarFragmento(GraficasFragment())
                R.id.nav_cuenta    -> cargarFragmento(CuentaFragment())
            }
            true
        }
    }

    private fun cargarFragmento(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.finanzasContainer, fragment)
            .commit()
    }
}