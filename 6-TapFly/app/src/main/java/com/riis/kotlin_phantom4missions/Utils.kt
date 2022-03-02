package com.riis.kotlin_phantom4missions

class Utils {
    companion object {
        fun addLineToSB(sb: StringBuffer, name: String, value: Any) {
            sb.apply {
                append(if ("" == name) "" else "$name: ")
                append(value.toString() + "")
                append("\n")
            }
        }
    }

}