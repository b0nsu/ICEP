import java.net.URISyntaxException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;
import java.util.regex.Pattern;

final class CliApp {
    private static final Pattern USER_ID_PATTERN = Pattern.compile("^[a-z][a-z0-9]{3,11}$");
    private static final Pattern USER_NAME_PATTERN = Pattern.compile("^[A-Za-z가-힣 ]{2,20}$");
    private static final Pattern ROOM_ID_PATTERN = Pattern.compile("^R[0-9]{3}$");
    private static final Pattern RESERVATION_ID_PATTERN = Pattern.compile("^rv[0-9]{4,}$");

    private final Scanner scanner = new Scanner(System.in);
    private final TextDataStore store = new TextDataStore(resolveProjectRoot());

    private String loggedInUserId;
    private Role loggedInRole;

    private static Path resolveProjectRoot() {
        try {
            Path codePath = Paths.get(CliApp.class.getProtectionDomain().getCodeSource().getLocation().toURI()).toAbsolutePath().normalize();
            Path base = codePath;
            if (codePath.toString().endsWith(".jar")) {
                base = codePath.getParent();
            }
            if (base != null && base.getFileName() != null && "classes".equals(base.getFileName().toString())) {
                Path parent = base.getParent();
                if (parent != null && parent.getFileName() != null && "out".equals(parent.getFileName().toString())) {
                    Path root = parent.getParent();
                    if (root != null) {
                        return root;
                    }
                }
            }
            if (base != null && base.getFileName() != null && "out".equals(base.getFileName().toString())) {
                Path root = base.getParent();
                if (root != null) {
                    return root;
                }
            }
        } catch (URISyntaxException ignored) {
        }
        return Paths.get("").toAbsolutePath().normalize();
    }

    void run() {
        try {
            store.ensureDataFiles();
            loadAndSync();
            while (true) {
                if (loggedInUserId == null) {
                    guestMenu();
                } else if (loggedInRole == Role.MEMBER) {
                    memberMenu();
                } else {
                    adminMenu();
                }
            }
        } catch (ExitProgramException ignored) {
        } catch (AppDataException e) {
            printFileErrorAndExit(e);
        }
    }

    private void guestMenu() throws AppDataException {
        System.out.println();
        System.out.println("[비로그인 메뉴]");
        System.out.println("1. 회원가입");
        System.out.println("2. 로그인");
        System.out.println("0. 종료");

        int menu = promptMenuChoice(Set.of(1, 2, 0));
        if (menu == 1) {
            handleSignup();
        } else if (menu == 2) {
            handleLogin();
        } else {
            System.out.println("프로그램을 종료합니다.");
            throw new ExitProgramException();
        }
    }

    private void memberMenu() throws AppDataException {
        System.out.println();
        System.out.println("[member 메뉴]");
        System.out.println("1. 현재 가상 시각 조회");
        System.out.println("2. 예약 가능 스터디룸 조회");
        System.out.println("3. 예약 신청");
        System.out.println("4. 예약 취소");
        System.out.println("5. 나의 예약 조회");
        System.out.println("6. 체크인");
        System.out.println("7. 사용 연장");
        System.out.println("8. 내 패널티 조회");
        System.out.println("0. 로그아웃");

        int menu = promptMenuChoice(Set.of(0, 1, 2, 3, 4, 5, 6, 7, 8));
        switch (menu) {
            case 1 -> handleViewCurrentTime();
            case 2 -> handleSearchAvailableRooms();
            case 3 -> handleCreateReservation();
            case 4 -> handleCancelReservation();
            case 5 -> handleMyReservations();
            case 6 -> handleCheckIn();
            case 7 -> handleExtendReservation();
            case 8 -> handleViewMyPenalty();
            case 0 -> handleLogout();
            default -> throw new IllegalStateException();
        }
    }

    private void adminMenu() throws AppDataException {
        System.out.println();
        System.out.println("[admin 메뉴]");
        System.out.println("1. 현재 가상 시각 조회");
        System.out.println("2. 현재 가상 시각 변경");
        System.out.println("3. 전체 예약 조회");
        System.out.println("4. 사용자 패널티 조회");
        System.out.println("5. 사용자 패널티 초기화");
        System.out.println("6. 룸 운영 상태 변경");
        System.out.println("0. 로그아웃");

        int menu = promptMenuChoice(Set.of(0, 1, 2, 3, 4, 5, 6));
        switch (menu) {
            case 1 -> handleViewCurrentTime();
            case 2 -> handleChangeCurrentTime();
            case 3 -> handleAllReservations();
            case 4 -> handlePenaltyList();
            case 5 -> handlePenaltyReset();
            case 6 -> handleRoomStatusMenu();
            case 0 -> handleLogout();
            default -> throw new IllegalStateException();
        }
    }

