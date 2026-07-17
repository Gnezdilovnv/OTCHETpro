package com.otchetpro.app.data

object VariableTypes {
    // 23 типа переменных
    const val TEXT = "text"
    const val TEXTAREA = "textarea"
    const val NUMBER = "number"
    const val DATE = "date"
    const val TIME = "time"
    const val DATETIME = "datetime"
    const val TIME_RANGE = "time_range"
    const val SELECT = "select"
    const val MULTISELECT = "multiselect"
    const val TOGGLE = "toggle"
    const val CHECKBOX = "checkbox"
    const val RADIO = "radio"
    const val COORDINATES = "coordinates"
    const val LOCATION = "location"
    const val OBJECT_TYPE = "object_type"
    const val RESULT = "result"
    const val CREW = "crew"
    const val WEAPON = "weapon"
    const val AMMUNITION = "ammunition"
    const val GROUP = "group"
    const val PERSON_LIST = "person_list"
    const val TECH_LIST = "tech_list"
    const val SEARCHABLE_SELECT = "searchable_select"
    
    val all = listOf(
        TEXT, TEXTAREA, NUMBER, DATE, TIME, DATETIME, TIME_RANGE,
        SELECT, MULTISELECT, TOGGLE, CHECKBOX, RADIO,
        COORDINATES, LOCATION, OBJECT_TYPE, RESULT, CREW,
        WEAPON, AMMUNITION, GROUP, PERSON_LIST, TECH_LIST, SEARCHABLE_SELECT
    )
    
    val displayNames = mapOf(
        TEXT to "Текстовое поле",
        TEXTAREA to "Текст полностью",
        NUMBER to "Цифровое значение",
        DATE to "Дата",
        TIME to "Время",
        DATETIME to "Дата и время",
        TIME_RANGE to "Период времени",
        SELECT to "Выпадающий список",
        MULTISELECT to "Мультивыбор",
        TOGGLE to "Переключатель",
        CHECKBOX to "Чекбокс",
        RADIO to "Радиокнопки",
        COORDINATES to "Координаты",
        LOCATION to "Населенный пункт",
        OBJECT_TYPE to "Объект",
        RESULT to "Результат",
        CREW to "Состав расчета",
        WEAPON to "ВВСТ",
        AMMUNITION to "Боеприпасы",
        GROUP to "Группа полей",
        PERSON_LIST to "Список людей",
        TECH_LIST to "Список техники",
        SEARCHABLE_SELECT to "Поисковый выбор"
    )
}
