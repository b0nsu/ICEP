# 07 Submission Ready Checklist

샘플 PDF `전기프-06-검사.pdf` 기준으로, 제출 직전 확인할 항목을 체크리스트로 정리했다.

## 첫 페이지

- [ ] 팀명 `C07` 표시
- [ ] `1차 검사 보고서` 문자열 표시
- [ ] 프로젝트 주제명 표시

## 본문 구성

- [ ] 기획서 흐름과 비슷한 절 순서 사용
- [ ] 각 TC 또는 TC 묶음이 어떤 기획서 절을 확인하는지 바로 보이게 표시
- [ ] 단위 검사 내용은 제외하고 전체 통합검사만 적기
- [ ] 같은 목표를 가진 TC는 표/묶음으로 정리
- [ ] 입력만 다른 경계값 TC는 대표 로그 1개만 붙여도 되는지 최종 확인

## 현재 inspection 자산 중 바로 옮길 위치

- 전체 TC 원본: [02_tc_matrix.md](/Users/bonsu/IdeaProjects/untitled/inspection/02_tc_matrix.md)
- 묶음형 본문 초안: [06_grouped_report_draft.md](/Users/bonsu/IdeaProjects/untitled/inspection/06_grouped_report_draft.md)
- 전체 요약표: [04_report_ready_tables.md](/Users/bonsu/IdeaProjects/untitled/inspection/04_report_ready_tables.md)
- 문서 불일치 부록: [03_discrepancies.md](/Users/bonsu/IdeaProjects/untitled/inspection/03_discrepancies.md)

## 대표 로그 후보

- 시작/파일 검증: `inspection/evidence/TC-2.3-01/terminal.log`
- 회원가입/로그인: `inspection/evidence/TC-6.1-01/terminal.log`
- 경계값 묶음: `inspection/evidence/TC-4.1-01/terminal.log`
- 예약 신청: `inspection/evidence/TC-6.2.3-01/terminal.log`
- 예약 길이 경계값: `inspection/evidence/TC-1.2-01/terminal.log`
- 영향 예약 처리: `inspection/evidence/TC-6.3.4-03/terminal.log`
- rollback: `inspection/evidence/TC-6.3.4-04/terminal.log`

## before/after 스냅샷 꼭 같이 볼 항목

- 예약 취소 삭제 확인: `TC-6.2.4-01`
- 예약 조정 roomId 변경: `TC-6.3.3-01`
- 정원 변경 + 영향 예약 이동: `TC-6.3.4-03`
- 정원 변경 rollback: `TC-6.3.4-04`
- 임시 휴업 + 영향 예약 삭제: `TC-6.3.4-07`
- 자동 상태 갱신: `TC-7.1-01`, `TC-7.1-02`

## 최종 출력 전 체크

- [ ] 목차 추가
- [ ] 페이지 번호 추가
- [ ] PDF 문자열 검색 가능 여부 확인
- [ ] 텍스트 복붙 블록은 고정폭 글꼴 유지
- [ ] FAIL이 있다면 숨기지 말고 예상/실제 차이를 그대로 표기
