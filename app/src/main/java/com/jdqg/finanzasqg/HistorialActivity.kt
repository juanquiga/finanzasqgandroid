package com.jdqg.finanzasqg

import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.addTextChangedListener
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import java.util.*

class HistorialActivity : AppCompatActivity() {

    private val db       = FirebaseDatabase.getInstance().reference
    private val auth     = FirebaseAuth.getInstance()
    private val todos    = mutableListOf<Movimiento>()
    private val filtrados = mutableListOf<Movimiento>()
    private lateinit var adapter: HistorialAdapter

    private var filtroPeriodo    = "Todo"
    private var mesSeleccionado  = Calendar.getInstance().get(Calendar.MONTH)
    private var anioSeleccionado = Calendar.getInstance().get(Calendar.YEAR)

    private val nombresMeses = listOf(
        "Enero","Febrero","Marzo","Abril","Mayo","Junio",
        "Julio","Agosto","Septiembre","Octubre","Noviembre","Diciembre")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_historial)

        val recycler      = findViewById<RecyclerView>(R.id.recyclerHistorialCompleto)
        val etBuscar      = findViewById<EditText>(R.id.etBuscarHistorial)
        val tvCount       = findViewById<TextView>(R.id.tvCountHistorial)
        val btnVolver     = findViewById<ImageButton>(R.id.btnVolverHistorial)
        val btnTodo       = findViewById<Button>(R.id.btnHistTodo)
        val btnMes        = findViewById<Button>(R.id.btnHistMes)
        val btnAnio       = findViewById<Button>(R.id.btnHistAnio)
        val spinnerMes    = findViewById<Spinner>(R.id.spinnerMesHistorial)
        val spinnerAnio   = findViewById<Spinner>(R.id.spinnerAnioHistorial)

        recycler.layoutManager = LinearLayoutManager(this)
        adapter = HistorialAdapter(filtrados)
        recycler.adapter = adapter

        btnVolver.setOnClickListener { finish() }

        // Años desde 2026 hasta el año actual + 5 años adelante
        val anioActual = Calendar.getInstance().get(Calendar.YEAR)
        val anioBase   = 2026
        val anioFin    = maxOf(anioActual, anioBase) + 5
        val anios      = (anioBase..anioFin).map { it.toString() }

        spinnerMes.adapter = ArrayAdapter(this,
            android.R.layout.simple_spinner_item, nombresMeses).also {
            it.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item) }
        spinnerMes.setSelection(mesSeleccionado, false)

        spinnerAnio.adapter = ArrayAdapter(this,
            android.R.layout.simple_spinner_item, anios).also {
            it.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item) }
        // Seleccionar el año actual
        val idxAnio = anios.indexOf(anioActual.toString()).takeIf { it >= 0 } ?: 0
        spinnerAnio.setSelection(idxAnio, false)

        spinnerMes.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(p: AdapterView<*>?, v: View?, pos: Int, id: Long) {
                mesSeleccionado = pos
                aplicarFiltros(etBuscar.text.toString(), tvCount)
            }
            override fun onNothingSelected(p: AdapterView<*>?) {}
        }
        spinnerAnio.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(p: AdapterView<*>?, v: View?, pos: Int, id: Long) {
                anioSeleccionado = anios[pos].toInt()
                aplicarFiltros(etBuscar.text.toString(), tvCount)
            }
            override fun onNothingSelected(p: AdapterView<*>?) {}
        }

        fun tint(btn: Button, activo: Boolean) {
            btn.backgroundTintList = android.content.res.ColorStateList.valueOf(
                if (activo) android.graphics.Color.parseColor("#4CAF50")
                else android.graphics.Color.parseColor("#0D1A2A"))
            btn.setTextColor(
                if (activo) android.graphics.Color.parseColor("#050510")
                else android.graphics.Color.parseColor("#4CAF50"))
        }

        fun actualizarBotones() {
            tint(btnTodo, filtroPeriodo == "Todo")
            tint(btnMes,  filtroPeriodo == "Mes")
            tint(btnAnio, filtroPeriodo == "Año")
            spinnerMes.visibility  =
                if (filtroPeriodo == "Mes")  View.VISIBLE else View.GONE
            spinnerAnio.visibility =
                if (filtroPeriodo == "Mes" || filtroPeriodo == "Año") View.VISIBLE
                else View.GONE
        }

        btnTodo.setOnClickListener {
            filtroPeriodo = "Todo"; actualizarBotones()
            aplicarFiltros(etBuscar.text.toString(), tvCount) }
        btnMes.setOnClickListener {
            filtroPeriodo = "Mes"; actualizarBotones()
            aplicarFiltros(etBuscar.text.toString(), tvCount) }
        btnAnio.setOnClickListener {
            filtroPeriodo = "Año"; actualizarBotones()
            aplicarFiltros(etBuscar.text.toString(), tvCount) }

        actualizarBotones()

        etBuscar.addTextChangedListener { text ->
            aplicarFiltros(text.toString().trim(), tvCount)
        }

        val uid = auth.currentUser?.uid ?: return
        db.child("movimientos").child(uid)
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snap: DataSnapshot) {
                    todos.clear()
                    for (s in snap.children) {
                        val m = s.getValue(Movimiento::class.java) ?: continue
                        todos.add(m)
                    }
                    todos.sortByDescending { it.timestamp }
                    aplicarFiltros(etBuscar.text.toString(), tvCount)
                }
                override fun onCancelled(e: DatabaseError) {}
            })
    }

    private fun aplicarFiltros(query: String, tvCount: TextView) {
        val porPeriodo = when (filtroPeriodo) {
            "Mes" -> todos.filter { m ->
                val c = Calendar.getInstance().apply { timeInMillis = m.timestamp }
                c.get(Calendar.MONTH) == mesSeleccionado &&
                        c.get(Calendar.YEAR)  == anioSeleccionado
            }
            "Año" -> todos.filter { m ->
                val c = Calendar.getInstance().apply { timeInMillis = m.timestamp }
                c.get(Calendar.YEAR) == anioSeleccionado
            }
            else -> todos
        }

        filtrados.clear()
        filtrados.addAll(
            if (query.isEmpty()) porPeriodo
            else porPeriodo.filter {
                it.categoria.lowercase().contains(query.lowercase()) ||
                        it.detalle.lowercase().contains(query.lowercase()) ||
                        it.subcategoria.lowercase().contains(query.lowercase()) ||
                        it.cuenta.lowercase().contains(query.lowercase())
            }
        )
        tvCount.text = "${filtrados.size} registros"
        adapter.notifyDataSetChanged()
    }
}