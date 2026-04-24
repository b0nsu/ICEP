package ku.com;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.regex.Pattern;

final class TextDataStore {
    static final String USERS_FILE = "users.txt";
    static final String ROOMS_FILE = "rooms.txt";
    static final String RESERVATIONS_FILE = "reservations.txt";
    static final String SYSTEM_TIME_FILE = "system_time.txt";

    private static final Pattern USER_ID_PATTERN = Pattern.compile("^user[0-9]{3}$");
    private static final Pattern LOGIN_ID_PATTERN = Pattern.compile("^[A-Za-z][A-Za-z0-9_]{3,19}$");
    private static final Pattern USER_NAME_PATTERN = Pattern.compile("^[A-Za-z][A-Za-z0-9_]{3,19}$");
    private static final Pattern ROOM_ID_PATTERN = Pattern.compile("^R[0-9]{3}$");
    private static final Pattern RESERVATION_ID_PATTERN = Pattern.compile("^rv[0-9]{4}$");

    private final Path dataDir;
    private final Path usersPath;
    private final Path roomsPath;
    private final Path reservationsPath;
    private final Path systemTimePath;

    TextDataStore(Path projectRoot) {
        this.dataDir = projectRoot.resolve("data");
        this.usersPath = dataDir.resolve(USERS_FILE);
        this.roomsPath = dataDir.resolve(ROOMS_FILE);
        this.reservationsPath = dataDir.resolve(RESERVATIONS_FILE);
        this.systemTimePath = dataDir.resolve(SYSTEM_TIME_FILE);
    }

    void ensureDataFiles() throws AppDataException {
        try {
            if (!Files.exists(dataDir)) {
                Files.createDirectories(dataDir);
            }
        } catch (IOException e) {
            throw new AppDataException("data", 0, "data 폴더 생성에 실패했습니다: " + e.getMessage());
        }

        createIfMissing(usersPath, USERS_FILE, defaultUsers());
        createIfMissing(roomsPath, ROOMS_FILE, defaultRooms());
        createIfMissing(reservationsPath, RESERVATIONS_FILE, "");
        createIfMissing(systemTimePath, SYSTEM_TIME_FILE, "NOW|2026-03-20 09:00\n");
    }

    SystemData loadAll() throws AppDataException {
        ensureDataFiles();
        LinkedHashMap<String, User> users = parseUsers();
        LinkedHashMap<String, Room> rooms = parseRooms();
        LinkedHashMap<String, Reservation> reservations = parseReservations();
        LocalDateTime currentTime = parseSystemTime();

        SystemData data = new SystemData(users, rooms, reservations, currentTime);
        validateSemantics(data);
        return data;
    }

    void saveAll(SystemData data) throws AppDataException {
        writeFile(usersPath, USERS_FILE, serializeUsers(data));
        writeFile(roomsPath, ROOMS_FILE, serializeRooms(data));
        writeFile(reservationsPath, RESERVATIONS_FILE, serializeReservations(data));
        writeFile(systemTimePath, SYSTEM_TIME_FILE, "NOW|" + TimeFormats.formatDateTime(data.currentTime) + "\n");
    }

    private LinkedHashMap<String, User> parseUsers() throws AppDataException {
        List<LineRecord> lines = readLogicalLines(usersPath, USERS_FILE);
        LinkedHashMap<String, User> users = new LinkedHashMap<>();
        LinkedHashMap<String, User> usersByLoginId = new LinkedHashMap<>();
        for (LineRecord line : lines) {
            String[] fields = splitFields(line, USERS_FILE, 6);
            require("USER".equals(fields[0]), USERS_FILE, line.lineNumber, "레코드 접두어는 USER여야 합니다.");
            String userId = fields[1].trim();
            String loginId = fields[2].trim();
            String password = fields[3].trim();
            String userName = fields[4].trim();
            Role role = Role.fromFile(fields[5], USERS_FILE, line.lineNumber);

            require(USER_ID_PATTERN.matcher(userId).matches(), USERS_FILE, line.lineNumber,
                    "userId 형식이 올바르지 않습니다.");
            require(LOGIN_ID_PATTERN.matcher(loginId).matches(), USERS_FILE, line.lineNumber,
                    "loginId 형식이 올바르지 않습니다.");
            require(password.length() >= 4 && password.length() <= 20, USERS_FILE, line.lineNumber,
                    "password 길이는 4~20이어야 합니다.");
            validateNoPipeOrNewline(loginId, USERS_FILE, line.lineNumber, "loginId");
            validateNoPipeOrNewline(password, USERS_FILE, line.lineNumber, "password");
            require(USER_NAME_PATTERN.matcher(userName).matches(), USERS_FILE, line.lineNumber,
                    "userName 형식이 올바르지 않습니다.");
            validateNoPipeOrNewline(userName, USERS_FILE, line.lineNumber, "userName");

            User parsed = new User(userId, loginId, password, userName, role, line.lineNumber);
            User prev = users.put(userId, parsed);
            require(prev == null, USERS_FILE, line.lineNumber, "중복된 userId가 존재합니다.");
            User byLoginId = usersByLoginId.put(loginId, parsed);
            require(byLoginId == null, USERS_FILE, line.lineNumber, "중복된 loginId가 존재합니다.");
        }
        return users;
    }

