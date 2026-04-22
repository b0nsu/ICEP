package ku.com;

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
        testInvalidUserIdSyntaxStopsStartup();
        testDuplicateSystemTimeStopsStartup();
        testSignupDuplicateAndSuccess();
        testSignupRejectsShortLoginId();
        testSignupRejectsShortPassword();
        testLoginFailureAndSuccess();
        testMemberTimeChangeSuccess();
        testAvailableRoomQueryFiltersRooms();
        testAvailableRoomQueryRejectsPastStart();
        testAvailableRoomQueryRejectsUserOverlap();
        testCreateReservationSuccess();
        testCreateReservationAllowsFourHourBoundary();
        testCreateReservationRejectsHalfHourTime();
        testCreateReservationRejectsTooLongWindow();
        testCreateReservationRejectsRoomOverlap();
        testCreateReservationRejectsUserOverlap();
        testCancelDeletesReservation();
        testCancelAfterStartRejected();
        testCheckInTooEarlyRejected();
        testCheckInBoundarySuccess();
        testCheckInClosedRoomRejected();
        testAdminTimeChangeRejectsPastAndAppliesTransitions();
        testAllReservationsShowsUserNames();
        testManualMoveRejectsClosedRoom();
        testManualMoveSameRoomRejectedAndThenSucceeds();
        testCapacityChangeWithHistoricalCompletedReservation();
        testCapacityChangeImpactedSameRoomRejectedAndMoveSuccess();
        testCapacityChangeRollbackRestoresState();
        testCloseRoomWithCheckedInReservationRejected();
        testCloseRoomImpactedDeleteAndSucceeds();
        testOpenRoomSuccess();

        System.out.println("Regression tests passed.");
    }

    private static void testAutoNoShowTransition() throws Exception {
        Path root = Files.createTempDirectory("study-room-test-");
        writeData(root,
                users("user011", "pw1234", "bonsu"),
                "ROOM|R101|A룸|4|OPEN\n",
                "RESV|rv0001|user011|R101|2026-03-20|11:00|12:00|2|RESERVED|2026-03-19 09:00|-\n",
                "NOW|2026-03-20 11:11\n");

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
                users("user011", "pw1234", "bonsu"),
                "ROOM|R101|A룸|4|OPEN\n",
                "RESV|rv0001|user011|R101|2026-03-20|11:00|12:00|2|RESERVED|2026-03-19 09:00|-\n"
                        + "RESV|rv0003|user011|R101|2026-03-21|11:00|12:00|2|RESERVED|2026-03-19 10:00|-\n",
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
                users("user011", "pw1234", "bonsu"),
                "ROOM|R101|A룸|4|OPEN\n",
                "RESV|rv0001|user011|R101|2026-03-20|11:00|12:00|6|COMPLETED|2026-03-19 09:00|2026-03-20 10:55\n",
                "NOW|2026-03-20 13:00\n");

        SystemData dataSet = new TextDataStore(root).loadAll();
        if (dataSet.reservations.get("rv0001").status != ReservationStatus.COMPLETED) {
            throw new AssertionError("Expected completed reservation to remain loadable after capacity reduction.");
        }
    }

    private static void testSameRoomMoveRejected() throws Exception {
        Path root = Files.createTempDirectory("study-room-move-test-");
        writeData(root,
                users("user011", "pw1234", "bonsu"),
                "ROOM|R101|A룸|4|OPEN\n"
                        + "ROOM|R102|B룸|6|OPEN\n",
                "RESV|rv0001|user011|R101|2026-03-21|11:00|12:00|2|RESERVED|2026-03-20 09:00|-\n",
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
        assertFileContains(root, "users.txt", "USER|user001|admin|admin1234|admin|admin");
        assertFileContains(root, "rooms.txt", "ROOM|R101|A룸|4|OPEN");
        assertFileContains(root, "system_time.txt", "NOW|2026-03-20 09:00");
    }

    private static void testInvalidFileSyntaxStopsStartup() throws Exception {
        Path root = createCliRoot();
        writeData(root,
                users("user011", "pw1234", "bonsu"),
                baseRooms(),
                "RESV|broken|line\n",
                "NOW|2026-03-20 09:00\n");

        String output = runCli(root, "");
        assertContains(output, "[파일 오류] reservations.txt");
    }

    private static void testInvalidUserIdSyntaxStopsStartup() throws Exception {
        Path root = createCliRoot();
        writeData(root,
                "USER|user001|admin|admin1234|admin|admin\n"
                        + "USER|abcd|user011|pw1234|bonsu|member\n",
                baseRooms(),
                "",
                "NOW|2026-03-20 09:00\n");

        String output = runCli(root, "");
        assertContains(output, "[파일 오류] users.txt 2행: userId 형식이 올바르지 않습니다.");
    }

    private static void testDuplicateSystemTimeStopsStartup() throws Exception {
        Path root = createCliRoot();
        writeData(root, baseUsers(), baseRooms(), "", "NOW|2026-03-20 09:00\nNOW|2026-03-20 10:00\n");

        String output = runCli(root, "");
        assertContains(output, "[파일 오류] system_time.txt");
    }

    private static void testSignupDuplicateAndSuccess() throws Exception {
        Path root = createCliRoot();
        writeData(root, baseUsers(), baseRooms(), "", "NOW|2026-03-20 09:00\n");

        String input = lines(
                "1",
                "user023",
                "pw12",
                "bonsu",
                "0");
        String output = runCli(root, input);

        assertContains(output, "회원가입이 완료되었습니다.");
        assertFileContains(root, "users.txt", "USER|user023|user023|pw12|bonsu|member");
    }

    private static void testSignupRejectsShortLoginId() throws Exception {
        Path root = createCliRoot();
        writeData(root, baseUsers(), baseRooms(), "", "NOW|2026-03-20 09:00\n");

        String output = runCli(root, lines(
                "1",
                "abc",
                "pw12",
                "bonsu",
                "0"));

        assertContains(output, "오류: 로그인 ID는 영문자로 시작하고 영문자/숫자/_ 만 사용하여 4~20자로 입력해야 합니다.");
    }

    private static void testSignupRejectsShortPassword() throws Exception {
        Path root = createCliRoot();
        writeData(root, baseUsers(), baseRooms(), "", "NOW|2026-03-20 09:00\n");

        String output = runCli(root, lines(
                "1",
                "user023",
                "abc",
                "bonsu",
                "0"));

        assertContains(output, "오류: 비밀번호는 4~20자로 입력해야 합니다.");
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
                "pw1234",
                "0",
                "0");
        String output = runCli(root, input);

        assertContains(output, "오류: 비밀번호가 일치하지 않습니다.");
        assertContains(output, "로그인 성공: bonsu (role: member)");
    }

    private static void testMemberTimeChangeSuccess() throws Exception {
        Path root = createCliRoot();
        writeData(root, baseUsers(), baseRooms(), "", "NOW|2026-03-20 09:00\n");

        String output = runCli(root, lines(
                "2",
                "user011",
                "pw1234",
                "1",
                "2026-03-20 10:00",
                "0",
                "0"));

        assertContains(output, "현재 시각이 변경되었습니다.");
        assertFileContains(root, "system_time.txt", "NOW|2026-03-20 10:00");
    }

    private static void testAvailableRoomQueryFiltersRooms() throws Exception {
        Path root = createCliRoot();
        writeData(root,
                baseUsers(),
                "ROOM|R101|A룸|4|OPEN\n"
                        + "ROOM|R102|B룸|6|CLOSED\n"
                        + "ROOM|R103|C룸|2|OPEN\n",
                "RESV|rv0001|user022|R101|2026-03-20|13:00|15:00|2|RESERVED|2026-03-20 09:00|-\n",
                "NOW|2026-03-20 09:00\n");

        String input = lines(
                "2",
                "user011",
                "pw1234",
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

    private static void testAvailableRoomQueryRejectsPastStart() throws Exception {
        Path root = createCliRoot();
        writeData(root, baseUsers(), baseRooms(), "", "NOW|2026-03-20 14:00\n");

        String input = lines(
                "2",
                "user011",
                "pw1234",
                "2",
                "2026-03-20",
                "13:00",
                "15:00",
                "2",
                "0",
                "0");
        String output = runCli(root, input);

        assertContains(output, "오류: 예약 시작 시각은 현재 가상 시각보다 미래여야 합니다.");
    }

    private static void testAvailableRoomQueryRejectsUserOverlap() throws Exception {
        Path root = createCliRoot();
        writeData(root,
                baseUsers(),
                baseRooms(),
                "RESV|rv0001|user011|R101|2026-03-20|13:00|15:00|2|RESERVED|2026-03-20 09:00|-\n",
                "NOW|2026-03-20 09:00\n");

        String input = lines(
                "2",
                "user011",
                "pw1234",
                "2",
                "2026-03-20",
                "13:00",
                "15:00",
                "2",
                "0",
                "0");
        String output = runCli(root, input);

        assertContains(output, "오류: 같은 시간대에 이미 다른 예약이 있습니다.");
    }

    private static void testCreateReservationSuccess() throws Exception {
        Path root = createCliRoot();
        writeData(root, baseUsers(), baseRooms(), "", "NOW|2026-03-20 09:00\n");

        String input = lines(
                "2",
                "user011",
                "pw1234",
                "3",
                "2026-03-20",
                "13:00",
                "15:00",
                "2",
                "R102",
                "0",
                "0");
        String output = runCli(root, input);

        assertContains(output, "예약이 완료되었습니다.");
        assertContains(output, "예약번호: rv0001");
        assertFileContains(root, "reservations.txt", "RESV|rv0001|user011|R102|2026-03-20|13:00|15:00|2|RESERVED|2026-03-20 09:00|-");
    }

    private static void testCreateReservationAllowsFourHourBoundary() throws Exception {
        Path root = createCliRoot();
        writeData(root, baseUsers(), baseRooms(), "", "NOW|2026-03-20 09:00\n");

        String output = runCli(root, lines(
                "2",
                "user011",
                "pw1234",
                "3",
                "2026-03-20",
                "13:00",
                "17:00",
                "2",
                "R102",
                "0",
                "0"));

        assertContains(output, "예약이 완료되었습니다.");
        assertContains(output, "예약번호: rv0001");
        assertFileContains(root, "reservations.txt", "RESV|rv0001|user011|R102|2026-03-20|13:00|17:00|2|RESERVED|2026-03-20 09:00|-");
    }

    private static void testCreateReservationRejectsHalfHourTime() throws Exception {
        Path root = createCliRoot();
        writeData(root, baseUsers(), baseRooms(), "", "NOW|2026-03-20 09:00\n");

        String input = lines(
                "2",
                "user011",
                "pw1234",
                "3",
                "2026-03-20",
                "13:30",
                "13:00",
                "15:00",
                "2",
                "R102",
                "0",
                "0");
        String output = runCli(root, input);

        assertContains(output, "오류: 예약 시각은 1시간 단위여야 합니다.");
    }

    private static void testCreateReservationRejectsTooLongWindow() throws Exception {
        Path root = createCliRoot();
        writeData(root, baseUsers(), baseRooms(), "", "NOW|2026-03-20 09:00\n");

        String output = runCli(root, lines(
                "2",
                "user011",
                "pw1234",
                "3",
                "2026-03-20",
                "13:00",
                "18:00",
                "2",
                "R102",
                "0",
                "0"));

        assertContains(output, "오류: 예약 길이는 1시간, 2시간, 3시간, 4시간 중 하나여야 합니다.");
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
                "pw1234",
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
                "pw1234",
                "3",
                "2026-03-20",
                "13:00",
                "15:00",
                "2",
                "R102",
                "0",
                "0");
        String output = runCli(root, input);

        assertContains(output, "오류: 같은 시간대에 이미 다른 예약이 있습니다.");
    }

    private static void testCancelDeletesReservation() throws Exception {
        Path root = createCliRoot();
        writeData(root,
                baseUsers(),
                baseRooms(),
                "RESV|rv0001|user011|R101|2026-03-20|13:00|15:00|2|RESERVED|2026-03-20 09:00|-\n",
                "NOW|2026-03-20 09:00\n");

        String input = lines(
                "2",
                "user011",
                "pw1234",
                "4",
                "rv0001",
                "0",
                "0");
        String output = runCli(root, input);

        assertContains(output, "예약이 취소되었습니다.");
        assertFileNotContains(root, "reservations.txt", "rv0001");
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
                "pw1234",
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
                "pw1234",
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
                "NOW|2026-03-20 11:10\n");

        String input = lines(
                "2",
                "user011",
                "pw1234",
                "6",
                "rv0001",
                "0",
                "0");
        String output = runCli(root, input);

        assertContains(output, "체크인이 완료되었습니다.");
        assertFileContains(root, "reservations.txt", "RESV|rv0001|user011|R101|2026-03-20|11:00|12:00|2|CHECKED_IN|2026-03-20 09:00|2026-03-20 11:10");
    }

    private static void testCheckInClosedRoomRejected() throws Exception {
        Path root = createCliRoot();
        writeData(root,
                baseUsers(),
                "ROOM|R101|A룸|4|CLOSED\n"
                        + "ROOM|R102|B룸|6|OPEN\n",
                "RESV|rv0001|user011|R101|2026-03-20|11:00|12:00|2|RESERVED|2026-03-20 09:00|-\n",
                "NOW|2026-03-20 10:55\n");

        String input = lines(
                "2",
                "user011",
                "pw1234",
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
                "1",
                "2026-03-20 08:00",
                "1",
                "2026-03-20 11:11",
                "0",
                "0");
        String output = runCli(root, input);

        assertContains(output, "오류: 현재 시각은 과거로 되돌릴 수 없습니다.");
        assertContains(output, "현재 시각이 변경되었습니다.");
        assertContains(output, "RESERVED -> NO_SHOW : 1건");
        assertContains(output, "CHECKED_IN -> COMPLETED : 1건");
        assertFileContains(root, "reservations.txt", "RESV|rv0001|user011|R101|2026-03-20|11:00|12:00|2|NO_SHOW|2026-03-20 09:00|-");
        assertFileContains(root, "reservations.txt", "RESV|rv0002|user022|R102|2026-03-20|09:00|10:00|2|COMPLETED|2026-03-20 08:30|2026-03-20 08:55");
    }

    private static void testAllReservationsShowsUserNames() throws Exception {
        Path root = createCliRoot();
        writeData(root,
                baseUsers(),
                baseRooms(),
                "RESV|rv0001|user011|R101|2026-03-21|11:00|12:00|2|RESERVED|2026-03-20 09:00|-\n"
                        + "RESV|rv0002|user022|R102|2026-03-21|13:00|14:00|3|RESERVED|2026-03-20 09:30|-\n",
                "NOW|2026-03-20 09:00\n");

        String output = runCli(root, lines(
                "2",
                "admin",
                "admin1234",
                "2",
                "0",
                "0"));

        assertContains(output, "resvId");
        assertContains(output, "userId");
        assertContains(output, "userName");
        assertContains(output, "user011");
        assertContains(output, "user022");
        assertContains(output, "bonsu");
        assertContains(output, "minseo");
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
                "3",
                "rv0001",
                "R101",
                "3",
                "rv0001",
                "R102",
                "0",
                "0");
        String output = runCli(root, input);

        assertContains(output, "오류: 현재 룸과 다른 룸으로만 이동할 수 있습니다.");
        assertContains(output, "변경 전 예약 레코드:");
        assertContains(output, "변경 후 예약 레코드:");
        assertContains(output, "예약 조정이 완료되었습니다.");
        assertFileContains(root, "reservations.txt", "RESV|rv0001|user011|R102|2026-03-21|11:00|12:00|2|RESERVED|2026-03-20 09:00|-");
    }

    private static void testManualMoveRejectsClosedRoom() throws Exception {
        Path root = createCliRoot();
        writeData(root,
                baseUsers(),
                "ROOM|R101|A룸|4|OPEN\n"
                        + "ROOM|R102|B룸|6|CLOSED\n",
                "RESV|rv0001|user011|R101|2026-03-21|11:00|12:00|2|RESERVED|2026-03-20 09:00|-\n",
                "NOW|2026-03-20 09:00\n");

        String output = runCli(root, lines(
                "2",
                "admin",
                "admin1234",
                "3",
                "rv0001",
                "R102",
                "0",
                "0"));

        assertContains(output, "오류: 대상 룸이 OPEN 상태가 아니어서 이동할 수 없습니다.");
    }

    private static void testCapacityChangeWithHistoricalCompletedReservation() throws Exception {
        Path root = createCliRoot();
        writeData(root,
                baseUsers(),
                "ROOM|R101|A룸|6|OPEN\n"
                        + "ROOM|R102|B룸|6|OPEN\n",
                "RESV|rv0001|user011|R101|2026-03-19|11:00|12:00|6|COMPLETED|2026-03-18 09:00|2026-03-19 10:55\n",
                "NOW|2026-03-20 09:00\n");

        String output = runCli(root, lines(
                "2",
                "admin",
                "admin1234",
                "4",
                "2",
                "R101",
                "4",
                "0",
                "0",
                "0"));

        assertContains(output, "변경 전 ROOM 레코드:");
        assertContains(output, "변경 후 ROOM 레코드:");
        assertContains(output, "룸 최대 수용 인원이 변경되었습니다.");
        assertFileContains(root, "rooms.txt", "ROOM|R101|A룸|4|OPEN");
    }

    private static void testCapacityChangeImpactedSameRoomRejectedAndMoveSuccess() throws Exception {
        Path root = createCliRoot();
        writeData(root,
                baseUsers(),
                "ROOM|R101|A룸|6|OPEN\n"
                        + "ROOM|R102|B룸|6|OPEN\n",
                "RESV|rv0001|user011|R101|2026-03-21|11:00|12:00|6|RESERVED|2026-03-20 09:00|-\n",
                "NOW|2026-03-20 09:00\n");

        String output = runCli(root, lines(
                "2",
                "admin",
                "admin1234",
                "4",
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

        assertContains(output, "현재 CHECKED_IN 예약 인원 검사 통과");
        assertContains(output, "영향 예약이 있어 처리 흐름을 시작합니다.");
        assertContains(output, "오류: 현재 룸과 다른 룸으로만 이동할 수 있습니다.");
        assertContains(output, "영향 예약 처리가 완료되었습니다.");
        assertContains(output, "변경 전 RESV 레코드:");
        assertContains(output, "변경 후 RESV 레코드:");
        assertContains(output, "변경 전 ROOM 레코드:");
        assertContains(output, "변경 후 ROOM 레코드:");
        assertContains(output, "룸 최대 수용 인원이 변경되었습니다.");
        assertFileContains(root, "rooms.txt", "ROOM|R101|A룸|4|OPEN");
        assertFileContains(root, "reservations.txt", "RESV|rv0001|user011|R102|2026-03-21|11:00|12:00|6|RESERVED|2026-03-20 09:00|-");
    }

    private static void testCapacityChangeRollbackRestoresState() throws Exception {
        Path root = createCliRoot();
        writeData(root,
                baseUsers(),
                "ROOM|R101|A룸|6|OPEN\n"
                        + "ROOM|R102|B룸|6|OPEN\n",
                "RESV|rv0001|user011|R101|2026-03-21|11:00|12:00|6|RESERVED|2026-03-20 09:00|-\n",
                "NOW|2026-03-20 09:00\n");

        String output = runCli(root, lines(
                "2",
                "admin",
                "admin1234",
                "4",
                "2",
                "R101",
                "4",
                "0",
                "0",
                "0",
                "0"));

        assertContains(output, "룸 컨디션 변경을 취소하여 원상복구했습니다.");
        assertFileContains(root, "rooms.txt", "ROOM|R101|A룸|6|OPEN");
        assertFileContains(root, "reservations.txt", "RESV|rv0001|user011|R101|2026-03-21|11:00|12:00|6|RESERVED|2026-03-20 09:00|-");
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
                "4",
                "3",
                "R101",
                "0",
                "0",
                "0"));

        assertContains(output, "오류: 현재 체크인 중인 예약이 있어 즉시 휴업할 수 없습니다.");
        assertFileContains(root, "rooms.txt", "ROOM|R101|A룸|4|OPEN");
    }

    private static void testCloseRoomImpactedDeleteAndSucceeds() throws Exception {
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
                "4",
                "3",
                "R101",
                "2",
                "0",
                "0",
                "0"));

        assertContains(output, "영향 예약이 있어 처리 흐름을 시작합니다.");
        assertContains(output, "변경 전 ROOM 레코드:");
        assertContains(output, "변경 후 ROOM 레코드:");
        assertContains(output, "룸이 임시 휴업 처리되었습니다.");
        assertFileContains(root, "rooms.txt", "ROOM|R101|A룸|4|CLOSED");
        assertFileNotContains(root, "reservations.txt", "rv0001");
    }

    private static void testOpenRoomSuccess() throws Exception {
        Path root = createCliRoot();
        writeData(root,
                baseUsers(),
                "ROOM|R101|A룸|4|CLOSED\n"
                        + "ROOM|R102|B룸|6|OPEN\n",
                "",
                "NOW|2026-03-20 09:00\n");

        String output = runCli(root, lines(
                "2",
                "admin",
                "admin1234",
                "4",
                "4",
                "R101",
                "0",
                "0",
                "0"));

        assertContains(output, "변경 전 ROOM 레코드:");
        assertContains(output, "변경 후 ROOM 레코드:");
        assertContains(output, "룸 운영이 재개되었습니다.");
        assertFileContains(root, "rooms.txt", "ROOM|R101|A룸|4|OPEN");
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
        return users("user011", "pw1234", "bonsu")
                + "USER|user022|user022|pw5678|minseo|member\n";
    }

    private static String users(String userId, String password, String userName) {
        return "USER|user001|admin|admin1234|admin|admin\n"
                + "USER|" + userId + "|" + userId + "|" + password + "|" + userName + "|member\n";
    }

    private static String baseRooms() {
        return "ROOM|R101|A룸|4|OPEN\n"
                + "ROOM|R102|B룸|6|OPEN\n"
                + "ROOM|R103|C룸|8|OPEN\n";
    }

    private static void assertContains(String output, String expected) {
        if (!output.contains(expected)) {
            throw new AssertionError("Expected output to contain: " + expected + "\nActual output:\n" + output);
        }
    }

    private static void assertFileContains(Path root, String fileName, String expected) throws Exception {
        String content = Files.readString(root.resolve("data").resolve(fileName), StandardCharsets.UTF_8);
        if (!content.contains(expected)) {
            throw new AssertionError("Expected " + fileName + " to contain: " + expected + "\nActual content:\n" + content);
        }
    }

    private static void assertFileNotContains(Path root, String fileName, String unexpected) throws Exception {
        String content = Files.readString(root.resolve("data").resolve(fileName), StandardCharsets.UTF_8);
        if (content.contains(unexpected)) {
            throw new AssertionError("Did not expect " + fileName + " to contain: " + unexpected + "\nActual content:\n" + content);
        }
    }
}
