package craftassist.api;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import static org.junit.jupiter.api.Assertions.*;

class ApiExceptionTest {

    @Test
    void constructor_withType_messageMatchesType() {
        var ex = new ApiException(ApiException.Type.AUTHENTICATION);
        assertEquals(ApiException.Type.AUTHENTICATION.getMessage(), ex.getMessage());
        assertEquals(ApiException.Type.AUTHENTICATION, ex.getType());
    }

    @Test
    void constructor_withDetails_appendsDetails() {
        var ex = new ApiException(ApiException.Type.SERVER_ERROR, "500 Internal");
        assertTrue(ex.getMessage().contains("500 Internal"));
        assertTrue(ex.getMessage().contains(ApiException.Type.SERVER_ERROR.getMessage()));
    }

    @Test
    void constructor_withCause_preservesCause() {
        var cause = new RuntimeException("connection reset");
        var ex = new ApiException(ApiException.Type.NETWORK_ERROR, cause);
        assertSame(cause, ex.getCause());
        assertEquals(ApiException.Type.NETWORK_ERROR.getMessage(), ex.getMessage());
    }

    @Test
    void isRetriable_retriableTypes_returnsTrue() {
        assertTrue(new ApiException(ApiException.Type.SERVER_ERROR).isRetriable());
        assertTrue(new ApiException(ApiException.Type.TIMEOUT).isRetriable());
        assertTrue(new ApiException(ApiException.Type.NETWORK_ERROR).isRetriable());
    }

    @Test
    void isRetriable_nonRetriableTypes_returnsFalse() {
        assertFalse(new ApiException(ApiException.Type.AUTHENTICATION).isRetriable());
        assertFalse(new ApiException(ApiException.Type.RATE_LIMIT).isRetriable());
        assertFalse(new ApiException(ApiException.Type.PARSE_ERROR).isRetriable());
    }

    @ParameterizedTest
    @EnumSource(ApiException.Type.class)
    void allTypes_haveNonNullMessage(ApiException.Type type) {
        assertNotNull(type.getMessage());
        assertFalse(type.getMessage().isEmpty());
    }

    @ParameterizedTest
    @EnumSource(ApiException.Type.class)
    void getType_returnsCorrectType(ApiException.Type type) {
        var ex = new ApiException(type);
        assertEquals(type, ex.getType());
    }
}
