# Branch Cleanup Notes

## Git 정리 결과

- `origin/v2`와 `codex-spec-loginid-align-v2`는 같은 트리 상태였다.
- `main`에는 `origin/v2` 내용을 기준으로 머지했다.
- `origin/main` 최신 커밋: `c8be1f2 chore: remove legacy root artifacts after v2 merge`
- 원격 중복 브랜치 `origin/codex-spec-loginid-align-v2`는 삭제했다.
- 로컬 중복 브랜치 `codex-spec-loginid-align-v2`도 삭제했다.
- 원격 `origin/develop`, `origin/documents`, `origin/v1`, `origin/v2-1`도 삭제했다.
- 로컬 `codex-spec-loginid-align`, `v10`, `v2-1`도 삭제했다.

## 남은 브랜치

- 로컬: `main`, `v2`
- 원격: `origin/main`, `origin/v2`

## 추가 폐기 판단 근거

- `origin/documents`
  - `main` 대비 고유 커밋 2개, 공통 유지선보다 오래된 문서 업로드 계열이었다.
  - 실제 diff는 최신 코드 기준보다 예전 `pdf/tex` 자산과 구형 CLI 파일 조합에 가까워 현행 라인과 거리가 멀었다.
- `origin/v1`
  - `main` 대비 고유 커밋 1개였지만, 현재 `data/` 구조와 최신 로그인/예약 규칙 반영 전 단계였다.
  - 최신 유지 라인보다 과거 상태 백업 성격이 강해 운영 브랜치로 남길 이유가 낮았다.
- `origin/v2-1`
  - `main` 대비 고유 커밋 3개였지만, 최상위 `users.txt/rooms.txt/...` 파일을 쓰던 과도기 브랜치였다.
  - 현재 표준인 `data/` 디렉토리 구조와 어긋나고, 이후 `v2`/`main` 라인에서 대체되었다.

## main에 반영하면서 같이 정리한 main-only 잔여 파일

아래 파일들은 `v2` 구조와 맞지 않는 오래된 최상위 잔여물이라 `main`에서 제거했다.

- `reservations.txt`
- `rooms.txt`
- `system_time.txt`
- `users.txt`
- `전기프_1차기획서원판_스터디룸예약관리CLI_구조동일.pdf`
- `전기프_1차기획서원판_스터디룸예약관리CLI_구조동일.tex`

## 현재 v2 작업트리에서 일부러 손대지 않은 로컬/비코드 자산

현재 작업트리 `v2`에는 사용자 로컬 변경과 무관 자산이 섞여 있어서, 이번 branch cleanup에서는 건드리지 않았다.

- 로컬 설정/도구: `.agents/`, `.idea/`, `.omx/`, `plugins/`, `src/.omx/`
- 보고서/증거 수집 산출물: `inspection/`
- draw.io 및 흐름도 자산: `[FLOW]_*.drawio`, `docs/*.drawio`, `docs/drawio-mcp.md`

## 일부러 유지한 것

- 현재 체크아웃된 로컬 `v2` 브랜치는 사용자 작업 중 변경이 있어 이동/리셋하지 않았다.
- 로컬 `v2`는 현재 `origin/v2`보다 뒤쳐져 있지만, 작업트리가 dirty 상태라 자동 fast-forward는 하지 않았다.
