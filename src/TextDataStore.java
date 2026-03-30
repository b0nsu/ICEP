import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Comparator;
import java.util.Map;
import java.util.regex.Pattern;

final class TextDataStore {
    static final String USERS_FILE = "users.txt";
    static final String ROOMS_FILE = "rooms.txt";
    static final String RESERVATIONS_FILE = "reservations.txt";
    static final String SYSTEM_TIME_FILE = "system_time.txt";

    private static final Pattern USER_ID_PATTERN = Pattern.compile("^[a-z][a-z0-9]{3,11}$");
    private static final Pattern PASSWORD_PATTERN = Pattern.compile("^[A-Za-z0-9!@#$%^&*._-]{6,16}$");
    private static final Pattern USER_NAME_PATTERN = Pattern.compile("^[A-Za-z가-힣 ]{2,20}$");
    private static final Pattern ROOM_ID_PATTERN = Pattern.compile("^R[0-9]{3}$");
    private static final Pattern RESERVATION_ID_PATTERN = Pattern.compile("^rv[0-9]{4,}$");
    private static final Pattern ROOM_STATUS_PATTERN = Pattern.compile("^(OPEN|CLOSED|MAINTENANCE)$");
    private static final Pattern EQUIPMENT_CODE_PATTERN = Pattern.compile("^[A-Z][A-Z0-9_]*$");

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
        createIfMissing(systemTimePath, SYSTEM_TIME_FILE, "NOW|2026-03-20T09:00\n");
    }

    SystemData loadAll() throws AppDataException {
        ensureDataFiles();
        verifyAccessible(usersPath, USERS_FILE);
        verifyAccessible(roomsPath, ROOMS_FILE);
        verifyAccessible(reservationsPath, RESERVATIONS_FILE);
        verifyAccessible(systemTimePath, SYSTEM_TIME_FILE);

        LinkedHashMap<String, User> users = parseUsers();
        LinkedHashMap<String, Room> rooms = parseRooms();
        LinkedHashMap<String, Reservation> reservations = parseReservations();
        LocalDateTime currentTime = parseSystemTime();

        SystemData data = new SystemData(users, rooms, reservations, currentTime);
        validateSemantics(data);
        return data;
    }

    void saveAll(SystemData data) throws AppDataException {
        String users = serializeUsers(data);
        String rooms = serializeRooms(data);
        String reservations = serializeReservations(data);
        String systemTime = "NOW|" + TimeFormats.formatDateTime(data.currentTime) + "\n";

        Path usersTmp = usersPath.resolveSibling(USERS_FILE + ".tmp");
        Path roomsTmp = roomsPath.resolveSibling(ROOMS_FILE + ".tmp");
        Path reservationsTmp = reservationsPath.resolveSibling(RESERVATIONS_FILE + ".tmp");
        Path systemTimeTmp = systemTimePath.resolveSibling(SYSTEM_TIME_FILE + ".tmp");

        try {
            writeFile(usersTmp, users);
            writeFile(roomsTmp, rooms);
            writeFile(reservationsTmp, reservations);
            writeFile(systemTimeTmp, systemTime);

            // 임시 파일 재검증
            verifyTempFiles(usersTmp, roomsTmp, reservationsTmp, systemTimeTmp);

            atomicReplace(usersTmp, usersPath);
            atomicReplace(roomsTmp, roomsPath);
            atomicReplace(reservationsTmp, reservationsPath);
            atomicReplace(systemTimeTmp, systemTimePath);
        } catch (IOException e) {
            throw new AppDataException("파일 저장", 0, "파일 저장 중 오류가 발생했습니다: " + e.getMessage());
        } finally {
            deleteQuietly(usersTmp);
            deleteQuietly(roomsTmp);
            deleteQuietly(reservationsTmp);
            deleteQuietly(systemTimeTmp);
        }
    }

    private void verifyTempFiles(Path usersTmp,
                                Path roomsTmp,
                                Path reservationsTmp,
                                Path systemTimeTmp) throws AppDataException {
        try {
            TextDataStore verifier = new TextDataStore(dataDir.getParent());
            LinkedHashMap<String, User> users = verifier.parseUsers(usersTmp);
            LinkedHashMap<String, Room> rooms = verifier.parseRooms(roomsTmp);
            LinkedHashMap<String, Reservation> reservations = verifier.parseReservations(reservationsTmp);
            LocalDateTime currentTime = verifier.parseSystemTime(systemTimeTmp);
            SystemData data = new SystemData(users, rooms, reservations, currentTime);
            verifier.validateSemantics(data);
        } catch (AppDataException e) {
            throw e;
        }
    }

    private LinkedHashMap<String, User> parseUsers() throws AppDataException {
        return parseUsers(usersPath);
    }

    private LinkedHashMap<String, User> parseUsers(Path path) throws AppDataException {
        List<LineRecord> lines = readLogicalLines(path, USERS_FILE);
        LinkedHashMap<String, User> users = new LinkedHashMap<>();

        for (LineRecord line : lines) {
            String[] fields = splitFields(line, USERS_FILE, 7);
            require("USER".equals(fields[0]), USERS_FILE, line.lineNumber, "레코드 접두어는 USER여야 합니다.");

            ensureNoOuterSpace(fields[0], USERS_FILE, line.lineNumber, "USER");
            requireNoEscape(fields[1], USERS_FILE, line.lineNumber, "userId");
            requireNoEscape(fields[2], USERS_FILE, line.lineNumber, "password");
            requireNoEscape(fields[4], USERS_FILE, line.lineNumber, "role");
            requireNoEscape(fields[5], USERS_FILE, line.lineNumber, "penalty");
            requireNoEscape(fields[6], USERS_FILE, line.lineNumber, "status");

            String userId = fields[1];
            String password = unescapeText(fields[2], USERS_FILE, line.lineNumber, "password");
            String name = unescapeText(fields[3], USERS_FILE, line.lineNumber, "name");
            Role role = Role.fromFile(fields[4], USERS_FILE, line.lineNumber);
            UserStatus status = UserStatus.fromFile(fields[6], USERS_FILE, line.lineNumber);

            ensureNoOuterSpace(userId, USERS_FILE, line.lineNumber, "userId");
            ensureNoOuterSpace(fields[2], USERS_FILE, line.lineNumber, "password");
            ensureNoOuterSpace(fields[3], USERS_FILE, line.lineNumber, "name");
            ensureNoOuterSpace(fields[4], USERS_FILE, line.lineNumber, "role");
            ensureNoOuterSpace(fields[5], USERS_FILE, line.lineNumber, "penalty");
            ensureNoOuterSpace(fields[6], USERS_FILE, line.lineNumber, "status");

            require(USER_ID_PATTERN.matcher(userId).matches(), USERS_FILE, line.lineNumber,
                    "userId 형식이 올바르지 않습니다.");
            require(PASSWORD_PATTERN.matcher(password).matches(), USERS_FILE, line.lineNumber,
                    "password 형식이 올바르지 않습니다.");
            require(USER_NAME_PATTERN.matcher(name).matches(), USERS_FILE, line.lineNumber,
                    "name 형식이 올바르지 않습니다.");
            require(!name.contains("  "), USERS_FILE, line.lineNumber, "name에 연속된 공백이 있습니다.");
            require(!name.startsWith(" ") && !name.endsWith(" "), USERS_FILE, line.lineNumber,
                    "name 앞뒤 공백은 허용되지 않습니다.");

            int penalty = parseInt(fields[5], USERS_FILE, line.lineNumber, "penalty");
            require(penalty >= 0 && penalty <= 999, USERS_FILE, line.lineNumber,
                    "penalty는 0~999 사이여야 합니다.");

            User prev = users.put(userId, new User(userId, password, name, role, penalty, status, line.lineNumber));
            require(prev == null, USERS_FILE, line.lineNumber, "중복된 userId가 존재합니다.");
        }

        return users;
    }

    private LinkedHashMap<String, Room> parseRooms() throws AppDataException {
        return parseRooms(roomsPath);
    }

    private LinkedHashMap<String, Room> parseRooms(Path path) throws AppDataException {
        List<LineRecord> lines = readLogicalLines(path, ROOMS_FILE);
        LinkedHashMap<String, Room> rooms = new LinkedHashMap<>();

        for (LineRecord line : lines) {
            String[] fields = splitFields(line, ROOMS_FILE, 7);
            require("ROOM".equals(fields[0]), ROOMS_FILE, line.lineNumber, "레코드 접두어는 ROOM여야 합니다.");

            ensureNoOuterSpace(fields[1], ROOMS_FILE, line.lineNumber, "roomId");
            ensureNoOuterSpace(fields[2], ROOMS_FILE, line.lineNumber, "capacity");
            ensureNoOuterSpace(fields[3], ROOMS_FILE, line.lineNumber, "equipmentList");
            ensureNoOuterSpace(fields[4], ROOMS_FILE, line.lineNumber, "status");
            ensureNoOuterSpace(fields[5], ROOMS_FILE, line.lineNumber, "openTime");
            ensureNoOuterSpace(fields[6], ROOMS_FILE, line.lineNumber, "closeTime");

            String roomId = fields[1];
            String equipmentList = fields[3];
            RoomStatus roomStatus = RoomStatus.fromFile(fields[4], ROOMS_FILE, line.lineNumber);
            LocalTime openTime = TimeFormats.parseTime(fields[5], ROOMS_FILE, line.lineNumber, "openTime");
            LocalTime closeTime = TimeFormats.parseTime(fields[6], ROOMS_FILE, line.lineNumber, "closeTime");

            require(ROOM_ID_PATTERN.matcher(roomId).matches(), ROOMS_FILE, line.lineNumber,
                    "roomId 형식이 올바르지 않습니다.");
            require(ROOM_STATUS_PATTERN.matcher(roomStatus.name()).matches(), ROOMS_FILE, line.lineNumber,
                    "status 값이 올바르지 않습니다.");
            require(openTime.getMinute() == 0 || openTime.getMinute() == 30, ROOMS_FILE, line.lineNumber,
                    "openTime 분 단위는 00 또는 30이어야 합니다.");
            require(closeTime.getMinute() == 0 || closeTime.getMinute() == 30, ROOMS_FILE, line.lineNumber,
                    "closeTime 분 단위는 00 또는 30이어야 합니다.");
            require(openTime.isBefore(closeTime), ROOMS_FILE, line.lineNumber,
                    "openTime은 closeTime보다 빨라야 합니다.");

            if (!"-".equals(equipmentList)) {
                String[] codes = equipmentList.split("\\+", -1);
                require(codes.length > 0, ROOMS_FILE, line.lineNumber, "equipmentList 형식이 올바르지 않습니다.");
                Map<String, Boolean> unique = new LinkedHashMap<>();
                for (String code : codes) {
                    require(!code.isEmpty(), ROOMS_FILE, line.lineNumber, "equipmentList 형식이 올바르지 않습니다.");
                    require(EQUIPMENT_CODE_PATTERN.matcher(code).matches(), ROOMS_FILE, line.lineNumber,
                            "equipment code 형식이 올바르지 않습니다.");
                    require(!unique.containsKey(code), ROOMS_FILE, line.lineNumber, "equipmentList에 중복 코드가 있습니다.");
                    unique.put(code, Boolean.TRUE);
                }
            }

            int capacity = parseInt(fields[2], ROOMS_FILE, line.lineNumber, "capacity");
            require(capacity >= 1 && capacity <= 20, ROOMS_FILE, line.lineNumber,
                    "capacity는 1~20 사이여야 합니다.");

            Room prev = rooms.put(roomId, new Room(roomId,
                    capacity,
                    equipmentList,
                    roomStatus,
                    openTime,
                    closeTime,
                    line.lineNumber));
            require(prev == null, ROOMS_FILE, line.lineNumber, "중복된 roomId가 존재합니다.");
        }

        return rooms;
    }

    private LinkedHashMap<String, Reservation> parseReservations() throws AppDataException {
        return parseReservations(reservationsPath);
    }

    private LinkedHashMap<String, Reservation> parseReservations(Path path) throws AppDataException {
        List<LineRecord> lines = readLogicalLines(path, RESERVATIONS_FILE);
        LinkedHashMap<String, Reservation> reservations = new LinkedHashMap<>();

        for (LineRecord line : lines) {
            String[] fields = splitFields(line, RESERVATIONS_FILE, 10);
            require("RESV".equals(fields[0]), RESERVATIONS_FILE, line.lineNumber, "레코드 접두어는 RESV여야 합니다.");

            ensureNoOuterSpace(fields[1], RESERVATIONS_FILE, line.lineNumber, "reservationId");
            ensureNoOuterSpace(fields[2], RESERVATIONS_FILE, line.lineNumber, "roomId");
            ensureNoOuterSpace(fields[3], RESERVATIONS_FILE, line.lineNumber, "userId");
            ensureNoOuterSpace(fields[4], RESERVATIONS_FILE, line.lineNumber, "date");
            ensureNoOuterSpace(fields[5], RESERVATIONS_FILE, line.lineNumber, "startTime");
            ensureNoOuterSpace(fields[6], RESERVATIONS_FILE, line.lineNumber, "endTime");
            ensureNoOuterSpace(fields[7], RESERVATIONS_FILE, line.lineNumber, "status");
            ensureNoOuterSpace(fields[8], RESERVATIONS_FILE, line.lineNumber, "checkedInAt");
            ensureNoOuterSpace(fields[9], RESERVATIONS_FILE, line.lineNumber, "extensionCount");

            requireNoEscape(fields[2], RESERVATIONS_FILE, line.lineNumber, "roomId");
            requireNoEscape(fields[3], RESERVATIONS_FILE, line.lineNumber, "userId");

            String reservationId = fields[1];
            String roomId = fields[2];
            String userId = fields[3];
            LocalDate date = TimeFormats.parseDate(fields[4], RESERVATIONS_FILE, line.lineNumber, "date");
            LocalTime startTime = TimeFormats.parseTime(fields[5], RESERVATIONS_FILE, line.lineNumber, "startTime");
            LocalTime endTime = TimeFormats.parseTime(fields[6], RESERVATIONS_FILE, line.lineNumber, "endTime");
            ReservationStatus status = ReservationStatus.fromFile(fields[7], RESERVATIONS_FILE, line.lineNumber);
            String checkedInRaw = fields[8];
            int extensionCount = parseInt(fields[9], RESERVATIONS_FILE, line.lineNumber, "extensionCount");

            require(RESERVATION_ID_PATTERN.matcher(reservationId).matches(), RESERVATIONS_FILE, line.lineNumber,
                    "reservationId 형식이 올바르지 않습니다.");
            require(ROOM_ID_PATTERN.matcher(roomId).matches(), RESERVATIONS_FILE, line.lineNumber,
                    "roomId 형식이 올바르지 않습니다.");
            require(USER_ID_PATTERN.matcher(userId).matches(), RESERVATIONS_FILE, line.lineNumber,
                    "userId 형식이 올바르지 않습니다.");
            require(startTime.getMinute() == 0 || startTime.getMinute() == 30, RESERVATIONS_FILE, line.lineNumber,
                    "startTime 분 단위는 00 또는 30이어야 합니다.");
            require(endTime.getMinute() == 0 || endTime.getMinute() == 30, RESERVATIONS_FILE, line.lineNumber,
                    "endTime 분 단위는 00 또는 30이어야 합니다.");
            require(startTime.isBefore(endTime), RESERVATIONS_FILE, line.lineNumber,
                    "startTime은 endTime보다 빨라야 합니다.");

            long minutes = Duration.between(LocalDateTime.of(date, startTime), LocalDateTime.of(date, endTime)).toMinutes();
            if (extensionCount == 0) {
                require(minutes >= 60 && minutes <= 240 && minutes % 30 == 0, RESERVATIONS_FILE, line.lineNumber,
                        "예약 길이는 60분~240분이며 30분 단위여야 합니다.");
            } else if (extensionCount == 1) {
                long baseMinutes = minutes - 30;
                require(baseMinutes >= 60 && baseMinutes <= 240, RESERVATIONS_FILE, line.lineNumber,
                        "extensionCount=1인 예약은 기본 사용시간이 60분~240분 이어야 합니다.");
                require(minutes >= 90 && minutes <= 270, RESERVATIONS_FILE, line.lineNumber,
                        "extensionCount=1인 예약은 최종 사용시간이 90분~270분이어야 합니다.");
                require(minutes % 30 == 0 && baseMinutes % 30 == 0, RESERVATIONS_FILE, line.lineNumber,
                        "예약 길이는 30분 단위여야 합니다.");
            } else {
                throw new AppDataException(RESERVATIONS_FILE, line.lineNumber, "extensionCount는 0 또는 1이어야 합니다.");
            }

            LocalDateTime checkedInAt = null;
            if (status == ReservationStatus.CHECKED_IN || status == ReservationStatus.COMPLETED) {
                require(!"-".equals(checkedInRaw), RESERVATIONS_FILE, line.lineNumber,
                        "CHECKED_IN/COMPLETED는 checkedInAt이 필요합니다.");
                checkedInAt = TimeFormats.parseDateTime(checkedInRaw, RESERVATIONS_FILE, line.lineNumber, "checkedInAt");
                if (extensionCount == 0 && status == ReservationStatus.CHECKED_IN) {
                    // checkedIn is allowed with extensionCount 0 or 1; no constraint here
                }
            } else {
                require("-".equals(checkedInRaw), RESERVATIONS_FILE, line.lineNumber,
                        "RESERVED/CANCELLED/NO_SHOW는 checkedInAt이 '-'이어야 합니다.");
            }

            if (extensionCount == 1) {
                require(status == ReservationStatus.CHECKED_IN || status == ReservationStatus.COMPLETED,
                        RESERVATIONS_FILE,
                        line.lineNumber,
                        "extensionCount=1 상태에서는 CHECKED_IN 또는 COMPLETED만 허용됩니다.");
            }

            Reservation prev = reservations.put(reservationId,
                    new Reservation(reservationId,
                            roomId,
                            userId,
                            date,
                            startTime,
                            endTime,
                            status,
                            checkedInAt,
                            extensionCount,
                            line.lineNumber));
            require(prev == null, RESERVATIONS_FILE, line.lineNumber, "중복된 reservationId가 존재합니다.");
        }

        return reservations;
    }

    private LocalDateTime parseSystemTime() throws AppDataException {
        return parseSystemTime(systemTimePath);
    }

    private LocalDateTime parseSystemTime(Path path) throws AppDataException {
        List<LineRecord> lines = readLogicalLines(path, SYSTEM_TIME_FILE);
        require(lines.size() == 1, SYSTEM_TIME_FILE, 0, "NOW 레코드는 정확히 1개여야 합니다.");
        LineRecord line = lines.get(0);
        String[] fields = splitFields(line, SYSTEM_TIME_FILE, 2);
        require("NOW".equals(fields[0]), SYSTEM_TIME_FILE, line.lineNumber, "레코드 접두어는 NOW여야 합니다.");
        ensureNoOuterSpace(fields[1], SYSTEM_TIME_FILE, line.lineNumber, "currentDateTime");
        requireNoEscape(fields[1], SYSTEM_TIME_FILE, line.lineNumber, "currentDateTime");
        return TimeFormats.parseDateTime(fields[1], SYSTEM_TIME_FILE, line.lineNumber, "NOW");
    }

    private void validateSemantics(SystemData data) throws AppDataException {
        int adminCount = 0;
        for (User user : data.users.values()) {
            if (user.role == Role.ADMIN) {
                adminCount++;
                require(user.penalty == 0, USERS_FILE, user.sourceLine, "admin 계정의 penalty는 0이어야 합니다.");
            }
            require(user.status == UserStatus.ACTIVE, USERS_FILE, user.sourceLine, "사용자 상태는 ACTIVE여야 합니다.");
        }
        require(adminCount >= 1, USERS_FILE, 0, "최소 1명의 admin 계정이 필요합니다.");

        List<Reservation> reservations = data.sortedReservations();
        Map<String, Integer> futureReservationCountByUser = new LinkedHashMap<>();

        for (Reservation reservation : reservations) {
            User user = data.users.get(reservation.userId);
            Room room = data.rooms.get(reservation.roomId);
            require(user != null, RESERVATIONS_FILE, reservation.sourceLine,
                    "존재하지 않는 userId를 참조합니다: " + reservation.userId);
            require(room != null, RESERVATIONS_FILE, reservation.sourceLine,
                    "존재하지 않는 roomId를 참조합니다: " + reservation.roomId);
            require(user.role == Role.MEMBER, RESERVATIONS_FILE, reservation.sourceLine,
                    "member 계정의 예약만 허용됩니다.");

            require(!reservation.startTime.equals(reservation.endTime), RESERVATIONS_FILE, reservation.sourceLine,
                    "startTime과 endTime이 같습니다.");

            LocalDateTime startAt = reservation.startDateTime();
            LocalDateTime endAt = reservation.endDateTime();

            require(!endAt.toLocalTime().isBefore(room.openTime) && !endAt.toLocalTime().isAfter(room.closeTime),
                    RESERVATIONS_FILE, reservation.sourceLine,
                    "예약이 룸 운영 시간 범위를 벗어납니다.");
            require(!startAt.toLocalTime().isBefore(room.openTime), RESERVATIONS_FILE, reservation.sourceLine,
                    "예약이 룸 운영 시간 범위를 벗어납니다.");
            require(room.openTime.isBefore(room.closeTime), RESERVATIONS_FILE, reservation.sourceLine,
                    "룸 운영 시작 시간이 종료 시간보다 빨라야 합니다.");

            if (reservation.status == ReservationStatus.CHECKED_IN || reservation.status == ReservationStatus.COMPLETED) {
                if (reservation.checkedInAt == null) {
                    throw new AppDataException(RESERVATIONS_FILE, reservation.sourceLine, "checkedInAt이 누락되었습니다.");
                }
                LocalDateTime open = startAt.minusMinutes(10);
                LocalDateTime close = startAt.plusMinutes(15);
                require((!reservation.checkedInAt.isBefore(open)) && (!reservation.checkedInAt.isAfter(close)),
                        RESERVATIONS_FILE,
                        reservation.sourceLine,
                        "checkedInAt이 CHECK-IN 가능 범위를 벗어났습니다.");
            }

            if ((reservation.status == ReservationStatus.RESERVED || reservation.status == ReservationStatus.CHECKED_IN)
                    && reservation.endDateTime().isAfter(data.currentTime)
                    && room.roomStatus != RoomStatus.OPEN) {
                throw new AppDataException(RESERVATIONS_FILE, reservation.sourceLine,
                        "운영 중이 아닌 룸에 향후 예약/이용 내역이 존재합니다.");
            }

            if ((reservation.status == ReservationStatus.RESERVED || reservation.status == ReservationStatus.CHECKED_IN)
                    && reservation.startDateTime().isAfter(data.currentTime)) {
                if (reservation.status == ReservationStatus.RESERVED) {
                    futureReservationCountByUser.put(reservation.userId,
                            futureReservationCountByUser.getOrDefault(reservation.userId, 0) + 1);
                }
            }
        }

        for (Map.Entry<String, Integer> entry : futureReservationCountByUser.entrySet()) {
            require(entry.getValue() <= 2, RESERVATIONS_FILE, 0,
                    String.format("예약 제한 초과: %s 회원은 미래 예약이 2개를 초과했습니다.", entry.getKey()));
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
                if (left.overlaps(right.startDateTime(), right.endDateTime())
                        && left.roomId.equals(right.roomId)) {
                    throw new AppDataException(RESERVATIONS_FILE, right.sourceLine, "같은 룸의 겹치는 예약이 존재합니다.");
                }
                if (left.overlaps(right.startDateTime(), right.endDateTime())
                        && left.userId.equals(right.userId)) {
                    throw new AppDataException(RESERVATIONS_FILE, right.sourceLine, "같은 사용자의 겹치는 예약이 존재합니다.");
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

    private void verifyAccessible(Path path, String logicalName) throws AppDataException {
        if (!Files.exists(path)) {
            throw new AppDataException(logicalName, 0, "파일이 존재하지 않습니다.");
        }
        if (!Files.isReadable(path)) {
            throw new AppDataException(logicalName, 0, "파일 읽기 권한이 없습니다.");
        }
        if (!Files.isWritable(path)) {
            throw new AppDataException(logicalName, 0, "파일 쓰기 권한이 없습니다.");
        }
    }

    private List<LineRecord> readLogicalLines(Path path, String logicalName) throws AppDataException {
        try {
            List<String> rawLines = Files.readAllLines(path, StandardCharsets.UTF_8);
            List<LineRecord> lines = new ArrayList<>();
            for (int i = 0; i < rawLines.size(); i++) {
                String line = rawLines.get(i);
                String lineNumbered = line;
                if (lineNumbered.isEmpty()) {
                    throw new AppDataException(logicalName, i + 1, "빈 줄은 허용되지 않습니다.");
                }
                if (lineNumbered.startsWith("#")) {
                    throw new AppDataException(logicalName, i + 1, "주석 줄은 허용되지 않습니다.");
                }
                lines.add(new LineRecord(i + 1, lineNumbered));
            }
            return lines;
        } catch (IOException e) {
            throw new AppDataException(logicalName, 0, "파일 읽기 중 오류가 발생했습니다: " + e.getMessage());
        }
    }

    private String[] splitFields(LineRecord line, String fileName, int expected) throws AppDataException {
        List<String> fields = new ArrayList<>();
        StringBuilder token = new StringBuilder();
        boolean escape = false;

        for (int i = 0; i < line.content.length(); i++) {
            char ch = line.content.charAt(i);
            if (escape) {
                switch (ch) {
                    case '|':
                    case '\\':
                    case 'n':
                    case 'r':
                        token.append('\\').append(ch);
                        break;
                    default:
                        throw new AppDataException(fileName, line.lineNumber, "허용되지 않는 역슬래시 조합: \\" + ch);
                }
                escape = false;
                continue;
            }
            if (ch == '\\') {
                escape = true;
                continue;
            }
            if (ch == '|') {
                fields.add(token.toString());
                token.setLength(0);
                continue;
            }
            token.append(ch);
        }

        if (escape) {
            throw new AppDataException(fileName, line.lineNumber, "역슬래시로 끝나는 레코드는 허용되지 않습니다.");
        }
        fields.add(token.toString());

        if (fields.size() != expected) {
            throw new AppDataException(fileName, line.lineNumber, "필드 개수가 올바르지 않습니다.");
        }

        return fields.toArray(new String[0]);
    }

    private String unescapeText(String raw, String fileName, int lineNumber, String fieldName) throws AppDataException {
        StringBuilder builder = new StringBuilder();
        boolean escape = false;
        for (int i = 0; i < raw.length(); i++) {
            char ch = raw.charAt(i);
            if (escape) {
                switch (ch) {
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
                        throw new AppDataException(fileName, lineLineNumber(fileName, lineNumber, fieldName),
                                fieldName + "에 허용되지 않는 이스케이프가 있습니다.");
                }
                escape = false;
            } else if (ch == '\\') {
                escape = true;
            } else {
                builder.append(ch);
            }
        }
        if (escape) {
            throw new AppDataException(fileName, lineNumber, fieldName + "에 허용되지 않는 이스케이프가 있습니다.");
        }
        return builder.toString();
    }

    private void ensureNoOuterSpace(String raw, String fileName, int lineNumber, String fieldName) throws AppDataException {
        require(raw.equals(raw.strip()), fileName, lineNumber,
                fieldName + " 앞뒤 공백은 허용되지 않습니다.");
    }

    private void requireNoEscape(String raw, String fileName, int lineNumber, String fieldName) throws AppDataException {
        if (raw.contains("\\")) {
            throw new AppDataException(fileName, lineNumber, fieldName + "에 허용되지 않는 이스케이프 문자가 있습니다.");
        }
    }

    private int lineLineNumber(String fileName, int lineNumber, String fieldName) {
        return lineNumber;
    }

    private int parseInt(String raw, String fileName, int lineNumber, String fieldName) throws AppDataException {
        try {
            return Integer.parseInt(raw);
        } catch (NumberFormatException e) {
            throw new AppDataException(fileName, lineNumber, fieldName + " 값은 정수여야 합니다.");
        }
    }

    private void require(boolean condition, String fileName, int lineNumber, String message) throws AppDataException {
        if (!condition) {
            throw new AppDataException(fileName, lineNumber, message);
        }
    }

    private String serializeUsers(SystemData data) {
        StringBuilder builder = new StringBuilder();
        List<User> users = new ArrayList<>(data.users.values());
        users.sort(Comparator.comparing(u -> u.userId));
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

    private void writeFile(Path path, String content) throws IOException {
        Files.writeString(path, content, StandardCharsets.UTF_8);
    }

    private void atomicReplace(Path source, Path target) throws IOException {
        try {
            Files.move(source, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (AtomicMoveNotSupportedException e) {
            Files.move(source, target, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    private void deleteQuietly(Path path) {
        try {
            Files.deleteIfExists(path);
        } catch (IOException ignored) {
        }
    }

    private String defaultUsers() {
        return "USER|admin|admin1234|관리자|admin|0|ACTIVE\n";
    }

    private String defaultRooms() {
        return "ROOM|R101|4|-|OPEN|09:00|23:00\n"
                + "ROOM|R102|6|-|OPEN|09:00|23:00\n"
                + "ROOM|R103|8|-|OPEN|09:00|23:00\n";
    }

    private record LineRecord(int lineNumber, String content) {
    }
}
