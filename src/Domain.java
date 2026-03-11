import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.StringJoiner;

/*
 * 사용자, 스터디룸, 예약과 관련된 상태 enum 및 도메인 객체,
 * 그리고 메모리 상 전체 시스템 데이터를 묶는 SystemDataset을 정의하는 파일이다.
 */
enum Role {
    MEMBER("member"),
    ADMIN("admin");

    private final String fileValue;

    Role(String fileValue) {
        this.fileValue = fileValue;
    }

    String fileValue() {
        return fileValue;
    }

    static Role fromFile(String raw, String fileName, int lineNumber) throws AppDataException {
        for (Role value : values()) {
            if (value.fileValue.equals(raw)) {
                return value;
            }
        }
        throw new AppDataException(fileName, lineNumber, "role 값이 올바르지 않습니다.");
    }
}

enum UserStatus {
    ACTIVE("ACTIVE");

    private final String fileValue;

    UserStatus(String fileValue) {
        this.fileValue = fileValue;
    }

    String fileValue() {
        return fileValue;
    }

    static UserStatus fromFile(String raw, String fileName, int lineNumber) throws AppDataException {
        for (UserStatus value : values()) {
            if (value.fileValue.equals(raw)) {
                return value;
            }
        }
        throw new AppDataException(fileName, lineNumber, "user status 값이 올바르지 않습니다.");
    }
}

enum RoomStatus {
    OPEN("OPEN"),
    CLOSED("CLOSED"),
    MAINTENANCE("MAINTENANCE");

    private final String fileValue;

    RoomStatus(String fileValue) {
        this.fileValue = fileValue;
    }

    String fileValue() {
        return fileValue;
    }

    static RoomStatus fromFile(String raw, String fileName, int lineNumber) throws AppDataException {
        for (RoomStatus value : values()) {
            if (value.fileValue.equals(raw)) {
                return value;
            }
        }
        throw new AppDataException(fileName, lineNumber, "room status 값이 올바르지 않습니다.");
    }
}

enum ReservationStatus {
    RESERVED("RESERVED"),
    CANCELLED("CANCELLED"),
    CHECKED_IN("CHECKED_IN"),
    COMPLETED("COMPLETED"),
    NO_SHOW("NO_SHOW");

    private final String fileValue;

    ReservationStatus(String fileValue) {
        this.fileValue = fileValue;
    }

    String fileValue() {
        return fileValue;
    }

    static ReservationStatus fromFile(String raw, String fileName, int lineNumber) throws AppDataException {
        for (ReservationStatus value : values()) {
            if (value.fileValue.equals(raw)) {
                return value;
            }
        }
        throw new AppDataException(fileName, lineNumber, "reservation status 값이 올바르지 않습니다.");
    }
}

class User {
    final String userId;
    final String password;
    final String name;
    final Role role;
    int penalty;
    final UserStatus status;
    final int sourceLine;

    User(String userId, String password, String name, Role role, int penalty, UserStatus status, int sourceLine) {
        this.userId = userId;
        this.password = password;
        this.name = name;
        this.role = role;
        this.penalty = penalty;
        this.status = status;
        this.sourceLine = sourceLine;
    }

    boolean isMember() {
        return role == Role.MEMBER;
    }

    String toRecord() {
        return String.join("|",
                "USER",
                TextDataStore.escapeText(userId),
                TextDataStore.escapeText(password),
                TextDataStore.escapeText(name),
                role.fileValue(),
                Integer.toString(penalty),
                status.fileValue());
    }
}

class Room {
    final String roomId;
    final int capacity;
    final List<String> equipmentList;
    RoomStatus status;
    final LocalTime openTime;
    final LocalTime closeTime;
    final int sourceLine;

    Room(String roomId, int capacity, List<String> equipmentList, RoomStatus status, LocalTime openTime, LocalTime closeTime, int sourceLine) {
        this.roomId = roomId;
        this.capacity = capacity;
        this.equipmentList = new ArrayList<>(equipmentList);
        this.status = status;
        this.openTime = openTime;
        this.closeTime = closeTime;
        this.sourceLine = sourceLine;
    }

    boolean hasAllEquipment(Collection<String> requiredEquipment) {
        return equipmentList.containsAll(requiredEquipment);
    }

    String equipmentDisplay() {
        if (equipmentList.isEmpty()) {
            return "-";
        }
        return String.join("+", equipmentList);
    }

