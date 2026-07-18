package com.otchetpro.app.activities

import android.app.AlertDialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.otchetpro.app.R
import com.otchetpro.app.data.*
import com.otchetpro.app.utils.DocxGenerator
import com.otchetpro.app.utils.SharedPrefs
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

class SettingsActivity : AppCompatActivity() {

    private lateinit var tvDeptInfo: TextView
    private lateinit var tvSettingsInfo: TextView
    private lateinit var tvSettingsTag: TextView
    private lateinit var tvImportResult: TextView
    private lateinit var tvExportResult: TextView
    private lateinit var llSubDepts: LinearLayout
    private lateinit var llTemplates: LinearLayout
    private lateinit var llVars: LinearLayout
    private lateinit var llRecipients: LinearLayout
    private lateinit var llDepts: LinearLayout
    private lateinit var etSubDeptNew: EditText
    private lateinit var etRecipientName: EditText
    private lateinit var etRecipientEmail: EditText
    private lateinit var btnImport: Button
    private lateinit var btnExport: Button
    private lateinit var btnTemplateDownload: Button
    private lateinit var btnTemplateAdd: Button
    private lateinit var btnVarAdd: Button

    private var dept = ""
    private var allDepts = mutableListOf<String>()
    private var subDepts = mutableListOf<String>()
    private var templates = mutableListOf<Template>()
    private var variables = mutableListOf<Variable>()
    private var recipients = mutableListOf<Recipient>()

