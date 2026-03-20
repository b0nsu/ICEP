import java.io.OutputStream;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;

public class RegressionTest {
    private static final Path BUILT_JAR = Path.of("out/study-room-cli.jar").toAbsolutePath();

    public static void main(String[] args) throws Exception {
        testAutoNoShowTransition();
        testNextReservationId();
        testHistoricalCompletedReservationCanExceedCurrentCapacity();
        testSameRoomMoveRejected();

        testMissingFilesAutoCreated();
        testInvalidFileSyntaxStopsStartup();
        testSignupDuplicateAndSuccess();
        testLoginFailureAndSuccess();
        testAvailableRoomQueryFiltersRooms();
        testCreateReservationSuccess();
        testCreateReservationRejectsPastTime();
        testCreateReservationRejectsRoomOverlap();
        testCreateReservationRejectsUserOverlap();
        testCancelSuccess();
        testCancelAfterStartRejected();
        testCheckInTooEarlyRejected();
        testCheckInBoundarySuccess();
        testCheckInClosedRoomRejected();
        testAdminTimeChangeRejectsPastAndAppliesTransitions();
        testManualMoveSameRoomRejectedAndThenSucceeds();
        testCapacityChangeWithHistoricalCompletedReservation();
        testCapacityChangeImpactedSameRoomRejectedAndMoveSuccess();
        testCapacityChangeRollbackRestoresState();
        testCloseRoomWithCheckedInReservationRejected();
        testCloseRoomImpactedSameRoomRejectedAndMoveSuccess();
        testOpenRoomSuccess();

        System.out.println("Regression tests passed.");
    }

    private static void testAutoNoShowTransition() throws Exception {
        Path root = Files.createTempDirectory("study-room-test-");
        writeData(root,
                users("u1001", "pw1234", "회원1"),
                "ROOM|R101|A룸|4|PROJECTOR|OPEN\n",
                "RESV|rv0001|u1001|R101|2026-03-20|11:00|12:00|2|RESERVED|2026-03-19 09:00|-\n",
                "NOW|2026-03-20 11:20\n");

        TextDataStore store = new TextDataStore(root);
        SystemData dataset = store.loadAll();
        UpdateSummary summary = AutoStateUpdater.apply(dataset);
        if (!summary.changed()) {
            throw new AssertionError("Expected reservation state update.");
        }
        if (dataset.reservations.get("rv0001").status != ReservationStatus.NO_SHOW) {
            throw new AssertionError("Expected rv0001 to become NO_SHOW.");
        }
    }

    private static void testNextReservationId() throws Exception {
        Path root = Files.createTempDirectory("study-room-id-test-");
        writeData(root,
                users("u1001", "pw1234", "회원1"),
                "ROOM|R101|A룸|4|PROJECTOR|OPEN\n",
                "RESV|rv0001|u1001|R101|2026-03-20|11:00|12:00|2|RESERVED|2026-03-19 09:00|-\n"
                        + "RESV|rv0003|u1001|R101|2026-03-21|11:00|12:00|2|RESERVED|2026-03-19 10:00|-\n",
                "NOW|2026-03-20 09:00\n");

        SystemData dataSet = new TextDataStore(root).loadAll();
        String next = dataSet.nextReservationId();
        if (!"rv0004".equals(next)) {
            throw new AssertionError("Expected rv0004 but got " + next);
        }
    }

    private static void testHistoricalCompletedReservationCanExceedCurrentCapacity() throws Exception {
        Path root = Files.createTempDirectory("study-room-capacity-test-");
        writeData(root,
                users("u1001", "pw1234", "회원1"),
                "ROOM|R101|A룸|4|PROJECTOR|OPEN\n",
                "RESV|rv0001|u1001|R101|2026-03-20|11:00|12:00|6|COMPLETED|2026-03-19 09:00|2026-03-20 10:55\n",
                "NOW|2026-03-20 13:00\n");

        SystemData dataSet = new TextDataStore(root).loadAll();
        if (dataSet.reservations.get("rv0001").status != ReservationStatus.COMPLETED) {
            throw new AssertionError("Expected completed reservation to remain loadable after capacity reduction.");
        }
    }

