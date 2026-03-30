import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

record TransitionCount(ReservationStatus from, ReservationStatus to, int count) {
}

final class UpdateSummary {
    private final LinkedHashMap<String, String> changes = new LinkedHashMap<>();

    void addChange(String reservationId, ReservationStatus from, ReservationStatus to) {
        if (from == to) {
            return;
        }
        changes.put(reservationId, from.name() + "->" + to.name());
    }

    boolean changed() {
        return !changes.isEmpty();
    }

    List<String> changedReservationIds() {
        return new ArrayList<>(changes.keySet());
    }

    List<TransitionCount> transitionCounts() {
        Map<String, Integer> counter = new LinkedHashMap<>();
        for (String transition : changes.values()) {
            counter.put(transition, counter.getOrDefault(transition, 0) + 1);
        }
        List<TransitionCount> result = new ArrayList<>();
        for (Map.Entry<String, Integer> entry : counter.entrySet()) {
            String[] tokens = entry.getKey().split("->", -1);
            result.add(new TransitionCount(
                    ReservationStatus.valueOf(tokens[0]),
                    ReservationStatus.valueOf(tokens[1]),
                    entry.getValue()));
        }
        return result;
    }
}

final class AutoStateUpdater {
    private AutoStateUpdater() {
    }

    static UpdateSummary apply(SystemData dataset) {
        UpdateSummary summary = new UpdateSummary();
        for (Reservation reservation : dataset.sortedReservations()) {
            applyForOne(dataset, reservation, summary);
        }
        return summary;
    }

    private static void applyForOne(SystemData dataset, Reservation reservation, UpdateSummary summary) {
        ReservationStatus before = reservation.status;
        LocalDateTime now = dataset.currentTime;

        if (reservation.status == ReservationStatus.RESERVED
                && now.isAfter(reservation.startDateTime().plusMinutes(10))) {
            reservation.status = ReservationStatus.NO_SHOW;
            reservation.checkedInAt = null;
        } else if (reservation.status == ReservationStatus.CHECKED_IN
                && !now.isBefore(reservation.endDateTime())) {
            reservation.status = ReservationStatus.COMPLETED;
        }

        if (before != reservation.status) {
            summary.addChange(reservation.reservationId, before, reservation.status);
        }
    }
}
