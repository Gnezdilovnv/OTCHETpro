package com.otchetpro.app.activities

import android.app.AlertDialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
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
        btnSend.setOnClickListener { send() }
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
                }
            }
        }
    }

    private fun openFile() {
        val dir = DocxGenerator.getDir()
        val files = dir.listFiles()
        var foundFile: File? = null
        files?.forEach { 
            if (it.name.contains(id.toString())) foundFile = it
        }
        val found = foundFile
        if (found != null && found.exists()) {
            val uri = Uri.fromFile(found)
            val i = Intent(Intent.ACTION_VIEW)
            i.setDataAndType(uri, "application/msword")
            i.flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
            startActivity(Intent.createChooser(i, "Открыть файл"))
        } else {
            Toast.makeText(this, "Файл не найден", Toast.LENGTH_SHORT).show()
        }
    }

    private fun send() {
        val recips = SharedPrefs.getRecipients(this)
        if (recips.isEmpty()) { 
            Toast.makeText(this, "Нет получателей", Toast.LENGTH_SHORT).show()
            return 
        }
        val names = recips.map { it.name }.toTypedArray()
        AlertDialog.Builder(this)
            .setTitle("Выберите получателя")
            .setItems(names) { _, i ->
                val r = recips[i]
                val intent = Intent(Intent.ACTION_SENDTO)
                intent.data = Uri.parse("mailto:${r.email}")
                intent.putExtra(Intent.EXTRA_SUBJECT, "Боевое донесение — ${report?.templateName}")
                intent.putExtra(Intent.EXTRA_TEXT, report?.text)
                startActivity(Intent.createChooser(intent, "Отправить"))

                CoroutineScope(Dispatchers.IO).launch {
                    report?.let { 
                        val updated = it.copy(status = "sent")
                        AppDatabase.getInstance(this@ReportDetailActivity).reportDao().update(updated)
                    }
                }
                tvStatus.text = "✅ Отправлен"
                tvStatus.setBackgroundColor(0xFFDDF0E6.toInt())
                tvStatus.setTextColor(0xFF0F6B3A.toInt())
                tvEmailStatus.visibility = View.VISIBLE
                Toast.makeText(this, "Письмо открыто", Toast.LENGTH_SHORT).show()
            }
            .show()
    }
}
