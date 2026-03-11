import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;

public class RegressionTest {
    private static final String BASE_USERS = String.join("\n",
            "USER|admin01|Admin123!|관리자 계정|admin|0|ACTIVE",
            "USER|asdad|1234asdf|김본수|member|0|ACTIVE");
    private static final String BASE_ROOMS = String.join("\n",
            "ROOM|R101|4|PROJECTOR+WHITEBOARD|OPEN|09:00|23:00",
            "ROOM|R102|6|WHITEBOARD|OPEN|09:00|22:00",
            "ROOM|R201|8|MONITOR+WHITEBOARD|OPEN|10:00|22:00");
    private static final String BASE_SYSTEM_TIME = "NOW|2026-03-10T13:00";

    public static void main(String[] args) throws Exception {
        testSameTimeCurrentTimeChangeStillAppliesAutoUpdate();
        testRejectsPrematureCompletedReservation();
        testRejectsPrematureNoShowReservation();
        testSaveAllRollsBackOnReplaceFailure();
        System.out.println("Regression tests passed.");
    }

    private static void testSameTimeCurrentTimeChangeStillAppliesAutoUpdate() throws Exception {
        Path dir = createDatasetDirectory("same-time-update", "");

        String output = runCliWithMidSessionFileEdit(
                dir,
                String.join("\n",
                        "2",
                        "admin01",
                        "Admin123!",
                        ""),
                () -> writeReservationFile(dir, "RESV|rv0001|R101|asdad|2026-03-10|12:00|13:00|RESERVED|-|0"),
                String.join("\n",
                        "1",
                        "2",
                        "2026-03-10 13:00",
                        "0",
                        "0",
                        ""));

        assertContains(output, "[안내] 자동 상태 갱신 결과: NO_SHOW 1건, 패널티 증가 1건");
        assertContains(Files.readString(dir.resolve("reservations.txt"), StandardCharsets.UTF_8), "|NO_SHOW|-|0");
        assertContains(Files.readString(dir.resolve("users.txt"), StandardCharsets.UTF_8), "USER|asdad|1234asdf|김본수|member|1|ACTIVE");
    }

    private static void testRejectsPrematureCompletedReservation() throws Exception {
        Path dir = createDatasetDirectory("invalid-completed",
                "RESV|rv0001|R101|asdad|2026-03-10|14:00|15:00|COMPLETED|2026-03-10T13:55|0");

        try {
            new TextDataStore(dir).loadAll();
            throw new AssertionError("Premature COMPLETED reservation should be rejected.");
        } catch (AppDataException e) {
            assertContains(e.getMessage(), "COMPLETED 상태 예약은 종료 시각이 지난 뒤에만 허용됩니다.");
        }
    }

    private static void testRejectsPrematureNoShowReservation() throws Exception {
        Path dir = createDatasetDirectory("invalid-noshow",
                "RESV|rv0001|R101|asdad|2026-03-10|14:00|15:00|NO_SHOW|-|0");

        try {
            new TextDataStore(dir).loadAll();
            throw new AssertionError("Premature NO_SHOW reservation should be rejected.");
        } catch (AppDataException e) {
            assertContains(e.getMessage(), "NO_SHOW 상태 예약은 체크인 마감 시각 이후여야 합니다.");
        }
    }

    private static void testSaveAllRollsBackOnReplaceFailure() throws Exception {
        Path dir = createDatasetDirectory("rollback-save", "");
        TextDataStore loader = new TextDataStore(dir);
        SystemDataset dataset = loader.loadAll();
        dataset.users.get("asdad").penalty = 2;
        dataset.currentTime = dataset.currentTime.plusMinutes(30);

        String originalUsers = Files.readString(dir.resolve("users.txt"), StandardCharsets.UTF_8);
        String originalRooms = Files.readString(dir.resolve("rooms.txt"), StandardCharsets.UTF_8);
        String originalReservations = Files.readString(dir.resolve("reservations.txt"), StandardCharsets.UTF_8);
        String originalSystemTime = Files.readString(dir.resolve("system_time.txt"), StandardCharsets.UTF_8);

        TextDataStore failingStore = new TextDataStore(dir, new TextDataStore.FileMover() {
            private boolean failed;

            @Override
            public void move(Path source, Path target) throws IOException {
                if (!failed && "rooms.txt".equals(target.getFileName().toString())) {
                    failed = true;
                    throw new IOException("forced move failure");
                }
                TextDataStore.moveReplacing(source, target);
            }
        });

        try {
            failingStore.saveAll(dataset);
            throw new AssertionError("saveAll should fail when a replace operation fails.");
        } catch (AppDataException e) {
            assertContains(e.getMessage(), "forced move failure");
        }

        assertEquals(originalUsers, Files.readString(dir.resolve("users.txt"), StandardCharsets.UTF_8), "users.txt should be rolled back.");
        assertEquals(originalRooms, Files.readString(dir.resolve("rooms.txt"), StandardCharsets.UTF_8), "rooms.txt should be rolled back.");
        assertEquals(originalReservations, Files.readString(dir.resolve("reservations.txt"), StandardCharsets.UTF_8), "reservations.txt should remain unchanged.");
        assertEquals(originalSystemTime, Files.readString(dir.resolve("system_time.txt"), StandardCharsets.UTF_8), "system_time.txt should remain unchanged.");
    }