    private val filePickerLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) result.data?.data?.let { importFromUri(it) }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        tvDeptInfo = findViewById(R.id.tv_dept_info_settings)
        tvSettingsInfo = findViewById(R.id.tv_settings_info)
        tvSettingsTag = findViewById(R.id.tv_settings_tag)
        tvImportResult = findViewById(R.id.tv_import_result)
        tvExportResult = findViewById(R.id.tv_export_result)
        llSubDepts = findViewById(R.id.ll_subdepts_list)
        llTemplates = findViewById(R.id.ll_templates_list)
        llVars = findViewById(R.id.ll_vars_list)
        llRecipients = findViewById(R.id.ll_recipients_list)
        llDepts = findViewById(R.id.ll_depts_list)
        etSubDeptNew = findViewById(R.id.et_subdept_new)
        etRecipientName = findViewById(R.id.et_recipient_name)
        etRecipientEmail = findViewById(R.id.et_recipient_email)
        btnImport = findViewById(R.id.btn_import)
        btnExport = findViewById(R.id.btn_export)
        btnTemplateDownload = findViewById(R.id.btn_template_download)
        btnTemplateAdd = findViewById(R.id.btn_template_add)
        btnVarAdd = findViewById(R.id.btn_var_add)

        dept = SharedPrefs.getDept(this)
        loadData()

        findViewById<Button>(R.id.btn_close_settings).setOnClickListener { finish() }
        findViewById<Button>(R.id.btn_subdept_add).setOnClickListener { addSubDept() }
        findViewById<Button>(R.id.btn_recipient_add).setOnClickListener { addRecipient() }
        btnTemplateAdd.setOnClickListener { startActivity(Intent(this, TemplateEditorActivity::class.java)) }
        btnVarAdd.setOnClickListener { showAddVariableDialog() }
        btnExport.setOnClickListener { exportSettings() }
        btnTemplateDownload.setOnClickListener { downloadTemplate() }
        btnImport.setOnClickListener { openFilePicker() }

        setupAccordion(R.id.accordion_dept_header, R.id.accordion_dept_body)
        setupAccordion(R.id.accordion_subdepts_header, R.id.accordion_subdepts_body)
        setupAccordion(R.id.accordion_templates_header, R.id.accordion_templates_body)
        setupAccordion(R.id.accordion_vars_header, R.id.accordion_vars_body)
        setupAccordion(R.id.accordion_recipients_header, R.id.accordion_recipients_body)
        setupAccordion(R.id.accordion_export_header, R.id.accordion_export_body)
    }

    override fun onResume() { super.onResume(); loadData() }

    private fun setupAccordion(headerId: Int, bodyId: Int) {
        val h = findViewById<TextView>(headerId)
        val b = findViewById<LinearLayout>(bodyId)
        h.setOnClickListener {
            if (b.visibility == View.VISIBLE) { b.visibility = View.GONE; h.text = h.text.toString().replace("▼", "▶") }
            else { b.visibility = View.VISIBLE; h.text = h.text.toString().replace("▶", "▼") }
        }
    }

    private fun loadData() {
        dept = SharedPrefs.getDept(this)
        allDepts = SharedPrefs.getDepts(this).toMutableList()
        subDepts = SharedPrefs.getSubDepts(this).toMutableList()
        templates = SharedPrefs.getTemplates(this).toMutableList()
        variables = SharedPrefs.getVariables(this).toMutableList()
        recipients = SharedPrefs.getRecipients(this).toMutableList()
        tvDeptInfo.text = "Настройки для подразделения: $dept"
        val unit = SharedPrefs.getDeptUnit(this, dept) ?: ""
        tvSettingsTag.text = if (unit.isNotEmpty()) "$dept — $unit" else dept
        tvSettingsInfo.text = "${allDepts.size} подр. · ${subDepts.size} соисп. · ${templates.size} шабл. · ${variables.size} перем."
        renderDepts(); renderSubDepts(); renderTemplates(); renderVariables(); renderRecipients()
    }

    private fun renderDepts() {
        llDepts.removeAllViews()
        allDepts.forEachIndexed { i, name ->
            val row = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; setPadding(0, 4, 0, 4) }
            val tv = TextView(this).apply {
                text = name
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                textSize = 14f
            }
            // Кнопка расчётов
            val unitBtn = Button(this).apply {
                text = "Р"
                setOnClickListener {
                    val intent = Intent(this@SettingsActivity, UnitEditorActivity::class.java)
                    intent.putExtra("dept_name", name)
                    startActivity(intent)
                }
            }
            val editBtn = Button(this).apply {
                text = "✎"
                setOnClickListener {
                    val input = EditText(this@SettingsActivity).apply { setText(name) }
                    AlertDialog.Builder(this@SettingsActivity).setTitle("Редактировать подразделение").setView(input)
                        .setPositiveButton("Сохранить") { d, w ->
                            val nn = input.text.toString().trim()
                            if (nn.isNotEmpty() && nn != name) {
                                if (allDepts.contains(nn)) { Toast.makeText(this@SettingsActivity, "❌ Уже существует", Toast.LENGTH_SHORT).show(); return@setPositiveButton }
                                val old = allDepts[i]; allDepts[i] = nn
                                SharedPrefs.saveDepts(this@SettingsActivity, allDepts)
                                if (dept == old) { SharedPrefs.saveDept(this@SettingsActivity, nn); dept = nn }
                                val vl = SharedPrefs.getVariables(this@SettingsActivity).toMutableList()
                                vl.forEachIndexed { vi, vv -> if (vv.dept == old) vl[vi] = vv.copy(dept = nn) }
                                SharedPrefs.saveVariables(this@SettingsActivity, vl)
                                val tl = SharedPrefs.getTemplates(this@SettingsActivity).toMutableList()
                                tl.forEachIndexed { ti, tt -> if (tt.dept == old) tl[ti] = tt.copy(dept = nn) }
                                SharedPrefs.saveTemplates(this@SettingsActivity, tl)
                                loadData()
                                Toast.makeText(this@SettingsActivity, "✅ Обновлено", Toast.LENGTH_SHORT).show()
                            }
                        }.setNegativeButton("Отмена", null).show()
                }
            }
            val delBtn = Button(this).apply {
                text = "✕"
                setOnClickListener {
                    if (allDepts.size <= 1) { Toast.makeText(this@SettingsActivity, "Нельзя удалить последнее", Toast.LENGTH_SHORT).show(); return@setOnClickListener }
                    showDeleteConfirmDialog("Удалить $name?", "Все данные подразделения будут удалены") {
                        allDepts.removeAt(i); SharedPrefs.saveDepts(this@SettingsActivity, allDepts)
                        val vl = SharedPrefs.getVariables(this@SettingsActivity).toMutableList(); vl.removeAll { it.dept == name }; SharedPrefs.saveVariables(this@SettingsActivity, vl)
                        val tl = SharedPrefs.getTemplates(this@SettingsActivity).toMutableList(); tl.removeAll { it.dept == name }; SharedPrefs.saveTemplates(this@SettingsActivity, tl)
                        if (dept == name) { SharedPrefs.saveDept(this@SettingsActivity, allDepts[0]); dept = allDepts[0] }
                        loadData()
                    }
                }
            }
            row.addView(tv); row.addView(unitBtn); row.addView(editBtn); row.addView(delBtn); llDepts.addView(row)
        }
        val addRow = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; setPadding(0, 8, 0, 0) }
        val addInput = EditText(this).apply { hint = "Новое подразделение"; layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f) }
        val addBtn = Button(this).apply {
            text = "➕"
            setOnClickListener {
                val nn = addInput.text.toString().trim()
                if (nn.isEmpty()) { Toast.makeText(this@SettingsActivity, "Введите название", Toast.LENGTH_SHORT).show(); return@setOnClickListener }
                if (allDepts.contains(nn)) { Toast.makeText(this@SettingsActivity, "❌ Уже существует", Toast.LENGTH_SHORT).show(); return@setOnClickListener }
                allDepts.add(nn); SharedPrefs.saveDepts(this@SettingsActivity, allDepts); addInput.text.clear(); loadData()
            }
        }
        addRow.addView(addInput); addRow.addView(addBtn); llDepts.addView(addRow)
    }

    private fun renderSubDepts() {
        llSubDepts.removeAllViews()
        subDepts.forEachIndexed { i, name ->
            val row = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; setPadding(0, 4, 0, 4) }
            val tv = TextView(this).apply { text = name; layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f); textSize = 14f }
            val editBtn = Button(this).apply {
                text = "✎"
                setOnClickListener {
                    val input = EditText(this@SettingsActivity).apply { setText(name) }
                    AlertDialog.Builder(this@SettingsActivity).setTitle("Редактировать").setView(input)
                        .setPositiveButton("Сохранить") { d, w ->
                            val nn = input.text.toString().trim()
                            if (nn.isNotEmpty()) { subDepts[i] = nn; SharedPrefs.saveSubDepts(this@SettingsActivity, subDepts); loadData() }
                        }.setNegativeButton("Отмена", null).show()
                }
            }
            val delBtn = Button(this).apply {
                text = "✕"
                setOnClickListener { showDeleteConfirmDialog("Удалить $name?", "") { subDepts.removeAt(i); SharedPrefs.saveSubDepts(this@SettingsActivity, subDepts); loadData() } }
            }
            row.addView(tv); row.addView(editBtn); row.addView(delBtn); llSubDepts.addView(row)
        }
    }

    private fun renderTemplates() {
        llTemplates.removeAllViews()
        templates.forEachIndexed { i, t ->
            val row = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(0, 8, 0, 8) }
            row.addView(TextView(this).apply { text = "${t.name}${if (t.type == "common") " (общий)" else if (t.dept.isNotEmpty()) " (${t.dept})" else ""}"; textSize = 14f; typeface = android.graphics.Typeface.DEFAULT_BOLD })
            row.addView(TextView(this).apply { text = t.text.take(60) + if (t.text.length > 60) "..." else ""; textSize = 12f; setTextColor(0xFF6F85A5.toInt()) })
            val btnRow = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
            val editBtn = Button(this).apply {
                text = "✎"
                setOnClickListener {
                    val intent = Intent(this@SettingsActivity, TemplateEditorActivity::class.java).apply {
                        putExtra("template_id", t.id); putExtra("template_name", t.name); putExtra("template_text", t.text); putExtra("template_type", t.type)
                    }
                    startActivity(intent)
                }
            }
            val delBtn = Button(this).apply { text = "✕"; setOnClickListener { showDeleteConfirmDialog("Удалить ${t.name}?", "") { templates.removeAt(i); SharedPrefs.saveTemplates(this@SettingsActivity, templates); loadData() } } }
            btnRow.addView(editBtn); btnRow.addView(delBtn); row.addView(btnRow); llTemplates.addView(row)
        }
    }

    private fun renderVariables() {
        llVars.removeAllViews()
        variables.forEachIndexed { i, v ->
            val row = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(0, 4, 0, 4) }
            val hRow = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
            hRow.addView(TextView(this).apply { text = "${v.name}${if (v.required) " *" else ""}"; layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f); textSize = 14f })
            hRow.addView(TextView(this).apply { text = " (${VariableTypes.displayNames[v.type] ?: v.type})"; textSize = 11f; setTextColor(0xFF6F85A5.toInt()) })
            val scopeText = when (v.typeGlobal) { "common" -> "[Общая]"; "dept" -> "[${v.dept}]"; "unit" -> "[${v.dept} — расчёт]"; else -> "" }
            hRow.addView(TextView(this).apply { text = scopeText; textSize = 11f; setTextColor(0xFF1A4CBA.toInt()) })
            row.addView(hRow)
            val bRow = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
            val editBtn = Button(this).apply { text = "✎"; setOnClickListener { showEditVariableDialog(i) } }
            val delBtn = Button(this).apply { text = "✕"; setOnClickListener { showDeleteConfirmDialog("Удалить ${v.name}?", "") { variables.removeAt(i); SharedPrefs.saveVariables(this@SettingsActivity, variables); loadData() } } }
            bRow.addView(editBtn); bRow.addView(delBtn); row.addView(bRow); llVars.addView(row)
        }
    }

    private fun renderRecipients() {
        llRecipients.removeAllViews()
        recipients.forEachIndexed { i, r ->
            val row = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; setPadding(0, 4, 0, 4) }
            row.addView(TextView(this).apply { text = "${r.name} (${r.email})"; layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f) })
            row.addView(Button(this).apply { text = "✕"; setOnClickListener { showDeleteConfirmDialog("Удалить ${r.name}?", "") { recipients.removeAt(i); SharedPrefs.saveRecipients(this@SettingsActivity, recipients); loadData() } } })
            llRecipients.addView(row)
        }
    }

    private fun showDeleteConfirmDialog(title: String, message: String, onConfirm: () -> Unit) {
        AlertDialog.Builder(this).setTitle(title).setMessage(message).setIcon(android.R.drawable.ic_dialog_alert)
            .setPositiveButton("Удалить") { d, w -> onConfirm() }.setNegativeButton("Отмена", null).show()
    }

    private fun addSubDept() {
        val n = etSubDeptNew.text.toString().trim()
        if (n.isEmpty()) { Toast.makeText(this, "Введите название", Toast.LENGTH_SHORT).show(); return }
        if (subDepts.contains(n)) { Toast.makeText(this, "❌ Уже существует", Toast.LENGTH_SHORT).show(); return }
        subDepts.add(n); SharedPrefs.saveSubDepts(this, subDepts); etSubDeptNew.text.clear(); loadData()
    }

    private fun addRecipient() {
        val n = etRecipientName.text.toString().trim(); val e = etRecipientEmail.text.toString().trim()
        if (n.isEmpty() || e.isEmpty()) { Toast.makeText(this, "Заполните поля", Toast.LENGTH_SHORT).show(); return }
        if (!e.contains("@") || !e.contains(".")) { Toast.makeText(this, "❌ Некорректный email", Toast.LENGTH_SHORT).show(); return }
        recipients.add(Recipient(UUID.randomUUID().toString(), n, e, dept)); SharedPrefs.saveRecipients(this, recipients)
        etRecipientName.text.clear(); etRecipientEmail.text.clear(); loadData()
    }

    private fun showAddVariableDialog() = showVariableDialog(null)
    private fun showEditVariableDialog(index: Int) = showVariableDialog(index)

    private fun showVariableDialog(index: Int?) {
        val isEdit = index != null
        val v = if (isEdit) variables[index!!] else null

        val nm = EditText(this).apply { hint = "Название *"; if (isEdit) setText(v!!.name) }
        val tp = Spinner(this).apply {
            adapter = ArrayAdapter(this@SettingsActivity, android.R.layout.simple_spinner_item, VariableTypes.all.map { VariableTypes.displayNames[it] ?: it }).also { it.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item) }
            if (isEdit) setSelection(VariableTypes.all.indexOf(v!!.type))
        }
        val scopeSp = Spinner(this).apply {
            adapter = ArrayAdapter(this@SettingsActivity, android.R.layout.simple_spinner_item, listOf("Общая", "Подразделение", "Расчёт")).also { it.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item) }
            if (isEdit) setSelection(when (v!!.typeGlobal) { "common" -> 0; "dept" -> 1; "unit" -> 2; else -> 0 })
        }
        val deptSp = Spinner(this).apply {
            adapter = ArrayAdapter(this@SettingsActivity, android.R.layout.simple_spinner_item, allDepts).also { it.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item) }
            if (isEdit) { val idx = allDepts.indexOf(v!!.dept); if (idx >= 0) setSelection(idx) }
            visibility = if (isEdit && v!!.typeGlobal != "common" || !isEdit && scopeSp.selectedItemPosition != 0) View.VISIBLE else View.GONE
        }
        val optsInput = EditText(this).apply {
            hint = "Варианты через запятую (для списка)"
            if (isEdit) setText(v!!.options.joinToString(", "))
            visibility = if (isEdit && (v!!.type == VariableTypes.SELECT || v!!.type == VariableTypes.MULTISELECT) || !isEdit && VariableTypes.all.getOrNull(tp.selectedItemPosition).let { it == VariableTypes.SELECT || it == VariableTypes.MULTISELECT }) View.VISIBLE else View.GONE
        }
        val ck = CheckBox(this).apply { text = "Обязательное"; if (isEdit) isChecked = v!!.required }

        tp.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(p: AdapterView<*>?, vw: View?, pos: Int, id: Long) {
                val t = VariableTypes.all.getOrNull(pos)
                optsInput.visibility = if (t == VariableTypes.SELECT || t == VariableTypes.MULTISELECT) View.VISIBLE else View.GONE
            }
            override fun onNothingSelected(p: AdapterView<*>?) {}
        }
        scopeSp.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(p: AdapterView<*>?, vw: View?, pos: Int, id: Long) {
                deptSp.visibility = if (pos == 0) View.GONE else View.VISIBLE
            }
            override fun onNothingSelected(p: AdapterView<*>?) {}
        }

        val ct = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; addView(nm); addView(tp); addView(scopeSp); addView(deptSp); addView(optsInput); addView(ck) }
        AlertDialog.Builder(this).setTitle(if (isEdit) "Редактировать" else "Добавить").setView(ct)
            .setPositiveButton("Сохранить") { d, w ->
                val name = nm.text.toString().trim()
                if (name.isEmpty()) { Toast.makeText(this, "❌ Введите название", Toast.LENGTH_SHORT).show(); return@setPositiveButton }
                val type = VariableTypes.all[tp.selectedItemPosition]
                val scope = when (scopeSp.selectedItemPosition) { 0 -> "common"; 1 -> "dept"; 2 -> "unit"; else -> "common" }
                val selDept = if (scope == "common") "" else deptSp.selectedItem.toString()
                val opts = if (type == VariableTypes.SELECT || type == VariableTypes.MULTISELECT) optsInput.text.toString().split(",").map { it.trim() }.filter { it.isNotEmpty() } else emptyList()
                if (isEdit) {
                    variables[index!!] = v!!.copy(name = name, type = type, required = ck.isChecked, typeGlobal = scope, dept = selDept, options = opts)
                } else {
                    if (variables.any { it.name == name && it.dept == selDept && it.typeGlobal == scope }) { Toast.makeText(this, "❌ Уже существует", Toast.LENGTH_SHORT).show(); return@setPositiveButton }
                    variables.add(Variable(id = UUID.randomUUID().toString(), name = name, type = type, required = ck.isChecked, typeGlobal = scope, dept = selDept, options = opts))
                }
                SharedPrefs.saveVariables(this, variables); loadData()
            }.setNegativeButton("Отмена", null).show()
    }

    private fun openFilePicker() = filePickerLauncher.launch(Intent(Intent.ACTION_OPEN_DOCUMENT).apply { addCategory(Intent.CATEGORY_OPENABLE); type = "application/json" })

    private fun importFromUri(uri: Uri) {
        try {
            val json = contentResolver.openInputStream(uri)?.bufferedReader()?.readText()
            if (json != null) { doImport(Gson().fromJson(json, Map::class.java)); tvImportResult.text = "✅ Импорт выполнен"; tvImportResult.visibility = View.VISIBLE }
            else Toast.makeText(this, "❌ Не удалось прочитать", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) { Toast.makeText(this, "❌ ${e.message}", Toast.LENGTH_SHORT).show() }
    }

    @Suppress("UNCHECKED_CAST")
    private fun doImport(data: Map<*, *>) {
        try {
            (data["allDepts"] as? List<String>)?.forEach { n -> if (!allDepts.contains(n)) allDepts.add(n) }; SharedPrefs.saveDepts(this, allDepts)
            (data["subDepts"] as? List<String>)?.forEach { n -> if (!subDepts.contains(n)) subDepts.add(n) }; SharedPrefs.saveSubDepts(this, subDepts)
            (data["templates"] as? List<Map<String, Any>>)?.forEach { t ->
                val tn = t["name"] as? String ?: ""
                if (templates.none { it.name == tn }) templates.add(Template(UUID.randomUUID().toString(), tn, t["text"] as? String ?: "", t["type"] as? String ?: "own", if ((t["type"] as? String) == "common") "" else dept))
            }; SharedPrefs.saveTemplates(this, templates)
            (data["variables"] as? List<Map<String, Any>>)?.forEach { v ->
                val vn = v["name"] as? String ?: ""; val vd = v["dept"] as? String ?: ""
                if (variables.none { it.name == vn && it.dept == vd }) variables.add(Variable(UUID.randomUUID().toString(), vn, v["type"] as? String ?: "text", v["required"] as? Boolean ?: false, v["typeGlobal"] as? String ?: "common", vd, (v["options"] as? List<String>) ?: emptyList()))
            }; SharedPrefs.saveVariables(this, variables)
            (data["recipients"] as? List<Map<String, String>>)?.forEach { r ->
                val re = r["email"] ?: ""
                if (recipients.none { it.email == re }) recipients.add(Recipient(UUID.randomUUID().toString(), r["name"] ?: "", re, dept))
            }; SharedPrefs.saveRecipients(this, recipients)
            loadData(); Toast.makeText(this, "✅ Импорт выполнен", Toast.LENGTH_LONG).show()
        } catch (e: Exception) { Toast.makeText(this, "❌ ${e.message}", Toast.LENGTH_SHORT).show() }
    }

    private fun exportSettings() {
        try {
            val json = GsonBuilder().setPrettyPrinting().create().toJson(mapOf("dept" to dept, "allDepts" to allDepts, "subDepts" to subDepts, "templates" to templates, "variables" to variables, "recipients" to recipients, "exportedAt" to System.currentTimeMillis()))
            val f = File(DocxGenerator.getSettingsDir(), "настройки_${dept}_${SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())}.json")
            FileOutputStream(f).use { it.write(json.toByteArray()) }
            tvExportResult.text = "✅ ${f.absolutePath}"; tvExportResult.visibility = View.VISIBLE
        } catch (e: Exception) { Toast.makeText(this, "❌ ${e.message}", Toast.LENGTH_SHORT).show() }
    }

    private fun downloadTemplate() {
        try {
            val json = GsonBuilder().setPrettyPrinting().create().toJson(mapOf("_comment" to "Шаблон настроек OTCHETpro", "allDepts" to listOf("БпЛА", "Миномет", "Артиллерия", "Танки"), "subDepts" to listOf("Соисполнитель 1"), "templates" to listOf(mapOf("name" to "Шаблон", "text" to "Текст с {{Переменная}}", "type" to "own или common")), "variables" to listOf(mapOf("name" to "Переменная", "type" to "text", "required" to true, "typeGlobal" to "common/dept/unit", "dept" to "БпЛА", "options" to listOf("В1", "В2"))), "recipients" to listOf(mapOf("name" to "ФИО", "email" to "email@domain.ru"))))
            val f = File(DocxGenerator.getSettingsDir(), "шаблон_настроек.json")
            FileOutputStream(f).use { it.write(json.toByteArray()) }
            tvExportResult.text = "✅ ${f.absolutePath}"; tvExportResult.visibility = View.VISIBLE
        } catch (e: Exception) { Toast.makeText(this, "❌ ${e.message}", Toast.LENGTH_SHORT).show() }
    }
}
