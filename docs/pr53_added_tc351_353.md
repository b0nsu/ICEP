# PR #53 추가 경계값 TC 공유

## 1. 새롭게 추가한 TC 목록
<!-- R2의 의미는 2차 보강 tc라는 의미여서 무시하면 됩니다.-->


| ID | 세부 목표 | 입력값 | 예상 결과 | 실제 결과 |
| --- | --- | --- | --- | --- |
| 5.2-R2-TC351 | 연장된 예약의 총 이용 시간 5시간 초과 거부 | `CHECKED_IN`, `10:00~16:00`, `checkedInAt=10:00`, `extensionCount=1` | 총 예약 길이가 5시간을 초과하므로 파일 오류 메시지를 출력하고 프로그램 시작을 중단해야 한다. | PASS - `[파일 오류] reservations.txt 1행: 예약 길이는 1시간 이상 5시간 이하이고 1시간 단위여야 합니다.` 출력 확인 |
| 5.2-R2-TC352 | `extensionCount` 최대 경계값 정상 로드 | `COMPLETED`, `10:00~15:00`, `checkedInAt=10:00`, `extensionCount=3` | `extensionCount=3`은 허용 상한값이므로 파일 오류 없이 `[비로그인 메뉴]`가 출력되어야 한다. | PASS - `[비로그인 메뉴]` 출력 확인 |
| 5.2-R2-TC353 | `checkedInAt` 하한 경계의 날짜 넘어감 정상 처리 | 예약일 `2026-03-20`, `startTime=00:00`, `endTime=01:00`, `status=CHECKED_IN`, `checkedInAt=2026-03-19 23:50`, `extensionCount=0` | `checkedInAt`이 예약 시작 10분 전이므로 파일 오류 없이 `[비로그인 메뉴]`가 출력되어야 한다. | PASS - `[비로그인 메뉴]` 출력 확인 |

