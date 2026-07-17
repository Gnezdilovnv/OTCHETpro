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

            val dir = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS), "OTCHETpro")
            if (!dir.exists()) dir.mkdirs()

            val f = File(dir, fileName)
            FileOutputStream(f).use { doc.write(it) }
            doc.close()
            f
        } catch (e: Exception) { null }
    }

    fun getDir() = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS), "OTCHETpro")
}
