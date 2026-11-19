package ru.example.tickets.exception;

import jakarta.validation.ConstraintViolationException;
import java.sql.SQLException;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

@RestControllerAdvice
public class GlobalExceptionHandler {
    private ResponseEntity<?> error(int status, String message, Map<String, String> fields) {
        return ResponseEntity.status(status).body(Map.of("message", message, "fields", fields));
    }

    @ExceptionHandler(BusinessException.class)
    ResponseEntity<?> business(BusinessException e) {
        return error(e.getStatus(), e.getMessage(), Map.of());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<?> validation(MethodArgumentNotValidException e) {
        var fields = new LinkedHashMap<String, String>();
        e.getBindingResult()
                .getFieldErrors()
                .forEach(
                        f ->
                                fields.put(
                                        f.getField(),
                                        translate(f.getCode(), f.getDefaultMessage())));
        return error(400, "Проверьте заполнение полей", fields);
    }

    private String translate(String code, String fallback) {
        return switch (code == null ? "" : code) {
            case "NotNull" -> "Обязательное поле";
            case "Positive" -> "Значение должно быть больше 0";
            case "Size" -> "Строка не может быть пустой";
            case "Min" -> "Значение должно быть не меньше 1";
            case "Max" -> "Значение должно быть не больше 100";
            default -> fallback;
        };
    }

    @ExceptionHandler(ConstraintViolationException.class)
    ResponseEntity<?> constraint(ConstraintViolationException e) {
        var fields = new LinkedHashMap<String, String>();
        e.getConstraintViolations()
                .forEach(
                        v ->
                                fields.put(
                                        v.getPropertyPath().toString(),
                                        translate(
                                                v.getConstraintDescriptor()
                                                        .getAnnotation()
                                                        .annotationType()
                                                        .getSimpleName(),
                                                v.getMessage())));
        return error(400, "Недопустимые значения полей", fields);
    }

    @ExceptionHandler({
        HttpMessageNotReadableException.class,
        MethodArgumentTypeMismatchException.class
    })
    ResponseEntity<?> format(Exception e) {
        return error(
                400,
                "Неверный формат: проверьте числа, допустимые значения и обязательные поля",
                Map.of());
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    ResponseEntity<?> missingParameter(MissingServletRequestParameterException e) {
        return error(400, "Не указан обязательный параметр: " + e.getParameterName(), Map.of());
    }

    @ExceptionHandler(NoResourceFoundException.class)
    ResponseEntity<?> missingResource(NoResourceFoundException e) {
        return error(404, "Ресурс не найден", Map.of());
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    ResponseEntity<?> methodNotAllowed(HttpRequestMethodNotSupportedException e) {
        return error(405, "Метод запроса не поддерживается", Map.of());
    }

    @ExceptionHandler({org.springframework.orm.ObjectOptimisticLockingFailureException.class,
        jakarta.persistence.OptimisticLockException.class})
    ResponseEntity<?> optimistic(Exception e) {
        return error(409, "Объект изменён другим пользователем. Обновите данные.", Map.of());
    }

    @ExceptionHandler(org.springframework.web.bind.MissingRequestHeaderException.class)
    ResponseEntity<?> missingHeader(Exception e) {
        return error(428, "Передайте текущую версию билета в заголовке If-Match", Map.of());
    }

    @ExceptionHandler(org.springframework.web.multipart.MaxUploadSizeExceededException.class)
    ResponseEntity<?> tooLarge(Exception e) {
        return error(413, "Максимальный размер файла — 5 МБ", Map.of());
    }

    @ExceptionHandler({org.springframework.web.multipart.support.MissingServletRequestPartException.class,
        org.springframework.web.multipart.MultipartException.class})
    ResponseEntity<?> multipart(Exception e) {
        return error(400, "Передайте файл в поле file запроса multipart/form-data", Map.of());
    }

    @ExceptionHandler(org.springframework.web.HttpMediaTypeNotSupportedException.class)
    ResponseEntity<?> mediaType(Exception e) {
        return error(415, "Неподдерживаемый Content-Type запроса", Map.of());
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<?> other(Exception e) {
        for (SQLException sql : DatabaseErrors.causes(e)) {
                String state = sql.getSQLState();
                if ("P0002".equals(state)) return error(404, databaseMessage(sql), Map.of());
                if ("22023".equals(state)) return error(400, databaseMessage(sql), Map.of());
                if ("23503".equals(state))
                    return error(
                            409,
                            "Объект используется или связанный объект уже удалён. Обновите данные и"
                                    + " выберите замену.",
                            Map.of());
                if (state != null && (state.startsWith("22") || state.startsWith("23")))
                    return error(
                            400,
                            "Данные нарушают ограничения БД: проверьте диапазоны чисел и"
                                    + " обязательные поля",
                            Map.of());
                if (DatabaseErrors.isTransactionConflict(sql))
                    return error(
                            409,
                            "Данные изменяются другим запросом. Повторите операцию.",
                            Map.of());
        }
        org.slf4j.LoggerFactory.getLogger(getClass()).error("Request failed", e);
        return error(500, "Ошибка сервера. Подробности записаны в журнал сервера.", Map.of());
    }

    private String databaseMessage(SQLException e) {
        return e.getMessage().split("\\n")[0].replaceFirst("^ERROR: ", "");
    }
}