    private static void testSameRoomMoveRejected() throws Exception {
        Path root = Files.createTempDirectory("study-room-move-test-");
        writeData(root,
                users("u1001", "pw1234", "회원1"),
                "ROOM|R101|A룸|4|PROJECTOR|OPEN\n"
                        + "ROOM|R102|B룸|6|MONITOR|OPEN\n",
                "RESV|rv0001|u1001|R101|2026-03-21|11:00|12:00|2|RESERVED|2026-03-20 09:00|-\n",
                "NOW|2026-03-20 09:00\n");

        SystemData dataSet = new TextDataStore(root).loadAll();
        Reservation reservation = dataSet.reservations.get("rv0001");
        CliApp app = new CliApp();
        Method validateRoomMove = CliApp.class.getDeclaredMethod("validateRoomMove", SystemData.class, Reservation.class, String.class);
        validateRoomMove.setAccessible(true);
        String error = (String) validateRoomMove.invoke(app, dataSet, reservation, "R101");
        if (error == null || !error.contains("다른 룸")) {
            throw new AssertionError("Expected same-room move to be rejected, but got: " + error);
        }
    }

    private static void testMissingFilesAutoCreated() throws Exception {
        Path root = createCliRoot();
        String output = runCli(root, "0\n");
        assertContains(output, "[비로그인 메뉴]");
        assertFileContains(root, "users.txt", "USER|admin|admin1234|관리자|admin|ACTIVE");
        assertFileContains(root, "rooms.txt", "ROOM|R101|A룸|4|PROJECTOR+WHITEBOARD|OPEN");
        assertFileContains(root, "system_time.txt", "NOW|2026-03-20 09:00");
    }

    private static void testInvalidFileSyntaxStopsStartup() throws Exception {
        Path root = createCliRoot();
        writeData(root,
                users("user011", "12345", "본수"),
                baseRooms(),
                "RESV|broken|line\n",
                "NOW|2026-03-20 09:00\n");

        String output = runCli(root, "");
        assertContains(output, "[파일 오류] reservations.txt");
    }

    private static void testSignupDuplicateAndSuccess() throws Exception {
        Path root = createCliRoot();
        writeData(root, baseUsers(), baseRooms(), "", "NOW|2026-03-20 09:00\n");

        String input = lines(
                "1",
                "admin",
                "newuser",
                "pw12",
                "새사용자",
                "0");
        String output = runCli(root, input);

        assertContains(output, "오류: 이미 존재하는 사용자 ID입니다.");
        assertContains(output, "회원가입이 완료되었습니다.");
        assertFileContains(root, "users.txt", "USER|newuser|pw12|새사용자|member|ACTIVE");
    }

    private static void testLoginFailureAndSuccess() throws Exception {
        Path root = createCliRoot();
        writeData(root, baseUsers(), baseRooms(), "", "NOW|2026-03-20 09:00\n");

        String input = lines(
                "2",
                "user011",
                "wrong",
                "2",
                "user011",
                "12345",
                "0",
                "0");
        String output = runCli(root, input);

        assertContains(output, "오류: 비밀번호가 일치하지 않습니다.");
        assertContains(output, "로그인 성공: 본수 (member)");
    }

    private static void testAvailableRoomQueryFiltersRooms() throws Exception {
        Path root = createCliRoot();
        writeData(root,
                baseUsers(),
                "ROOM|R101|A룸|4|PROJECTOR|OPEN\n"
                        + "ROOM|R102|B룸|6|MONITOR|CLOSED\n"
                        + "ROOM|R103|C룸|2|-|OPEN\n",
                "RESV|rv0001|user022|R101|2026-03-20|13:00|15:00|2|RESERVED|2026-03-20 09:00|-\n",
                "NOW|2026-03-20 09:00\n");

        String input = lines(
                "2",
                "user011",
                "12345",
                "2",
                "2026-03-20",
                "13:00",
                "15:00",
                "3",
                "0",
                "0");
        String output = runCli(root, input);

        assertContains(output, "조회 결과가 없습니다.");
    }

    private static void testCreateReservationSuccess() throws Exception {
        Path root = createCliRoot();
        writeData(root, baseUsers(), baseRooms(), "", "NOW|2026-03-20 09:00\n");

        String input = lines(
                "2",
                "user011",
                "12345",
                "3",
                "2026-03-20",
                "13:00",
                "15:00",
                "2",
                "R102",
                "0",
                "0");
        String output = runCli(root, input);

        assertContains(output, "예약이 완료되었습니다. 예약번호: rv0001");
        assertFileContains(root, "reservations.txt", "RESV|rv0001|user011|R102|2026-03-20|13:00|15:00|2|RESERVED");
    }

