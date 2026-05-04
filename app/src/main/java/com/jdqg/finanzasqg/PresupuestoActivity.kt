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

data class SubsubPresupuesto(
    var nombre: String = "",
    var presupuesto: Double = 0.0
)
data class SubPresupuesto(
    var nombre: String = "",
    var presupuesto: Double = 0.0,
    val subsubcategorias: MutableList<SubsubPresupuesto> = mutableListOf()
) {
    val presupuestoTotal: Double get() =
        if (subsubcategorias.isEmpty()) presupuesto
        else subsubcategorias.sumOf { it.presupuesto }.let {
            if (it > 0) it else presupuesto
        }
}
data class CatPresupuesto(
    var nombre: String = "",
    var presupuesto: Double = 0.0,
    val subcategorias: MutableList<SubPresupuesto> = mutableListOf()
) {
    val presupuestoTotal: Double get() =
        if (subcategorias.isEmpty()) presupuesto
        else subcategorias.sumOf { it.presupuestoTotal }.let {
            if (it > 0) it else presupuesto
        }
}

class PresupuestoActivity : AppCompatActivity() {

    private val db   = FirebaseDatabase.getInstance().reference
    private val auth = FirebaseAuth.getInstance()

    private var presupuestoMaxCOP = 0.0
    private var presupuestoMaxUSD = 0.0
    private val categorias = mutableListOf<CatPresupuesto>()

    private var catExpandida = ""
    private var subExpandida = ""

    private lateinit var container  : LinearLayout
    private lateinit var btnGuardar : Button
    private lateinit var tvTotalGen : TextView

    private val tvTotCat = mutableMapOf<String, TextView>()
    private val tvTotSub = mutableMapOf<String, TextView>()

