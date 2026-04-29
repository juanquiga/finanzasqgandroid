package com.jdqg.finanzasqg

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.*
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import java.util.Calendar

class CuentaFragment : Fragment() {

    private val auth  = FirebaseAuth.getInstance()
    private val db    = FirebaseDatabase.getInstance().reference
    private val PREFS = "finanzasqg_prefs"

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view  = inflater.inflate(R.layout.fragment_cuenta, container, false)
        val user  = auth.currentUser
        val uid   = user?.uid ?: ""

        val tvNombre = view.findViewById<TextView>(R.id.tvNombreCuenta)
        val tvEmail  = view.findViewById<TextView>(R.id.tvEmailCuenta)
        val tvTotal  = view.findViewById<TextView>(R.id.tvTotalMovimientos)
        val tvAnio   = view.findViewById<TextView>(R.id.tvMesActivo)

        tvEmail.text = user?.email ?: "sin correo"
        tvAnio.text  = Calendar.getInstance().get(Calendar.YEAR).toString()

        db.child("users").child(uid)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snap: DataSnapshot) {
                    tvNombre.text = snap.child("name").getValue(String::class.java)
                        ?: user?.email?.substringBefore("@") ?: "Usuario"
                }
                override fun onCancelled(e: DatabaseError) {}
            })

        db.child("movimientos").child(uid)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snap: DataSnapshot) {
                    tvTotal.text = snap.childrenCount.toString()
                }
                override fun onCancelled(e: DatabaseError) {}
            })

        view.findViewById<View>(R.id.filaCambiarNombre)
            .setOnClickListener { mostrarDialogoCambiarNombre(uid, tvNombre) }

        view.findViewById<View>(R.id.filaCambiarContrasena).setOnClickListener {
            auth.sendPasswordResetEmail(user?.email ?: "").addOnSuccessListener {
                Toast.makeText(requireContext(), "Correo enviado ✅", Toast.LENGTH_LONG).show()
            }
        }

        view.findViewById<View>(R.id.filaModificarRegistros).setOnClickListener {
            startActivity(Intent(requireActivity(), SetupCategoriasActivity::class.java))
        }

        view.findViewById<View>(R.id.filaContacto).setOnClickListener {
            val url = "https://wa.me/573246538855?text=${
                android.net.Uri.encode("Hola, necesito ayuda con FinanzasQG")}"
            startActivity(Intent(Intent.ACTION_VIEW, android.net.Uri.parse(url)))
        }

        view.findViewById<View>(R.id.filaTerminos).setOnClickListener {
            mostrarDialogoTerminos()
        }

        view.findViewById<Button>(R.id.btnCerrarSesionFinanzas).setOnClickListener {
            mostrarDialogoCerrarSesion()
        }

        return view
    }

    private fun mostrarDialogoTerminos() {
        val vista  = LayoutInflater.from(requireContext())
            .inflate(R.layout.dialog_terminos, null)
        val dialog = AlertDialog.Builder(requireContext(), R.style.DialogFinanzas)
            .setView(vista).create()
        dialog.window?.apply {
            setBackgroundDrawableResource(android.R.color.transparent)
            setLayout((resources.displayMetrics.widthPixels * 0.90).toInt(),
                ViewGroup.LayoutParams.WRAP_CONTENT)
        }
        vista.findViewById<Button>(R.id.btnAceptarTerminos)
            .setOnClickListener { dialog.dismiss() }
        dialog.show()
    }

    private fun mostrarDialogoCambiarNombre(uid: String, tvNombre: TextView) {
        val vista    = LayoutInflater.from(requireContext())
            .inflate(R.layout.dialog_cambiar_nombre, null)
        val etNombre = vista.findViewById<EditText>(R.id.etNuevoNombre)
        etNombre.setText(tvNombre.text); etNombre.setSelection(etNombre.text.length)

        val dialog = AlertDialog.Builder(requireContext(), R.style.DialogFinanzas)
            .setView(vista).create()
        dialog.window?.apply {
            setBackgroundDrawableResource(android.R.color.transparent)
            setLayout((resources.displayMetrics.widthPixels * 0.90).toInt(),
                ViewGroup.LayoutParams.WRAP_CONTENT)
        }
        vista.findViewById<Button>(R.id.btnGuardarNombre).setOnClickListener {
            val n = etNombre.text.toString().trim()
            if (n.isEmpty()) { etNombre.error = "Campo requerido"; return@setOnClickListener }
            db.child("users").child(uid).child("name").setValue(n).addOnSuccessListener {
                tvNombre.text = n
                Toast.makeText(requireContext(), "✅ Nombre actualizado", Toast.LENGTH_SHORT).show()
                dialog.dismiss()
            }
        }
        vista.findViewById<Button>(R.id.btnCancelarNombre)
            .setOnClickListener { dialog.dismiss() }
        dialog.show()
    }

    private fun mostrarDialogoCerrarSesion() {
        val vista  = LayoutInflater.from(requireContext())
            .inflate(R.layout.dialog_cerrar_sesion, null)
        val dialog = AlertDialog.Builder(requireContext(), R.style.DialogFinanzas)
            .setView(vista).create()
        dialog.window?.apply {
            setBackgroundDrawableResource(android.R.color.transparent)
            setLayout((resources.displayMetrics.widthPixels * 0.88).toInt(),
                ViewGroup.LayoutParams.WRAP_CONTENT)
        }
        vista.findViewById<Button>(R.id.btnConfirmarCerrar).setOnClickListener {
            requireActivity().getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .edit().clear().apply()
            auth.signOut()
            requireActivity().startActivity(
                Intent(requireActivity(), Login::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                })
            requireActivity().finish()
        }
        vista.findViewById<Button>(R.id.btnCancelarCerrar)
            .setOnClickListener { dialog.dismiss() }
        dialog.show()
    }
}