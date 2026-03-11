import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * 텍스트 파일 4종(users, rooms, reservations, system_time)을 읽고 검증하며,
 * 임시 파일 재검증 후 원본을 교체하는 저장소 계층이다.
 */
final class TextDataStore {
    @FunctionalInterface
    interface FileMover {
        void move(Path source, Path target) throws IOException;
    }

    private static final Pattern USER_ID_PATTERN = Pattern.compile("^[a-z][a-z0-9]{3,11}$");
    private static final Pattern PASSWORD_PATTERN = Pattern.compile("^[A-Za-z0-9!@#$%^&*._-]{6,16}$");
    private static final Pattern NAME_PATTERN = Pattern.compile("^[A-Za-z가-힣 ]{2,20}$");
    private static final Pattern ROOM_ID_PATTERN = Pattern.compile("^R\\d{3}$");
    private static final Pattern RESERVATION_ID_PATTERN = Pattern.compile("^rv\\d{4,}$");
    private static final Pattern EQUIPMENT_CODE_PATTERN = Pattern.compile("^[A-Z][A-Z0-9_]*$");
    private static final Pattern NON_NEGATIVE_INTEGER_PATTERN = Pattern.compile("^\\d+$");

    private final Path rootDirectory;
    private final Path usersPath;
    private final Path roomsPath;
    private final Path reservationsPath;
    private final Path systemTimePath;
    private final FileMover fileMover;

    TextDataStore(Path rootDirectory) {
        this(rootDirectory, TextDataStore::moveReplacing);
    }

    TextDataStore(Path rootDirectory, FileMover fileMover) {
        this.rootDirectory = rootDirectory;
        this.usersPath = rootDirectory.resolve("users.txt");
        this.roomsPath = rootDirectory.resolve("rooms.txt");
        this.reservationsPath = rootDirectory.resolve("reservations.txt");
        this.systemTimePath = rootDirectory.resolve("system_time.txt");
        this.fileMover = fileMover;
    }

    SystemDataset loadAll() throws AppDataException {
        return loadAllFrom(usersPath, roomsPath, reservationsPath, systemTimePath);
    }

    void saveAll(SystemDataset dataset) throws AppDataException {
        Path usersTemp = null;
        Path roomsTemp = null;
        Path reservationsTemp = null;
        Path systemTimeTemp = null;
        Path usersBackup = null;
        Path roomsBackup = null;
        Path reservationsBackup = null;
        Path systemTimeBackup = null;
        try {
            usersTemp = Files.createTempFile(rootDirectory, "users", ".tmp");
            roomsTemp = Files.createTempFile(rootDirectory, "rooms", ".tmp");
            reservationsTemp = Files.createTempFile(rootDirectory, "reservations", ".tmp");
            systemTimeTemp = Files.createTempFile(rootDirectory, "system_time", ".tmp");

            Files.writeString(usersTemp, serializeUsers(dataset), StandardCharsets.UTF_8);
            Files.writeString(roomsTemp, serializeRooms(dataset), StandardCharsets.UTF_8);
            Files.writeString(reservationsTemp, serializeReservations(dataset), StandardCharsets.UTF_8);
            Files.writeString(systemTimeTemp, serializeSystemTime(dataset), StandardCharsets.UTF_8);

            loadAllFrom(usersTemp, roomsTemp, reservationsTemp, systemTimeTemp);

            usersBackup = backupOriginal(usersPath, "users");
            roomsBackup = backupOriginal(roomsPath, "rooms");
            reservationsBackup = backupOriginal(reservationsPath, "reservations");
            systemTimeBackup = backupOriginal(systemTimePath, "system_time");

            PendingSaveFile usersFile = new PendingSaveFile(usersTemp, usersPath, usersBackup);
            PendingSaveFile roomsFile = new PendingSaveFile(roomsTemp, roomsPath, roomsBackup);
            PendingSaveFile reservationsFile = new PendingSaveFile(reservationsTemp, reservationsPath, reservationsBackup);
            PendingSaveFile systemTimeFile = new PendingSaveFile(systemTimeTemp, systemTimePath, systemTimeBackup);

            replaceAll(usersFile, roomsFile, reservationsFile, systemTimeFile);

            usersTemp = null;
            roomsTemp = null;
            reservationsTemp = null;
            systemTimeTemp = null;
            usersBackup = null;
            roomsBackup = null;
            reservationsBackup = null;
            systemTimeBackup = null;
        } catch (IOException e) {
            throw new AppDataException("저장", 0, "파일 저장 중 I/O 오류가 발생했습니다: " + e.getMessage());
        } finally {
            deleteIfExists(usersTemp);
            deleteIfExists(roomsTemp);
            deleteIfExists(reservationsTemp);
            deleteIfExists(systemTimeTemp);
            deleteIfExists(usersBackup);
            deleteIfExists(roomsBackup);
            deleteIfExists(reservationsBackup);
            deleteIfExists(systemTimeBackup);
        }
    }

