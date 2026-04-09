package com.geraldeberhard.libraryapp

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

class DatabaseHelper(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        private const val DATABASE_NAME = "library.db"
        private const val DATABASE_VERSION = 1

        // Table Names
        const val TABLE_BUKU = "buku"
        const val TABLE_ANGGOTA = "anggota"
        const val TABLE_PEMINJAMAN = "peminjaman"

        // Common Column
        const val KEY_ID = "id"

        // BUKU Table - column names
        const val KEY_BUKU_JUDUL = "judul"
        const val KEY_BUKU_PENGARANG = "pengarang"
        const val KEY_BUKU_ISBN = "isbn"
        const val KEY_BUKU_TAHUN = "tahun"
        const val KEY_BUKU_STOK = "stok"

        // ANGGOTA Table - column names
        const val KEY_ANGGOTA_NAMA = "nama"
        const val KEY_ANGGOTA_EMAIL = "email"
        const val KEY_ANGGOTA_NO_HP = "no_hp"
        const val KEY_ANGGOTA_TGL_DAFTAR = "tgl_daftar"

        // PEMINJAMAN Table - column names
        const val KEY_PEMINJAMAN_BUKU_ID = "buku_id"
        const val KEY_PEMINJAMAN_ANGGOTA_ID = "anggota_id"
        const val KEY_PEMINJAMAN_TGL_PINJAM = "tgl_pinjam"
        const val KEY_PEMINJAMAN_TGL_KEMBALI = "tgl_kembali"
        const val KEY_PEMINJAMAN_STATUS = "status"
    }

    override fun onCreate(db: SQLiteDatabase) {
        val CREATE_BUKU_TABLE = ("CREATE TABLE " + TABLE_BUKU + "("
                + KEY_ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                + KEY_BUKU_JUDUL + " TEXT NOT NULL,"
                + KEY_BUKU_PENGARANG + " TEXT NOT NULL,"
                + KEY_BUKU_ISBN + " TEXT UNIQUE,"
                + KEY_BUKU_TAHUN + " INTEGER,"
                + KEY_BUKU_STOK + " INTEGER DEFAULT 1" + ")")

        val CREATE_ANGGOTA_TABLE = ("CREATE TABLE " + TABLE_ANGGOTA + "("
                + KEY_ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                + KEY_ANGGOTA_NAMA + " TEXT NOT NULL,"
                + KEY_ANGGOTA_EMAIL + " TEXT UNIQUE NOT NULL,"
                + KEY_ANGGOTA_NO_HP + " TEXT,"
                + KEY_ANGGOTA_TGL_DAFTAR + " TEXT DEFAULT (date('now'))" + ")")

        val CREATE_PEMINJAMAN_TABLE = ("CREATE TABLE " + TABLE_PEMINJAMAN + "("
                + KEY_ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                + KEY_PEMINJAMAN_BUKU_ID + " INTEGER NOT NULL,"
                + KEY_PEMINJAMAN_ANGGOTA_ID + " INTEGER NOT NULL,"
                + KEY_PEMINJAMAN_TGL_PINJAM + " TEXT DEFAULT (date('now')),"
                + KEY_PEMINJAMAN_TGL_KEMBALI + " TEXT,"
                + KEY_PEMINJAMAN_STATUS + " TEXT DEFAULT 'dipinjam',"
                + "FOREIGN KEY(" + KEY_PEMINJAMAN_BUKU_ID + ") REFERENCES " + TABLE_BUKU + "(" + KEY_ID + "),"
                + "FOREIGN KEY(" + KEY_PEMINJAMAN_ANGGOTA_ID + ") REFERENCES " + TABLE_ANGGOTA + "(" + KEY_ID + ")" + ")")

        db.execSQL(CREATE_BUKU_TABLE)
        db.execSQL(CREATE_ANGGOTA_TABLE)
        db.execSQL(CREATE_PEMINJAMAN_TABLE)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS $TABLE_BUKU")
        db.execSQL("DROP TABLE IF EXISTS $TABLE_ANGGOTA")
        db.execSQL("DROP TABLE IF EXISTS $TABLE_PEMINJAMAN")
        onCreate(db)
    }

    // CRUD Buku
    fun addBuku(judul: String, pengarang: String, isbn: String, tahun: Int, stok: Int): Long {
        val db = this.writableDatabase
        val values = ContentValues()
        values.put(KEY_BUKU_JUDUL, judul)
        values.put(KEY_BUKU_PENGARANG, pengarang)
        values.put(KEY_BUKU_ISBN, isbn)
        values.put(KEY_BUKU_TAHUN, tahun)
        values.put(KEY_BUKU_STOK, stok)
        return db.insert(TABLE_BUKU, null, values)
    }

    fun getAllBuku(): Cursor {
        val db = this.readableDatabase
        return db.rawQuery("SELECT * FROM $TABLE_BUKU", null)
    }

    // CRUD Anggota
    fun addAnggota(nama: String, email: String, noHp: String): Long {
        val db = this.writableDatabase
        val values = ContentValues()
        values.put(KEY_ANGGOTA_NAMA, nama)
        values.put(KEY_ANGGOTA_EMAIL, email)
        values.put(KEY_ANGGOTA_NO_HP, noHp)
        return db.insert(TABLE_ANGGOTA, null, values)
    }

    fun getAllAnggota(): Cursor {
        val db = this.readableDatabase
        return db.rawQuery("SELECT * FROM $TABLE_ANGGOTA", null)
    }

    // Transaksi Peminjaman (Sesuai Study Case)
    fun prosesPeminjaman(bukuId: Int, anggotaId: Int): Boolean {
        val db = writableDatabase
        db.beginTransaction()
        return try {
            // Step 1: Insert peminjaman
            val values = ContentValues().apply {
                put(KEY_PEMINJAMAN_BUKU_ID, bukuId)
                put(KEY_PEMINJAMAN_ANGGOTA_ID, anggotaId)
            }
            val peminjamanId = db.insert(TABLE_PEMINJAMAN, null, values)
            if (peminjamanId == -1L) throw Exception("Gagal insert peminjaman")

            // Step 2: Kurangi stok buku
            db.execSQL(
                "UPDATE $TABLE_BUKU SET $KEY_BUKU_STOK = $KEY_BUKU_STOK - 1 WHERE $KEY_ID = ? AND $KEY_BUKU_STOK > 0",
                arrayOf(bukuId.toString())
            )

            db.setTransactionSuccessful()
            true
        } catch (e: Exception) {
            false
        } finally {
            db.endTransaction()
        }
    }

    // Pengembalian
    fun prosesPengembalian(peminjamanId: Int, bukuId: Int): Boolean {
        val db = writableDatabase
        db.beginTransaction()
        return try {
            // Update status peminjaman
            val values = ContentValues().apply {
                put(KEY_PEMINJAMAN_STATUS, "kembali")
                put(KEY_PEMINJAMAN_TGL_KEMBALI, "date('now')") // Simplification, usually use a date formatter
            }
            db.update(TABLE_PEMINJAMAN, values, "$KEY_ID = ?", arrayOf(peminjamanId.toString()))

            // Tambah stok buku
            db.execSQL(
                "UPDATE $TABLE_BUKU SET $KEY_BUKU_STOK = $KEY_BUKU_STOK + 1 WHERE $KEY_ID = ?",
                arrayOf(bukuId.toString())
            )

            db.setTransactionSuccessful()
            true
        } catch (e: Exception) {
            false
        } finally {
            db.endTransaction()
        }
    }

    // Query 1: Daftar peminjaman aktif (JOIN)
    fun getPeminjamanAktif(): Cursor {
        val db = readableDatabase
        val queryAktif = """
            SELECT p.$KEY_ID, b.$KEY_BUKU_JUDUL, b.$KEY_BUKU_PENGARANG, a.$KEY_ANGGOTA_NAMA, a.$KEY_ANGGOTA_EMAIL,
                   p.$KEY_PEMINJAMAN_TGL_PINJAM,
                   julianday('now') - julianday(p.$KEY_PEMINJAMAN_TGL_PINJAM) AS hari_pinjam
            FROM $TABLE_PEMINJAMAN p
            INNER JOIN $TABLE_BUKU b ON p.$KEY_PEMINJAMAN_BUKU_ID = b.$KEY_ID
            INNER JOIN $TABLE_ANGGOTA a ON p.$KEY_PEMINJAMAN_ANGGOTA_ID = a.$KEY_ID
            WHERE p.$KEY_PEMINJAMAN_STATUS = 'dipinjam'
            ORDER BY p.$KEY_PEMINJAMAN_TGL_PINJAM ASC
        """.trimIndent()
        return db.rawQuery(queryAktif, null)
    }

    // Query 2: Statistik dashboard
    fun getDashboardStats(): Cursor {
        val db = readableDatabase
        val queryStats = """
            SELECT
                (SELECT COUNT(*) FROM $TABLE_BUKU) AS total_buku,
                (SELECT COUNT(*) FROM $TABLE_ANGGOTA) AS total_anggota,
                (SELECT COUNT(*) FROM $TABLE_PEMINJAMAN WHERE $KEY_PEMINJAMAN_STATUS='dipinjam') AS sedang_pinjam,
                (SELECT SUM($KEY_BUKU_STOK) FROM $TABLE_BUKU) AS total_stok
        """.trimIndent()
        return db.rawQuery(queryStats, null)
    }

    // Query 3: Buku terpopuler
    fun getBukuTerpopuler(): Cursor {
        val db = readableDatabase
        val queryPopuler = """
            SELECT b.$KEY_BUKU_JUDUL, b.$KEY_BUKU_PENGARANG, COUNT(p.$KEY_ID) AS frekuensi
            FROM $TABLE_BUKU b
            LEFT JOIN $TABLE_PEMINJAMAN p ON b.$KEY_ID = p.$KEY_PEMINJAMAN_BUKU_ID
            GROUP BY b.$KEY_ID
            ORDER BY frekuensi DESC
            LIMIT 5
        """.trimIndent()
        return db.rawQuery(queryPopuler, null)
    }
}