    private static void testCreateReservationRejectsPastTime() throws Exception {
        Path root = createCliRoot();
        writeData(root, baseUsers(), baseRooms(), "", "NOW|2026-03-20 14:00\n");

        String input = lines(
                "2",
                "user011",
                "12345",
                "3",
                "2026-03-20",
                "13:00",
                "15:00",
                "2",
                "R102",
                "0",
                "0");
        String output = runCli(root, input);

        assertContains(output, "오류: 예약 시작 시각은 현재 가상 시각보다 미래여야 합니다.");
    }

    private static void testCreateReservationRejectsRoomOverlap() throws Exception {
        Path root = createCliRoot();
        writeData(root,
                baseUsers(),
                baseRooms(),
                "RESV|rv0001|user022|R101|2026-03-20|13:00|15:00|2|RESERVED|2026-03-20 09:00|-\n",
                "NOW|2026-03-20 09:00\n");

        String input = lines(
                "2",
                "user011",
                "12345",
                "3",
                "2026-03-20",
                "13:00",
                "15:00",
                "2",
                "R101",
                "0",
                "0");
        String output = runCli(root, input);

        assertContains(output, "오류: 해당 시간대에 이미 예약된 룸입니다.");
    }

    private static void testCreateReservationRejectsUserOverlap() throws Exception {
        Path root = createCliRoot();
        writeData(root,
                baseUsers(),
                baseRooms(),
                "RESV|rv0001|user011|R101|2026-03-20|13:00|15:00|2|RESERVED|2026-03-20 09:00|-\n",
                "NOW|2026-03-20 09:00\n");

        String input = lines(
                "2",
                "user011",
                "12345",
                "3",
                "2026-03-20",
                "13:30",
                "15:30",
                "2",
                "R102",
                "0",
                "0");
        String output = runCli(root, input);

        assertContains(output, "오류: 같은 시간대에 이미 다른 예약이 있습니다.");
    }

    private static void testCancelSuccess() throws Exception {
        Path root = createCliRoot();
        writeData(root,
                baseUsers(),
                baseRooms(),
                "RESV|rv0001|user011|R101|2026-03-20|13:00|15:00|2|RESERVED|2026-03-20 09:00|-\n",
                "NOW|2026-03-20 09:00\n");

        String input = lines(
                "2",
                "user011",
                "12345",
                "4",
                "rv0001",
                "0",
                "0");
        String output = runCli(root, input);

        assertContains(output, "예약이 취소되었습니다.");
        assertFileContains(root, "reservations.txt", "RESV|rv0001|user011|R101|2026-03-20|13:00|15:00|2|CANCELLED");
    }

    private static void testCancelAfterStartRejected() throws Exception {
        Path root = createCliRoot();
        writeData(root,
                baseUsers(),
                baseRooms(),
                "RESV|rv0001|user011|R101|2026-03-20|11:00|12:00|2|RESERVED|2026-03-20 09:00|-\n",
                "NOW|2026-03-20 11:00\n");

        String input = lines(
                "2",
                "user011",
                "12345",
                "4",
                "rv0001",
                "0",
                "0");
        String output = runCli(root, input);

        assertContains(output, "오류: 이미 진행 중이거나 종료된 예약은 취소할 수 없습니다.");
    }

    private static void testCheckInTooEarlyRejected() throws Exception {
        Path root = createCliRoot();
        writeData(root,
                baseUsers(),
                baseRooms(),
                "RESV|rv0001|user011|R101|2026-03-20|11:00|12:00|2|RESERVED|2026-03-20 09:00|-\n",
                "NOW|2026-03-20 10:49\n");

        String input = lines(
                "2",
                "user011",
                "12345",
                "6",
                "rv0001",
                "0",
                "0");
        String output = runCli(root, input);

        assertContains(output, "오류: 아직 체크인 가능한 시간이 아닙니다.");
    }