    private SystemDataset loadAllFrom(Path usersFile, Path roomsFile, Path reservationsFile, Path systemTimeFile) throws AppDataException {
        LinkedHashMap<String, User> users = parseUsers(usersFile, "users.txt");
        LinkedHashMap<String, Room> rooms = parseRooms(roomsFile, "rooms.txt");
        LinkedHashMap<String, Reservation> reservations = parseReservations(reservationsFile, "reservations.txt");
        LocalDateTime currentTime = parseSystemTime(systemTimeFile, "system_time.txt");

        SystemDataset dataset = new SystemDataset(users, rooms, reservations, currentTime);
        validateUsers(dataset.users);
        validateRooms(dataset.rooms);
        validateReservations(dataset);
        validateClosedRooms(dataset);
        return dataset;
    }

    private LinkedHashMap<String, User> parseUsers(Path path, String logicalName) throws AppDataException {
        LinkedHashMap<String, User> users = new LinkedHashMap<>();
        List<String> lines = readLines(path, logicalName);
        for (int index = 0; index < lines.size(); index++) {
            int lineNumber = index + 1;
            List<String> fields = splitFields(logicalName, lineNumber, lines.get(index), 7);
            if (!"USER".equals(fields.get(0))) {
                throw new AppDataException(logicalName, lineNumber, "레코드 접두어는 USER여야 합니다.");
            }
            String userId = decodeTextField(fields.get(1), logicalName, lineNumber, "userId");
            String password = decodeTextField(fields.get(2), logicalName, lineNumber, "password");
            String name = decodeTextField(fields.get(3), logicalName, lineNumber, "name");
            Role role = Role.fromFile(fields.get(4), logicalName, lineNumber);
            int penalty = parseNonNegativeInteger(fields.get(5), logicalName, lineNumber, "penalty");
            UserStatus status = UserStatus.fromFile(fields.get(6), logicalName, lineNumber);

            if (!USER_ID_PATTERN.matcher(userId).matches()) {
                throw new AppDataException(logicalName, lineNumber, "userId 문법이 올바르지 않습니다.");
            }
            if (!PASSWORD_PATTERN.matcher(password).matches()) {
                throw new AppDataException(logicalName, lineNumber, "password 문법이 올바르지 않습니다.");
            }
            if (!NAME_PATTERN.matcher(name).matches()) {
                throw new AppDataException(logicalName, lineNumber, "name 문법이 올바르지 않습니다.");
            }
            if (penalty > 999) {
                throw new AppDataException(logicalName, lineNumber, "penalty 범위가 올바르지 않습니다.");
            }
            User previous = users.put(userId, new User(userId, password, name, role, penalty, status, lineNumber));
            if (previous != null) {
                throw new AppDataException(logicalName, lineNumber, "중복된 userId가 존재합니다.");
            }
        }
        return users;
    }

