# 06 Grouped Report Draft

첫 페이지 표기 초안:
- 팀명: `C07`
- 문서명: `1차 검사 보고서`
- 프로젝트 주제명: `스터디룸 예약 관리 CLI`

아래 묶음은 샘플 PDF의 권장 형식에 맞춰 `기획서 흐름` 기준으로 정리했다.
원본 증거는 각 `evidence` 경로의 `terminal.log`, `before/`, `after/`를 사용하면 된다.

## 2.3 / 7.2 시작 및 필수 파일

목표: 시작 단계 파일 검증, 자동 생성, 시작 중단 분기 확인

| ID | 입력 | 방법 | 예상 결과 | 실제 결과 | evidence |
|---|---|---|---|---|---|
| 2.3-01 | `data/` 없음 후 실행 | 빈 작업 디렉토리에서 JAR 실행 | 필수 파일 4종 자동 생성 후 비로그인 메뉴 진입 | 자동 생성 후 `[비로그인 메뉴]` 출력 | `inspection/evidence/TC-2.3-01` |
| 5.1-01 | `RESV\|broken\|line` | 손상된 `reservations.txt`로 재실행 | 필드 개수 오류 출력 후 시작 중단 | `[파일 오류] reservations.txt 1행: 필드 개수가 올바르지 않습니다.` | `inspection/evidence/TC-5.1-01` |
| 5.2-01 | 같은 룸 겹침 RESERVED 2건 | 겹치는 예약이 있는 파일로 재실행 | 의미 오류 출력 후 시작 중단 | `[파일 오류] reservations.txt 2행: 같은 룸의 겹치는 시간대 예약이 존재합니다.` | `inspection/evidence/TC-5.2-01` |

대표 로그:
- `inspection/evidence/TC-2.3-01/terminal.log`
- `inspection/evidence/TC-5.1-01/terminal.log`

## 4.1 / 6.1 비로그인 메뉴군

목표: 회원가입, 로그인, 비로그인 메뉴 오류 처리 확인

| ID | 입력 | 예상 결과 | 실제 결과 | evidence |
|---|---|---|---|---|
| 6.1-01 | `sujin01 / pass1234 / sujin01` | 회원가입 성공, `userId` 자동 발급 | `USER\|user023\|sujin01...` 저장 확인 | `inspection/evidence/TC-6.1-01` |
| 6.1-02 | 기존 `loginId=user011` | 중복 오류, 저장 없음 | 중복 오류 출력, `users.txt` 무변경 | `inspection/evidence/TC-6.1-02` |
| 4.1-01 | `loginId` 경계값 6종 | 허용 범위만 통과 | 4자/20자 통과, 나머지 형식 오류 | `inspection/evidence/TC-4.1-01` |
| 4.1-02 | `password` 경계값 4종 | 4자/20자 통과, 3자/21자 거부 | 기획서 기준과 일치 | `inspection/evidence/TC-4.1-02` |
| 4.1-03 | `userName` 경계값 6종 | 허용 범위만 통과 | 기획서 기준과 일치 | `inspection/evidence/TC-4.1-03` |
| 6.1-03 | `alpha011 / pw1234` | `loginId + password`로 로그인 성공 | `로그인 성공: bonsu (role: member)` | `inspection/evidence/TC-6.1-03` |
| 6.1-04 | `user011 / wrong` | 비밀번호 불일치 오류 후 메뉴 복귀 | 기획서 기준과 일치 | `inspection/evidence/TC-6.1-04` |
| 7.3-01 | 메뉴 번호 `9` | 존재하지 않는 메뉴 번호 오류 | 비로그인 메뉴 재표시 확인 | `inspection/evidence/TC-7.3-01` |

대표 로그:
- `inspection/evidence/TC-6.1-01/terminal.log`
- `inspection/evidence/TC-4.1-01/terminal.log`

## 6.2.1 현재 가상 시각 변경

목표: 미래 변경 성공과 과거 되돌리기 거부 확인

| ID | 입력 | 예상 결과 | 실제 결과 | evidence |
|---|---|---|---|---|
| 6.2.1-01 | `2026-03-20 10:00` | 시각 변경 성공, 파일 저장 | `system_time.txt` 변경 확인 | `inspection/evidence/TC-6.2.1-01` |
| 6.2.1-02 | `2026-03-20 08:00` | 과거 변경 거부 | `오류: 현재 시각은 과거로 되돌릴 수 없습니다.` | `inspection/evidence/TC-6.2.1-02` |

## 6.2.2 예약 가능 스터디룸 조회

목표: 조회 성공, 입력 형식 오류, 의미 오류 확인

