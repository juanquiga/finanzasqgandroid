package com.jdqg.finanzasqg

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.text.Editable
import android.text.InputType
import android.text.TextWatcher
import android.view.*
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import java.text.NumberFormat
import java.util.Locale

data class CategoriaSetup(
    var nombre: String = "",
    val subcategorias: MutableList<SubcategoriaSetup> = mutableListOf()
)
data class SubcategoriaSetup(
    var nombre: String = "",
    val subsubcategorias: MutableList<String> = mutableListOf()
)

class SetupCategoriasActivity : AppCompatActivity() {

    private val db   = FirebaseDatabase.getInstance().reference
    private val auth = FirebaseAuth.getInstance()

    private var seccion = "egresos"
    private var cargado = false

    private val egresosTree  = mutableListOf<CategoriaSetup>()
    private val ingresosTree = mutableListOf<CategoriaSetup>()
    private val cuentas      = mutableListOf<String>()

    private lateinit var btnEgresos     : Button
    private lateinit var btnIngresos    : Button
    private lateinit var btnCuentas     : Button
    private lateinit var container      : LinearLayout
    private lateinit var btnGuardar     : Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_setup_categorias)

        btnEgresos  = findViewById(R.id.btnSeccionEgresos)
        btnIngresos = findViewById(R.id.btnSeccionIngresos)
        btnCuentas  = findViewById(R.id.btnSeccionCuentas)
        container   = findViewById(R.id.containerSetup)
        btnGuardar  = findViewById(R.id.btnGuardarSetup)



        btnEgresos.setOnClickListener  { seccion = "egresos";  actualizarBotones(); renderizar() }
        btnIngresos.setOnClickListener { seccion = "ingresos"; actualizarBotones(); renderizar() }
        btnCuentas.setOnClickListener  { seccion = "cuentas";  actualizarBotones(); renderizar() }
        btnGuardar.setOnClickListener  { guardar() }

        if (savedInstanceState != null)
            seccion = savedInstanceState.getString("seccion", "egresos")

        if (!cargado) cargarDesdeFirebase()
        else { actualizarBotones(); renderizar() }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString("seccion", seccion)
    }

    private fun cargarDesdeFirebase() {
        val uid = auth.currentUser?.uid ?: run {
            cargarDefaults(); actualizarBotones(); renderizar(); return }
        db.child("configuracion").child(uid)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snap: DataSnapshot) {
                    egresosTree.clear()
                    parsearArbol(snap.child("egresos"), egresosTree)
                    if (egresosTree.isEmpty()) cargarEgresosDefault()

                    ingresosTree.clear()
                    parsearArbol(snap.child("ingresos"), ingresosTree)
                    if (ingresosTree.isEmpty()) cargarIngresosDefault()

                    cuentas.clear()
                    for (c in snap.child("cuentas").children)
                        c.getValue(String::class.java)?.let { cuentas.add(it) }
                    if (cuentas.isEmpty())
                        cuentas.addAll(listOf("Bancolombia","Nu","Nequi",
                            "Efectivo","Tarjeta crédito"))

                    cargado = true; actualizarBotones(); renderizar()
                }
                override fun onCancelled(e: DatabaseError) {
                    cargarDefaults(); cargado = true; actualizarBotones(); renderizar()
                }
            })
    }

    private fun cargarDefaults() {
        cargarEgresosDefault(); cargarIngresosDefault()
        if (cuentas.isEmpty())
            cuentas.addAll(listOf("Bancolombia","Nu","Nequi","Efectivo","Tarjeta crédito"))
    }

    private fun cargarEgresosDefault() {
        egresosTree.addAll(listOf(
            CategoriaSetup("Comida", mutableListOf(
                SubcategoriaSetup("Almuerzo", mutableListOf("Restaurante","Casa","Delivery")),
                SubcategoriaSetup("Mercado",  mutableListOf("Supermercado","Tienda")),
                SubcategoriaSetup("Cafetería",mutableListOf("Café","Snack")))),
            CategoriaSetup("Casa", mutableListOf(
                SubcategoriaSetup("Servicios",mutableListOf("Luz","Agua","Gas","Internet")),
                SubcategoriaSetup("Arriendo", mutableListOf()),
                SubcategoriaSetup("Mercado",  mutableListOf()))),
            CategoriaSetup("Transporte", mutableListOf(
                SubcategoriaSetup("Gasolina",          mutableListOf("Carro","Moto")),
                SubcategoriaSetup("Transporte público", mutableListOf("Bus","Metro")),
                SubcategoriaSetup("Taxi/Uber",          mutableListOf()))),
            CategoriaSetup("Salud", mutableListOf(
                SubcategoriaSetup("Medicamentos",mutableListOf()),
                SubcategoriaSetup("Consulta",   mutableListOf("Médico","Odontólogo")),
                SubcategoriaSetup("Gym",        mutableListOf()))),
            CategoriaSetup("Familia", mutableListOf(
                SubcategoriaSetup("Mesada",  mutableListOf()),
                SubcategoriaSetup("Salidas", mutableListOf("Restaurante","Cine")),
                SubcategoriaSetup("Mascotas",mutableListOf("Veterinario","Comida")))),
            CategoriaSetup("Ocio", mutableListOf(
                SubcategoriaSetup("Entretenimiento",mutableListOf("Streaming","Juegos")),
                SubcategoriaSetup("Viajes",         mutableListOf("Tiquetes","Hotel")),
                SubcategoriaSetup("Ropa",           mutableListOf())))
        ))
    }

    private fun cargarIngresosDefault() {
        ingresosTree.addAll(listOf(
            CategoriaSetup("Salario", mutableListOf(
                SubcategoriaSetup("Nómina",mutableListOf("Quincenal","Mensual")),
                SubcategoriaSetup("Prima", mutableListOf()))),
            CategoriaSetup("Negocio", mutableListOf(
                SubcategoriaSetup("Ventas",   mutableListOf()),
                SubcategoriaSetup("Servicios",mutableListOf()))),
            CategoriaSetup("Inmuebles", mutableListOf(
                SubcategoriaSetup("Arriendo",mutableListOf()),
                SubcategoriaSetup("Venta",   mutableListOf()))),
            CategoriaSetup("Otros", mutableListOf(
                SubcategoriaSetup("Regalo",  mutableListOf()),
                SubcategoriaSetup("Premio",  mutableListOf()),
                SubcategoriaSetup("Préstamo",mutableListOf())))
        ))
    }

    private fun parsearArbol(snap: DataSnapshot, destino: MutableList<CategoriaSetup>) {
        if (!snap.exists()) return
        for (cs in snap.children) {
            val nombre = cs.child("nombre").getValue(String::class.java) ?: continue
            val cat    = CategoriaSetup(nombre)
            for (ss in cs.child("subcategorias").children) {
                val sn  = ss.child("nombre").getValue(String::class.java) ?: continue
                val sub = SubcategoriaSetup(sn)
                for (sss in ss.child("subsubcategorias").children)
                    sss.getValue(String::class.java)?.let { sub.subsubcategorias.add(it) }
                cat.subcategorias.add(sub)
            }
            destino.add(cat)
        }
    }

    private fun actualizarBotones() {
        listOf(btnEgresos to "egresos", btnIngresos to "ingresos",
            btnCuentas to "cuentas")
            .forEach { (btn, s) ->
                val on = s == seccion
                btn.backgroundTintList = android.content.res.ColorStateList.valueOf(
                    if (on) Color.parseColor("#4CAF50") else Color.parseColor("#0D1A2A"))
                btn.setTextColor(
                    if (on) Color.parseColor("#0A1628") else Color.parseColor("#4CAF50"))
            }
    }

    private fun renderizar() {
        container.removeAllViews()
        when (seccion) {
            "egresos"  -> renderArbol(egresosTree)
            "ingresos" -> renderArbol(ingresosTree)
            "cuentas"  -> renderCuentas()
        }
    }

    private fun renderArbol(arbol: MutableList<CategoriaSetup>) {
        container.addView(botonAccion("＋  Agregar categoría") {
            arbol.add(CategoriaSetup("Nueva categoría")); renderizar() })
        arbol.forEachIndexed { catIdx, cat ->
            val card = card()
            val fC = filaH()
            fC.addView(inputTexto(cat.nombre, true) { cat.nombre = it })
            fC.addView(botonElim { arbol.removeAt(catIdx); renderizar() })
            card.addView(fC)
            cat.subcategorias.forEachIndexed { subIdx, sub ->
                card.addView(divider())
                val fS = filaH(dpToPx(16))
                fS.addView(tvArrow("›"))
                fS.addView(inputTexto(sub.nombre, false) { sub.nombre = it })
                fS.addView(botonX { cat.subcategorias.removeAt(subIdx); renderizar() })
                card.addView(fS)
                sub.subsubcategorias.forEachIndexed { ssIdx, ss ->
                    val fSS = filaH(dpToPx(32))
                    fSS.addView(tvArrow("»"))
                    fSS.addView(inputTexto(ss, false) { sub.subsubcategorias[ssIdx] = it })
                    fSS.addView(botonX { sub.subsubcategorias.removeAt(ssIdx); renderizar() })
                    card.addView(fSS)
                }
                card.addView(linkBtn("  ＋ sub-sub") {
                    sub.subsubcategorias.add("Nueva"); renderizar()
                }.apply { (layoutParams as LinearLayout.LayoutParams)
                    .setMargins(dpToPx(32),0,0,0) })
            }
            card.addView(linkBtn("  ＋ subcategoría") {
                cat.subcategorias.add(SubcategoriaSetup("Nueva subcategoría")); renderizar() })
            container.addView(card)
        }
    }

    private fun renderCuentas() {
        container.addView(botonAccion("＋  Agregar cuenta") {
            cuentas.add("Nueva cuenta"); renderizar() })
        cuentas.forEachIndexed { idx, cuenta ->
            val card = card(); val row = filaH()
            row.addView(inputTexto(cuenta, true) { cuentas[idx] = it })
            row.addView(botonElim { cuentas.removeAt(idx); renderizar() })
            card.addView(row); container.addView(card)
        }
    }

    private fun guardar() {
        btnGuardar.isEnabled = false; btnGuardar.text = "Guardando..."
        val uid = auth.currentUser?.uid ?: run {
            Toast.makeText(this, "Debes iniciar sesión", Toast.LENGTH_SHORT).show()
            btnGuardar.isEnabled = true; btnGuardar.text = "GUARDAR Y CONTINUAR"; return }

        val config = mapOf(
            "egresos"  to serArbol(egresosTree),
            "ingresos" to serArbol(ingresosTree),
            "cuentas"  to cuentas
        )

        db.child("configuracion").child(uid).updateChildren(config)
            .addOnSuccessListener {
                db.child("users").child(uid).child("isNewUser").setValue(false)
                    .addOnCompleteListener {
                        Toast.makeText(this, "✅ Categorías guardadas", Toast.LENGTH_SHORT).show()

                        val desdeOnboarding = intent.getBooleanExtra("desde_onboarding", false)
                        if (desdeOnboarding) {
                            // Paso 2 del onboarding: ir a configurar presupuesto
                            startActivity(Intent(this, PresupuestoActivity::class.java).apply {
                                putExtra("desde_onboarding", true)
                            })
                        } else {
                            // Desde configuración normal: volver a MainActivity
                            startActivity(Intent(this, MainActivity::class.java).apply {
                                flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or
                                        Intent.FLAG_ACTIVITY_SINGLE_TOP
                                putExtra("tab", "config")
                            })
                        }
                        finish()
                    }
            }
            .addOnFailureListener {
                btnGuardar.isEnabled = true; btnGuardar.text = "GUARDAR Y CONTINUAR"
                Toast.makeText(this, "Error: ${it.message}", Toast.LENGTH_LONG).show()
            }
    }

    private fun serArbol(a: List<CategoriaSetup>) = a.map { cat ->
        mapOf("nombre" to cat.nombre, "subcategorias" to cat.subcategorias.map { sub ->
            mapOf("nombre" to sub.nombre, "subsubcategorias" to sub.subsubcategorias) }) }

    private fun card() = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        background = android.graphics.drawable.GradientDrawable().apply {
            setColor(Color.parseColor("#0D1A2A"))
            cornerRadius = dpToPx(16).toFloat()
            setStroke(1, Color.parseColor("#1A3A4A"))
        }
        setPadding(dpToPx(16), dpToPx(14), dpToPx(16), dpToPx(14))
        layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT).apply { setMargins(0,0,0,dpToPx(12)) }
    }

    private fun filaH(marginStart: Int = 0) = LinearLayout(this).apply {
        orientation = LinearLayout.HORIZONTAL
        gravity = android.view.Gravity.CENTER_VERTICAL
        layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT).apply {
            setMargins(marginStart, dpToPx(4), 0, dpToPx(4)) }
    }

    private fun inputTexto(valor: String, grande: Boolean, onChange: (String) -> Unit) =
        EditText(this).apply {
            setText(valor); setTextColor(Color.WHITE)
            textSize = if (grande) 15f else 13f
            if (grande) setTypeface(typeface, android.graphics.Typeface.BOLD)
            setBackgroundResource(R.drawable.finanzas_input_bg)
            setPadding(dpToPx(12), dpToPx(10), dpToPx(12), dpToPx(10))
            layoutParams = LinearLayout.LayoutParams(0,
                if (grande) dpToPx(46) else dpToPx(40), 1f)
            addTextChangedListener(object : TextWatcher {
                override fun afterTextChanged(s: Editable?) { onChange(s.toString()) }
                override fun beforeTextChanged(s: CharSequence?, st: Int, c: Int, a: Int) {}
                override fun onTextChanged(s: CharSequence?, st: Int, b: Int, c: Int) {}
            })
        }

    private fun botonAccion(label: String, onClick: () -> Unit) = Button(this).apply {
        text = label; setTextColor(Color.parseColor("#4CAF50"))
        background = android.graphics.drawable.GradientDrawable().apply {
            setColor(Color.parseColor("#0D2A1A"))
            cornerRadius = dpToPx(12).toFloat()
            setStroke(1, Color.parseColor("#1A4A2A"))
        }
        textSize = 13f; isAllCaps = false
        layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, dpToPx(48)).apply {
            setMargins(0,0,0,dpToPx(10)) }
        setOnClickListener { onClick() }
    }

    private fun botonElim(onClick: () -> Unit) = Button(this).apply {
        text = "🗑"; textSize = 15f; isAllCaps = false
        background = android.graphics.drawable.GradientDrawable().apply {
            setColor(Color.parseColor("#2A0A0A")); cornerRadius = dpToPx(10).toFloat() }
        layoutParams = LinearLayout.LayoutParams(dpToPx(44), dpToPx(44)).apply {
            setMargins(dpToPx(8),0,0,0) }
        setOnClickListener { onClick() }
    }

    private fun botonX(onClick: () -> Unit) = Button(this).apply {
        text = "✕"; textSize = 11f; isAllCaps = false
        setTextColor(Color.parseColor("#FF5252"))
        background = android.graphics.drawable.GradientDrawable().apply {
            setColor(Color.parseColor("#2A0A0A")); cornerRadius = dpToPx(8).toFloat() }
        layoutParams = LinearLayout.LayoutParams(dpToPx(34), dpToPx(34)).apply {
            setMargins(dpToPx(4),0,0,0) }
        setOnClickListener { onClick() }
    }

    private fun linkBtn(label: String, onClick: () -> Unit) = Button(this).apply {
        text = label; textSize = 12f; isAllCaps = false
        setTextColor(Color.parseColor("#4CAF50"))
        setBackgroundColor(Color.TRANSPARENT)
        layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT, dpToPx(36)).apply {
            setMargins(dpToPx(8),0,0,0) }
        setOnClickListener { onClick() }
    }

    private fun tvArrow(txt: String) = TextView(this).apply {
        text = txt; setTextColor(Color.parseColor("#4CAF50")); textSize = 20f
        layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT,
            LinearLayout.LayoutParams.WRAP_CONTENT).apply {
            setMargins(0,0,dpToPx(8),0)
            gravity = android.view.Gravity.CENTER_VERTICAL }
    }

    private fun divider() = View(this).apply {
        layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, 1).apply {
            setMargins(0, dpToPx(6), 0, dpToPx(6)) }
        setBackgroundColor(Color.parseColor("#1A3A4A"))
    }

    private fun dpToPx(dp: Int) = (dp * resources.displayMetrics.density).toInt()
}