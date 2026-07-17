package com.otchetpro.app.utils

import android.content.Context
import android.os.Environment
import org.apache.poi.xwpf.usermodel.*
import java.io.*

object DocxGenerator {
    fun generate(context: Context, text: String, fileName: String): File? {
        return try {
            val doc = XWPFDocument()
            val p = doc.createParagraph()
            p.alignment = ParagraphAlignment.LEFT
            val r = p.createRun()
            r.setText(text)
            r.fontSize = 12
            r.fontFamily = "Times New Roman"

            // Ищем папку "Отчеты" в Documents
            val docsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS)
            val reportsDir = File(docsDir, "OTCHETpro")
            
            // Если папки нет — создаём
            if (!reportsDir.exists()) {
                reportsDir.mkdirs()
            }

            val f = File(reportsDir, fileName)
            FileOutputStream(f).use { doc.write(it) }
            doc.close()
            f
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun getReportsDir(): File {
        val docsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS)
        val reportsDir = File(docsDir, "OTCHETpro")
        if (!reportsDir.exists()) {
            reportsDir.mkdirs()
        }
        return reportsDir
    }
}