    private LinkedHashMap<String, Room> parseRooms() throws AppDataException {
        List<LineRecord> lines = readLogicalLines(roomsPath, ROOMS_FILE);
        LinkedHashMap<String, Room> rooms = new LinkedHashMap<>();
        for (LineRecord line : lines) {
            String[] fields = splitFields(line, ROOMS_FILE, 5);
            require("ROOM".equals(fields[0]), ROOMS_FILE, line.lineNumber, "레코드 접두어는 ROOM이어야 합니다.");
            String roomId = fields[1].trim();
            String roomName = fields[2].trim();
            int maxCapacity = parseInt(fields[3], ROOMS_FILE, line.lineNumber, "maxCapacity");
            RoomStatus roomStatus = RoomStatus.fromFile(fields[4], ROOMS_FILE, line.lineNumber);

            require(ROOM_ID_PATTERN.matcher(roomId).matches(), ROOMS_FILE, line.lineNumber,
                    "roomId 형식이 올바르지 않습니다.");
            require(!roomName.isEmpty(), ROOMS_FILE, line.lineNumber, "roomName은 비어 있을 수 없습니다.");
            validateNoPipeOrNewline(roomName, ROOMS_FILE, line.lineNumber, "roomName");
            require(maxCapacity >= 1, ROOMS_FILE, line.lineNumber, "maxCapacity는 1 이상이어야 합니다.");

            Room prev = rooms.put(roomId, new Room(roomId, roomName, maxCapacity, roomStatus, line.lineNumber));
            require(prev == null, ROOMS_FILE, line.lineNumber, "중복된 roomId가 존재합니다.");
        }
        return rooms;
    }

