package com.jdqg.finanzasqg

import android.graphics.Color
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

class DetalleMovimientoActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_detalle_movimiento)

        val tipo       = intent.getStringExtra("tipo")          ?: ""
        val categoria  = intent.getStringExtra("categoria")     ?: ""
        val subcat     = intent.getStringExtra("subcategoria")  ?: ""
        val detalle    = intent.getStringExtra("detalle")       ?: ""
        val valor      = intent.getDoubleExtra("valor",          0.0)
        val timestamp  = intent.getLongExtra("timestamp",        0L)
        val cuenta     = intent.getStringExtra("cuenta")        ?: ""
        val moneda     = intent.getStringExtra("moneda")        ?: "COP"
        val cuentaOrig = intent.getStringExtra("cuentaOrigen")  ?: ""

        val esIngreso = tipo == "Ingreso"
        val esTransf  = tipo == "Transferencia"

        val colorTipo = when {
            esIngreso -> "#69F0AE"
            esTransf  -> "#2196F3"
            else      -> "#FF5252"
        }
        val emoji = when {
            esTransf  -> "🔄"
            esIngreso -> "📈"
            else      -> "📉"
        }

        findViewById<TextView>(R.id.tvDetalleTipoEmoji).text  = emoji
        findViewById<TextView>(R.id.tvDetalleTipo).apply {
            text      = tipo
            setTextColor(Color.parseColor(colorTipo))
        }
        findViewById<TextView>(R.id.tvDetalleValor).apply {
            text      = formatValor(valor, moneda)
            setTextColor(Color.parseColor(colorTipo))
        }
        findViewById<TextView>(R.id.tvDetalleMoneda).text     = moneda
        findViewById<TextView>(R.id.tvDetalleCategoria).text  =
            categoria.ifEmpty { "-" }
        findViewById<TextView>(R.id.tvDetalleSubcat).text     =
            subcat.ifEmpty { "-" }
        findViewById<TextView>(R.id.tvDetalleDetalle).text    =
            detalle.ifEmpty { "-" }
        findViewById<TextView>(R.id.tvDetalleCuenta).text     =
            if (esTransf) "$cuentaOrig → $cuenta"
            else cuenta.ifEmpty { "-" }
        findViewById<TextView>(R.id.tvDetalleFecha).text      =
            SimpleDateFormat("dd/MM/yyyy  HH:mm", Locale.getDefault())
                .format(Date(timestamp))

        findViewById<Button>(R.id.btnCerrarDetalle).setOnClickListener { finish() }
    }

    private fun formatValor(valor: Double, moneda: String): String {
        return if (moneda == "USD") {
            val f = NumberFormat.getNumberInstance(Locale.US).apply {
                maximumFractionDigits = 2; minimumFractionDigits = 0
            }
            "${f.format(valor)} USD"
        } else {
            val f = NumberFormat.getNumberInstance(Locale("es","CO")).apply {
                maximumFractionDigits = 0
            }
            "$${f.format(valor)}"
        }
    }
}