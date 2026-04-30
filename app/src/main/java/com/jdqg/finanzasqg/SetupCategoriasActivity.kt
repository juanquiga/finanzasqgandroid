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

    private var seccion       = "egresos"
    private var editandoValor = false
    private var cargado       = false

    private val egresosTree  = mutableListOf<CategoriaSetup>()
    private val ingresosTree = mutableListOf<CategoriaSetup>()
    private val cuentas      = mutableListOf<String>()

    private val maximosCategorias = mutableMapOf<String, MutableMap<String, Double>>()
    private var presupuestoMaxCOP = 0.0
    private var presupuestoMaxUSD = 0.0

    private lateinit var btnEgresos     : Button
    private lateinit var btnIngresos    : Button
    private lateinit var btnCuentas     : Button
    private lateinit var btnPresupuesto : Button
    private lateinit var container      : LinearLayout
    private lateinit var btnGuardar     : Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_setup_categorias)

        btnEgresos     = findViewById(R.id.btnSeccionEgresos)
        btnIngresos    = findViewById(R.id.btnSeccionIngresos)
        btnCuentas     = findViewById(R.id.btnSeccionCuentas)
        btnPresupuesto = findViewById(R.id.btnSeccionPresupuesto)
        container      = findViewById(R.id.containerSetup)
        btnGuardar     = findViewById(R.id.btnGuardarSetup)

        btnEgresos.setOnClickListener     { seccion = "egresos";     actualizarBotones(); renderizar() }
        btnIngresos.setOnClickListener    { seccion = "ingresos";    actualizarBotones(); renderizar() }
        btnCuentas.setOnClickListener     { seccion = "cuentas";     actualizarBotones(); renderizar() }
        btnPresupuesto.setOnClickListener { seccion = "presupuesto"; actualizarBotones(); renderizar() }
        btnGuardar.setOnClickListener     { guardar() }

        if (savedInstanceState != null) {
            seccion = savedInstanceState.getString("seccion", "egresos")
        }

        if (!cargado) cargarDesdeFirebase()
        else { actualizarBotones(); renderizar() }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString("seccion", seccion)
    }

    private fun cargarDesdeFirebase() {
        val uid = auth.currentUser?.uid ?: run {
            cargarDefaults(); actualizarBotones(); renderizar(); return
        }
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
                    for (c in snap.child("cuentas").children) {
                        c.getValue(String::class.java)?.let { cuentas.add(it) }
                    }
                    if (cuentas.isEmpty())
                        cuentas.addAll(listOf("Bancolombia","Nu","Nequi",
                            "Efectivo","Tarjeta crédito"))

                    presupuestoMaxCOP = snap.child("presupuestoMax")
                        .child("max_COP").getValue(Double::class.java) ?: 0.0
                    presupuestoMaxUSD = snap.child("presupuestoMax")
                        .child("max_USD").getValue(Double::class.java) ?: 0.0

                    maximosCategorias.clear()
                    val snapCat = if (snap.child("presupuestoCategorias").exists())
                        snap.child("presupuestoCategorias")
                    else snap.child("maximosCategorias")
                    for (s in snapCat.children) {
                        val cat = s.key ?: continue
                        val m   = mutableMapOf<String, Double>()
                        m["max_COP"] = s.child("max_COP").getValue(Double::class.java) ?: 0.0
                        m["max_USD"] = s.child("max_USD").getValue(Double::class.java) ?: 0.0
                        maximosCategorias[cat] = m
                    }

                    cargado = true
                    actualizarBotones()
                    renderizar()
                }
                override fun onCancelled(e: DatabaseError) {
                    cargarDefaults(); cargado = true
                    actualizarBotones(); renderizar()
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
                for (sss in ss.child("subsubcategorias").children) {
                    sss.getValue(String::class.java)?.let { sub.subsubcategorias.add(it) }
                }
                cat.subcategorias.add(sub)
            }
            destino.add(cat)
        }
    }

    private fun actualizarBotones() {
        listOf(btnEgresos to "egresos", btnIngresos to "ingresos",
            btnCuentas to "cuentas", btnPresupuesto to "presupuesto")
            .forEach { (btn, s) ->
                val on = s == seccion
                btn.backgroundTintList = android.content.res.ColorStateList.valueOf(
                    if (on) Color.parseColor("#4CAF50") else Color.parseColor("#0D1A2A"))
                btn.setTextColor(
                    if (on) Color.parseColor("#050510") else Color.parseColor("#4CAF50"))
            }
    }

    private fun renderizar() {
        container.removeAllViews()
        when (seccion) {
            "egresos"     -> renderArbol(egresosTree)
            "ingresos"    -> renderArbol(ingresosTree)
            "cuentas"     -> renderCuentas()
            "presupuesto" -> renderPresupuesto()
        }
    }

    // ── ÁRBOL ─────────────────────────────────────────────────────────────────
    private fun renderArbol(arbol: MutableList<CategoriaSetup>) {
        container.addView(botonAccion("+ Agregar categoría") {
            arbol.add(CategoriaSetup("Nueva categoría")); renderizar()
        })
        arbol.forEachIndexed { catIdx, cat ->
            val card = card()
            val filaC = filaH()
            filaC.addView(inputTexto(cat.nombre, true) { cat.nombre = it })
            filaC.addView(botonElim { arbol.removeAt(catIdx); renderizar() })
            card.addView(filaC)

            cat.subcategorias.forEachIndexed { subIdx, sub ->
                card.addView(divider())
                val filaSub = filaH(dpToPx(16))
                filaSub.addView(tvArrow("›"))
                filaSub.addView(inputTexto(sub.nombre, false) { sub.nombre = it })
                filaSub.addView(botonX { cat.subcategorias.removeAt(subIdx); renderizar() })
                card.addView(filaSub)

                sub.subsubcategorias.forEachIndexed { ssIdx, ss ->
                    val filaSS = filaH(dpToPx(32))
                    filaSS.addView(tvArrow("»"))
                    filaSS.addView(inputTexto(ss, false) { sub.subsubcategorias[ssIdx] = it })
                    filaSS.addView(botonX { sub.subsubcategorias.removeAt(ssIdx); renderizar() })
                    card.addView(filaSS)
                }
                card.addView(linkBtn("  + sub-sub") {
                    sub.subsubcategorias.add("Nueva"); renderizar()
                }.apply {
                    (layoutParams as LinearLayout.LayoutParams)
                        .setMargins(dpToPx(32), 0, 0, 0)
                })
            }
            card.addView(linkBtn("  + subcategoría") {
                cat.subcategorias.add(SubcategoriaSetup("Nueva subcategoría")); renderizar()
            })
            container.addView(card)
        }
    }

    // ── CUENTAS ───────────────────────────────────────────────────────────────
    private fun renderCuentas() {
        container.addView(botonAccion("+ Agregar cuenta") {
            cuentas.add("Nueva cuenta"); renderizar()
        })
        cuentas.forEachIndexed { idx, cuenta ->
            val card = card()
            val row  = filaH()
            row.addView(inputTexto(cuenta, true) { cuentas[idx] = it })
            row.addView(botonElim { cuentas.removeAt(idx); renderizar() })
            card.addView(row)
            container.addView(card)
        }
    }

    // ── PRESUPUESTO ────────────────────────────────────────────────────────────
    private fun renderPresupuesto() {
        val cardGen = card()
        cardGen.addView(tvSeccion("💳 Presupuesto máximo mensual"))
        cardGen.addView(tvHint("¿Cuánto quieres gastar en total al mes?"))
        cardGen.addView(tvLabel("Presupuesto COP ($)"))
        cardGen.addView(etNumero(
            if (presupuestoMaxCOP > 0) fmtNum(presupuestoMaxCOP.toLong()) else ""
        ) { presupuestoMaxCOP = it })
        cardGen.addView(tvLabel("Presupuesto USD"))
        cardGen.addView(etNumeroUSD(
            if (presupuestoMaxUSD > 0) presupuestoMaxUSD.toString() else ""
        ) { presupuestoMaxUSD = it })
        container.addView(cardGen)

        val cardCat = card()
        cardCat.addView(tvSeccion("🎯 Presupuesto por categoría"))
        cardCat.addView(tvHint("Configura límites por categoría, subcategoría y sub-sub"))

        egresosTree.forEach { cat ->
            val maxCat = maximosCategorias.getOrPut(cat.nombre) { mutableMapOf() }
            cardCat.addView(divider())

            // Nombre de categoría
            cardCat.addView(TextView(this).apply {
                text = "📂 ${cat.nombre}"
                setTextColor(Color.WHITE); textSize = 14f
                setTypeface(typeface, android.graphics.Typeface.BOLD)
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                    setMargins(0, dpToPx(8), 0, dpToPx(4)) }
            })

            // COP categoría
            val rCOP = filaH()
            rCOP.addView(TextView(this).apply {
                text = "COP"; setTextColor(Color.parseColor("#4CAF50")); textSize = 12f
                layoutParams = LinearLayout.LayoutParams(dpToPx(44),
                    LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                    gravity = android.view.Gravity.CENTER_VERTICAL } })
            rCOP.addView(etNumero(
                if ((maxCat["max_COP"] ?: 0.0) > 0)
                    fmtNum((maxCat["max_COP"] ?: 0.0).toLong()) else ""
            ) { maxCat["max_COP"] = it })
            cardCat.addView(rCOP)

            // USD categoría
            val rUSD = filaH()
            rUSD.addView(TextView(this).apply {
                text = "USD"; setTextColor(Color.parseColor("#2196F3")); textSize = 12f
                layoutParams = LinearLayout.LayoutParams(dpToPx(44),
                    LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                    gravity = android.view.Gravity.CENTER_VERTICAL } })
            rUSD.addView(etNumeroUSD(
                if ((maxCat["max_USD"] ?: 0.0) > 0)
                    (maxCat["max_USD"] ?: 0.0).toString() else ""
            ) { maxCat["max_USD"] = it })
            cardCat.addView(rUSD)

            // Subcategorías
            cat.subcategorias.forEach { sub ->
                val claveSub = "${cat.nombre} > ${sub.nombre}"
                val maxSub   = maximosCategorias.getOrPut(claveSub) { mutableMapOf() }

                cardCat.addView(TextView(this).apply {
                    text = "   📁 ${sub.nombre}"
                    setTextColor(Color.parseColor("#AABBCC")); textSize = 12f
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                        setMargins(dpToPx(16), dpToPx(6), 0, dpToPx(2)) } })

                val sSubCOP = filaH(dpToPx(16))
                sSubCOP.addView(TextView(this).apply {
                    text = "COP"; setTextColor(Color.parseColor("#4CAF50")); textSize = 11f
                    layoutParams = LinearLayout.LayoutParams(dpToPx(44),
                        LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                        gravity = android.view.Gravity.CENTER_VERTICAL } })
                sSubCOP.addView(etNumero(
                    if ((maxSub["max_COP"] ?: 0.0) > 0)
                        fmtNum((maxSub["max_COP"] ?: 0.0).toLong()) else ""
                ) { maxSub["max_COP"] = it })
                cardCat.addView(sSubCOP)

                val sSubUSD = filaH(dpToPx(16))
                sSubUSD.addView(TextView(this).apply {
                    text = "USD"; setTextColor(Color.parseColor("#2196F3")); textSize = 11f
                    layoutParams = LinearLayout.LayoutParams(dpToPx(44),
                        LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                        gravity = android.view.Gravity.CENTER_VERTICAL } })
                sSubUSD.addView(etNumeroUSD(
                    if ((maxSub["max_USD"] ?: 0.0) > 0)
                        (maxSub["max_USD"] ?: 0.0).toString() else ""
                ) { maxSub["max_USD"] = it })
                cardCat.addView(sSubUSD)

                // Subsubcategorías
                sub.subsubcategorias.forEach { ss ->
                    val claveSS = "${cat.nombre} > ${sub.nombre} > $ss"
                    val maxSS   = maximosCategorias.getOrPut(claveSS) { mutableMapOf() }

                    cardCat.addView(TextView(this).apply {
                        text = "      📌 $ss"
                        setTextColor(Color.parseColor("#778899")); textSize = 11f
                        layoutParams = LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.MATCH_PARENT,
                            LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                            setMargins(dpToPx(32), dpToPx(4), 0, dpToPx(2)) } })

                    val sssCOP = filaH(dpToPx(32))
                    sssCOP.addView(TextView(this).apply {
                        text = "COP"; setTextColor(Color.parseColor("#4CAF50")); textSize = 10f
                        layoutParams = LinearLayout.LayoutParams(dpToPx(44),
                            LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                            gravity = android.view.Gravity.CENTER_VERTICAL } })
                    sssCOP.addView(etNumero(
                        if ((maxSS["max_COP"] ?: 0.0) > 0)
                            fmtNum((maxSS["max_COP"] ?: 0.0).toLong()) else ""
                    ) { maxSS["max_COP"] = it })
                    cardCat.addView(sssCOP)

                    val sssUSD = filaH(dpToPx(32))
                    sssUSD.addView(TextView(this).apply {
                        text = "USD"; setTextColor(Color.parseColor("#2196F3")); textSize = 10f
                        layoutParams = LinearLayout.LayoutParams(dpToPx(44),
                            LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                            gravity = android.view.Gravity.CENTER_VERTICAL } })
                    sssUSD.addView(etNumeroUSD(
                        if ((maxSS["max_USD"] ?: 0.0) > 0)
                            (maxSS["max_USD"] ?: 0.0).toString() else ""
                    ) { maxSS["max_USD"] = it })
                    cardCat.addView(sssUSD)
                }
            }
        }
        container.addView(cardCat)
    }

    // ── GUARDAR — navega a MainActivity (pestaña Configuración) al terminar ───
    private fun guardar() {
        btnGuardar.isEnabled = false
        btnGuardar.text      = "Guardando..."

        val uid = auth.currentUser?.uid ?: run {
            Toast.makeText(this, "Debes iniciar sesión", Toast.LENGTH_SHORT).show()
            btnGuardar.isEnabled = true
            btnGuardar.text      = "GUARDAR Y CONTINUAR"
            return
        }

        val presupCatSer = maximosCategorias
            .filter { (_, v) -> (v["max_COP"] ?: 0.0) > 0 || (v["max_USD"] ?: 0.0) > 0 }
            .mapValues { (_, v) ->
                mapOf(
                    "max_COP" to (v["max_COP"] ?: 0.0),
                    "max_USD" to (v["max_USD"] ?: 0.0)
                )
            }

        val config = mapOf(
            "egresos"               to serArbol(egresosTree),
            "ingresos"              to serArbol(ingresosTree),
            "cuentas"               to cuentas,
            "presupuestoMax"        to mapOf(
                "max_COP" to presupuestoMaxCOP,
                "max_USD" to presupuestoMaxUSD),
            "presupuestoCategorias" to presupCatSer,
            "maximosCategorias"     to presupCatSer   // compatibilidad iOS
        )

        // Guardar en Firebase bajo el UID del usuario actual
        db.child("configuracion").child(uid).setValue(config)
            .addOnSuccessListener {
                // isNewUser = false para que no vuelva al onboarding
                db.child("users").child(uid).child("isNewUser").setValue(false)
                    .addOnCompleteListener {
                        Toast.makeText(this,
                            "✅ Configuración guardada", Toast.LENGTH_SHORT).show()

                        // Volver a MainActivity con la pestaña de Configuración activa
                        val intent = Intent(this, MainActivity::class.java).apply {
                            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or
                                    Intent.FLAG_ACTIVITY_SINGLE_TOP
                            // Extra para que MainActivity abra la pestaña Config
                            putExtra("tab", "config")
                        }
                        startActivity(intent)
                        finish()
                    }
            }
            .addOnFailureListener { e ->
                btnGuardar.isEnabled = true
                btnGuardar.text      = "GUARDAR Y CONTINUAR"
                Toast.makeText(this,
                    "Error al guardar: ${e.message}", Toast.LENGTH_LONG).show()
            }
    }

    private fun serArbol(a: List<CategoriaSetup>) = a.map { cat ->
        mapOf("nombre" to cat.nombre,
            "subcategorias" to cat.subcategorias.map { sub ->
                mapOf("nombre" to sub.nombre,
                    "subsubcategorias" to sub.subsubcategorias)
            })
    }

    // ── UI HELPERS ─────────────────────────────────────────────────────────────
    private fun card() = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        setBackgroundResource(R.drawable.finanzas_card_background)
        setPadding(dpToPx(14), dpToPx(12), dpToPx(14), dpToPx(12))
        layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT).apply {
            setMargins(0, 0, 0, dpToPx(10)) }
    }

    private fun filaH(marginStart: Int = 0) = LinearLayout(this).apply {
        orientation = LinearLayout.HORIZONTAL
        gravity     = android.view.Gravity.CENTER_VERTICAL
        layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT).apply {
            setMargins(marginStart, dpToPx(4), 0, dpToPx(4)) }
    }

    private fun inputTexto(valor: String, grande: Boolean, onChange: (String) -> Unit) =
        EditText(this).apply {
            setText(valor)
            setTextColor(Color.WHITE)
            textSize = if (grande) 15f else 13f
            if (grande) setTypeface(typeface, android.graphics.Typeface.BOLD)
            setBackgroundResource(R.drawable.finanzas_input_bg)
            setPadding(dpToPx(10), dpToPx(8), dpToPx(10), dpToPx(8))
            layoutParams = LinearLayout.LayoutParams(0,
                if (grande) dpToPx(44) else dpToPx(38), 1f)
            addTextChangedListener(simpleWatcher { onChange(it) })
        }

    private fun etNumero(valorInicial: String, onChange: (Double) -> Unit) =
        EditText(this).apply {
            setText(valorInicial)
            hint = "0"
            setHintTextColor(Color.parseColor("#334455"))
            setTextColor(Color.WHITE)
            textSize  = 14f
            inputType = InputType.TYPE_CLASS_NUMBER
            setBackgroundResource(R.drawable.finanzas_input_bg)
            setPadding(dpToPx(10), dpToPx(8), dpToPx(10), dpToPx(8))
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dpToPx(42)).apply {
                setMargins(0, dpToPx(4), 0, dpToPx(4)) }
            addTextChangedListener(object : TextWatcher {
                override fun afterTextChanged(s: Editable?) {
                    if (editandoValor) return
                    editandoValor = true
                    val limpio = s.toString().replace(".", "").replace(",", "")
                    val num    = limpio.toLongOrNull() ?: 0L
                    onChange(num.toDouble())
                    if (num > 0) {
                        val f = NumberFormat.getNumberInstance(Locale("es","CO")).apply {
                            maximumFractionDigits = 0 }
                        setText(f.format(num))
                        setSelection(text.length)
                    }
                    editandoValor = false
                }
                override fun beforeTextChanged(s: CharSequence?, st: Int, c: Int, a: Int) {}
                override fun onTextChanged(s: CharSequence?, st: Int, b: Int, c: Int) {}
            })
        }

    private fun etNumeroUSD(valorInicial: String, onChange: (Double) -> Unit) =
        EditText(this).apply {
            setText(valorInicial)
            hint = "0 USD"
            setHintTextColor(Color.parseColor("#334455"))
            setTextColor(Color.WHITE)
            textSize  = 14f
            inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL
            setBackgroundResource(R.drawable.finanzas_input_bg)
            setPadding(dpToPx(10), dpToPx(8), dpToPx(10), dpToPx(8))
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dpToPx(42)).apply {
                setMargins(0, dpToPx(4), 0, dpToPx(8)) }
            addTextChangedListener(simpleWatcher { onChange(it.toDoubleOrNull() ?: 0.0) })
        }

    private fun botonAccion(label: String, onClick: () -> Unit) = Button(this).apply {
        text = label; setTextColor(Color.parseColor("#4CAF50"))
        backgroundTintList = android.content.res.ColorStateList.valueOf(
            Color.parseColor("#0D2A1A"))
        textSize = 13f; isAllCaps = false
        layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, dpToPx(46)).apply {
            setMargins(0, 0, 0, dpToPx(8)) }
        setOnClickListener { onClick() }
    }

    private fun botonElim(onClick: () -> Unit) = Button(this).apply {
        text = "🗑"; textSize = 14f; isAllCaps = false
        backgroundTintList = android.content.res.ColorStateList.valueOf(
            Color.parseColor("#4A1010"))
        layoutParams = LinearLayout.LayoutParams(dpToPx(44), dpToPx(44)).apply {
            setMargins(dpToPx(6), 0, 0, 0) }
        setOnClickListener { onClick() }
    }

    private fun botonX(onClick: () -> Unit) = Button(this).apply {
        text = "✕"; textSize = 11f; isAllCaps = false
        backgroundTintList = android.content.res.ColorStateList.valueOf(
            Color.parseColor("#4A1010"))
        layoutParams = LinearLayout.LayoutParams(dpToPx(34), dpToPx(34)).apply {
            setMargins(dpToPx(4), 0, 0, 0) }
        setOnClickListener { onClick() }
    }

    private fun linkBtn(label: String, onClick: () -> Unit) = Button(this).apply {
        text = label; textSize = 12f; isAllCaps = false
        setTextColor(Color.parseColor("#4CAF50"))
        backgroundTintList = android.content.res.ColorStateList.valueOf(Color.TRANSPARENT)
        layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT, dpToPx(34)).apply {
            setMargins(dpToPx(8), 0, 0, 0) }
        setOnClickListener { onClick() }
    }

    private fun tvArrow(txt: String) = TextView(this).apply {
        text = txt; setTextColor(Color.parseColor("#4CAF50")); textSize = 18f
        layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT,
            LinearLayout.LayoutParams.WRAP_CONTENT).apply {
            setMargins(0, 0, dpToPx(6), 0)
            gravity = android.view.Gravity.CENTER_VERTICAL }
    }

    private fun divider() = View(this).apply {
        layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, 1).apply {
            setMargins(0, dpToPx(4), 0, dpToPx(4)) }
        setBackgroundColor(Color.parseColor("#1E3A4A"))
    }

    private fun tvSeccion(txt: String) = TextView(this).apply {
        text = txt; setTextColor(Color.parseColor("#4CAF50"))
        textSize = 15f; setTypeface(typeface, android.graphics.Typeface.BOLD)
        layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT).apply {
            setMargins(0, 0, 0, dpToPx(4)) }
    }

    private fun tvHint(txt: String) = TextView(this).apply {
        text = txt; setTextColor(Color.parseColor("#445566")); textSize = 12f
        layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT).apply {
            setMargins(0, 0, 0, dpToPx(12)) }
    }

    private fun tvLabel(txt: String) = TextView(this).apply {
        text = txt; setTextColor(Color.parseColor("#4CAF50"))
        textSize = 12f; setTypeface(typeface, android.graphics.Typeface.BOLD)
        layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT).apply {
            setMargins(0, dpToPx(8), 0, dpToPx(4)) }
    }

    private fun fmtNum(n: Long): String {
        if (n == 0L) return ""
        return NumberFormat.getNumberInstance(Locale("es","CO")).apply {
            maximumFractionDigits = 0 }.format(n)
    }

    private fun simpleWatcher(cb: (String) -> Unit) = object : TextWatcher {
        override fun afterTextChanged(s: Editable?) { cb(s.toString()) }
        override fun beforeTextChanged(s: CharSequence?, st: Int, c: Int, a: Int) {}
        override fun onTextChanged(s: CharSequence?, st: Int, b: Int, c: Int) {}
    }

    private fun dpToPx(dp: Int) = (dp * resources.displayMetrics.density).toInt()
}