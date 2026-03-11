import java.time.LocalDateTime;

/**
 * 현재 공용 시각을 기준으로 예약 상태를 자동 전이하고,
 * 그 과정에서 발생한 변경 건수를 함께 집계하는 유틸리티다.
 */
final class AutoStateUpdater {
    private AutoStateUpdater() {
    }

    static UpdateResult apply(SystemDataset dataset) {
        UpdateResult result = new UpdateResult();
        for (Reservation reservation : dataset.sortedReservations()) {
            LocalDateTime startDateTime = reservation.startDateTime();
            LocalDateTime endDateTime = reservation.endDateTime();
            if (reservation.status == ReservationStatus.RESERVED && dataset.currentTime.isAfter(startDateTime.plusMinutes(15))) {
                reservation.status = ReservationStatus.NO_SHOW;
                reservation.checkedInAt = null;
                User user = dataset.users.get(reservation.userId);
                if (user != null) {
                    user.penalty += 1;
                    result.penaltyCount += 1;
                }
                result.noShowCount += 1;
            } else if (reservation.status == ReservationStatus.CHECKED_IN && !dataset.currentTime.isBefore(endDateTime)) {
                reservation.status = ReservationStatus.COMPLETED;
                result.completedCount += 1;
            }
        }
        return result;
    }
}

/**
 * 자동 상태 갱신 과정에서 발생한 예약 상태 변경과 패널티 증가 건수를 담는다.
 */
class UpdateResult {
    int noShowCount;
    int completedCount;
    int penaltyCount;

    boolean changed() {
        return noShowCount > 0 || completedCount > 0 || penaltyCount > 0;
    }
}