| ID | 입력 | 예상 결과 | 실제 결과 | evidence |
|---|---|---|---|---|
| 6.2.2-01 | `2026-03-20 / 13:00 / 15:00 / 3` | 조건 만족 룸만 조회 | `R102`만 출력 | `inspection/evidence/TC-6.2.2-01` |
| 6.2.2-02 | 시작 시각 `13:30` | 1시간 단위 오류 | 형식 오류 후 메뉴 복귀 | `inspection/evidence/TC-6.2.2-02` |
| 6.2.2-03 | `15:00 ~ 13:00` | 시작<종료 위반 오류 | 의미 오류 후 메뉴 복귀 | `inspection/evidence/TC-6.2.2-03` |

## 6.2.3 예약 신청

목표: 예약 생성 정상/실패 분기와 경계값 확인

| ID | 입력 | 예상 결과 | 실제 결과 | evidence |
|---|---|---|---|---|
| 6.2.3-01 | `2026-03-20 / 13:00 / 15:00 / 2 / R102` | RESERVED 예약 생성 | `rv0001` 생성 | `inspection/evidence/TC-6.2.3-01` |
| 6.2.3-02 | 시작 시각 현재 이하 | 미래 시각 오류 | 기획서 기준과 일치 | `inspection/evidence/TC-6.2.3-02` |
| 6.2.3-03 | 시작 시각 `13:30` | 1시간 단위 오류 | 기획서 기준과 일치 | `inspection/evidence/TC-6.2.3-03` |
| 6.2.3-04 | 같은 룸 충돌 | 룸 충돌 오류 | 기획서 기준과 일치 | `inspection/evidence/TC-6.2.3-04` |
| 6.2.3-05 | 같은 사용자 충돌 | 사용자 충돌 오류 | 기획서 기준과 일치 | `inspection/evidence/TC-6.2.3-05` |
| 1.2-01 | 길이 1h/4h/0h/5h | 허용/거부 경계 확인 | 기획서 기준과 일치 | `inspection/evidence/TC-1.2-01` |
| 1.2-02 | `partySize` 1 / 초과 | 최소값/정원 초과 확인 | 기획서 기준과 일치 | `inspection/evidence/TC-1.2-02` |
| 1.2-03 | 날짜/시각 형식, 실존 날짜, 공백 | 입력 형식 경계 확인 | 기획서 기준과 일치 | `inspection/evidence/TC-1.2-03` |

대표 로그:
- `inspection/evidence/TC-6.2.3-01/terminal.log`
- `inspection/evidence/TC-1.2-01/terminal.log`

## 6.2.4 예약 취소

목표: 미래 RESERVED 취소와 진행 중 예약 취소 거부 확인

| ID | 입력 | 예상 결과 | 실제 결과 | evidence |
|---|---|---|---|---|
| 6.2.4-01 | `rv0001` | 예약 레코드 삭제 | after에서 `rv0001` 삭제 확인 | `inspection/evidence/TC-6.2.4-01` |
| 6.2.4-02 | 시작 후 `rv0001` | 진행 중/종료 예약 취소 거부 | 기획서 기준과 일치 | `inspection/evidence/TC-6.2.4-02` |

## 6.2.5 나의 예약 조회

목표: 본인 예약만 출력, 예약 없음 분기 확인

| ID | 입력 | 예상 결과 | 실제 결과 | evidence |
|---|---|---|---|---|
| 6.2.5-01 | 나의 예약 조회 | 본인 예약 2건만 출력 | `rv0001`, `rv0003`만 출력 | `inspection/evidence/TC-6.2.5-01` |
| 6.2.5-02 | 나의 예약 조회 | 예약 없음 안내 | `나의 예약이 없습니다.` | `inspection/evidence/TC-6.2.5-02` |

## 6.2.6 체크인

목표: 체크인 가능 구간 경계와 CLOSED 룸 거부 확인

| ID | 입력 | 예상 결과 | 실제 결과 | evidence |
|---|---|---|---|---|
| 6.2.6-01 | 현재 `10:49`, `rv0001` | 너무 이른 시점 거부 | 기획서 기준과 일치 | `inspection/evidence/TC-6.2.6-01` |
| 6.2.6-02 | 현재 `11:10`, `rv0001` | CHECKED_IN 저장 | `checkedInAt=2026-03-20 11:10` | `inspection/evidence/TC-6.2.6-02` |
| 6.2.6-03 | CLOSED 룸 `rv0001` | 체크인 거부 | 기획서 기준과 일치 | `inspection/evidence/TC-6.2.6-03` |

## 6.3.2 전체 예약 정보 조회