    private static void testCheckInBoundarySuccess() throws Exception {
        Path root = createCliRoot();
        writeData(root,
                baseUsers(),
                baseRooms(),
                "RESV|rv0001|user011|R101|2026-03-20|11:00|12:00|2|RESERVED|2026-03-20 09:00|-\n",
                "NOW|2026-03-20 10:50\n");

        String input = lines(
                "2",
                "user011",
                "12345",
                "6",
                "rv0001",
                "0",
                "0");
        String output = runCli(root, input);

        assertContains(output, "체크인이 완료되었습니다.");
        assertFileContains(root, "reservations.txt", "RESV|rv0001|user011|R101|2026-03-20|11:00|12:00|2|CHECKED_IN|2026-03-20 09:00|2026-03-20 10:50");
    }

    private static void testCheckInClosedRoomRejected() throws Exception {
        Path root = createCliRoot();
        writeData(root,
                baseUsers(),
                "ROOM|R101|A룸|4|PROJECTOR|CLOSED\n"
                        + "ROOM|R102|B룸|6|MONITOR|OPEN\n",
                "RESV|rv0001|user011|R101|2026-03-20|11:00|12:00|2|RESERVED|2026-03-20 09:00|-\n",
                "NOW|2026-03-20 10:55\n");

        String input = lines(
                "2",
                "user011",
                "12345",
                "6",
                "rv0001",
                "0",
                "0");
        String output = runCli(root, input);

        assertContains(output, "오류: 운영 중이 아닌 룸은 체크인할 수 없습니다.");
    }

    private static void testAdminTimeChangeRejectsPastAndAppliesTransitions() throws Exception {
        Path root = createCliRoot();
        writeData(root,
                baseUsers(),
                baseRooms(),
                "RESV|rv0001|user011|R101|2026-03-20|11:00|12:00|2|RESERVED|2026-03-20 09:00|-\n"
                        + "RESV|rv0002|user022|R102|2026-03-20|09:00|10:00|2|CHECKED_IN|2026-03-20 08:30|2026-03-20 08:55\n",
                "NOW|2026-03-20 09:00\n");

        String input = lines(
                "2",
                "admin",
                "admin1234",
                "2",
                "2026-03-20 08:00",
                "2",
                "2026-03-20 11:20",
                "0",
                "0");
        String output = runCli(root, input);

        assertContains(output, "오류: 현재 시각은 과거로 되돌릴 수 없습니다.");
        assertContains(output, "현재 시각이 변경되었습니다.");
        assertContains(output, "RESERVED -> NO_SHOW : 1건");
        assertContains(output, "CHECKED_IN -> COMPLETED : 1건");
        assertFileContains(root, "reservations.txt", "RESV|rv0001|user011|R101|2026-03-20|11:00|12:00|2|NO_SHOW");
        assertFileContains(root, "reservations.txt", "RESV|rv0002|user022|R102|2026-03-20|09:00|10:00|2|COMPLETED");
    }

    private static void testManualMoveSameRoomRejectedAndThenSucceeds() throws Exception {
        Path root = createCliRoot();
        writeData(root,
                baseUsers(),
                baseRooms(),
                "RESV|rv0001|user011|R101|2026-03-21|11:00|12:00|2|RESERVED|2026-03-20 09:00|-\n",
                "NOW|2026-03-20 09:00\n");

        String input = lines(
                "2",
                "admin",
                "admin1234",
                "5",
                "rv0001",
                "R101",
                "5",
                "rv0001",
                "R102",
                "0",
                "0");
        String output = runCli(root, input);

        assertContains(output, "오류: 현재 룸과 다른 룸으로만 이동할 수 있습니다.");
        assertContains(output, "예약 조정이 완료되었습니다.");
        assertFileContains(root, "reservations.txt", "RESV|rv0001|user011|R102|2026-03-21|11:00|12:00|2|RESERVED");
    }