    private static Path createDatasetDirectory(String prefix, String reservationsContent) throws IOException {
        Path dir = Files.createTempDirectory(prefix);
        Files.writeString(dir.resolve("users.txt"), BASE_USERS, StandardCharsets.UTF_8);
        Files.writeString(dir.resolve("rooms.txt"), BASE_ROOMS, StandardCharsets.UTF_8);
        writeReservationFile(dir, reservationsContent);
        Files.writeString(dir.resolve("system_time.txt"), BASE_SYSTEM_TIME, StandardCharsets.UTF_8);
        return dir;
    }

    private static String runCli(Path workingDirectory, String input) throws Exception {
        Process process = new ProcessBuilder(
                javaExecutable(),
                "-cp",
                System.getProperty("java.class.path"),
                "Main")
                .directory(workingDirectory.toFile())
                .redirectErrorStream(true)
                .start();

        try (OutputStream stdin = process.getOutputStream()) {
            stdin.write(input.getBytes(StandardCharsets.UTF_8));
        }

        String output;
        try (InputStream stdout = process.getInputStream()) {
            output = new String(readAllBytes(stdout), StandardCharsets.UTF_8);
        }

        int exitCode = process.waitFor();
        if (exitCode != 0) {
            throw new AssertionError("CLI exited with code " + exitCode + "\n" + output);
        }
        return output;
    }

    private static String runCliWithMidSessionFileEdit(Path workingDirectory,
                                                       String firstInput,
                                                       ThrowingRunnable midSessionEdit,
                                                       String remainingInput) throws Exception {
        Process process = new ProcessBuilder(
                javaExecutable(),
                "-cp",
                System.getProperty("java.class.path"),
                "Main")
                .directory(workingDirectory.toFile())
                .redirectErrorStream(true)
                .start();

        try (OutputStream stdin = process.getOutputStream()) {
            stdin.write(firstInput.getBytes(StandardCharsets.UTF_8));
            stdin.flush();
            Thread.sleep(300);
            midSessionEdit.run();
            stdin.write(remainingInput.getBytes(StandardCharsets.UTF_8));
        }

        String output;
        try (InputStream stdout = process.getInputStream()) {
            output = new String(readAllBytes(stdout), StandardCharsets.UTF_8);
        }

        int exitCode = process.waitFor();
        if (exitCode != 0) {
            throw new AssertionError("CLI exited with code " + exitCode + "\n" + output);
        }
        return output;
    }

    private static String javaExecutable() {
        return Path.of(System.getProperty("java.home"), "bin", "java").toString();
    }

    private static byte[] readAllBytes(InputStream inputStream) throws IOException {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        inputStream.transferTo(outputStream);
        return outputStream.toByteArray();
    }

    private static void writeReservationFile(Path dir, String reservationsContent) throws IOException {
        Files.writeString(dir.resolve("reservations.txt"), reservationsContent, StandardCharsets.UTF_8);
    }

    private static void assertContains(String text, String expected) {
        if (!text.contains(expected)) {
            throw new AssertionError("Expected to find [" + expected + "] in:\n" + text);
        }
    }

    private static void assertEquals(String expected, String actual, String message) {
        if (!expected.equals(actual)) {
            throw new AssertionError(message + "\nExpected:\n" + expected + "\nActual:\n" + actual);
        }
    }

    @FunctionalInterface
    private interface ThrowingRunnable {
        void run() throws Exception;
    }
}
