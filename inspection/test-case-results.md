# 검사 결과

- 기준 문서: `/Users/bonsu/Downloads/전기프-06-검사 (1).pdf`
- 실행 명령: `sh gradlew test regressionTest`
- 실행 결과: `37/37 PASS`

## 요약

최신 실행 기준으로 전체 `37`개 테스트케이스가 모두 통과했다.

```text
Regression tests passed.
BUILD SUCCESSFUL
```

## 테스트케이스별 결과

| ID | 테스트케이스 | 핵심 입력/조건 | 예상 결과 | 실제 결과 |
| --- | --- | --- | --- | --- |
| TC01 | 자동 `NO_SHOW` 전환 | `RESERVED`, 시작+10분 초과 | `NO_SHOW` 전환 | `rv0001 -> NO_SHOW` 확인, PASS |
| TC02 | 다음 예약번호 생성 | 기존 `rv0001`, `rv0003` 존재 | `rv0004` 생성 | `rv0004` 확인, PASS |
| TC03 | 과거 `COMPLETED` 예약 적재 | 현재 정원보다 큰 완료 예약 | 정상 로드 | `COMPLETED` 유지, PASS |
| TC04 | 같은 룸으로 이동 거절 | 방 이동 대상=`현재 룸` | 거절 메시지 | 거절 확인, PASS |
| TC05 | 필수 파일 자동 생성 | `data`/파일 없음 | 기본 파일 생성 | `users/rooms/system_time` 생성 확인, PASS |
| TC06 | 잘못된 예약 파일 문법 | `RESV|broken|line` | 시작 중단 | `[파일 오류] reservations.txt` 확인, PASS |
| TC07 | 잘못된 `userId` 형식 | `USER|abcd|...` | 시작 중단 | `[파일 오류] users.txt 2행` 확인, PASS |
| TC08 | 중복 `NOW` 레코드 | `system_time.txt`에 `NOW` 2개 | 시작 중단 | `[파일 오류] system_time.txt` 확인, PASS |
| TC09 | 회원가입 성공 | 신규 `loginId=user023` | 회원 생성 | 저장 레코드 생성 확인, PASS |
| TC10 | 회원가입 `loginId` 최소길이 거절 | `loginId=abc` | 입력 오류 | 형식 오류 메시지 확인, PASS |
| TC11 | 회원가입 비밀번호 최소길이 거절 | `password=abc` | 입력 오류 | 길이 오류 메시지 확인, PASS |
| TC12 | 로그인 실패 후 성공 | 오비밀번호 후 재로그인 | 실패 후 성공 | 두 결과 모두 확인, PASS |
| TC13 | 회원 현재시각 변경 성공 | `09:00 -> 10:00` | 저장 반영 | `system_time.txt` 반영 확인, PASS |
| TC14 | 예약 가능 조회 필터링 | `CLOSED`/정원부족/충돌 룸 포함 | 조회 결과 없음 | `조회 결과가 없습니다.` 확인, PASS |
| TC15 | 예약 가능 조회 과거 시작 거절 | `NOW 14:00`, 시작 `13:00` | 오류 | 미래 시각 오류 확인, PASS |
| TC16 | 예약 가능 조회 사용자 충돌 거절 | 같은 회원 기존 예약 중복 | 오류 | 사용자 충돌 오류 확인, PASS |
| TC17 | 예약 생성 성공 | 정상 입력, `R102` | 예약 생성 | `rv0001` 저장 확인, PASS |
| TC18 | 예약 생성 4시간 경계 허용 | `13:00~17:00` | 성공 | 4시간 예약 저장 확인, PASS |
| TC19 | 예약 생성 30분 단위 거절 | `13:30` 입력 | 오류 | 1시간 단위 오류 확인, PASS |
| TC20 | 예약 생성 5시간 초과 거절 | `13:00~18:00` | 오류 | 길이 오류 확인, PASS |
| TC21 | 예약 생성 룸 충돌 거절 | 같은 룸 시간 중복 | 오류 | 룸 충돌 오류 확인, PASS |
| TC22 | 예약 생성 사용자 충돌 거절 | 같은 회원 시간 중복 | 오류 | 사용자 충돌 오류 확인, PASS |
| TC23 | 예약 취소 성공 | 미래 `RESERVED` 취소 | 레코드 삭제 | `rv0001` 삭제 확인, PASS |
| TC24 | 시작 후 예약 취소 거절 | `NOW=start` | 오류 | 진행중/종료 예약 오류 확인, PASS |
| TC25 | 체크인 너무 이른 시각 거절 | `10:49`, 시작 `11:00` | 오류 | 체크인 가능 시간 아님 확인, PASS |
| TC26 | 체크인 상한 경계 성공 | `11:10`, 시작 `11:00` | `CHECKED_IN` | 상태/`checkedInAt` 저장 확인, PASS |
| TC27 | `CLOSED` 룸 체크인 거절 | 룸 상태 `CLOSED` | 오류 | 운영 중 아님 오류 확인, PASS |
| TC28 | 관리자 시각변경+상태전이 | 과거 입력 거절 후 `11:11` | `NO_SHOW`, `COMPLETED` 반영 | 두 전이와 파일 반영 확인, PASS |
| TC29 | 전체 예약 조회 표시 | 관리자 조회 | `userId`,`userName` 표시 | 헤더/사용자명 출력 확인, PASS |
| TC30 | 예약 조정 `CLOSED` 룸 거절 | 대상 `R102(CLOSED)` | 오류 | `OPEN 상태 아님` 확인, PASS |
| TC31 | 예약 조정 같은 룸 거절 후 성공 | `R101` 거절 후 `R102` 이동 | 최종 성공 | 전/후 레코드와 저장 확인, PASS |
| TC32 | 과거 완료 예약 있는 상태 정원 변경 | `COMPLETED` 예약이 더 큼 | 변경 허용 | ROOM 변경 성공 확인, PASS |
| TC33 | 정원 변경 영향예약 이동 성공 | 동일 룸 거절 후 다른 룸 이동 | 영향예약 처리 + 변경 성공 | RESV/ROOM 변경 확인, PASS |
| TC34 | 정원 변경 전체 취소 복구 | 영향예약 처리 중 `0` 선택 | 원상복구 | ROOM/RESV 원복 확인, PASS |
| TC35 | 체크인 중 예약 있는 룸 휴업 거절 | `CHECKED_IN` 존재 | 오류 | 즉시 휴업 불가 확인, PASS |
| TC36 | 영향예약 삭제 후 룸 휴업 성공 | 미래 `RESERVED` 존재, 삭제 선택 | 휴업 성공 | ROOM `CLOSED`, 예약 삭제 확인, PASS |
| TC37 | 룸 운영 재개 성공 | `R101(CLOSED)` | `OPEN` 전환 | ROOM `OPEN` 확인, PASS |

## 근거

- 테스트 코드: `/Users/bonsu/IdeaProjects/untitled/src/test/java/ku/com/RegressionTest.java`
- HTML 리포트: `/Users/bonsu/IdeaProjects/untitled/out/reports/tests/test/index.html`
- OCR 추출본: `/tmp/inspection_criteria_ocr.txt`