    private static void testCapacityChangeWithHistoricalCompletedReservation() throws Exception {
        Path root = createCliRoot();
        writeData(root,
                baseUsers(),
                "ROOM|R101|A룸|6|PROJECTOR|OPEN\n"
                        + "ROOM|R102|B룸|6|MONITOR|OPEN\n",
                "RESV|rv0001|user011|R101|2026-03-19|11:00|12:00|6|COMPLETED|2026-03-18 09:00|2026-03-19 10:55\n",
                "NOW|2026-03-20 09:00\n");

        String output = runCli(root, lines(
                "2",
                "admin",
                "admin1234",
                "6",
                "2",
                "R101",
                "4",
                "0",
                "0",
                "0"));

        assertContains(output, "룸 최대 수용 인원이 변경되었습니다.");
        assertFileContains(root, "rooms.txt", "ROOM|R101|A룸|4|PROJECTOR|OPEN");

        String restartOutput = runCli(root, "0\n");
        assertNotContains(restartOutput, "[파일 오류]");
        assertContains(restartOutput, "[비로그인 메뉴]");
    }

    private static void testCapacityChangeImpactedSameRoomRejectedAndMoveSuccess() throws Exception {
        Path root = createCliRoot();
        writeData(root,
                baseUsers(),
                "ROOM|R101|A룸|6|PROJECTOR|OPEN\n"
                        + "ROOM|R102|B룸|6|MONITOR|OPEN\n",
                "RESV|rv0001|user011|R101|2026-03-21|11:00|12:00|6|RESERVED|2026-03-20 09:00|-\n",
                "NOW|2026-03-20 09:00\n");

        String output = runCli(root, lines(
                "2",
                "admin",
                "admin1234",
                "6",
                "2",
                "R101",
                "4",
                "1",
                "R101",
                "1",
                "R102",
                "0",
                "0",
                "0"));

        assertContains(output, "영향 예약이 있어 처리 흐름을 시작합니다.");
        assertContains(output, "오류: 현재 룸과 다른 룸으로만 이동할 수 있습니다.");
        assertContains(output, "룸 최대 수용 인원이 변경되었습니다.");
        assertFileContains(root, "rooms.txt", "ROOM|R101|A룸|4|PROJECTOR|OPEN");
        assertFileContains(root, "reservations.txt", "RESV|rv0001|user011|R102|2026-03-21|11:00|12:00|6|RESERVED");
    }

    private static void testCapacityChangeRollbackRestoresState() throws Exception {
        Path root = createCliRoot();
        writeData(root,
                baseUsers(),
                "ROOM|R101|A룸|6|PROJECTOR|OPEN\n"
                        + "ROOM|R102|B룸|6|MONITOR|OPEN\n",
                "RESV|rv0001|user011|R101|2026-03-21|11:00|12:00|6|RESERVED|2026-03-20 09:00|-\n",
                "NOW|2026-03-20 09:00\n");

        String output = runCli(root, lines(
                "2",
                "admin",
                "admin1234",
                "6",
                "2",
                "R101",
                "4",
                "0",
                "0",
                "0",
                "0"));

        assertContains(output, "룸 컨디션 변경을 취소하여 원상복구했습니다.");
        assertFileContains(root, "rooms.txt", "ROOM|R101|A룸|6|PROJECTOR|OPEN");
        assertFileContains(root, "reservations.txt", "RESV|rv0001|user011|R101|2026-03-21|11:00|12:00|6|RESERVED");
    }

    private static void testCloseRoomWithCheckedInReservationRejected() throws Exception {
        Path root = createCliRoot();
        writeData(root,
                baseUsers(),
                baseRooms(),
                "RESV|rv0001|user011|R101|2026-03-20|09:00|10:00|2|CHECKED_IN|2026-03-20 08:00|2026-03-20 08:55\n",
                "NOW|2026-03-20 09:30\n");

        String output = runCli(root, lines(
                "2",
                "admin",
                "admin1234",
                "6",
                "3",
                "R101",
                "0",
                "0",
                "0"));

        assertContains(output, "오류: 현재 체크인 중인 예약이 있어 즉시 휴업할 수 없습니다.");
        assertFileContains(root, "rooms.txt", "ROOM|R101|A룸|4|PROJECTOR|OPEN");
    }

