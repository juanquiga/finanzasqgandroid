package com.jdqg.finanzasqg

import android.content.Intent
import android.graphics.Color
import android.view.*
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

class HistorialAdapter(
    private val lista: MutableList<Movimiento>
) : RecyclerView.Adapter<HistorialAdapter.VH>() {

    private val sdf = SimpleDateFormat("dd/MM/yyyy  HH:mm", Locale.getDefault())

    class VH(v: View) : RecyclerView.ViewHolder(v) {
        val tvTipo     : TextView = v.findViewById(R.id.tvTipoIndicador)
        val tvCategoria: TextView = v.findViewById(R.id.tvCategoria)
        val tvDetalle  : TextView = v.findViewById(R.id.tvDetalle)
        val tvFecha    : TextView = v.findViewById(R.id.tvFecha)
        val tvValor    : TextView = v.findViewById(R.id.tvValor)
    }

    override fun onCreateViewHolder(parent: ViewGroup, vt: Int): VH {
        val v = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_movimiento, parent, false)
        return VH(v)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val m         = lista[position]
        val esIngreso = m.tipoTransaccion == "Ingreso"
        val esTransf  = m.tipoTransaccion == "Transferencia"

        holder.tvTipo.text = when { esTransf -> "🔄"; esIngreso -> "📈"; else -> "📉" }
        holder.tvTipo.setBackgroundResource(
            if (esIngreso) R.drawable.finanzas_badge_ingreso
            else R.drawable.finanzas_badge_egreso)

        holder.tvCategoria.text = m.categoria.ifEmpty { m.tipoTransaccion }
        holder.tvDetalle.text = m.detalle.ifEmpty {
            if (esTransf) "${m.cuentaOrigen} → ${m.cuenta}" else "-" }

        val fechaMs = if (m.fechaPersonalizada > 0) m.fechaPersonalizada else m.timestamp
        holder.tvFecha.text = if (fechaMs > 0) sdf.format(Date(fechaMs))
        else m.fechaTransaccion

        holder.tvValor.text = fmt(m.valor, m.moneda)
        holder.tvValor.setTextColor(Color.parseColor(when {
            esIngreso -> "#69F0AE"; esTransf -> "#2196F3"; else -> "#FF5252" }))

        holder.itemView.setOnClickListener {
            val ctx = holder.itemView.context
            ctx.startActivity(Intent(ctx, DetalleMovimientoActivity::class.java).apply {
                putExtra("id",              m.id)
                putExtra("tipo",            m.tipoTransaccion)
                putExtra("categoria",       m.categoria)
                putExtra("subcategoria",    m.subcategoria)
                putExtra("subsubcategoria", m.subsubcategoria)
                putExtra("detalle",         m.detalle)
                putExtra("valor",           m.valor)
                putExtra("timestamp",       fechaMs)
                putExtra("cuenta",          m.cuenta)
                putExtra("moneda",          m.moneda)
                putExtra("cuentaOrigen",    m.cuentaOrigen)
            })
        }
    }

    override fun getItemCount() = lista.size

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