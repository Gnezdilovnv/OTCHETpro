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

    // Экранирование спецсимволов
    private fun escapeXml(text: String): String {
        return text
            .replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("'", "&apos;")
    }

    // Форматирование текста с поддержкой простых тегов
    private fun applyFormatting(paragraph: XWPFParagraph, text: String) {
        // Разбиваем текст на части по тегам
        val parts = text.split(Regex("(<b>|</b>|<i>|</i>|<u>|</u>)"))
        var isBold = false
        var isItalic = false
        var isUnderline = false
        
        // Обрабатываем каждую часть
        var currentIndex = 0
        while (currentIndex < text.length) {
            when {
                text.startsWith("<b>", currentIndex) -> { isBold = true; currentIndex += 3 }
                text.startsWith("</b>", currentIndex) -> { isBold = false; currentIndex += 4 }
                text.startsWith("<i>", currentIndex) -> { isItalic = true; currentIndex += 3 }
                text.startsWith("</i>", currentIndex) -> { isItalic = false; currentIndex += 4 }
                text.startsWith("<u>", currentIndex) -> { isUnderline = true; currentIndex += 3 }
                text.startsWith("</u>", currentIndex) -> { isUnderline = false; currentIndex += 4 }
                else -> {
                    // Находим следующий тег
                    val nextTag = text.indexOf('<', currentIndex)
                    val end = if (nextTag == -1) text.length else nextTag
                    val chunk = text.substring(currentIndex, end)
                    
                    if (chunk.isNotEmpty()) {
                        val run = paragraph.createRun()
                        run.setText(escapeXml(chunk))
                        run.isBold = isBold
                        run.isItalic = isItalic
                        run.isUnderline = isUnderline
                        run.fontSize = 12
                        run.fontFamily = "Times New Roman"
                    }
                    currentIndex = end
                }
            }
        }
    }

    fun generateReport(context: Context, text: String, fileName: String): File? {
        return try {
            val doc = XWPFDocument()
            
            // Добавляем заголовок
            val titlePara = doc.createParagraph()
            titlePara.alignment = ParagraphAlignment.CENTER
            val titleRun = titlePara.createRun()
            titleRun.setText("БОЕВОЕ ДОНЕСЕНИЕ")
            titleRun.isBold = true
            titleRun.fontSize = 16
            titleRun.fontFamily = "Times New Roman"
            
            // Пустая строка
            val emptyPara = doc.createParagraph()
            emptyPara.createRun().text = ""
            
            // Основной текст с форматированием
            val para = doc.createParagraph()
            para.alignment = ParagraphAlignment.LEFT
            applyFormatting(para, text)

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

    // Создание таблицы в DOCX
    fun createTable(doc: XWPFDocument, headers: List<String>, rows: List<List<String>>): XWPFTable {
        val table = doc.createTable(rows.size + 1, headers.size)
        
        // Заголовки
        val headerRow = table.getRow(0)
        headers.forEachIndexed { i, header ->
            val cell = headerRow.getCell(i)
            val p = cell.paragraphs[0]
            val r = p.createRun()
            r.setText(header)
            r.isBold = true
        }
        
        // Данные
        rows.forEachIndexed { rowIndex, row ->
            val tableRow = table.getRow(rowIndex + 1)
            row.forEachIndexed { colIndex, value ->
                val cell = tableRow.getCell(colIndex)
                val p = cell.paragraphs[0]
                val r = p.createRun()
                r.setText(value)
            }
        }
        
        return table
    }