    private LinkedHashMap<String, Room> parseRooms(Path path, String logicalName) throws AppDataException {
        LinkedHashMap<String, Room> rooms = new LinkedHashMap<>();
        List<String> lines = readLines(path, logicalName);
        for (int index = 0; index < lines.size(); index++) {
            int lineNumber = index + 1;
            List<String> fields = splitFields(logicalName, lineNumber, lines.get(index), 7);
            if (!"ROOM".equals(fields.get(0))) {
                throw new AppDataException(logicalName, lineNumber, "레코드 접두어는 ROOM이어야 합니다.");
            }
            String roomId = rawStructuredField(fields.get(1), logicalName, lineNumber, "roomId");
            int capacity = parseNonNegativeInteger(fields.get(2), logicalName, lineNumber, "capacity");
            String equipmentField = rawStructuredField(fields.get(3), logicalName, lineNumber, "equipmentList");
            RoomStatus status = RoomStatus.fromFile(fields.get(4), logicalName, lineNumber);
            LocalTime openTime = TimeFormats.parseFileTime(rawStructuredField(fields.get(5), logicalName, lineNumber, "openTime"), logicalName, lineNumber, "openTime");
            LocalTime closeTime = TimeFormats.parseFileTime(rawStructuredField(fields.get(6), logicalName, lineNumber, "closeTime"), logicalName, lineNumber, "closeTime");

            if (!ROOM_ID_PATTERN.matcher(roomId).matches()) {
                throw new AppDataException(logicalName, lineNumber, "roomId 문법이 올바르지 않습니다.");
            }
            if (capacity < 1 || capacity > 20) {
                throw new AppDataException(logicalName, lineNumber, "capacity 범위가 올바르지 않습니다.");
            }
            ensureHalfHourTime(openTime, logicalName, lineNumber, "openTime");
            ensureHalfHourTime(closeTime, logicalName, lineNumber, "closeTime");

            List<String> equipmentList = parseEquipmentList(equipmentField, logicalName, lineNumber);
            Room previous = rooms.put(roomId, new Room(roomId, capacity, equipmentList, status, openTime, closeTime, lineNumber));
            if (previous != null) {
                throw new AppDataException(logicalName, lineNumber, "중복된 roomId가 존재합니다.");
            }
        }
        return rooms;
    }

    private LinkedHashMap<String, Reservation> parseReservations(Path path, String logicalName) throws AppDataException {
        LinkedHashMap<String, Reservation> reservations = new LinkedHashMap<>();
        List<String> lines = readLines(path, logicalName);
        for (int index = 0; index < lines.size(); index++) {
            int lineNumber = index + 1;
            List<String> fields = splitFields(logicalName, lineNumber, lines.get(index), 10);
            if (!"RESV".equals(fields.get(0))) {
                throw new AppDataException(logicalName, lineNumber, "레코드 접두어는 RESV여야 합니다.");
            }
            String reservationId = rawStructuredField(fields.get(1), logicalName, lineNumber, "reservationId");
            String roomId = rawStructuredField(fields.get(2), logicalName, lineNumber, "roomId");
            String userId = rawStructuredField(fields.get(3), logicalName, lineNumber, "userId");
            LocalDate date = TimeFormats.parseFileDate(rawStructuredField(fields.get(4), logicalName, lineNumber, "date"), logicalName, lineNumber, "date");
            LocalTime startTime = TimeFormats.parseFileTime(rawStructuredField(fields.get(5), logicalName, lineNumber, "startTime"), logicalName, lineNumber, "startTime");
            LocalTime endTime = TimeFormats.parseFileTime(rawStructuredField(fields.get(6), logicalName, lineNumber, "endTime"), logicalName, lineNumber, "endTime");
            ReservationStatus status = ReservationStatus.fromFile(fields.get(7), logicalName, lineNumber);
            String checkedInAtRaw = rawStructuredField(fields.get(8), logicalName, lineNumber, "checkedInAt");
            int extensionCount = parseNonNegativeInteger(fields.get(9), logicalName, lineNumber, "extensionCount");

            if (!RESERVATION_ID_PATTERN.matcher(reservationId).matches()) {
                throw new AppDataException(logicalName, lineNumber, "reservationId 문법이 올바르지 않습니다.");
            }
            if (!ROOM_ID_PATTERN.matcher(roomId).matches()) {
                throw new AppDataException(logicalName, lineNumber, "roomId 문법이 올바르지 않습니다.");
            }
            if (!USER_ID_PATTERN.matcher(userId).matches()) {
                throw new AppDataException(logicalName, lineNumber, "userId 문법이 올바르지 않습니다.");
            }
            ensureHalfHourTime(startTime, logicalName, lineNumber, "startTime");
            ensureHalfHourTime(endTime, logicalName, lineNumber, "endTime");
            if (extensionCount < 0 || extensionCount > 1) {
                throw new AppDataException(logicalName, lineNumber, "extensionCount 값은 0 또는 1이어야 합니다.");
            }

            LocalDateTime checkedInAt = null;
            if (!"-".equals(checkedInAtRaw)) {
                checkedInAt = TimeFormats.parseFileDateTime(checkedInAtRaw, logicalName, lineNumber, "checkedInAt");
            }

            Reservation previous = reservations.put(reservationId, new Reservation(
                    reservationId,
                    roomId,
                    userId,
                    date,
                    startTime,
                    endTime,
                    status,
                    checkedInAt,
                    extensionCount,
                    lineNumber));
            if (previous != null) {
                throw new AppDataException(logicalName, lineNumber, "중복된 reservationId가 존재합니다.");
            }
        }
        return reservations;
    }

