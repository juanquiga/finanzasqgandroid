package com.jdqg.finanzasqg

import android.content.Intent
import android.graphics.Color
import android.view.*
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

class HistorialAdapter(
    private val lista: MutableList<Movimiento>
) : RecyclerView.Adapter<HistorialAdapter.VH>() {

    private val db   = FirebaseDatabase.getInstance().reference
    private val auth = FirebaseAuth.getInstance()
    private val sdf  = SimpleDateFormat("dd/MM/yyyy  HH:mm", Locale.getDefault())

    class VH(v: View) : RecyclerView.ViewHolder(v) {
        val tvTipo     : TextView = v.findViewById(R.id.tvTipoIndicador)
        val tvCategoria: TextView = v.findViewById(R.id.tvCategoria)
        val tvDetalle  : TextView = v.findViewById(R.id.tvDetalle)
        val tvFecha    : TextView = v.findViewById(R.id.tvFecha)
        val tvValor    : TextView = v.findViewById(R.id.tvValor)
        val btnEliminar: Button   = v.findViewById(R.id.btnEliminarMovimiento)
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
        holder.tvDetalle.text   = m.detalle.ifEmpty {
            if (esTransf) "${m.cuentaOrigen} → ${m.cuenta}" else "-" }
        holder.tvFecha.text     = if (m.timestamp > 0) sdf.format(Date(m.timestamp))
        else m.fechaTransaccion
        holder.tvValor.text     = formatValor(m.valor, m.moneda)
        holder.tvValor.setTextColor(Color.parseColor(when {
            esIngreso -> "#69F0AE"; esTransf -> "#2196F3"; else -> "#FF5252" }))

        holder.itemView.setOnClickListener {
            val ctx = holder.itemView.context
            ctx.startActivity(Intent(ctx, DetalleMovimientoActivity::class.java).apply {
                putExtra("tipo",         m.tipoTransaccion)
                putExtra("categoria",    m.categoria)
                putExtra("subcategoria", m.subcategoria)
                putExtra("detalle",      m.detalle)
                putExtra("valor",        m.valor)
                putExtra("timestamp",    m.timestamp)
                putExtra("cuenta",       m.cuenta)
                putExtra("moneda",       m.moneda)
                putExtra("cuentaOrigen", m.cuentaOrigen)
            })
        }

        holder.btnEliminar.setOnClickListener {
            mostrarDialogoEliminar(holder.itemView.context, m)
        }
    }

    private fun mostrarDialogoEliminar(ctx: android.content.Context, m: Movimiento) {
        val tipo  = m.tipoTransaccion
        val emoji = when (tipo) { "Ingreso" -> "📈"; "Transferencia" -> "🔄"; else -> "📉" }
        val color = when (tipo) { "Ingreso" -> "#69F0AE"; "Transferencia" -> "#2196F3"; else -> "#FF5252" }

        val raiz = android.widget.LinearLayout(ctx).apply {
            orientation = android.widget.LinearLayout.VERTICAL
            setBackgroundColor(Color.parseColor("#111E2E"))
            setPadding(dp(ctx,24), dp(ctx,24), dp(ctx,24), dp(ctx,20))
        }
        raiz.addView(TextView(ctx).apply {
            text = "🗑️"; textSize = 36f; gravity = android.view.Gravity.CENTER
            layoutParams = lp().apply { setMargins(0,0,0,dp(ctx,8)) }
        })
        raiz.addView(TextView(ctx).apply {
            text = "Eliminar registro"; textSize = 18f; gravity = android.view.Gravity.CENTER
            setTextColor(Color.parseColor("#FF5252"))
            setTypeface(typeface, android.graphics.Typeface.BOLD)
            layoutParams = lp().apply { setMargins(0,0,0,dp(ctx,4)) }
        })
        raiz.addView(TextView(ctx).apply {
            text = "$emoji  $tipo  •  ${formatValor(m.valor, m.moneda)}"
            textSize = 15f; gravity = android.view.Gravity.CENTER
            setTextColor(Color.parseColor(color))
            layoutParams = lp().apply { setMargins(0,0,0,dp(ctx,6)) }
        })
        raiz.addView(TextView(ctx).apply {
            text = "Esta acción no se puede deshacer"
            textSize = 12f; gravity = android.view.Gravity.CENTER
            setTextColor(Color.parseColor("#445566"))
            layoutParams = lp().apply { setMargins(0,0,0,dp(ctx,16)) }
        })
        raiz.addView(View(ctx).apply {
            layoutParams = android.widget.LinearLayout.LayoutParams(
                android.widget.LinearLayout.LayoutParams.MATCH_PARENT, 1).apply {
                setMargins(0,0,0,dp(ctx,16)) }
            setBackgroundColor(Color.parseColor("#1E3A4A"))
        })

        val filaBtns = android.widget.LinearLayout(ctx).apply {
            orientation = android.widget.LinearLayout.HORIZONTAL
            layoutParams = lp()
        }
        val dialog = AlertDialog.Builder(ctx).setView(raiz).create()
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        filaBtns.addView(Button(ctx).apply {
            text = "Cancelar"; isAllCaps = false; textSize = 14f
            setTextColor(Color.parseColor("#445566"))
            backgroundTintList = android.content.res.ColorStateList.valueOf(Color.parseColor("#0D1A2A"))
            layoutParams = android.widget.LinearLayout.LayoutParams(0,
                android.widget.LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply {
                setMargins(0,0,dp(ctx,8),0) }
            setOnClickListener { dialog.dismiss() }
        })
        filaBtns.addView(Button(ctx).apply {
            text = "🗑 Eliminar"; isAllCaps = false; textSize = 14f
            setTextColor(Color.WHITE)
            backgroundTintList = android.content.res.ColorStateList.valueOf(Color.parseColor("#C62828"))
            layoutParams = android.widget.LinearLayout.LayoutParams(0,
                android.widget.LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            setOnClickListener {
                dialog.dismiss()
                val uid = auth.currentUser?.uid ?: return@setOnClickListener
                if (m.id.isEmpty()) return@setOnClickListener
                db.child("movimientos").child(uid).child(m.id).removeValue()
                    .addOnSuccessListener {
                        val pos = lista.indexOfFirst { it.id == m.id }
                        if (pos >= 0) { lista.removeAt(pos); notifyItemRemoved(pos) }
                    }
            }
        })
        raiz.addView(filaBtns)
        dialog.show()
        dialog.window?.let {
            val w = (ctx.resources.displayMetrics.widthPixels * 0.85).toInt()
            it.setLayout(w, android.widget.LinearLayout.LayoutParams.WRAP_CONTENT)
        }
    }

    override fun getItemCount() = lista.size

    private fun lp() = android.widget.LinearLayout.LayoutParams(
        android.widget.LinearLayout.LayoutParams.MATCH_PARENT,
        android.widget.LinearLayout.LayoutParams.WRAP_CONTENT)

    private fun dp(ctx: android.content.Context, dp: Int) =
        (dp * ctx.resources.displayMetrics.density).toInt()

    private fun formatValor(valor: Double, moneda: String): String {
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