스터디룸 예약 관리 CLI 실행 방법

이 브랜치는 프로그램 실행과 빌드에 필요한 제출 파일만 포함합니다.

필수 실행 파일:
- study-room-cli.jar
- data/users.txt
- data/rooms.txt
- data/reservations.txt
- data/system_time.txt

빌드 관련 파일:
- src/main/java/
- build.gradle
- settings.gradle
- gradlew
- gradle/wrapper/

실행 방법:

```bash
java -jar study-room-cli.jar
```

빌드 방법:

```bash
sh gradlew jar
```

기본 로그인 계정:
- 관리자: admin / admin1234
- 회원: user101 / 12345
- 회원: user102 / 1234

주의:
- data 폴더는 study-room-cli.jar와 같은 폴더에 있어야 합니다.
- Java 17 이상 환경에서 실행하세요.
