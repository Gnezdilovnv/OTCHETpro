package com.otchetpro.app.activities

import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.otchetpro.app.R
import com.otchetpro.app.data.*
import com.otchetpro.app.utils.*
import kotlinx.coroutines.*

class CreateReportActivity : AppCompatActivity() {

    private lateinit var spinnerTemplate: Spinner
    private lateinit var tvPreview: TextView
    private lateinit var btnSave: Button
    private lateinit var btnClose: Button
    private lateinit var linearVariables: LinearLayout
    private lateinit var tvVarCount: TextView
    private lateinit var spinnerDept: Spinner
    private lateinit var spinnerUnit: Spinner
    
    private var dept = ""
    private val variableValues = mutableMapOf<String, String>()
    private var selectedDept = ""
    private var selectedUnit = ""
    private var templates = listOf<Template>()
    private var allVariables = listOf<Variable>()
    private var subDepts = listOf<String>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_create_report)

        spinnerTemplate = findViewById(R.id.spinner_template)
        tvPreview = findViewById(R.id.tv_preview)
        btnSave = findViewById(R.id.btn_save_report)
        btnClose = findViewById(R.id.btn_close_create)
        linearVariables = findViewById(R.id.linear_variables)
        tvVarCount = findViewById(R.id.tv_var_count)
        spinnerDept = findViewById(R.id.spinner_dept)
        spinnerUnit = findViewById(R.id.spinner_unit)
        
        dept = SharedPrefs.getDept(this)
        subDepts = SharedPrefs.getSubDepts(this)

        // Загружаем шаблоны (свои + общие)
        val allTemplates = SharedPrefs.getTemplates(this)
        templates = allTemplates.filter { it.dept == dept || it.type == "common" }
        
        // Настраиваем спиннеры
        setupDeptSpinner()
        setupUnitSpinner()
        setupTemplateSpinner()
        
        allVariables = SharedPrefs.getVariables(this).filter { it.dept == dept || it.typeGlobal == "common" }
        
        generateVariableFields()

        spinnerTemplate.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) { 
                updatePreview() 
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        btnSave.setOnClickListener { saveReport() }
        btnClose.setOnClickListener { finish() }
        
        updatePreview()
    }

    private fun setupDeptSpinner() {
        val depts = listOf("БпЛА", "Миномет", "Артиллерия", "Танки")
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, depts)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerDept.adapter = adapter
        spinnerDept.setSelection(depts.indexOf(dept))
        
        spinnerDept.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                selectedDept = parent?.getItemAtPosition(position).toString() ?: dept
                setupUnitSpinner()
                updatePreview()
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }

    private fun setupUnitSpinner() {
        val unitMap = mapOf(
            "БпЛА" to listOf("ПВР №2 «Пчела»", "ПВР №3 «Шмель»", "ПВР №4 «Оса»"),
            "Миномет" to listOf("расчет миномета (2Б11 И, 120мм) «ТИГР»", "расчет миномета (2Б9) «Град»", "расчет миномета (2Б14) «Сани»"),
            "Артиллерия" to listOf("расчет 152-мм гаубицы «Гиацинт»", "расчет 152-мм гаубицы «Мста»", "расчет 203-мм гаубицы «Акация»"),
            "Танки" to listOf("танковый взвод Т-72", "танковый взвод Т-80", "танковый взвод Т-90")
        )
        
        val currentDept = spinnerDept.selectedItem.toString()
        val units = unitMap[currentDept] ?: listOf()
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, units)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerUnit.adapter = adapter
        
        spinnerUnit.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                selectedUnit = parent?.getItemAtPosition(position).toString() ?: ""
                updatePreview()
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }

    private fun setupTemplateSpinner() {
        val templateNames = templates.map { it.name }
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, templateNames)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerTemplate.adapter = adapter
    }

    private fun generateVariableFields() {
        linearVariables.removeAllViews()
        
        allVariables.forEach { variable ->
            val row = LinearLayout(this).apply { 
                orientation = LinearLayout.VERTICAL
                setPadding(0, 0, 0, 16)
            }
            
            val label = TextView(this).apply {
                text = variable.name + if (variable.required) " *" else ""
                setTextColor(0xFF0B1A2F.toInt())
                typeface = android.graphics.Typeface.DEFAULT_BOLD
            }
            row.addView(label)
            
            val inputField = when (variable.type) {
                "date" -> EditText(this).apply {
                    hint = "ДД.ММ.ГГГГ"
                    setPadding(12, 12, 12, 12)
                    setBackgroundResource(android.R.drawable.editbox_background)
                    addTextChangedListener(object : android.text.TextWatcher {
                        override fun afterTextChanged(text: android.text.Editable?) { 
                            variableValues[variable.name] = text.toString()
                            updatePreview()
                        }
                        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
                    })
                }
                "number" -> EditText(this).apply {
                    inputType = android.text.InputType.TYPE_CLASS_NUMBER
                    hint = "Введите число"
                    setPadding(12, 12, 12, 12)
                    setBackgroundResource(android.R.drawable.editbox_background)
                    addTextChangedListener(object : android.text.TextWatcher {
                        override fun afterTextChanged(text: android.text.Editable?) { 
                            variableValues[variable.name] = text.toString()
                            updatePreview()
                        }
                        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
                    })
                }
                "select" -> Spinner(this).apply {
                    val options = variable.options.toTypedArray()
                    adapter = ArrayAdapter(this@CreateReportActivity, android.R.layout.simple_spinner_item, 
                        if (options.isEmpty()) arrayOf("Нет вариантов") else options)
                    setPadding(12, 12, 12, 12)
                    setBackgroundResource(android.R.drawable.editbox_background)
                    setOnItemSelectedListener(object : AdapterView.OnItemSelectedListener {
                        override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                            val selected = (parent?.adapter as? ArrayAdapter<String>)?.getItem(position) ?: ""
                            variableValues[variable.name] = selected
                            updatePreview()
                        }
                        override fun onNothingSelected(parent: AdapterView<*>?) {}
                    })
                }
                "multiselect" -> Spinner(this).apply {
                    val options = variable.options.toTypedArray()
                    adapter = ArrayAdapter(this@CreateReportActivity, android.R.layout.simple_spinner_item, 
                        if (options.isEmpty()) arrayOf("Нет вариантов") else options)
                    setPadding(12, 12, 12, 12)
                    setBackgroundResource(android.R.drawable.editbox_background)
                    setOnItemSelectedListener(object : AdapterView.OnItemSelectedListener {
                        override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                            val selected = (parent?.adapter as? ArrayAdapter<String>)?.getItem(position) ?: ""
                            variableValues[variable.name] = selected
                            updatePreview()
                        }
                        override fun onNothingSelected(parent: AdapterView<*>?) {}
                    })
                }
                else -> EditText(this).apply {
                    hint = "Введите значение"
                    setPadding(12, 12, 12, 12)
                    setBackgroundResource(android.R.drawable.editbox_background)
                    addTextChangedListener(object : android.text.TextWatcher {
                        override fun afterTextChanged(text: android.text.Editable?) { 
                            variableValues[variable.name] = text.toString()
                            updatePreview()
                        }
                        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
                    })
                }
            }
            row.addView(inputField)
            
            val hint = TextView(this).apply {
                text = if (variable.required) "Обязательное поле" else "Необязательное поле"
                textSize = 11f
                setTextColor(0xFF6F85A5.toInt())
            }
            row.addView(hint)
            linearVariables.addView(row)
        }
    }

    private fun updatePreview() {
        val position = spinnerTemplate.selectedItemPosition
        if (position < 0 || position >= templates.size) {
            tvPreview.text = "Выберите шаблон"
            return
        }
        
        var text = templates[position].text
        
        // Подставляем подразделение и расчет
        val deptName = spinnerDept.selectedItem.toString()
        val unitName = spinnerUnit.selectedItem.toString()
        text = text.replace("{{Подразделение}}", deptName)
        text = text.replace("{{Расчет}}", unitName)
        
        // Подставляем переменные
        allVariables.forEach { variable ->
            val value = variableValues[variable.name] ?: ""
            text = text.replace("{{${variable.name}}}", if (value.isNotEmpty()) value else "[${variable.name}]")
        }
        
        tvPreview.text = text
        tvVarCount.text = "${variableValues.size} переменных"
    }

    private fun saveReport() {
        val position = spinnerTemplate.selectedItemPosition
        if (position < 0 || position >= templates.size) {
            Toast.makeText(this, "Выберите шаблон", Toast.LENGTH_SHORT).show()
            return
        }
        
        var text = templates[position].text
        
        // Подставляем подразделение и расчет
        val deptName = spinnerDept.selectedItem.toString()
        val unitName = spinnerUnit.selectedItem.toString()
        text = text.replace("{{Подразделение}}", deptName)
        text = text.replace("{{Расчет}}", unitName)
        
        // Подставляем переменные
        allVariables.forEach { variable ->
            val value = variableValues[variable.name] ?: ""
            text = text.replace("{{${variable.name}}}", if (value.isNotEmpty()) value else "[${variable.name}]")
        }
        
        // Проверяем обязательные поля
        var allFilled = true
        allVariables.forEach { variable ->
            if (variable.required) {
                val value = variableValues[variable.name] ?: ""
                if (value.isEmpty()) {
                    allFilled = false
                }
            }
        }
        
        if (!allFilled) {
            Toast.makeText(this, "Заполните все обязательные поля!", Toast.LENGTH_SHORT).show()
            return
        }
        
        val report = Report(
            dept = deptName,
            templateName = templates[position].name,
            text = text,
            variables = variableValues.toString(),
            status = "saved"
        )

        CoroutineScope(Dispatchers.IO).launch {
            val id = AppDatabase.getInstance(this@CreateReportActivity).reportDao().insert(report)
            val fileName = "Отчет_${id}.docx"
            val file = DocxGenerator.generate(this@CreateReportActivity, text, fileName)
            withContext(Dispatchers.Main) {
                if (file != null) {
                    Toast.makeText(this@CreateReportActivity, "✅ Сохранено: ${file.absolutePath}", Toast.LENGTH_LONG).show()
                } else {
                    Toast.makeText(this@CreateReportActivity, "❌ Ошибка сохранения файла", Toast.LENGTH_SHORT).show()
                }
                finish()
            }
        }
    }
}
