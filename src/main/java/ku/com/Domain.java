package ku.com;

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

enum RoomStatus {
    OPEN,
    CLOSED;

    static RoomStatus fromFile(String raw, String fileName, int lineNumber) throws AppDataException {
        try {
            return RoomStatus.valueOf(raw);
        } catch (IllegalArgumentException e) {
            throw new AppDataException(fileName, lineNumber, "roomStatus 값은 OPEN 또는 CLOSED 이어야 합니다.");
        }
    }
}

enum ReservationStatus {
    RESERVED,
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
    final String loginId;
    final String password;
    final String userName;
    final Role role;
    final int sourceLine;

    User(String userId, String loginId, String password, String userName, Role role, int sourceLine) {
        this.userId = userId;
        this.loginId = loginId;
        this.password = password;
        this.userName = userName;
        this.role = role;
        this.sourceLine = sourceLine;
    }

    String toRecord() {
        return String.join("|", "USER", userId, loginId, password, userName, role.fileValue());
    }
}

final class Room {
    final String roomId;
    final String roomName;
    int maxCapacity;
    RoomStatus roomStatus;
    final int sourceLine;

    Room(String roomId, String roomName, int maxCapacity, RoomStatus roomStatus, int sourceLine) {
        this.roomId = roomId;
        this.roomName = roomName;
        this.maxCapacity = maxCapacity;
        this.roomStatus = roomStatus;
        this.sourceLine = sourceLine;
    }

    String toRecord() {
        return String.join("|", "ROOM", roomId, roomName, String.valueOf(maxCapacity), roomStatus.name());
    }
}

final class Reservation {
    final String reservationId;
    final String userId;
    String roomId;
    final LocalDate date;
    final LocalTime startTime;
    final LocalTime endTime;
    final int partySize;
    ReservationStatus status;
    final LocalDateTime createdAt;
    LocalDateTime checkedInAt;
    final int sourceLine;

    Reservation(String reservationId,
                String userId,
                String roomId,
                LocalDate date,
                LocalTime startTime,
                LocalTime endTime,
                int partySize,
                ReservationStatus status,
                LocalDateTime createdAt,
                LocalDateTime checkedInAt,
                int sourceLine) {
        this.reservationId = reservationId;
        this.userId = userId;
        this.roomId = roomId;
        this.date = date;
        this.startTime = startTime;
        this.endTime = endTime;
        this.partySize = partySize;
        this.status = status;
        this.createdAt = createdAt;
        this.checkedInAt = checkedInAt;
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
        return new Reservation(
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
                sourceLine);
    }

    String toRecord() {
        return String.join("|",
                "RESV",
                reservationId,
                userId,
                roomId,
                TimeFormats.formatDate(date),
                TimeFormats.formatTime(startTime),
                TimeFormats.formatTime(endTime),
                String.valueOf(partySize),
                status.name(),
                TimeFormats.formatDateTime(createdAt),
                checkedInAtText());
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
            }
        }
        return String.format("rv%04d", max + 1);
    }

    String nextUserId() {
        int max = 0;
        for (String id : users.keySet()) {
            if (!id.startsWith("user") || id.length() <= 4) {
                continue;
            }
            try {
                max = Math.max(max, Integer.parseInt(id.substring(4)));
            } catch (NumberFormatException ignored) {
            }
        }
        return String.format("user%03d", max + 1);
    }

    User findUserByLoginId(String loginId) {
        for (User user : users.values()) {
            if (user.loginId.equals(loginId)) {
                return user;
            }
        }
        return null;
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
