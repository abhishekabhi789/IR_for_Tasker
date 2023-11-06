package com.abhi.irfortasker

enum class ErrorCodes(val code: Int, val message: String) {
    ERROR(1, "unknown error"),
    EMPTY_INPUT(2, "variable value unset"),
    INVALID_FREQUENCY(3, "invalid frequency"),
    INVALID_LENGTH(4, "invalid code length calculated"),
    NO_BUILTIN_IR_BLASTER(5, "no builtin ir emitter, fallback not permitted"),
    UNKNOWN_ERROR_DURING_TRANSMISSION(6, "unknown error during transmission"),
    NO_WIRED_HEADPHONE_CONNECTED(7, "no ir blaster detected on 3.5mm audio port")
}