package com.jdqg.finanzasqg

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View
import java.text.NumberFormat
import java.util.Locale

class GraficaDonutView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : View(context, attrs) {

    private var ingresos = 0.0
    private var egresos  = 0.0
    private val stroke   = 48f

    private val paintIngreso = Paint().apply {
        color       = Color.parseColor("#4CAF50")
        style       = Paint.Style.STROKE
        strokeWidth = stroke
        isAntiAlias = true
        strokeCap   = Paint.Cap.BUTT
    }
    private val paintEgreso = Paint().apply {
        color       = Color.parseColor("#FF5252")
        style       = Paint.Style.STROKE
        strokeWidth = stroke
        isAntiAlias = true
        strokeCap   = Paint.Cap.BUTT
    }
    private val paintFondo = Paint().apply {
        color       = Color.parseColor("#1E3A4A")
        style       = Paint.Style.STROKE
        strokeWidth = stroke
        isAntiAlias = true
    }
    private val paintTexto = Paint().apply {
        color     = Color.WHITE
        textAlign = Paint.Align.CENTER
        textSize  = 38f
        typeface  = Typeface.DEFAULT_BOLD
        isAntiAlias = true
    }
    private val paintSubTexto = Paint().apply {
        color     = Color.parseColor("#445566")
        textAlign = Paint.Align.CENTER
        textSize  = 24f
        isAntiAlias = true
    }

    fun setDatos(ing: Double, egr: Double) {
        ingresos = ing; egresos = egr; invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        val total = ingresos + egresos
        val cx    = width  / 2f
        val cy    = height / 2f
        val radio = minOf(cx, cy) - stroke - 16f
        val rect  = RectF(cx - radio, cy - radio, cx + radio, cy + radio)

        canvas.drawArc(rect, 0f, 360f, false, paintFondo)

        if (total > 0) {
            val angIng = (ingresos / total * 360).toFloat()
            val angEgr = (egresos  / total * 360).toFloat()
            // Ingresos verde desde -90
            canvas.drawArc(rect, -90f, angIng, false, paintIngreso)
            // Egresos rojo a continuación
            canvas.drawArc(rect, -90f + angIng, angEgr, false, paintEgreso)

            val pctIng = (ingresos / total * 100).toInt()
            val pctEgr = (egresos  / total * 100).toInt()

            // Texto centro
            canvas.drawText("${pctIng}%", cx, cy - 10f, paintTexto.apply {
                color = Color.parseColor("#4CAF50")
            })
            canvas.drawText("${pctEgr}%", cx, cy + 34f, paintTexto.apply {
                color = Color.parseColor("#FF5252")
                textSize = 32f
            })
        } else {
            canvas.drawText("0%", cx, cy + 14f, paintTexto.apply {
                color = Color.WHITE; textSize = 38f
            })
        }
    }
}