    String toRecord() {
        return String.join("|",
                "ROOM",
                roomId,
                Integer.toString(capacity),
                equipmentDisplay(),
                status.fileValue(),
                TimeFormats.formatFileTime(openTime),
                TimeFormats.formatFileTime(closeTime));
    }
}

class Reservation {
    final String reservationId;
    final String roomId;
    final String userId;
    final LocalDate date;
    final LocalTime startTime;
    LocalTime endTime;
    ReservationStatus status;
    LocalDateTime checkedInAt;
    int extensionCount;
    final int sourceLine;

    Reservation(String reservationId,
                String roomId,
                String userId,
                LocalDate date,
                LocalTime startTime,
                LocalTime endTime,
                ReservationStatus status,
                LocalDateTime checkedInAt,
                int extensionCount,
                int sourceLine) {
        this.reservationId = reservationId;
        this.roomId = roomId;
        this.userId = userId;
        this.date = date;
        this.startTime = startTime;
        this.endTime = endTime;
        this.status = status;
        this.checkedInAt = checkedInAt;
        this.extensionCount = extensionCount;
        this.sourceLine = sourceLine;
    }

    LocalDateTime startDateTime() {
        return LocalDateTime.of(date, startTime);
    }

    LocalDateTime endDateTime() {
        return LocalDateTime.of(date, endTime);
    }

    long durationMinutes() {
        return java.time.Duration.between(startDateTime(), endDateTime()).toMinutes();
    }

    boolean isActiveForConflict() {
        return status == ReservationStatus.RESERVED || status == ReservationStatus.CHECKED_IN;
    }

    boolean isFutureReserved(LocalDateTime currentTime) {
        return status == ReservationStatus.RESERVED && startDateTime().isAfter(currentTime);
    }

    String checkedInAtDisplay() {
        return checkedInAt == null ? "-" : TimeFormats.formatFileDateTime(checkedInAt);
    }

    String toRecord() {
        return String.join("|",
                "RESV",
                reservationId,
                roomId,
                userId,
                TimeFormats.formatFileDate(date),
                TimeFormats.formatFileTime(startTime),
                TimeFormats.formatFileTime(endTime),
                status.fileValue(),
                checkedInAtDisplay(),
                Integer.toString(extensionCount));
    }
}

class SystemDataset {
    final LinkedHashMap<String, User> users;
    final LinkedHashMap<String, Room> rooms;
    final LinkedHashMap<String, Reservation> reservations;
    LocalDateTime currentTime;

    SystemDataset(LinkedHashMap<String, User> users,
                  LinkedHashMap<String, Room> rooms,
                  LinkedHashMap<String, Reservation> reservations,
                  LocalDateTime currentTime) {
        this.users = users;
        this.rooms = rooms;
        this.reservations = reservations;
        this.currentTime = currentTime;
    }

    List<Room> sortedRooms() {
        List<Room> list = new ArrayList<>(rooms.values());
        list.sort(Comparator.comparing(room -> room.roomId));
        return list;
    }

    List<User> sortedUsers() {
        List<User> list = new ArrayList<>(users.values());
        list.sort(Comparator.comparing(user -> user.userId));
        return list;
    }

    List<Reservation> sortedReservations() {
        List<Reservation> list = new ArrayList<>(reservations.values());
        list.sort(Comparator
                .comparing(Reservation::startDateTime)
                .thenComparing(reservation -> reservation.reservationId));
        return list;
    }

    String nextReservationId() {
        int maxNumber = 0;
        for (String reservationId : reservations.keySet()) {
            if (reservationId.startsWith("rv")) {
                try {
                    int value = Integer.parseInt(reservationId.substring(2));
                    maxNumber = Math.max(maxNumber, value);
                } catch (NumberFormatException ignored) {
                    // load 단계에서 이미 검증한다.
                }
            }
        }
        return String.format("rv%04d", maxNumber + 1);
    }

    String summarizeAutoUpdate(UpdateResult result) {
        StringJoiner joiner = new StringJoiner(", ");
        if (result.noShowCount > 0) {
            joiner.add("NO_SHOW " + result.noShowCount + "건");
        }
        if (result.completedCount > 0) {
            joiner.add("COMPLETED " + result.completedCount + "건");
        }
        if (result.penaltyCount > 0) {
            joiner.add("패널티 증가 " + result.penaltyCount + "건");
        }
        return joiner.length() == 0 ? "변경 없음" : joiner.toString();
    }

    Map<String, User> userMap() {
        return users;
    }
}
