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

    private var periodoActual = "mensual"
    private var filtroTipo    = "Todo"
    private var filtroMoneda  = "AMBAS"

    private var listenerMov: ValueEventListener? = null
    private var refMov: DatabaseReference? = null

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

        tvSaludo      = view.findViewById(R.id.tvSaludoResumen)
        tvIngresos    = view.findViewById(R.id.tvTotalIngresos)
        tvEgresos     = view.findViewById(R.id.tvTotalEgresos)
        tvBalance     = view.findViewById(R.id.tvBalance)
        cardBalance   = view.findViewById(R.id.cardBalance)
        cardIngresos  = view.findViewById(R.id.cardIngresos)
        cardEgresos   = view.findViewById(R.id.cardEgresos)
        btnDiario     = view.findViewById(R.id.btnDiario)
        btnMensual    = view.findViewById(R.id.btnMensual)
        btnAnual      = view.findViewById(R.id.btnAnual)
        btnTodo       = view.findViewById(R.id.btnFiltroTodo)
        btnIngreso    = view.findViewById(R.id.btnFiltroIngreso)
        btnEgreso     = view.findViewById(R.id.btnFiltroEgreso)
        btnTransf     = view.findViewById(R.id.btnFiltroTransferencia)
        btnMonedaTodas = view.findViewById(R.id.btnMonedaTodas)
        btnMonedaCOP   = view.findViewById(R.id.btnMonedaCOP)
        btnMonedaUSD   = view.findViewById(R.id.btnMonedaUSD)

        val recycler = view.findViewById<RecyclerView>(R.id.recyclerMovimientos)
        recycler.layoutManager = LinearLayoutManager(requireContext())
        adapter = MovimientoAdapter(listaFiltrada)
        recycler.adapter = adapter

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

        btnDiario.setOnClickListener  { periodoActual = "diario";  actualizarBotonesPeriodo(); aplicarFiltros() }
        btnMensual.setOnClickListener { periodoActual = "mensual"; actualizarBotonesPeriodo(); aplicarFiltros() }
        btnAnual.setOnClickListener   { periodoActual = "anual";   actualizarBotonesPeriodo(); aplicarFiltros() }
        btnTodo.setOnClickListener    { filtroTipo = "Todo";       actualizarBotonesFiltr();   aplicarFiltros() }
        btnIngreso.setOnClickListener { filtroTipo = "Ingreso";    actualizarBotonesFiltr();   aplicarFiltros() }
        btnEgreso.setOnClickListener  { filtroTipo = "Egreso";     actualizarBotonesFiltr();   aplicarFiltros() }
        btnTransf.setOnClickListener  { filtroTipo = "Transferencia"; actualizarBotonesFiltr(); aplicarFiltros() }
        btnMonedaTodas.setOnClickListener { filtroMoneda = "AMBAS"; actualizarBotonesMoneda(); aplicarFiltros() }
        btnMonedaCOP.setOnClickListener   { filtroMoneda = "COP";   actualizarBotonesMoneda(); aplicarFiltros() }
        btnMonedaUSD.setOnClickListener   { filtroMoneda = "USD";   actualizarBotonesMoneda(); aplicarFiltros() }

        actualizarBotonesPeriodo(); actualizarBotonesFiltr(); actualizarBotonesMoneda()
        iniciarListener(uid)
        return view
    }

    // Obtiene la fecha efectiva del movimiento (personalizada o timestamp)
    private fun fechaEfectiva(m: Movimiento): Long =
        if (m.fechaPersonalizada > 0) m.fechaPersonalizada else m.timestamp

    private fun iniciarListener(uid: String) {
        refMov = db.child("movimientos").child(uid)
        listenerMov = object : ValueEventListener {
            override fun onDataChange(snap: DataSnapshot) {
                todos.clear()
                for (child in snap.children) {
                    val m = child.getValue(Movimiento::class.java) ?: continue
                    todos.add(m)
                }
                todos.sortByDescending { fechaEfectiva(it) }
                if (isAdded) aplicarFiltros()
            }
            override fun onCancelled(e: DatabaseError) {}
        }
        refMov?.addValueEventListener(listenerMov!!)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        listenerMov?.let { refMov?.removeEventListener(it) }
    }

    private fun tint(btn: Button, activo: Boolean, clr: String = "#4CAF50") {
        btn.backgroundTintList = ColorStateList.valueOf(
            if (activo) Color.parseColor(clr) else Color.parseColor("#0D1A2A"))
        btn.setTextColor(if (activo) Color.parseColor("#050510") else Color.parseColor(clr))
    }

    private fun actualizarBotonesPeriodo() {
        tint(btnDiario, periodoActual=="diario"); tint(btnMensual, periodoActual=="mensual")
        tint(btnAnual, periodoActual=="anual")
    }
    private fun actualizarBotonesFiltr() {
        tint(btnTodo, filtroTipo=="Todo"); tint(btnIngreso, filtroTipo=="Ingreso")
        tint(btnEgreso, filtroTipo=="Egreso"); tint(btnTransf, filtroTipo=="Transferencia")
        cardIngresos.visibility = if (filtroTipo == "Egreso" || filtroTipo == "Transferencia") View.GONE else View.VISIBLE
        cardEgresos.visibility  = if (filtroTipo == "Ingreso" || filtroTipo == "Transferencia") View.GONE else View.VISIBLE
        cardBalance.visibility  = if (filtroTipo == "Todo") View.VISIBLE else View.GONE
    }
    private fun actualizarBotonesMoneda() {
        tint(btnMonedaTodas, filtroMoneda=="AMBAS"); tint(btnMonedaCOP, filtroMoneda=="COP")
        tint(btnMonedaUSD, filtroMoneda=="USD")
    }

    private fun aplicarFiltros() {
        if (!isAdded) return
        val ahora = Calendar.getInstance()

        // Usa fechaEfectiva para los filtros de período
        val porPeriodo = todos.filter { m ->
            val cal = Calendar.getInstance().apply { timeInMillis = fechaEfectiva(m) }
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

        val monedaD  = if (filtroMoneda == "USD") "USD" else "COP"
        val ingresos = porMoneda.filter { it.tipoTransaccion == "Ingreso" }.sumOf { it.valor }
        val egresos  = porMoneda.filter { it.tipoTransaccion == "Egreso"  }.sumOf { it.valor }
        val balance  = ingresos - egresos

        tvIngresos.text = fmt(ingresos, monedaD)
        tvEgresos.text  = fmt(egresos,  monedaD)
        tvBalance.text  = fmt(balance,  monedaD)
        tvBalance.setTextColor(Color.parseColor(if (balance >= 0) "#4CAF50" else "#FF5252"))
    }

    private fun fmt(valor: Double, moneda: String): String {
        return if (moneda == "USD") {
            val f = NumberFormat.getNumberInstance(Locale.US).apply {
                maximumFractionDigits = 2; minimumFractionDigits = 0 }
            "${f.format(valor)}USD"
        } else {
            val f = NumberFormat.getNumberInstance(Locale("es","CO")).apply {
                maximumFractionDigits = 0 }
            "$${f.format(valor)}"
        }
    }
}