    private void handleSignup() throws AppDataException {
        SystemData data = store.loadAll();
        String userId = promptNewUserId(data);
        String password = promptPassword();
        String displayName = promptDisplayName();

        data.users.put(userId, new User(userId, password, displayName, Role.MEMBER, 0, UserStatus.ACTIVE, 0));
        store.saveAll(data);
        System.out.println("회원가입이 완료되었습니다.");
    }

    private void handleLogin() throws AppDataException {
        SystemData loaded = store.loadAll();
        String userId = promptLine("사용자 ID: ").trim();
        String password = promptLine("비밀번호: ").trim();

        User user = loaded.users.get(userId);
        if (user == null) {
            System.out.println("오류: 존재하지 않는 사용자 ID입니다.");
            return;
        }
        if (!user.password.equals(password)) {
            System.out.println("오류: 비밀번호가 일치하지 않습니다.");
            return;
        }
        if (user.status != UserStatus.ACTIVE) {
            System.out.println("오류: 비활성 사용자입니다.");
            return;
        }

        loggedInUserId = user.userId;
        loggedInRole = user.role;
        System.out.println("로그인 성공: " + user.displayName + " (" + user.role.fileValue() + ")");
    }

    private void handleLogout() {
        loggedInUserId = null;
        loggedInRole = null;
        System.out.println("로그아웃되었습니다.");
    }

    private void handleViewCurrentTime() throws AppDataException {
        SystemData data = store.loadAll();
        System.out.println("현재 가상 시각: " + TimeFormats.formatDateTime(data.currentTime));
    }

    private void handleSearchAvailableRooms() throws AppDataException {
        requireRole(Role.MEMBER);
        SystemData data = loadAndSync();

        LocalDate date = promptDate("날짜 입력(yyyy-MM-dd): ");
        LocalTime start = promptTime("시작 시각 입력(HH:mm): ");
        LocalTime end = promptTime("종료 시각 입력(HH:mm): ");
        int people = promptPositiveInt("이용 인원 입력: ");

        String error = validateReservationWindow(date, start, end);
        if (error != null) {
            System.out.println("오류: " + error);
            return;
        }

        LocalDateTime startAt = LocalDateTime.of(date, start);
        LocalDateTime endAt = LocalDateTime.of(date, end);

        printRoomHeader();
        int count = 0;
        for (Room room : data.sortedRooms()) {
            if (room.roomStatus != RoomStatus.OPEN) {
                continue;
            }
            if (room.capacity < people) {
                continue;
            }
            if (!isWithinRoomOperation(room, startAt, endAt)) {
                continue;
            }
            if (hasRoomOverlap(data, room.roomId, startAt, endAt, null)) {
                continue;
            }
            printRoomRow(room);
            count++;
        }

        if (count == 0) {
            System.out.println("조회 결과가 없습니다.");
        }
    }

