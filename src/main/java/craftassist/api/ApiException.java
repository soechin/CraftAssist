package craftassist.api;

public class ApiException extends RuntimeException {

    public enum Type {
        AUTHENTICATION("API Key 無效，請檢查 config/craftassist.json"),
        RATE_LIMIT("API 速率限制，請稍後再試"),
        SERVER_ERROR("API 伺服器錯誤"),
        TIMEOUT("API 請求超時"),
        NETWORK_ERROR("網路連線失敗"),
        PARSE_ERROR("無法解析 API 回應");

        private final String message;

        Type(String message) {
            this.message = message;
        }

        public String getMessage() {
            return message;
        }
    }

    private final Type type;

    public ApiException(Type type) {
        super(type.getMessage());
        this.type = type;
    }

    public ApiException(Type type, String details) {
        super(type.getMessage() + "：" + details);
        this.type = type;
    }

    public ApiException(Type type, Throwable cause) {
        super(type.getMessage(), cause);
        this.type = type;
    }

    public Type getType() {
        return type;
    }

    public boolean isRetriable() {
        return type == Type.SERVER_ERROR || type == Type.TIMEOUT || type == Type.NETWORK_ERROR;
    }
}
