package com.otchetpro.app.data

data class Variable(
    val id: String = "",
    val name: String = "",
    val type: String = "text",
    val required: Boolean = false,
    val typeGlobal: String = "own",
    val dept: String = "",
    val options: List<String> = emptyList(),
    val config: Map<String, Any> = emptyMap()
)