    private LocalDateTime parseSystemTime(Path path, String logicalName) throws AppDataException {
        List<String> lines = readLines(path, logicalName);
        if (lines.size() != 1) {
            throw new AppDataException(logicalName, 0, "NOW 레코드는 정확히 한 줄이어야 합니다.");
        }
        List<String> fields = splitFields(logicalName, 1, lines.get(0), 2);
        if (!"NOW".equals(fields.get(0))) {
            throw new AppDataException(logicalName, 1, "레코드 접두어는 NOW여야 합니다.");
        }
        return TimeFormats.parseFileDateTime(rawStructuredField(fields.get(1), logicalName, 1, "currentDateTime"), logicalName, 1, "currentDateTime");
    }

    private void validateUsers(Map<String, User> users) throws AppDataException {
        boolean hasAdmin = false;
        for (User user : users.values()) {
            if (!user.name.equals(user.name.strip())) {
                throw new AppDataException("users.txt", user.sourceLine, "name 앞뒤 공백은 허용되지 않습니다.");
            }
            if (user.name.contains("  ")) {
                throw new AppDataException("users.txt", user.sourceLine, "name에는 연속 공백 두 칸 이상을 사용할 수 없습니다.");
            }
            if (user.role == Role.ADMIN) {
                hasAdmin = true;
                if (user.penalty != 0) {
                    throw new AppDataException("users.txt", user.sourceLine, "admin 계정의 penalty는 0이어야 합니다.");
                }
            }
        }
        if (!hasAdmin) {
            throw new AppDataException("users.txt", 0, "최소 1개의 admin 계정이 필요합니다.");
        }
    }

    private void validateRooms(Map<String, Room> rooms) throws AppDataException {
        for (Room room : rooms.values()) {
            if (!room.openTime.isBefore(room.closeTime)) {
                throw new AppDataException("rooms.txt", room.sourceLine, "운영 시작 시각은 종료 시각보다 빨라야 합니다.");
            }
        }
    }

