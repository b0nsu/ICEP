# 00 Repo Facts

## 기준 자료
- 실제 코드 기준: 현재 브랜치 `v2`
- 실행 기준 문서: [README.md](/Users/bonsu/IdeaProjects/untitled/README.md)
- 기획서 기준 문서: `/Users/bonsu/Downloads/2026 전공기초프로젝트 C07팀 1차 기획서 원판 최종.pdf`
- 기획서 텍스트 추출본: [inspection/_sources/c07_plan_pdf_extract.txt](/Users/bonsu/IdeaProjects/untitled/inspection/_sources/c07_plan_pdf_extract.txt)

## 실제 실행 명령
- 빌드: `./build.sh`
- 실행: `./run.sh`
- 직접 실행: `java -jar out/study-room-cli.jar`

## 기본 data/ 구조
- 작업 디렉토리 기준 `data/`
- `users.txt`: `USER|userId|loginId|password|userName|role`
- `rooms.txt`: `ROOM|roomId|roomName|maxCapacity|roomStatus`
- `reservations.txt`: `RESV|reservationId|userId|roomId|date|startTime|endTime|partySize|status|createdAt|checkedInAt`
- `system_time.txt`: `NOW|yyyy-MM-dd HH:mm`

## 기본 admin 계정
- `userId=user001`
- `loginId=user001`
- `password=admin1234`
- `userName=admin`
- `role=admin`

## 현재 브랜치에서 확인한 핵심 규칙
- 회원가입 입력 필드: `loginId`, `password`, `userName`
- 회원가입 자동 발급 필드: `userId`
- 로그인 기준 필드: `loginId + password`
- 예약 취소 처리 방식: `RESERVED` 미래 예약만 취소 가능, 성공 시 예약 레코드 삭제
- 자동 상태 갱신: `RESERVED` + `현재시각 > 시작+10분` -> `NO_SHOW`, `CHECKED_IN` + `현재시각 >= 종료시각` -> `COMPLETED`
- member 메뉴: 현재 가상 시각 변경 / 예약 가능 스터디룸 조회 / 예약 신청 / 예약 취소 / 나의 예약 조회 / 체크인 / 로그아웃
- admin 메뉴: 현재 가상 시각 변경 / 전체 예약 정보 조회 / 예약 조정(방 이동) / 룸 컨디션 관리
- 룸 컨디션 관리: 전체 룸 조회 / 최대 수용 인원 변경 / 임시 휴업 / 운영 재개
- 파일 오류 형식: `[파일 오류] <파일명> <줄번호>행: <원인>` 또는 `[파일 오류] <파일명>: <원인>`
- 입력 오류 복귀 방식: 잘못된 메뉴 번호는 같은 메뉴에서 재입력, 필드 입력 형식 오류는 해당 기능을 중단하고 상위 메뉴로 복귀
- 영향 예약 처리 흐름: 정원 감소/임시 휴업 시 미래 `RESERVED` 예약을 순차적으로 `다른 룸 이동 / 해당 예약 취소 / 이번 변경 전체 취소` 중 하나로 처리

## 수집 통계
- 총 TC 수: 49
- 증거 폴더 루트: [inspection/evidence](/Users/bonsu/IdeaProjects/untitled/inspection/evidence)