    private void handleCreateReservation() throws AppDataException {
        requireRole(Role.MEMBER);
        SystemData data = loadAndSync();

        User me = data.users.get(loggedInUserId);
        if (me == null) {
            System.out.println("오류: 회원 정보를 확인할 수 없습니다.");
            return;
        }
        if (me.penalty >= 2) {
            System.out.println("오류: 패널티 2점 이상은 예약이 제한됩니다.");
            return;
        }
        if (futureReservedCount(data, loggedInUserId) >= 2) {
            System.out.println("오류: 미래 예약은 최대 2건까지만 가능합니다.");
            return;
        }

        LocalDate date = promptDate("날짜 입력(yyyy-MM-dd): ");
        LocalTime start = promptTime("시작 시각 입력(HH:mm): ");
        LocalTime end = promptTime("종료 시각 입력(HH:mm): ");
        int people = promptPositiveInt("이용 인원 입력: ");
        String roomId = promptRoomId("룸 ID 입력: ");

        String windowError = validateReservationWindow(date, start, end);
        if (windowError != null) {
            System.out.println("오류: " + windowError);
            return;
        }

        Room room = data.rooms.get(roomId);
        if (room == null) {
            System.out.println("오류: 존재하지 않는 룸 ID입니다.");
            return;
        }
        if (room.roomStatus != RoomStatus.OPEN) {
            System.out.println("오류: 해당 룸은 현재 운영 중이 아닙니다.");
            return;
        }
        if (room.capacity < people) {
            System.out.println("오류: 수용 인원을 초과했습니다.");
            return;
        }

        LocalDateTime startAt = LocalDateTime.of(date, start);
        LocalDateTime endAt = LocalDateTime.of(date, end);
        if (!startAt.isAfter(data.currentTime)) {
            System.out.println("오류: 예약 시작 시각은 현재 가상 시각보다 미래여야 합니다.");
            return;
        }
        if (!isWithinRoomOperation(room, startAt, endAt)) {
            System.out.println("오류: 예약 구간이 룸 운영 시간을 벗어납니다.");
            return;
        }
        if (hasRoomOverlap(data, roomId, startAt, endAt, null)) {
            System.out.println("오류: 해당 시간대에 이미 예약된 룸입니다.");
            return;
        }
        if (hasUserOverlap(data, loggedInUserId, startAt, endAt, null)) {
            System.out.println("오류: 같은 시간대에 이미 본인 예약이 있습니다.");
            return;
        }

        String reservationId = data.nextReservationId();
        Reservation reservation = new Reservation(
                reservationId,
                roomId,
                loggedInUserId,
                date,
                start,
                end,
                ReservationStatus.RESERVED,
                null,
                0,
                0);
        data.reservations.put(reservationId, reservation);
        store.saveAll(data);
        System.out.println("예약이 완료되었습니다. 예약번호: " + reservationId);
    }

    private void handleCancelReservation() throws AppDataException {
        requireRole(Role.MEMBER);
        SystemData data = loadAndSync();

        String reservationId = promptReservationId("취소할 예약번호 입력: ");
        Reservation reservation = data.reservations.get(reservationId);
        if (reservation == null) {
            System.out.println("오류: 존재하지 않는 예약번호입니다.");
            return;
        }
        if (!reservation.userId.equals(loggedInUserId)) {
            System.out.println("오류: 본인의 예약만 취소할 수 있습니다.");
            return;
        }
        if (reservation.status != ReservationStatus.RESERVED) {
            System.out.println("오류: 취소 가능한 상태의 예약이 아닙니다.");
            return;
        }
        if (!reservation.startDateTime().isAfter(data.currentTime)) {
            System.out.println("오류: 예약 시작 전이 아닌 건은 취소할 수 없습니다.");
            return;
        }

        boolean delayed = data.currentTime.isAfter(reservation.startDateTime().minusMinutes(30));
        reservation.status = ReservationStatus.CANCELLED;
        reservation.checkedInAt = null;

        if (delayed) {
            User current = data.users.get(loggedInUserId);
            if (current != null) {
                data.users.put(current.userId, current.withPenalty(current.penalty + 1));
            }
        }

        store.saveAll(data);
        System.out.println("예약이 취소되었습니다.");
        if (delayed) {
            System.out.println("지연 취소로 패널티가 1점 부과되었습니다.");
        }
    }

    private void handleMyReservations() throws AppDataException {
        requireRole(Role.MEMBER);
        SystemData data = loadAndSync();

        List<Reservation> mine = new ArrayList<>();
        for (Reservation reservation : data.sortedReservations()) {
            if (reservation.userId.equals(loggedInUserId)) {
                mine.add(reservation);
            }
        }

        if (mine.isEmpty()) {
            System.out.println("나의 예약이 없습니다.");
            return;
        }

        System.out.printf("%-8s %-6s %-10s %-5s %-5s %-12s %-16s %s%n",
                "resvId", "room", "date", "start", "end", "status", "checkedInAt", "ext");
        for (Reservation reservation : mine) {
            System.out.printf("%-8s %-6s %-10s %-5s %-5s %-12s %-16s %d%n",
                    reservation.reservationId,
                    reservation.roomId,
                    TimeFormats.formatDate(reservation.date),
                    TimeFormats.formatTime(reservation.startTime),
                    TimeFormats.formatTime(reservation.endTime),
                    reservation.status.name(),
                    reservation.checkedInAtText(),
                    reservation.extensionCount);
        }
    }