    private void validateReservations(SystemDataset dataset) throws AppDataException {
        Map<String, Integer> futureReservationCountByUser = new HashMap<>();
        List<Reservation> reservations = dataset.sortedReservations();
        for (Reservation reservation : reservations) {
            Room room = dataset.rooms.get(reservation.roomId);
            if (room == null) {
                throw new AppDataException("reservations.txt", reservation.sourceLine, "존재하지 않는 roomId를 참조합니다.");
            }
            User user = dataset.users.get(reservation.userId);
            if (user == null) {
                throw new AppDataException("reservations.txt", reservation.sourceLine, "존재하지 않는 userId를 참조합니다.");
            }
            if (!user.isMember()) {
                throw new AppDataException("reservations.txt", reservation.sourceLine, "admin 계정은 예약을 가질 수 없습니다.");
            }
            if (!reservation.startTime.isBefore(reservation.endTime)) {
                throw new AppDataException("reservations.txt", reservation.sourceLine, "예약 시작 시각은 종료 시각보다 빨라야 합니다.");
            }
            long duration = reservation.durationMinutes();
            if (duration % 30 != 0) {
                throw new AppDataException("reservations.txt", reservation.sourceLine, "예약 길이는 30분 단위여야 합니다.");
            }
            if (reservation.extensionCount == 0) {
                if (duration < 60 || duration > 240) {
                    throw new AppDataException("reservations.txt", reservation.sourceLine, "초기 예약 길이는 1시간 이상 4시간 이하이어야 합니다.");
                }
            } else {
                if (duration < 90 || duration > 270) {
                    throw new AppDataException("reservations.txt", reservation.sourceLine, "연장된 예약 길이는 1시간 30분 이상 4시간 30분 이하이어야 합니다.");
                }
                long initialDuration = duration - 30;
                if (initialDuration < 60 || initialDuration > 240) {
                    throw new AppDataException("reservations.txt", reservation.sourceLine, "연장 전 초기 예약 길이가 올바르지 않습니다.");
                }
                if (reservation.status != ReservationStatus.CHECKED_IN && reservation.status != ReservationStatus.COMPLETED) {
                    throw new AppDataException("reservations.txt", reservation.sourceLine, "연장된 예약은 CHECKED_IN 또는 COMPLETED 상태여야 합니다.");
                }
            }
            if (reservation.startTime.isBefore(room.openTime) || reservation.endTime.isAfter(room.closeTime)) {
                throw new AppDataException("reservations.txt", reservation.sourceLine, "예약 시각이 룸 운영 시간을 벗어납니다.");
            }

            if (reservation.status == ReservationStatus.CHECKED_IN || reservation.status == ReservationStatus.COMPLETED) {
                if (reservation.checkedInAt == null) {
                    throw new AppDataException("reservations.txt", reservation.sourceLine, "CHECKED_IN 또는 COMPLETED는 checkedInAt이 필요합니다.");
                }
                LocalDateTime windowStart = reservation.startDateTime().minusMinutes(10);
                LocalDateTime windowEnd = reservation.startDateTime().plusMinutes(15);
                if (reservation.checkedInAt.isBefore(windowStart) || reservation.checkedInAt.isAfter(windowEnd)) {
                    throw new AppDataException("reservations.txt", reservation.sourceLine, "checkedInAt이 허용된 체크인 구간을 벗어납니다.");
                }
            } else if (reservation.checkedInAt != null) {
                throw new AppDataException("reservations.txt", reservation.sourceLine, "현재 상태에서는 checkedInAt이 '-'여야 합니다.");
            }

            if (reservation.status == ReservationStatus.NO_SHOW && !dataset.currentTime.isAfter(reservation.startDateTime().plusMinutes(15))) {
                throw new AppDataException("reservations.txt", reservation.sourceLine, "NO_SHOW 상태 예약은 체크인 마감 시각 이후여야 합니다.");
            }
            if (reservation.status == ReservationStatus.COMPLETED && dataset.currentTime.isBefore(reservation.endDateTime())) {
                throw new AppDataException("reservations.txt", reservation.sourceLine, "COMPLETED 상태 예약은 종료 시각이 지난 뒤에만 허용됩니다.");
            }

            if (reservation.isFutureReserved(dataset.currentTime)) {
                int count = futureReservationCountByUser.getOrDefault(reservation.userId, 0) + 1;
                futureReservationCountByUser.put(reservation.userId, count);
                if (count > 2) {
                    throw new AppDataException("reservations.txt", reservation.sourceLine, "한 사용자의 미래 예약은 최대 2개까지 허용됩니다.");
                }
            }
        }

        for (int i = 0; i < reservations.size(); i++) {
            Reservation left = reservations.get(i);
            if (!left.isActiveForConflict()) {
                continue;
            }
            for (int j = i + 1; j < reservations.size(); j++) {
                Reservation right = reservations.get(j);
                if (!right.isActiveForConflict()) {
                    continue;
                }
                if (left.roomId.equals(right.roomId) && overlaps(left, right)) {
                    throw new AppDataException("reservations.txt", right.sourceLine, "같은 룸의 활성 예약 시간이 겹칩니다.");
                }
                if (left.userId.equals(right.userId) && overlaps(left, right)) {
                    throw new AppDataException("reservations.txt", right.sourceLine, "같은 사용자의 활성 예약 시간이 겹칩니다.");
                }
            }
        }
    }

