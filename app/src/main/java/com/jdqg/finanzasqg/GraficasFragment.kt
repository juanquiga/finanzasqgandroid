package com.jdqg.finanzasqg

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.*
import android.widget.*
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import java.text.NumberFormat
import java.util.*

class GraficasFragment : Fragment() {

    private val db   = FirebaseDatabase.getInstance().reference
    private val auth = FirebaseAuth.getInstance()

    private var periodoGraf  = "mensual"
    private var filtroGraf   = "Todo"
    private var filtroMoneda = "AMBAS"

    private var listenerMovimientos: ValueEventListener? = null
    private var listenerConfig: ValueEventListener? = null
    private var refMov: DatabaseReference? = null
    private var refCfg: DatabaseReference? = null

    private val todosMovimientos = mutableListOf<Movimiento>()
    private var presupuestoMaxCOP = 0.0
    private var presupuestoMaxUSD = 0.0
    private val maximosCategorias = mutableMapOf<String, MutableMap<String, Double>>()

    private lateinit var btnMensual     : Button
    private lateinit var btnAnual       : Button
    private lateinit var btnTodo        : Button
    private lateinit var btnIngreso     : Button
    private lateinit var btnEgreso      : Button
    private lateinit var btnMonedaTodas : Button
    private lateinit var btnMonedaCOP   : Button
    private lateinit var btnMonedaUSD   : Button

    private var rootView: View? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val view = inflater.inflate(R.layout.fragment_graficas, container, false)
        rootView = view

        btnMensual     = view.findViewById(R.id.btnGrafMensual)
        btnAnual       = view.findViewById(R.id.btnGrafAnual)
        btnTodo        = view.findViewById(R.id.btnGrafTodo)
        btnIngreso     = view.findViewById(R.id.btnGrafIngreso)
        btnEgreso      = view.findViewById(R.id.btnGrafEgreso)
        btnMonedaTodas = view.findViewById(R.id.btnGrafMonedaTodas)
        btnMonedaCOP   = view.findViewById(R.id.btnGrafMonedaCOP)
        btnMonedaUSD   = view.findViewById(R.id.btnGrafMonedaUSD)

        btnMensual.setOnClickListener     { periodoGraf = "mensual"; actualizarBotPeriodo(); renderizar() }
        btnAnual.setOnClickListener       { periodoGraf = "anual";   actualizarBotPeriodo(); renderizar() }
        btnTodo.setOnClickListener        { filtroGraf = "Todo";     actualizarBotFiltro();  renderizar() }
        btnIngreso.setOnClickListener     { filtroGraf = "Ingreso";  actualizarBotFiltro();  renderizar() }
        btnEgreso.setOnClickListener      { filtroGraf = "Egreso";   actualizarBotFiltro();  renderizar() }
        btnMonedaTodas.setOnClickListener { filtroMoneda = "AMBAS";  actualizarBotMoneda();  renderizar() }
        btnMonedaCOP.setOnClickListener   { filtroMoneda = "COP";    actualizarBotMoneda();  renderizar() }
        btnMonedaUSD.setOnClickListener   { filtroMoneda = "USD";    actualizarBotMoneda();  renderizar() }

        view.findViewById<Button>(R.id.btnVerHistorial).setOnClickListener {
            startActivity(Intent(requireContext(), HistorialActivity::class.java))
        }

        actualizarBotPeriodo(); actualizarBotFiltro(); actualizarBotMoneda()

        val uid = auth.currentUser?.uid ?: return view
        iniciarListeners(uid)