    private LinkedHashMap<String, Reservation> parseReservations() throws AppDataException {
        List<LineRecord> lines = readLogicalLines(reservationsPath, RESERVATIONS_FILE);
        LinkedHashMap<String, Reservation> reservations = new LinkedHashMap<>();
        for (LineRecord line : lines) {
            String[] fields = splitFields(line, RESERVATIONS_FILE, 11);
            require("RESV".equals(fields[0]), RESERVATIONS_FILE, line.lineNumber, "레코드 접두어는 RESV여야 합니다.");
            String reservationId = fields[1].trim();
            String userId = fields[2].trim();
            String roomId = fields[3].trim();
            LocalDate date = TimeFormats.parseDate(fields[4], RESERVATIONS_FILE, line.lineNumber, "date");
            LocalTime startTime = TimeFormats.parseTime(fields[5], RESERVATIONS_FILE, line.lineNumber, "startTime");
            LocalTime endTime = TimeFormats.parseTime(fields[6], RESERVATIONS_FILE, line.lineNumber, "endTime");
            int partySize = parseInt(fields[7], RESERVATIONS_FILE, line.lineNumber, "partySize");
            ReservationStatus status = ReservationStatus.fromFile(fields[8], RESERVATIONS_FILE, line.lineNumber);
            LocalDateTime createdAt = TimeFormats.parseDateTime(fields[9], RESERVATIONS_FILE, line.lineNumber, "createdAt");
            String checkedInRaw = fields[10].trim();

            require(RESERVATION_ID_PATTERN.matcher(reservationId).matches(), RESERVATIONS_FILE, line.lineNumber,
                    "reservationId 형식이 올바르지 않습니다.");
            require(USER_ID_PATTERN.matcher(userId).matches(), RESERVATIONS_FILE, line.lineNumber,
                    "userId 형식이 올바르지 않습니다.");
            require(ROOM_ID_PATTERN.matcher(roomId).matches(), RESERVATIONS_FILE, line.lineNumber,
                    "roomId 형식이 올바르지 않습니다.");
            require(partySize >= 1, RESERVATIONS_FILE, line.lineNumber, "partySize는 1 이상이어야 합니다.");
            require(startTime.getMinute() == 0 && endTime.getMinute() == 0,
                    RESERVATIONS_FILE, line.lineNumber, "startTime/endTime은 1시간 단위여야 합니다.");
            require(startTime.isBefore(endTime), RESERVATIONS_FILE, line.lineNumber,
                    "startTime은 endTime보다 빨라야 합니다.");

            long minutes = Duration.between(LocalDateTime.of(date, startTime), LocalDateTime.of(date, endTime)).toMinutes();
            require(minutes == 60 || minutes == 120 || minutes == 180 || minutes == 240,
                    RESERVATIONS_FILE, line.lineNumber, "예약 길이는 1시간, 2시간, 3시간, 4시간 중 하나여야 합니다.");

            LocalDateTime checkedInAt = null;
            if (status == ReservationStatus.CHECKED_IN || status == ReservationStatus.COMPLETED) {
                require(!"-".equals(checkedInRaw), RESERVATIONS_FILE, line.lineNumber,
                        "CHECKED_IN/COMPLETED 상태는 checkedInAt 값이 필요합니다.");
                checkedInAt = TimeFormats.parseDateTime(checkedInRaw, RESERVATIONS_FILE, line.lineNumber, "checkedInAt");
            } else {
                require("-".equals(checkedInRaw), RESERVATIONS_FILE, line.lineNumber,
                        "RESERVED/NO_SHOW 상태는 checkedInAt이 '-' 이어야 합니다.");
            }

            Reservation prev = reservations.put(reservationId, new Reservation(
                    reservationId,
                    userId,
                    roomId,
                    date,
                    startTime,
                    endTime,
                    partySize,
                    status,
                    createdAt,
                    checkedInAt,
                    line.lineNumber));
            require(prev == null, RESERVATIONS_FILE, line.lineNumber, "중복된 reservationId가 존재합니다.");
        }
        return reservations;
    }

    private LocalDateTime parseSystemTime() throws AppDataException {
        List<LineRecord> lines = readLogicalLines(systemTimePath, SYSTEM_TIME_FILE);
        require(lines.size() == 1, SYSTEM_TIME_FILE, 0, "NOW 레코드는 정확히 1개여야 합니다.");
        LineRecord line = lines.get(0);
        String[] fields = splitFields(line, SYSTEM_TIME_FILE, 2);
        require("NOW".equals(fields[0]), SYSTEM_TIME_FILE, line.lineNumber, "레코드 접두어는 NOW여야 합니다.");
        return TimeFormats.parseDateTime(fields[1], SYSTEM_TIME_FILE, line.lineNumber, "NOW");
    }

    private void validateSemantics(SystemData data) throws AppDataException {
        boolean hasAdmin = false;
        for (User user : data.users.values()) {
            if (user.role == Role.ADMIN) {
                hasAdmin = true;
            }
        }
        require(hasAdmin, USERS_FILE, 0, "최소 1명의 admin 계정이 필요합니다.");

        List<Reservation> reservations = data.sortedReservations();
        for (Reservation reservation : reservations) {
            User user = data.users.get(reservation.userId);
            Room room = data.rooms.get(reservation.roomId);
            require(user != null, RESERVATIONS_FILE, reservation.sourceLine,
                    "존재하지 않는 userId를 참조합니다: " + reservation.userId);
            require(user.role == Role.MEMBER, RESERVATIONS_FILE, reservation.sourceLine,
                    "reservation은 member 사용자만 참조할 수 있습니다.");
            require(room != null, RESERVATIONS_FILE, reservation.sourceLine,
                    "존재하지 않는 roomId를 참조합니다: " + reservation.roomId);
            if (reservation.activeForOverlap()) {
                require(reservation.partySize <= room.maxCapacity, RESERVATIONS_FILE, reservation.sourceLine,
                        "partySize가 해당 room의 maxCapacity를 초과합니다.");
            }
        }

        for (int i = 0; i < reservations.size(); i++) {
            Reservation left = reservations.get(i);
            if (!left.activeForOverlap()) {
                continue;
            }
            for (int j = i + 1; j < reservations.size(); j++) {
                Reservation right = reservations.get(j);
                if (!right.activeForOverlap()) {
                    continue;
                }
                if (left.roomId.equals(right.roomId) && left.overlaps(right.startDateTime(), right.endDateTime())) {
                    throw new AppDataException(RESERVATIONS_FILE, right.sourceLine,
                            "같은 룸의 겹치는 시간대 예약이 존재합니다.");
                }
                if (left.userId.equals(right.userId) && left.overlaps(right.startDateTime(), right.endDateTime())) {
                    throw new AppDataException(RESERVATIONS_FILE, right.sourceLine,
                            "같은 사용자의 겹치는 시간대 예약이 존재합니다.");
                }
            }
        }
    }

