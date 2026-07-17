package com.otchetpro.app.activities

import android.app.AlertDialog
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.DocumentsContract
import android.provider.OpenableColumns
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
import java.io.FileReader
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

    // Регистрация для выбора файла
    private val filePickerLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            result.data?.data?.let { uri ->
                importFromUri(uri)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        tvDeptInfo = findViewById(R.id.tv_dept_info_settings)
        tvSettingsInfo = findViewById(R.id.tv_settings_info)
        tvSettingsTag = findViewById(R.id.tv_settings_tag)
        tvImportResult = findViewById(R.id.tv_import_result)
        tvExportResult = findViewById(R.id.tv_export_result)
        
        llDepts = findViewById(R.id.ll_depts_list)
        llSubDepts = findViewById(R.id.ll_subdepts_list)
        llTemplates = findViewById(R.id.ll_templates_list)
        llVars = findViewById(R.id.ll_vars_list)
        llRecipients = findViewById(R.id.ll_recipients_list)
        
        etSubDeptNew = findViewById(R.id.et_subdept_new)
        etRecipientName = findViewById(R.id.et_recipient_name)
        etRecipientEmail = findViewById(R.id.et_recipient_email)
        
        btnImport = findViewById(R.id.btn_import)
        btnExport = findViewById(R.id.btn_export)
        btnTemplateDownload = findViewById(R.id.btn_template_download)
        btnTemplateAdd = findViewById(R.id.btn_template_add)
        btnVarAdd = findViewById(R.id.btn_var_add)

        findViewById<Button>(R.id.btn_close_settings).setOnClickListener { finish() }
        findViewById<Button>(R.id.btn_subdept_add).setOnClickListener { addSubDept() }
        findViewById<Button>(R.id.btn_recipient_add).setOnClickListener { addRecipient() }
        
        btnTemplateAdd.setOnClickListener { 
            startActivity(Intent(this, TemplateEditorActivity::class.java))
        }
        btnVarAdd.setOnClickListener { addVariable() }
        
        btnExport.setOnClickListener { exportSettings() }
        btnImport.setOnClickListener { openFilePicker() }
        btnTemplateDownload.setOnClickListener { downloadTemplate() }

        setupAccordion(R.id.accordion_dept_header, R.id.accordion_dept_body)
        setupAccordion(R.id.accordion_subdepts_header, R.id.accordion_subdepts_body)
        setupAccordion(R.id.accordion_templates_header, R.id.accordion_templates_body)
        setupAccordion(R.id.accordion_vars_header, R.id.accordion_vars_body)
        setupAccordion(R.id.accordion_recipients_header, R.id.accordion_recipients_body)
        setupAccordion(R.id.accordion_export_header, R.id.accordion_export_body)

        loadData()
    }

    private fun setupAccordion(headerId: Int, bodyId: Int) {
        val header = findViewById<TextView>(headerId)
        val body = findViewById<LinearLayout>(bodyId)
        header.setOnClickListener {
            if (body.visibility == View.VISIBLE) {
                body.visibility = View.GONE
                header.text = "▶ " + header.text.toString().substring(2)
            } else {
                body.visibility = View.VISIBLE
                header.text = "▼ " + header.text.toString().substring(2)
            }
        }
    }

    private fun loadData() {
        dept = SharedPrefs.getDept(this)
        allDepts = SharedPrefs.getDepts(this).toMutableList()
        if (allDepts.isEmpty()) {
            allDepts = listOf("БпЛА", "Миномет", "Артиллерия", "Танки").toMutableList()
        }
        
        tvDeptInfo.text = "Настройки для подразделения: $dept"
        
        subDepts = SharedPrefs.getSubDepts(this).toMutableList()
        templates = SharedPrefs.getTemplates(this).filter { it.dept == dept || it.type == "common" }.toMutableList()
        variables = SharedPrefs.getVariables(this).filter { it.dept == dept || it.typeGlobal == "common" }.toMutableList()
        recipients = SharedPrefs.getRecipients(this).toMutableList()

        // === ПОДРАЗДЕЛЕНИЯ ===
        llDepts.removeAllViews()
        allDepts.forEachIndexed { i, name ->
            val row = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; setPadding(0, 4, 0, 4) }
            val tv = TextView(this).apply { 
                text = name
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                textSize = 14f
            }
            val editBtn = Button(this).apply {
                text = "✎"
                setOnClickListener {
                    val input = EditText(this@SettingsActivity).apply { setText(name) }
                    AlertDialog.Builder(this@SettingsActivity)
                        .setTitle("Редактировать подразделение")
                        .setView(input)
                        .setPositiveButton("Сохранить") { _, _ ->
                            val newName = input.text.toString().trim()
                            if (newName.isNotEmpty()) {
                                allDepts[i] = newName
                                SharedPrefs.saveDepts(this@SettingsActivity, allDepts)
                                if (dept == name) {
                                    SharedPrefs.saveDept(this@SettingsActivity, newName)
                                    dept = newName
                                }
                                loadData()
                                Toast.makeText(this@SettingsActivity, "✅ Обновлено: $newName", Toast.LENGTH_SHORT).show()
                            }
                        }
                        .setNegativeButton("Отмена", null)
                        .show()
                }
            }
            val deleteBtn = Button(this).apply {
                text = "✕"
                setOnClickListener {
                    if (allDepts.size <= 1) {
                        Toast.makeText(this@SettingsActivity, "Нельзя удалить последнее подразделение", Toast.LENGTH_SHORT).show()
                        return@setOnClickListener
                    }
                    AlertDialog.Builder(this@SettingsActivity)
                        .setTitle("Удалить подразделение")
                        .setMessage("Удалить $name и все связанные данные?")
                        .setPositiveButton("Удалить") { _, _ ->
                            allDepts.removeAt(i)
                            SharedPrefs.saveDepts(this@SettingsActivity, allDepts)
                            val vars = SharedPrefs.getVariables(this@SettingsActivity).toMutableList()
                            vars.removeAll { it.dept == name }
                            SharedPrefs.saveVariables(this@SettingsActivity, vars)
                            val temps = SharedPrefs.getTemplates(this@SettingsActivity).toMutableList()
                            temps.removeAll { it.dept == name }
                            SharedPrefs.saveTemplates(this@SettingsActivity, temps)
                            if (dept == name) {
                                SharedPrefs.saveDept(this@SettingsActivity, allDepts[0])
                                dept = allDepts[0]
                            }
                            loadData()
                            Toast.makeText(this@SettingsActivity, "✅ Удалено", Toast.LENGTH_SHORT).show()
                        }
                        .setNegativeButton("Отмена", null)
                        .show()
                }
            }
            row.addView(tv)
            row.addView(editBtn)
            row.addView(deleteBtn)
            llDepts.addView(row)
        }
        
        val addDeptRow = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; setPadding(0, 8, 0, 0) }
        val addDeptInput = EditText(this).apply { 
            hint = "Название нового подразделения"
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }
        val addDeptBtn = Button(this).apply {
            text = "➕"
            setOnClickListener {
                val name = addDeptInput.text.toString().trim()
                if (name.isNotEmpty() && !allDepts.contains(name)) {
                    allDepts.add(name)
                    SharedPrefs.saveDepts(this@SettingsActivity, allDepts)
                    addDeptInput.text.clear()
                    loadData()
                    Toast.makeText(this@SettingsActivity, "✅ Подразделение добавлено", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this@SettingsActivity, "Некорректное имя или уже существует", Toast.LENGTH_SHORT).show()
                }
            }
        }
        addDeptRow.addView(addDeptInput)
        addDeptRow.addView(addDeptBtn)
        llDepts.addView(addDeptRow)

        // === СОИСПОЛНИТЕЛИ (подподразделения) ===
        llSubDepts.removeAllViews()
        subDepts.forEachIndexed { i, name ->
            val row = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; setPadding(0, 4, 0, 4) }
            val tv = TextView(this).apply { 
                text = name
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                textSize = 14f
            }
            val editBtn = Button(this).apply {
                text = "✎"
                setOnClickListener {
                    val input = EditText(this@SettingsActivity).apply { setText(name) }
                    AlertDialog.Builder(this@SettingsActivity)
                        .setTitle("Редактировать соисполнителя")
                        .setView(input)
                        .setPositiveButton("Сохранить") { _, _ ->
                            val newName = input.text.toString().trim()
                            if (newName.isNotEmpty()) {
                                subDepts[i] = newName
                                SharedPrefs.saveSubDepts(this@SettingsActivity, subDepts)
                                loadData()
                            }
                        }
                        .setNegativeButton("Отмена", null)
                        .show()
                }
            }
            val deleteBtn = Button(this).apply {
                text = "✕"
                setOnClickListener {
                    subDepts.removeAt(i)
                    SharedPrefs.saveSubDepts(this@SettingsActivity, subDepts)
                    loadData()
                }
            }
            row.addView(tv)
            row.addView(editBtn)
            row.addView(deleteBtn)
            llSubDepts.addView(row)
        }

        // === ШАБЛОНЫ ===
        llTemplates.removeAllViews()
        templates.forEachIndexed { i, t ->
            val row = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(0, 8, 0, 8) }
            val nameTv = TextView(this).apply { 
                text = t.name + if (t.type == "common") " (общий)" else " (подразделения)"
                textSize = 14f
                typeface = android.graphics.Typeface.DEFAULT_BOLD
            }
            row.addView(nameTv)
            val previewTv = TextView(this).apply { 
                text = t.text.take(60) + if (t.text.length > 60) "..." else ""
                textSize = 12f
                setTextColor(0xFF6F85A5.toInt())
            }
            row.addView(previewTv)
            val btnRow = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
            val editBtn = Button(this).apply { 
                text = "✎ Редактировать"
                setOnClickListener { 
                    val intent = Intent(this@SettingsActivity, TemplateEditorActivity::class.java)
                    intent.putExtra("template_id", t.id)
                    intent.putExtra("template_name", t.name)
                    intent.putExtra("template_text", t.text)
                    intent.putExtra("template_type", t.type)
                    startActivity(intent)
                }
            }
            val deleteBtn = Button(this).apply { 
                text = "✕ Удалить"
                setOnClickListener {
                    templates.removeAt(i)
                    SharedPrefs.saveTemplates(this@SettingsActivity, templates)
                    loadData()
                }
            }
            btnRow.addView(editBtn)
            btnRow.addView(deleteBtn)
            row.addView(btnRow)
            llTemplates.addView(row)
        }

        // === ПЕРЕМЕННЫЕ ===
        llVars.removeAllViews()
        variables.forEachIndexed { i, v ->
            val row = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(0, 4, 0, 4) }
            val headerRow = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
            val tv = TextView(this).apply { 
                text = v.name + if (v.required) " *" else ""
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                textSize = 14f
            }
            val typeTv = TextView(this).apply { 
                text = " (${VariableTypes.displayNames[v.type] ?: v.type})"
                textSize = 11f
                setTextColor(0xFF6F85A5.toInt())
            }
            val scopeTv = TextView(this).apply {
                text = when (v.typeGlobal) {
                    "common" -> " [Общая]"
                    "dept" -> " [Подразделение]"
                    "unit" -> " [Расчет]"
                    else -> ""
                }
                textSize = 11f
                setTextColor(0xFF1A4CBA.toInt())
            }
            headerRow.addView(tv)
            headerRow.addView(typeTv)
            headerRow.addView(scopeTv)
            
            val btnRow = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
            val editBtn = Button(this).apply {
                text = "✎"
                setOnClickListener { editVariable(i) }
            }
            val deleteBtn = Button(this).apply {
                text = "✕"
                setOnClickListener {
                    variables.removeAt(i)
                    SharedPrefs.saveVariables(this@SettingsActivity, variables)
                    loadData()
                }
            }
            btnRow.addView(editBtn)
            btnRow.addView(deleteBtn)
            
            row.addView(headerRow)
            row.addView(btnRow)
            llVars.addView(row)
        }

        // === АДРЕСНАЯ КНИГА ===
        llRecipients.removeAllViews()
        recipients.forEachIndexed { i, r ->
            val row = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; setPadding(0, 4, 0, 4) }
            val tv = TextView(this).apply { 
                text = "${r.name} (${r.email})"
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            }
            val deleteBtn = Button(this).apply { 
                text = "✕"
                setOnClickListener {
                    recipients.removeAt(i)
                    SharedPrefs.saveRecipients(this@SettingsActivity, recipients)
                    loadData()
                }
            }
            row.addView(tv)
            row.addView(deleteBtn)
            llRecipients.addView(row)
        }

        val unit = SharedPrefs.getDeptUnit(this, dept) ?: getDefaultUnit(dept)
        tvSettingsInfo.text = 
            "${allDepts.size} подразделений · ${subDepts.size} соисполнителей · ${templates.size} шаблонов · ${variables.size} переменных"
        tvSettingsTag.text = "$dept — $unit"
    }

    private fun editVariable(index: Int) {
        val v = variables[index]
        val nm = EditText(this).apply { setText(v.name) }
        val tp = Spinner(this)
        val items = VariableTypes.all.map { VariableTypes.displayNames[it] ?: it }
        val spinnerAdapter = ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, items)
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        tp.adapter = spinnerAdapter
        tp.setSelection(VariableTypes.all.indexOf(v.type))
        
        // Выбор области видимости
        val scopeSpinner = Spinner(this)
        val scopeItems = listOf("Общая", "Подразделение", "Расчет")
        val scopeAdapter = ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, scopeItems)
        scopeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        scopeSpinner.adapter = scopeAdapter
        val scopeIndex = when (v.typeGlobal) {
            "common" -> 0
            "dept" -> 1
            "unit" -> 2
            else -> 0
        }
        scopeSpinner.setSelection(scopeIndex)
        
        // Для подразделения/расчета — выбор подразделения
        val deptSpinner = Spinner(this)
        val deptAdapter = ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, allDepts)
        deptAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        deptSpinner.adapter = deptAdapter
        val deptIndex = allDepts.indexOf(v.dept)
        if (deptIndex >= 0) deptSpinner.setSelection(deptIndex)
        
        // Для расчета — выбор расчета (список переменных "Расчет")
        val unitSpinner = Spinner(this)
        val unitItems = variables.filter { it.name == "Расчет" && it.dept == v.dept }
            .flatMap { it.options }
            .toList()
        val unitAdapter = ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, 
            if (unitItems.isEmpty()) listOf("Нет расчетов") else unitItems)
        unitAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        unitSpinner.adapter = unitAdapter
        
        val optionsInput = EditText(this).apply {
            hint = "Варианты через запятую (для списка)"
            setText(v.options.joinToString(", "))
            visibility = if (v.type == VariableTypes.SELECT || v.type == VariableTypes.MULTISELECT) View.VISIBLE else View.GONE
        }
        
        tp.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val type = VariableTypes.all[position]
                optionsInput.visibility = if (type == VariableTypes.SELECT || type == VariableTypes.MULTISELECT) {
                    View.VISIBLE
                } else {
                    View.GONE
                }
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
        
        // Показываем/скрываем дополнительные поля
        scopeSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                deptSpinner.visibility = if (position == 1 || position == 2) View.VISIBLE else View.GONE
                unitSpinner.visibility = if (position == 2) View.VISIBLE else View.GONE
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
        
        val ck = CheckBox(this).apply { isChecked = v.required; text = "Обязательное" }
        val ct = LinearLayout(this).apply { 
            orientation = LinearLayout.VERTICAL
            addView(nm)
            addView(tp)
            addView(scopeSpinner)
            addView(deptSpinner)
            addView(unitSpinner)
            addView(optionsInput)
            addView(ck)
        }
        
        AlertDialog.Builder(this).setTitle("Редактировать переменную").setView(ct)
            .setPositiveButton("Сохранить") { _, _ ->
                val name = nm.text.toString().trim()
                if (name.isEmpty()) { Toast.makeText(this, "Введите название", Toast.LENGTH_SHORT).show(); return@setPositiveButton }
                val type = VariableTypes.all[tp.selectedItemPosition]
                val scope = when (scopeSpinner.selectedItemPosition) {
                    0 -> "common"
                    1 -> "dept"
                    2 -> "unit"
                    else -> "common"
                }
                val options = if (type == VariableTypes.SELECT || type == VariableTypes.MULTISELECT) {
                    optionsInput.text.toString().split(",").map { it.trim() }.filter { it.isNotEmpty() }
                } else emptyList()
                
                val selectedDept = if (scope == "common") "" else deptSpinner.selectedItem.toString()
                
                variables[index] = v.copy(
                    name = name,
                    type = type,
                    required = ck.isChecked,
                    typeGlobal = scope,
                    dept = selectedDept,
                    options = options
                )
                SharedPrefs.saveVariables(this, variables)
                loadData()
                Toast.makeText(this, "✅ Переменная обновлена", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Отмена", null)
            .show()
    }

    private fun getDefaultUnit(dept: String): String {
        return when(dept) {
            "БпЛА" -> "ПВР №2 «Пчела»"
            "Миномет" -> "расчет миномета «ТИГР»"
            "Артиллерия" -> "152-мм гаубица «Гиацинт»"
            "Танки" -> "танковый взвод Т-72"
            else -> ""
        }
    }

    private fun addSubDept() {
        val name = etSubDeptNew.text.toString().trim()
        if (name.isEmpty()) { Toast.makeText(this, "Введите название", Toast.LENGTH_SHORT).show(); return }
        subDepts.add(name)
        SharedPrefs.saveSubDepts(this, subDepts)
        etSubDeptNew.text.clear()
        loadData()
        Toast.makeText(this, "✅ Добавлено", Toast.LENGTH_SHORT).show()
    }

    private fun addRecipient() {
        val name = etRecipientName.text.toString().trim()
        val email = etRecipientEmail.text.toString().trim()
        if (name.isEmpty() || email.isEmpty()) { Toast.makeText(this, "Заполните поля", Toast.LENGTH_SHORT).show(); return }
        if (!email.contains("@")) { Toast.makeText(this, "Некорректный email", Toast.LENGTH_SHORT).show(); return }
        recipients.add(Recipient(UUID.randomUUID().toString(), name, email, dept))
        SharedPrefs.saveRecipients(this, recipients)
        etRecipientName.text.clear()
        etRecipientEmail.text.clear()
        loadData()
        Toast.makeText(this, "✅ Добавлен", Toast.LENGTH_SHORT).show()
    }

    private fun addVariable() {
        val nm = EditText(this).apply { hint = "Название переменной" }
        val tp = Spinner(this)
        val items = VariableTypes.all.map { VariableTypes.displayNames[it] ?: it }
        val spinnerAdapter = ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, items)
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        tp.adapter = spinnerAdapter
        
        // Выбор области видимости
        val scopeSpinner = Spinner(this)
        val scopeItems = listOf("Общая", "Подразделение", "Расчет")
        val scopeAdapter = ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, scopeItems)
        scopeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        scopeSpinner.adapter = scopeAdapter
        
        // Для подразделения/расчета — выбор подразделения
        val deptSpinner = Spinner(this)
        val deptAdapter = ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, allDepts)
        deptAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        deptSpinner.adapter = deptAdapter
        deptSpinner.visibility = View.GONE
        
        // Для расчета — выбор расчета (список переменных "Расчет")
        val unitSpinner = Spinner(this)
        val unitAdapter = ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, listOf("Нет расчетов"))
        unitAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        unitSpinner.adapter = unitAdapter
        unitSpinner.visibility = View.GONE
        
        val optionsInput = EditText(this).apply { 
            hint = "Варианты через запятую (для списка)"
            visibility = View.GONE
        }
        
        tp.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val type = VariableTypes.all[position]
                optionsInput.visibility = if (type == VariableTypes.SELECT || type == VariableTypes.MULTISELECT) {
                    View.VISIBLE
                } else {
                    View.GONE
                }
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
        
        scopeSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                deptSpinner.visibility = if (position == 1 || position == 2) View.VISIBLE else View.GONE
                unitSpinner.visibility = if (position == 2) View.VISIBLE else View.GONE
                if (position == 2) {
                    // Обновляем список расчетов для выбранного подразделения
                    val selectedDept = deptSpinner.selectedItem.toString()
                    val unitVars = variables.filter { it.name == "Расчет" && it.dept == selectedDept }
                    val unitOptions = unitVars.flatMap { it.options }
                    val newAdapter = ArrayAdapter<String>(
                        this@SettingsActivity, 
                        android.R.layout.simple_spinner_item,
                        if (unitOptions.isEmpty()) listOf("Нет расчетов") else unitOptions
                    )
                    newAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                    unitSpinner.adapter = newAdapter
                }
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
        
        // Обновляем расчеты при смене подразделения
        deptSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                if (scopeSpinner.selectedItemPosition == 2) {
                    val selectedDept = deptSpinner.selectedItem.toString()
                    val unitVars = variables.filter { it.name == "Расчет" && it.dept == selectedDept }
                    val unitOptions = unitVars.flatMap { it.options }
                    val newAdapter = ArrayAdapter<String>(
                        this@SettingsActivity, 
                        android.R.layout.simple_spinner_item,
                        if (unitOptions.isEmpty()) listOf("Нет расчетов") else unitOptions
                    )
                    newAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                    unitSpinner.adapter = newAdapter
                }
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
        
        val ck = CheckBox(this).apply { text = "Обязательное" }
        val ct = LinearLayout(this).apply { 
            orientation = LinearLayout.VERTICAL
            addView(nm)
            addView(tp)
            addView(scopeSpinner)
            addView(deptSpinner)
            addView(unitSpinner)
            addView(optionsInput)
            addView(ck)
        }
        
        AlertDialog.Builder(this).setTitle("Добавить переменную").setView(ct)
            .setPositiveButton("OK") { _, _ ->
                val name = nm.text.toString().trim()
                if (name.isEmpty()) { Toast.makeText(this, "Введите название", Toast.LENGTH_SHORT).show(); return@setPositiveButton }
                val type = VariableTypes.all[tp.selectedItemPosition]
                val scope = when (scopeSpinner.selectedItemPosition) {
                    0 -> "common"
                    1 -> "dept"
                    2 -> "unit"
                    else -> "common"
                }
                val options = if (type == VariableTypes.SELECT || type == VariableTypes.MULTISELECT) {
                    optionsInput.text.toString().split(",").map { it.trim() }.filter { it.isNotEmpty() }
                } else emptyList()
                
                val selectedDept = if (scope == "common") "" else deptSpinner.selectedItem.toString()
                
                variables.add(Variable(
                    id = UUID.randomUUID().toString(),
                    name = name,
                    type = type,
                    required = ck.isChecked,
                    typeGlobal = scope,
                    dept = selectedDept,
                    options = options
                ))
                SharedPrefs.saveVariables(this, variables)
                loadData()
                Toast.makeText(this, "✅ Переменная добавлена", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Отмена", null)
            .show()
    }

    // ================================================================
    // ОТКРЫТИЕ ФАЙЛОВОГО МЕНЕДЖЕРА ДЛЯ ИМПОРТА
    // ================================================================
    private fun openFilePicker() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "application/json"
        }
        filePickerLauncher.launch(intent)
    }

    // ================================================================
    // ИМПОРТ ИЗ URI
    // ================================================================
    private fun importFromUri(uri: Uri) {
        try {
            val contentResolver = contentResolver
            val inputStream = contentResolver.openInputStream(uri)
            val json = inputStream?.bufferedReader()?.readText()
            inputStream?.close()
            
            if (json != null) {
                val data = Gson().fromJson(json, Map::class.java)
                doImport(data)
            } else {
                Toast.makeText(this, "❌ Не удалось прочитать файл", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            Toast.makeText(this, "❌ Ошибка импорта: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    // ================================================================
    // ВЫПОЛНЕНИЕ ИМПОРТА
    // ================================================================
    @Suppress("UNCHECKED_CAST")
    private fun doImport(data: Map<*, *>) {
        try {
            val importedDepts = data["allDepts"] as? List<String>
            if (importedDepts != null && importedDepts.isNotEmpty()) {
                allDepts.clear()
                allDepts.addAll(importedDepts)
                SharedPrefs.saveDepts(this, allDepts)
            }

            val importedSubDepts = data["subDepts"] as? List<String>
            if (importedSubDepts != null) {
                subDepts.clear()
                subDepts.addAll(importedSubDepts)
                SharedPrefs.saveSubDepts(this, subDepts)
            }

            val importedTemplates = data["templates"] as? List<Map<String, Any>>
            if (importedTemplates != null) {
                val newTemplates = importedTemplates.map { 
                    Template(
                        id = UUID.randomUUID().toString(),
                        name = it["name"] as? String ?: "",
                        text = it["text"] as? String ?: "",
                        type = it["type"] as? String ?: "own",
                        dept = if ((it["type"] as? String) == "common") "" else dept
                    )
                }
                templates.addAll(newTemplates)
                SharedPrefs.saveTemplates(this, templates)
            }

            val importedVariables = data["variables"] as? List<Map<String, Any>>
            if (importedVariables != null) {
                val newVariables = importedVariables.map {
                    Variable(
                        id = UUID.randomUUID().toString(),
                        name = it["name"] as? String ?: "",
                        type = it["type"] as? String ?: "text",
                        required = it["required"] as? Boolean ?: false,
                        typeGlobal = it["typeGlobal"] as? String ?: "common",
                        dept = it["dept"] as? String ?: "",
                        options = (it["options"] as? List<String>) ?: emptyList()
                    )
                }
                variables.addAll(newVariables)
                SharedPrefs.saveVariables(this, variables)
            }

            val importedRecipients = data["recipients"] as? List<Map<String, String>>
            if (importedRecipients != null) {
                val newRecipients = importedRecipients.map {
                    Recipient(
                        id = UUID.randomUUID().toString(),
                        name = it["name"] ?: "",
                        email = it["email"] ?: "",
                        dept = dept
                    )
                }
                recipients.addAll(newRecipients)
                SharedPrefs.saveRecipients(this, recipients)
            }

            loadData()
            tvImportResult.text = "✅ Импорт выполнен успешно!"
            tvImportResult.visibility = View.VISIBLE
            Toast.makeText(this, "✅ Импорт выполнен успешно!", Toast.LENGTH_LONG).show()
        } catch (e: Exception) {
            Toast.makeText(this, "❌ Ошибка импорта: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    // ================================================================
    // ЭКСПОРТ
    // ================================================================
    private fun exportSettings() {
        try {
            val settingsDir = DocxGenerator.getSettingsDir()

            val data = mapOf(
                "dept" to dept,
                "allDepts" to allDepts,
                "subDepts" to subDepts,
                "templates" to templates,
                "variables" to variables,
                "recipients" to recipients,
                "exportedAt" to System.currentTimeMillis()
            )
            val json = GsonBuilder().setPrettyPrinting().create().toJson(data)
            val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val fileName = "настройки_${dept}_${dateFormat.format(Date())}.json"
            val file = File(settingsDir, fileName)
            FileOutputStream(file).use { it.write(json.toByteArray()) }
            
            tvExportResult.text = "✅ Выгружено: ${file.absolutePath}"
            tvExportResult.visibility = View.VISIBLE
            Toast.makeText(this, "✅ Выгружено: ${file.absolutePath}", Toast.LENGTH_LONG).show()
        } catch (e: Exception) {
            Toast.makeText(this, "❌ Ошибка: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    // ================================================================
    // СКАЧАТЬ ШАБЛОН НАСТРОЕК
    // ================================================================
    private fun downloadTemplate() {
        try {
            val settingsDir = DocxGenerator.getSettingsDir()

            val template = mapOf(
                "_comment" to "Шаблон настроек OTCHETpro",
                "allDepts" to listOf("БпЛА", "Миномет", "Артиллерия", "Танки"),
                "subDepts" to listOf("Соисполнитель 1", "Соисполнитель 2"),
                "templates" to listOf(
                    mapOf(
                        "name" to "Название шаблона",
                        "text" to "Текст с {{Переменная}}",
                        "type" to "own или common"
                    )
                ),
                "variables" to listOf(
                    mapOf(
                        "name" to "Переменная",
                        "type" to "text",
                        "required" to true,
                        "typeGlobal" to "common / dept / unit",
                        "dept" to "БпЛА (если dept или unit)",
                        "options" to listOf("Вариант1", "Вариант2")
                    )
                ),
                "recipients" to listOf(
                    mapOf("name" to "ФИО", "email" to "email@domain.ru")
                ),
                "_instructions" to listOf(
                    "1. typeGlobal: common — общая, dept — подразделение, unit — расчет",
                    "2. Для dept и unit укажите dept",
                    "3. Для unit укажите расчет в options",
                    "4. Для списков заполните options"
                )
            )
            
            val json = GsonBuilder().setPrettyPrinting().create().toJson(template)
            val file = File(settingsDir, "шаблон_настроек.json")
            FileOutputStream(file).use { it.write(json.toByteArray()) }
            
            tvImportResult.text = "✅ Шаблон скачан: ${file.absolutePath}"
            tvImportResult.visibility = View.VISIBLE
            Toast.makeText(this, "✅ Шаблон скачан: ${file.absolutePath}", Toast.LENGTH_LONG).show()
        } catch (e: Exception) {
            Toast.makeText(this, "❌ Ошибка: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
}