    private void validateClosedRooms(SystemDataset dataset) throws AppDataException {
        for (Room room : dataset.rooms.values()) {
            if (room.status == RoomStatus.OPEN) {
                continue;
            }
            for (Reservation reservation : dataset.reservations.values()) {
                if (!reservation.roomId.equals(room.roomId)) {
                    continue;
                }
                if (!reservation.isActiveForConflict()) {
                    continue;
                }
                if (reservation.endDateTime().isAfter(dataset.currentTime)) {
                    throw new AppDataException("rooms.txt", room.sourceLine, "닫힌 룸 또는 점검 중 룸에 종료되지 않은 활성 예약이 존재합니다.");
                }
            }
        }
    }

    private List<String> readLines(Path path, String logicalName) throws AppDataException {
        if (!Files.exists(path)) {
            throw new AppDataException(logicalName, 0, "파일이 존재하지 않습니다.");
        }
        try {
            List<String> lines = Files.readAllLines(path, StandardCharsets.UTF_8);
            for (int index = 0; index < lines.size(); index++) {
                if (lines.get(index).isEmpty()) {
                    throw new AppDataException(logicalName, index + 1, "빈 줄은 허용되지 않습니다.");
                }
            }
            return lines;
        } catch (IOException e) {
            throw new AppDataException(logicalName, 0, "파일을 읽는 중 I/O 오류가 발생했습니다: " + e.getMessage());
        }
    }

    private List<String> splitFields(String fileName, int lineNumber, String line, int expectedFieldCount) throws AppDataException {
        List<String> fields = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        for (int index = 0; index < line.length(); index++) {
            char ch = line.charAt(index);
            if (ch == '|') {
                int backslashCount = 0;
                for (int cursor = index - 1; cursor >= 0 && line.charAt(cursor) == '\\'; cursor--) {
                    backslashCount++;
                }
                if (backslashCount % 2 == 0) {
                    fields.add(current.toString());
                    current.setLength(0);
                    continue;
                }
            }
            current.append(ch);
        }
        fields.add(current.toString());
        if (fields.size() != expectedFieldCount) {
            throw new AppDataException(fileName, lineNumber, "필드 수가 올바르지 않습니다.");
        }
        return fields;
    }

    private String decodeTextField(String raw, String fileName, int lineNumber, String fieldName) throws AppDataException {
        StringBuilder builder = new StringBuilder();
        for (int index = 0; index < raw.length(); index++) {
            char ch = raw.charAt(index);
            if (ch != '\\') {
                builder.append(ch);
                continue;
            }
            if (index + 1 >= raw.length()) {
                throw new AppDataException(fileName, lineNumber, fieldName + "의 이스케이프가 올바르지 않습니다.");
            }
            char next = raw.charAt(++index);
            switch (next) {
                case '|':
                    builder.append('|');
                    break;
                case '\\':
                    builder.append('\\');
                    break;
                case 'n':
                    builder.append('\n');
                    break;
                case 'r':
                    builder.append('\r');
                    break;
                default:
                    throw new AppDataException(fileName, lineNumber, fieldName + "의 이스케이프가 올바르지 않습니다.");
            }
        }
        return builder.toString();
    }

    private String rawStructuredField(String raw, String fileName, int lineNumber, String fieldName) throws AppDataException {
        if (raw.indexOf('\\') >= 0) {
            throw new AppDataException(fileName, lineNumber, fieldName + "에는 이스케이프를 사용할 수 없습니다.");
        }
        return raw;
    }

