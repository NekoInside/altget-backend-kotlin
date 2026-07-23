package ltd.guimc.web.altget.component

import ltd.guimc.web.altget.entity.response.ResponseBase
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice

@RestControllerAdvice
class GlobalExceptionHandlerAdvice {
    @ExceptionHandler(AdminBadRequestException::class)
    fun handleAdminBadRequest(ex: AdminBadRequestException): ResponseEntity<ResponseBase<String>> {
        return ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .body(ResponseBase(400, ex.message ?: "Bad request"))
    }

    @ExceptionHandler
    fun handleException(ex: Exception): ResponseBase<String> {
        return ResponseBase(-1, ex.message ?: "Unknown error")
    }
}
