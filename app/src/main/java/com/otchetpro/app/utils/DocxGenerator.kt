package com.otchetpro.app.utils

import android.content.Context
import android.os.Environment
import org.apache.poi.xwpf.usermodel.*
import java.io.*

object DocxGenerator {

    private fun getRootDir(): File {
        val docsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS)
        val root = File(docsDir, "OTCHETpro")
        if (!root.exists()) root.mkdirs()
        return root
    }

    fun getReportsDir(): File {
        val dir = File(getRootDir(), "Отчеты")
        if (!dir.exists()) dir.mkdirs()
        return dir
    }

    fun getSettingsDir(): File {
        val dir = File(getRootDir(), "Настройки")
        if (!dir.exists()) dir.mkdirs()
        return dir
    }

    fun getExportDir(): File {
        val dir = File(getRootDir(), "Экспорт")
        if (!dir.exists()) dir.mkdirs()
        return dir
    }

    fun generateReport(context: Context, text: String, fileName: String): File? {
        return try {
            val doc = XWPFDocument()
            val p = doc.createParagraph()
            p.alignment = ParagraphAlignment.LEFT
            val r = p.createRun()
            r.setText(text)
            r.fontSize = 12
            r.fontFamily = "Times New Roman"

            val dir = getReportsDir()
            val f = File(dir, fileName)
            FileOutputStream(f).use { doc.write(it) }
            doc.close()
            f
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
