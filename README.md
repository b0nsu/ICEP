# 스터디룸 예약 관리 CLI

Java 17 기반의 문자 인터페이스 프로그램입니다. 회원은 스터디룸 예약 조회, 예약, 취소, 체크인을 할 수 있고, 관리자는 전체 예약 조회와 예약 이동, 룸 정원 및 운영 상태 조정 기능을 사용할 수 있습니다.

모든 시간 판단은 실제 OS 시계가 아니라 `data/system_time.txt`에 저장된 공용 가상 현재 시각을 기준으로 합니다.

## 빌드

Windows:

```powershell
.\gradlew.bat clean check
```

macOS / Linux:

```bash
./gradlew clean check
```

## 실행

Windows:

```powershell
.\gradlew.bat run
```

macOS / Linux:

```bash
./gradlew run
```

## 기본 admin 계정

- userId: `user001`
- loginId: `user001`
- userName: `admin`
- password: `admin1234`

## 데이터 파일

프로젝트 루트의 `data/` 폴더를 사용합니다.

- `users.txt`: `USER|userId|loginId|password|userName|role`
- `rooms.txt`: `ROOM|roomId|roomName|maxCapacity|roomStatus`
- `reservations.txt`: `RESV|reservationId|userId|roomId|date|startTime|endTime|partySize|status|createdAt|checkedInAt`
- `system_time.txt`: `NOW|yyyy-MM-dd HH:mm`

공통 규칙:

- UTF-8 인코딩
- 필드 구분자 `|`
- 빈 줄 허용
- `#`로 시작하는 줄은 주석으로 무시

## 검증

`check` 작업은 컴파일과 함께 CLI 회귀 테스트를 실행합니다.
