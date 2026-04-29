package com.jdqg.finanzasqg

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
import java.util.Locale

class RegistrarFragment : Fragment() {

    private val db   = FirebaseDatabase.getInstance().reference
    private val auth = FirebaseAuth.getInstance()

    private val categoriasEgreso  = mutableListOf<String>()
    private val categoriasIngreso = mutableListOf<String>()
    private val subcategoriasMap  = mutableMapOf<String, List<String>>()
    private val cuentas           = mutableListOf<String>()
    private val monedas           = listOf("COP", "USD")

    private var tipoSeleccionado = "Egreso"
    private var editandoValor    = false
    private var rootView: View?  = null

    private lateinit var btnEgreso        : Button
    private lateinit var btnIngreso       : Button
    private lateinit var btnTransferencia : Button
    private lateinit var layoutNormal     : LinearLayout
    private lateinit var layoutTransf     : LinearLayout
    private lateinit var spinnerCat       : Spinner
    private lateinit var spinnerSubcat    : Spinner
    private lateinit var layoutSubcat     : LinearLayout
    private lateinit var tvLabelSubcat    : TextView
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

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_registrar, container, false)
        rootView = view
        bindViews(view)
        cargarConfiguracion()
        btnEgreso.setOnClickListener        { actualizarTipo("Egreso") }
        btnIngreso.setOnClickListener       { actualizarTipo("Ingreso") }
        btnTransferencia.setOnClickListener { actualizarTipo("Transferencia") }
        btnGuardar.setOnClickListener       { confirmarYGuardar() }
        return view
    }

    private fun bindViews(view: View) {
        btnEgreso        = view.findViewById(R.id.btnTipoEgreso)
        btnIngreso       = view.findViewById(R.id.btnTipoIngreso)
        btnTransferencia = view.findViewById(R.id.btnTipoTransferencia)
        layoutNormal     = view.findViewById(R.id.layoutFormularioNormal)
        layoutTransf     = view.findViewById(R.id.layoutFormularioTransferencia)
        spinnerCat       = view.findViewById(R.id.spinnerCategoria)
        spinnerSubcat    = view.findViewById(R.id.spinnerSubcategoria)
        layoutSubcat     = view.findViewById(R.id.layoutSubcat)
        tvLabelSubcat    = view.findViewById(R.id.tvLabelSubcat)
        spinnerCuenta    = view.findViewById(R.id.spinnerCuenta)
        spinnerMoneda    = view.findViewById(R.id.spinnerMoneda)
        spinnerOrigen    = view.findViewById(R.id.spinnerCuentaOrigen)
        spinnerDestino   = view.findViewById(R.id.spinnerCuentaDestino)
        etDetalle        = view.findViewById(R.id.etDetalle)
        etValor          = view.findViewById(R.id.etValor)
        etDetalleTransf  = view.findViewById(R.id.etDetalleTransf)
        etValorTransf    = view.findViewById(R.id.etValorTransf)
        tvMonedaBadge    = view.findViewById(R.id.tvMonedaBadge)
        btnGuardar       = view.findViewById(R.id.btnGuardarMovimiento)
    }

    private fun cargarConfiguracion() {
        val uid = auth.currentUser?.uid ?: return
        db.child("configuracion").child(uid)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snap: DataSnapshot) {
                    categoriasEgreso.clear(); subcategoriasMap.clear()
                    for (cs in snap.child("egresos").children) {
                        val n = cs.child("nombre").getValue(String::class.java) ?: continue
                        categoriasEgreso.add(n)
                        val subs = mutableListOf<String>()
                        for (ss in cs.child("subcategorias").children)
                            ss.child("nombre").getValue(String::class.java)?.let { subs.add(it) }
                        if (subs.isNotEmpty()) subcategoriasMap[n] = subs
                    }
                    categoriasIngreso.clear()
                    for (cs in snap.child("ingresos").children) {
                        val n = cs.child("nombre").getValue(String::class.java) ?: continue
                        categoriasIngreso.add(n)
                        val subs = mutableListOf<String>()
                        for (ss in cs.child("subcategorias").children)
                            ss.child("nombre").getValue(String::class.java)?.let { subs.add(it) }
                        if (subs.isNotEmpty()) subcategoriasMap[n] = subs
                    }
                    cuentas.clear()
                    for (c in snap.child("cuentas").children)
                        c.getValue(String::class.java)?.let { cuentas.add(it) }
                    if (categoriasEgreso.isEmpty())
                        categoriasEgreso.addAll(listOf("Comida","Casa","Transporte","Salud","Familia","Ocio","Otro"))
                    if (categoriasIngreso.isEmpty())
                        categoriasIngreso.addAll(listOf("Salario","Negocio","Inmuebles","Regalo","Otros"))
                    if (cuentas.isEmpty())
                        cuentas.addAll(listOf("Bancolombia","Nu","Nequi","Efectivo","Tarjeta crédito"))
                    if (isAdded) { configurarSpinners(); actualizarTipo("Egreso")
                        configurarFormato(etValor); configurarFormato(etValorTransf) }
                }
                override fun onCancelled(e: DatabaseError) {
                    categoriasEgreso.addAll(listOf("Comida","Casa","Transporte","Salud","Familia","Ocio","Otro"))
                    categoriasIngreso.addAll(listOf("Salario","Negocio","Inmuebles","Regalo","Otros"))
                    cuentas.addAll(listOf("Bancolombia","Nu","Nequi","Efectivo","Tarjeta crédito"))
                    if (isAdded) { configurarSpinners(); actualizarTipo("Egreso")
                        configurarFormato(etValor); configurarFormato(etValorTransf) }
                }
            })
    }

    private fun configurarFormato(et: EditText) {
        et.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                if (editandoValor) return; editandoValor = true
                val d = s.toString().filter { it.isDigit() }
                if (d.isNotEmpty()) {
                    val n = d.toLongOrNull() ?: 0L
                    val f = NumberFormat.getNumberInstance(Locale("es","CO")).apply { maximumFractionDigits = 0 }
                    et.setText(f.format(n)); et.setSelection(et.text.length)
                }
                editandoValor = false
            }
            override fun beforeTextChanged(s: CharSequence?, st: Int, c: Int, a: Int) {}
            override fun onTextChanged(s: CharSequence?, st: Int, b: Int, c: Int) {}
        })
    }

    private fun actualizarBadgeMoneda() {
        val m = runCatching { spinnerMoneda.selectedItem?.toString() ?: "COP" }.getOrDefault("COP")
        tvMonedaBadge.text = if (m == "USD") "🇺🇸 USD" else "🇨🇴 COP"
        tvMonedaBadge.setTextColor(Color.parseColor(if (m == "USD") "#2196F3" else "#4CAF50"))
    }

    private fun actualizarTipo(tipo: String) {
        tipoSeleccionado = tipo
        val rojo = Color.parseColor("#FF5252"); val verde = Color.parseColor("#4CAF50")
        val azul = Color.parseColor("#2196F3"); val osc   = Color.parseColor("#0D1A2A")
        btnEgreso.backgroundTintList   = ColorStateList.valueOf(if (tipo=="Egreso") rojo else osc)
        btnEgreso.setTextColor(if (tipo=="Egreso") Color.WHITE else rojo)
        btnIngreso.backgroundTintList  = ColorStateList.valueOf(if (tipo=="Ingreso") verde else osc)
        btnIngreso.setTextColor(if (tipo=="Ingreso") Color.WHITE else verde)
        btnTransferencia.backgroundTintList = ColorStateList.valueOf(if (tipo=="Transferencia") azul else osc)
        btnTransferencia.setTextColor(if (tipo=="Transferencia") Color.WHITE else azul)
        if (tipo == "Transferencia") {
            layoutNormal.visibility = View.GONE; layoutTransf.visibility = View.VISIBLE
        } else {
            layoutNormal.visibility = View.VISIBLE; layoutTransf.visibility = View.GONE
            val lista = if (tipo == "Egreso") categoriasEgreso else categoriasIngreso
            if (lista.isNotEmpty()) spinnerCat.adapter = spinnerAdapter(lista)
        }
        etValor.setText(""); etValorTransf.setText("")
    }

    private fun configurarSpinners() {
        if (!isAdded) return
        val catList = if (tipoSeleccionado == "Egreso") categoriasEgreso else categoriasIngreso
        if (catList.isNotEmpty()) spinnerCat.adapter = spinnerAdapter(catList)
        val cl = cuentas.ifEmpty { listOf("Bancolombia","Nu","Nequi","Efectivo","Tarjeta crédito") }
        spinnerCuenta.adapter  = spinnerAdapter(cl)
        spinnerMoneda.adapter  = spinnerAdapter(monedas)
        spinnerOrigen.adapter  = spinnerAdapter(cl)
        spinnerDestino.adapter = spinnerAdapter(cl)
        spinnerMoneda.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(p: AdapterView<*>?, v: View?, pos: Int, id: Long) { actualizarBadgeMoneda() }
            override fun onNothingSelected(p: AdapterView<*>?) {}
        }
        spinnerCat.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(p: AdapterView<*>?, v: View?, pos: Int, id: Long) {
                val cat = runCatching { spinnerCat.selectedItem?.toString() ?: "" }.getOrDefault("")
                val subs = subcategoriasMap[cat]
                if (!subs.isNullOrEmpty()) {
                    layoutSubcat.visibility = View.VISIBLE; tvLabelSubcat.visibility = View.VISIBLE
                    spinnerSubcat.adapter = spinnerAdapter(subs)
                } else { layoutSubcat.visibility = View.GONE; tvLabelSubcat.visibility = View.GONE }
            }
            override fun onNothingSelected(p: AdapterView<*>?) {}
        }
    }

    private fun spinnerAdapter(items: List<String>) =
        ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, items).also {
            it.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item) }

    private fun bajarTeclado() {
        val imm = requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        val v   = activity?.currentFocus ?: rootView ?: return
        imm.hideSoftInputFromWindow(v.windowToken, 0)
    }

    private fun confirmarYGuardar() {
        if (!isAdded) return
        bajarTeclado()
        val uid = auth.currentUser?.uid ?: run {
            Toast.makeText(requireContext(), "Debes iniciar sesión", Toast.LENGTH_SHORT).show(); return }

        val data = mutableMapOf<String, Any>(
            "id" to "", "tipoTransaccion" to tipoSeleccionado,
            "timestamp" to System.currentTimeMillis(), "detalle" to "",
            "categoria" to "", "subcategoria" to "", "cuenta" to "",
            "moneda" to "COP", "cuentaOrigen" to "", "valor" to 0.0)

        try {
            when (tipoSeleccionado) {
                "Transferencia" -> {
                    val v = etValorTransf.text.toString().replace(".","").replace(",",".").trim().toDoubleOrNull()
                    if (v == null || v <= 0) { Toast.makeText(requireContext(), "Ingresa un valor válido", Toast.LENGTH_SHORT).show(); return }
                    data["valor"] = v; data["cuentaOrigen"] = spinnerOrigen.selectedItem?.toString() ?: ""
                    data["cuenta"] = spinnerDestino.selectedItem?.toString() ?: ""
                    data["categoria"] = "Transferencia"; data["subcategoria"] = ""
                    data["detalle"] = etDetalleTransf.text.toString().trim(); data["moneda"] = "COP"
                }
                else -> {
                    val v = etValor.text.toString().replace(".","").replace(",",".").trim().toDoubleOrNull()
                    if (v == null || v <= 0) { Toast.makeText(requireContext(), "Ingresa un valor válido", Toast.LENGTH_SHORT).show(); return }
                    data["valor"] = v
                    data["categoria"] = runCatching { spinnerCat.selectedItem?.toString() ?: "" }.getOrDefault("")
                    data["subcategoria"] = if (layoutSubcat.visibility == View.VISIBLE)
                        runCatching { spinnerSubcat.selectedItem?.toString() ?: "" }.getOrDefault("") else ""
                    data["cuenta"] = runCatching { spinnerCuenta.selectedItem?.toString() ?: "" }.getOrDefault("")
                    data["moneda"] = runCatching { spinnerMoneda.selectedItem?.toString() ?: "COP" }.getOrDefault("COP")
                    data["detalle"] = etDetalle.text.toString().trim()
                }
            }
        } catch (e: Exception) {
            Toast.makeText(requireContext(), "Error: ${e.message}", Toast.LENGTH_LONG).show(); return }

        mostrarDialogoConfirmacion(uid, data)
    }

    // ── Dialog de confirmación con estética de la app ─────────────────────────
    private fun mostrarDialogoConfirmacion(uid: String, data: MutableMap<String, Any>) {
        if (!isAdded) return
        val tipo   = data["tipoTransaccion"] as String
        val valor  = data["valor"] as Double
        val moneda = data["moneda"] as String
        val cat    = data["categoria"] as String
        val det    = data["detalle"] as String
        val cuenta = data["cuenta"] as String
        val origen = data["cuentaOrigen"] as String

        val colorTipo = when (tipo) {
            "Ingreso"       -> "#69F0AE"
            "Transferencia" -> "#2196F3"
            else            -> "#FF5252"
        }
        val emoji = when (tipo) { "Ingreso" -> "📈"; "Transferencia" -> "🔄"; else -> "📉" }

        // Inflar view personalizado
        val ctx  = requireContext()
        val raiz = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.parseColor("#111E2E"))
            setPadding(dpToPx(24), dpToPx(24), dpToPx(24), dpToPx(20))
        }

        // Emoji + tipo
        raiz.addView(TextView(ctx).apply {
            text = emoji; textSize = 44f; gravity = android.view.Gravity.CENTER
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                setMargins(0, 0, 0, dpToPx(4)) }
        })
        raiz.addView(TextView(ctx).apply {
            text = tipo; textSize = 18f; gravity = android.view.Gravity.CENTER
            setTextColor(Color.parseColor(colorTipo))
            setTypeface(typeface, android.graphics.Typeface.BOLD)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                setMargins(0, 0, 0, dpToPx(4)) }
        })
        raiz.addView(TextView(ctx).apply {
            text = formatValor(valor, moneda); textSize = 28f
            gravity = android.view.Gravity.CENTER
            setTextColor(Color.parseColor(colorTipo))
            setTypeface(typeface, android.graphics.Typeface.BOLD)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                setMargins(0, 0, 0, dpToPx(16)) }
        })

        // Divisor
        raiz.addView(divisorView(ctx))

        // Filas de info
        fun fila(label: String, valor2: String, color2: String = "#EEFFEE") {
            if (valor2.isEmpty()) return
            val row = LinearLayout(ctx).apply {
                orientation = LinearLayout.HORIZONTAL
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                    setMargins(0, dpToPx(8), 0, 0) }
            }
            row.addView(TextView(ctx).apply {
                text = label; setTextColor(Color.parseColor("#445566")); textSize = 13f
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            })
            row.addView(TextView(ctx).apply {
                text = valor2; setTextColor(Color.parseColor(color2)); textSize = 13f
                setTypeface(typeface, android.graphics.Typeface.BOLD)
                gravity = android.view.Gravity.END
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            })
            raiz.addView(row)
        }

        if (tipo == "Transferencia") {
            fila("Origen",  origen, "#2196F3")
            fila("Destino", cuenta, "#00BCD4")
        } else {
            if (cat.isNotEmpty()) fila("Categoría", cat)
            fila("Cuenta", cuenta)
        }
        fila("Detalle", det)
        fila("Moneda", moneda, if (moneda == "USD") "#2196F3" else "#4CAF50")

        raiz.addView(divisorView(ctx).apply {
            (layoutParams as LinearLayout.LayoutParams).setMargins(0, dpToPx(16), 0, dpToPx(16)) })

        // Botones
        val filaBtns = LinearLayout(ctx).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
        }

        val dialog = androidx.appcompat.app.AlertDialog.Builder(ctx)
            .setView(raiz).create()
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        val btnCancelar = Button(ctx).apply {
            text = "Cancelar"; isAllCaps = false; textSize = 14f
            setTextColor(Color.parseColor("#445566"))
            backgroundTintList = android.content.res.ColorStateList.valueOf(
                Color.parseColor("#0D1A2A"))
            layoutParams = LinearLayout.LayoutParams(0,
                LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply {
                setMargins(0, 0, dpToPx(8), 0) }
            setOnClickListener { dialog.dismiss() }
        }
        val btnConfirmar = Button(ctx).apply {
            text = "✅ Guardar"; isAllCaps = false; textSize = 14f
            setTextColor(Color.parseColor("#050510"))
            backgroundTintList = android.content.res.ColorStateList.valueOf(
                Color.parseColor("#4CAF50"))
            layoutParams = LinearLayout.LayoutParams(0,
                LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            setOnClickListener {
                dialog.dismiss()
                guardarEnFirebase(uid, data)
            }
        }
        filaBtns.addView(btnCancelar); filaBtns.addView(btnConfirmar)
        raiz.addView(filaBtns)

        dialog.show()
        dialog.window?.let { win ->
            val w = (resources.displayMetrics.widthPixels * 0.88).toInt()
            win.setLayout(w, LinearLayout.LayoutParams.WRAP_CONTENT)
        }
    }

    private fun divisorView(ctx: android.content.Context) = View(ctx).apply {
        layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, 1)
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
                limpiar(); btnGuardar.isEnabled = true; btnGuardar.text = "GUARDAR MOVIMIENTO"
            }
            .addOnFailureListener { e ->
                if (!isAdded) return@addOnFailureListener
                Toast.makeText(requireContext(), "Error: ${e.message}", Toast.LENGTH_LONG).show()
                btnGuardar.isEnabled = true; btnGuardar.text = "GUARDAR MOVIMIENTO"
            }
    }

    private fun limpiar() {
        if (!isAdded) return
        try { etDetalle.setText(""); etValor.setText("")
            etDetalleTransf.setText(""); etValorTransf.setText("") } catch (e: Exception) {}
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

    private fun dpToPx(dp: Int) = (dp * resources.displayMetrics.density).toInt()
}