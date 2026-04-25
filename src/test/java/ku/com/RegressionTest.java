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
        testGuestExit();
        testInvalidFileSyntaxStopsStartup();
        testInvalidUsersFieldCountStopsStartup();
        testInvalidUserIdSyntaxStopsStartup();
        testDuplicateUserIdStopsStartup();
        testDuplicateLoginIdStopsStartup();
        testInvalidRoleStopsStartup();
        testInvalidRoomsFieldCountStopsStartup();
        testInvalidRoomIdSyntaxStopsStartup();
        testDuplicateRoomIdStopsStartup();
        testInvalidRoomStatusStopsStartup();
        testEmptyRoomNameStopsStartup();
        testRoomRejectsZeroMaxCapacity();
        testRoomRejectsNegativeMaxCapacity();
        testRoomRejectsNonNumericMaxCapacity();
        testDuplicateSystemTimeStopsStartup();
        testInvalidSystemTimeFormatStopsStartup();
        testInvalidReservationIdSyntaxStopsStartup();
        testDuplicateReservationIdStopsStartup();
        testInvalidReservationStatusStopsStartup();
        testInvalidReservationStartTimeFormatStopsStartup();
        testInvalidReservationEndTimeFormatStopsStartup();
        testInvalidCreatedAtFormatStopsStartup();
        testInvalidCheckedInAtFormatStopsStartup();
        testReservationRejectsMissingUserReference();
        testReservationRejectsMissingRoomReference();
        testReservationRejectsMissingCheckedInAtForCheckedIn();
        testReservationRejectsCheckedInAtPresentForReserved();
        testReservationRejectsPartySizeOverRoomCapacity();
        testReservationRejectsRoomOverlapInData();
        testReservationRejectsUserOverlapInData();
        testSignupDuplicateAndSuccess();
        testSignupAcceptsBoundaryLengthFields();
        testSignupRejectsShortLoginId();
        testSignupRejectsNumericLeadingLoginId();
        testSignupRejectsSpecialCharacterLoginId();
        testSignupRejectsLongLoginId();
        testSignupRejectsShortUserName();
        testSignupRejectsLongUserName();
        testSignupRejectsNumericLeadingUserName();
        testSignupRejectsUnderscoreLeadingUserName();
        testSignupRejectsSpecialCharacterUserName();
        testSignupAcceptsCompositeUserName();
        testSignupAllowsDuplicateUserName();
        testSignupRejectsShortPassword();
        testSignupRejectsLongPassword();
        testSignupAcceptsMaxBoundaryPassword();
        testSignupRejectsDuplicateLoginId();
        testSignupRejectsUnderscoreLeadingLoginId();
        testSignupRejectsKoreanLeadingLoginId();
        testSignupAcceptsUppercaseDistinctLoginId();
        testLoginFailureAndSuccess();
        testLoginRejectsMissingLoginId();
        testGuestMenuRejectsEmptyInput();
        testGuestMenuRejectsWhitespaceOnlyInput();
        testGuestMenuRejectsNegativeInput();
        testGuestMenuRejectsAlphabeticInput();
        testGuestMenuRejectsFloatLikeInput();
        testGuestMenuRejectsLeadingSpaceInput();
        testGuestMenuRejectsTrailingSpaceInput();
        testGuestMenuRejectsTabbedInput();
        testGuestMenuRejectsMiddleSpaceInput();
        testGuestMenuRejectsOverflowInput();
        testGuestMenuAcceptsLeadingZeroChoice();
        testMenuInvalidChoiceRepromptsInPlace();
        testMemberMenuInvalidChoiceReprompts();
        testAdminMenuInvalidChoiceReprompts();
        testMemberLogoutReturnsToGuestMenu();
        testAdminLogoutReturnsToGuestMenu();
        testMemberTimeChangeSuccess();
        testChangeCurrentTimeRejectsInvalidDateTimeFormat();
        testChangeCurrentTimeRejectsIsoSeparator();
        testChangeCurrentTimeRejectsDoubleSpaceDateTime();
        testChangeCurrentTimeAcceptsSameValue();
        testChangeCurrentTimeRejectsDateOnly();
        testChangeCurrentTimeRejectsTimeOnly();
        testChangeCurrentTimeRejectsSeconds();
        testChangeCurrentTimeRejectsOneDigitHour();
        testChangeCurrentTimeRejectsMinute60();
        testChangeCurrentTimeAcceptsLeapDay();
        testMemberTimeChangeRejectsPast();
        testAvailableRoomQueryFiltersRooms();
        testAvailableRoomQueryListsMatchingRooms();
        testAvailableRoomQueryRejectsInvalidDate();
        testAvailableRoomQueryRejectsSlashDate();
        testAvailableRoomQueryRejectsOneDigitHour();
        testAvailableRoomQueryRejectsHour24();
        testAvailableRoomQueryRejectsEndBeforeStart();
        testAvailableRoomQueryRejectsZeroPartySize();
        testAvailableRoomQueryRejectsPastStart();
        testAvailableRoomQueryRejectsUserOverlap();
        testCreateReservationSuccess();
        testCreateReservationAllowsOneHourBoundary();
        testCreateReservationAllowsFourHourBoundary();
        testCreateReservationRejectsInvalidDate();
        testCreateReservationRejectsHalfHourTime();
        testCreateReservationRejectsTooLongWindow();
        testCreateReservationRejectsInvalidRoomIdFormat();
        testCreateReservationRejectsNonexistentRoomId();
        testCreateReservationRejectsZeroYearDate();
        testCreateReservationRejectsZeroPartySize();
        testCreateReservationRejectsCapacityOverflow();
        testCreateReservationRejectsRoomOverlap();
        testCreateReservationRejectsUserOverlap();
        testCancelDeletesReservation();
        testCancelRejectsOtherUsersReservation();
        testCancelRejectsMissingReservation();
        testCancelAfterStartRejected();
        testMyReservationsEmptyAndSorted();
        testCheckInTooEarlyRejected();
        testCheckInLowerBoundarySuccess();
        testCheckInBoundarySuccess();
        testCheckInAtStartTimeSuccess();
        testCheckInRejectsOtherUsersReservation();
        testCheckInRejectsMissingReservation();
        testCheckInAfterNoShowTransitionRejected();
        testCheckInClosedRoomRejected();
        testAdminTimeChangeRejectsPastAndAppliesTransitions();
        testAllReservationsEmpty();
        testAllReservationsShowsUserNamesCheckedInAtAndSortedRows();
        testRoomListShowsAllRooms();
        testRoomConditionInputErrorReturnsToRoomConditionMenu();
        testManualMoveRejectsClosedRoom();
        testManualMoveRejectsCapacityInsufficient();
        testManualMoveRejectsTargetRoomOverlap();
        testManualMoveRejectsMissingReservation();
        testManualMoveSameRoomRejectedAndThenSucceeds();
        testCapacityChangeWithHistoricalCompletedReservation();
        testCapacityChangeRejectsCheckedInOverCapacity();
        testCapacityChangeCheckInWindowReservedHandledAsImpacted();
        testCapacityChangeImpactedSameRoomRejectedAndMoveSuccess();
        testCapacityChangeRollbackRestoresState();
        testCloseRoomWithCheckedInReservationRejected();
        testCloseRoomCheckInWindowReservedHandledAsImpacted();
        testCloseRoomImpactedDeleteAndSucceeds();
        testOpenRoomSuccess();
        testSignupRejectsLeadingIdeographicSpaceInPassword();
        testDataFileRejectsZeroYearDate();
        testDataFileRejectsNegativeYearDate();

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

    private static void testGuestExit() throws Exception {
        Path root = createCliRoot();
        writeData(root, baseUsers(), baseRooms(), "", "NOW|2026-03-20 09:00\n");

        String output = runCli(root, "0\n");
        assertContains(output, "프로그램을 종료합니다.");
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

    private static void testInvalidUsersFieldCountStopsStartup() throws Exception {
        Path root = createCliRoot();
        writeData(root,
                "USER|user001|admin|admin1234|admin\n",
                baseRooms(),
                "",
                "NOW|2026-03-20 09:00\n");

        String output = runCli(root, "");
        assertContains(output, "[파일 오류] users.txt 1행: 필드 개수가 올바르지 않습니다.");
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

    private static void testDuplicateUserIdStopsStartup() throws Exception {
        Path root = createCliRoot();
        writeData(root,
                "USER|user001|admin|admin1234|admin|admin\n"
                        + "USER|user001|user011|pw1234|bonsu|member\n",
                baseRooms(),
                "",
                "NOW|2026-03-20 09:00\n");

        String output = runCli(root, "");
        assertContains(output, "[파일 오류] users.txt 2행: 중복된 userId가 존재합니다.");
    }

    private static void testDuplicateLoginIdStopsStartup() throws Exception {
        Path root = createCliRoot();
        writeData(root,
                "USER|user001|admin|admin1234|admin|admin\n"
                        + "USER|user011|admin|pw1234|bonsu|member\n",
                baseRooms(),
                "",
                "NOW|2026-03-20 09:00\n");

        String output = runCli(root, "");
        assertContains(output, "[파일 오류] users.txt 2행: 중복된 loginId가 존재합니다.");
    }

    private static void testInvalidRoleStopsStartup() throws Exception {
        Path root = createCliRoot();
        writeData(root,
                "USER|user001|admin|admin1234|admin|superadmin\n",
                baseRooms(),
                "",
                "NOW|2026-03-20 09:00\n");

        String output = runCli(root, "");
        assertContains(output, "[파일 오류] users.txt 1행: role 값은 member 또는 admin 이어야 합니다.");
    }

    private static void testInvalidRoomsFieldCountStopsStartup() throws Exception {
        Path root = createCliRoot();
        writeData(root,
                baseUsers(),
                "ROOM|R101|A룸|4\n",
                "",
                "NOW|2026-03-20 09:00\n");

        String output = runCli(root, "");
        assertContains(output, "[파일 오류] rooms.txt 1행: 필드 개수가 올바르지 않습니다.");
    }

    private static void testInvalidRoomIdSyntaxStopsStartup() throws Exception {
        Path root = createCliRoot();
        writeData(root,
                baseUsers(),
                "ROOM|room101|A룸|4|OPEN\n",
                "",
                "NOW|2026-03-20 09:00\n");

        String output = runCli(root, "");
        assertContains(output, "[파일 오류] rooms.txt 1행: roomId 형식이 올바르지 않습니다.");
    }

    private static void testDuplicateRoomIdStopsStartup() throws Exception {
        Path root = createCliRoot();
        writeData(root,
                baseUsers(),
                "ROOM|R101|A룸|4|OPEN\n"
                        + "ROOM|R101|B룸|6|OPEN\n",
                "",
                "NOW|2026-03-20 09:00\n");

        String output = runCli(root, "");
        assertContains(output, "[파일 오류] rooms.txt 2행: 중복된 roomId가 존재합니다.");
    }

    private static void testInvalidRoomStatusStopsStartup() throws Exception {
        Path root = createCliRoot();
        writeData(root,
                baseUsers(),
                "ROOM|R101|A룸|4|BROKEN\n",
                "",
                "NOW|2026-03-20 09:00\n");

        String output = runCli(root, "");
        assertContains(output, "[파일 오류] rooms.txt 1행: roomStatus 값은 OPEN 또는 CLOSED 이어야 합니다.");
    }

    private static void testEmptyRoomNameStopsStartup() throws Exception {
        Path root = createCliRoot();
        writeData(root,
                baseUsers(),
                "ROOM|R101||4|OPEN\n",
                "",
                "NOW|2026-03-20 09:00\n");

        String output = runCli(root, "");
        assertContains(output, "[파일 오류] rooms.txt 1행: roomName은 비어 있을 수 없습니다.");
    }

    private static void testRoomRejectsZeroMaxCapacity() throws Exception {
        Path root = createCliRoot();
        writeData(root,
                baseUsers(),
                "ROOM|R101|A룸|0|OPEN\n",
                "",
                "NOW|2026-03-20 09:00\n");

        String output = runCli(root, "");
        assertContains(output, "[파일 오류] rooms.txt 1행: maxCapacity는 1 이상이어야 합니다.");
    }

    private static void testRoomRejectsNegativeMaxCapacity() throws Exception {
        Path root = createCliRoot();
        writeData(root,
                baseUsers(),
                "ROOM|R101|A룸|-1|OPEN\n",
                "",
                "NOW|2026-03-20 09:00\n");

        String output = runCli(root, "");
        assertContains(output, "[파일 오류] rooms.txt 1행: maxCapacity는 1 이상이어야 합니다.");
    }

    private static void testRoomRejectsNonNumericMaxCapacity() throws Exception {
        Path root = createCliRoot();
        writeData(root,
                baseUsers(),
                "ROOM|R101|A룸|zero|OPEN\n",
                "",
                "NOW|2026-03-20 09:00\n");

        String output = runCli(root, "");
        assertContains(output, "[파일 오류] rooms.txt 1행: maxCapacity 값은 정수여야 합니다.");
    }

    private static void testDuplicateSystemTimeStopsStartup() throws Exception {
        Path root = createCliRoot();
        writeData(root, baseUsers(), baseRooms(), "", "NOW|2026-03-20 09:00\nNOW|2026-03-20 10:00\n");

        String output = runCli(root, "");
        assertContains(output, "[파일 오류] system_time.txt");
    }

    private static void testInvalidSystemTimeFormatStopsStartup() throws Exception {
        Path root = createCliRoot();
        writeData(root, baseUsers(), baseRooms(), "", "NOW|2026/03/20 09:00\n");

        String output = runCli(root, "");
        assertContains(output, "[파일 오류] system_time.txt 1행: NOW 형식이 올바르지 않습니다.");
    }

    private static void testInvalidReservationIdSyntaxStopsStartup() throws Exception {
        Path root = createCliRoot();
        writeData(root,
                baseUsers(),
                baseRooms(),
                "RESV|reservation1|user011|R101|2026-03-20|13:00|14:00|2|RESERVED|2026-03-20 09:00|-\n",
                "NOW|2026-03-20 09:00\n");

        String output = runCli(root, "");
        assertContains(output, "[파일 오류] reservations.txt 1행: reservationId 형식이 올바르지 않습니다.");
    }

    private static void testDuplicateReservationIdStopsStartup() throws Exception {
        Path root = createCliRoot();
        writeData(root,
                baseUsers(),
                baseRooms(),
                "RESV|rv0001|user011|R101|2026-03-20|13:00|14:00|2|RESERVED|2026-03-20 09:00|-\n"
                        + "RESV|rv0001|user022|R102|2026-03-20|15:00|16:00|2|RESERVED|2026-03-20 09:10|-\n",
                "NOW|2026-03-20 09:00\n");

        String output = runCli(root, "");
        assertContains(output, "[파일 오류] reservations.txt 2행: 중복된 reservationId가 존재합니다.");
    }

    private static void testInvalidReservationStatusStopsStartup() throws Exception {
        Path root = createCliRoot();
        writeData(root,
                baseUsers(),
                baseRooms(),
                "RESV|rv0001|user011|R101|2026-03-20|13:00|14:00|2|INVALID|2026-03-20 09:00|-\n",
                "NOW|2026-03-20 09:00\n");

        String output = runCli(root, "");
        assertContains(output, "[파일 오류] reservations.txt 1행: status 값이 올바르지 않습니다.");
    }

    private static void testInvalidReservationStartTimeFormatStopsStartup() throws Exception {
        Path root = createCliRoot();
        writeData(root,
                baseUsers(),
                baseRooms(),
                "RESV|rv0001|user011|R101|2026-03-20|13-00|14:00|2|RESERVED|2026-03-20 09:00|-\n",
                "NOW|2026-03-20 09:00\n");

        String output = runCli(root, "");
        assertContains(output, "[파일 오류] reservations.txt 1행: startTime 형식이 올바르지 않습니다.");
    }

    private static void testInvalidReservationEndTimeFormatStopsStartup() throws Exception {
        Path root = createCliRoot();
        writeData(root,
                baseUsers(),
                baseRooms(),
                "RESV|rv0001|user011|R101|2026-03-20|13:00|14-00|2|RESERVED|2026-03-20 09:00|-\n",
                "NOW|2026-03-20 09:00\n");

        String output = runCli(root, "");
        assertContains(output, "[파일 오류] reservations.txt 1행: endTime 형식이 올바르지 않습니다.");
    }

    private static void testInvalidCreatedAtFormatStopsStartup() throws Exception {
        Path root = createCliRoot();
        writeData(root,
                baseUsers(),
                baseRooms(),
                "RESV|rv0001|user011|R101|2026-03-20|13:00|14:00|2|RESERVED|2026/03/20 09:00|-\n",
                "NOW|2026-03-20 09:00\n");

        String output = runCli(root, "");
        assertContains(output, "[파일 오류] reservations.txt 1행: createdAt 형식이 올바르지 않습니다.");
    }

    private static void testInvalidCheckedInAtFormatStopsStartup() throws Exception {
        Path root = createCliRoot();
        writeData(root,
                baseUsers(),
                baseRooms(),
                "RESV|rv0001|user011|R101|2026-03-20|13:00|14:00|2|CHECKED_IN|2026-03-20 09:00|2026/03/20 12:55\n",
                "NOW|2026-03-20 09:00\n");

        String output = runCli(root, "");
        assertContains(output, "[파일 오류] reservations.txt 1행: checkedInAt 형식이 올바르지 않습니다.");
    }

    private static void testReservationRejectsMissingUserReference() throws Exception {
        Path root = createCliRoot();
        writeData(root,
                baseUsers(),
                baseRooms(),
                "RESV|rv0001|user999|R101|2026-03-20|13:00|14:00|2|RESERVED|2026-03-20 09:00|-\n",
                "NOW|2026-03-20 09:00\n");

        String output = runCli(root, "");
        assertContains(output, "[파일 오류] reservations.txt 1행: 존재하지 않는 userId를 참조합니다: user999");
    }

    private static void testReservationRejectsMissingRoomReference() throws Exception {
        Path root = createCliRoot();
        writeData(root,
                baseUsers(),
                baseRooms(),
                "RESV|rv0001|user011|R999|2026-03-20|13:00|14:00|2|RESERVED|2026-03-20 09:00|-\n",
                "NOW|2026-03-20 09:00\n");

        String output = runCli(root, "");
        assertContains(output, "[파일 오류] reservations.txt 1행: 존재하지 않는 roomId를 참조합니다: R999");
    }

    private static void testReservationRejectsMissingCheckedInAtForCheckedIn() throws Exception {
        Path root = createCliRoot();
        writeData(root,
                baseUsers(),
                baseRooms(),
                "RESV|rv0001|user011|R101|2026-03-20|13:00|14:00|2|CHECKED_IN|2026-03-20 09:00|-\n",
                "NOW|2026-03-20 09:00\n");

        String output = runCli(root, "");
        assertContains(output, "[파일 오류] reservations.txt 1행: CHECKED_IN/COMPLETED 상태는 checkedInAt 값이 필요합니다.");
    }

    private static void testReservationRejectsCheckedInAtPresentForReserved() throws Exception {
        Path root = createCliRoot();
        writeData(root,
                baseUsers(),
                baseRooms(),
                "RESV|rv0001|user011|R101|2026-03-20|13:00|14:00|2|RESERVED|2026-03-20 09:00|2026-03-20 12:55\n",
                "NOW|2026-03-20 09:00\n");

        String output = runCli(root, "");
        assertContains(output, "[파일 오류] reservations.txt 1행: RESERVED/NO_SHOW 상태는 checkedInAt이 '-' 이어야 합니다.");
    }

    private static void testReservationRejectsPartySizeOverRoomCapacity() throws Exception {
        Path root = createCliRoot();
        writeData(root,
                baseUsers(),
                "ROOM|R101|A룸|4|OPEN\n",
                "RESV|rv0001|user011|R101|2026-03-20|13:00|14:00|5|RESERVED|2026-03-20 09:00|-\n",
                "NOW|2026-03-20 09:00\n");

        String output = runCli(root, "");
        assertContains(output, "[파일 오류] reservations.txt 1행: partySize가 해당 room의 maxCapacity를 초과합니다.");
    }

    private static void testReservationRejectsRoomOverlapInData() throws Exception {
        Path root = createCliRoot();
        writeData(root,
                baseUsers(),
                baseRooms(),
                "RESV|rv0001|user011|R101|2026-03-20|13:00|15:00|2|RESERVED|2026-03-20 09:00|-\n"
                        + "RESV|rv0002|user022|R101|2026-03-20|14:00|16:00|2|RESERVED|2026-03-20 09:10|-\n",
                "NOW|2026-03-20 09:00\n");

        String output = runCli(root, "");
        assertContains(output, "[파일 오류] reservations.txt 2행: 같은 룸의 겹치는 시간대 예약이 존재합니다.");
    }

    private static void testReservationRejectsUserOverlapInData() throws Exception {
        Path root = createCliRoot();
        writeData(root,
                baseUsers(),
                baseRooms(),
                "RESV|rv0001|user011|R101|2026-03-20|13:00|15:00|2|RESERVED|2026-03-20 09:00|-\n"
                        + "RESV|rv0002|user011|R102|2026-03-20|14:00|16:00|2|RESERVED|2026-03-20 09:10|-\n",
                "NOW|2026-03-20 09:00\n");

        String output = runCli(root, "");
        assertContains(output, "[파일 오류] reservations.txt 2행: 같은 사용자의 겹치는 시간대 예약이 존재합니다.");
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

    private static void testSignupAcceptsBoundaryLengthFields() throws Exception {
        Path root = createCliRoot();
        writeData(root, baseUsers(), baseRooms(), "", "NOW|2026-03-20 09:00\n");

        String output = runCli(root, lines(
                "1",
                "abcd",
                "1234",
                "abcdefghijklmnopqrst",
                "0"));

        assertContains(output, "회원가입이 완료되었습니다.");
        assertFileContains(root, "users.txt", "USER|user023|abcd|1234|abcdefghijklmnopqrst|member");
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

    private static void testSignupRejectsNumericLeadingLoginId() throws Exception {
        Path root = createCliRoot();
        writeData(root, baseUsers(), baseRooms(), "", "NOW|2026-03-20 09:00\n");

        String output = runCli(root, lines(
                "1",
                "1abc",
                "pw12",
                "bonsu",
                "0"));

        assertContains(output, "오류: 로그인 ID는 영문자로 시작하고 영문자/숫자/_ 만 사용하여 4~20자로 입력해야 합니다.");
    }

    private static void testSignupRejectsSpecialCharacterLoginId() throws Exception {
        Path root = createCliRoot();
        writeData(root, baseUsers(), baseRooms(), "", "NOW|2026-03-20 09:00\n");

        String output = runCli(root, lines(
                "1",
                "ab!c",
                "pw12",
                "bonsu",
                "0"));

        assertContains(output, "오류: 로그인 ID는 영문자로 시작하고 영문자/숫자/_ 만 사용하여 4~20자로 입력해야 합니다.");
    }

    private static void testSignupRejectsLongLoginId() throws Exception {
        Path root = createCliRoot();
        writeData(root, baseUsers(), baseRooms(), "", "NOW|2026-03-20 09:00\n");

        String output = runCli(root, lines(
                "1",
                "abcdefghijklmnopqrstu",
                "pw12",
                "bonsu",
                "0"));

        assertContains(output, "오류: 로그인 ID는 영문자로 시작하고 영문자/숫자/_ 만 사용하여 4~20자로 입력해야 합니다.");
    }

    private static void testSignupRejectsShortUserName() throws Exception {
        Path root = createCliRoot();
        writeData(root, baseUsers(), baseRooms(), "", "NOW|2026-03-20 09:00\n");

        String output = runCli(root, lines(
                "1",
                "user023",
                "pw12",
                "abc",
                "0"));

        assertContains(output, "오류: 사용자명은 영문자로 시작하고 영문자/숫자/_ 만 사용하여 4~20자로 입력해야 합니다.");
    }

    private static void testSignupRejectsLongUserName() throws Exception {
        Path root = createCliRoot();
        writeData(root, baseUsers(), baseRooms(), "", "NOW|2026-03-20 09:00\n");

        String output = runCli(root, lines(
                "1",
                "user023",
                "pw12",
                "abcdefghijklmnopqrstu",
                "0"));

        assertContains(output, "오류: 사용자명은 영문자로 시작하고 영문자/숫자/_ 만 사용하여 4~20자로 입력해야 합니다.");
    }

    private static void testSignupRejectsNumericLeadingUserName() throws Exception {
        Path root = createCliRoot();
        writeData(root, baseUsers(), baseRooms(), "", "NOW|2026-03-20 09:00\n");

        String output = runCli(root, lines(
                "1",
                "user023",
                "pw12",
                "1bonsu",
                "0"));

        assertContains(output, "오류: 사용자명은 영문자로 시작하고 영문자/숫자/_ 만 사용하여 4~20자로 입력해야 합니다.");
    }

    private static void testSignupRejectsUnderscoreLeadingUserName() throws Exception {
        Path root = createCliRoot();
        writeData(root, baseUsers(), baseRooms(), "", "NOW|2026-03-20 09:00\n");

        String output = runCli(root, lines(
                "1",
                "user023",
                "pw12",
                "_bonsu",
                "0"));

        assertContains(output, "오류: 사용자명은 영문자로 시작하고 영문자/숫자/_ 만 사용하여 4~20자로 입력해야 합니다.");
    }

    private static void testSignupRejectsSpecialCharacterUserName() throws Exception {
        Path root = createCliRoot();
        writeData(root, baseUsers(), baseRooms(), "", "NOW|2026-03-20 09:00\n");

        String output = runCli(root, lines(
                "1",
                "user023",
                "pw12",
                "bo@nsu",
                "0"));

        assertContains(output, "오류: 사용자명은 영문자로 시작하고 영문자/숫자/_ 만 사용하여 4~20자로 입력해야 합니다.");
    }

    private static void testSignupAcceptsCompositeUserName() throws Exception {
        Path root = createCliRoot();
        writeData(root, baseUsers(), baseRooms(), "", "NOW|2026-03-20 09:00\n");

        String output = runCli(root, lines(
                "1",
                "user023",
                "pw12",
                "bo_nsu123",
                "0"));

        assertContains(output, "회원가입이 완료되었습니다.");
        assertFileContains(root, "users.txt", "USER|user023|user023|pw12|bo_nsu123|member");
    }

    private static void testSignupAllowsDuplicateUserName() throws Exception {
        Path root = createCliRoot();
        writeData(root, baseUsers(), baseRooms(), "", "NOW|2026-03-20 09:00\n");

        String output = runCli(root, lines(
                "1",
                "user023",
                "pw12",
                "bonsu",
                "0"));

        assertContains(output, "회원가입이 완료되었습니다.");
        assertFileContains(root, "users.txt", "USER|user023|user023|pw12|bonsu|member");
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

    private static void testSignupRejectsLongPassword() throws Exception {
        Path root = createCliRoot();
        writeData(root, baseUsers(), baseRooms(), "", "NOW|2026-03-20 09:00\n");

        String output = runCli(root, lines(
                "1",
                "user023",
                "123456789012345678901",
                "bonsu",
                "0"));

        assertContains(output, "오류: 비밀번호는 4~20자로 입력해야 합니다.");
    }

    private static void testSignupAcceptsMaxBoundaryPassword() throws Exception {
        Path root = createCliRoot();
        writeData(root, baseUsers(), baseRooms(), "", "NOW|2026-03-20 09:00\n");

        String output = runCli(root, lines(
                "1",
                "user023",
                "abcdefghijklmnopqrst",
                "bonsu",
                "0"));

        assertContains(output, "회원가입이 완료되었습니다.");
        assertFileContains(root, "users.txt", "USER|user023|user023|abcdefghijklmnopqrst|bonsu|member");
    }

    private static void testSignupRejectsDuplicateLoginId() throws Exception {
        Path root = createCliRoot();
        writeData(root, baseUsers(), baseRooms(), "", "NOW|2026-03-20 09:00\n");

        String output = runCli(root, lines(
                "1",
                "user011",
                "pw12",
                "bonsu",
                "0"));

        assertContains(output, "오류: 이미 사용 중인 로그인 ID입니다.");
    }

    private static void testSignupRejectsUnderscoreLeadingLoginId() throws Exception {
        Path root = createCliRoot();
        writeData(root, baseUsers(), baseRooms(), "", "NOW|2026-03-20 09:00\n");

        String output = runCli(root, lines(
                "1",
                "_abc",
                "pw12",
                "bonsu",
                "0"));

        assertContains(output, "오류: 로그인 ID는 영문자로 시작하고 영문자/숫자/_ 만 사용하여 4~20자로 입력해야 합니다.");
    }

    private static void testSignupRejectsKoreanLeadingLoginId() throws Exception {
        Path root = createCliRoot();
        writeData(root, baseUsers(), baseRooms(), "", "NOW|2026-03-20 09:00\n");

        String output = runCli(root, lines(
                "1",
                "가abc",
                "pw12",
                "bonsu",
                "0"));

        assertContains(output, "오류: 로그인 ID는 영문자로 시작하고 영문자/숫자/_ 만 사용하여 4~20자로 입력해야 합니다.");
    }

    private static void testSignupAcceptsUppercaseDistinctLoginId() throws Exception {
        Path root = createCliRoot();
        writeData(root, baseUsers(), baseRooms(), "", "NOW|2026-03-20 09:00\n");

        String output = runCli(root, lines(
                "1",
                "User023",
                "pw12",
                "bonsu",
                "0"));

        assertContains(output, "회원가입이 완료되었습니다.");
        assertFileContains(root, "users.txt", "USER|user023|User023|pw12|bonsu|member");
    }

    private static void testSignupRejectsLeadingIdeographicSpaceInPassword() throws Exception {
        Path root = createCliRoot();
        writeData(root, baseUsers(), baseRooms(), "", "NOW|2026-03-20 09:00\n");

        String output = runCli(root, lines(
                "1",
                "user023",
                "\u3000pw12",
                "bonsu",
                "0"));

        assertContains(output, "오류: 입력값 앞뒤에 공백을 넣을 수 없습니다.");
    }

    private static void testGuestMenuRejectsEmptyInput() throws Exception {
        Path root = createCliRoot();
        writeData(root, baseUsers(), baseRooms(), "", "NOW|2026-03-20 09:00\n");

        String output = runCli(root, lines(
                "",
                "0"));

        assertContains(output, "오류: 메뉴 번호는 숫자로 입력해야 합니다.");
        assertAppearsBeforeLast(output, "오류: 메뉴 번호는 숫자로 입력해야 합니다.", "프로그램을 종료합니다.");
    }

    private static void testGuestMenuRejectsWhitespaceOnlyInput() throws Exception {
        Path root = createCliRoot();
        writeData(root, baseUsers(), baseRooms(), "", "NOW|2026-03-20 09:00\n");

        String output = runCli(root, lines(
                " ",
                "0"));

        assertContains(output, "오류: 메뉴 선택 앞뒤에 공백을 넣을 수 없습니다.");
        assertAppearsBeforeLast(output, "오류: 메뉴 선택 앞뒤에 공백을 넣을 수 없습니다.", "프로그램을 종료합니다.");
    }

    private static void testGuestMenuRejectsNegativeInput() throws Exception {
        Path root = createCliRoot();
        writeData(root, baseUsers(), baseRooms(), "", "NOW|2026-03-20 09:00\n");

        String output = runCli(root, lines(
                "-1",
                "0"));

        assertContains(output, "오류: 메뉴 번호는 숫자로 입력해야 합니다.");
    }

    private static void testGuestMenuRejectsAlphabeticInput() throws Exception {
        Path root = createCliRoot();
        writeData(root, baseUsers(), baseRooms(), "", "NOW|2026-03-20 09:00\n");

        String output = runCli(root, lines(
                "a",
                "0"));

        assertContains(output, "오류: 메뉴 번호는 숫자로 입력해야 합니다.");
    }

    private static void testGuestMenuRejectsFloatLikeInput() throws Exception {
        Path root = createCliRoot();
        writeData(root, baseUsers(), baseRooms(), "", "NOW|2026-03-20 09:00\n");

        String output = runCli(root, lines(
                "1.0",
                "0"));

        assertContains(output, "오류: 메뉴 번호는 숫자로 입력해야 합니다.");
    }

    private static void testGuestMenuRejectsLeadingSpaceInput() throws Exception {
        Path root = createCliRoot();
        writeData(root, baseUsers(), baseRooms(), "", "NOW|2026-03-20 09:00\n");

        String output = runCli(root, lines(
                " 2",
                "0"));

        assertContains(output, "오류: 메뉴 선택 앞뒤에 공백을 넣을 수 없습니다.");
    }

    private static void testGuestMenuRejectsTrailingSpaceInput() throws Exception {
        Path root = createCliRoot();
        writeData(root, baseUsers(), baseRooms(), "", "NOW|2026-03-20 09:00\n");

        String output = runCli(root, lines(
                "1 ",
                "0"));

        assertContains(output, "오류: 메뉴 선택 앞뒤에 공백을 넣을 수 없습니다.");
    }

    private static void testGuestMenuRejectsTabbedInput() throws Exception {
        Path root = createCliRoot();
        writeData(root, baseUsers(), baseRooms(), "", "NOW|2026-03-20 09:00\n");

        String output = runCli(root, lines(
                "\t1",
                "0"));

        assertContains(output, "오류: 메뉴 선택 앞뒤에 공백을 넣을 수 없습니다.");
    }

    private static void testGuestMenuRejectsMiddleSpaceInput() throws Exception {
        Path root = createCliRoot();
        writeData(root, baseUsers(), baseRooms(), "", "NOW|2026-03-20 09:00\n");

        String output = runCli(root, lines(
                "1 2",
                "0"));

        assertContains(output, "오류: 메뉴 번호는 숫자로 입력해야 합니다.");
    }

    private static void testGuestMenuRejectsOverflowInput() throws Exception {
        Path root = createCliRoot();
        writeData(root, baseUsers(), baseRooms(), "", "NOW|2026-03-20 09:00\n");

        String output = runCli(root, lines(
                "99999999999",
                "0"));

        assertContains(output, "오류: 메뉴 번호는 숫자로 입력해야 합니다.");
    }

    private static void testGuestMenuAcceptsLeadingZeroChoice() throws Exception {
        Path root = createCliRoot();
        writeData(root, baseUsers(), baseRooms(), "", "NOW|2026-03-20 09:00\n");

        String output = runCli(root, lines(
                "02",
                "user999",
                "pw1234",
                "0"));

        assertContains(output, "[로그인]");
        assertContains(output, "로그인 ID:");
        assertContains(output, "오류: 존재하지 않는 로그인 ID입니다.");
    }

    private static void testMenuInvalidChoiceRepromptsInPlace() throws Exception {
        Path root = createCliRoot();
        writeData(root, baseUsers(), baseRooms(), "", "NOW|2026-03-20 09:00\n");

        String output = runCli(root, lines(
                "9",
                "0"));

        assertContains(output, "오류: 존재하지 않는 메뉴 번호입니다.");
        assertContains(output, "메뉴 선택: 오류: 존재하지 않는 메뉴 번호입니다.");
    }

    private static void testMemberMenuInvalidChoiceReprompts() throws Exception {
        Path root = createCliRoot();
        writeData(root, baseUsers(), baseRooms(), "", "NOW|2026-03-20 09:00\n");

        String output = runCli(root, lines(
                "2",
                "user011",
                "pw1234",
                "9",
                "0",
                "0"));

        assertContains(output, "[member 메뉴]");
        assertContains(output, "오류: 존재하지 않는 메뉴 번호입니다.");
    }

    private static void testAdminMenuInvalidChoiceReprompts() throws Exception {
        Path root = createCliRoot();
        writeData(root, baseUsers(), baseRooms(), "", "NOW|2026-03-20 09:00\n");

        String output = runCli(root, lines(
                "2",
                "admin",
                "admin1234",
                "9",
                "0",
                "0"));

        assertContains(output, "[admin 메뉴]");
        assertContains(output, "오류: 존재하지 않는 메뉴 번호입니다.");
    }

    private static void testLoginRejectsMissingLoginId() throws Exception {
        Path root = createCliRoot();
        writeData(root, baseUsers(), baseRooms(), "", "NOW|2026-03-20 09:00\n");

        String output = runCli(root, lines(
                "2",
                "user999",
                "pw1234",
                "0"));

        assertContains(output, "오류: 존재하지 않는 로그인 ID입니다.");
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

    private static void testMemberLogoutReturnsToGuestMenu() throws Exception {
        Path root = createCliRoot();
        writeData(root, baseUsers(), baseRooms(), "", "NOW|2026-03-20 09:00\n");

        String output = runCli(root, lines(
                "2",
                "user011",
                "pw1234",
                "0",
                "0"));

        assertContains(output, "로그아웃되었습니다.");
        assertAppearsBeforeLast(output, "로그아웃되었습니다.", "[비로그인 메뉴]");
    }

    private static void testAdminLogoutReturnsToGuestMenu() throws Exception {
        Path root = createCliRoot();
        writeData(root, baseUsers(), baseRooms(), "", "NOW|2026-03-20 09:00\n");

        String output = runCli(root, lines(
                "2",
                "admin",
                "admin1234",
                "0",
                "0"));

        assertContains(output, "로그아웃되었습니다.");
        assertAppearsBeforeLast(output, "로그아웃되었습니다.", "[비로그인 메뉴]");
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

    private static void testChangeCurrentTimeRejectsInvalidDateTimeFormat() throws Exception {
        Path root = createCliRoot();
        writeData(root, baseUsers(), baseRooms(), "", "NOW|2026-03-20 09:00\n");

        String output = runCli(root, lines(
                "2",
                "user011",
                "pw1234",
                "1",
                "2026-02-30 09:00",
                "0",
                "0"));

        assertContains(output, "오류: 날짜/시각 형식이 올바르지 않습니다. 예: 2026-03-20 09:00");
    }

    private static void testChangeCurrentTimeRejectsIsoSeparator() throws Exception {
        Path root = createCliRoot();
        writeData(root, baseUsers(), baseRooms(), "", "NOW|2026-03-20 09:00\n");

        String output = runCli(root, lines(
                "2",
                "user011",
                "pw1234",
                "1",
                "2026-03-20T09:00",
                "0",
                "0"));

        assertContains(output, "오류: 날짜/시각 형식이 올바르지 않습니다. 예: 2026-03-20 09:00");
    }

    private static void testChangeCurrentTimeRejectsDoubleSpaceDateTime() throws Exception {
        Path root = createCliRoot();
        writeData(root, baseUsers(), baseRooms(), "", "NOW|2026-03-20 09:00\n");

        String output = runCli(root, lines(
                "2",
                "user011",
                "pw1234",
                "1",
                "2026-03-20  09:00",
                "0",
                "0"));

        assertContains(output, "오류: 날짜/시각 형식이 올바르지 않습니다. 예: 2026-03-20 09:00");
    }

    private static void testChangeCurrentTimeAcceptsSameValue() throws Exception {
        Path root = createCliRoot();
        writeData(root, baseUsers(), baseRooms(), "", "NOW|2026-03-20 10:00\n");

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

    private static void testChangeCurrentTimeRejectsDateOnly() throws Exception {
        Path root = createCliRoot();
        writeData(root, baseUsers(), baseRooms(), "", "NOW|2026-03-20 09:00\n");

        String output = runCli(root, lines(
                "2",
                "user011",
                "pw1234",
                "1",
                "2026-03-20",
                "0",
                "0"));

        assertContains(output, "오류: 날짜/시각 형식이 올바르지 않습니다. 예: 2026-03-20 09:00");
    }

    private static void testChangeCurrentTimeRejectsTimeOnly() throws Exception {
        Path root = createCliRoot();
        writeData(root, baseUsers(), baseRooms(), "", "NOW|2026-03-20 09:00\n");

        String output = runCli(root, lines(
                "2",
                "user011",
                "pw1234",
                "1",
                "10:00",
                "0",
                "0"));

        assertContains(output, "오류: 날짜/시각 형식이 올바르지 않습니다. 예: 2026-03-20 09:00");
    }

    private static void testChangeCurrentTimeRejectsSeconds() throws Exception {
        Path root = createCliRoot();
        writeData(root, baseUsers(), baseRooms(), "", "NOW|2026-03-20 09:00\n");

        String output = runCli(root, lines(
                "2",
                "user011",
                "pw1234",
                "1",
                "2026-03-20 10:00:00",
                "0",
                "0"));

        assertContains(output, "오류: 날짜/시각 형식이 올바르지 않습니다. 예: 2026-03-20 09:00");
    }

    private static void testChangeCurrentTimeRejectsOneDigitHour() throws Exception {
        Path root = createCliRoot();
        writeData(root, baseUsers(), baseRooms(), "", "NOW|2026-03-20 09:00\n");

        String output = runCli(root, lines(
                "2",
                "user011",
                "pw1234",
                "1",
                "2026-03-20 9:00",
                "0",
                "0"));

        assertContains(output, "오류: 날짜/시각 형식이 올바르지 않습니다. 예: 2026-03-20 09:00");
    }

    private static void testChangeCurrentTimeRejectsMinute60() throws Exception {
        Path root = createCliRoot();
        writeData(root, baseUsers(), baseRooms(), "", "NOW|2026-03-20 09:00\n");

        String output = runCli(root, lines(
                "2",
                "user011",
                "pw1234",
                "1",
                "2026-03-20 10:60",
                "0",
                "0"));

        assertContains(output, "오류: 날짜/시각 형식이 올바르지 않습니다. 예: 2026-03-20 09:00");
    }

    private static void testChangeCurrentTimeAcceptsLeapDay() throws Exception {
        Path root = createCliRoot();
        writeData(root, baseUsers(), baseRooms(), "", "NOW|2026-03-20 09:00\n");

        String output = runCli(root, lines(
                "2",
                "user011",
                "pw1234",
                "1",
                "2028-02-29 10:00",
                "0",
                "0"));

        assertContains(output, "현재 시각이 변경되었습니다.");
        assertFileContains(root, "system_time.txt", "NOW|2028-02-29 10:00");
    }

    private static void testMemberTimeChangeRejectsPast() throws Exception {
        Path root = createCliRoot();
        writeData(root, baseUsers(), baseRooms(), "", "NOW|2026-03-20 11:30\n");

        String output = runCli(root, lines(
                "2",
                "user011",
                "pw1234",
                "1",
                "2026-03-20 10:00",
                "0",
                "0"));

        assertContains(output, "오류: 현재 시각은 과거로 되돌릴 수 없습니다.");
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

    private static void testAvailableRoomQueryListsMatchingRooms() throws Exception {
        Path root = createCliRoot();
        writeData(root,
                baseUsers(),
                "ROOM|R101|A룸|4|OPEN\n"
                        + "ROOM|R102|B룸|6|OPEN\n"
                        + "ROOM|R103|C룸|8|OPEN\n",
                "RESV|rv0001|user022|R101|2026-03-20|13:00|15:00|2|RESERVED|2026-03-20 09:00|-\n",
                "NOW|2026-03-20 09:00\n");

        String output = runCli(root, lines(
                "2",
                "user011",
                "pw1234",
                "2",
                "2026-03-20",
                "13:00",
                "15:00",
                "4",
                "0",
                "0"));

        assertContains(output, "roomId");
        assertContains(output, "R102");
        assertContains(output, "R103");
        assertContains(output, "조회가 끝났습니다.");
    }

    private static void testAvailableRoomQueryRejectsInvalidDate() throws Exception {
        Path root = createCliRoot();
        writeData(root, baseUsers(), baseRooms(), "", "NOW|2026-03-20 09:00\n");

        String output = runCli(root, lines(
                "2",
                "user011",
                "pw1234",
                "2",
                "2026-02-30",
                "13:00",
                "15:00",
                "2",
                "0",
                "0"));

        assertContains(output, "오류: 날짜 형식이 올바르지 않습니다. 예: 2026-03-20");
    }

    private static void testAvailableRoomQueryRejectsSlashDate() throws Exception {
        Path root = createCliRoot();
        writeData(root, baseUsers(), baseRooms(), "", "NOW|2026-03-20 09:00\n");

        String output = runCli(root, lines(
                "2",
                "user011",
                "pw1234",
                "2",
                "2026/03/20",
                "13:00",
                "15:00",
                "2",
                "0",
                "0"));

        assertContains(output, "오류: 날짜 형식이 올바르지 않습니다. 예: 2026-03-20");
    }

    private static void testAvailableRoomQueryRejectsOneDigitHour() throws Exception {
        Path root = createCliRoot();
        writeData(root, baseUsers(), baseRooms(), "", "NOW|2026-03-20 09:00\n");

        String output = runCli(root, lines(
                "2",
                "user011",
                "pw1234",
                "2",
                "2026-03-20",
                "9:00",
                "15:00",
                "2",
                "0",
                "0"));

        assertContains(output, "오류: 시각 형식이 올바르지 않습니다. 예: 13:00");
    }

    private static void testAvailableRoomQueryRejectsHour24() throws Exception {
        Path root = createCliRoot();
        writeData(root, baseUsers(), baseRooms(), "", "NOW|2026-03-20 09:00\n");

        String output = runCli(root, lines(
                "2",
                "user011",
                "pw1234",
                "2",
                "2026-03-20",
                "24:00",
                "15:00",
                "2",
                "0",
                "0"));

        assertContains(output, "오류: 시각 형식이 올바르지 않습니다. 예: 13:00");
    }

    private static void testAvailableRoomQueryRejectsEndBeforeStart() throws Exception {
        Path root = createCliRoot();
        writeData(root, baseUsers(), baseRooms(), "", "NOW|2026-03-20 09:00\n");

        String output = runCli(root, lines(
                "2",
                "user011",
                "pw1234",
                "2",
                "2026-03-20",
                "15:00",
                "13:00",
                "2",
                "0",
                "0"));

        assertContains(output, "오류: 시작 시각은 종료 시각보다 빨라야 합니다.");
    }

    private static void testAvailableRoomQueryRejectsZeroPartySize() throws Exception {
        Path root = createCliRoot();
        writeData(root, baseUsers(), baseRooms(), "", "NOW|2026-03-20 09:00\n");

        String output = runCli(root, lines(
                "2",
                "user011",
                "pw1234",
                "2",
                "2026-03-20",
                "13:00",
                "15:00",
                "0",
                "0",
                "0"));

        assertContains(output, "오류: 1 이상의 정수를 입력해야 합니다.");
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

    private static void testCreateReservationAllowsOneHourBoundary() throws Exception {
        Path root = createCliRoot();
        writeData(root, baseUsers(), baseRooms(), "", "NOW|2026-03-20 09:00\n");

        String output = runCli(root, lines(
                "2",
                "user011",
                "pw1234",
                "3",
                "2026-03-20",
                "13:00",
                "14:00",
                "1",
                "R102",
                "0",
                "0"));

        assertContains(output, "예약이 완료되었습니다.");
        assertFileContains(root, "reservations.txt", "RESV|rv0001|user011|R102|2026-03-20|13:00|14:00|1|RESERVED|2026-03-20 09:00|-");
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

    private static void testCreateReservationRejectsInvalidDate() throws Exception {
        Path root = createCliRoot();
        writeData(root, baseUsers(), baseRooms(), "", "NOW|2026-03-20 09:00\n");

        String output = runCli(root, lines(
                "2",
                "user011",
                "pw1234",
                "3",
                "2026-02-30",
                "13:00",
                "14:00",
                "2",
                "R102",
                "0",
                "0"));

        assertContains(output, "오류: 날짜 형식이 올바르지 않습니다. 예: 2026-03-20");
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

    private static void testCreateReservationRejectsInvalidRoomIdFormat() throws Exception {
        Path root = createCliRoot();
        writeData(root, baseUsers(), baseRooms(), "", "NOW|2026-03-20 09:00\n");

        String output = runCli(root, lines(
                "2",
                "user011",
                "pw1234",
                "3",
                "2026-03-20",
                "13:00",
                "14:00",
                "2",
                "bad",
                "0",
                "0"));

        assertContains(output, "오류: 룸 ID 형식이 올바르지 않습니다. 예: R101");
    }

    private static void testCreateReservationRejectsNonexistentRoomId() throws Exception {
        Path root = createCliRoot();
        writeData(root, baseUsers(), baseRooms(), "", "NOW|2026-03-20 09:00\n");

        String output = runCli(root, lines(
                "2",
                "user011",
                "pw1234",
                "3",
                "2026-03-20",
                "13:00",
                "14:00",
                "2",
                "R999",
                "0",
                "0"));

        assertContains(output, "오류: 존재하지 않는 룸 ID입니다.");
    }

    private static void testCreateReservationRejectsZeroYearDate() throws Exception {
        Path root = createCliRoot();
        writeData(root, baseUsers(), baseRooms(), "", "NOW|2026-03-20 09:00\n");

        String output = runCli(root, lines(
                "2",
                "user011",
                "pw1234",
                "3",
                "0000-01-01",
                "13:00",
                "14:00",
                "2",
                "R102",
                "0",
                "0"));

        assertContains(output, "오류: 날짜 형식이 올바르지 않습니다. 예: 2026-03-20");
    }

    private static void testCreateReservationRejectsZeroPartySize() throws Exception {
        Path root = createCliRoot();
        writeData(root, baseUsers(), baseRooms(), "", "NOW|2026-03-20 09:00\n");

        String output = runCli(root, lines(
                "2",
                "user011",
                "pw1234",
                "3",
                "2026-03-20",
                "13:00",
                "14:00",
                "0",
                "R102",
                "0",
                "0"));

        assertContains(output, "오류: 1 이상의 정수를 입력해야 합니다.");
    }

    private static void testCreateReservationRejectsCapacityOverflow() throws Exception {
        Path root = createCliRoot();
        writeData(root, baseUsers(), baseRooms(), "", "NOW|2026-03-20 09:00\n");

        String output = runCli(root, lines(
                "2",
                "user011",
                "pw1234",
                "3",
                "2026-03-20",
                "13:00",
                "14:00",
                "9",
                "R101",
                "0",
                "0"));

        assertContains(output, "오류: 수용 인원을 초과했습니다.");
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

    private static void testCancelRejectsOtherUsersReservation() throws Exception {
        Path root = createCliRoot();
        writeData(root,
                baseUsers(),
                baseRooms(),
                "RESV|rv0001|user022|R101|2026-03-20|13:00|15:00|2|RESERVED|2026-03-20 09:00|-\n",
                "NOW|2026-03-20 09:00\n");

        String output = runCli(root, lines(
                "2",
                "user011",
                "pw1234",
                "4",
                "rv0001",
                "0",
                "0"));

        assertContains(output, "오류: 본인의 예약만 취소할 수 있습니다.");
    }

    private static void testCancelRejectsMissingReservation() throws Exception {
        Path root = createCliRoot();
        writeData(root, baseUsers(), baseRooms(), "", "NOW|2026-03-20 09:00\n");

        String output = runCli(root, lines(
                "2",
                "user011",
                "pw1234",
                "4",
                "rv0001",
                "0",
                "0"));

        assertContains(output, "오류: 존재하지 않는 예약번호입니다.");
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

    private static void testMyReservationsEmptyAndSorted() throws Exception {
        Path emptyRoot = createCliRoot();
        writeData(emptyRoot, baseUsers(), baseRooms(), "", "NOW|2026-03-20 09:00\n");
        String emptyOutput = runCli(emptyRoot, lines(
                "2",
                "user011",
                "pw1234",
                "5",
                "0",
                "0"));
        assertContains(emptyOutput, "나의 예약이 없습니다.");

        Path sortedRoot = createCliRoot();
        writeData(sortedRoot,
                baseUsers(),
                baseRooms(),
                "RESV|rv0003|user011|R103|2026-03-21|09:00|10:00|2|RESERVED|2026-03-20 09:00|-\n"
                        + "RESV|rv0001|user011|R102|2026-03-20|13:00|14:00|2|RESERVED|2026-03-20 09:00|-\n"
                        + "RESV|rv0002|user022|R101|2026-03-20|10:00|11:00|2|RESERVED|2026-03-20 09:00|-\n",
                "NOW|2026-03-20 09:00\n");
        String sortedOutput = runCli(sortedRoot, lines(
                "2",
                "user011",
                "pw1234",
                "5",
                "0",
                "0"));

        assertContains(sortedOutput, "rv0001");
        assertContains(sortedOutput, "rv0003");
        assertNotContains(sortedOutput, "rv0002");
        assertAppearsBefore(sortedOutput, "rv0001", "rv0003");
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

    private static void testCheckInLowerBoundarySuccess() throws Exception {
        Path root = createCliRoot();
        writeData(root,
                baseUsers(),
                baseRooms(),
                "RESV|rv0001|user011|R101|2026-03-20|11:00|12:00|2|RESERVED|2026-03-20 09:00|-\n",
                "NOW|2026-03-20 10:50\n");

        String output = runCli(root, lines(
                "2",
                "user011",
                "pw1234",
                "6",
                "rv0001",
                "0",
                "0"));

        assertContains(output, "체크인이 완료되었습니다.");
        assertFileContains(root, "reservations.txt", "RESV|rv0001|user011|R101|2026-03-20|11:00|12:00|2|CHECKED_IN|2026-03-20 09:00|2026-03-20 10:50");
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

    private static void testCheckInAtStartTimeSuccess() throws Exception {
        Path root = createCliRoot();
        writeData(root,
                baseUsers(),
                baseRooms(),
                "RESV|rv0001|user011|R101|2026-03-20|11:00|12:00|2|RESERVED|2026-03-20 09:00|-\n",
                "NOW|2026-03-20 11:00\n");

        String output = runCli(root, lines(
                "2",
                "user011",
                "pw1234",
                "6",
                "rv0001",
                "0",
                "0"));

        assertContains(output, "체크인이 완료되었습니다.");
        assertFileContains(root, "reservations.txt", "RESV|rv0001|user011|R101|2026-03-20|11:00|12:00|2|CHECKED_IN|2026-03-20 09:00|2026-03-20 11:00");
    }

    private static void testCheckInRejectsOtherUsersReservation() throws Exception {
        Path root = createCliRoot();
        writeData(root,
                baseUsers(),
                baseRooms(),
                "RESV|rv0001|user022|R101|2026-03-20|11:00|12:00|2|RESERVED|2026-03-20 09:00|-\n",
                "NOW|2026-03-20 10:55\n");

        String output = runCli(root, lines(
                "2",
                "user011",
                "pw1234",
                "6",
                "rv0001",
                "0",
                "0"));

        assertContains(output, "오류: 본인 예약만 체크인할 수 있습니다.");
    }

    private static void testCheckInRejectsMissingReservation() throws Exception {
        Path root = createCliRoot();
        writeData(root, baseUsers(), baseRooms(), "", "NOW|2026-03-20 10:55\n");

        String output = runCli(root, lines(
                "2",
                "user011",
                "pw1234",
                "6",
                "rv0001",
                "0",
                "0"));

        assertContains(output, "오류: 존재하지 않는 예약번호입니다.");
    }

    private static void testCheckInAfterNoShowTransitionRejected() throws Exception {
        Path root = createCliRoot();
        writeData(root,
                baseUsers(),
                baseRooms(),
                "RESV|rv0001|user011|R101|2026-03-20|11:00|12:00|2|RESERVED|2026-03-20 09:00|-\n",
                "NOW|2026-03-20 11:11\n");

        String output = runCli(root, lines(
                "2",
                "user011",
                "pw1234",
                "6",
                "rv0001",
                "0",
                "0"));

        assertContains(output, "오류: 체크인 가능한 상태의 예약이 아닙니다.");
        assertFileContains(root, "reservations.txt", "RESV|rv0001|user011|R101|2026-03-20|11:00|12:00|2|NO_SHOW|2026-03-20 09:00|-");
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

    private static void testAllReservationsEmpty() throws Exception {
        Path root = createCliRoot();
        writeData(root, baseUsers(), baseRooms(), "", "NOW|2026-03-20 09:00\n");

        String output = runCli(root, lines(
                "2",
                "admin",
                "admin1234",
                "2",
                "0",
                "0"));

        assertContains(output, "예약 데이터가 없습니다.");
    }

    private static void testAllReservationsShowsUserNamesCheckedInAtAndSortedRows() throws Exception {
        Path root = createCliRoot();
        writeData(root,
                baseUsers(),
                baseRooms(),
                "RESV|rv0001|user011|R101|2026-03-21|11:00|12:00|2|RESERVED|2026-03-20 09:00|-\n"
                        + "RESV|rv0002|user022|R102|2026-03-20|13:00|14:00|3|CHECKED_IN|2026-03-20 09:30|2026-03-20 12:55\n",
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
        assertContains(output, "checkedInAt");
        assertContains(output, "2026-03-20 12:55");
        assertAppearsBefore(output, "rv0002", "rv0001");
    }

    private static void testRoomListShowsAllRooms() throws Exception {
        Path root = createCliRoot();
        writeData(root,
                baseUsers(),
                "ROOM|R101|A룸|4|OPEN\n"
                        + "ROOM|R102|B룸|6|OPEN\n"
                        + "ROOM|R103|C룸|8|CLOSED\n",
                "",
                "NOW|2026-03-20 09:00\n");

        String output = runCli(root, lines(
                "2",
                "admin",
                "admin1234",
                "4",
                "1",
                "0",
                "0",
                "0"));

        assertContains(output, "[전체 룸 조회]");
        assertContains(output, "R101");
        assertContains(output, "R102");
        assertContains(output, "R103");
        assertContains(output, "CLOSED");
    }

    private static void testRoomConditionInputErrorReturnsToRoomConditionMenu() throws Exception {
        Path root = createCliRoot();
        writeData(root, baseUsers(), baseRooms(), "", "NOW|2026-03-20 09:00\n");

        String output = runCli(root, lines(
                "2",
                "admin",
                "admin1234",
                "4",
                "2",
                "bad",
                "0",
                "0"));

        assertContains(output, "[룸 컨디션 관리]");
        assertContains(output, "오류: 룸 ID 형식이 올바르지 않습니다. 예: R101");
        assertAppearsBeforeLast(output, "오류: 룸 ID 형식이 올바르지 않습니다. 예: R101", "[룸 컨디션 관리]");
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

    private static void testManualMoveRejectsCapacityInsufficient() throws Exception {
        Path root = createCliRoot();
        writeData(root,
                baseUsers(),
                "ROOM|R101|A룸|6|OPEN\n"
                        + "ROOM|R102|B룸|1|OPEN\n",
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

        assertContains(output, "오류: 대상 룸의 수용 인원이 부족합니다.");
    }

    private static void testManualMoveRejectsTargetRoomOverlap() throws Exception {
        Path root = createCliRoot();
        writeData(root,
                baseUsers(),
                baseRooms(),
                "RESV|rv0001|user011|R101|2026-03-21|11:00|12:00|2|RESERVED|2026-03-20 09:00|-\n"
                        + "RESV|rv0002|user022|R102|2026-03-21|11:00|12:00|2|RESERVED|2026-03-20 09:30|-\n",
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

        assertContains(output, "오류: 대상 룸의 같은 시간대에 이미 예약이 있습니다.");
    }

    private static void testManualMoveRejectsMissingReservation() throws Exception {
        Path root = createCliRoot();
        writeData(root, baseUsers(), baseRooms(), "", "NOW|2026-03-20 09:00\n");

        String output = runCli(root, lines(
                "2",
                "admin",
                "admin1234",
                "3",
                "rv0001",
                "R101",
                "0",
                "0"));

        assertContains(output, "오류: 존재하지 않는 예약번호입니다.");
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

    private static void testCapacityChangeCheckInWindowReservedHandledAsImpacted() throws Exception {
        Path root = createCliRoot();
        writeData(root,
                baseUsers(),
                "ROOM|R101|A룸|6|OPEN\n"
                        + "ROOM|R102|B룸|6|OPEN\n",
                "RESV|rv0001|user011|R101|2026-03-20|09:00|10:00|5|RESERVED|2026-03-20 08:00|-\n",
                "NOW|2026-03-20 09:05\n");

        String output = runCli(root, lines(
                "2",
                "admin",
                "admin1234",
                "4",
                "2",
                "R101",
                "4",
                "2",
                "0",
                "0",
                "0"));

        assertContains(output, "영향 예약이 있어 처리 흐름을 시작합니다.");
        assertContains(output, "영향 예약 처리가 완료되었습니다.");
        assertContains(output, "룸 최대 수용 인원이 변경되었습니다.");
        assertFileContains(root, "rooms.txt", "ROOM|R101|A룸|4|OPEN");
        assertFileNotContains(root, "reservations.txt", "rv0001");
    }

    private static void testCapacityChangeRejectsCheckedInOverCapacity() throws Exception {
        Path root = createCliRoot();
        writeData(root,
                baseUsers(),
                "ROOM|R101|A룸|6|OPEN\n"
                        + "ROOM|R102|B룸|6|OPEN\n",
                "RESV|rv0001|user011|R101|2026-03-20|09:00|10:00|5|CHECKED_IN|2026-03-20 08:00|2026-03-20 08:55\n",
                "NOW|2026-03-20 09:05\n");

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

        assertContains(output, "오류: 현재 CHECKED_IN 예약 인원이 새 최대 수용 인원을 초과하여 변경할 수 없습니다.");
        assertFileContains(root, "rooms.txt", "ROOM|R101|A룸|6|OPEN");
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

    private static void testCloseRoomCheckInWindowReservedHandledAsImpacted() throws Exception {
        Path root = createCliRoot();
        writeData(root,
                baseUsers(),
                baseRooms(),
                "RESV|rv0001|user011|R101|2026-03-20|09:00|10:00|2|RESERVED|2026-03-20 08:00|-\n",
                "NOW|2026-03-20 09:05\n");

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
        assertContains(output, "룸이 임시 휴업 처리되었습니다.");
        assertFileContains(root, "rooms.txt", "ROOM|R101|A룸|4|CLOSED");
        assertFileNotContains(root, "reservations.txt", "rv0001");
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

    private static void testDataFileRejectsZeroYearDate() throws Exception {
        Path root = createCliRoot();
        writeData(root,
                baseUsers(),
                baseRooms(),
                "RESV|rv0001|user011|R101|0000-01-01|13:00|14:00|2|RESERVED|2026-03-20 09:00|-\n",
                "NOW|2026-03-20 09:00\n");

        String output = runCli(root, "");
        assertContains(output, "[파일 오류] reservations.txt 1행: date 형식이 올바르지 않습니다.");
    }

    private static void testDataFileRejectsNegativeYearDate() throws Exception {
        Path root = createCliRoot();
        writeData(root,
                baseUsers(),
                baseRooms(),
                "RESV|rv0001|user011|R101|-0001-01-01|13:00|14:00|2|RESERVED|2026-03-20 09:00|-\n",
                "NOW|2026-03-20 09:00\n");

        String output = runCli(root, "");
        assertContains(output, "[파일 오류] reservations.txt 1행: date 형식이 올바르지 않습니다.");
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

    private static void assertNotContains(String output, String unexpected) {
        if (output.contains(unexpected)) {
            throw new AssertionError("Did not expect output to contain: " + unexpected + "\nActual output:\n" + output);
        }
    }

    private static void assertAppearsBefore(String output, String first, String second) {
        int firstIndex = output.indexOf(first);
        int secondIndex = output.indexOf(second);
        if (firstIndex < 0 || secondIndex < 0 || firstIndex >= secondIndex) {
            throw new AssertionError("Expected '" + first + "' to appear before '" + second + "'.\nActual output:\n" + output);
        }
    }

    private static void assertAppearsBeforeLast(String output, String first, String second) {
        int firstIndex = output.indexOf(first);
        int secondIndex = output.lastIndexOf(second);
        if (firstIndex < 0 || secondIndex < 0 || firstIndex >= secondIndex) {
            throw new AssertionError("Expected '" + first + "' to appear before the last '" + second + "'.\nActual output:\n" + output);
        }
    }
}
