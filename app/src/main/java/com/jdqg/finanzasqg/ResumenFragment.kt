package com.jdqg.finanzasqg

import android.content.res.ColorStateList
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

class ResumenFragment : Fragment() {

    private val db   = FirebaseDatabase.getInstance().reference
    private val auth = FirebaseAuth.getInstance()

    private val todos         = mutableListOf<Movimiento>()
    private val listaFiltrada = mutableListOf<Movimiento>()
    private lateinit var adapter: MovimientoAdapter

    private var periodoActual  = "mensual"
    private var filtroTipo     = "Todo"
    private var filtroMoneda   = "AMBAS"

    // Listeners activos — para removerlos al destruir el fragment
    private var listenerMovimientos: ValueEventListener? = null
    private var refMovimientos: DatabaseReference? = null

    private lateinit var tvSaludo    : TextView
    private lateinit var tvIngresos  : TextView
    private lateinit var tvEgresos   : TextView
    private lateinit var tvBalance   : TextView
    private lateinit var cardBalance : LinearLayout
    private lateinit var cardIngresos: LinearLayout
    private lateinit var cardEgresos : LinearLayout
    private lateinit var btnDiario   : Button
    private lateinit var btnMensual  : Button
    private lateinit var btnAnual    : Button
    private lateinit var btnTodo     : Button
    private lateinit var btnIngreso  : Button
    private lateinit var btnEgreso   : Button
    private lateinit var btnTransf   : Button
    private lateinit var btnMonedaTodas: Button
    private lateinit var btnMonedaCOP  : Button
    private lateinit var btnMonedaUSD  : Button

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_resumen, container, false)

        tvSaludo       = view.findViewById(R.id.tvSaludoResumen)
        tvIngresos     = view.findViewById(R.id.tvTotalIngresos)
        tvEgresos      = view.findViewById(R.id.tvTotalEgresos)
        tvBalance      = view.findViewById(R.id.tvBalance)
        cardBalance    = view.findViewById(R.id.cardBalance)
        cardIngresos   = view.findViewById(R.id.cardIngresos)
        cardEgresos    = view.findViewById(R.id.cardEgresos)
        btnDiario      = view.findViewById(R.id.btnDiario)
        btnMensual     = view.findViewById(R.id.btnMensual)
        btnAnual       = view.findViewById(R.id.btnAnual)
        btnTodo        = view.findViewById(R.id.btnFiltroTodo)
        btnIngreso     = view.findViewById(R.id.btnFiltroIngreso)
        btnEgreso      = view.findViewById(R.id.btnFiltroEgreso)
        btnTransf      = view.findViewById(R.id.btnFiltroTransferencia)
        btnMonedaTodas = view.findViewById(R.id.btnMonedaTodas)
        btnMonedaCOP   = view.findViewById(R.id.btnMonedaCOP)
        btnMonedaUSD   = view.findViewById(R.id.btnMonedaUSD)

        val recycler = view.findViewById<RecyclerView>(R.id.recyclerMovimientos)
        recycler.layoutManager = LinearLayoutManager(requireContext())
        adapter = MovimientoAdapter(listaFiltrada)
        recycler.adapter = adapter

        // Cargar nombre
        val uid = auth.currentUser?.uid ?: ""
        db.child("users").child(uid)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snap: DataSnapshot) {
                    val nombre = snap.child("name").getValue(String::class.java)
                        ?: auth.currentUser?.email?.substringBefore("@") ?: "usuario"
                    if (isAdded) tvSaludo.text = "Hola, $nombre 👋"
                }
                override fun onCancelled(e: DatabaseError) {}
            })

        // Botones período
        btnDiario.setOnClickListener  { periodoActual = "diario";  actualizarBotonesPeriodo(); aplicarFiltros() }
        btnMensual.setOnClickListener { periodoActual = "mensual"; actualizarBotonesPeriodo(); aplicarFiltros() }
        btnAnual.setOnClickListener   { periodoActual = "anual";   actualizarBotonesPeriodo(); aplicarFiltros() }

        // Botones tipo
        btnTodo.setOnClickListener    { filtroTipo = "Todo";          actualizarBotonesFiltr(); aplicarFiltros() }
        btnIngreso.setOnClickListener { filtroTipo = "Ingreso";       actualizarBotonesFiltr(); aplicarFiltros() }
        btnEgreso.setOnClickListener  { filtroTipo = "Egreso";        actualizarBotonesFiltr(); aplicarFiltros() }
        btnTransf.setOnClickListener  { filtroTipo = "Transferencia"; actualizarBotonesFiltr(); aplicarFiltros() }

        // Botones moneda
        btnMonedaTodas.setOnClickListener { filtroMoneda = "AMBAS"; actualizarBotonesMoneda(); aplicarFiltros() }
        btnMonedaCOP.setOnClickListener   { filtroMoneda = "COP";   actualizarBotonesMoneda(); aplicarFiltros() }
        btnMonedaUSD.setOnClickListener   { filtroMoneda = "USD";   actualizarBotonesMoneda(); aplicarFiltros() }

        actualizarBotonesPeriodo()
        actualizarBotonesFiltr()
        actualizarBotonesMoneda()

        // Escuchar movimientos en tiempo real — igual que Xcode
        iniciarListenerMovimientos(uid)

        return view
    }

    private fun iniciarListenerMovimientos(uid: String) {
        refMovimientos = db.child("movimientos").child(uid)
        listenerMovimientos = object : ValueEventListener {
            override fun onDataChange(snap: DataSnapshot) {
                todos.clear()
                for (child in snap.children) {
                    val m = child.getValue(Movimiento::class.java) ?: continue
                    todos.add(m)
                }
                todos.sortByDescending { it.timestamp }
                if (isAdded) aplicarFiltros()
            }
            override fun onCancelled(e: DatabaseError) {}
        }
        refMovimientos?.addValueEventListener(listenerMovimientos!!)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        listenerMovimientos?.let { refMovimientos?.removeEventListener(it) }
    }

    private fun tint(btn: Button, activo: Boolean, colorActivo: String = "#4CAF50") {
        btn.backgroundTintList = ColorStateList.valueOf(
            if (activo) Color.parseColor(colorActivo) else Color.parseColor("#0D1A2A"))
        btn.setTextColor(
            if (activo) Color.parseColor("#050510") else Color.parseColor(colorActivo))
    }

    private fun actualizarBotonesPeriodo() {
        tint(btnDiario,  periodoActual == "diario")
        tint(btnMensual, periodoActual == "mensual")
        tint(btnAnual,   periodoActual == "anual")
    }

    private fun actualizarBotonesFiltr() {
        tint(btnTodo,   filtroTipo == "Todo")
        tint(btnIngreso,filtroTipo == "Ingreso")
        tint(btnEgreso, filtroTipo == "Egreso")
        tint(btnTransf, filtroTipo == "Transferencia")

        cardIngresos.visibility = if (filtroTipo == "Egreso" ||
            filtroTipo == "Transferencia") View.GONE else View.VISIBLE
        cardEgresos.visibility  = if (filtroTipo == "Ingreso" ||
            filtroTipo == "Transferencia") View.GONE else View.VISIBLE
        cardBalance.visibility  = if (filtroTipo == "Todo") View.VISIBLE else View.GONE
    }

    private fun actualizarBotonesMoneda() {
        tint(btnMonedaTodas, filtroMoneda == "AMBAS")
        tint(btnMonedaCOP,   filtroMoneda == "COP")
        tint(btnMonedaUSD,   filtroMoneda == "USD")
    }

    private fun aplicarFiltros() {
        if (!isAdded) return
        val ahora = Calendar.getInstance()

        val porPeriodo = todos.filter { m ->
            val cal = Calendar.getInstance().apply { timeInMillis = m.timestamp }
            when (periodoActual) {
                "diario"  -> cal.get(Calendar.DAY_OF_YEAR) == ahora.get(Calendar.DAY_OF_YEAR) &&
                        cal.get(Calendar.YEAR) == ahora.get(Calendar.YEAR)
                "mensual" -> cal.get(Calendar.MONTH) == ahora.get(Calendar.MONTH) &&
                        cal.get(Calendar.YEAR)  == ahora.get(Calendar.YEAR)
                else      -> cal.get(Calendar.YEAR) == ahora.get(Calendar.YEAR)
            }
        }

        val porMoneda = when (filtroMoneda) {
            "COP"  -> porPeriodo.filter { it.moneda == "COP" }
            "USD"  -> porPeriodo.filter { it.moneda == "USD" }
            else   -> porPeriodo
        }

        listaFiltrada.clear()
        listaFiltrada.addAll(when (filtroTipo) {
            "Ingreso"       -> porMoneda.filter { it.tipoTransaccion == "Ingreso" }
            "Egreso"        -> porMoneda.filter { it.tipoTransaccion == "Egreso" }
            "Transferencia" -> porMoneda.filter { it.tipoTransaccion == "Transferencia" }
            else            -> porMoneda
        })
        adapter.notifyDataSetChanged()

        val monedaDisplay = if (filtroMoneda == "USD") "USD" else "COP"
        val ingresos = porMoneda.filter { it.tipoTransaccion == "Ingreso" }.sumOf { it.valor }
        val egresos  = porMoneda.filter { it.tipoTransaccion == "Egreso"  }.sumOf { it.valor }
        val balance  = ingresos - egresos

        tvIngresos.text = formatValor(ingresos, monedaDisplay)
        tvEgresos.text  = formatValor(egresos,  monedaDisplay)
        tvBalance.text  = formatValor(balance,  monedaDisplay)
        tvBalance.setTextColor(Color.parseColor(if (balance >= 0) "#4CAF50" else "#FF5252"))
    }

    private fun formatValor(valor: Double, moneda: String): String {
        return if (moneda == "USD") {
            val f = NumberFormat.getNumberInstance(Locale.US).apply {
                maximumFractionDigits = 2; minimumFractionDigits = 0
            }
            "${f.format(valor)}USD"
        } else {
            val f = NumberFormat.getNumberInstance(Locale("es","CO")).apply {
                maximumFractionDigits = 0
            }
            "$${f.format(valor)}"
        }
    }
}