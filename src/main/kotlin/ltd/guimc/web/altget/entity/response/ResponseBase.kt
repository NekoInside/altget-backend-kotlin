package ltd.guimc.web.altget.entity.response

class ResponseBase<T>(
    val code: Int,
    val message: String,
    val data: T? = null
) {
    constructor(code: Int, message: String) : this(code, message, null)
    constructor(data: T) : this(0, "success", data)
}