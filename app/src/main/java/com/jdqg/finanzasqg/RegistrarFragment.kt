package com.jdqg.finanzasqg

import android.app.DatePickerDialog
import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.*
import android.view.inputmethod.InputMethodManager
import android.widget.*
import androidx.fragment.app.Fragment
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

class RegistrarFragment : Fragment() {

    private val db   = FirebaseDatabase.getInstance().reference
    private val auth = FirebaseAuth.getInstance()

    private val categoriasEgreso     = mutableListOf<String>()
    private val categoriasIngreso    = mutableListOf<String>()
    private val subcategoriasMap     = mutableMapOf<String, List<String>>()
    private val subsubcategoriasMap  = mutableMapOf<String, List<String>>()
    private val cuentas              = mutableListOf<String>()
    private val monedas              = listOf("COP", "USD")

    private var tipoSeleccionado  = "Egreso"
    private var editandoValor     = false
    private var fechaSeleccionada = Calendar.getInstance()
    private var rootView: View?   = null

    private lateinit var btnEgreso        : Button
    private lateinit var btnIngreso       : Button
    private lateinit var btnTransferencia : Button
    private lateinit var layoutNormal     : LinearLayout
    private lateinit var layoutTransf     : LinearLayout
    private lateinit var spinnerCat       : Spinner
    private lateinit var spinnerSubcat    : Spinner
    private lateinit var spinnerSubSubcat : Spinner
    private lateinit var layoutSubcat     : LinearLayout
    private lateinit var layoutSubSubcat  : LinearLayout
    private lateinit var tvLabelSubcat    : TextView
    private lateinit var tvLabelSubSubcat : TextView
    private lateinit var spinnerCuenta    : Spinner
    private lateinit var spinnerMoneda    : Spinner
    private lateinit var spinnerOrigen    : Spinner
    private lateinit var spinnerDestino   : Spinner
    private lateinit var etDetalle        : EditText
    private lateinit var etValor          : EditText
    private lateinit var etDetalleTransf  : EditText
    private lateinit var etValorTransf    : EditText
    private lateinit var tvMonedaBadge    : TextView
    private lateinit var btnGuardar       : Button
    private lateinit var btnFecha         : Button
    private lateinit var tvFechaSeleccionada : TextView

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_registrar, container, false)
        rootView = view
        bindViews(view)
        limpiar()
        actualizarTextoFecha()
        cargarConfiguracion()

        btnEgreso.setOnClickListener        { actualizarTipo("Egreso") }
        btnIngreso.setOnClickListener       { actualizarTipo("Ingreso") }
        btnTransferencia.setOnClickListener { actualizarTipo("Transferencia") }
        btnGuardar.setOnClickListener       { confirmarYGuardar() }

        btnFecha.setOnClickListener {
            val cal = fechaSeleccionada
            DatePickerDialog(
                requireContext(),
                { _, year, month, day ->
                    fechaSeleccionada = Calendar.getInstance().apply {
                        set(year, month, day,
                            Calendar.getInstance().get(Calendar.HOUR_OF_DAY),
                            Calendar.getInstance().get(Calendar.MINUTE),
                            0)
                    }
                    actualizarTextoFecha()
                },
                cal.get(Calendar.YEAR),
                cal.get(Calendar.MONTH),
                cal.get(Calendar.DAY_OF_MONTH)
            ).show()
        }

        return view
    }

    private fun bindViews(view: View) {
        btnEgreso           = view.findViewById(R.id.btnTipoEgreso)
        btnIngreso          = view.findViewById(R.id.btnTipoIngreso)
        btnTransferencia    = view.findViewById(R.id.btnTipoTransferencia)
        layoutNormal        = view.findViewById(R.id.layoutFormularioNormal)
        layoutTransf        = view.findViewById(R.id.layoutFormularioTransferencia)
        spinnerCat          = view.findViewById(R.id.spinnerCategoria)
        spinnerSubcat       = view.findViewById(R.id.spinnerSubcategoria)
        spinnerSubSubcat    = view.findViewById(R.id.spinnerSubSubcategoria)
        layoutSubcat        = view.findViewById(R.id.layoutSubcat)
        layoutSubSubcat     = view.findViewById(R.id.layoutSubSubcat)
        tvLabelSubcat       = view.findViewById(R.id.tvLabelSubcat)
        tvLabelSubSubcat    = view.findViewById(R.id.tvLabelSubSubcat)
        spinnerCuenta       = view.findViewById(R.id.spinnerCuenta)
        spinnerMoneda       = view.findViewById(R.id.spinnerMoneda)
        spinnerOrigen       = view.findViewById(R.id.spinnerCuentaOrigen)
        spinnerDestino      = view.findViewById(R.id.spinnerCuentaDestino)
        etDetalle           = view.findViewById(R.id.etDetalle)
        etValor             = view.findViewById(R.id.etValor)
        etDetalleTransf     = view.findViewById(R.id.etDetalleTransf)
        etValorTransf       = view.findViewById(R.id.etValorTransf)
        tvMonedaBadge       = view.findViewById(R.id.tvMonedaBadge)
        btnGuardar          = view.findViewById(R.id.btnGuardarMovimiento)
        btnFecha            = view.findViewById(R.id.btnFecha)
        tvFechaSeleccionada = view.findViewById(R.id.tvFechaSeleccionada)
    }

    private fun actualizarTextoFecha() {
        val sdf  = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        val hoy  = Calendar.getInstance()
        val ayer = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -1) }
        val fechaStr = sdf.format(fechaSeleccionada.time)
        val label = when (fechaStr) {
            sdf.format(hoy.time)  -> "📅 Hoy — $fechaStr"
            sdf.format(ayer.time) -> "📅 Ayer — $fechaStr"
            else                  -> "📅 $fechaStr"
        }
        tvFechaSeleccionada.text = label
    }

    private fun cargarConfiguracion() {
        val uid = auth.currentUser?.uid ?: run { usarDefaults(); return }
        db.child("configuracion").child(uid)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snap: DataSnapshot) {
                    if (!snap.exists() || !snap.hasChild("egresos")) {
                        usarDefaults(); return }

                    categoriasEgreso.clear()
                    subcategoriasMap.clear()
                    subsubcategoriasMap.clear()

                    for (cs in snap.child("egresos").children) {
                        val n = cs.child("nombre").getValue(String::class.java) ?: continue
                        categoriasEgreso.add(n)
                        val subs = mutableListOf<String>()
                        for (ss in cs.child("subcategorias").children) {
                            val sn = ss.child("nombre")
                                .getValue(String::class.java) ?: continue
                            subs.add(sn)
                            val subsubs = mutableListOf<String>()
                            for (sss in ss.child("subsubcategorias").children)
                                sss.getValue(String::class.java)?.let { subsubs.add(it) }
                            if (subsubs.isNotEmpty()) subsubcategoriasMap[sn] = subsubs
                        }
                        if (subs.isNotEmpty()) subcategoriasMap[n] = subs
                    }

                    categoriasIngreso.clear()
                    for (cs in snap.child("ingresos").children) {
                        val n = cs.child("nombre").getValue(String::class.java) ?: continue
                        categoriasIngreso.add(n)
                        val subs = mutableListOf<String>()
                        for (ss in cs.child("subcategorias").children) {
                            val sn = ss.child("nombre")
                                .getValue(String::class.java) ?: continue
                            subs.add(sn)
                            val subsubs = mutableListOf<String>()
                            for (sss in ss.child("subsubcategorias").children)
                                sss.getValue(String::class.java)?.let { subsubs.add(it) }
                            if (subsubs.isNotEmpty()) subsubcategoriasMap[sn] = subsubs
                        }
                        if (subs.isNotEmpty()) subcategoriasMap[n] = subs
                    }

                    cuentas.clear()
                    for (c in snap.child("cuentas").children)
                        c.getValue(String::class.java)?.let { cuentas.add(it) }

                    if (categoriasEgreso.isEmpty())
                        categoriasEgreso.addAll(listOf("Comida","Casa","Transporte",
                            "Salud","Familia","Ocio","Otro"))
                    if (categoriasIngreso.isEmpty())
                        categoriasIngreso.addAll(listOf("Salario","Negocio",
                            "Inmuebles","Regalo","Otros"))
                    if (cuentas.isEmpty())
                        cuentas.addAll(listOf("Bancolombia","Nu","Nequi",
                            "Efectivo","Tarjeta crédito"))

                    if (isAdded) {
                        configurarSpinners()
                        actualizarTipo("Egreso")
                        configurarFormato(etValor)
                        configurarFormato(etValorTransf)
                    }
                }
                override fun onCancelled(e: DatabaseError) { usarDefaults() }
            })
    }

    private fun usarDefaults() {
        if (categoriasEgreso.isEmpty())
            categoriasEgreso.addAll(listOf("Comida","Casa","Transporte",
                "Salud","Familia","Ocio","Otro"))
        if (categoriasIngreso.isEmpty())
            categoriasIngreso.addAll(listOf("Salario","Negocio",
                "Inmuebles","Regalo","Otros"))
        if (cuentas.isEmpty())
            cuentas.addAll(listOf("Bancolombia","Nu","Nequi",
                "Efectivo","Tarjeta crédito"))
        if (isAdded) {
            configurarSpinners()
            actualizarTipo("Egreso")
            configurarFormato(etValor)
            configurarFormato(etValorTransf)
        }
    }

    private fun configurarFormato(et: EditText) {
        et.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                if (editandoValor) return; editandoValor = true
                val d = s.toString().filter { it.isDigit() }
                if (d.isNotEmpty()) {
                    val n = d.toLongOrNull() ?: 0L
                    val f = NumberFormat.getNumberInstance(Locale("es","CO")).apply {
                        maximumFractionDigits = 0 }
                    et.setText(f.format(n)); et.setSelection(et.text.length)
                }
                editandoValor = false
            }
            override fun beforeTextChanged(s: CharSequence?, st: Int, c: Int, a: Int) {}
            override fun onTextChanged(s: CharSequence?, st: Int, b: Int, c: Int) {}
        })
    }

    private fun actualizarBadgeMoneda() {
        val m = runCatching {
            spinnerMoneda.selectedItem?.toString() ?: "COP" }.getOrDefault("COP")
        tvMonedaBadge.text = if (m == "USD") "🇺🇸 USD" else "🇨🇴 COP"
        tvMonedaBadge.setTextColor(
            Color.parseColor(if (m == "USD") "#2196F3" else "#4CAF50"))
    }

    private fun actualizarTipo(tipo: String) {
        tipoSeleccionado = tipo
        val rojo  = Color.parseColor("#FF5252"); val verde = Color.parseColor("#4CAF50")
        val azul  = Color.parseColor("#2196F3"); val osc   = Color.parseColor("#0D1A2A")
        btnEgreso.backgroundTintList =
            ColorStateList.valueOf(if (tipo == "Egreso") rojo else osc)
        btnEgreso.setTextColor(if (tipo == "Egreso") Color.WHITE else rojo)
        btnIngreso.backgroundTintList =
            ColorStateList.valueOf(if (tipo == "Ingreso") verde else osc)
        btnIngreso.setTextColor(if (tipo == "Ingreso") Color.WHITE else verde)
        btnTransferencia.backgroundTintList =
            ColorStateList.valueOf(if (tipo == "Transferencia") azul else osc)
        btnTransferencia.setTextColor(if (tipo == "Transferencia") Color.WHITE else azul)

        if (tipo == "Transferencia") {
            layoutNormal.visibility = View.GONE
            layoutTransf.visibility = View.VISIBLE
        } else {
            layoutNormal.visibility = View.VISIBLE
            layoutTransf.visibility = View.GONE
            val lista = if (tipo == "Egreso") categoriasEgreso else categoriasIngreso
            if (lista.isNotEmpty()) spinnerCat.adapter = spinnerAdapter(lista)
        }
        etValor.setText(""); etValorTransf.setText("")
        etDetalle.setText(""); etDetalleTransf.setText("")
        ocultarSubcat(); ocultarSubSubcat()
    }

    private fun ocultarSubcat() {
        layoutSubcat.visibility = View.GONE; tvLabelSubcat.visibility = View.GONE }
    private fun ocultarSubSubcat() {
        layoutSubSubcat.visibility = View.GONE; tvLabelSubSubcat.visibility = View.GONE }

    private fun configurarSpinners() {
        if (!isAdded) return
        val catList = if (tipoSeleccionado == "Egreso")
            categoriasEgreso else categoriasIngreso
        if (catList.isNotEmpty()) spinnerCat.adapter = spinnerAdapter(catList)
        val cl = cuentas.ifEmpty {
            listOf("Bancolombia","Nu","Nequi","Efectivo","Tarjeta crédito") }
        spinnerCuenta.adapter  = spinnerAdapter(cl)
        spinnerMoneda.adapter  = spinnerAdapter(monedas)
        spinnerOrigen.adapter  = spinnerAdapter(cl)
        spinnerDestino.adapter = spinnerAdapter(cl)

        spinnerMoneda.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(p: AdapterView<*>?, v: View?, pos: Int, id: Long) {
                actualizarBadgeMoneda() }
            override fun onNothingSelected(p: AdapterView<*>?) {}
        }
        spinnerCat.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(p: AdapterView<*>?, v: View?, pos: Int, id: Long) {
                val cat  = runCatching {
                    spinnerCat.selectedItem?.toString() ?: "" }.getOrDefault("")
                val subs = subcategoriasMap[cat]
                ocultarSubSubcat()
                if (!subs.isNullOrEmpty()) {
                    layoutSubcat.visibility  = View.VISIBLE
                    tvLabelSubcat.visibility = View.VISIBLE
                    spinnerSubcat.adapter    = spinnerAdapter(subs)
                    actualizarSubSubcat(subs[0])
                } else ocultarSubcat()
            }
            override fun onNothingSelected(p: AdapterView<*>?) {}
        }
        spinnerSubcat.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(p: AdapterView<*>?, v: View?, pos: Int, id: Long) {
                val sub = runCatching {
                    spinnerSubcat.selectedItem?.toString() ?: "" }.getOrDefault("")
                actualizarSubSubcat(sub)
            }
            override fun onNothingSelected(p: AdapterView<*>?) {}
        }
    }

    private fun actualizarSubSubcat(sub: String) {
        val subsubs = subsubcategoriasMap[sub]
        if (!subsubs.isNullOrEmpty()) {
            layoutSubSubcat.visibility  = View.VISIBLE
            tvLabelSubSubcat.visibility = View.VISIBLE
            spinnerSubSubcat.adapter    = spinnerAdapter(subsubs)
        } else ocultarSubSubcat()
    }

    private fun spinnerAdapter(items: List<String>) =
        ArrayAdapter(requireContext(),
            android.R.layout.simple_spinner_item, items).also {
            it.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item) }

    private fun bajarTeclado() {
        val imm = requireContext()
            .getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        val v = activity?.currentFocus ?: rootView ?: return
        imm.hideSoftInputFromWindow(v.windowToken, 0)
    }

    private fun confirmarYGuardar() {
        if (!isAdded) return
        bajarTeclado()
        val uid = auth.currentUser?.uid ?: run {
            Toast.makeText(requireContext(),
                "Debes iniciar sesión", Toast.LENGTH_SHORT).show(); return }

        val ahora = fechaSeleccionada.timeInMillis

        val data = mutableMapOf<String, Any>(
            "id"                 to "",
            "tipoTransaccion"    to tipoSeleccionado,
            "timestamp"          to ahora,
            "fechaPersonalizada" to ahora,
            "detalle"            to "",
            "categoria"          to "",
            "subcategoria"       to "",
            "subsubcategoria"    to "",
            "cuenta"             to "",
            "moneda"             to "COP",
            "cuentaOrigen"       to "",
            "valor"              to 0.0
        )

        try {
            when (tipoSeleccionado) {
                "Transferencia" -> {
                    val v = etValorTransf.text.toString()
                        .replace(".","").replace(",",".").trim().toDoubleOrNull()
                    if (v == null || v <= 0) {
                        Toast.makeText(requireContext(),
                            "Ingresa un valor válido", Toast.LENGTH_SHORT).show(); return }
                    data["valor"]        = v
                    data["cuentaOrigen"] = runCatching {
                        spinnerOrigen.selectedItem?.toString() ?: "" }.getOrDefault("")
                    data["cuenta"]       = runCatching {
                        spinnerDestino.selectedItem?.toString() ?: "" }.getOrDefault("")
                    data["categoria"]    = "Transferencia"
                    data["subcategoria"] = ""
                    data["detalle"]      = etDetalleTransf.text.toString().trim()
                    data["moneda"]       = "COP"
                }
                else -> {
                    val v = etValor.text.toString()
                        .replace(".","").replace(",",".").trim().toDoubleOrNull()
                    if (v == null || v <= 0) {
                        Toast.makeText(requireContext(),
                            "Ingresa un valor válido", Toast.LENGTH_SHORT).show(); return }
                    data["valor"]           = v
                    data["categoria"]       = runCatching {
                        spinnerCat.selectedItem?.toString() ?: "" }.getOrDefault("")
                    data["subcategoria"]    = if (layoutSubcat.visibility == View.VISIBLE)
                        runCatching { spinnerSubcat.selectedItem?.toString() ?: "" }
                            .getOrDefault("") else ""
                    data["subsubcategoria"] = if (layoutSubSubcat.visibility == View.VISIBLE)
                        runCatching { spinnerSubSubcat.selectedItem?.toString() ?: "" }
                            .getOrDefault("") else ""
                    data["cuenta"]  = runCatching {
                        spinnerCuenta.selectedItem?.toString() ?: "" }.getOrDefault("")
                    data["moneda"]  = runCatching {
                        spinnerMoneda.selectedItem?.toString() ?: "COP" }.getOrDefault("COP")
                    data["detalle"] = etDetalle.text.toString().trim()
                }
            }
        } catch (e: Exception) {
            Toast.makeText(requireContext(),
                "Error: ${e.message}", Toast.LENGTH_LONG).show(); return }

        mostrarDialogoConfirmacion(uid, data)
    }

    private fun mostrarDialogoConfirmacion(uid: String, data: MutableMap<String, Any>) {
        if (!isAdded) return
        val tipo   = data["tipoTransaccion"] as String
        val valor  = data["valor"] as Double
        val moneda = data["moneda"] as String
        val cat    = data["categoria"] as String
        val det    = data["detalle"] as String
        val cuenta = data["cuenta"] as String
        val origen = data["cuentaOrigen"] as String
        val sdf    = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        val fechaStr = sdf.format(Date(fechaSeleccionada.timeInMillis))

        val colorTipo = when (tipo) {
            "Ingreso" -> "#69F0AE"; "Transferencia" -> "#2196F3"; else -> "#FF5252" }
        val emoji = when (tipo) {
            "Ingreso" -> "📈"; "Transferencia" -> "🔄"; else -> "📉" }
        val ctx = requireContext()

        val raiz = android.widget.LinearLayout(ctx).apply {
            orientation = android.widget.LinearLayout.VERTICAL
            setBackgroundColor(Color.parseColor("#111E2E"))
            setPadding(dp(24), dp(24), dp(24), dp(20))
        }

        fun tv(txt: String, size: Float, clr: String,
               bold: Boolean = false, center: Boolean = false) =
            TextView(ctx).apply {
                text = txt; textSize = size; setTextColor(Color.parseColor(clr))
                if (bold) setTypeface(typeface, android.graphics.Typeface.BOLD)
                if (center) gravity = android.view.Gravity.CENTER
                layoutParams = android.widget.LinearLayout.LayoutParams(
                    android.widget.LinearLayout.LayoutParams.MATCH_PARENT,
                    android.widget.LinearLayout.LayoutParams.WRAP_CONTENT)
            }

        raiz.addView(tv(emoji, 44f, "#FFFFFF", center = true))
        raiz.addView(tv(tipo,  18f, colorTipo, bold = true, center = true))
        raiz.addView(tv(formatValor(valor, moneda), 28f, colorTipo,
            bold = true, center = true).apply {
            (layoutParams as android.widget.LinearLayout.LayoutParams)
                .setMargins(0,0,0,dp(16)) })

        raiz.addView(lineaDivisor(ctx))

        fun fila(label: String, valor2: String, clr: String = "#EEFFEE") {
            if (valor2.isEmpty()) return
            val row = android.widget.LinearLayout(ctx).apply {
                orientation = android.widget.LinearLayout.HORIZONTAL
                layoutParams = android.widget.LinearLayout.LayoutParams(
                    android.widget.LinearLayout.LayoutParams.MATCH_PARENT,
                    android.widget.LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                    setMargins(0,0,0,dp(8)) }
            }
            row.addView(TextView(ctx).apply {
                text = label; setTextColor(Color.parseColor("#445566")); textSize = 13f
                layoutParams = android.widget.LinearLayout.LayoutParams(0,
                    android.widget.LinearLayout.LayoutParams.WRAP_CONTENT, 1f) })
            row.addView(TextView(ctx).apply {
                text = valor2; setTextColor(Color.parseColor(clr)); textSize = 13f
                setTypeface(typeface, android.graphics.Typeface.BOLD)
                gravity = android.view.Gravity.END
                layoutParams = android.widget.LinearLayout.LayoutParams(0,
                    android.widget.LinearLayout.LayoutParams.WRAP_CONTENT, 1f) })
            raiz.addView(row)
        }

        fila("📅 Fecha", fechaStr, "#AABBCC")
        if (tipo == "Transferencia") {
            fila("Origen",  origen, "#2196F3")
            fila("Destino", cuenta, "#00BCD4")
        } else {
            if (cat.isNotEmpty()) fila("Categoría", cat)
            fila("Cuenta", cuenta)
        }
        if (det.isNotEmpty()) fila("Detalle", det)
        fila("Moneda", moneda, if (moneda == "USD") "#2196F3" else "#4CAF50")

        raiz.addView(lineaDivisor(ctx).apply {
            (layoutParams as android.widget.LinearLayout.LayoutParams)
                .setMargins(0,dp(8),0,dp(16)) })

        val filaBtns = android.widget.LinearLayout(ctx).apply {
            orientation = android.widget.LinearLayout.HORIZONTAL
            layoutParams = android.widget.LinearLayout.LayoutParams(
                android.widget.LinearLayout.LayoutParams.MATCH_PARENT,
                android.widget.LinearLayout.LayoutParams.WRAP_CONTENT) }

        val dialog = androidx.appcompat.app.AlertDialog.Builder(ctx)
            .setView(raiz).create()
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        filaBtns.addView(Button(ctx).apply {
            text = "Cancelar"; isAllCaps = false; textSize = 14f
            setTextColor(Color.parseColor("#445566"))
            backgroundTintList = android.content.res.ColorStateList.valueOf(
                Color.parseColor("#0D1A2A"))
            layoutParams = android.widget.LinearLayout.LayoutParams(0,
                android.widget.LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply {
                setMargins(0,0,dp(8),0) }
            setOnClickListener { dialog.dismiss() }
        })
        filaBtns.addView(Button(ctx).apply {
            text = "✅ Guardar"; isAllCaps = false; textSize = 14f
            setTextColor(Color.parseColor("#050510"))
            backgroundTintList = android.content.res.ColorStateList.valueOf(
                Color.parseColor("#4CAF50"))
            layoutParams = android.widget.LinearLayout.LayoutParams(0,
                android.widget.LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            setOnClickListener { dialog.dismiss(); guardarEnFirebase(uid, data) }
        })
        raiz.addView(filaBtns)
        dialog.show()
        dialog.window?.let {
            val w = (resources.displayMetrics.widthPixels * 0.88).toInt()
            it.setLayout(w, android.widget.LinearLayout.LayoutParams.WRAP_CONTENT) }
    }

    private fun lineaDivisor(ctx: android.content.Context) =
        View(ctx).apply {
            layoutParams = android.widget.LinearLayout.LayoutParams(
                android.widget.LinearLayout.LayoutParams.MATCH_PARENT, 1).apply {
                setMargins(0,0,0,dp(12)) }
            setBackgroundColor(Color.parseColor("#1E3A4A"))
        }

    private fun guardarEnFirebase(uid: String, data: MutableMap<String, Any>) {
        val ref = db.child("movimientos").child(uid).push()
        data["id"] = ref.key ?: ""
        btnGuardar.isEnabled = false; btnGuardar.text = "Guardando..."
        ref.setValue(data)
            .addOnSuccessListener {
                if (!isAdded) return@addOnSuccessListener
                Toast.makeText(requireContext(), "✅ Guardado", Toast.LENGTH_SHORT).show()
                limpiar()
                // Resetear fecha a hoy
                fechaSeleccionada = Calendar.getInstance()
                actualizarTextoFecha()
                btnGuardar.isEnabled = true; btnGuardar.text = "GUARDAR MOVIMIENTO"
            }
            .addOnFailureListener { e ->
                if (!isAdded) return@addOnFailureListener
                Toast.makeText(requireContext(),
                    "Error: ${e.message}", Toast.LENGTH_LONG).show()
                btnGuardar.isEnabled = true; btnGuardar.text = "GUARDAR MOVIMIENTO"
            }
    }

    private fun limpiar() {
        if (!isAdded) return
        try {
            etDetalle.setText(""); etValor.setText("")
            etDetalleTransf.setText(""); etValorTransf.setText("")
            ocultarSubcat(); ocultarSubSubcat()
        } catch (e: Exception) { /* ignorar */ }
    }

    private fun formatValor(valor: Double, moneda: String): String {
        return if (moneda == "USD") {
            val f = NumberFormat.getNumberInstance(Locale.US).apply {
                maximumFractionDigits = 2; minimumFractionDigits = 0 }
            "${f.format(valor)} USD"
        } else {
            val f = NumberFormat.getNumberInstance(Locale("es","CO")).apply {
                maximumFractionDigits = 0 }
            "$${f.format(valor)}"
        }
    }

    private fun dp(dp: Int) = (dp * resources.displayMetrics.density).toInt()
}