    private void handleCheckIn() throws AppDataException {
        requireRole(Role.MEMBER);
        SystemData data = loadAndSync();
        String reservationId = promptReservationId("체크인할 예약번호 입력: ");

        Reservation reservation = data.reservations.get(reservationId);
        if (reservation == null) {
            System.out.println("오류: 존재하지 않는 예약번호입니다.");
            return;
        }
        if (!reservation.userId.equals(loggedInUserId)) {
            System.out.println("오류: 본인 예약만 체크인할 수 있습니다.");
            return;
        }
        if (reservation.status != ReservationStatus.RESERVED) {
            System.out.println("오류: 체크인 가능한 상태의 예약이 아닙니다.");
            return;
        }

        Room room = data.rooms.get(reservation.roomId);
        if (room == null || room.roomStatus != RoomStatus.OPEN) {
            System.out.println("오류: 운영 중이 아닌 룸은 체크인할 수 없습니다.");
            return;
        }

        LocalDateTime now = data.currentTime;
        LocalDateTime open = reservation.startDateTime().minusMinutes(10);
        LocalDateTime close = reservation.startDateTime().plusMinutes(15);

        if (now.isBefore(open)) {
            System.out.println("오류: 아직 체크인 가능한 시간이 아닙니다.");
            return;
        }
        if (now.isAfter(close)) {
            System.out.println("오류: 체크인 마감 시간이 지났습니다.");
            return;
        }

        reservation.status = ReservationStatus.CHECKED_IN;
        reservation.checkedInAt = now;
        store.saveAll(data);
        System.out.println("체크인이 완료되었습니다.");
    }

    private void handleExtendReservation() throws AppDataException {
        requireRole(Role.MEMBER);
        SystemData data = loadAndSync();
        String reservationId = promptReservationId("연장할 예약번호 입력: ");

        Reservation reservation = data.reservations.get(reservationId);
        if (reservation == null) {
            System.out.println("오류: 존재하지 않는 예약번호입니다.");
            return;
        }
        if (!reservation.userId.equals(loggedInUserId)) {
            System.out.println("오류: 본인 예약만 연장할 수 있습니다.");
            return;
        }
        if (reservation.status != ReservationStatus.CHECKED_IN) {
            System.out.println("오류: CHECKED_IN 상태에서만 연장할 수 있습니다.");
            return;
        }
        if (reservation.extensionCount >= 1) {
            System.out.println("오류: 연장은 1회만 가능합니다.");
            return;
        }

        Room room = data.rooms.get(reservation.roomId);
        if (room == null || room.roomStatus != RoomStatus.OPEN) {
            System.out.println("오류: 운영 중이 아닌 룸은 연장할 수 없습니다.");
            return;
        }

        LocalDateTime candidateEnd = reservation.endDateTime().plusMinutes(30);
        long finalMinutes = Duration.between(reservation.startDateTime(), candidateEnd).toMinutes();
        long baseMinutes = finalMinutes - 30;
        if (!(baseMinutes >= 60 && baseMinutes <= 240)) {
            System.out.println("오류: 연장 불가(기존 길이가 규칙을 벗어납니다).");
            return;
        }
        if (!(finalMinutes >= 90 && finalMinutes <= 270)) {
            System.out.println("오류: 연장 후 사용 길이는 90분~270분이어야 합니다.");
            return;
        }
        if (!isWithinRoomOperation(room, reservation.startDateTime(), candidateEnd)) {
            System.out.println("오류: 연장 후 예약이 룸 운영 시간을 벗어납니다.");
            return;
        }
        if (hasRoomOverlap(data, room.roomId, reservation.startDateTime(), candidateEnd, reservation.reservationId)) {
            System.out.println("오류: 연장 후 시간대에 다른 예약이 존재합니다.");
            return;
        }

        reservation.endTime = candidateEnd.toLocalTime();
        reservation.extensionCount = 1;
        store.saveAll(data);
        System.out.println("예약이 연장되었습니다. 연장 횟수 제한이 완료되었습니다.");
    }

    private void handleChangeCurrentTime() throws AppDataException {
        requireRole(Role.ADMIN);
        SystemData data = loadAndSync();

        LocalDateTime next = promptDateTime("새 현재 시각 입력(yyyy-MM-ddTHH:mm): ");
        if (next.isBefore(data.currentTime)) {
            System.out.println("오류: 현재 시각은 과거로 되돌릴 수 없습니다.");
            return;
        }

        data.currentTime = next;
        UpdateSummary summary = AutoStateUpdater.apply(data);
        store.saveAll(data);
        System.out.println("현재 시각이 변경되었습니다.");
        printUpdateSummary(summary);
    }

