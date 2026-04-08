# 04 Report Ready Tables

## 전체 요약

| 항목 | 값 |
|---|---|
| 총 TC 수 | 49 |
| PASS | 49 |
| FAIL | 0 |

## 검사 결과 표

| TC ID | 대응 절 | 예상 결과(기획서 기준) | 실제 결과 | 판정 |
|---|---|---|---|---|
| 2.3-01 | 2.3 / 7.2 | 누락된 data/와 필수 파일 4종이 자동 생성되고, 파일 검증을 통과하면 [비로그인 메뉴]가 출력된다. | missing_files: 비로그인 메뉴 출력 후 종료했고, after 스냅샷에 users.txt·rooms.txt·reservations.txt·system_time.txt 자동 생성이 확인되었다. | PASS |
| 5.1-01 | 5.1 / 7.2 | 문법 오류가 있는 reservations.txt를 읽는 즉시 [파일 오류] reservations.txt ... 형식으로 출력하고 비로그인 메뉴 진입 없이 시작을 중단한다. | broken_reservation_record: 터미널에 '[파일 오류] reservations.txt 1행: 필드 개수가 올바르지 않습니다.'와 '프로그램 시작을 중단합니다.'가 출력되었고 메뉴는 열리지 않았다. | PASS |
| 5.2-01 | 5.2 / 5.3.1 / 7.2 | 의미 규칙 위반으로 [파일 오류] reservations.txt ... 같은 룸의 겹치는 시간대 예약이 존재합니다. 출력 후 시작을 중단한다. | overlap_same_room: 터미널에 '[파일 오류] reservations.txt 2행: 같은 룸의 겹치는 시간대 예약이 존재합니다.'와 시작 중단 메시지가 출력되었다. | PASS |
| 6.1-01 | 4.1 / 6.1 | 회원가입 성공 메시지를 출력하고 users.txt에 auto userId가 발급된 member 레코드 1건이 추가된다. | signup_success: 회원가입 완료 메시지와 발급된 사용자 ID가 출력되었고, after/users.txt에 USER|user023|sujin01|pass1234|sujin01|member 레코드가 추가되었다. | PASS |
| 6.1-02 | 4.1 / 6.1 | 오류: 이미 사용 중인 로그인 ID입니다.를 출력하고 users.txt는 변경되지 않는다. | signup_duplicate_login_id: 중복 loginId 오류가 출력되었고 after/users.txt에는 신규 USER 레코드가 추가되지 않았다. | PASS |
| 4.1-01 | 4.1 / 6.1 | 4자와 20자 loginId는 허용되고, 3자·21자·비영문 시작·비허용 문자 포함 값은 형식 오류로 저장되지 않는다. | min_valid_4: 4자 loginId는 회원가입 성공 / max_valid_20: 20자 loginId는 회원가입 성공 / min_minus_1: 3자 loginId는 형식 오류로 종료 / max_plus_1: 21자 loginId는 형식 오류로 종료 / invalid_start_char: 숫자로 시작하는 loginId는 형식 오류 / invalid_allowed_char: 허용되지 않은 '-' 포함 loginId는 형식 오류 | PASS |
| 4.1-02 | 4.1 / 6.1 | 4자와 20자 비밀번호는 허용되고, 3자와 21자는 형식 오류로 저장되지 않는다. | min_valid_4: 4자 비밀번호는 회원가입 성공 / max_valid_20: 20자 비밀번호는 회원가입 성공 / min_minus_1: 3자 비밀번호는 길이 오류 / max_plus_1: 21자 비밀번호는 길이 오류 | PASS |
| 4.1-03 | 4.1 / 6.1 | 4자와 20자 userName은 허용되고, 3자·21자·비영문 시작·비허용 문자 포함 값은 형식 오류로 저장되지 않는다. | min_valid_4: 4자 userName은 회원가입 성공 / max_valid_20: 20자 userName은 회원가입 성공 / min_minus_1: 3자 userName은 형식 오류 / max_plus_1: 21자 userName은 형식 오류 / invalid_start_char: 숫자로 시작하는 userName은 형식 오류 / invalid_allowed_char: 허용되지 않은 '-' 포함 userName은 형식 오류 | PASS |
| 6.1-03 | 4.1 / 6.1 | loginId alpha011과 비밀번호 pw1234가 일치하면 member 로그인에 성공한다. | login_success_distinct_login_id: 로그인 성공: bonsu (role: member) 가 출력되어 loginId + password 기준 인증이 확인되었다. | PASS |
| 6.1-04 | 4.1 / 6.1 | 오류: 비밀번호가 일치하지 않습니다.를 출력하고 비로그인 메뉴가 다시 표시된다. | login_failure_wrong_password: 비밀번호 불일치 오류가 출력된 뒤 비로그인 메뉴가 다시 표시되었다. | PASS |
| 7.3-01 | 6.1 / 7.3 | 오류: 존재하지 않는 메뉴 번호입니다.를 출력하고 비로그인 메뉴를 다시 보여 준다. | guest_invalid_menu: 존재하지 않는 메뉴 번호 오류가 출력된 뒤 비로그인 메뉴가 재표시되었다. | PASS |
| 6.2.1-01 | 6.2.1 / 7.1 | 기존/새 시각을 출력한 뒤 현재 시각이 변경되었습니다.를 출력하고 system_time.txt가 새 값으로 저장된다. | member_time_change_success: 현재 시각 변경 성공 메시지가 출력되었고 after/system_time.txt가 NOW|2026-03-20 10:00으로 바뀌었다. | PASS |
| 6.2.1-02 | 6.2.1 | 오류: 현재 시각은 과거로 되돌릴 수 없습니다.를 출력하고 system_time.txt는 유지된다. | member_time_change_fail_past: 과거 시각 거부 오류가 출력되었고 after/system_time.txt는 NOW|2026-03-20 09:00 그대로였다. | PASS |
| 6.2.2-01 | 6.2.2 | 조회 결과에는 조건을 만족하는 R102만 표시되고 조회가 끝났습니다.가 출력된다. | available_rooms_success: 조회 표에 R102만 출력되고 R101(충돌)·R103(CLOSED/정원부족)는 제외되었다. | PASS |
| 6.2.2-02 | 6.2.2 / 7.3 | 오류: 예약 시각은 1시간 단위여야 합니다.를 출력하고 member 메뉴를 다시 보여 준다. | available_rooms_format_error: 시작 시각 입력 직후 '오류: 예약 시각은 1시간 단위여야 합니다.'가 출력되고 member 메뉴가 다시 표시되었다. | PASS |
| 6.2.2-03 | 6.2.2 / 7.3 | 오류: 시작 시각은 종료 시각보다 빨라야 합니다.를 출력하고 member 메뉴를 다시 보여 준다. | available_rooms_semantic_error: 시작/종료 역전 의미 오류가 출력되고 member 메뉴가 재표시되었다. | PASS |
| 6.2.3-01 | 6.2.3 | 예약 완료 메시지와 예약번호가 출력되고 reservations.txt에 RESERVED 레코드 1건이 추가된다. | reservation_create_success: 예약 완료 메시지와 rv0001이 출력되었고 after/reservations.txt에 RESERVED 레코드가 추가되었다. | PASS |
| 6.2.3-02 | 6.2.3 | 오류: 예약 시작 시각은 현재 가상 시각보다 미래여야 합니다.를 출력하고 reservations.txt는 유지된다. | reservation_create_fail_past_or_current: 현재보다 늦지 않은 시작 시각 오류가 출력되었고 after/reservations.txt는 빈 상태를 유지했다. | PASS |
| 6.2.3-03 | 1.2 / 6.2.3 | 오류: 예약 시각은 1시간 단위여야 합니다.를 출력하고 예약 레코드를 추가하지 않는다. | reservation_create_fail_time_unit: 시작 시각 입력 단계에서 1시간 단위 오류가 출력되었고 after/reservations.txt는 변경되지 않았다. | PASS |
| 6.2.3-04 | 1.2 / 6.2.3 | 오류: 해당 시간대에 이미 예약된 룸입니다.를 출력하고 신규 예약을 저장하지 않는다. | reservation_create_fail_room_overlap: 같은 룸 충돌 오류가 출력되었고 after/reservations.txt에는 기존 rv0001만 유지되었다. | PASS |
| 6.2.3-05 | 1.2 / 6.2.3 | 오류: 같은 시간대에 이미 다른 예약이 있습니다.를 출력하고 신규 예약을 저장하지 않는다. | reservation_create_fail_user_overlap: 같은 사용자 충돌 오류가 출력되었고 after/reservations.txt에는 기존 rv0001만 유지되었다. | PASS |
| 1.2-01 | 1.2 / 5.2 / 6.2.3 | 1시간과 4시간은 허용되고, 0시간과 5시간은 오류 메시지와 함께 저장되지 않는다. | length_1hour_success: 1시간 예약은 성공했고 after/reservations.txt에 13:00~14:00 RESERVED가 저장되었다. / length_4hour_success: 4시간 예약은 성공했고 after/reservations.txt에 13:00~17:00 RESERVED가 저장되었다. / length_0hour_fail: 시작=종료 입력은 시작/종료 역전 오류로 거부되었다. / length_5hour_fail: 5시간 예약은 길이 제한 오류로 거부되었다. | PASS |
| 1.2-02 | 1.2 / 5.2 / 6.2.3 | partySize 1은 허용되고, room 정원을 넘는 partySize는 오류와 함께 저장되지 않는다. | party_min_1_success: partySize 1은 정상 저장되었다. / party_over_capacity_fail: room maxCapacity 6을 넘는 partySize 7은 수용 인원 초과 오류로 거부되었다. | PASS |
| 1.2-03 | 1.2 / 6.2.3 / 7.3 | 잘못된 형식·실존하지 않는 날짜·앞뒤 공백은 각각 구체적 오류 메시지를 출력하고 상위 메뉴로 복귀한다. | invalid_time_format: 시각 형식 13 입력은 '오류: 시각 형식이 올바르지 않습니다. 예: 13:00'으로 거부되었다. / invalid_date_format: 슬래시 날짜는 날짜 형식 오류로 거부되었다. / nonexistent_date: 존재하지 않는 날짜 2026-02-30은 날짜 형식 오류로 거부되었다. / trim_rejected: 입력값 앞 공백이 있는 날짜는 '오류: 입력값 앞뒤에 공백을 넣을 수 없습니다.'로 거부되었다. | PASS |
| 6.2.4-01 | 6.2.4 | 예약이 취소되었습니다.를 출력하고 해당 RESV 레코드를 삭제한다. | cancel_success_delete: 취소 성공 메시지가 출력되었고 after/reservations.txt에서 rv0001 레코드가 삭제되었다. | PASS |
| 6.2.4-02 | 6.2.4 | 오류: 이미 진행 중이거나 종료된 예약은 취소할 수 없습니다.를 출력하고 레코드를 유지한다. | cancel_fail_started: 진행 중/종료 예약 취소 불가 오류가 출력되었고 after/reservations.txt에 rv0001이 유지되었다. | PASS |
| 6.2.5-01 | 6.2.5 | resvId/room/date/start/end/인원/status 표가 출력되고 user011 예약 2건만 보인다. | my_reservations_success: 나의 예약 조회 표에 rv0001, rv0003만 출력되고 rv0002(user022)는 제외되었다. | PASS |
| 6.2.5-02 | 6.2.5 | 나의 예약이 없습니다.를 출력한다. | my_reservations_none: 예약 없음 안내가 출력되었다. | PASS |
| 6.2.6-01 | 1.2 / 6.2.6 | 오류: 아직 체크인 가능한 시간이 아닙니다.를 출력하고 status는 RESERVED로 유지된다. | checkin_too_early: 체크인 가능 시간 이전 오류가 출력되었고 after/reservations.txt의 rv0001 status는 RESERVED였다. | PASS |
| 6.2.6-02 | 1.2 / 6.2.6 | 체크인이 완료되었습니다.를 출력하고 status=CHECKED_IN, checkedInAt=현재 시각으로 저장한다. | checkin_boundary_success: 체크인 성공 메시지가 출력되었고 after/reservations.txt에 CHECKED_IN과 checkedInAt=2026-03-20 11:10이 저장되었다. | PASS |
| 6.2.6-03 | 1.3 / 6.2.6 | 오류: 운영 중이 아닌 룸은 체크인할 수 없습니다.를 출력하고 status는 RESERVED로 유지한다. | checkin_closed_room_fail: 운영 중이 아닌 룸 체크인 불가 오류가 출력되었고 after/reservations.txt는 RESERVED 상태를 유지했다. | PASS |
| 6.3.2-01 | 6.3.2 | resvId / userId / userName / room / date / start / end / 인원 / status / checkedInAt 헤더와 예약 데이터가 출력된다. | admin_all_reservations: 전체 예약 표에 userId, userName 컬럼과 두 예약이 함께 출력되었다. | PASS |
| 6.3.3-01 | 6.3.3 | 변경 전/후 RESV 레코드를 출력하고 reservations.txt의 roomId가 R102로 바뀐다. | adjust_success: 변경 전/후 예약 레코드가 출력되었고 after/reservations.txt의 rv0001 roomId가 R102로 변경되었다. | PASS |
| 6.3.3-02 | 6.3.3 | 오류: 현재 룸과 다른 룸으로만 이동할 수 있습니다.를 출력하고 레코드는 유지된다. | adjust_fail_same_room: 동일 룸 이동 불가 오류가 출력되었고 after/reservations.txt의 rv0001은 R101 그대로였다. | PASS |
| 6.3.3-03 | 6.3.3 | 오류: 대상 룸이 OPEN 상태가 아니어서 이동할 수 없습니다.를 출력한다. | adjust_fail_closed_room: CLOSED 룸 이동 불가 오류가 출력되었다. | PASS |
| 6.3.3-04 | 6.3.3 | 오류: 대상 룸의 수용 인원이 부족합니다.를 출력한다. | adjust_fail_capacity: 대상 룸 정원 부족 오류가 출력되었다. | PASS |
| 6.3.4-01 | 6.3.4 | roomId roomName 정원 status 헤더와 모든 ROOM 데이터가 출력된다. | room_list_query: 전체 룸 조회 표에 R101, R102, R103과 status가 출력되었다. | PASS |
| 6.3.4-02 | 5.3.2 / 6.3.4 | 오류: 현재 CHECKED_IN 예약 인원이 새 최대 수용 인원을 초과하여 변경할 수 없습니다.를 출력하고 ROOM 레코드는 유지된다. | capacity_change_fail_checked_in_over: 현재 CHECKED_IN 예약 인원 초과 오류가 출력되었고 after/rooms.txt의 R101 정원은 6 그대로였다. | PASS |
| 6.3.4-03 | 5.3.2 / 6.3.4 | 영향 예약 처리 완료 후 변경 전/후 RESV, ROOM 레코드를 출력하고 R102 정원은 3으로, rv0015 roomId는 R101로 저장된다. | capacity_change_move_impacted: 영향 예약 처리 흐름이 실행되어 rv0015가 R101로 이동했고, after/rooms.txt의 R102 정원은 3으로 줄었다. | PASS |
| 6.3.4-04 | 5.3.2 / 6.3.4 | 룸 컨디션 변경을 취소하여 원상복구했습니다.를 출력하고 ROOM/RESV before 상태를 유지한다. | capacity_change_rollback: 영향 예약 처리 중 전체 취소를 선택하자 원상복구 메시지가 출력되었고 ROOM/RESV 파일은 before 상태를 유지했다. | PASS |
| 6.3.4-05 | 5.3.2 / 6.3.4 | 오류: 현재 체크인 중인 예약이 있어 즉시 휴업할 수 없습니다.를 출력하고 ROOM 레코드를 유지한다. | close_room_fail_checked_in: 현재 체크인 중 예약 존재 오류가 출력되었고 after/rooms.txt의 R101 status는 OPEN이었다. | PASS |
| 6.3.4-06 | 6.3.4 | 변경 전/후 ROOM 레코드 출력 후 R102 status가 CLOSED로 저장된다. | close_room_success: 변경 전/후 ROOM 레코드가 출력되었고 after/rooms.txt의 R102 status가 CLOSED로 저장되었다. | PASS |
| 6.3.4-07 | 5.3.2 / 6.3.4 | 영향 예약 처리 후 rv0001이 삭제되고 ROOM R101 status는 CLOSED로 저장된다. | close_room_impacted_delete: 영향 예약 처리 흐름 후 rv0001이 삭제되었고 after/rooms.txt의 R101 status가 CLOSED가 되었다. | PASS |
| 6.3.4-08 | 6.3.4 | 변경 전/후 ROOM 레코드 출력 후 R101 status가 OPEN으로 저장된다. | reopen_room_success: 변경 전/후 ROOM 레코드가 출력되었고 after/rooms.txt의 R101 status가 OPEN으로 저장되었다. | PASS |
| 7.1-01 | 7.1 | 상태 변화 요약에 RESERVED -> NO_SHOW : 1건이 출력되고 reservations.txt의 status가 NO_SHOW로 바뀐다. | auto_noshow: 상태 변화 요약에 RESERVED -> NO_SHOW : 1건이 출력되었고 after/reservations.txt의 rv0001 status가 NO_SHOW가 되었다. | PASS |
| 7.1-02 | 7.1 | 상태 변화 요약에 CHECKED_IN -> COMPLETED : 1건이 출력되고 reservations.txt의 status가 COMPLETED로 바뀐다. | auto_completed: 상태 변화 요약에 CHECKED_IN -> COMPLETED : 1건이 출력되었고 after/reservations.txt의 rv0002 status가 COMPLETED가 되었다. | PASS |
| 7.1-03 | 7.1 | 현재 시각이 변경되었습니다. 후 상태 변화 없음이 출력된다. | auto_no_state_change: 현재 시각 변경 성공 후 '상태 변화 없음'이 출력되었다. | PASS |
| 7.4-01 | 7.4 | 유효한 직접 수정 파일은 검증을 통과하고 비로그인 메뉴까지 정상 진입한다. | manual_edit_valid_rerun: 주석/빈 줄이 포함된 직접 수정 파일로도 비로그인 메뉴가 정상 표시되었고 시작 중단은 발생하지 않았다. | PASS |
| 7.4-02 | 7.4 / 7.2 | [파일 오류] rooms.txt ... roomStatus 값은 OPEN 또는 CLOSED 이어야 합니다.를 출력하고 시작을 중단한다. | manual_edit_invalid_rerun: 잘못 수정된 rooms.txt로 재실행하자 roomStatus 파일 오류와 시작 중단 메시지가 출력되었다. | PASS |
