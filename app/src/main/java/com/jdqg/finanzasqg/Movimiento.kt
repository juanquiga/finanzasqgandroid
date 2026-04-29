package com.jdqg.finanzasqg

data class Movimiento(
    var id: String = "",
    var tipoTransaccion: String = "",
    var detalle: String = "",
    var categoria: String = "",
    var subcategoria: String = "",
    var subsubcategoria: String = "",
    var valor: Double = 0.0,
    var timestamp: Long = 0L,
    var cuenta: String = "",
    var moneda: String = "COP",
    var cuentaOrigen: String = "",
    var cuentaDestino: String = "",
    var fechaTransaccion: String = ""
)