package ku.com;

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
    private static final Pattern LOGIN_ID_PATTERN = Pattern.compile("^[A-Za-z][A-Za-z0-9_]{3,19}$");
    private static final Pattern USER_NAME_PATTERN = Pattern.compile("^[A-Za-z][A-Za-z0-9_]{3,19}$");
    private static final Pattern ROOM_ID_PATTERN = Pattern.compile("^R[0-9]{3}$");
    private static final Pattern RESERVATION_ID_PATTERN = Pattern.compile("^rv[0-9]{4}$");

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
        try {
            if (menu == 1) {
                handleSignup();
            } else if (menu == 2) {
                handleLogin();
            } else {
                System.out.println("프로그램을 종료합니다.");
                throw new ExitProgramException();
            }
        } catch (ActionAbortedException ignored) {
        }
    }

    private void memberMenu() throws AppDataException {
        SystemData data = loadAndSync();
        System.out.println();
        System.out.println("[member 메뉴]");
        System.out.println("현재 가상 시각: " + TimeFormats.formatDateTime(data.currentTime));
        System.out.println("1. 현재 가상 시각 변경");
        System.out.println("2. 예약 가능 스터디룸 조회");
        System.out.println("3. 예약 신청");
        System.out.println("4. 예약 취소");
        System.out.println("5. 나의 예약 조회");
        System.out.println("6. 체크인");
        System.out.println("0. 로그아웃");

        int menu = promptMenuChoice(Set.of(0, 1, 2, 3, 4, 5, 6));
        try {
            switch (menu) {
                case 1 -> handleChangeCurrentTime();
                case 2 -> handleSearchAvailableRooms();
                case 3 -> handleCreateReservation();
                case 4 -> handleCancelReservation();
                case 5 -> handleMyReservations();
                case 6 -> handleCheckIn();
                case 0 -> handleLogout();
                default -> throw new IllegalStateException();
            }
        } catch (ActionAbortedException ignored) {
        }
    }

    private void adminMenu() throws AppDataException {
        SystemData data = loadAndSync();
        System.out.println();
        System.out.println("[admin 메뉴]");
        System.out.println("현재 가상 시각: " + TimeFormats.formatDateTime(data.currentTime));
        System.out.println("1. 현재 가상 시각 변경");
        System.out.println("2. 전체 예약 정보 조회");
        System.out.println("3. 예약 조정(방 이동)");
        System.out.println("4. 룸 컨디션 관리");
        System.out.println("0. 로그아웃");

        int menu = promptMenuChoice(Set.of(1, 2, 3, 4, 0));
        try {
            switch (menu) {
                case 1 -> handleChangeCurrentTime();
                case 2 -> handleAllReservations();
                case 3 -> handleAdjustReservationRoom();
                case 4 -> handleRoomConditionMenu();
                case 0 -> handleLogout();
                default -> throw new IllegalStateException();
            }
        } catch (ActionAbortedException ignored) {
        }
    }

    private void handleSignup() throws AppDataException {
        SystemData data = store.loadAll();
        System.out.println("[회원가입]");
        String loginId = promptLoginId("로그인 ID 입력: ");
        String password = promptPassword();
        String userName = promptNewUserName();
        if (data.findUserByLoginId(loginId) != null) {
            System.out.println("오류: 이미 사용 중인 로그인 ID입니다.");
            return;
        }
        String userId = data.nextUserId();

        data.users.put(userId, new User(userId, loginId, password, userName, Role.MEMBER, 0));
        store.saveAll(data);
        System.out.println("회원가입이 완료되었습니다.");
    }

    private void handleLogin() throws AppDataException {
        System.out.println("[로그인]");
        String loginId = promptLoginId("로그인 ID: ");
        String password = promptPassword("비밀번호: ");
        SystemData data = loadAndSync();
        User user = data.findUserByLoginId(loginId);
        if (user == null) {
            System.out.println("오류: 존재하지 않는 로그인 ID입니다.");
            return;
        }
        if (!user.password.equals(password)) {
            System.out.println("오류: 비밀번호가 일치하지 않습니다.");
            return;
        }

        loggedInUserId = user.userId;
        loggedInRole = user.role;
        System.out.println("로그인 성공: " + user.userName + " (role: " + user.role.fileValue() + ")");
    }

    private void handleLogout() {
        loggedInUserId = null;
        loggedInRole = null;
        System.out.println("로그아웃되었습니다.");
    }

    private void handleSearchAvailableRooms() throws AppDataException {
        requireRole(Role.MEMBER);
        SystemData data = loadAndSync();
        System.out.println("[예약 가능 스터디룸 조회]");
        System.out.println("현재 가상 시각: " + TimeFormats.formatDateTime(data.currentTime));
        LocalDate date = promptDate("날짜 입력(yyyy-MM-dd): ");
        LocalTime start = promptTime("시작 시각 입력(HH:mm): ");
        LocalTime end = promptTime("종료 시각 입력(HH:mm): ");
        int partySize = promptPositiveInt("인원 수 입력: ");

        String error = validateReservationWindow(date, start, end);
        if (error != null) {
            System.out.println("오류: " + error);
            return;
        }

        LocalDateTime startAt = LocalDateTime.of(date, start);
        LocalDateTime endAt = LocalDateTime.of(date, end);
        if (!startAt.isAfter(data.currentTime)) {
            System.out.println("오류: 예약 시작 시각은 현재 가상 시각보다 미래여야 합니다.");
            return;
        }
        if (hasUserOverlap(data, loggedInUserId, startAt, endAt, null)) {
            System.out.println("오류: 같은 시간대에 이미 다른 예약이 있습니다.");
            return;
        }

        printRoomHeader();
        int count = 0;
        for (Room room : data.sortedRooms()) {
            if (room.roomStatus != RoomStatus.OPEN) {
                continue;
            }
            if (room.maxCapacity < partySize) {
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
            return;
        }
        System.out.println("조회가 끝났습니다.");
    }

    private void handleCreateReservation() throws AppDataException {
        requireRole(Role.MEMBER);
        SystemData data = loadAndSync();
        System.out.println("[예약 신청]");
        System.out.println("현재 가상 시각: " + TimeFormats.formatDateTime(data.currentTime));

        LocalDate date = promptDate("날짜 입력(yyyy-MM-dd): ");
        LocalTime start = promptTime("시작 시각 입력(HH:mm): ");
        LocalTime end = promptTime("종료 시각 입력(HH:mm): ");
        int partySize = promptPositiveInt("인원 수 입력: ");
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
        if (partySize > room.maxCapacity) {
            System.out.println("오류: 수용 인원을 초과했습니다.");
            return;
        }

        LocalDateTime startAt = LocalDateTime.of(date, start);
        LocalDateTime endAt = LocalDateTime.of(date, end);
        if (!startAt.isAfter(data.currentTime)) {
            System.out.println("오류: 예약 시작 시각은 현재 가상 시각보다 미래여야 합니다.");
            return;
        }
        if (hasRoomOverlap(data, roomId, startAt, endAt, null)) {
            System.out.println("오류: 해당 시간대에 이미 예약된 룸입니다.");
            return;
        }
        if (hasUserOverlap(data, loggedInUserId, startAt, endAt, null)) {
            System.out.println("오류: 같은 시간대에 이미 다른 예약이 있습니다.");
            return;
        }

        String reservationId = data.nextReservationId();
        Reservation reservation = new Reservation(
                reservationId,
                loggedInUserId,
                roomId,
                date,
                start,
                end,
                partySize,
                ReservationStatus.RESERVED,
                data.currentTime,
                null,
                0);
        data.reservations.put(reservationId, reservation);
        store.saveAll(data);
        System.out.println("예약이 완료되었습니다.");
        System.out.println("예약번호: " + reservationId);
    }

    private void handleCancelReservation() throws AppDataException {
        requireRole(Role.MEMBER);
        SystemData data = loadAndSync();
        System.out.println("[예약 취소]");
        System.out.println("현재 가상 시각: " + TimeFormats.formatDateTime(data.currentTime));
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
            System.out.println("오류: 이미 진행 중이거나 종료된 예약은 취소할 수 없습니다.");
            return;
        }

        data.reservations.remove(reservationId);
        store.saveAll(data);
        System.out.println("예약이 취소되었습니다.");
    }

    private void handleMyReservations() throws AppDataException {
        requireRole(Role.MEMBER);
        SystemData data = loadAndSync();
        System.out.println("[나의 예약 조회]");
        System.out.println("현재 가상 시각: " + TimeFormats.formatDateTime(data.currentTime));
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

        printMyReservationHeader();
        for (Reservation reservation : mine) {
            Room room = data.rooms.get(reservation.roomId);
            String roomName = room == null ? "-" : room.roomName;
            System.out.printf("%-8s %-6s %-10s %-10s %-5s %-5s %-4d %-12s%n",
                    reservation.reservationId,
                    reservation.roomId,
                    cut(roomName, 10),
                    TimeFormats.formatDate(reservation.date),
                    TimeFormats.formatTime(reservation.startTime),
                    TimeFormats.formatTime(reservation.endTime),
                    reservation.partySize,
                    reservation.status.name());
        }
        System.out.println("조회가 끝났습니다.");
    }

    private void handleCheckIn() throws AppDataException {
        requireRole(Role.MEMBER);
        SystemData data = loadAndSync();
        System.out.println("[체크인]");
        System.out.println("현재 가상 시각: " + TimeFormats.formatDateTime(data.currentTime));
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
        LocalDateTime close = reservation.startDateTime().plusMinutes(10);
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

    private void handleChangeCurrentTime() throws AppDataException {
        requireLoggedIn();
        SystemData data = loadAndSync();
        System.out.println("[현재 가상 시각 변경]");
        System.out.println("현재 가상 시각: " + TimeFormats.formatDateTime(data.currentTime));
        LocalDateTime before = data.currentTime;
        LocalDateTime next = promptDateTime("새 현재 시각 입력(yyyy-MM-dd HH:mm): ");
        System.out.println("기존 시각: " + TimeFormats.formatDateTime(before));
        System.out.println("새 시각: " + TimeFormats.formatDateTime(next));
        if (next.isBefore(before)) {
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
        System.out.println("[전체 예약 정보 조회]");
        System.out.println("현재 가상 시각: " + TimeFormats.formatDateTime(data.currentTime));
        if (data.reservations.isEmpty()) {
            System.out.println("예약 데이터가 없습니다.");
            return;
        }

        System.out.printf("%-8s %-10s %-10s %-6s %-10s %-5s %-5s %-4s %-12s %-16s%n",
                "resvId", "userId", "userName", "room", "date", "start", "end", "인원", "status", "checkedInAt");
        for (Reservation reservation : data.sortedReservations()) {
            User user = data.users.get(reservation.userId);
            String userName = user == null ? reservation.userId : user.userName;
            System.out.printf("%-8s %-10s %-10s %-6s %-10s %-5s %-5s %-4d %-12s %-16s%n",
                    reservation.reservationId,
                    reservation.userId,
                    cut(userName, 10),
                    reservation.roomId,
                    TimeFormats.formatDate(reservation.date),
                    TimeFormats.formatTime(reservation.startTime),
                    TimeFormats.formatTime(reservation.endTime),
                    reservation.partySize,
                    reservation.status.name(),
                    reservation.checkedInAtText());
        }
        System.out.println("조회가 끝났습니다.");
    }

    private void handleAdjustReservationRoom() throws AppDataException {
        requireRole(Role.ADMIN);
        SystemData data = loadAndSync();
        System.out.println("[예약 조정(방 이동)]");
        System.out.println("현재 가상 시각: " + TimeFormats.formatDateTime(data.currentTime));

        String reservationId = promptReservationId("조정할 예약번호 입력: ");
        String targetRoomId = promptRoomId("대상 룸 ID 입력: ");

        Reservation reservation = data.reservations.get(reservationId);
        if (reservation == null) {
            System.out.println("오류: 존재하지 않는 예약번호입니다.");
            return;
        }
        if (reservation.status != ReservationStatus.RESERVED || !reservation.startDateTime().isAfter(data.currentTime)) {
            System.out.println("오류: 조정 가능한 미래 RESERVED 예약만 이동할 수 있습니다.");
            return;
        }

        String moveError = validateRoomMove(data, reservation, targetRoomId);
        if (moveError != null) {
            System.out.println("오류: " + moveError);
            return;
        }

        String beforeRecord = reservation.toRecord();
        reservation.roomId = targetRoomId;
        store.saveAll(data);
        printRecordChange("예약", beforeRecord, reservation.toRecord());
        System.out.println("예약 조정이 완료되었습니다.");
    }

    private void handleRoomConditionMenu() throws AppDataException {
        requireRole(Role.ADMIN);
        while (true) {
            SystemData data = loadAndSync();
            System.out.println();
            System.out.println("[룸 컨디션 관리]");
            System.out.println("현재 가상 시각: " + TimeFormats.formatDateTime(data.currentTime));
            System.out.println("1. 전체 룸 조회");
            System.out.println("2. 룸 최대 수용 인원 변경");
            System.out.println("3. 룸 임시 휴업");
            System.out.println("4. 룸 운영 재개");
            System.out.println("0. 상위 메뉴로");
            int menu = promptMenuChoice(Set.of(1, 2, 3, 4, 0));
            if (menu == 0) {
                return;
            }
            try {
                if (menu == 1) {
                    handleRoomList();
                } else if (menu == 2) {
                    handleChangeRoomCapacity();
                } else if (menu == 3) {
                    handleCloseRoom();
                } else {
                    handleOpenRoom();
                }
            } catch (ActionAbortedException ignored) {
            }
        }
    }

    private void handleRoomList() throws AppDataException {
        SystemData data = loadAndSync();
        System.out.println("[전체 룸 조회]");
        printRoomHeader();
        for (Room room : data.sortedRooms()) {
            printRoomRow(room);
        }
        System.out.println("조회가 끝났습니다.");
    }

    private void handleChangeRoomCapacity() throws AppDataException {
        SystemData data = loadAndSync();
        System.out.println("[최대 수용 인원 변경]");
        System.out.println("현재 가상 시각: " + TimeFormats.formatDateTime(data.currentTime));
        String roomId = promptRoomId("룸 ID 입력: ");
        int newCapacity = promptPositiveInt("새 최대 수용 인원 입력: ");

        Room room = data.rooms.get(roomId);
        if (room == null) {
            System.out.println("오류: 존재하지 않는 룸 ID입니다.");
            return;
        }

        if (hasActiveReservationOverCapacity(data, roomId, newCapacity)) {
            System.out.println("오류: 현재 CHECKED_IN 예약 인원이 새 최대 수용 인원을 초과하여 변경할 수 없습니다.");
            return;
        }

        List<Reservation> impacted = findFutureReservedWithTooManyPeople(data, roomId, newCapacity);
        if (!impacted.isEmpty()) {
            System.out.println("현재 CHECKED_IN 예약 인원 검사 통과");
            System.out.println("영향 예약이 있어 처리 흐름을 시작합니다.");
            boolean success = processImpactedReservations(data, impacted);
            if (!success) {
                System.out.println("룸 컨디션 변경을 취소하여 원상복구했습니다.");
                return;
            }
            System.out.println("영향 예약 처리가 완료되었습니다.");
        }

        String beforeRoomRecord = room.toRecord();
        room.maxCapacity = newCapacity;
        store.saveAll(data);
        printRecordChange("ROOM", beforeRoomRecord, room.toRecord());
        System.out.println("룸 최대 수용 인원이 변경되었습니다.");
    }

    private void handleCloseRoom() throws AppDataException {
        SystemData data = loadAndSync();
        System.out.println("[룸 임시 휴업]");
        System.out.println("현재 가상 시각: " + TimeFormats.formatDateTime(data.currentTime));
        String roomId = promptRoomId("휴업할 룸 ID 입력: ");
        Room room = data.rooms.get(roomId);
        if (room == null) {
            System.out.println("오류: 존재하지 않는 룸 ID입니다.");
            return;
        }

        for (Reservation reservation : data.reservations.values()) {
            if (reservation.roomId.equals(roomId) && reservation.status == ReservationStatus.CHECKED_IN) {
                System.out.println("오류: 현재 체크인 중인 예약이 있어 즉시 휴업할 수 없습니다.");
                return;
            }
        }

        List<Reservation> impacted = findFutureReservedInRoom(data, roomId);
        RoomStatus originalStatus = room.roomStatus;
        if (!impacted.isEmpty()) {
            System.out.println("영향 예약이 있어 처리 흐름을 시작합니다.");
            boolean success = processImpactedReservations(data, impacted);
            if (!success) {
                room.roomStatus = originalStatus;
                System.out.println("룸 컨디션 변경을 취소하여 원상복구했습니다.");
                return;
            }
        }
        if (hasFutureReservedInRoom(data, roomId)) {
            System.out.println("오류: 영향 예약이 모두 처리되지 않아 휴업할 수 없습니다.");
            return;
        }

        String beforeRoomRecord = room.toRecord();
        room.roomStatus = RoomStatus.CLOSED;
        store.saveAll(data);
        printRecordChange("ROOM", beforeRoomRecord, room.toRecord());
        System.out.println("룸이 임시 휴업 처리되었습니다.");
    }

    private void handleOpenRoom() throws AppDataException {
        SystemData data = loadAndSync();
        System.out.println("[룸 운영 재개]");
        System.out.println("현재 가상 시각: " + TimeFormats.formatDateTime(data.currentTime));
        String roomId = promptRoomId("운영 재개할 룸 ID 입력: ");
        Room room = data.rooms.get(roomId);
        if (room == null) {
            System.out.println("오류: 존재하지 않는 룸 ID입니다.");
            return;
        }
        String beforeRoomRecord = room.toRecord();
        room.roomStatus = RoomStatus.OPEN;
        store.saveAll(data);
        printRecordChange("ROOM", beforeRoomRecord, room.toRecord());
        System.out.println("룸 운영이 재개되었습니다.");
    }

    private boolean processImpactedReservations(SystemData data, List<Reservation> impacted) {
        Map<String, Reservation> originals = new LinkedHashMap<>();
        for (Reservation reservation : impacted) {
            originals.put(reservation.reservationId, reservation.copy());
        }

        for (Reservation reservation : impacted) {
            while (true) {
                Reservation current = data.reservations.getOrDefault(reservation.reservationId, reservation);
                System.out.println("영향 예약 레코드:");
                System.out.println(current.toRecord());
                System.out.println("1) 다른 룸으로 이동");
                System.out.println("2) 해당 예약 취소");
                System.out.println("0) 이번 룸 컨디션 변경 전체 취소");

                int choice = promptMenuChoice(Set.of(1, 2, 0));
                if (choice == 0) {
                    rollbackImpacted(data, originals);
                    return false;
                }
                if (choice == 2) {
                    data.reservations.remove(current.reservationId);
                    break;
                }

                String targetRoomId = promptRoomId("이동할 대상 룸 ID 입력: ");
                String moveError = validateRoomMove(data, current, targetRoomId);
                if (moveError != null) {
                    System.out.println("오류: " + moveError);
                    continue;
                }
                String beforeRecord = current.toRecord();
                current.roomId = targetRoomId;
                data.reservations.put(current.reservationId, current);
                printRecordChange("RESV", beforeRecord, current.toRecord());
                break;
            }
        }
        return true;
    }

    private void rollbackImpacted(SystemData data, Map<String, Reservation> originals) {
        for (Map.Entry<String, Reservation> entry : originals.entrySet()) {
            data.reservations.put(entry.getKey(), entry.getValue().copy());
        }
    }

    private List<Reservation> findFutureReservedWithTooManyPeople(SystemData data, String roomId, int newCapacity) {
        List<Reservation> impacted = new ArrayList<>();
        for (Reservation reservation : data.sortedReservations()) {
            if (reservation.roomId.equals(roomId)
                    && reservation.status == ReservationStatus.RESERVED
                    && reservation.startDateTime().isAfter(data.currentTime)
                    && reservation.partySize > newCapacity) {
                impacted.add(reservation);
            }
        }
        return impacted;
    }

    private List<Reservation> findFutureReservedInRoom(SystemData data, String roomId) {
        List<Reservation> impacted = new ArrayList<>();
        for (Reservation reservation : data.sortedReservations()) {
            if (reservation.roomId.equals(roomId)
                    && reservation.status == ReservationStatus.RESERVED
                    && reservation.startDateTime().isAfter(data.currentTime)) {
                impacted.add(reservation);
            }
        }
        return impacted;
    }

    private boolean hasFutureReservedInRoom(SystemData data, String roomId) {
        for (Reservation reservation : data.reservations.values()) {
            if (reservation.roomId.equals(roomId)
                    && reservation.status == ReservationStatus.RESERVED
                    && reservation.startDateTime().isAfter(data.currentTime)) {
                return true;
            }
        }
        return false;
    }

    private boolean hasActiveReservationOverCapacity(SystemData data, String roomId, int newCapacity) {
        for (Reservation reservation : data.reservations.values()) {
            if (!reservation.roomId.equals(roomId)) {
                continue;
            }
            if (reservation.status != ReservationStatus.CHECKED_IN) {
                continue;
            }
            if (reservation.partySize > newCapacity) {
                return true;
            }
        }
        return false;
    }

    private String validateRoomMove(SystemData data, Reservation reservation, String targetRoomId) {
        Room target = data.rooms.get(targetRoomId);
        if (target == null) {
            return "존재하지 않는 룸 ID입니다.";
        }
        if (reservation.roomId.equals(targetRoomId)) {
            return "현재 룸과 다른 룸으로만 이동할 수 있습니다.";
        }
        if (target.roomStatus != RoomStatus.OPEN) {
            return "대상 룸이 OPEN 상태가 아니어서 이동할 수 없습니다.";
        }
        if (target.maxCapacity < reservation.partySize) {
            return "대상 룸의 수용 인원이 부족합니다.";
        }
        if (hasRoomOverlap(data, targetRoomId, reservation.startDateTime(), reservation.endDateTime(), reservation.reservationId)) {
            return "대상 룸의 같은 시간대에 이미 예약이 있습니다.";
        }
        return null;
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
        if (start.getMinute() != 0 || end.getMinute() != 0) {
            return "예약 시각은 1시간 단위여야 합니다.";
        }
        if (!start.isBefore(end)) {
            return "시작 시각은 종료 시각보다 빨라야 합니다.";
        }
        long minutes = Duration.between(LocalDateTime.of(date, start), LocalDateTime.of(date, end)).toMinutes();
        if (minutes != 60 && minutes != 120 && minutes != 180 && minutes != 240) {
            return "예약 길이는 1시간, 2시간, 3시간, 4시간 중 하나여야 합니다.";
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
    }

    private void printRoomHeader() {
        System.out.printf("%-6s %-12s %-4s %-8s%n", "roomId", "roomName", "정원", "status");
    }

    private void printRoomRow(Room room) {
        System.out.printf("%-6s %-12s %-4d %-8s%n",
                room.roomId,
                cut(room.roomName, 12),
                room.maxCapacity,
                room.roomStatus.name());
    }

    private void printMyReservationHeader() {
        System.out.printf("%-8s %-6s %-10s %-10s %-5s %-5s %-4s %-12s%n",
                "resvId", "room", "roomName", "date", "start", "end", "인원", "status");
    }

    private String cut(String value, int width) {
        if (value == null) {
            return "";
        }
        if (value.length() <= width) {
            return value;
        }
        if (width < 2) {
            return value.substring(0, width);
        }
        return value.substring(0, width - 1) + "~";
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

    private void requireLoggedIn() {
        if (loggedInRole == null || loggedInUserId == null) {
            throw new FatalAppException();
        }
    }

    private int promptMenuChoice(Set<Integer> allowed) {
        while (true) {
            String input = promptLine("메뉴 선택: ");
            if (!input.equals(input.trim())) {
                System.out.println("오류: 메뉴 선택 앞뒤에 공백을 넣을 수 없습니다.");
                continue;
            }
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

    private String promptNewUserName() {
        String userName = promptStrictValue("사용자명 입력: ");
        if (!USER_NAME_PATTERN.matcher(userName).matches()) {
            abortAction("오류: 사용자명은 영문자로 시작하고 영문자/숫자/_ 만 사용하여 4~20자로 입력해야 합니다.");
        }
        return userName;
    }

    private String promptLoginId(String prompt) {
        String loginId = promptStrictValue(prompt);
        if (!LOGIN_ID_PATTERN.matcher(loginId).matches()) {
            abortAction("오류: 로그인 ID는 영문자로 시작하고 영문자/숫자/_ 만 사용하여 4~20자로 입력해야 합니다.");
        }
        return loginId;
    }

    private String promptPassword() {
        return promptPassword("비밀번호 입력: ");
    }

    private String promptPassword(String prompt) {
        String password = promptStrictValue(prompt);
        if (password.length() < 4 || password.length() > 20) {
            abortAction("오류: 비밀번호는 4~20자로 입력해야 합니다.");
        }
        if (hasForbiddenChars(password)) {
            abortAction("오류: 비밀번호에 사용할 수 없는 문자가 포함되어 있습니다.");
        }
        return password;
    }

    private String promptRoomId(String prompt) {
        String roomId = promptStrictValue(prompt);
        if (!ROOM_ID_PATTERN.matcher(roomId).matches()) {
            abortAction("오류: 룸 ID 형식이 올바르지 않습니다. 예: R101");
        }
        return roomId;
    }

    private String promptReservationId(String prompt) {
        String reservationId = promptStrictValue(prompt);
        if (!RESERVATION_ID_PATTERN.matcher(reservationId).matches()) {
            abortAction("오류: 예약번호 형식이 올바르지 않습니다. 예: rv0001");
        }
        return reservationId;
    }

    private LocalDate promptDate(String prompt) {
        String dateText = promptStrictValue(prompt);
        try {
            return LocalDate.parse(dateText, TimeFormats.DATE);
        } catch (DateTimeParseException e) {
            abortAction("오류: 날짜 형식이 올바르지 않습니다. 예: 2026-03-20");
            return null;
        }
    }

    private LocalTime promptTime(String prompt) {
        String timeText = promptStrictValue(prompt);
        try {
            LocalTime time = LocalTime.parse(timeText, TimeFormats.TIME);
            if (time.getMinute() != 0) {
                abortAction("오류: 예약 시각은 1시간 단위여야 합니다.");
            }
            return time;
        } catch (DateTimeParseException e) {
            abortAction("오류: 시각 형식이 올바르지 않습니다. 예: 13:00");
            return null;
        }
    }

    private LocalDateTime promptDateTime(String prompt) {
        String text = promptStrictValue(prompt);
        try {
            return LocalDateTime.parse(text, TimeFormats.DATE_TIME);
        } catch (DateTimeParseException e) {
            abortAction("오류: 날짜/시각 형식이 올바르지 않습니다. 예: 2026-03-20 09:00");
            return null;
        }
    }

    private int promptPositiveInt(String prompt) {
        String text = promptStrictValue(prompt);
        try {
            int value = Integer.parseInt(text);
            if (value < 1) {
                abortAction("오류: 1 이상의 정수를 입력해야 합니다.");
            }
            return value;
        } catch (NumberFormatException e) {
            abortAction("오류: 숫자를 입력해야 합니다.");
            return -1;
        }
    }

    private String promptLine(String prompt) {
        System.out.print(prompt);
        if (!scanner.hasNextLine()) {
            throw new ExitProgramException();
        }
        return scanner.nextLine();
    }

    private boolean hasForbiddenChars(String text) {
        return text.indexOf('|') >= 0 || text.indexOf('\n') >= 0 || text.indexOf('\r') >= 0;
    }

    private String promptStrictValue(String prompt) {
        String raw = promptLine(prompt);
        if (!raw.equals(raw.trim())) {
            abortAction("오류: 입력값 앞뒤에 공백을 넣을 수 없습니다.");
        }
        if (raw.isEmpty()) {
            abortAction("오류: 빈 값을 입력할 수 없습니다.");
        }
        return raw;
    }

    private void abortAction(String message) {
        System.out.println(message);
        throw new ActionAbortedException();
    }

    private void printRecordChange(String label, String before, String after) {
        System.out.println("변경 전 " + label + " 레코드:");
        System.out.println(before);
        System.out.println("변경 후 " + label + " 레코드:");
        System.out.println(after);
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