    // Referencias al campo general para actualizar automáticamente
    private var etPresupGenCOP: EditText? = null
    private var etPresupGenUSD: EditText? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_presupuesto)

        container  = findViewById(R.id.containerPresupuesto)
        btnGuardar = findViewById(R.id.btnGuardarPresupuesto)
        tvTotalGen = findViewById(R.id.tvTotalGeneralPresupuesto)

        findViewById<ImageButton>(R.id.btnVolverPresupuesto)
            .setOnClickListener { finish() }

        btnGuardar.setOnClickListener { guardar() }

        cargarDatos()
    }

    private fun cargarDatos() {
        val uid = auth.currentUser?.uid ?: return
        db.child("configuracion").child(uid)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snap: DataSnapshot) {
                    presupuestoMaxCOP = snap.child("presupuestoMax")
                        .child("max_COP").getValue(Double::class.java) ?: 0.0
                    presupuestoMaxUSD = snap.child("presupuestoMax")
                        .child("max_USD").getValue(Double::class.java) ?: 0.0

                    categorias.clear()
                    for (cs in snap.child("egresos").children) {
                        val catNombre = cs.child("nombre")
                            .getValue(String::class.java) ?: continue
                        val cat = CatPresupuesto(catNombre)
                        for (ss in cs.child("subcategorias").children) {
                            val subNombre = ss.child("nombre")
                                .getValue(String::class.java) ?: continue
                            val sub = SubPresupuesto(subNombre)
                            for (sss in ss.child("subsubcategorias").children) {
                                val ssNombre = sss.getValue(String::class.java) ?: continue
                                sub.subsubcategorias.add(SubsubPresupuesto(ssNombre))
                            }
                            cat.subcategorias.add(sub)
                        }
                        categorias.add(cat)
                    }

                    // Cargar presupuestos guardados
                    val snapP = if (snap.child("presupuestoCategorias").exists())
                        snap.child("presupuestoCategorias")
                    else snap.child("maximosCategorias")

                    for (s in snapP.children) {
                        val clave = s.key ?: continue
                        val val_ = s.child("max_COP")
                            .getValue(Double::class.java) ?: 0.0

                        val partes = clave.split(" > ")
                        when (partes.size) {
                            1 -> categorias.find { it.nombre == partes[0] }
                                ?.presupuesto = val_
                            2 -> categorias.find { it.nombre == partes[0] }
                                ?.subcategorias?.find { it.nombre == partes[1] }
                                ?.presupuesto = val_
                            3 -> categorias.find { it.nombre == partes[0] }
                                ?.subcategorias?.find { it.nombre == partes[1] }
                                ?.subsubcategorias?.find { it.nombre == partes[2] }
                                ?.presupuesto = val_
                        }
                    }

                    renderizar()
                }
                override fun onCancelled(e: DatabaseError) { renderizar() }
            })
    }

    private fun totalGeneral(): Double = categorias.sumOf { it.presupuestoTotal }

    // ── FIX PRINCIPAL: actualiza totales SIN re-renderizar ni perder cursor ────
    private fun actualizarTotalesEnPantalla() {
        val total = totalGeneral()

        // Actualizar texto del header
        tvTotalGen.text = if (total > 0) "Total presupuestado: ${fmt(total)}" else ""

        // Actualizar totales de categorías y subcategorías
        categorias.forEach { cat ->
            tvTotCat[cat.nombre]?.text =
                if (cat.presupuestoTotal > 0) fmt(cat.presupuestoTotal) else ""
            cat.subcategorias.forEach { sub ->
                tvTotSub["${cat.nombre}|${sub.nombre}"]?.text =
                    if (sub.presupuestoTotal > 0) fmt(sub.presupuestoTotal) else ""
            }
        }

        // ── ACTUALIZAR CAMPO GENERAL AUTOMÁTICAMENTE ──
        // Si presupuestoMaxCOP == 0 (sin valor manual), se muestra el total calculado
        // como hint y se sincroniza el valor interno, SIN llamar setText()
        if (presupuestoMaxCOP <= 0 && total > 0) {
            presupuestoMaxCOP = total   // sincronizar valor interno
            etPresupGenCOP?.hint = "Auto: ${fmt(total)}"
        } else if (presupuestoMaxCOP <= 0 && total == 0.0) {
            etPresupGenCOP?.hint = "0 COP"
        }
    }

    private fun renderizar() {
        tvTotCat.clear(); tvTotSub.clear()
        etPresupGenCOP = null; etPresupGenUSD = null
        container.removeAllViews()

        // Banner info
        container.addView(bannerInfo())

        // Presupuesto general
        val cardGen = card()
        cardGen.addView(seccionHeader("💳  Presupuesto general mensual"))
        cardGen.addView(tvHint("Máximo total que quieres gastar al mes"))

        cardGen.addView(tvLabel("COP  ($)"))
        val totalAuto = totalGeneral()
        val etCOP = etLibre(
            if (presupuestoMaxCOP > 0) presupuestoMaxCOP else 0.0,
            if (totalAuto > 0) "Auto: ${fmt(totalAuto)}" else "0 COP"
        ) { v ->
            presupuestoMaxCOP = v
            // Si borra el valor, volver al automático
            if (v <= 0) {
                presupuestoMaxCOP = totalGeneral()
                etPresupGenCOP?.hint = if (totalGeneral() > 0)
                    "Auto: ${fmt(totalGeneral())}" else "0 COP"
            }
        }
        etPresupGenCOP = etCOP
        cardGen.addView(etCOP)

        cardGen.addView(tvLabel("USD"))
        val etUSD = etLibre(
            presupuestoMaxUSD, "0 USD", decimal = true
        ) { v -> presupuestoMaxUSD = v }
        etPresupGenUSD = etUSD
        cardGen.addView(etUSD)

        // Mostrar total calculado si hay valores
        if (totalAuto > 0) {
            cardGen.addView(TextView(this).apply {
                text = "✅ Total calculado: ${fmt(totalAuto)}"
                setTextColor(Color.parseColor("#4CAF50"))
                textSize = 13f
                setTypeface(typeface, android.graphics.Typeface.BOLD)
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                    setMargins(0, dpToPx(6), 0, 0)
                }
            })
        }
        container.addView(cardGen)

        // Presupuesto jerárquico
        val cardCats = card()
        cardCats.addView(seccionHeader("🎯  Presupuesto por categoría"))
        cardCats.addView(tvHint("Toca una categoría para expandirla y configurar sus subcategorías"))

        categorias.forEach { cat ->
            val esExpandida = catExpandida == cat.nombre

            val tvTot = TextView(this).apply {
                text = if (cat.presupuestoTotal > 0) fmt(cat.presupuestoTotal) else ""
                setTextColor(Color.parseColor("#4CAF50"))
                textSize = 12f
                setTypeface(typeface, android.graphics.Typeface.BOLD)
            }
            tvTotCat[cat.nombre] = tvTot

            val filaCat = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = android.view.Gravity.CENTER_VERTICAL
                background = android.graphics.drawable.GradientDrawable().apply {
                    setColor(Color.parseColor("#0D1A2A"))
                    cornerRadius = dpToPx(10).toFloat()
                }
                setPadding(dpToPx(12), dpToPx(12), dpToPx(12), dpToPx(12))
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                    setMargins(0, 0, 0, if (esExpandida) 0 else dpToPx(6))
                }
            }

            filaCat.addView(TextView(this).apply {
                text = if (esExpandida) "▼" else "▶"
                setTextColor(Color.parseColor("#4CAF50")); textSize = 11f
                layoutParams = LinearLayout.LayoutParams(dpToPx(20),
                    LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                    gravity = android.view.Gravity.CENTER_VERTICAL }
            })
            filaCat.addView(TextView(this).apply {
                text = "📂"; textSize = 16f
                layoutParams = LinearLayout.LayoutParams(dpToPx(28),
                    LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                    setMargins(0, 0, dpToPx(6), 0) }
            })
            filaCat.addView(TextView(this).apply {
                text = cat.nombre
                setTextColor(Color.WHITE); textSize = 14f
                setTypeface(typeface, android.graphics.Typeface.BOLD)
                layoutParams = LinearLayout.LayoutParams(0,
                    LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            })
            filaCat.addView(tvTot)

            filaCat.setOnClickListener {
                catExpandida = if (catExpandida == cat.nombre) "" else cat.nombre
                subExpandida = ""
                renderizar()
            }
            cardCats.addView(filaCat)

            if (esExpandida) {
                val bloqueExpandido = LinearLayout(this).apply {
                    orientation = LinearLayout.VERTICAL
                    setPadding(dpToPx(16), dpToPx(8), 0, dpToPx(6))
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                        setMargins(0, 0, 0, dpToPx(6))
                    }
                }

                bloqueExpandido.addView(campoInline(
                    "Presupuesto propio de \"${cat.nombre}\"",
                    cat.presupuesto
                ) { v ->
                    cat.presupuesto = v
                    actualizarTotalesEnPantalla()
                })

                cat.subcategorias.forEach { sub ->
                    val esSubExpandida = subExpandida == "${cat.nombre}|${sub.nombre}"

                    val tvTotS = TextView(this).apply {
                        text = if (sub.presupuestoTotal > 0) fmt(sub.presupuestoTotal) else ""
                        setTextColor(Color.parseColor("#69F0AE"))
                        textSize = 11f
                        setTypeface(typeface, android.graphics.Typeface.BOLD)
                    }
                    tvTotSub["${cat.nombre}|${sub.nombre}"] = tvTotS

                    val filaSub = LinearLayout(this).apply {
                        orientation = LinearLayout.HORIZONTAL
                        gravity = android.view.Gravity.CENTER_VERTICAL
                        background = android.graphics.drawable.GradientDrawable().apply {
                            setColor(Color.parseColor("#0A1520"))
                            cornerRadius = dpToPx(8).toFloat()
                        }
                        setPadding(dpToPx(10), dpToPx(8), dpToPx(10), dpToPx(8))
                        layoutParams = LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.MATCH_PARENT,
                            LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                            setMargins(0, dpToPx(4), 0, 0)
                        }
                    }

                    val iconSub = if (sub.subsubcategorias.isEmpty()) "—"
                    else if (esSubExpandida) "▼" else "▶"
                    filaSub.addView(TextView(this).apply {
                        text = iconSub
                        setTextColor(Color.parseColor("#445566")); textSize = 10f
                        layoutParams = LinearLayout.LayoutParams(dpToPx(18),
                            LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                            gravity = android.view.Gravity.CENTER_VERTICAL }
                    })
                    filaSub.addView(TextView(this).apply {
                        text = "📁"; textSize = 13f
                        layoutParams = LinearLayout.LayoutParams(dpToPx(22),
                            LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                            setMargins(0, 0, dpToPx(4), 0) }
                    })
                    filaSub.addView(TextView(this).apply {
                        text = sub.nombre
                        setTextColor(Color.parseColor("#AABBCC")); textSize = 13f
                        layoutParams = LinearLayout.LayoutParams(0,
                            LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                    })
                    filaSub.addView(tvTotS)

                    filaSub.setOnClickListener {
                        val key = "${cat.nombre}|${sub.nombre}"
                        subExpandida = if (subExpandida == key) "" else key
                        renderizar()
                    }
                    bloqueExpandido.addView(filaSub)

                    if (esSubExpandida) {
                        val bloqueSub = LinearLayout(this).apply {
                            orientation = LinearLayout.VERTICAL
                            setPadding(dpToPx(32), dpToPx(6), 0, dpToPx(4))
                            layoutParams = LinearLayout.LayoutParams(
                                LinearLayout.LayoutParams.MATCH_PARENT,
                                LinearLayout.LayoutParams.WRAP_CONTENT)
                        }

                        bloqueSub.addView(campoInline(
                            "Presupuesto de \"${sub.nombre}\"",
                            sub.presupuesto
                        ) { v ->
                            sub.presupuesto = v
                            actualizarTotalesEnPantalla()
                        })

                        if (sub.subsubcategorias.isNotEmpty()) {
                            bloqueSub.addView(TextView(this).apply {
                                text = "Subsubcategorías:"
                                setTextColor(Color.parseColor("#445566"))
                                textSize = 11f
                                setTypeface(typeface, android.graphics.Typeface.BOLD)
                                layoutParams = LinearLayout.LayoutParams(
                                    LinearLayout.LayoutParams.MATCH_PARENT,
                                    LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                                    setMargins(0, dpToPx(6), 0, dpToPx(4))
                                }
                            })

                            val cardSubsub = LinearLayout(this).apply {
                                orientation = LinearLayout.VERTICAL
                                background = android.graphics.drawable.GradientDrawable().apply {
                                    setColor(Color.parseColor("#060F1A"))
                                    cornerRadius = dpToPx(8).toFloat()
                                    setStroke(1, Color.parseColor("#1A3A4A"))
                                }
                                setPadding(dpToPx(10), dpToPx(8), dpToPx(10), dpToPx(8))
                                layoutParams = LinearLayout.LayoutParams(
                                    LinearLayout.LayoutParams.MATCH_PARENT,
                                    LinearLayout.LayoutParams.WRAP_CONTENT)
                            }

                            sub.subsubcategorias.forEach { ssub ->
                                val filaSSub = LinearLayout(this).apply {
                                    orientation = LinearLayout.HORIZONTAL
                                    gravity = android.view.Gravity.CENTER_VERTICAL
                                    layoutParams = LinearLayout.LayoutParams(
                                        LinearLayout.LayoutParams.MATCH_PARENT,
                                        LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                                        setMargins(0, dpToPx(3), 0, dpToPx(3))
                                    }
                                }
                                filaSSub.addView(TextView(this).apply {
                                    text = "↳"; setTextColor(Color.parseColor("#334455"))
                                    textSize = 12f
                                    layoutParams = LinearLayout.LayoutParams(dpToPx(20),
                                        LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                                        gravity = android.view.Gravity.CENTER_VERTICAL }
                                })
                                filaSSub.addView(TextView(this).apply {
                                    text = "📌"; textSize = 12f
                                    layoutParams = LinearLayout.LayoutParams(dpToPx(22),
                                        LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                                        setMargins(0, 0, dpToPx(4), 0) }
                                })
                                filaSSub.addView(TextView(this).apply {
                                    text = ssub.nombre
                                    setTextColor(Color.parseColor("#778899"))
                                    textSize = 12f
                                    layoutParams = LinearLayout.LayoutParams(0,
                                        LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                                })
                                // Campo sin renderizar() — nunca pierde cursor
                                filaSSub.addView(etSubsubLibre(ssub.presupuesto) { v ->
                                    ssub.presupuesto = v
                                    actualizarTotalesEnPantalla()
                                })
                                filaSSub.addView(TextView(this).apply {
                                    text = "$"
                                    setTextColor(Color.parseColor("#445566")); textSize = 11f
                                    layoutParams = LinearLayout.LayoutParams(
                                        LinearLayout.LayoutParams.WRAP_CONTENT,
                                        LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                                        setMargins(dpToPx(4), 0, 0, 0) }
                                })
                                cardSubsub.addView(filaSSub)
                            }
                            bloqueSub.addView(cardSubsub)
                        }
                        bloqueExpandido.addView(bloqueSub)
                    }
                }
                cardCats.addView(bloqueExpandido)
            }
        }
        container.addView(cardCats)
    }

    private fun guardar() {
        btnGuardar.isEnabled = false; btnGuardar.text = "Guardando..."
        val uid = auth.currentUser?.uid ?: run {
            btnGuardar.isEnabled = true; btnGuardar.text = "GUARDAR PRESUPUESTO"; return }

        // Presupuesto general = manual o suma automática
        val genCOP = if (presupuestoMaxCOP > 0) presupuestoMaxCOP
        else totalGeneral()
        val genUSD = presupuestoMaxUSD

        val presupMap = mutableMapOf<String, Any>()
        categorias.forEach { cat ->
            if (cat.presupuesto > 0)
                presupMap[cat.nombre] = mapOf("max_COP" to cat.presupuesto, "max_USD" to 0.0)

            val totCat = cat.presupuestoTotal
            if (totCat > 0)
                presupMap["${cat.nombre}__total"] = mapOf("max_COP" to totCat, "max_USD" to 0.0)

            cat.subcategorias.forEach { sub ->
                if (sub.presupuesto > 0)
                    presupMap["${cat.nombre} > ${sub.nombre}"] =
                        mapOf("max_COP" to sub.presupuesto, "max_USD" to 0.0)
                val totSub = sub.presupuestoTotal
                if (totSub > 0)
                    presupMap["${cat.nombre} > ${sub.nombre}__total"] =
                        mapOf("max_COP" to totSub, "max_USD" to 0.0)

                sub.subsubcategorias.forEach { ssub ->
                    if (ssub.presupuesto > 0)
                        presupMap["${cat.nombre} > ${sub.nombre} > ${ssub.nombre}"] =
                            mapOf("max_COP" to ssub.presupuesto, "max_USD" to 0.0)
                }
            }
        }

        db.child("configuracion").child(uid).child("presupuestoMax")
            .setValue(mapOf("max_COP" to genCOP, "max_USD" to genUSD))

        db.child("configuracion").child(uid).child("presupuestoCategorias")
            .setValue(presupMap)
        db.child("configuracion").child(uid).child("maximosCategorias")
            .setValue(presupMap)
            .addOnSuccessListener {
                btnGuardar.isEnabled = true; btnGuardar.text = "GUARDAR PRESUPUESTO"
                Toast.makeText(this, "✅ Presupuesto guardado", Toast.LENGTH_SHORT).show()

                // Si viene del onboarding, ir a la app principal
                val desdeOnboarding = intent.getBooleanExtra("desde_onboarding", false)
                if (desdeOnboarding) {
                    startActivity(Intent(this, MainActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    })
                } else {
                    finish()
                }
            }
            .addOnFailureListener {
                btnGuardar.isEnabled = true; btnGuardar.text = "GUARDAR PRESUPUESTO"
                Toast.makeText(this, "Error: ${it.message}", Toast.LENGTH_LONG).show()
            }
    }

    private fun etSubsubLibre(valorInicial: Double, onChange: (Double) -> Unit) =
        EditText(this).apply {
            if (valorInicial > 0) setText(valorInicial.toLong().toString())
            hint = "0"
            setHintTextColor(Color.parseColor("#2A4A5A"))
            setTextColor(Color.WHITE); textSize = 13f
            inputType = InputType.TYPE_CLASS_NUMBER
            background = android.graphics.drawable.GradientDrawable().apply {
                setColor(Color.parseColor("#0D1A2A"))
                cornerRadius = dpToPx(8).toFloat()
                setStroke(1, Color.parseColor("#1A3A4A"))
            }
            setPadding(dpToPx(8), dpToPx(6), dpToPx(8), dpToPx(6))
            layoutParams = LinearLayout.LayoutParams(dpToPx(90),
                LinearLayout.LayoutParams.WRAP_CONTENT)
            gravity = android.view.Gravity.END
            addTextChangedListener(object : TextWatcher {
                override fun afterTextChanged(s: Editable?) {
                    val raw = s?.toString()?.trim() ?: ""
                    onChange(raw.replace(".", "").replace(",", "")
                        .toLongOrNull()?.toDouble() ?: 0.0)
                }
                override fun beforeTextChanged(s: CharSequence?, st: Int, c: Int, a: Int) {}
                override fun onTextChanged(s: CharSequence?, st: Int, b: Int, c: Int) {}
            })
        }

    private fun campoInline(label: String, valorInicial: Double,
                            onChange: (Double) -> Unit): LinearLayout {
        val fila = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = android.view.Gravity.CENTER_VERTICAL
            background = android.graphics.drawable.GradientDrawable().apply {
                setColor(Color.parseColor("#0A1218"))
                cornerRadius = dpToPx(8).toFloat()
            }
            setPadding(dpToPx(10), dpToPx(8), dpToPx(10), dpToPx(8))
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                setMargins(0, dpToPx(4), 0, dpToPx(4))
            }
        }
        fila.addView(TextView(this).apply {
            text = label
            setTextColor(Color.parseColor("#445566")); textSize = 12f
            layoutParams = LinearLayout.LayoutParams(0,
                LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        })
        fila.addView(etLibre(valorInicial, "0", onChange = onChange).apply {
            layoutParams = LinearLayout.LayoutParams(dpToPx(110),
                LinearLayout.LayoutParams.WRAP_CONTENT)
            gravity = android.view.Gravity.END
        })
        fila.addView(TextView(this).apply {
            text = "$"; setTextColor(Color.parseColor("#445566")); textSize = 11f
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                setMargins(dpToPx(4), 0, 0, 0) }
        })
        return fila
    }

    private fun bannerInfo() = LinearLayout(this).apply {
        orientation = LinearLayout.HORIZONTAL
        gravity = android.view.Gravity.CENTER_VERTICAL
        background = android.graphics.drawable.GradientDrawable().apply {
            setColor(Color.parseColor("#0D2A1A"))
            cornerRadius = dpToPx(12).toFloat()
            setStroke(1, Color.parseColor("#1A4A2A"))
        }
        setPadding(dpToPx(14), dpToPx(12), dpToPx(14), dpToPx(12))
        layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT).apply {
            setMargins(0, 0, 0, dpToPx(4))
        }
        addView(TextView(this@PresupuestoActivity).apply {
            text = "🔒"; textSize = 20f
            layoutParams = LinearLayout.LayoutParams(dpToPx(32),
                LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                gravity = android.view.Gravity.CENTER_VERTICAL }
        })
        val col = LinearLayout(this@PresupuestoActivity).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(0,
                LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }
        col.addView(TextView(this@PresupuestoActivity).apply {
            text = "Presupuestos personales"
            setTextColor(Color.WHITE); textSize = 13f
            setTypeface(typeface, android.graphics.Typeface.BOLD)
        })
        col.addView(TextView(this@PresupuestoActivity).apply {
            text = "Las subsubcategorías suman automáticamente hacia arriba ↑"
            setTextColor(Color.parseColor("#3A6A4A")); textSize = 11f
        })
        addView(col)
    }

    private fun etLibre(
        valorInicial: Double, hintText: String,
        decimal: Boolean = false,
        onChange: (Double) -> Unit
    ) = EditText(this).apply {
        if (valorInicial > 0) setText(
            if (decimal) valorInicial.toString()
            else fmtNum(valorInicial.toLong()))
        hint = hintText
        setHintTextColor(Color.parseColor("#2A4A5A"))
        setTextColor(Color.WHITE); textSize = 15f
        inputType = if (decimal)
            InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL
        else InputType.TYPE_CLASS_NUMBER
        setBackgroundResource(R.drawable.finanzas_input_bg)
        setPadding(dpToPx(12), dpToPx(10), dpToPx(12), dpToPx(10))
        layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, dpToPx(46)).apply {
            setMargins(0, dpToPx(4), 0, dpToPx(6)) }
        addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                val raw = s?.toString()?.trim() ?: ""
                val v = if (decimal) raw.toDoubleOrNull() ?: 0.0
                else raw.replace(".", "").replace(",", "")
                    .toLongOrNull()?.toDouble() ?: 0.0
                onChange(v)
            }
            override fun beforeTextChanged(s: CharSequence?, st: Int, c: Int, a: Int) {}
            override fun onTextChanged(s: CharSequence?, st: Int, b: Int, c: Int) {}
        })
    }

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
            LinearLayout.LayoutParams.WRAP_CONTENT).apply {
            setMargins(0, 0, 0, dpToPx(14))
        }
    }

    private fun seccionHeader(txt: String) = TextView(this).apply {
        text = txt; setTextColor(Color.parseColor("#4CAF50")); textSize = 15f
        setTypeface(typeface, android.graphics.Typeface.BOLD)
        layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT).apply {
            setMargins(0, 0, 0, dpToPx(4)) }
    }

    private fun tvHint(txt: String) = TextView(this).apply {
        text = txt; setTextColor(Color.parseColor("#3A5A6A")); textSize = 12f
        layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT).apply {
            setMargins(0, 0, 0, dpToPx(12)) }
    }

    private fun tvLabel(txt: String) = TextView(this).apply {
        text = txt; setTextColor(Color.parseColor("#4CAF50")); textSize = 11f
        setTypeface(typeface, android.graphics.Typeface.BOLD)
        letterSpacing = 0.06f
        layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT).apply {
            setMargins(0, dpToPx(8), 0, dpToPx(4)) }
    }

    private fun fmt(n: Double): String {
        val f = NumberFormat.getNumberInstance(Locale("es","CO")).apply {
            maximumFractionDigits = 0 }
        return "$${f.format(n)}"
    }

    private fun fmtNum(n: Long): String {
        if (n == 0L) return ""
        return NumberFormat.getNumberInstance(Locale("es","CO")).apply {
            maximumFractionDigits = 0 }.format(n)
    }

    private fun dpToPx(dp: Int) = (dp * resources.displayMetrics.density).toInt()
}