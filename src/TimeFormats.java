import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;

/**
 * 파일 저장 형식과 사용자 출력 형식에 맞춘 날짜/시간 파싱·포맷 유틸리티다.
 */
final class TimeFormats {
    static final DateTimeFormatter INPUT_DATE_TIME = DateTimeFormatter.ofPattern("uuuu-MM-dd HH:mm").withResolverStyle(ResolverStyle.STRICT);
    static final DateTimeFormatter FILE_DATE = DateTimeFormatter.ofPattern("uuuu-MM-dd").withResolverStyle(ResolverStyle.STRICT);
    static final DateTimeFormatter FILE_TIME = DateTimeFormatter.ofPattern("HH:mm").withResolverStyle(ResolverStyle.STRICT);
    static final DateTimeFormatter FILE_DATE_TIME = DateTimeFormatter.ofPattern("uuuu-MM-dd'T'HH:mm").withResolverStyle(ResolverStyle.STRICT);

    private TimeFormats() {
    }

    static LocalDate parseFileDate(String raw, String fileName, int lineNumber, String fieldName) throws AppDataException {
        try {
            return LocalDate.parse(raw, FILE_DATE);
        } catch (DateTimeParseException e) {
            throw new AppDataException(fileName, lineNumber, fieldName + " 형식이 잘못되었습니다.");
        }
    }

    static LocalTime parseFileTime(String raw, String fileName, int lineNumber, String fieldName) throws AppDataException {
        try {
            return LocalTime.parse(raw, FILE_TIME);
        } catch (DateTimeParseException e) {
            throw new AppDataException(fileName, lineNumber, fieldName + " 형식이 잘못되었습니다.");
        }
    }

    static LocalDateTime parseFileDateTime(String raw, String fileName, int lineNumber, String fieldName) throws AppDataException {
        try {
            return LocalDateTime.parse(raw, FILE_DATE_TIME);
        } catch (DateTimeParseException e) {
            throw new AppDataException(fileName, lineNumber, fieldName + " 형식이 잘못되었습니다.");
        }
    }

    static String formatUserDateTime(LocalDateTime value) {
        return value.format(INPUT_DATE_TIME);
    }

    static String formatUserDate(LocalDate value) {
        return value.format(FILE_DATE);
    }

    static String formatUserTime(LocalTime value) {
        return value.format(FILE_TIME);
    }

    static String formatFileDate(LocalDate value) {
        return value.format(FILE_DATE);
    }

    static String formatFileTime(LocalTime value) {
        return value.format(FILE_TIME);
    }

    static String formatFileDateTime(LocalDateTime value) {
        return value.format(FILE_DATE_TIME);
    }
}
