package com.jdqg.finanzasqg

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View

class GraficaBarrasView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : View(context, attrs) {

    data class Barra(val label: String, val valor: Double, val color: Int)

    private var barras: List<Barra> = emptyList()

    private val paintBarra = Paint().apply {
        style = Paint.Style.FILL; isAntiAlias = true }
    private val paintFondo = Paint().apply {
        color = Color.parseColor("#1E3A4A")
        style = Paint.Style.FILL; isAntiAlias = true }
    private val paintLabel = Paint().apply {
        color = Color.parseColor("#AABBCC")
        textSize = 24f; isAntiAlias = true
        textAlign = Paint.Align.LEFT }
    private val paintValor = Paint().apply {
        color = Color.WHITE; textSize = 22f; isAntiAlias = true
        textAlign = Paint.Align.RIGHT
        typeface = Typeface.DEFAULT_BOLD }

    fun setDatos(data: List<Barra>) { barras = data; invalidate() }

    override fun onDraw(canvas: Canvas) {
        if (barras.isEmpty()) return
        val max      = barras.maxOf { it.valor }.takeIf { it > 0 } ?: 1.0
        // Barras MÁS DELGADAS — 18dp en lugar de 28dp
        val barH     = 18f
        val gap      = (height - barras.size * barH) / (barras.size + 1f)
        val labelW   = 150f
        val valorW   = 140f
        val barStart = labelW + 8f
        val barEnd   = width - valorW - 8f
        val barW     = (barEnd - barStart).coerceAtLeast(10f)

        barras.forEachIndexed { i, b ->
            val top   = gap + i * (barH + gap)
            val midY  = top + barH / 2f
            val fillW = (b.valor / max * barW).toFloat().coerceAtLeast(4f)
            val r     = barH / 2f

            canvas.drawRoundRect(
                RectF(barStart, top, barStart + barW, top + barH),
                r, r, paintFondo)

            paintBarra.color = b.color
            canvas.drawRoundRect(
                RectF(barStart, top, barStart + fillW, top + barH),
                r, r, paintBarra)

            canvas.drawText(b.label, 0f, midY + 8f, paintLabel)
            canvas.drawText(
                "$${java.text.NumberFormat.getNumberInstance(java.util.Locale("es","CO"))
                    .apply { maximumFractionDigits = 0 }.format(b.valor)}",
                width.toFloat(), midY + 8f, paintValor)
        }
    }
}