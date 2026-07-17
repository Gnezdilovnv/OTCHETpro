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
    
    private var dept = ""
    private val variableValues = mutableMapOf<String, String>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_create_report)

        spinnerTemplate = findViewById(R.id.spinner_template)
        tvPreview = findViewById(R.id.tv_preview)
        btnSave = findViewById(R.id.btn_save_report)
        btnClose = findViewById(R.id.btn_close_create)
        linearVariables = findViewById(R.id.linear_variables)
        tvVarCount = findViewById(R.id.tv_var_count)
        
        dept = SharedPrefs.getDept(this)

        val templates = SharedPrefs.getTemplates(this).filter { it.dept == dept || it.type == "common" }
        spinnerTemplate.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, templates.map { it.name })
            .also { it.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item) }

        val allVariables = SharedPrefs.getVariables(this).filter { it.dept == dept || it.typeGlobal == "common" }
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
                            updatePreview(templates)
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
                            updatePreview(templates)
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
                            updatePreview(templates)
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
                            updatePreview(templates)
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
                            updatePreview(templates)
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

        spinnerTemplate.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) { 
                updatePreview(templates) 
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        btnSave.setOnClickListener { saveReport(templates) }
        btnClose.setOnClickListener { finish() }
        
        updatePreview(templates)
    }

    private fun updatePreview(templates: List<Template>) {
        val position = spinnerTemplate.selectedItemPosition
        if (position < 0 || position >= templates.size) {
            tvPreview.text = "Выберите шаблон"
            return
        }
        
        var text = templates[position].text
        variableValues.forEach { (key, value) -> 
            text = text.replace("{{$key}}", if (value.isNotEmpty()) value else "[$key]")
        }
        tvPreview.text = text
        tvVarCount.text = "${variableValues.size} переменных"
    }

    private fun saveReport(templates: List<Template>) {
        val position = spinnerTemplate.selectedItemPosition
        if (position < 0 || position >= templates.size) {
            Toast.makeText(this, "Выберите шаблон", Toast.LENGTH_SHORT).show()
            return
        }
        
        var text = templates[position].text
        variableValues.forEach { (key, value) -> 
            text = text.replace("{{$key}}", if (value.isNotEmpty()) value else "[$key]")
        }
        
        val report = Report(
            dept = dept, 
            templateName = templates[position].name, 
            text = text, 
            variables = variableValues.toString(), 
            status = "saved"
        )

        CoroutineScope(Dispatchers.IO).launch {
            AppDatabase.getInstance(this@CreateReportActivity).reportDao().insert(report)
            DocxGenerator.generate(this@CreateReportActivity, text, "Отчет_${System.currentTimeMillis()}.docx")
            withContext(Dispatchers.Main) {
                Toast.makeText(this@CreateReportActivity, "✅ Сохранено!", Toast.LENGTH_SHORT).show()
                finish()
            }
        }
    }
}
