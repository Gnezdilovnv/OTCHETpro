package com.otchetpro.app.activities

import android.app.AlertDialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import com.otchetpro.app.R
import com.otchetpro.app.data.*
import com.otchetpro.app.utils.*
import kotlinx.coroutines.*
import java.io.File

class ReportDetailActivity : AppCompatActivity() {

    private lateinit var tvTitle: TextView
    private lateinit var tvStatus: TextView
    private lateinit var tvContent: TextView
    private lateinit var btnSend: Button
    private lateinit var btnShare: Button
    private lateinit var btnClose: Button
    private lateinit var tvEmailStatus: TextView
    private lateinit var tvReportDate: TextView
    private var id: Long = 0
    private var report: Report? = null
    private var filePath: String? = null

    override fun onCreate(s: Bundle?) {
        super.onCreate(s)
        setContentView(R.layout.activity_report_detail)

        tvTitle = findViewById(R.id.tv_title_report)
        tvStatus = findViewById(R.id.tv_status)
        tvContent = findViewById(R.id.tv_content)
        btnSend = findViewById(R.id.btn_send_email)
        btnShare = findViewById(R.id.btn_share)
        btnClose = findViewById(R.id.btn_close_report)
        tvEmailStatus = findViewById(R.id.tv_email_status)
        tvReportDate = findViewById(R.id.tv_report_date)
        id = intent.getLongExtra("id", 0)

        btnClose.setOnClickListener { finish() }
        btnSend.setOnClickListener { sendEmail() }
        btnShare.setOnClickListener {
            val i = Intent(Intent.ACTION_SEND)
            i.type = "text/plain"
            i.putExtra(Intent.EXTRA_TEXT, report?.text)
            startActivity(Intent.createChooser(i, "Поделиться"))
        }
        findViewById<Button>(R.id.btn_open_file).setOnClickListener { openFile() }

        load()
    }

    private fun load() {
        CoroutineScope(Dispatchers.IO).launch {
            report = AppDatabase.getInstance(this@ReportDetailActivity).reportDao().getById(id)
            withContext(Dispatchers.Main) {
                report?.let { r ->
                    tvTitle.text = r.templateName
                    tvContent.text = r.text
                    tvStatus.text = if (r.status == "sent") "✅ Отправлен" else "💾 Сохранен"
                    tvStatus.setBackgroundColor(if (r.status == "sent") 0xFFDDF0E6.toInt() else 0xFFEEF4FC.toInt())
                    tvStatus.setTextColor(if (r.status == "sent") 0xFF0F6B3A.toInt() else 0xFF1A4CBA.toInt())
                    if (r.status == "sent") tvEmailStatus.visibility = View.VISIBLE
                    else tvEmailStatus.visibility = View.GONE
                    tvReportDate.text = "Создан: ${java.text.SimpleDateFormat("dd.MM.yyyy HH:mm").format(r.createdAt)}"
                    
                    findFile(r.id)
                }
            }
        }
    }

