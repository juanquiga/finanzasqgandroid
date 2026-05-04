package com.jdqg.finanzasqg

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.*
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.viewpager2.widget.ViewPager2
import androidx.recyclerview.widget.RecyclerView

class OnboardingActivity : AppCompatActivity() {

    private val paginas = listOf(
        PaginaOnboarding(
            emoji = "💰",
            titulo = "Bienvenido a FinanzasQG",
            subtitulo = "Tu asistente financiero personal",
            descripcion = "Controla tus finanzas de forma inteligente. Registra ingresos, egresos y transferencias en segundos. Todo sincronizado entre iOS y Android en tiempo real.",
            color = "#4CAF50",
            bullets = listOf(
                "✅ Sincronización en tiempo real",
                "✅ Disponible en iOS y Android",
                "✅ Datos 100% privados y seguros",
                "✅ Sin anuncios, sin costos ocultos"
            )
        ),
        PaginaOnboarding(
            emoji = "📊",
            titulo = "Visualiza tu dinero",
            subtitulo = "Gráficas y reportes inteligentes",
            descripcion = "Entiende en dónde gastas tu dinero con gráficas claras. Compara ingresos vs egresos, analiza por categoría y lleva un historial completo de todos tus movimientos.",
            color = "#2196F3",
            bullets = listOf(
                "📈 Gráfica de barras por categoría",
                "🍩 Donut de ingresos vs egresos",
                "🏦 Resumen por cuenta bancaria",
                "📋 Historial filtrable por mes y año"
            )
        ),
        PaginaOnboarding(
            emoji = "🎯",
            titulo = "Presupuestos inteligentes",
            subtitulo = "Ponle límite a tus gastos",
            descripcion = "Define un presupuesto general mensual y también por categoría. Las subcategorías suman automáticamente hacia arriba. Recibe alertas visuales cuando estés cerca del límite.",
            color = "#FF9800",
            bullets = listOf(
                "💳 Presupuesto general COP y USD",
                "🗂️ Presupuesto por categoría",
                "📂 Por subcategoría y subsubcategoría",
                "🚨 Alerta visual al superar el límite"
            )
        ),
        PaginaOnboarding(
            emoji = "🔄",
            titulo = "Registra cualquier movimiento",
            subtitulo = "Egresos, ingresos y transferencias",
            descripcion = "Registra tus gastos e ingresos en segundos. Elige la fecha exacta con el calendario integrado. Organiza todo por categorías que tú mismo defines.",
            color = "#9C27B0",
            bullets = listOf(
                "📉 Egresos con categoría jerárquica",
                "📈 Ingresos por fuente de dinero",
                "🔄 Transferencias entre cuentas",
                "📅 Elige la fecha de cada movimiento"
            )
        ),
        PaginaOnboarding(
            emoji = "🔒",
            titulo = "Tu información es tuya",
            subtitulo = "Privacidad y seguridad total",
            descripcion = "Cada cuenta es completamente independiente. Tus categorías, cuentas y presupuestos son exclusivamente tuyos. Protege el acceso con huella digital.",
            color = "#F44336",
            bullets = listOf(
                "🔐 Huella digital para mayor seguridad",
                "👤 Datos aislados por usuario",
                "☁️ Respaldo automático en la nube",
                "🚫 Sin acceso de terceros a tus datos"
            )
        ),
        PaginaOnboarding(
            emoji = "⚙️",
            titulo = "Personaliza tu experiencia",
            subtitulo = "Todo a tu manera",
            descripcion = "Crea tus propias categorías con subcategorías y subsubcategorías. Agrega tus cuentas bancarias. Luego configura presupuestos para tener control total.",
            color = "#00BCD4",
            bullets = listOf(
                "🗂️ Categorías completamente personalizables",
                "🏦 Agrega todas tus cuentas bancarias",
                "💰 Presupuestos por nivel jerárquico",
                "🔄 Sincronización iOS ↔ Android"
            )
        )
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_onboarding)