    private void handleAllReservations() throws AppDataException {
        requireRole(Role.ADMIN);
        SystemData data = loadAndSync();

        if (data.reservations.isEmpty()) {
            System.out.println("예약 데이터가 없습니다.");
            return;
        }

        System.out.printf("%-8s %-10s %-6s %-10s %-5s %-5s %-12s %-16s %s%n",
                "resvId", "userId", "room", "date", "start", "end", "status", "checkedInAt", "ext");
        for (Reservation reservation : data.sortedReservations()) {
            System.out.printf("%-8s %-10s %-6s %-10s %-5s %-5s %-12s %-16s %d%n",
                    reservation.reservationId,
                    reservation.userId,
                    reservation.roomId,
                    TimeFormats.formatDate(reservation.date),
                    TimeFormats.formatTime(reservation.startTime),
                    TimeFormats.formatTime(reservation.endTime),
                    reservation.status.name(),
                    reservation.checkedInAtText(),
                    reservation.extensionCount);
        }
    }

    private void handlePenaltyList() throws AppDataException {
        requireRole(Role.ADMIN);
        SystemData data = loadAndSync();

        System.out.printf("%-10s %-16s %s%n", "userId", "이름", "penalty");
        for (User user : data.users.values()) {
            if (user.role == Role.MEMBER) {
                System.out.printf("%-10s %-16s %d%n", user.userId, user.displayName, user.penalty);
            }
        }
    }

    private void handlePenaltyReset() throws AppDataException {
        requireRole(Role.ADMIN);
        SystemData data = loadAndSync();
        String userId = promptLine("패널티를 초기화할 회원 ID: ").trim();

        User user = data.users.get(userId);
        if (user == null || user.role != Role.MEMBER) {
            System.out.println("오류: 존재하지 않는 member ID입니다.");
            return;
        }
        if (user.penalty == 0) {
            System.out.println("해당 회원의 패널티는 이미 0점입니다.");
            return;
        }

        data.users.put(userId, user.withPenalty(0));
        store.saveAll(data);
        System.out.println("패널티가 초기화되었습니다.");
    }

    private void handleRoomStatusMenu() throws AppDataException {
        requireRole(Role.ADMIN);
        SystemData data = loadAndSync();

        printRoomHeader();
        for (Room room : data.sortedRooms()) {
            printRoomRow(room);
        }

        String roomId = promptRoomId("상태를 변경할 룸 ID: ");
        Room room = data.rooms.get(roomId);
        if (room == null) {
            System.out.println("오류: 존재하지 않는 룸 ID입니다.");
            return;
        }

        System.out.println("새 상태를 입력하세요: 1) OPEN  2) CLOSED  3) MAINTENANCE");
        int selected = promptMenuChoice(Set.of(1, 2, 3));
        RoomStatus target = selected == 1 ? RoomStatus.OPEN : (selected == 2 ? RoomStatus.CLOSED : RoomStatus.MAINTENANCE);

        if (target != RoomStatus.OPEN) {
            for (Reservation reservation : data.reservations.values()) {
                if (reservation.roomId.equals(roomId)
                        && reservation.activeForOverlap()
                        && reservation.endDateTime().isAfter(data.currentTime)) {
                    System.out.println("오류: 해당 룸에 진행 예정인 예약이 있어 상태를 변경할 수 없습니다.");
                    return;
                }
            }
        }

        room.roomStatus = target;
        store.saveAll(data);
        System.out.println("룸 상태가 변경되었습니다.");
    }

    private int futureReservedCount(SystemData data, String userId) {
        int count = 0;
        for (Reservation reservation : data.reservations.values()) {
            if (!reservation.userId.equals(userId)) {
                continue;
            }
            if (reservation.status != ReservationStatus.RESERVED) {
                continue;
            }
            if (reservation.startDateTime().isAfter(data.currentTime)) {
                count++;
            }
        }
        return count;
    }

    private boolean isWithinRoomOperation(Room room, LocalDateTime start, LocalDateTime end) {
        return !start.toLocalTime().isBefore(room.openTime) && !end.toLocalTime().isAfter(room.closeTime);
    }