    private fun findFile(reportId: Long) {
        try {
            val reportsDir = DocxGenerator.getReportsDir()
            if (reportsDir.exists()) {
                val files = reportsDir.listFiles { file -> 
                    file.isFile && file.name.contains("Отчет_$reportId") && file.extension == "docx"
                }
                if (!files.isNullOrEmpty()) {
                    filePath = files[0].absolutePath
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun openFile() {
        if (filePath == null) {
            findFile(id)
        }
        
        if (filePath == null) {
            AlertDialog.Builder(this)
                .setTitle("Файл не найден")
                .setMessage("Файл отчета не найден. Возможно, он был удален. Попробуйте пересохранить отчет.")
                .setPositiveButton("OK", null)
                .show()
            return
        }
        
        try {
            val file = File(filePath)
            if (!file.exists()) {
                AlertDialog.Builder(this)
                    .setTitle("Файл не найден")
                    .setMessage("Файл отчета не существует. Попробуйте пересохранить отчет.")
                    .setPositiveButton("OK", null)
                    .show()
                return
            }
            
            val uri = FileProvider.getUriForFile(
                this,
                "${packageName}.fileprovider",
                file
            )
            
            val intent = Intent(Intent.ACTION_VIEW)
            intent.setDataAndType(uri, "application/msword")
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            startActivity(Intent.createChooser(intent, "Открыть файл"))
        } catch (e: Exception) {
            Toast.makeText(this, "Ошибка открытия: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    // ============================================================
    // ОТПРАВКА НА ПОЧТУ С ПРИКРЕПЛЕНИЕМ ФАЙЛА
    // ============================================================
    private fun sendEmail() {
        // Проверяем, что отчет сохранен
        if (report == null) {
            Toast.makeText(this, "Отчет не найден", Toast.LENGTH_SHORT).show()
            return
        }

        val recips = SharedPrefs.getRecipients(this)
        
        if (recips.isEmpty()) {
            showManualEmailDialog()
            return
        }
        
        val names = recips.map { it.name }.toTypedArray()
        AlertDialog.Builder(this)
            .setTitle("Выберите получателя")
            .setItems(names) { _, i ->
                val r = recips[i]
                sendEmailWithAttachment(r.email, r.name)
            }
            .setPositiveButton("Ввести вручную") { _, _ -> showManualEmailDialog() }
            .setNegativeButton("Отмена", null)
            .show()
    }

    private fun showManualEmailDialog() {
        val nameInput = EditText(this).apply { hint = "ФИО получателя" }
        val emailInput = EditText(this).apply { hint = "Email получателя" }
        
        val container = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(16, 16, 16, 16)
            addView(nameInput)
            addView(emailInput)
        }
        
        AlertDialog.Builder(this)
            .setTitle("Введите получателя")
            .setView(container)
            .setPositiveButton("Отправить") { _, _ ->
                val name = nameInput.text.toString().trim()
                val email = emailInput.text.toString().trim()
                if (name.isNotEmpty() && email.isNotEmpty() && email.contains("@")) {
                    sendEmailWithAttachment(email, name)
                } else {
                    Toast.makeText(this, "Введите корректные данные", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Отмена", null)
            .show()
    }

    private fun sendEmailWithAttachment(email: String, name: String) {
        val r = report ?: return
        
        // Ищем файл
        if (filePath == null) {
            findFile(r.id)
        }
        
        val subject = "Боевое донесение — ${r.templateName}"
        val body = """
            Уважаемый(ая) $name!

            Направляю боевое донесение в прикреплённом файле.

            -- 
            Сгенерировано автоматически в OTCHETpro
        """.trimIndent()

        val intent = Intent(Intent.ACTION_SEND)
        intent.type = "application/msword"
        intent.putExtra(Intent.EXTRA_SUBJECT, subject)
        intent.putExtra(Intent.EXTRA_TEXT, body)
        
        // Прикрепляем файл, если он есть
        if (filePath != null) {
            val file = File(filePath)
            if (file.exists()) {
                val uri = FileProvider.getUriForFile(
                    this,
                    "${packageName}.fileprovider",
                    file
                )
                intent.putExtra(Intent.EXTRA_STREAM, uri)
                intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            } else {
                // Файл не найден, отправляем только текст
                intent.type = "text/plain"
                intent.putExtra(Intent.EXTRA_TEXT, body + "\n\n" + r.text)
            }
        } else {
            // Файл не найден, отправляем только текст
            intent.type = "text/plain"
            intent.putExtra(Intent.EXTRA_TEXT, body + "\n\n" + r.text)
        }
        
        startActivity(Intent.createChooser(intent, "Отправить письмо"))

        // Обновляем статус в БД
        CoroutineScope(Dispatchers.IO).launch {
            val db = AppDatabase.getInstance(this@ReportDetailActivity)
            val current = db.reportDao().getById(id)
            if (current != null) {
                val updated = current.copy(status = "sent")
                db.reportDao().update(updated)
            }
        }

        tvStatus.text = "✅ Отправлен"
        tvStatus.setBackgroundColor(0xFFDDF0E6.toInt())
        tvStatus.setTextColor(0xFF0F6B3A.toInt())
        tvEmailStatus.visibility = View.VISIBLE
        Toast.makeText(this, "Письмо открыто", Toast.LENGTH_SHORT).show()
    }
}
