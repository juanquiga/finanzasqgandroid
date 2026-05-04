package com.jdqg.finanzasqg

import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

class DetalleMovimientoActivity : AppCompatActivity() {

    private val db   = FirebaseDatabase.getInstance().reference
    private val auth = FirebaseAuth.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_detalle_movimiento)

        val id          = intent.getStringExtra("id")          ?: ""
        val tipo        = intent.getStringExtra("tipo")         ?: ""
        val categoria   = intent.getStringExtra("categoria")    ?: ""
        val subcat      = intent.getStringExtra("subcategoria") ?: ""
        val subsubcat   = intent.getStringExtra("subsubcategoria") ?: ""
        val detalle     = intent.getStringExtra("detalle")      ?: ""
        val valor       = intent.getDoubleExtra("valor",         0.0)
        val timestamp   = intent.getLongExtra("timestamp",       0L)
        val cuenta      = intent.getStringExtra("cuenta")       ?: ""
        val moneda      = intent.getStringExtra("moneda")       ?: "COP"
        val cuentaOrig  = intent.getStringExtra("cuentaOrigen") ?: ""

        val esIngreso = tipo == "Ingreso"
        val esTransf  = tipo == "Transferencia"

        val colorTipo = when {
            esIngreso -> "#69F0AE"
            esTransf  -> "#2196F3"
            else      -> "#FF5252"
        }
        val emoji = when { esIngreso -> "📈"; esTransf -> "🔄"; else -> "📉" }
        val bgEmoji = when {
            esIngreso -> "#1B4332"
            esTransf  -> "#0D2A4A"
            else      -> "#4A1010"
        }

        // Header emoji
        findViewById<TextView>(R.id.tvDetalleTipoEmoji).apply {
            text = emoji
            setBackgroundColor(Color.parseColor(bgEmoji))
        }
        findViewById<TextView>(R.id.tvDetalleTipo).apply {
            text = tipo.uppercase()
            setTextColor(Color.parseColor(colorTipo))
        }
        findViewById<TextView>(R.id.tvDetalleValor).apply {
            text = formatValor(valor, moneda)
            setTextColor(Color.parseColor(colorTipo))
        }
        findViewById<TextView>(R.id.tvDetalleFechaHeader).text =
            SimpleDateFormat("dd/MM/yyyy  HH:mm", Locale.getDefault())
                .format(Date(timestamp))

        // Filas de detalle — mostrar/ocultar según datos
        fun fila(rowId: Int, labelId: Int, valueId: Int, label: String,
                 value: String, colorValue: String = "#EEFFEE") {
            val row = findViewById<View>(rowId) ?: return
            if (value.isEmpty()) { row.visibility = View.GONE; return }
            row.visibility = View.VISIBLE
            (row.findViewWithTag<TextView>("label") ?: return).text = label
            (row.findViewWithTag<TextView>("value") ?: return).apply {
                text = value
                setTextColor(Color.parseColor(colorValue))
            }
        }

        // Categoría
        setFila(R.id.filaCategoria, "🏷 Categoría",
            categoria.ifEmpty { tipo }, colorTipo)
        // Subcategoría
        setFila(R.id.filaSubcategoria, "📂 Subcategoría", subcat)
        // Sub-subcategoría
        setFila(R.id.filaSubsubcategoria, "📌 Detalle categ.", subsubcat)
        // Descripción
        setFila(R.id.filaDetalle, "📝 Descripción", detalle)
        // Cuenta
        if (esTransf) {
            setFila(R.id.filaCuenta, "🔄 Transferencia",
                "${cuentaOrig.ifEmpty { "—" }} → ${cuenta.ifEmpty { "—" }}", "#2196F3")
        } else {
            setFila(R.id.filaCuenta, "🏦 Cuenta", cuenta)
        }
        // Moneda
        val monedaLabel = if (moneda == "USD") "🇺🇸 USD" else "🇨🇴 COP"
        setFila(R.id.filaMoneda, "💱 Moneda", monedaLabel)
        // Fecha
        setFila(R.id.filaFecha, "📅 Fecha",
            SimpleDateFormat("dd/MM/yyyy  HH:mm", Locale.getDefault())
                .format(Date(timestamp)), "#AABBCC")

        // Volver
        findViewById<View>(R.id.btnVolverDetalle).setOnClickListener { finish() }

        // ── BOTÓN ELIMINAR ────────────────────────────────────────────────────
        findViewById<Button>(R.id.btnEliminarDetalle).setOnClickListener {
            mostrarDialogoEliminar(id, tipo, valor, moneda)
        }
    }

    private fun setFila(rowId: Int, label: String, value: String,
                        colorValue: String = "#EEFFEE") {
        val row = findViewById<View>(rowId) ?: return
        if (value.isEmpty()) { row.visibility = View.GONE; return }
        row.visibility = View.VISIBLE
        row.findViewWithTag<TextView>("label")?.text = label
        row.findViewWithTag<TextView>("value")?.apply {
            text = value
            setTextColor(Color.parseColor(colorValue))
        }
    }

    private fun mostrarDialogoEliminar(id: String, tipo: String,
                                       valor: Double, moneda: String) {
        val ctx  = this
        val raiz = android.widget.LinearLayout(ctx).apply {
            orientation = android.widget.LinearLayout.VERTICAL
            setBackgroundColor(Color.parseColor("#111E2E"))
            setPadding(dp(24), dp(24), dp(24), dp(20))
        }

        raiz.addView(TextView(ctx).apply {
            text = "🗑️"; textSize = 36f; gravity = android.view.Gravity.CENTER
            layoutParams = android.widget.LinearLayout.LayoutParams(
                android.widget.LinearLayout.LayoutParams.MATCH_PARENT,
                android.widget.LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                setMargins(0,0,0,dp(8)) }
        })
        raiz.addView(TextView(ctx).apply {
            text = "¿Eliminar este registro?"
            textSize = 18f; gravity = android.view.Gravity.CENTER
            setTextColor(Color.parseColor("#FF5252"))
            setTypeface(typeface, android.graphics.Typeface.BOLD)
            layoutParams = android.widget.LinearLayout.LayoutParams(
                android.widget.LinearLayout.LayoutParams.MATCH_PARENT,
                android.widget.LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                setMargins(0,0,0,dp(6)) }
        })
        raiz.addView(TextView(ctx).apply {
            text = "$tipo  •  ${formatValor(valor, moneda)}"
            textSize = 14f; gravity = android.view.Gravity.CENTER
            setTextColor(Color.parseColor("#AABBCC"))
            layoutParams = android.widget.LinearLayout.LayoutParams(
                android.widget.LinearLayout.LayoutParams.MATCH_PARENT,
                android.widget.LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                setMargins(0,0,0,dp(6)) }
        })
        raiz.addView(TextView(ctx).apply {
            text = "Esta acción no se puede deshacer"
            textSize = 12f; gravity = android.view.Gravity.CENTER
            setTextColor(Color.parseColor("#445566"))
            layoutParams = android.widget.LinearLayout.LayoutParams(
                android.widget.LinearLayout.LayoutParams.MATCH_PARENT,
                android.widget.LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                setMargins(0,0,0,dp(20)) }
        })
        raiz.addView(View(ctx).apply {
            layoutParams = android.widget.LinearLayout.LayoutParams(
                android.widget.LinearLayout.LayoutParams.MATCH_PARENT, 1).apply {
                setMargins(0,0,0,dp(16)) }
            setBackgroundColor(Color.parseColor("#1E3A4A"))
        })

        val filaBtns = android.widget.LinearLayout(ctx).apply {
            orientation = android.widget.LinearLayout.HORIZONTAL
            layoutParams = android.widget.LinearLayout.LayoutParams(
                android.widget.LinearLayout.LayoutParams.MATCH_PARENT,
                android.widget.LinearLayout.LayoutParams.WRAP_CONTENT) }

        val dialog = AlertDialog.Builder(ctx).setView(raiz).create()
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
            text = "🗑 Eliminar"; isAllCaps = false; textSize = 14f
            setTextColor(Color.WHITE)
            backgroundTintList = android.content.res.ColorStateList.valueOf(
                Color.parseColor("#C62828"))
            layoutParams = android.widget.LinearLayout.LayoutParams(0,
                android.widget.LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            setOnClickListener {
                dialog.dismiss()
                val uid = auth.currentUser?.uid ?: return@setOnClickListener
                if (id.isEmpty()) return@setOnClickListener
                db.child("movimientos").child(uid).child(id).removeValue()
                    .addOnSuccessListener { finish() }
            }
        })
        raiz.addView(filaBtns)
        dialog.show()
        dialog.window?.let {
            val w = (resources.displayMetrics.widthPixels * 0.86).toInt()
            it.setLayout(w, android.widget.LinearLayout.LayoutParams.WRAP_CONTENT) }
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