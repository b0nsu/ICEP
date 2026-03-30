import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;

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
        for (Role role : values()) {
            if (role.fileValue.equals(raw)) {
                return role;
            }
        }
        throw new AppDataException(fileName, lineNumber, "role 값은 member 또는 admin 이어야 합니다.");
    }
}

enum UserStatus {
    ACTIVE;

    static UserStatus fromFile(String raw, String fileName, int lineNumber) throws AppDataException {
        if ("ACTIVE".equals(raw)) {
            return ACTIVE;
        }
        throw new AppDataException(fileName, lineNumber, "status 값은 ACTIVE만 허용됩니다.");
    }
}

enum RoomStatus {
    OPEN,
    CLOSED,
    MAINTENANCE;

    static RoomStatus fromFile(String raw, String fileName, int lineNumber) throws AppDataException {
        try {
            return RoomStatus.valueOf(raw);
        } catch (IllegalArgumentException e) {
            throw new AppDataException(fileName, lineNumber, "status 값은 OPEN, CLOSED, MAINTENANCE만 허용됩니다.");
        }
    }
}

enum ReservationStatus {
    RESERVED,
    CANCELLED,
    CHECKED_IN,
    COMPLETED,
    NO_SHOW;

    static ReservationStatus fromFile(String raw, String fileName, int lineNumber) throws AppDataException {
        try {
            return ReservationStatus.valueOf(raw);
        } catch (IllegalArgumentException e) {
            throw new AppDataException(fileName, lineNumber, "status 값이 올바르지 않습니다.");
        }
    }
}

final class User {
    final String userId;
    final String password;
    final String displayName;
    final Role role;
    final int penalty;
    final UserStatus status;
    final int sourceLine;

    User(String userId,
         String password,
         String displayName,
         Role role,
         int penalty,
         UserStatus status,
         int sourceLine) {
        this.userId = userId;
        this.password = password;
        this.displayName = displayName;
        this.role = role;
        this.penalty = penalty;
        this.status = status;
        this.sourceLine = sourceLine;
    }

    User withPenalty(int newPenalty) {
        return new User(userId, password, displayName, role, newPenalty, status, sourceLine);
    }

    String toRecord() {
        return String.join("|", "USER", userId, password, displayName, role.fileValue(), String.valueOf(penalty), status.name());
    }
}

final class Room {
    final String roomId;
    final int capacity;
    final String equipment;
    RoomStatus roomStatus;
    final LocalTime openTime;
    final LocalTime closeTime;
    final int sourceLine;

    Room(String roomId,
         int capacity,
         String equipment,
         RoomStatus roomStatus,
         LocalTime openTime,
         LocalTime closeTime,
         int sourceLine) {
        this.roomId = roomId;
        this.capacity = capacity;
        this.equipment = equipment;
        this.roomStatus = roomStatus;
        this.openTime = openTime;
        this.closeTime = closeTime;
        this.sourceLine = sourceLine;
    }

    String toRecord() {
        return String.join("|", "ROOM", roomId, String.valueOf(capacity), equipment, roomStatus.name(),
                TimeFormats.formatTime(openTime), TimeFormats.formatTime(closeTime));
    }
}

final class Reservation {
    final String reservationId;
    String roomId;
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

    boolean overlaps(LocalDateTime otherStart, LocalDateTime otherEnd) {
        return startDateTime().isBefore(otherEnd) && otherStart.isBefore(endDateTime());
    }

    boolean activeForOverlap() {
        return status == ReservationStatus.RESERVED || status == ReservationStatus.CHECKED_IN;
    }

    String checkedInAtText() {
        return checkedInAt == null ? "-" : TimeFormats.formatDateTime(checkedInAt);
    }

    Reservation copy() {
        return new Reservation(reservationId, roomId, userId, date, startTime, endTime, status, checkedInAt, extensionCount,
                sourceLine);
    }

    String toRecord() {
        return String.join("|",
                "RESV",
                reservationId,
                roomId,
                userId,
                TimeFormats.formatDate(date),
                TimeFormats.formatTime(startTime),
                TimeFormats.formatTime(endTime),
                status.name(),
                checkedInAtText(),
                String.valueOf(extensionCount));
    }
}

final class SystemData {
    final LinkedHashMap<String, User> users;
    final LinkedHashMap<String, Room> rooms;
    final LinkedHashMap<String, Reservation> reservations;
    LocalDateTime currentTime;

    SystemData(LinkedHashMap<String, User> users,
               LinkedHashMap<String, Room> rooms,
               LinkedHashMap<String, Reservation> reservations,
               LocalDateTime currentTime) {
        this.users = users;
        this.rooms = rooms;
        this.reservations = reservations;
        this.currentTime = currentTime;
    }

    String nextReservationId() {
        int max = 0;
        for (String id : reservations.keySet()) {
            if (!id.startsWith("rv") || id.length() != 6) {
                continue;
            }
            try {
                max = Math.max(max, Integer.parseInt(id.substring(2)));
            } catch (NumberFormatException ignored) {
                // already validated by parser, keep defensively
            }
        }
        return String.format("rv%04d", max + 1);
    }

    List<Room> sortedRooms() {
        List<Room> list = new ArrayList<>(rooms.values());
        list.sort(Comparator.comparing(room -> room.roomId));
        return list;
    }

    List<Reservation> sortedReservations() {
        List<Reservation> list = new ArrayList<>(reservations.values());
        list.sort(Comparator
                .comparing(Reservation::startDateTime)
                .thenComparing(reservation -> reservation.roomId)
                .thenComparing(reservation -> reservation.reservationId));
        return list;
    }
}