    private boolean hasRoomOverlap(SystemData data,
                                  String roomId,
                                  LocalDateTime start,
                                  LocalDateTime end,
                                  String exceptReservationId) {
        for (Reservation reservation : data.reservations.values()) {
            if (!reservation.roomId.equals(roomId)) {
                continue;
            }
            if (exceptReservationId != null && exceptReservationId.equals(reservation.reservationId)) {
                continue;
            }
            if (!reservation.activeForOverlap()) {
                continue;
            }
            if (reservation.overlaps(start, end)) {
                return true;
            }
        }
        return false;
    }

    private boolean hasUserOverlap(SystemData data,
                                  String userId,
                                  LocalDateTime start,
                                  LocalDateTime end,
                                  String exceptReservationId) {
        for (Reservation reservation : data.reservations.values()) {
            if (!reservation.userId.equals(userId)) {
                continue;
            }
            if (exceptReservationId != null && exceptReservationId.equals(reservation.reservationId)) {
                continue;
            }
            if (!reservation.activeForOverlap()) {
                continue;
            }
            if (reservation.overlaps(start, end)) {
                return true;
            }
        }
        return false;
    }

    private String validateReservationWindow(LocalDate date, LocalTime start, LocalTime end) {
        if (start.getMinute() != 0 && start.getMinute() != 30) {
            return "예약 시각은 30분 단위여야 합니다.";
        }
        if (end.getMinute() != 0 && end.getMinute() != 30) {
            return "예약 시각은 30분 단위여야 합니다.";
        }
        if (!start.isBefore(end)) {
            return "시작 시각은 종료 시각보다 빨라야 합니다.";
        }

        long minutes = Duration.between(LocalDateTime.of(date, start), LocalDateTime.of(date, end)).toMinutes();
        if (minutes < 60 || minutes > 240 || minutes % 30 != 0) {
            return "예약 길이는 60분~240분(30분 단위)이어야 합니다.";
        }
        return null;
    }

    private void printUpdateSummary(UpdateSummary summary) {
        if (!summary.changed()) {
            System.out.println("상태 변화 없음");
            return;
        }
        System.out.println("상태 변화 요약:");
        for (TransitionCount count : summary.transitionCounts()) {
            System.out.printf("- %s -> %s : %d건%n", count.from().name(), count.to().name(), count.count());
        }
        if (summary.totalPenaltyIncreased() > 0) {
            System.out.printf("- 사용자 패널티 +%d건%n", summary.totalPenaltyIncreased());
        }
    }

    private void printRoomHeader() {
        System.out.printf("%-6s %-4s %-10s %-8s %-8s %-20s%n", "room", "cap", "open", "close", "status", "equipment");
    }

    private void printRoomRow(Room room) {
        System.out.printf("%-6s %-4d %-10s %-8s %-8s %-20s%n",
                room.roomId,
                room.capacity,
                TimeFormats.formatTime(room.openTime),
                TimeFormats.formatTime(room.closeTime),
                room.roomStatus.name(),
                room.equipment);
    }

    private void handleViewMyPenalty() throws AppDataException {
        SystemData data = loadAndSync();
        User me = data.users.get(loggedInUserId);
        if (me == null) {
            System.out.println("오류: 회원 정보를 찾을 수 없습니다.");
            return;
        }
        System.out.println("현재 패널티: " + me.penalty);
    }

    private SystemData loadAndSync() throws AppDataException {
        SystemData data = store.loadAll();
        UpdateSummary summary = AutoStateUpdater.apply(data);
        if (summary.changed()) {
            store.saveAll(data);
            data = store.loadAll();
        }
        return data;
    }

    private void requireRole(Role role) {
        if (loggedInRole != role) {
            throw new FatalAppException();
        }
    }

    private int promptMenuChoice(Set<Integer> allowed) {
        while (true) {
            String input = promptLine("메뉴 선택: ").trim();
            if (!input.matches("^[0-9]+$")) {
                System.out.println("오류: 메뉴 번호는 숫자로 입력해야 합니다.");
                continue;
            }
            int value;
            try {
                value = Integer.parseInt(input);
            } catch (NumberFormatException e) {
                System.out.println("오류: 메뉴 번호는 숫자로 입력해야 합니다.");
                continue;
            }
            if (!allowed.contains(value)) {
                System.out.println("오류: 존재하지 않는 메뉴 번호입니다.");
                continue;
            }
            return value;
        }
    }

