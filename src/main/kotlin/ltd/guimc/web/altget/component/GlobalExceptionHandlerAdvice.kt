package ltd.guimc.web.altget.component

import ltd.guimc.web.altget.entity.response.ResponseBase
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice

@RestControllerAdvice
class GlobalExceptionHandlerAdvice {
    @ExceptionHandler
    fun handleException(ex: Exception): ResponseBase<String> {
        return ResponseBase(-1, ex.message ?: "Unknown error")
    }
}