        val viewPager  = findViewById<ViewPager2>(R.id.viewPagerOnboarding)
        val indicators = findViewById<LinearLayout>(R.id.indicadoresOnboarding)
        val btnSig     = findViewById<Button>(R.id.btnSiguienteOnboarding)
        val btnSaltar  = findViewById<TextView>(R.id.btnSaltarOnboarding)

        viewPager.adapter = object : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
            override fun getItemCount() = paginas.size
            override fun onCreateViewHolder(parent: ViewGroup, vt: Int) =
                object : RecyclerView.ViewHolder(
                    LayoutInflater.from(parent.context)
                        .inflate(R.layout.item_onboarding_page, parent, false)) {}

            override fun onBindViewHolder(holder: RecyclerView.ViewHolder, pos: Int) {
                val p = paginas[pos]
                val v = holder.itemView
                v.findViewById<TextView>(R.id.tvOnboardingEmoji).text = p.emoji
                v.findViewById<TextView>(R.id.tvOnboardingTitulo).apply {
                    text = p.titulo
                    setTextColor(Color.parseColor(p.color))
                }
                v.findViewById<TextView>(R.id.tvOnboardingSubtitulo).text = p.subtitulo
                v.findViewById<TextView>(R.id.tvOnboardingDescripcion).text = p.descripcion

                val bulletContainer = v.findViewById<LinearLayout>(R.id.containerBullets)
                bulletContainer.removeAllViews()
                p.bullets.forEach { bullet ->
                    bulletContainer.addView(TextView(this@OnboardingActivity).apply {
                        text = bullet
                        setTextColor(Color.parseColor("#EEFFEE"))
                        textSize = 14f
                        layoutParams = LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.MATCH_PARENT,
                            LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                            setMargins(0, 0, 0, dp(8))
                        }
                    })
                }

                // Color del fondo de bullets
                v.findViewById<View>(R.id.cardBullets).background =
                    android.graphics.drawable.GradientDrawable().apply {
                        setColor(Color.parseColor(p.color).and(0x1AFFFFFF.toInt()))
                        cornerRadius = dp(14).toFloat()
                        setStroke(1, Color.parseColor(p.color).and(0x33FFFFFF.toInt()))
                    }
            }
        }

        // Crear indicadores
        fun actualizarIndicadores(pos: Int) {
            indicators.removeAllViews()
            paginas.indices.forEach { i ->
                val dot = View(this).apply {
                    background = android.graphics.drawable.GradientDrawable().apply {
                        shape = android.graphics.drawable.GradientDrawable.OVAL
                        setColor(if (i == pos) Color.parseColor(paginas[pos].color)
                        else Color.parseColor("#1A3A4A"))
                    }
                    val size = if (i == pos) dp(24) else dp(8)
                    layoutParams = LinearLayout.LayoutParams(size, dp(8)).apply {
                        setMargins(dp(3), 0, dp(3), 0)
                    }
                }
                indicators.addView(dot)
            }
        }
        actualizarIndicadores(0)

        viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(pos: Int) {
                actualizarIndicadores(pos)
                val esUltima = pos == paginas.size - 1
                btnSig.text = if (esUltima) "¡Personalizar mi app!" else "Siguiente →"
                btnSig.backgroundTintList = android.content.res.ColorStateList.valueOf(
                    Color.parseColor(paginas[pos].color))
                btnSaltar.visibility = if (esUltima) View.INVISIBLE else View.VISIBLE
            }
        })

        btnSig.setOnClickListener {
            val actual = viewPager.currentItem
            if (actual < paginas.size - 1) {
                viewPager.currentItem = actual + 1
            } else {
                irASetup()
            }
        }

        btnSaltar.setOnClickListener { irASetup() }
    }

    // Al terminar onboarding → Setup (categorías) → Presupuesto → App
    private fun irASetup() {
        startActivity(Intent(this, SetupCategoriasActivity::class.java))
        finish()
    }

    private fun dp(dp: Int) = (dp * resources.displayMetrics.density).toInt()

    data class PaginaOnboarding(
        val emoji: String,
        val titulo: String,
        val subtitulo: String,
        val descripcion: String,
        val color: String,
        val bullets: List<String>
    )
}