        return view
    }

    private fun iniciarListeners(uid: String) {
        refCfg = db.child("configuracion").child(uid)
        listenerConfig = object : ValueEventListener {
            override fun onDataChange(snap: DataSnapshot) {

                presupuestoMaxCOP = snap.child("presupuestoMax")
                    .child("max_COP").getValue(Double::class.java) ?: 0.0
                presupuestoMaxUSD = snap.child("presupuestoMax")
                    .child("max_USD").getValue(Double::class.java) ?: 0.0

                // 🔥 CAMBIO AQUÍ (LO ÚNICO MODIFICADO)
                maximosCategorias.clear()

                val snapCat = if (snap.child("presupuestoCategorias").exists())
                    snap.child("presupuestoCategorias")
                else
                    snap.child("maximosCategorias")

                for (s in snapCat.children) {
                    val cat = s.key ?: continue
                    val m   = mutableMapOf<String, Double>()
                    m["max_COP"] = s.child("max_COP").getValue(Double::class.java) ?: 0.0
                    m["max_USD"] = s.child("max_USD").getValue(Double::class.java) ?: 0.0
                    maximosCategorias[cat] = m
                }

                if (isAdded) renderizar()
            }
            override fun onCancelled(e: DatabaseError) {}
        }
        refCfg?.addValueEventListener(listenerConfig!!)

        refMov = db.child("movimientos").child(uid)
        listenerMovimientos = object : ValueEventListener {
            override fun onDataChange(snap: DataSnapshot) {
                todosMovimientos.clear()
                for (s in snap.children) {
                    val m = s.getValue(Movimiento::class.java) ?: continue
                    todosMovimientos.add(m)
                }
                if (isAdded) renderizar()
            }
            override fun onCancelled(e: DatabaseError) {}
        }
        refMov?.addValueEventListener(listenerMovimientos!!)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        listenerMovimientos?.let { refMov?.removeEventListener(it) }
        listenerConfig?.let { refCfg?.removeEventListener(it) }
        rootView = null
    }

    // 🔽 TODO tu resto del código sigue EXACTAMENTE IGUAL (renderizar, tablas, etc.)

    private fun tint(btn: Button, activo: Boolean) {
        btn.backgroundTintList = android.content.res.ColorStateList.valueOf(
            if (activo) Color.parseColor("#4CAF50") else Color.parseColor("#0D1A2A"))
        btn.setTextColor(
            if (activo) Color.parseColor("#050510") else Color.parseColor("#4CAF50"))
    }

    private fun actualizarBotPeriodo() {
        tint(btnMensual, periodoGraf == "mensual")
        tint(btnAnual,   periodoGraf == "anual")
    }
    private fun actualizarBotFiltro() {
        tint(btnTodo,    filtroGraf == "Todo")
        tint(btnIngreso, filtroGraf == "Ingreso")
        tint(btnEgreso,  filtroGraf == "Egreso")
    }
    private fun actualizarBotMoneda() {
        tint(btnMonedaTodas, filtroMoneda == "AMBAS")
        tint(btnMonedaCOP,   filtroMoneda == "COP")
        tint(btnMonedaUSD,   filtroMoneda == "USD")
    }

    private fun fmt(valor: Double): String {
        return if (filtroMoneda == "USD") {
            val f = NumberFormat.getNumberInstance(Locale.US).apply {
                maximumFractionDigits = 2; minimumFractionDigits = 0 }
            "${f.format(valor)}USD"
        } else {
            val f = NumberFormat.getNumberInstance(Locale("es","CO")).apply {
                maximumFractionDigits = 0 }
            "$${f.format(valor)}"
        }
    }

    private fun renderizar() {
        val view = rootView ?: return
        if (!isAdded) return

        val ahora = Calendar.getInstance()

        // Paso 1: periodo
        val porPeriodo = todosMovimientos.filter { m ->
            val cal = Calendar.getInstance().apply { timeInMillis = m.timestamp }
            when (periodoGraf) {
                "mensual" -> cal.get(Calendar.MONTH) == ahora.get(Calendar.MONTH) &&
                        cal.get(Calendar.YEAR)  == ahora.get(Calendar.YEAR)
                else      -> cal.get(Calendar.YEAR)  == ahora.get(Calendar.YEAR)
            }
        }

        // Paso 2: moneda
        val porMoneda = when (filtroMoneda) {
            "COP"  -> porPeriodo.filter { it.moneda == "COP" }
            "USD"  -> porPeriodo.filter { it.moneda == "USD" }
            else   -> porPeriodo
        }

        // Paso 3: filtro tipo
        val filtrados = when (filtroGraf) {
            "Ingreso" -> porMoneda.filter { it.tipoTransaccion == "Ingreso" }
            "Egreso"  -> porMoneda.filter { it.tipoTransaccion == "Egreso" }
            else      -> porMoneda
        }

        val ingresos = porMoneda.filter { it.tipoTransaccion == "Ingreso" }.sumOf { it.valor }
        val egresos  = porMoneda.filter { it.tipoTransaccion == "Egreso"  }.sumOf { it.valor }

        // ── DONUT ────────────────────────────────────────────────────────────
        val graficaDonut = view.findViewById<GraficaDonutView>(R.id.graficaDonut)
        graficaDonut.setDatos(ingresos, egresos)

        // Leyenda donut
        val tvTituloDonut = view.findViewById<TextView>(R.id.tvTituloDonut)
        tvTituloDonut.text = when (filtroGraf) {
            "Ingreso" -> "📈 Distribución Ingresos"
            "Egreso"  -> "📉 Distribución Egresos"
            else      -> "Ingresos vs Egresos"
        }

        // Leyenda valores donut
        view.findViewById<TextView>(R.id.tvLeyendaIngresos).text = fmt(ingresos)
        view.findViewById<TextView>(R.id.tvLeyendaEgresos).text  = fmt(egresos)
        val balance = ingresos - egresos
        view.findViewById<TextView>(R.id.tvLeyendaBalance).apply {
            text = fmt(balance)
            setTextColor(Color.parseColor(if (balance >= 0) "#4CAF50" else "#FF5252"))
        }

        // ── BARRAS ────────────────────────────────────────────────────────────
        val graficaBarras  = view.findViewById<GraficaBarrasView>(R.id.graficaBarras)
        val tvTituloBarras = view.findViewById<TextView>(R.id.tvTituloBarras)

        tvTituloBarras.text = when (filtroGraf) {
            "Ingreso" -> "Categorías de Ingreso"
            "Egreso"  -> "Categorías de Egreso"
            else      -> "Por Categoría"
        }

        val porCategoria = filtrados
            .filter { it.categoria.isNotEmpty() && it.categoria != "Transferencia" }
            .groupBy { it.categoria }
            .mapValues { (_, v) -> v.sumOf { it.valor } }
            .entries.sortedByDescending { it.value }

        val colores = listOf(
            "#FF5252","#FF9800","#2196F3",
            "#9C27B0","#00BCD4","#4CAF50","#FF6B6B")
        graficaBarras.setDatos(
            porCategoria.take(6).mapIndexed { i, (cat, total) ->
                GraficaBarrasView.Barra(cat.take(8), total,
                    Color.parseColor(colores[i % colores.size]))
            })

        // ── TOP CATEGORÍAS ─────────────────────────────────────────────────
        val recyclerTop = view.findViewById<RecyclerView>(R.id.recyclerTopCategorias)
        recyclerTop.layoutManager = LinearLayoutManager(requireContext())
        val topItems = porCategoria.take(5).toList()

        recyclerTop.adapter = object : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
            inner class VH(v: View) : RecyclerView.ViewHolder(v) {
                val lbl  : TextView = v.findViewById(android.R.id.text1)
                val valor: TextView = v.findViewById(android.R.id.text2)
            }
            override fun onCreateViewHolder(p: ViewGroup, vt: Int) =
                VH(LayoutInflater.from(p.context)
                    .inflate(android.R.layout.simple_list_item_2, p, false))
            override fun onBindViewHolder(h: RecyclerView.ViewHolder, pos: Int) {
                val (cat, total) = topItems[pos]
                (h as VH).lbl.apply {
                    text = "${pos + 1}. $cat"
                    setTextColor(Color.parseColor("#AABBCC"))
                }
                h.valor.apply {
                    text = fmt(total)
                    setTextColor(Color.parseColor(
                        if (filtroGraf == "Ingreso") "#4CAF50" else "#FF5252"))
                }
            }
            override fun getItemCount() = topItems.size
        }

        // ── PROYECCIÓN GENERAL ────────────────────────────────────────────
        val monedaKey     = if (filtroMoneda == "USD") "USD" else "COP"
        val presupMax     = if (filtroMoneda == "USD") presupuestoMaxUSD else presupuestoMaxCOP

        // Egresos del MES actual (siempre mensual para proyección)
        val egresosMes = porMoneda.filter {
            it.tipoTransaccion == "Egreso" &&
                    run {
                        val cal = Calendar.getInstance().apply { timeInMillis = it.timestamp }
                        cal.get(Calendar.MONTH) == ahora.get(Calendar.MONTH) &&
                                cal.get(Calendar.YEAR)  == ahora.get(Calendar.YEAR)
                    }
        }.sumOf { it.valor }

        val tvPresupuesto   = view.findViewById<TextView>(R.id.tvPresupuestoActual)
        val tvGastoTotal    = view.findViewById<TextView>(R.id.tvGastoTotalMes)
        val barraProyeccion = view.findViewById<ProgressBar>(R.id.barraProyeccion)
        val tvPorcentaje    = view.findViewById<TextView>(R.id.tvPorcentajeGasto)

        tvGastoTotal.text = "Gastado este mes: ${fmt(egresosMes)}"

        if (presupMax > 0) {
            tvPresupuesto.text = "Presupuesto máximo: ${fmt(presupMax)}"
            val pct = ((egresosMes / presupMax) * 100).toInt().coerceIn(0, 100)
            barraProyeccion.progress = pct
            tvPorcentaje.text = "$pct% del presupuesto usado"
            val color = when {
                pct >= 90 -> "#FF5252"
                pct >= 70 -> "#FF9800"
                else      -> "#4CAF50"
            }
            tvPorcentaje.setTextColor(Color.parseColor(color))
            barraProyeccion.progressTintList =
                android.content.res.ColorStateList.valueOf(Color.parseColor(color))
        } else {
            tvPresupuesto.text = "Sin presupuesto configurado"
            barraProyeccion.progress = 0
            tvPorcentaje.text = "Ve a ⚙️ → Modificar categorías → 💳 Máximos"
            tvPorcentaje.setTextColor(Color.parseColor("#445566"))
        }

        // ── CONTROL POR CATEGORÍA ─────────────────────────────────────────
        val tablaMaxCat = view.findViewById<LinearLayout>(R.id.tablaMaximosCategorias)
        renderizarControlCategorias(tablaMaxCat, porMoneda, ahora, monedaKey)

        // ── TABLA CUENTAS ─────────────────────────────────────────────────
        val tablaCuentas = view.findViewById<LinearLayout>(R.id.tablaCuentas)
        renderizarTablaCuentas(tablaCuentas, porMoneda)

        // Historial count
        view.findViewById<TextView>(R.id.tvTotalHistorial).text =
            "${todosMovimientos.size} registros totales"
    }

    private fun renderizarControlCategorias(
        container: LinearLayout,
        movimientos: List<Movimiento>,
        ahora: Calendar,
        monedaKey: String
    ) {
        container.removeAllViews()

        // Gastos reales por categoría en el mes
        val gastoPorCat = movimientos
            .filter {
                it.tipoTransaccion == "Egreso" &&
                        run {
                            val cal = Calendar.getInstance().apply { timeInMillis = it.timestamp }
                            cal.get(Calendar.MONTH) == ahora.get(Calendar.MONTH) &&
                                    cal.get(Calendar.YEAR)  == ahora.get(Calendar.YEAR)
                        }
            }
            .groupBy { it.categoria }
            .mapValues { (_, v) -> v.sumOf { it.valor } }

        // Unir categorías con máximo configurado Y categorías con gasto
        val todasCats = mutableSetOf<String>()
        todasCats.addAll(maximosCategorias.keys)
        todasCats.addAll(gastoPorCat.keys)

        if (todasCats.isEmpty()) {
            container.addView(TextView(requireContext()).apply {
                text = "Sin datos. Agrega egresos o configura máximos en ⚙️"
                setTextColor(Color.parseColor("#445566"))
                textSize = 12f
                gravity = android.view.Gravity.CENTER
                setPadding(0, dpToPx(12), 0, dpToPx(12))
            })
            return
        }

        for (cat in todasCats.sorted()) {
            val gasto = gastoPorCat[cat] ?: 0.0
            val max   = maximosCategorias[cat]?.get("max_$monedaKey") ?: 0.0
            val pct   = if (max > 0) ((gasto / max) * 100).toInt().coerceIn(0, 100) else 0

            val color = when {
                max <= 0  -> "#2196F3"   // Sin límite — azul
                pct >= 90 -> "#FF5252"   // Peligro — rojo
                pct >= 70 -> "#FF9800"   // Advertencia — naranja
                else      -> "#4CAF50"   // Ok — verde
            }

            // Card para cada categoría
            val card = LinearLayout(requireContext()).apply {
                orientation = LinearLayout.VERTICAL
                setBackgroundColor(Color.parseColor("#0D1A2A"))
                setPadding(dpToPx(12), dpToPx(10), dpToPx(12), dpToPx(10))
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                    setMargins(0, 0, 0, dpToPx(8))
                }
            }

            // Fila superior: nombre + porcentaje
            val filaTop = LinearLayout(requireContext()).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity     = android.view.Gravity.CENTER_VERTICAL
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                    setMargins(0, 0, 0, dpToPx(6))
                }
            }
            filaTop.addView(TextView(requireContext()).apply {
                text = cat
                setTextColor(Color.WHITE)
                textSize = 13f
                setTypeface(typeface, android.graphics.Typeface.BOLD)
                layoutParams = LinearLayout.LayoutParams(0,
                    LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            })
            filaTop.addView(TextView(requireContext()).apply {
                text = if (max > 0) "$pct%" else "Sin límite"
                setTextColor(Color.parseColor(color))
                textSize = 12f
                setTypeface(typeface, android.graphics.Typeface.BOLD)
            })
            card.addView(filaTop)

            // Fila valores: gastado / máximo
            card.addView(TextView(requireContext()).apply {
                text = if (max > 0)
                    "Gastado: ${fmt(gasto)}  /  Máx: ${fmt(max)}"
                else
                    "Gastado: ${fmt(gasto)}  (sin límite configurado)"
                setTextColor(Color.parseColor("#AABBCC"))
                textSize = 11f
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                    setMargins(0, 0, 0, dpToPx(6))
                }
            })

            // Barra de progreso solo si hay máximo
            if (max > 0) {
                val barra = ProgressBar(requireContext(), null,
                    android.R.attr.progressBarStyleHorizontal).apply {
                    this.max = 100
                    progress = pct
                    progressTintList = android.content.res.ColorStateList.valueOf(
                        Color.parseColor(color))
                    progressBackgroundTintList = android.content.res.ColorStateList.valueOf(
                        Color.parseColor("#1E3A4A"))
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT, dpToPx(14))
                }
                card.addView(barra)
            }

            container.addView(card)
        }
    }

    private fun renderizarTablaCuentas(container: LinearLayout, movimientos: List<Movimiento>) {
        container.removeAllViews()

        data class CuentaR(
            val nombre: String,
            var ingresos: Double = 0.0,
            var egresos: Double  = 0.0,
            var enviadas: Double  = 0.0,
            var recibidas: Double = 0.0,
            var total: Int = 0
        )

        val mapC = mutableMapOf<String, CuentaR>()

        for (m in movimientos) {
            when (m.tipoTransaccion) {
                "Ingreso" -> {
                    val r = mapC.getOrPut(m.cuenta) { CuentaR(m.cuenta) }
                    r.ingresos += m.valor; r.total++
                }
                "Egreso" -> {
                    val r = mapC.getOrPut(m.cuenta) { CuentaR(m.cuenta) }
                    r.egresos += m.valor; r.total++
                }
                "Transferencia" -> {
                    if (m.cuentaOrigen.isNotEmpty()) {
                        val ro = mapC.getOrPut(m.cuentaOrigen) { CuentaR(m.cuentaOrigen) }
                        ro.enviadas += m.valor; ro.total++
                    }
                    if (m.cuenta.isNotEmpty()) {
                        val rd = mapC.getOrPut(m.cuenta) { CuentaR(m.cuenta) }
                        rd.recibidas += m.valor; rd.total++
                    }
                }
            }
        }

        if (mapC.isEmpty()) {
            container.addView(TextView(requireContext()).apply {
                text = "Sin movimientos en el período seleccionado"
                setTextColor(Color.parseColor("#445566"))
                textSize = 12f
                gravity = android.view.Gravity.CENTER
                setPadding(0, dpToPx(16), 0, dpToPx(16))
            })
            return
        }

        // Header
        try {
            val header = layoutInflater.inflate(R.layout.item_cuenta_header, container, false)
            container.addView(header)
        } catch (e: Exception) { /* ignorar si no existe */ }

        val ordenadas = mapC.values.sortedByDescending { it.total }

        for ((idx, cuenta) in ordenadas.withIndex()) {
            // Card por cuenta
            val card = LinearLayout(requireContext()).apply {
                orientation = LinearLayout.VERTICAL
                setBackgroundColor(
                    if (idx == 0) Color.parseColor("#0D2A0D")
                    else Color.parseColor("#0D1A2A"))
                setPadding(dpToPx(12), dpToPx(10), dpToPx(12), dpToPx(10))
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                    setMargins(0, 0, 0, dpToPx(6))
                }
            }

            // Nombre + medalla
            card.addView(TextView(requireContext()).apply {
                text = "${if (idx == 0) "🥇" else if (idx == 1) "🥈" else "🥉"} ${
                    cuenta.nombre.ifEmpty { "Sin cuenta" }}"
                setTextColor(if (idx == 0) Color.parseColor("#4CAF50") else Color.WHITE)
                textSize = 14f
                setTypeface(typeface, android.graphics.Typeface.BOLD)
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                    setMargins(0, 0, 0, dpToPx(8))
                }
            })

            // Grid de valores
            val grid = GridLayout(requireContext()).apply {
                columnCount = 2
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT)
            }

            fun addRow(label: String, valor: Double, color: String) {
                val params = GridLayout.LayoutParams().apply {
                    width = 0
                    columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f)
                }
                grid.addView(TextView(requireContext()).apply {
                    text = label
                    setTextColor(Color.parseColor("#445566"))
                    textSize = 11f
                    layoutParams = params
                })
                grid.addView(TextView(requireContext()).apply {
                    text = fmt(valor)
                    setTextColor(Color.parseColor(color))
                    textSize = 11f
                    setTypeface(typeface, android.graphics.Typeface.BOLD)
                    gravity = android.view.Gravity.END
                    layoutParams = params
                })
            }

            addRow("📈 Ingresos",     cuenta.ingresos,  "#69F0AE")
            addRow("📉 Egresos",      cuenta.egresos,   "#FF5252")
            addRow("↑ Enviadas",      cuenta.enviadas,  "#2196F3")
            addRow("↓ Recibidas",     cuenta.recibidas, "#00BCD4")
            val balanceCta = cuenta.ingresos + cuenta.recibidas - cuenta.egresos - cuenta.enviadas
            addRow("Balance",         balanceCta,
                if (balanceCta >= 0) "#4CAF50" else "#FF5252")

            card.addView(grid)
            container.addView(card)
        }
    }

    private fun dpToPx(dp: Int) = (dp * resources.displayMetrics.density).toInt()
}