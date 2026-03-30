# 스터디룸 예약 관리 시스템 (Java 17 CLI)

사용자가 회원가입/로그인 후 스터디룸 예약 가능 여부를 조회하고, 예약/취소/체크인을 수행하는 Java 17 기반 CLI 프로그램입니다.  
관리자는 전체 예약 조회, 가상 현재 시각 변경, 예약 방 이동, 룸 정원/운영 상태 조정을 수행할 수 있습니다.

모든 시간 판단은 실제 시스템 시간이 아니라 `data/system_time.txt`의 공용 가상 현재 시각을 기준으로 합니다.

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

## 데이터 파일 형식

기본 데이터 폴더는 프로젝트 루트의 `data/` 입니다.

- `data/users.txt`
  - `USER|userId|password|userName|role`
- `data/rooms.txt`
  - `ROOM|roomId|roomName|maxCapacity|roomStatus`
- `data/reservations.txt`
  - `RESV|reservationId|userId|roomId|date|startTime|endTime|partySize|status|createdAt|checkedInAt`
- `data/system_time.txt`
  - `NOW|yyyy-MM-dd HH:mm`

공통 규칙:

- UTF-8
- 필드 구분자 `|`
- 빈 줄 허용
- `#` 시작 줄은 주석으로 무시

## 주요 기능

- 비로그인: 회원가입, 로그인, 종료
- member:
  - 현재 가상 시각 조회
  - 현재 가상 시각 변경
  - 예약 가능 스터디룸 조회
  - 예약 신청
  - 예약 취소
  - 나의 예약 조회
  - 체크인
- admin:
  - 현재 가상 시각 조회/변경
  - 전체 예약 정보 조회
  - 예약 조정(방 이동)
  - 룸 컨디션 관리

## 주요 규칙

- 예약 시작/종료는 정시(`HH:00`) 단위
- 예약 길이 1시간 이상 4시간 이하
- 같은 룸 시간 겹침 예약 금지
- 같은 사용자 시간 겹침 예약 금지
- 체크인 가능 구간: `[시작-10분, 시작+10분]`
- `CLOSED` 룸은 신규 예약/체크인 불가
- 예약 취소는 레코드 삭제로 처리
- 자동 상태 갱신:
  - `RESERVED` + 현재 시각이 `시작+10분` 초과 -> `NO_SHOW`
  - `CHECKED_IN` + 현재 시각이 종료 시각 이상 -> `COMPLETED`

## 파일 오류 처리 정책

- 프로그램 시작 시 데이터 파일을 검증합니다.
- 오류 발생 시 아래 형식으로 출력 후 시작을 중단합니다.
  - `[파일 오류] <파일명> <줄번호>행: <원인>`
  - 줄번호가 없으면 `[파일 오류] <파일명>: <원인>`
- 필수 파일이 없으면 기본 파일을 자동 생성합니다.
