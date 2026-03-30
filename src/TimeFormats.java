import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;

final class TimeFormats {
    static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("uuuu-MM-dd").withResolverStyle(ResolverStyle.STRICT);
    static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm").withResolverStyle(ResolverStyle.STRICT);
    static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("uuuu-MM-dd'T'HH:mm").withResolverStyle(ResolverStyle.STRICT);

    private TimeFormats() {
    }

    static LocalDate parseDate(String raw, String fileName, int lineNumber, String fieldName) throws AppDataException {
        try {
            return LocalDate.parse(raw, DATE);
        } catch (DateTimeParseException e) {
            throw new AppDataException(fileName, lineNumber, fieldName + " 형식이 올바르지 않습니다.");
        }
    }

    static LocalTime parseTime(String raw, String fileName, int lineNumber, String fieldName) throws AppDataException {
        try {
            return LocalTime.parse(raw, TIME);
        } catch (DateTimeParseException e) {
            throw new AppDataException(fileName, lineNumber, fieldName + " 형식이 올바르지 않습니다.");
        }
    }

    static LocalDateTime parseDateTime(String raw, String fileName, int lineNumber, String fieldName) throws AppDataException {
        try {
            return LocalDateTime.parse(raw, DATE_TIME);
        } catch (DateTimeParseException e) {
            throw new AppDataException(fileName, lineNumber, fieldName + " 형식이 올바르지 않습니다.");
        }
    }

    static String formatDate(LocalDate value) {
        return value.format(DATE);
    }

    static String formatTime(LocalTime value) {
        return value.format(TIME);
    }

    static String formatDateTime(LocalDateTime value) {
        return value.format(DATE_TIME);
    }
}
