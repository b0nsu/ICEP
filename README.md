# 스터디룸 예약 관리 시스템 (Java 17 CLI)

## 프로젝트 개요
- Java 17 표준 라이브러리만 사용하는 순수 CLI 기반 스터디룸 예약 관리 시스템입니다.
- 실제 컴퓨터 시간이 아니라 `data/system_time.txt`의 공유 가상 현재 시각을 기준으로 동작합니다.
- `member` / `admin` 로그인 기반 다중 사용자 환경을 지원합니다.

## 실행 방법
### 1) 빌드
```bash
./build.sh
```

### 2) 실행
```bash
./run.sh
```

또는:
```bash
java -jar out/study-room-cli.jar
```

## 기본 admin 계정
- userId: `admin`
- password: `admin1234`

## 데이터 파일 위치와 형식 요약
기본 데이터 폴더는 프로젝트 루트의 `data/` 입니다.

- `data/users.txt`
  - `USER|userId|password|displayName|role|status`
- `data/rooms.txt`
  - `ROOM|roomId|roomName|maxCapacity|equipmentList|roomStatus`
- `data/reservations.txt`
  - `RESV|reservationId|userId|roomId|date|startTime|endTime|partySize|status|createdAt|checkedInAt`
- `data/system_time.txt`
  - `NOW|yyyy-MM-dd HH:mm`

공통 규칙:
- UTF-8
- 필드 구분자 `|`
- 빈 줄 허용
- `#` 시작 줄은 주석으로 무시

## 가상 현재 시각 규칙
- 시스템의 단일 현재 시각은 `data/system_time.txt`의 `NOW` 값입니다.
- admin만 현재 시각을 변경할 수 있습니다.
- 현재 시각은 과거로 되돌릴 수 없습니다.
- 시각 변경 직후 전체 예약 상태를 즉시 재판정합니다.
- 프로그램 시작 시에도 상태 갱신을 1회 수행합니다.

## 주요 기능 요약
- 비로그인: 회원가입, 로그인, 종료
- member:
  - 현재 가상 시각 조회
  - 예약 가능 스터디룸 조회
  - 예약 신청
  - 예약 취소
  - 나의 예약 조회
  - 체크인
- admin:
  - 현재 가상 시각 조회/변경
  - 전체 예약 조회
  - 전체 체크인 여부 조회
  - 예약 조정(방 이동)
  - 룸 컨디션 관리(정원 변경/임시 휴업/운영 재개)

## 주요 제약사항
- 예약 시작/종료는 30분 단위
- 시작 < 종료, 하루 넘김 금지
- 예약 길이 1시간 이상 4시간 이하
- 같은 룸 시간 겹침 예약 금지
- 같은 사용자 시간 겹침 예약 금지
- 체크인 가능 구간: `[시작-10분, 시작+15분]` (양 끝 포함)
- `CLOSED` 룸은 신규 예약/체크인 모두 금지
- 노쇼는 `NO_SHOW` 상태로만 기록하며 벌점/제재 기능은 구현하지 않음

## 잘못된 파일 처리 정책
- 파일 문법/의미 오류를 절대 조용히 무시하지 않습니다.
- 오류 발생 시 아래 형식으로 출력 후 프로그램 시작을 중단합니다.
  - `[파일 오류] <파일명> <줄번호>행: <원인>`
  - 줄번호가 없는 경우 `[파일 오류] <파일명>: <원인>`
- 파일이 없으면 기본 파일을 자동 생성합니다.