    private void createIfMissing(Path path, String logicalName, String defaultContent) throws AppDataException {
        if (Files.exists(path)) {
            return;
        }
        try {
            Files.writeString(path, defaultContent, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new AppDataException(logicalName, 0, "기본 파일 생성에 실패했습니다: " + e.getMessage());
        }
    }

    private List<LineRecord> readLogicalLines(Path path, String logicalName) throws AppDataException {
        try {
            List<String> rawLines = Files.readAllLines(path, StandardCharsets.UTF_8);
            List<LineRecord> lines = new ArrayList<>();
            for (int i = 0; i < rawLines.size(); i++) {
                String line = rawLines.get(i);
                String trimmed = line.trim();
                if (trimmed.isEmpty() || trimmed.startsWith("#")) {
                    continue;
                }
                lines.add(new LineRecord(i + 1, trimmed));
            }
            return lines;
        } catch (IOException e) {
            throw new AppDataException(logicalName, 0, "파일 읽기 중 오류가 발생했습니다: " + e.getMessage());
        }
    }

    private String[] splitFields(LineRecord line, String fileName, int expected) throws AppDataException {
        String[] fields = line.content.split("\\|", -1);
        if (fields.length != expected) {
            throw new AppDataException(fileName, line.lineNumber, "필드 개수가 올바르지 않습니다.");
        }
        return fields;
    }

    private int parseInt(String raw, String fileName, int lineNumber, String fieldName) throws AppDataException {
        try {
            return Integer.parseInt(raw.trim());
        } catch (NumberFormatException e) {
            throw new AppDataException(fileName, lineNumber, fieldName + " 값은 정수여야 합니다.");
        }
    }

    private void validateNoPipeOrNewline(String value, String fileName, int lineNumber, String fieldName) throws AppDataException {
        if (value.indexOf('|') >= 0 || value.indexOf('\n') >= 0 || value.indexOf('\r') >= 0) {
            throw new AppDataException(fileName, lineNumber, fieldName + "에 사용할 수 없는 문자가 포함되어 있습니다.");
        }
    }

    private String serializeUsers(SystemData data) {
        StringBuilder builder = new StringBuilder();
        List<User> users = new ArrayList<>(data.users.values());
        users.sort((a, b) -> a.userId.compareTo(b.userId));
        for (User user : users) {
            builder.append(user.toRecord()).append('\n');
        }
        return builder.toString();
    }

    private String serializeRooms(SystemData data) {
        StringBuilder builder = new StringBuilder();
        for (Room room : data.sortedRooms()) {
            builder.append(room.toRecord()).append('\n');
        }
        return builder.toString();
    }

    private String serializeReservations(SystemData data) {
        StringBuilder builder = new StringBuilder();
        for (Reservation reservation : data.sortedReservations()) {
            builder.append(reservation.toRecord()).append('\n');
        }
        return builder.toString();
    }

    private void writeFile(Path path, String logicalName, String content) throws AppDataException {
        try {
            Files.writeString(path, content, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new AppDataException(logicalName, 0, "파일 저장 중 오류가 발생했습니다: " + e.getMessage());
        }
    }

    private void require(boolean condition, String fileName, int lineNumber, String message) throws AppDataException {
        if (!condition) {
            throw new AppDataException(fileName, lineNumber, message);
        }
    }

    private String defaultUsers() {
        return "USER|user001|admin|admin1234|admin|admin\n";
    }

    private String defaultRooms() {
        return "ROOM|R101|A룸|4|OPEN\n"
                + "ROOM|R102|B룸|6|OPEN\n"
                + "ROOM|R103|C룸|8|OPEN\n";
    }

    private record LineRecord(int lineNumber, String content) {
    }
}