목표: admin 전체 예약 표 출력 확인

| ID | 입력 | 예상 결과 | 실제 결과 | evidence |
|---|---|---|---|---|
| 6.3.2-01 | 전체 예약 정보 조회 | `userId`, `userName` 포함 표 출력 | 2건 출력 확인 | `inspection/evidence/TC-6.3.2-01` |

## 6.3.3 예약 조정(방 이동)

목표: 성공, 같은 룸 실패, CLOSED 룸 실패, 정원 부족 실패 확인

| ID | 입력 | 예상 결과 | 실제 결과 | evidence |
|---|---|---|---|---|
| 6.3.3-01 | `rv0001 -> R102` | roomId 변경 성공 | before/after RESV 출력 | `inspection/evidence/TC-6.3.3-01` |
| 6.3.3-02 | `rv0001 -> R101` | 같은 룸 이동 거부 | 기획서 기준과 일치 | `inspection/evidence/TC-6.3.3-02` |
| 6.3.3-03 | `rv0001 -> R103(CLOSED)` | OPEN 아님 오류 | 기획서 기준과 일치 | `inspection/evidence/TC-6.3.3-03` |
| 6.3.3-04 | 정원 부족 룸 이동 | 수용 인원 부족 오류 | 기획서 기준과 일치 | `inspection/evidence/TC-6.3.3-04` |

## 6.3.4 룸 컨디션 관리

목표: 전체 룸 조회, 정원 변경, 임시 휴업, 운영 재개, 영향 예약 처리 확인

| ID | 입력 | 예상 결과 | 실제 결과 | evidence |
|---|---|---|---|---|
| 6.3.4-01 | 전체 룸 조회 | room 표 출력 | `R101/R102/R103` 출력 | `inspection/evidence/TC-6.3.4-01` |
| 6.3.4-02 | CHECKED_IN 초과 정원 변경 | 즉시 거부 | 기획서 기준과 일치 | `inspection/evidence/TC-6.3.4-02` |
| 6.3.4-03 | 영향 예약 이동 후 정원 감소 | RESV 이동 + ROOM 정원 변경 | before/after RESV/ROOM 확인 | `inspection/evidence/TC-6.3.4-03` |
| 6.3.4-04 | 영향 예약 처리 중 `0` | rollback | before 상태 유지 | `inspection/evidence/TC-6.3.4-04` |
| 6.3.4-05 | CHECKED_IN 존재 휴업 | 즉시 거부 | 기획서 기준과 일치 | `inspection/evidence/TC-6.3.4-05` |
| 6.3.4-06 | 예약 없는 룸 휴업 | CLOSED 저장 | before/after ROOM 확인 | `inspection/evidence/TC-6.3.4-06` |
| 6.3.4-07 | 영향 예약 삭제 후 휴업 | RESV 삭제 + ROOM CLOSED | before/after 확인 | `inspection/evidence/TC-6.3.4-07` |
| 6.3.4-08 | 운영 재개 | OPEN 저장 | before/after ROOM 확인 | `inspection/evidence/TC-6.3.4-08` |

대표 로그:
- `inspection/evidence/TC-6.3.4-03/terminal.log`
- `inspection/evidence/TC-6.3.4-04/terminal.log`

## 7.1 상태 자동 갱신

목표: `RESERVED -> NO_SHOW`, `CHECKED_IN -> COMPLETED`, 상태 변화 없음 확인

| ID | 입력 | 예상 결과 | 실제 결과 | evidence |
|---|---|---|---|---|
| 7.1-01 | 시각 `11:11` | `NO_SHOW` 전환 | 요약과 after 파일 확인 | `inspection/evidence/TC-7.1-01` |
| 7.1-02 | 시각 `10:00` | `COMPLETED` 전환 | 요약과 after 파일 확인 | `inspection/evidence/TC-7.1-02` |
| 7.1-03 | 시각 `09:30` | 상태 변화 없음 | `상태 변화 없음` 출력 | `inspection/evidence/TC-7.1-03` |

## 7.4 파일 직접 수정 시 처리

목표: 유효한 수정 재실행 통과, 잘못된 수정 재실행 차단 확인

| ID | 입력 | 예상 결과 | 실제 결과 | evidence |
|---|---|---|---|---|
| 7.4-01 | 주석/빈 줄 포함 유효 파일 | 정상 시작 | `[비로그인 메뉴]` 진입 | `inspection/evidence/TC-7.4-01` |
| 7.4-02 | 잘못된 `roomStatus` | 파일 오류 후 시작 중단 | 기획서 기준과 일치 | `inspection/evidence/TC-7.4-02` |
