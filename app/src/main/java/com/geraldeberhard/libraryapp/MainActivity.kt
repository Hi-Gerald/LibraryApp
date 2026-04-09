package com.geraldeberhard.libraryapp

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class MainActivity : AppCompatActivity() {

    private lateinit var dbHelper: DatabaseHelper
    private lateinit var tvTotalBuku: TextView
    private lateinit var tvTotalAnggota: TextView
    private lateinit var tvSedangDipinjam: TextView
    private lateinit var tvLog: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        dbHelper = DatabaseHelper(this)

        tvTotalBuku = findViewById(R.id.tvTotalBuku)
        tvTotalAnggota = findViewById(R.id.tvTotalAnggota)
        tvSedangDipinjam = findViewById(R.id.tvSedangDipinjam)
        tvLog = findViewById(R.id.tvLog)

        val btnRefresh = findViewById<Button>(R.id.btnRefresh)
        btnRefresh.setOnClickListener {
            refreshDashboard()
        }

        val btnAddData = findViewById<Button>(R.id.btnAddData)
        btnAddData.setOnClickListener {
            val intent = Intent(this, InputActivity::class.java)
            startActivity(intent)
        }

        // Jalankan demo data jika database masih kosong
        initDemoData()
        refreshDashboard()
    }

    private fun initDemoData() {
        val cursorBuku = dbHelper.getAllBuku()
        if (cursorBuku.count == 0) {
            logStatus("Inisialisasi data dummy...")
            
            // Tambah Buku
            val bukuId = dbHelper.addBuku("Android Programming", "John Doe", "12345", 2023, 5)
            dbHelper.addBuku("Kotlin for Beginners", "Jane Smith", "67890", 2022, 3)
            
            // Tambah Anggota
            val anggotaId = dbHelper.addAnggota("Budi", "budi@mail.com", "08123456789")
            dbHelper.addAnggota("Siti", "siti@mail.com", "08987654321")

            logStatus("Data dummy berhasil ditambahkan.")

            // Simulasi Proses Peminjaman (Transaksi sesuai Study Case)
            if (bukuId != -1L && anggotaId != -1L) {
                val success = dbHelper.prosesPeminjaman(bukuId.toInt(), anggotaId.toInt())
                if (success) {
                    logStatus("Transaksi Peminjaman Berhasil: Buku ID $bukuId oleh Anggota ID $anggotaId")
                } else {
                    logStatus("Transaksi Peminjaman Gagal!")
                }
            }
        }
        cursorBuku.close()
    }

    private fun refreshDashboard() {
        val cursor = dbHelper.getDashboardStats()
        if (cursor.moveToFirst()) {
            val totalBuku = cursor.getInt(cursor.getColumnIndexOrThrow("total_buku"))
            val totalAnggota = cursor.getInt(cursor.getColumnIndexOrThrow("total_anggota"))
            val sedangPinjam = cursor.getInt(cursor.getColumnIndexOrThrow("sedang_pinjam"))

            tvTotalBuku.text = getString(R.string.total_buku, totalBuku)
            tvTotalAnggota.text = getString(R.string.total_anggota, totalAnggota)
            tvSedangDipinjam.text = getString(R.string.sedang_dipinjam, sedangPinjam)
            
            logStatus("Dashboard diupdate.")
        }
        cursor.close()

        // Log Peminjaman Aktif (JOIN Query dari Study Case)
        val cursorAktif = dbHelper.getPeminjamanAktif()
        if (cursorAktif.moveToFirst()) {
            logStatus("\n--- Daftar Peminjaman Aktif (JOIN) ---")
            do {
                val judul = cursorAktif.getString(cursorAktif.getColumnIndexOrThrow(DatabaseHelper.KEY_BUKU_JUDUL))
                val nama = cursorAktif.getString(cursorAktif.getColumnIndexOrThrow(DatabaseHelper.KEY_ANGGOTA_NAMA))
                val hari = cursorAktif.getInt(cursorAktif.getColumnIndexOrThrow("hari_pinjam"))
                logStatus("- $judul dipinjam oleh $nama ($hari hari)")
            } while (cursorAktif.moveToNext())
        } else {
            logStatus("\nTidak ada peminjaman aktif.")
        }
        cursorAktif.close()
        
        // Log Buku Terpopuler
        val cursorPopuler = dbHelper.getBukuTerpopuler()
        if (cursorPopuler.moveToFirst()) {
            logStatus("\n--- 5 Buku Terpopuler ---")
            do {
                val judul = cursorPopuler.getString(cursorPopuler.getColumnIndexOrThrow(DatabaseHelper.KEY_BUKU_JUDUL))
                val freq = cursorPopuler.getInt(cursorPopuler.getColumnIndexOrThrow("frekuensi"))
                logStatus("- $judul ($freq kali dipinjam)")
            } while (cursorPopuler.moveToNext())
        }
        cursorPopuler.close()
    }

    private fun logStatus(message: String) {
        val currentLog = tvLog.text.toString()
        tvLog.text = "$message\n$currentLog"
    }
}