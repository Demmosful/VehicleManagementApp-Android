package com.demmos.parqueaderoapp.data.model

data class Vehiculo(
    val id: String = "",
    val matricula: String = "",
    val marcaNombre: String = "",
    val modeloNombre: String = "",
    val ubicacion: String = "",
    val detalle: String? = null,
    val fechaIngreso: Long = 0L,
    val estado: String = "",
    val usuarioRegistrador: String = "",
    val nombreCompletoRegistrador: String? = null,


    val fechaSalida: Long? = null,
    val usuarioSalidaId: String? = null,
    val nombreCompletoUsuarioSalida: String? = null
)