    private static void testCloseRoomImpactedSameRoomRejectedAndMoveSuccess() throws Exception {
        Path root = createCliRoot();
        writeData(root,
                baseUsers(),
                baseRooms(),
                "RESV|rv0001|user011|R101|2026-03-21|11:00|12:00|2|RESERVED|2026-03-20 09:00|-\n",
                "NOW|2026-03-20 09:00\n");

        String output = runCli(root, lines(
                "2",
                "admin",
                "admin1234",
                "6",
                "3",
                "R101",
                "1",
                "R101",
                "1",
                "R102",
                "0",
                "0",
                "0"));

        assertContains(output, "영향 예약이 있어 처리 흐름을 시작합니다.");
        assertContains(output, "오류: 현재 룸과 다른 룸으로만 이동할 수 있습니다.");
        assertContains(output, "룸이 임시 휴업 상태(CLOSED)로 변경되었습니다.");
        assertFileContains(root, "rooms.txt", "ROOM|R101|A룸|4|PROJECTOR|CLOSED");
        assertFileContains(root, "reservations.txt", "RESV|rv0001|user011|R102|2026-03-21|11:00|12:00|2|RESERVED");
    }

    private static void testOpenRoomSuccess() throws Exception {
        Path root = createCliRoot();
        writeData(root,
                baseUsers(),
                "ROOM|R101|A룸|4|PROJECTOR|CLOSED\n"
                        + "ROOM|R102|B룸|6|MONITOR|OPEN\n",
                "",
                "NOW|2026-03-20 09:00\n");

        String output = runCli(root, lines(
                "2",
                "admin",
                "admin1234",
                "6",
                "4",
                "R101",
                "0",
                "0",
                "0"));

        assertContains(output, "룸 운영이 재개되었습니다.");
        assertFileContains(root, "rooms.txt", "ROOM|R101|A룸|4|PROJECTOR|OPEN");
    }

    private static Path createCliRoot() throws Exception {
        Path root = Files.createTempDirectory("study-room-cli-e2e-");
        Files.copy(BUILT_JAR, root.resolve("study-room-cli.jar"));
        return root;
    }

    private static void writeData(Path root,
                                  String users,
                                  String rooms,
                                  String reservations,
                                  String now) throws Exception {
        Path data = root.resolve("data");
        Files.createDirectories(data);
        Files.writeString(data.resolve("users.txt"), users, StandardCharsets.UTF_8);
        Files.writeString(data.resolve("rooms.txt"), rooms, StandardCharsets.UTF_8);
        Files.writeString(data.resolve("reservations.txt"), reservations, StandardCharsets.UTF_8);
        Files.writeString(data.resolve("system_time.txt"), now, StandardCharsets.UTF_8);
    }

    private static String runCli(Path root, String input) throws Exception {
        ProcessBuilder builder = new ProcessBuilder("java", "-jar", "study-room-cli.jar");
        builder.directory(root.toFile());
        builder.redirectErrorStream(true);
        Process process = builder.start();
        try (OutputStream stream = process.getOutputStream()) {
            stream.write(input.getBytes(StandardCharsets.UTF_8));
        }

        if (!process.waitFor(20, TimeUnit.SECONDS)) {
            process.destroyForcibly();
            throw new AssertionError("CLI timed out for root: " + root);
        }
        return new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
    }

    private static String lines(String... lines) {
        return String.join("\n", lines) + "\n";
    }

    private static String baseUsers() {
        return users("user011", "12345", "본수")
                + "USER|user022|abcde|다른회원|member|ACTIVE\n";
    }

    private static String users(String userId, String password, String displayName) {
        return "USER|admin|admin1234|관리자|admin|ACTIVE\n"
                + "USER|" + userId + "|" + password + "|" + displayName + "|member|ACTIVE\n";
    }

    private static String baseRooms() {
        return "ROOM|R101|A룸|4|PROJECTOR|OPEN\n"
                + "ROOM|R102|B룸|6|MONITOR|OPEN\n"
                + "ROOM|R103|C룸|8|-|OPEN\n";
    }

    private static void assertContains(String output, String expected) {
        if (!output.contains(expected)) {
            throw new AssertionError("Expected output to contain: " + expected + "\nActual output:\n" + output);
        }
    }

    private static void assertNotContains(String output, String unexpected) {
        if (output.contains(unexpected)) {
            throw new AssertionError("Did not expect output to contain: " + unexpected + "\nActual output:\n" + output);
        }
    }

    private static void assertFileContains(Path root, String fileName, String expected) throws Exception {
        String content = Files.readString(root.resolve("data").resolve(fileName), StandardCharsets.UTF_8);
        if (!content.contains(expected)) {
            throw new AssertionError("Expected " + fileName + " to contain: " + expected + "\nActual content:\n" + content);
        }
    }
}
