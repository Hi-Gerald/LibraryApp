package com.geraldeberhard.libraryapp

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class InputActivity : AppCompatActivity() {

    private lateinit var dbHelper: DatabaseHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_input)

        dbHelper = DatabaseHelper(this)

        // Form Buku
        val etJudul = findViewById<EditText>(R.id.etJudul)
        val etPengarang = findViewById<EditText>(R.id.etPengarang)
        val etIsbn = findViewById<EditText>(R.id.etIsbn)
        val etTahun = findViewById<EditText>(R.id.etTahun)
        val etStok = findViewById<EditText>(R.id.etStok)
        val btnSimpanBuku = findViewById<Button>(R.id.btnSimpanBuku)

        btnSimpanBuku.setOnClickListener {
            val judul = etJudul.text.toString()
            val pengarang = etPengarang.text.toString()
            val isbn = etIsbn.text.toString()
            val tahun = etTahun.text.toString().toIntOrNull() ?: 0
            val stok = etStok.text.toString().toIntOrNull() ?: 0

            if (judul.isNotEmpty() && pengarang.isNotEmpty()) {
                val id = dbHelper.addBuku(judul, pengarang, isbn, tahun, stok)
                if (id != -1L) {
                    Toast.makeText(this, "Buku berhasil disimpan!", Toast.LENGTH_SHORT).show()
                    clearBukuFields(etJudul, etPengarang, etIsbn, etTahun, etStok)
                } else {
                    Toast.makeText(this, "Gagal menyimpan buku (ISBN mungkin duplikat)", Toast.LENGTH_SHORT).show()
                }
            }
        }

        // Form Anggota
        val etNama = findViewById<EditText>(R.id.etNama)
        val etEmail = findViewById<EditText>(R.id.etEmail)
        val etNoHp = findViewById<EditText>(R.id.etNoHp)
        val btnSimpanAnggota = findViewById<Button>(R.id.btnSimpanAnggota)

        btnSimpanAnggota.setOnClickListener {
            val nama = etNama.text.toString()
            val email = etEmail.text.toString()
            val noHp = etNoHp.text.toString()

            if (nama.isNotEmpty() && email.isNotEmpty()) {
                val id = dbHelper.addAnggota(nama, email, noHp)
                if (id != -1L) {
                    Toast.makeText(this, "Anggota berhasil didaftarkan!", Toast.LENGTH_SHORT).show()
                    etNama.text.clear()
                    etEmail.text.clear()
                    etNoHp.text.clear()
                } else {
                    Toast.makeText(this, "Gagal registrasi (Email mungkin sudah ada)", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun clearBukuFields(vararg editTexts: EditText) {
        for (et in editTexts) {
            et.text.clear()
        }
    }
}