    private String promptNewUserId(SystemData data) {
        while (true) {
            String userId = promptLine("사용자 ID 입력: ").trim();
            if (!USER_ID_PATTERN.matcher(userId).matches()) {
                System.out.println("오류: 사용자 ID는 영문 소문자로 시작하고 영문 소문자/숫자로 4~12자로 입력해야 합니다.");
                continue;
            }
            if (data.users.containsKey(userId)) {
                System.out.println("오류: 이미 존재하는 사용자 ID입니다.");
                continue;
            }
            return userId;
        }
    }

    private String promptPassword() {
        while (true) {
            String password = promptLine("비밀번호 입력: ").trim();
            if (!Pattern.compile("^[A-Za-z0-9!@#$%^&*._-]{6,16}$").matcher(password).matches()) {
                System.out.println("오류: 비밀번호는 6~16자 영문/숫자/특수문자(!@#$%^&*._-)로 입력해야 합니다.");
                continue;
            }
            return password;
        }
    }

    private String promptDisplayName() {
        while (true) {
            String name = promptLine("이름 입력: ").trim();
            if (!USER_NAME_PATTERN.matcher(name).matches()) {
                System.out.println("오류: 이름은 영문/한글과 공백으로 2~20자만 입력 가능합니다.");
                continue;
            }
            if (name.contains("  ")) {
                System.out.println("오류: 이름에 연속된 공백을 사용할 수 없습니다.");
                continue;
            }
            return name;
        }
    }

    private String promptRoomId(String prompt) {
        while (true) {
            String roomId = promptLine(prompt).trim();
            if (!ROOM_ID_PATTERN.matcher(roomId).matches()) {
                System.out.println("오류: 룸 ID 형식이 올바르지 않습니다. 예: R101");
                continue;
            }
            return roomId;
        }
    }

    private String promptReservationId(String prompt) {
        while (true) {
            String reservationId = promptLine(prompt).trim();
            if (!RESERVATION_ID_PATTERN.matcher(reservationId).matches()) {
                System.out.println("오류: 예약번호 형식이 올바르지 않습니다. 예: rv0001");
                continue;
            }
            return reservationId;
        }
    }

    private LocalDate promptDate(String prompt) {
        while (true) {
            String dateText = promptLine(prompt).trim();
            try {
                return LocalDate.parse(dateText, TimeFormats.DATE);
            } catch (DateTimeParseException e) {
                System.out.println("오류: 날짜 형식이 올바르지 않습니다. 예: 2026-03-20");
            }
        }
    }

    private LocalTime promptTime(String prompt) {
        while (true) {
            String timeText = promptLine(prompt).trim();
            try {
                return LocalTime.parse(timeText, TimeFormats.TIME);
            } catch (DateTimeParseException e) {
                System.out.println("오류: 시각 형식이 올바르지 않습니다. 예: 13:00");
            }
        }
    }

    private LocalDateTime promptDateTime(String prompt) {
        while (true) {
            String text = promptLine(prompt).trim();
            try {
                return LocalDateTime.parse(text, TimeFormats.DATE_TIME);
            } catch (DateTimeParseException e) {
                System.out.println("오류: 날짜/시각 형식이 올바르지 않습니다. 예: 2026-03-20T09:00");
            }
        }
    }

    private int promptPositiveInt(String prompt) {
        while (true) {
            String text = promptLine(prompt).trim();
            try {
                int value = Integer.parseInt(text);
                if (value < 1) {
                    System.out.println("오류: 1 이상의 정수를 입력해야 합니다.");
                    continue;
                }
                return value;
            } catch (NumberFormatException e) {
                System.out.println("오류: 숫자를 입력해야 합니다.");
            }
        }
    }

    private String promptLine(String prompt) {
        System.out.print(prompt);
        if (!scanner.hasNextLine()) {
            throw new ExitProgramException();
        }
        return scanner.nextLine();
    }

    private void printFileErrorAndExit(AppDataException e) {
        if (e.getLineNumber() > 0) {
            System.out.println("[파일 오류] " + e.getFileName() + " " + e.getLineNumber() + "행: " + e.getMessage());
        } else {
            System.out.println("[파일 오류] " + e.getFileName() + ": " + e.getMessage());
        }
        System.out.println("프로그램 시작을 중단합니다.");
    }
}
