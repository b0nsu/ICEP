# 03 Discrepancies

1. `MANUAL_TEST_SCENARIOS.md`는 회원가입/로그인 입력 필드를 `userId` 기준으로 설명하지만, 최종 C07 기획서와 실제 코드는 `loginId`를 직접 입력하고 `userId`를 자동 발급한다.
2. `MANUAL_TEST_SCENARIOS.md`는 체크인 허용 구간을 `[시작-10분, 시작+15분]`로 적고 있으나, 최종 C07 기획서와 실제 코드는 `[시작-10분, 시작+10분]`을 사용한다.
3. `MANUAL_TEST_SCENARIOS.md`는 예약 취소 성공 결과를 `CANCELLED` 전환으로 적고 있으나, 최종 C07 기획서와 실제 코드는 예약 레코드 삭제 방식이다.
4. 저장소의 구형 초안 문서 `docs/1차기획서_스터디룸예약CLI.md`는 30분 단위, `CANCELLED`, `MAINTENANCE`, 패널티, 연장 기능을 포함하지만, 최종 C07 PDF와 현재 브랜치 구현 범위에는 해당 규칙이 없다.
5. 구형 초안 문서 `docs/1차기획서_스터디룸예약CLI.md`는 `users.txt`에 penalty/status, `rooms.txt`에 운영시간/비품, `reservations.txt`에 extensionCount를 두는 구조를 적지만, 최종 C07 PDF와 현재 브랜치 실제 파일 형식은 더 단순한 6/5/11/2 필드 구조다.
6. 최종 C07 PDF 7.2는 data 폴더/필수 파일 누락을 “시작 단계 오류”라고 서술하면서도 2.3에서는 자동 생성 후 진행을 설명한다. 실제 구현은 생성이 가능하면 조용히 자동 생성 후 메뉴까지 진입한다.