    private int parseNonNegativeInteger(String raw, String fileName, int lineNumber, String fieldName) throws AppDataException {
        String value = rawStructuredField(raw, fileName, lineNumber, fieldName);
        if (!NON_NEGATIVE_INTEGER_PATTERN.matcher(value).matches()) {
            throw new AppDataException(fileName, lineNumber, fieldName + "는 0 이상의 정수여야 합니다.");
        }
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            throw new AppDataException(fileName, lineNumber, fieldName + " 범위가 너무 큽니다.");
        }
    }

    private void ensureHalfHourTime(LocalTime value, String fileName, int lineNumber, String fieldName) throws AppDataException {
        int minute = value.getMinute();
        if (minute != 0 && minute != 30) {
            throw new AppDataException(fileName, lineNumber, fieldName + "은 30분 단위여야 합니다.");
        }
    }

    private List<String> parseEquipmentList(String raw, String fileName, int lineNumber) throws AppDataException {
        if ("-".equals(raw)) {
            return new ArrayList<>();
        }
        String[] parts = raw.split("\\+");
        List<String> result = new ArrayList<>();
        Set<String> dedupe = new HashSet<>();
        for (String part : parts) {
            if (!EQUIPMENT_CODE_PATTERN.matcher(part).matches()) {
                throw new AppDataException(fileName, lineNumber, "equipmentList 문법이 올바르지 않습니다.");
            }
            if (!dedupe.add(part)) {
                throw new AppDataException(fileName, lineNumber, "equipmentList에 중복된 비품 코드가 있습니다.");
            }
            result.add(part);
        }
        return result;
    }

    private boolean overlaps(Reservation left, Reservation right) {
        return left.startDateTime().isBefore(right.endDateTime()) && right.startDateTime().isBefore(left.endDateTime());
    }

    private String serializeUsers(SystemDataset dataset) {
        List<String> lines = new ArrayList<>();
        for (User user : dataset.sortedUsers()) {
            lines.add(user.toRecord());
        }
        return String.join("\n", lines);
    }

    private String serializeRooms(SystemDataset dataset) {
        List<String> lines = new ArrayList<>();
        for (Room room : dataset.sortedRooms()) {
            lines.add(room.toRecord());
        }
        return String.join("\n", lines);
    }

    private String serializeReservations(SystemDataset dataset) {
        List<String> lines = new ArrayList<>();
        for (Reservation reservation : dataset.sortedReservations()) {
            lines.add(reservation.toRecord());
        }
        return String.join("\n", lines);
    }

    private String serializeSystemTime(SystemDataset dataset) {
        return "NOW|" + TimeFormats.formatFileDateTime(dataset.currentTime);
    }

    static void moveReplacing(Path source, Path target) throws IOException {
        try {
            Files.move(source, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (AtomicMoveNotSupportedException e) {
            Files.move(source, target, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    private Path backupOriginal(Path source, String prefix) throws IOException {
        Path backup = Files.createTempFile(rootDirectory, prefix, ".bak");
        Files.copy(source, backup, StandardCopyOption.REPLACE_EXISTING);
        return backup;
    }

    private void replaceAll(PendingSaveFile... files) throws IOException {
        List<PendingSaveFile> replacedFiles = new ArrayList<>();
        try {
            for (PendingSaveFile file : files) {
                fileMover.move(file.tempFile, file.targetFile);
                replacedFiles.add(file);
            }
        } catch (IOException e) {
            rollback(replacedFiles, e);
            throw e;
        }
    }

    private void rollback(List<PendingSaveFile> replacedFiles, IOException originalFailure) throws IOException {
        IOException rollbackFailure = null;
        for (int index = replacedFiles.size() - 1; index >= 0; index--) {
            PendingSaveFile file = replacedFiles.get(index);
            try {
                moveReplacing(file.backupFile, file.targetFile);
            } catch (IOException e) {
                if (rollbackFailure == null) {
                    rollbackFailure = e;
                } else {
                    rollbackFailure.addSuppressed(e);
                }
            }
        }
        if (rollbackFailure != null) {
            originalFailure.addSuppressed(rollbackFailure);
            throw new IOException(originalFailure.getMessage() + " (롤백 실패)", originalFailure);
        }
    }

    private void deleteIfExists(Path path) {
        if (path == null) {
            return;
        }
        try {
            Files.deleteIfExists(path);
        } catch (IOException ignored) {
            // 임시 파일 정리 실패는 무시한다.
        }
    }

    static String escapeText(String value) {
        return value
                .replace("\\", "\\\\")
                .replace("|", "\\|")
                .replace("\n", "\\n")
                .replace("\r", "\\r");
    }

    private static final class PendingSaveFile {
        private final Path tempFile;
        private final Path targetFile;
        private final Path backupFile;

        private PendingSaveFile(Path tempFile, Path targetFile, Path backupFile) {
            this.tempFile = tempFile;
            this.targetFile = targetFile;
            this.backupFile = backupFile;
        }
    }
}
