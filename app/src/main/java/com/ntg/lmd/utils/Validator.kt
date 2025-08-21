package com.ntg.lmd.utils

private const val AGENT_ID_LENGTH = 14

class Validator {
    val passwordRegex = Regex("^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[^A-Za-z0-9]).{8,}$")

    fun isPasswordValid(password: String): Boolean = passwordRegex.matches(password)

    fun isUsernameValid(agentId: String): Boolean =
        agentId.length == AGENT_ID_LENGTH &&
            agentId.all {
                it.isDigit()
            }
}
