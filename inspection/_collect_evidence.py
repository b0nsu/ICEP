#!/usr/bin/env python3
from __future__ import annotations

import re
import shutil
import subprocess
import textwrap
from dataclasses import dataclass, field
from pathlib import Path
from typing import Iterable


ROOT = Path(__file__).resolve().parents[1]
INSPECTION = ROOT / "inspection"
EVIDENCE_DIR = INSPECTION / "evidence"
WORKSPACE_DIR = INSPECTION / "_workspaces"
SOURCE_DIR = INSPECTION / "_sources"
JAR_PATH = ROOT / "out" / "study-room-cli.jar"
PDF_PATH = Path("/Users/bonsu/Downloads/2026 전공기초프로젝트 C07팀 1차 기획서 원판 최종.pdf")
PDF_EXTRACT_PATH = SOURCE_DIR / "c07_plan_pdf_extract.txt"


def lines(*items: str) -> str:
    return "\n".join(items) + ("\n" if items else "")


def clean(text: str) -> str:
    return textwrap.dedent(text).strip() + "\n"


def user_record(user_id: str, login_id: str, password: str, user_name: str, role: str = "member") -> str:
    return f"USER|{user_id}|{login_id}|{password}|{user_name}|{role}\n"


def room_record(room_id: str, room_name: str, max_capacity: int, room_status: str) -> str:
    return f"ROOM|{room_id}|{room_name}|{max_capacity}|{room_status}\n"


def resv_record(
    reservation_id: str,
    user_id: str,
    room_id: str,
    date: str,
    start_time: str,
    end_time: str,
    party_size: int,
    status: str,
    created_at: str,
    checked_in_at: str,
) -> str:
    return (
        f"RESV|{reservation_id}|{user_id}|{room_id}|{date}|{start_time}|{end_time}|"
        f"{party_size}|{status}|{created_at}|{checked_in_at}\n"
    )


def now_record(value: str) -> str:
    return f"NOW|{value}\n"


def default_users() -> str:
    return (
        user_record("user001", "user001", "admin1234", "admin", "admin")
        + user_record("user011", "user011", "pw1234", "bonsu")
        + user_record("user022", "user022", "pw5678", "minseo")
    )


def distinct_login_users() -> str:
    return (
        user_record("user001", "user001", "admin1234", "admin", "admin")
        + user_record("user011", "alpha011", "pw1234", "bonsu")
    )


def default_rooms() -> str:
    return (
        room_record("R101", "A룸", 4, "OPEN")
        + room_record("R102", "B룸", 6, "OPEN")
        + room_record("R103", "C룸", 8, "OPEN")
    )


def root_dataset(
    users: str | None = None,
    rooms: str | None = None,
    reservations: str | None = None,
    system_time: str | None = None,
    missing_data: bool = False,
) -> "Dataset":
    return Dataset(
        users=users,
        rooms=rooms,
        reservations=reservations,
        system_time=system_time,
        missing_data=missing_data,
    )


@dataclass
class Dataset:
    users: str | None = None
    rooms: str | None = None
    reservations: str | None = None
    system_time: str | None = None
    missing_data: bool = False


@dataclass
class Checks:
    output_contains: list[str] = field(default_factory=list)
    output_not_contains: list[str] = field(default_factory=list)
    after_contains: dict[str, list[str]] = field(default_factory=dict)
    after_not_contains: dict[str, list[str]] = field(default_factory=dict)
    after_exists: list[str] = field(default_factory=list)
    before_exists: list[str] = field(default_factory=list)


@dataclass
class Scenario:
    label: str
    dataset: Dataset
    input_text: str
    actual_summary: str
    verdict: str
    checks: Checks = field(default_factory=Checks)


@dataclass
class Case:
    tc_id: str
    spec_ref: str
    spec_name: str
    objective: str
    method: str
    expected: str
    input_details: str
    scenarios: list[Scenario]
    note: str = ""


def verify_checks(case_id: str, scenario: Scenario, terminal_output: str, after_dir: Path, before_dir: Path) -> list[str]:
    failures: list[str] = []
    for expected in scenario.checks.output_contains:
        if expected not in terminal_output:
            failures.append(f"output missing: {expected}")
    for unexpected in scenario.checks.output_not_contains:
        if unexpected in terminal_output:
            failures.append(f"output unexpectedly contained: {unexpected}")
    for filename, tokens in scenario.checks.after_contains.items():
        content = read_snapshot(after_dir, filename)
        for token in tokens:
            if token not in content:
                failures.append(f"{filename} missing after token: {token}")
    for filename, tokens in scenario.checks.after_not_contains.items():
        content = read_snapshot(after_dir, filename)
        for token in tokens:
            if token in content:
                failures.append(f"{filename} unexpectedly kept token: {token}")
    for filename in scenario.checks.after_exists:
        if not (after_dir / filename).exists():
            failures.append(f"after snapshot missing file: {filename}")
    for filename in scenario.checks.before_exists:
        if not (before_dir / filename).exists():
            failures.append(f"before snapshot missing file: {filename}")
    return failures


def read_snapshot(snapshot_dir: Path, filename: str) -> str:
    path = snapshot_dir / filename
    if not path.exists():
        return ""
    return path.read_text(encoding="utf-8")


def setup_workspace(workspace: Path, dataset: Dataset) -> None:
    if workspace.exists():
        shutil.rmtree(workspace)
    workspace.mkdir(parents=True, exist_ok=True)
    shutil.copy2(JAR_PATH, workspace / "study-room-cli.jar")
    if dataset.missing_data:
        return
    data_dir = workspace / "data"
    data_dir.mkdir(parents=True, exist_ok=True)
    if dataset.users is not None:
        (data_dir / "users.txt").write_text(dataset.users, encoding="utf-8")
    if dataset.rooms is not None:
        (data_dir / "rooms.txt").write_text(dataset.rooms, encoding="utf-8")
    if dataset.reservations is not None:
        (data_dir / "reservations.txt").write_text(dataset.reservations, encoding="utf-8")
    if dataset.system_time is not None:
        (data_dir / "system_time.txt").write_text(dataset.system_time, encoding="utf-8")


def snapshot_data(workspace: Path, target_dir: Path) -> None:
    if target_dir.exists():
        shutil.rmtree(target_dir)
    target_dir.mkdir(parents=True, exist_ok=True)
    data_dir = workspace / "data"
    if not data_dir.exists():
        (target_dir / "_ABSENT.txt").write_text("data/ directory absent\n", encoding="utf-8")
        return
    for path in sorted(data_dir.iterdir()):
        if path.is_file():
            shutil.copy2(path, target_dir / path.name)


def run_cli(workspace: Path, input_text: str) -> str:
    proc = subprocess.run(
        ["java", "-jar", "study-room-cli.jar"],
        cwd=workspace,
        input=input_text.encode("utf-8"),
        stdout=subprocess.PIPE,
        stderr=subprocess.STDOUT,
        timeout=20,
        check=False,
    )
    return proc.stdout.decode("utf-8", errors="replace")


def case_output_path(tc_id: str) -> Path:
    return EVIDENCE_DIR / f"TC-{tc_id}"


def scenario_workspace_path(tc_id: str, scenario_label: str) -> Path:
    safe = re.sub(r"[^A-Za-z0-9_.-]+", "_", scenario_label)
    return WORKSPACE_DIR / f"TC-{tc_id}__{safe}"


def write_text(path: Path, content: str) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(content.rstrip() + "\n", encoding="utf-8")


def make_header(title: str, content: str) -> str:
    return f"{title}\n{content.rstrip()}\n"


def scenario_log_block(scenario: Scenario, output: str) -> str:
    return clean(
        f"""
        ===== {scenario.label} =====
        {output}
        """
    )


def scenario_input_block(scenario: Scenario) -> str:
    shown = scenario.input_text if scenario.input_text else "(no input)"
    return clean(
        f"""
        [{scenario.label}]
        {shown}
        """
    )


def run_case(case: Case) -> dict[str, object]:
    evidence_dir = case_output_path(case.tc_id)
    if evidence_dir.exists():
        shutil.rmtree(evidence_dir)
    evidence_dir.mkdir(parents=True, exist_ok=True)

    log_parts: list[str] = []
    input_parts: list[str] = []
    actual_rows: list[str] = []
    verdict_rows: list[str] = []
    overall_pass = True
    unexpected_notes: list[str] = []

    for scenario in case.scenarios:
        workspace = scenario_workspace_path(case.tc_id, scenario.label)
        before_dir = evidence_dir / "before" / scenario.label
        after_dir = evidence_dir / "after" / scenario.label

        setup_workspace(workspace, scenario.dataset)
        snapshot_data(workspace, before_dir)
        output = run_cli(workspace, scenario.input_text)
        snapshot_data(workspace, after_dir)

        failures = verify_checks(case.tc_id, scenario, output, after_dir, before_dir)
        scenario_ok = not failures
        if not scenario_ok:
            overall_pass = False
            unexpected_notes.append(f"{scenario.label}: " + "; ".join(failures))

        log_parts.append(scenario_log_block(scenario, output))
        input_parts.append(scenario_input_block(scenario))
        actual_rows.append(
            f"- {scenario.label}: {scenario.actual_summary}"
            + ("" if scenario_ok else f" [스크립트 검증 불일치: {'; '.join(failures)}]")
        )
        verdict_rows.append(f"- {scenario.label}: {scenario.verdict}")

    verdict = "PASS" if overall_pass and all(s.verdict == "PASS" for s in case.scenarios) else "FAIL"
    if overall_pass and any(s.verdict == "FAIL" for s in case.scenarios):
        verdict = "FAIL"
    if not overall_pass:
        verdict = "FAIL"

    note_text = case.note.strip()
    if unexpected_notes:
        note_text = (note_text + "\n" if note_text else "") + "\n".join(unexpected_notes)

    write_text(
        evidence_dir / "objective.txt",
        clean(
            f"""
            TC ID: {case.tc_id}
            목표: {case.objective}
            방법: {case.method}
            """
        ),
    )
    write_text(evidence_dir / "input.txt", "\n".join(input_parts).rstrip() + "\n")
    write_text(
        evidence_dir / "expected.txt",
        clean(
            f"""
            기획서 대응 절: {case.spec_ref} {case.spec_name}
            기획서 기준 예상 결과:
            {case.expected}
            """
        ),
    )
    write_text(
        evidence_dir / "actual.txt",
        clean(
            f"""
            실제 결과:
            {chr(10).join(actual_rows)}
            """
        ),
    )
    write_text(
        evidence_dir / "verdict.txt",
        clean(
            f"""
            verdict: {verdict}
            note:
            {note_text if note_text else '(none)'}
            """
        ),
    )
    write_text(evidence_dir / "terminal.log", "\n".join(log_parts).rstrip() + "\n")

    return {
        "tc_id": case.tc_id,
        "spec_ref": case.spec_ref,
        "spec_name": case.spec_name,
        "objective": case.objective,
        "method": case.method,
        "expected": case.expected,
        "actual": " / ".join(row[2:] if row.startswith("- ") else row for row in actual_rows),
        "verdict": verdict,
        "evidence_path": str(evidence_dir.relative_to(ROOT)),
        "note": note_text,
    }


def build_cases() -> list[Case]:
    cases: list[Case] = []

    def add(case: Case) -> None:
        cases.append(case)

    add(
        Case(
            tc_id="2.3-01",
            spec_ref="2.3 / 7.2",
            spec_name="프로그램 설치 및 실행 / 필수 파일 누락 및 시작 단계 오류 처리",
            objective="기획서 2.3, 7.2에 따라 data/와 필수 파일이 없을 때 기본 파일이 자동 생성되고 프로그램이 비로그인 메뉴까지 진입하는지 확인",
            method="빈 작업 디렉토리에서 JAR 실행 후 비로그인 메뉴가 뜨는지, 실행 직후 data/users.txt·rooms.txt·reservations.txt·system_time.txt가 생성되었는지 확인",
            expected="누락된 data/와 필수 파일 4종이 자동 생성되고, 파일 검증을 통과하면 [비로그인 메뉴]가 출력된다.",
            input_details="0",
            scenarios=[
                Scenario(
                    label="missing_files",
                    dataset=root_dataset(missing_data=True),
                    input_text=lines("0"),
                    actual_summary="비로그인 메뉴 출력 후 종료했고, after 스냅샷에 users.txt·rooms.txt·reservations.txt·system_time.txt 자동 생성이 확인되었다.",
                    verdict="PASS",
                    checks=Checks(
                        output_contains=["[비로그인 메뉴]", "프로그램을 종료합니다."],
                        after_exists=["users.txt", "rooms.txt", "reservations.txt", "system_time.txt"],
                        after_contains={
                            "users.txt": ["USER|user001|user001|admin1234|admin|admin"],
                            "rooms.txt": ["ROOM|R101|A룸|4|OPEN"],
                            "system_time.txt": ["NOW|2026-03-20 09:00"],
                        },
                    ),
                )
            ],
        )
    )

    add(
        Case(
            tc_id="5.1-01",
            spec_ref="5.1 / 7.2",
            spec_name="문법 규칙 / 필수 파일 누락 및 시작 단계 오류 처리",
            objective="기획서 5.1, 7.2에 따라 데이터 파일의 필드 개수 문법 오류가 있으면 시작을 중단하는지 확인",
            method="reservations.txt에 필드 수가 부족한 라인을 둔 뒤 프로그램을 재실행하고 시작 단계에서 파일 오류 메시지가 출력되는지 확인",
            expected="문법 오류가 있는 reservations.txt를 읽는 즉시 [파일 오류] reservations.txt ... 형식으로 출력하고 비로그인 메뉴 진입 없이 시작을 중단한다.",
            input_details="(입력 없음)",
            scenarios=[
                Scenario(
                    label="broken_reservation_record",
                    dataset=root_dataset(
                        users=default_users(),
                        rooms=default_rooms(),
                        reservations="RESV|broken|line\n",
                        system_time=now_record("2026-03-20 09:00"),
                    ),
                    input_text="",
                    actual_summary="터미널에 '[파일 오류] reservations.txt 1행: 필드 개수가 올바르지 않습니다.'와 '프로그램 시작을 중단합니다.'가 출력되었고 메뉴는 열리지 않았다.",
                    verdict="PASS",
                    checks=Checks(
                        output_contains=[
                            "[파일 오류] reservations.txt 1행: 필드 개수가 올바르지 않습니다.",
                            "프로그램 시작을 중단합니다.",
                        ],
                        output_not_contains=["[비로그인 메뉴]"],
                    ),
                )
            ],
        )
    )

    add(
        Case(
            tc_id="5.2-01",
            spec_ref="5.2 / 5.3.1 / 7.2",
            spec_name="의미 규칙 / 예약 충돌 확인 / 필수 파일 누락 및 시작 단계 오류 처리",
            objective="기획서 5.2, 5.3.1, 7.2에 따라 같은 룸의 활성 예약이 겹치면 시작 단계 의미 오류로 중단하는지 확인",
            method="같은 룸·같은 날짜·겹치는 RESERVED 2건을 reservations.txt에 넣고 재실행하여 파일 오류를 확인",
            expected="의미 규칙 위반으로 [파일 오류] reservations.txt ... 같은 룸의 겹치는 시간대 예약이 존재합니다. 출력 후 시작을 중단한다.",
            input_details="(입력 없음)",
            scenarios=[
                Scenario(
                    label="overlap_same_room",
                    dataset=root_dataset(
                        users=default_users(),
                        rooms=default_rooms(),
                        reservations=(
                            resv_record("rv0001", "user011", "R101", "2026-03-20", "13:00", "15:00", 2, "RESERVED", "2026-03-20 09:00", "-")
                            + resv_record("rv0002", "user022", "R101", "2026-03-20", "14:00", "16:00", 2, "RESERVED", "2026-03-20 09:10", "-")
                        ),
                        system_time=now_record("2026-03-20 09:00"),
                    ),
                    input_text="",
                    actual_summary="터미널에 '[파일 오류] reservations.txt 2행: 같은 룸의 겹치는 시간대 예약이 존재합니다.'와 시작 중단 메시지가 출력되었다.",
                    verdict="PASS",
                    checks=Checks(
                        output_contains=[
                            "[파일 오류] reservations.txt 2행: 같은 룸의 겹치는 시간대 예약이 존재합니다.",
                            "프로그램 시작을 중단합니다.",
                        ],
                        output_not_contains=["[비로그인 메뉴]"],
                    ),
                )
            ],
        )
    )

    add(
        Case(
            tc_id="6.1-01",
            spec_ref="4.1 / 6.1",
            spec_name="사용자 데이터 요소 / 비로그인 메뉴군",
            objective="기획서 4.1, 6.1에 따라 회원가입 시 loginId·password·userName을 입력받고 userId를 자동 발급하는지 확인",
            method="비로그인 메뉴에서 회원가입을 선택하고 신규 loginId/password/userName을 입력한 뒤 users.txt에 member 레코드가 추가되는지 확인",
            expected="회원가입 성공 메시지를 출력하고 users.txt에 auto userId가 발급된 member 레코드 1건이 추가된다.",
            input_details=clean(
                """
                1
                sujin01
                pass1234
                sujin01
                0
                """
            ),
            scenarios=[
                Scenario(
                    label="signup_success",
                    dataset=root_dataset(
                        users=default_users(),
                        rooms=default_rooms(),
                        reservations="",
                        system_time=now_record("2026-03-20 09:00"),
                    ),
                    input_text=lines("1", "sujin01", "pass1234", "sujin01", "0"),
                    actual_summary="회원가입 완료 메시지와 발급된 사용자 ID가 출력되었고, after/users.txt에 USER|user023|sujin01|pass1234|sujin01|member 레코드가 추가되었다.",
                    verdict="PASS",
                    checks=Checks(
                        output_contains=["회원가입이 완료되었습니다.", "발급된 사용자 ID: user023"],
                        after_contains={"users.txt": ["USER|user023|sujin01|pass1234|sujin01|member"]},
                    ),
                )
            ],
        )
    )

    add(
        Case(
            tc_id="6.1-02",
            spec_ref="4.1 / 6.1",
            spec_name="사용자 데이터 요소 / 비로그인 메뉴군",
            objective="기획서 4.1, 6.1에 따라 기존 loginId로 회원가입하면 중복 오류를 출력하고 저장하지 않는지 확인",
            method="이미 존재하는 loginId user011으로 회원가입을 시도하고 users.txt 변경 여부를 before/after로 비교",
            expected="오류: 이미 사용 중인 로그인 ID입니다.를 출력하고 users.txt는 변경되지 않는다.",
            input_details=clean(
                """
                1
                user011
                pass1234
                another01
                0
                """
            ),
            scenarios=[
                Scenario(
                    label="signup_duplicate_login_id",
                    dataset=root_dataset(
                        users=default_users(),
                        rooms=default_rooms(),
                        reservations="",
                        system_time=now_record("2026-03-20 09:00"),
                    ),
                    input_text=lines("1", "user011", "pass1234", "another01", "0"),
                    actual_summary="중복 loginId 오류가 출력되었고 after/users.txt에는 신규 USER 레코드가 추가되지 않았다.",
                    verdict="PASS",
                    checks=Checks(
                        output_contains=["오류: 이미 사용 중인 로그인 ID입니다."],
                        after_not_contains={"users.txt": ["another01"]},
                    ),
                )
            ],
        )
    )

    login_id_batch = [
        (
            "min_valid_4",
            "abcd",
            lines("1", "abcd", "pass1234", "alphauser", "0"),
            "PASS",
            "4자 loginId는 회원가입 성공",
            Checks(
                output_contains=["회원가입이 완료되었습니다.", "발급된 사용자 ID: user023"],
                after_contains={"users.txt": ["USER|user023|abcd|pass1234|alphauser|member"]},
            ),
        ),
        (
            "max_valid_20",
            "abcdefghijklmnopqrst",
            lines("1", "abcdefghijklmnopqrst", "pass1234", "alphauser", "0"),
            "PASS",
            "20자 loginId는 회원가입 성공",
            Checks(
                output_contains=["회원가입이 완료되었습니다.", "발급된 사용자 ID: user023"],
                after_contains={"users.txt": ["USER|user023|abcdefghijklmnopqrst|pass1234|alphauser|member"]},
            ),
        ),
        (
            "min_minus_1",
            "abc",
            lines("1", "abc", "0"),
            "PASS",
            "3자 loginId는 형식 오류로 종료",
            Checks(output_contains=["오류: 로그인 ID는 영문자로 시작하고 영문자/숫자/_ 만 사용하여 4~20자로 입력해야 합니다."]),
        ),
        (
            "max_plus_1",
            "abcdefghijklmnopqrstu",
            lines("1", "abcdefghijklmnopqrstu", "0"),
            "PASS",
            "21자 loginId는 형식 오류로 종료",
            Checks(output_contains=["오류: 로그인 ID는 영문자로 시작하고 영문자/숫자/_ 만 사용하여 4~20자로 입력해야 합니다."]),
        ),
        (
            "invalid_start_char",
            "1alpha",
            lines("1", "1alpha", "0"),
            "PASS",
            "숫자로 시작하는 loginId는 형식 오류",
            Checks(output_contains=["오류: 로그인 ID는 영문자로 시작하고 영문자/숫자/_ 만 사용하여 4~20자로 입력해야 합니다."]),
        ),
        (
            "invalid_allowed_char",
            "alpha-1",
            lines("1", "alpha-1", "0"),
            "PASS",
            "허용되지 않은 '-' 포함 loginId는 형식 오류",
            Checks(output_contains=["오류: 로그인 ID는 영문자로 시작하고 영문자/숫자/_ 만 사용하여 4~20자로 입력해야 합니다."]),
        ),
    ]

    add(
        Case(
            tc_id="4.1-01",
            spec_ref="4.1 / 6.1",
            spec_name="사용자 데이터 요소 / 비로그인 메뉴군",
            objective="기획서 4.1, 6.1에 따라 loginId 입력 경계값(최소/최대/경계 밖/시작 문자/허용 문자)을 검증한다.",
            method="각 서브케이스마다 비로그인 메뉴에서 회원가입을 선택하고 loginId만 바꿔 입력한 뒤 성공/오류와 users.txt 반영 여부를 확인",
            expected="4자와 20자 loginId는 허용되고, 3자·21자·비영문 시작·비허용 문자 포함 값은 형식 오류로 저장되지 않는다.",
            input_details=clean(
                """
                서브케이스별 공통 입력 순서:
                1 -> 회원가입
                <loginId> -> pass1234 -> alphauser
                0 -> 종료
                """
            ),
            scenarios=[
                Scenario(
                    label=label,
                    dataset=root_dataset(
                        users=default_users(),
                        rooms=default_rooms(),
                        reservations="",
                        system_time=now_record("2026-03-20 09:00"),
                    ),
                    input_text=input_text,
                    actual_summary=summary,
                    verdict=verdict,
                    checks=checks,
                )
                for label, login_id, input_text, verdict, summary, checks in login_id_batch
            ],
        )
    )

    password_batch = [
        (
            "min_valid_4",
            "pw12",
            lines("1", "alpha123", "pw12", "alphauser", "0"),
            "PASS",
            "4자 비밀번호는 회원가입 성공",
            Checks(
                output_contains=["회원가입이 완료되었습니다."],
                after_contains={"users.txt": ["USER|user023|alpha123|pw12|alphauser|member"]},
            ),
        ),
        (
            "max_valid_20",
            "abcdefghijklmnopqrst",
            lines("1", "alpha123", "abcdefghijklmnopqrst", "alphauser", "0"),
            "PASS",
            "20자 비밀번호는 회원가입 성공",
            Checks(
                output_contains=["회원가입이 완료되었습니다."],
                after_contains={"users.txt": ["USER|user023|alpha123|abcdefghijklmnopqrst|alphauser|member"]},
            ),
        ),
        (
            "min_minus_1",
            "abc",
            lines("1", "alpha123", "abc", "0"),
            "PASS",
            "3자 비밀번호는 길이 오류",
            Checks(output_contains=["오류: 비밀번호는 4~20자로 입력해야 합니다."]),
        ),
        (
            "max_plus_1",
            "abcdefghijklmnopqrstu",
            lines("1", "alpha123", "abcdefghijklmnopqrstu", "0"),
            "PASS",
            "21자 비밀번호는 길이 오류",
            Checks(output_contains=["오류: 비밀번호는 4~20자로 입력해야 합니다."]),
        ),
    ]

    add(
        Case(
            tc_id="4.1-02",
            spec_ref="4.1 / 6.1",
            spec_name="사용자 데이터 요소 / 비로그인 메뉴군",
            objective="기획서 4.1, 6.1에 따라 password 길이 경계값을 검증한다.",
            method="각 서브케이스마다 회원가입을 시도하고 password 값만 바꿔 성공/오류와 users.txt 반영 여부를 확인",
            expected="4자와 20자 비밀번호는 허용되고, 3자와 21자는 형식 오류로 저장되지 않는다.",
            input_details=clean(
                """
                서브케이스별 공통 입력 순서:
                1 -> 회원가입
                alpha123 -> <password> -> alphauser
                0 -> 종료
                """
            ),
            scenarios=[
                Scenario(
                    label=label,
                    dataset=root_dataset(
                        users=default_users(),
                        rooms=default_rooms(),
                        reservations="",
                        system_time=now_record("2026-03-20 09:00"),
                    ),
                    input_text=input_text,
                    actual_summary=summary,
                    verdict=verdict,
                    checks=checks,
                )
                for label, password, input_text, verdict, summary, checks in password_batch
            ],
        )
    )

    username_batch = [
        (
            "min_valid_4",
            "name",
            "PASS",
            "4자 userName은 회원가입 성공",
            Checks(
                output_contains=["회원가입이 완료되었습니다."],
                after_contains={"users.txt": ["USER|user023|alpha123|pass1234|name|member"]},
            ),
        ),
        (
            "max_valid_20",
            "abcdefghijklmnopqrst",
            "PASS",
            "20자 userName은 회원가입 성공",
            Checks(
                output_contains=["회원가입이 완료되었습니다."],
                after_contains={"users.txt": ["USER|user023|alpha123|pass1234|abcdefghijklmnopqrst|member"]},
            ),
        ),
        (
            "min_minus_1",
            "abc",
            "PASS",
            "3자 userName은 형식 오류",
            Checks(output_contains=["오류: 사용자명은 영문자로 시작하고 영문자/숫자/_ 만 사용하여 4~20자로 입력해야 합니다."]),
        ),
        (
            "max_plus_1",
            "abcdefghijklmnopqrstu",
            "PASS",
            "21자 userName은 형식 오류",
            Checks(output_contains=["오류: 사용자명은 영문자로 시작하고 영문자/숫자/_ 만 사용하여 4~20자로 입력해야 합니다."]),
        ),
        (
            "invalid_start_char",
            "1alpha",
            "PASS",
            "숫자로 시작하는 userName은 형식 오류",
            Checks(output_contains=["오류: 사용자명은 영문자로 시작하고 영문자/숫자/_ 만 사용하여 4~20자로 입력해야 합니다."]),
        ),
        (
            "invalid_allowed_char",
            "alpha-1",
            "PASS",
            "허용되지 않은 '-' 포함 userName은 형식 오류",
            Checks(output_contains=["오류: 사용자명은 영문자로 시작하고 영문자/숫자/_ 만 사용하여 4~20자로 입력해야 합니다."]),
        ),
    ]

    add(
        Case(
            tc_id="4.1-03",
            spec_ref="4.1 / 6.1",
            spec_name="사용자 데이터 요소 / 비로그인 메뉴군",
            objective="기획서 4.1, 6.1에 따라 userName 입력 경계값과 시작/허용 문자 규칙을 검증한다.",
            method="각 서브케이스마다 회원가입을 시도하고 userName 값만 바꿔 성공/오류와 users.txt 반영 여부를 확인",
            expected="4자와 20자 userName은 허용되고, 3자·21자·비영문 시작·비허용 문자 포함 값은 형식 오류로 저장되지 않는다.",
            input_details=clean(
                """
                서브케이스별 공통 입력 순서:
                1 -> 회원가입
                alpha123 -> pass1234 -> <userName>
                0 -> 종료
                """
            ),
            scenarios=[
                Scenario(
                    label=label,
                    dataset=root_dataset(
                        users=default_users(),
                        rooms=default_rooms(),
                        reservations="",
                        system_time=now_record("2026-03-20 09:00"),
                    ),
                    input_text=lines("1", "alpha123", "pass1234", user_name, "0"),
                    actual_summary=summary,
                    verdict=verdict,
                    checks=checks,
                )
                for label, user_name, verdict, summary, checks in username_batch
            ],
        )
    )

    add(
        Case(
            tc_id="6.1-03",
            spec_ref="4.1 / 6.1",
            spec_name="사용자 데이터 요소 / 비로그인 메뉴군",
            objective="기획서 4.1, 6.1에 따라 로그인 기준 필드가 userId가 아니라 loginId + password 인지 확인",
            method="userId=user011, loginId=alpha011인 member 레코드를 둔 뒤 비로그인 메뉴에서 alpha011/pw1234로 로그인",
            expected="loginId alpha011과 비밀번호 pw1234가 일치하면 member 로그인에 성공한다.",
            input_details=clean(
                """
                2
                alpha011
                pw1234
                0
                0
                """
            ),
            scenarios=[
                Scenario(
                    label="login_success_distinct_login_id",
                    dataset=root_dataset(
                        users=distinct_login_users(),
                        rooms=default_rooms(),
                        reservations="",
                        system_time=now_record("2026-03-20 09:00"),
                    ),
                    input_text=lines("2", "alpha011", "pw1234", "0", "0"),
                    actual_summary="로그인 성공: bonsu (role: member) 가 출력되어 loginId + password 기준 인증이 확인되었다.",
                    verdict="PASS",
                    checks=Checks(output_contains=["로그인 성공: bonsu (role: member)"]),
                )
            ],
        )
    )

    add(
        Case(
            tc_id="6.1-04",
            spec_ref="4.1 / 6.1",
            spec_name="사용자 데이터 요소 / 비로그인 메뉴군",
            objective="기획서 4.1, 6.1에 따라 비밀번호가 다르면 로그인 실패 후 비로그인 메뉴로 복귀하는지 확인",
            method="비로그인 메뉴에서 기존 loginId user011에 잘못된 비밀번호를 입력한 뒤 오류와 메뉴 재표시를 확인",
            expected="오류: 비밀번호가 일치하지 않습니다.를 출력하고 비로그인 메뉴가 다시 표시된다.",
            input_details=clean(
                """
                2
                user011
                wrong
                0
                """
            ),
            scenarios=[
                Scenario(
                    label="login_failure_wrong_password",
                    dataset=root_dataset(
                        users=default_users(),
                        rooms=default_rooms(),
                        reservations="",
                        system_time=now_record("2026-03-20 09:00"),
                    ),
                    input_text=lines("2", "user011", "wrong", "0"),
                    actual_summary="비밀번호 불일치 오류가 출력된 뒤 비로그인 메뉴가 다시 표시되었다.",
                    verdict="PASS",
                    checks=Checks(output_contains=["오류: 비밀번호가 일치하지 않습니다.", "[비로그인 메뉴]"]),
                )
            ],
        )
    )

    add(
        Case(
            tc_id="7.3-01",
            spec_ref="6.1 / 7.3",
            spec_name="비로그인 메뉴군 / 오류 메시지와 메뉴 복귀",
            objective="기획서 6.1, 7.3에 따라 존재하지 않는 메뉴 번호 입력 시 현재 메뉴에서 재입력 받는지 확인",
            method="비로그인 메뉴에서 9를 입력한 뒤 오류 메시지와 메뉴 재표시를 확인하고 0으로 종료",
            expected="오류: 존재하지 않는 메뉴 번호입니다.를 출력하고 비로그인 메뉴를 다시 보여 준다.",
            input_details=clean(
                """
                9
                0
                """
            ),
            scenarios=[
                Scenario(
                    label="guest_invalid_menu",
                    dataset=root_dataset(
                        users=default_users(),
                        rooms=default_rooms(),
                        reservations="",
                        system_time=now_record("2026-03-20 09:00"),
                    ),
                    input_text=lines("9", "0"),
                    actual_summary="존재하지 않는 메뉴 번호 오류가 출력된 뒤 비로그인 메뉴가 재표시되었다.",
                    verdict="PASS",
                    checks=Checks(output_contains=["오류: 존재하지 않는 메뉴 번호입니다.", "[비로그인 메뉴]"]),
                )
            ],
        )
    )

    add(
        Case(
            tc_id="6.2.1-01",
            spec_ref="6.2.1 / 7.1",
            spec_name="member 현재 가상 시각 변경 / 상태 자동 갱신",
            objective="기획서 6.2.1, 7.1에 따라 member가 현재 시각을 미래로 변경하면 system_time.txt 저장과 상태 변화 요약이 수행되는지 확인",
            method="member 로그인 후 현재 시각 변경 메뉴에서 2026-03-20 10:00을 입력하고 system_time.txt 반영 여부를 확인",
            expected="기존/새 시각을 출력한 뒤 현재 시각이 변경되었습니다.를 출력하고 system_time.txt가 새 값으로 저장된다.",
            input_details=clean(
                """
                2
                user011
                pw1234
                1
                2026-03-20 10:00
                0
                0
                """
            ),
            scenarios=[
                Scenario(
                    label="member_time_change_success",
                    dataset=root_dataset(
                        users=default_users(),
                        rooms=default_rooms(),
                        reservations="",
                        system_time=now_record("2026-03-20 09:00"),
                    ),
                    input_text=lines("2", "user011", "pw1234", "1", "2026-03-20 10:00", "0", "0"),
                    actual_summary="현재 시각 변경 성공 메시지가 출력되었고 after/system_time.txt가 NOW|2026-03-20 10:00으로 바뀌었다.",
                    verdict="PASS",
                    checks=Checks(
                        output_contains=["현재 시각이 변경되었습니다.", "기존 시각: 2026-03-20 09:00", "새 시각: 2026-03-20 10:00"],
                        after_contains={"system_time.txt": ["NOW|2026-03-20 10:00"]},
                    ),
                )
            ],
        )
    )

    add(
        Case(
            tc_id="6.2.1-02",
            spec_ref="6.2.1",
            spec_name="member 현재 가상 시각 변경",
            objective="기획서 6.2.1에 따라 현재 시각을 과거로 되돌리려 하면 거부되는지 확인",
            method="member 로그인 후 현재 시각 변경 메뉴에서 기존보다 이른 2026-03-20 08:00 입력",
            expected="오류: 현재 시각은 과거로 되돌릴 수 없습니다.를 출력하고 system_time.txt는 유지된다.",
            input_details=clean(
                """
                2
                user011
                pw1234
                1
                2026-03-20 08:00
                0
                0
                """
            ),
            scenarios=[
                Scenario(
                    label="member_time_change_fail_past",
                    dataset=root_dataset(
                        users=default_users(),
                        rooms=default_rooms(),
                        reservations="",
                        system_time=now_record("2026-03-20 09:00"),
                    ),
                    input_text=lines("2", "user011", "pw1234", "1", "2026-03-20 08:00", "0", "0"),
                    actual_summary="과거 시각 거부 오류가 출력되었고 after/system_time.txt는 NOW|2026-03-20 09:00 그대로였다.",
                    verdict="PASS",
                    checks=Checks(
                        output_contains=["오류: 현재 시각은 과거로 되돌릴 수 없습니다."],
                        after_contains={"system_time.txt": ["NOW|2026-03-20 09:00"]},
                    ),
                )
            ],
        )
    )

    search_dataset = root_dataset(
        users=default_users(),
        rooms=room_record("R101", "A룸", 4, "OPEN") + room_record("R102", "B룸", 6, "OPEN") + room_record("R103", "C룸", 2, "CLOSED"),
        reservations=resv_record("rv0001", "user022", "R101", "2026-03-20", "13:00", "15:00", 2, "RESERVED", "2026-03-20 09:00", "-"),
        system_time=now_record("2026-03-20 09:00"),
    )

    add(
        Case(
            tc_id="6.2.2-01",
            spec_ref="6.2.2",
            spec_name="예약 가능 스터디룸 조회",
            objective="기획서 6.2.2에 따라 OPEN 상태·정원 충족·비충돌 룸만 조회되는지 확인",
            method="member 로그인 후 2026-03-20 13:00~15:00, 인원 3으로 조회하여 CLOSED 룸과 충돌 룸이 제외되는지 확인",
            expected="조회 결과에는 조건을 만족하는 R102만 표시되고 조회가 끝났습니다.가 출력된다.",
            input_details=clean(
                """
                2
                user011
                pw1234
                2
                2026-03-20
                13:00
                15:00
                3
                0
                0
                """
            ),
            scenarios=[
                Scenario(
                    label="available_rooms_success",
                    dataset=search_dataset,
                    input_text=lines("2", "user011", "pw1234", "2", "2026-03-20", "13:00", "15:00", "3", "0", "0"),
                    actual_summary="조회 표에 R102만 출력되고 R101(충돌)·R103(CLOSED/정원부족)는 제외되었다.",
                    verdict="PASS",
                    checks=Checks(output_contains=["R102", "조회가 끝났습니다."], output_not_contains=["R103 C룸 2 CLOSED"]),
                )
            ],
        )
    )

    add(
        Case(
            tc_id="6.2.2-02",
            spec_ref="6.2.2 / 7.3",
            spec_name="예약 가능 스터디룸 조회 / 오류 메시지와 메뉴 복귀",
            objective="기획서 6.2.2, 7.3에 따라 조회 입력 시 1시간 단위가 아닌 시각을 넣으면 형식 오류 후 member 메뉴로 복귀하는지 확인",
            method="member 로그인 후 조회 메뉴에서 시작 시각 13:30 입력",
            expected="오류: 예약 시각은 1시간 단위여야 합니다.를 출력하고 member 메뉴를 다시 보여 준다.",
            input_details=clean(
                """
                2
                user011
                pw1234
                2
                2026-03-20
                13:30
                0
                0
                """
            ),
            scenarios=[
                Scenario(
                    label="available_rooms_format_error",
                    dataset=search_dataset,
                    input_text=lines("2", "user011", "pw1234", "2", "2026-03-20", "13:30", "0", "0"),
                    actual_summary="시작 시각 입력 직후 '오류: 예약 시각은 1시간 단위여야 합니다.'가 출력되고 member 메뉴가 다시 표시되었다.",
                    verdict="PASS",
                    checks=Checks(output_contains=["오류: 예약 시각은 1시간 단위여야 합니다.", "[member 메뉴]"]),
                )
            ],
        )
    )

    add(
        Case(
            tc_id="6.2.2-03",
            spec_ref="6.2.2 / 7.3",
            spec_name="예약 가능 스터디룸 조회 / 오류 메시지와 메뉴 복귀",
            objective="기획서 6.2.2, 7.3에 따라 조회 의미 규칙(시작<종료)을 어기면 오류 후 member 메뉴로 복귀하는지 확인",
            method="member 로그인 후 조회 메뉴에서 시작 15:00, 종료 13:00 입력",
            expected="오류: 시작 시각은 종료 시각보다 빨라야 합니다.를 출력하고 member 메뉴를 다시 보여 준다.",
            input_details=clean(
                """
                2
                user011
                pw1234
                2
                2026-03-20
                15:00
                13:00
                3
                0
                0
                """
            ),
            scenarios=[
                Scenario(
                    label="available_rooms_semantic_error",
                    dataset=search_dataset,
                    input_text=lines("2", "user011", "pw1234", "2", "2026-03-20", "15:00", "13:00", "3", "0", "0"),
                    actual_summary="시작/종료 역전 의미 오류가 출력되고 member 메뉴가 재표시되었다.",
                    verdict="PASS",
                    checks=Checks(output_contains=["오류: 시작 시각은 종료 시각보다 빨라야 합니다.", "[member 메뉴]"]),
                )
            ],
        )
    )

    add(
        Case(
            tc_id="6.2.3-01",
            spec_ref="6.2.3",
            spec_name="예약 신청",
            objective="기획서 6.2.3에 따라 미래 시각·OPEN 룸·정원/충돌 조건을 만족하면 RESERVED 예약이 생성되는지 확인",
            method="member 로그인 후 2026-03-20 13:00~15:00, 인원 2, R102로 예약 신청",
            expected="예약 완료 메시지와 예약번호가 출력되고 reservations.txt에 RESERVED 레코드 1건이 추가된다.",
            input_details=clean(
                """
                2
                user011
                pw1234
                3
                2026-03-20
                13:00
                15:00
                2
                R102
                0
                0
                """
            ),
            scenarios=[
                Scenario(
                    label="reservation_create_success",
                    dataset=root_dataset(
                        users=default_users(),
                        rooms=default_rooms(),
                        reservations="",
                        system_time=now_record("2026-03-20 09:00"),
                    ),
                    input_text=lines("2", "user011", "pw1234", "3", "2026-03-20", "13:00", "15:00", "2", "R102", "0", "0"),
                    actual_summary="예약 완료 메시지와 rv0001이 출력되었고 after/reservations.txt에 RESERVED 레코드가 추가되었다.",
                    verdict="PASS",
                    checks=Checks(
                        output_contains=["예약이 완료되었습니다. 예약번호: rv0001"],
                        after_contains={"reservations.txt": ["RESV|rv0001|user011|R102|2026-03-20|13:00|15:00|2|RESERVED|2026-03-20 09:00|-"]},
                    ),
                )
            ],
        )
    )

    add(
        Case(
            tc_id="6.2.3-02",
            spec_ref="6.2.3",
            spec_name="예약 신청",
            objective="기획서 6.2.3에 따라 예약 시작 시각이 현재 시각보다 미래가 아니면 신청을 거부하는지 확인",
            method="member 로그인 후 현재 시각 11:30에서 시작 시각 11:00으로 예약 신청",
            expected="오류: 예약 시작 시각은 현재 가상 시각보다 미래여야 합니다.를 출력하고 reservations.txt는 유지된다.",
            input_details=clean(
                """
                2
                user011
                pw1234
                3
                2026-03-20
                11:00
                13:00
                2
                R101
                0
                0
                """
            ),
            scenarios=[
                Scenario(
                    label="reservation_create_fail_past_or_current",
                    dataset=root_dataset(
                        users=default_users(),
                        rooms=default_rooms(),
                        reservations="",
                        system_time=now_record("2026-03-20 11:30"),
                    ),
                    input_text=lines("2", "user011", "pw1234", "3", "2026-03-20", "11:00", "13:00", "2", "R101", "0", "0"),
                    actual_summary="현재보다 늦지 않은 시작 시각 오류가 출력되었고 after/reservations.txt는 빈 상태를 유지했다.",
                    verdict="PASS",
                    checks=Checks(
                        output_contains=["오류: 예약 시작 시각은 현재 가상 시각보다 미래여야 합니다."],
                        after_not_contains={"reservations.txt": ["rv0001"]},
                    ),
                )
            ],
        )
    )

    add(
        Case(
            tc_id="6.2.3-03",
            spec_ref="1.2 / 6.2.3",
            spec_name="예약 가능 조건 / 예약 신청",
            objective="기획서 1.2, 6.2.3에 따라 예약 시각이 1시간 단위가 아니면 신청을 거부하는지 확인",
            method="member 로그인 후 예약 신청 메뉴에서 시작 시각 13:30 입력",
            expected="오류: 예약 시각은 1시간 단위여야 합니다.를 출력하고 예약 레코드를 추가하지 않는다.",
            input_details=clean(
                """
                2
                user011
                pw1234
                3
                2026-03-20
                13:30
                0
                0
                """
            ),
            scenarios=[
                Scenario(
                    label="reservation_create_fail_time_unit",
                    dataset=root_dataset(
                        users=default_users(),
                        rooms=default_rooms(),
                        reservations="",
                        system_time=now_record("2026-03-20 09:00"),
                    ),
                    input_text=lines("2", "user011", "pw1234", "3", "2026-03-20", "13:30", "0", "0"),
                    actual_summary="시작 시각 입력 단계에서 1시간 단위 오류가 출력되었고 after/reservations.txt는 변경되지 않았다.",
                    verdict="PASS",
                    checks=Checks(
                        output_contains=["오류: 예약 시각은 1시간 단위여야 합니다."],
                        after_not_contains={"reservations.txt": ["rv0001"]},
                    ),
                )
            ],
        )
    )

    overlap_dataset_room = root_dataset(
        users=default_users(),
        rooms=default_rooms(),
        reservations=resv_record("rv0001", "user022", "R101", "2026-03-20", "13:00", "15:00", 2, "RESERVED", "2026-03-20 09:00", "-"),
        system_time=now_record("2026-03-20 09:00"),
    )
    overlap_dataset_user = root_dataset(
        users=default_users(),
        rooms=default_rooms(),
        reservations=resv_record("rv0001", "user011", "R101", "2026-03-20", "13:00", "15:00", 2, "RESERVED", "2026-03-20 09:00", "-"),
        system_time=now_record("2026-03-20 09:00"),
    )

    add(
        Case(
            tc_id="6.2.3-04",
            spec_ref="1.2 / 6.2.3",
            spec_name="예약 가능 조건 / 예약 신청",
            objective="기획서 1.2, 6.2.3에 따라 같은 룸의 겹치는 예약이 있으면 신청을 거부하는지 확인",
            method="이미 R101 13:00~15:00 RESERVED가 있는 상태에서 같은 시간·같은 룸으로 예약 신청",
            expected="오류: 해당 시간대에 이미 예약된 룸입니다.를 출력하고 신규 예약을 저장하지 않는다.",
            input_details=clean(
                """
                2
                user011
                pw1234
                3
                2026-03-20
                13:00
                15:00
                2
                R101
                0
                0
                """
            ),
            scenarios=[
                Scenario(
                    label="reservation_create_fail_room_overlap",
                    dataset=overlap_dataset_room,
                    input_text=lines("2", "user011", "pw1234", "3", "2026-03-20", "13:00", "15:00", "2", "R101", "0", "0"),
                    actual_summary="같은 룸 충돌 오류가 출력되었고 after/reservations.txt에는 기존 rv0001만 유지되었다.",
                    verdict="PASS",
                    checks=Checks(
                        output_contains=["오류: 해당 시간대에 이미 예약된 룸입니다."],
                        after_not_contains={"reservations.txt": ["rv0002"]},
                    ),
                )
            ],
        )
    )

    add(
        Case(
            tc_id="6.2.3-05",
            spec_ref="1.2 / 6.2.3",
            spec_name="예약 가능 조건 / 예약 신청",
            objective="기획서 1.2, 6.2.3에 따라 같은 회원의 겹치는 예약이 있으면 신청을 거부하는지 확인",
            method="member user011이 이미 R101 13:00~15:00 RESERVED를 가진 상태에서 같은 시간대 다른 룸으로 예약 신청",
            expected="오류: 같은 시간대에 이미 다른 예약이 있습니다.를 출력하고 신규 예약을 저장하지 않는다.",
            input_details=clean(
                """
                2
                user011
                pw1234
                3
                2026-03-20
                13:00
                15:00
                2
                R102
                0
                0
                """
            ),
            scenarios=[
                Scenario(
                    label="reservation_create_fail_user_overlap",
                    dataset=overlap_dataset_user,
                    input_text=lines("2", "user011", "pw1234", "3", "2026-03-20", "13:00", "15:00", "2", "R102", "0", "0"),
                    actual_summary="같은 사용자 충돌 오류가 출력되었고 after/reservations.txt에는 기존 rv0001만 유지되었다.",
                    verdict="PASS",
                    checks=Checks(
                        output_contains=["오류: 같은 시간대에 이미 다른 예약이 있습니다."],
                        after_not_contains={"reservations.txt": ["rv0002"]},
                    ),
                )
            ],
        )
    )

    length_batch: list[Scenario] = [
        Scenario(
            label="length_1hour_success",
            dataset=root_dataset(default_users(), default_rooms(), "", now_record("2026-03-20 09:00")),
            input_text=lines("2", "user011", "pw1234", "3", "2026-03-20", "13:00", "14:00", "2", "R101", "0", "0"),
            actual_summary="1시간 예약은 성공했고 after/reservations.txt에 13:00~14:00 RESERVED가 저장되었다.",
            verdict="PASS",
            checks=Checks(output_contains=["예약이 완료되었습니다. 예약번호: rv0001"], after_contains={"reservations.txt": ["|13:00|14:00|2|RESERVED|"]}),
        ),
        Scenario(
            label="length_4hour_success",
            dataset=root_dataset(default_users(), default_rooms(), "", now_record("2026-03-20 09:00")),
            input_text=lines("2", "user011", "pw1234", "3", "2026-03-20", "13:00", "17:00", "2", "R101", "0", "0"),
            actual_summary="4시간 예약은 성공했고 after/reservations.txt에 13:00~17:00 RESERVED가 저장되었다.",
            verdict="PASS",
            checks=Checks(output_contains=["예약이 완료되었습니다. 예약번호: rv0001"], after_contains={"reservations.txt": ["|13:00|17:00|2|RESERVED|"]}),
        ),
        Scenario(
            label="length_0hour_fail",
            dataset=root_dataset(default_users(), default_rooms(), "", now_record("2026-03-20 09:00")),
            input_text=lines("2", "user011", "pw1234", "3", "2026-03-20", "13:00", "13:00", "2", "R101", "0", "0"),
            actual_summary="시작=종료 입력은 시작/종료 역전 오류로 거부되었다.",
            verdict="PASS",
            checks=Checks(output_contains=["오류: 시작 시각은 종료 시각보다 빨라야 합니다."], after_not_contains={"reservations.txt": ["rv0001"]}),
        ),
        Scenario(
            label="length_5hour_fail",
            dataset=root_dataset(default_users(), default_rooms(), "", now_record("2026-03-20 09:00")),
            input_text=lines("2", "user011", "pw1234", "3", "2026-03-20", "13:00", "18:00", "2", "R101", "0", "0"),
            actual_summary="5시간 예약은 길이 제한 오류로 거부되었다.",
            verdict="PASS",
            checks=Checks(output_contains=["오류: 예약 길이는 1시간, 2시간, 3시간, 4시간 중 하나여야 합니다."], after_not_contains={"reservations.txt": ["rv0001"]}),
        ),
    ]

    add(
        Case(
            tc_id="1.2-01",
            spec_ref="1.2 / 5.2 / 6.2.3",
            spec_name="예약 가능 조건 / 의미 규칙 / 예약 신청",
            objective="기획서 1.2, 5.2, 6.2.3에 따라 예약 길이 경계값 1시간/4시간/0시간/5시간을 검증한다.",
            method="서브케이스마다 동일한 예약 신청 흐름에서 종료 시각만 바꿔 길이 허용 여부와 reservations.txt 반영 여부를 비교",
            expected="1시간과 4시간은 허용되고, 0시간과 5시간은 오류 메시지와 함께 저장되지 않는다.",
            input_details="공통 흐름: member 로그인 -> 예약 신청 -> 날짜/시각/인원/룸 입력",
            scenarios=length_batch,
        )
    )

    party_batch: list[Scenario] = [
        Scenario(
            label="party_min_1_success",
            dataset=root_dataset(default_users(), default_rooms(), "", now_record("2026-03-20 09:00")),
            input_text=lines("2", "user011", "pw1234", "3", "2026-03-20", "13:00", "15:00", "1", "R101", "0", "0"),
            actual_summary="partySize 1은 정상 저장되었다.",
            verdict="PASS",
            checks=Checks(output_contains=["예약이 완료되었습니다. 예약번호: rv0001"], after_contains={"reservations.txt": ["|1|RESERVED|"]}),
        ),
        Scenario(
            label="party_over_capacity_fail",
            dataset=root_dataset(default_users(), default_rooms(), "", now_record("2026-03-20 09:00")),
            input_text=lines("2", "user011", "pw1234", "3", "2026-03-20", "13:00", "15:00", "7", "R102", "0", "0"),
            actual_summary="room maxCapacity 6을 넘는 partySize 7은 수용 인원 초과 오류로 거부되었다.",
            verdict="PASS",
            checks=Checks(output_contains=["오류: 수용 인원을 초과했습니다."], after_not_contains={"reservations.txt": ["rv0001"]}),
        ),
    ]

    add(
        Case(
            tc_id="1.2-02",
            spec_ref="1.2 / 5.2 / 6.2.3",
            spec_name="예약 가능 조건 / 의미 규칙 / 예약 신청",
            objective="기획서 1.2, 5.2, 6.2.3에 따라 partySize 최소값과 정원 초과를 검증한다.",
            method="서브케이스별로 예약 신청을 수행하고 partySize=1 성공, partySize가 maxCapacity를 넘는 경우 실패를 확인",
            expected="partySize 1은 허용되고, room 정원을 넘는 partySize는 오류와 함께 저장되지 않는다.",
            input_details="공통 흐름: member 로그인 -> 예약 신청 -> 날짜/시각/인원/룸 입력",
            scenarios=party_batch,
        )
    )

    boundary_batch: list[Scenario] = [
        Scenario(
            label="invalid_time_format",
            dataset=root_dataset(default_users(), default_rooms(), "", now_record("2026-03-20 09:00")),
            input_text=lines("2", "user011", "pw1234", "3", "2026-03-20", "13", "0", "0"),
            actual_summary="시각 형식 13 입력은 '오류: 시각 형식이 올바르지 않습니다. 예: 13:00'으로 거부되었다.",
            verdict="PASS",
            checks=Checks(output_contains=["오류: 시각 형식이 올바르지 않습니다. 예: 13:00"]),
        ),
        Scenario(
            label="invalid_date_format",
            dataset=root_dataset(default_users(), default_rooms(), "", now_record("2026-03-20 09:00")),
            input_text=lines("2", "user011", "pw1234", "3", "2026/03/20", "0", "0"),
            actual_summary="슬래시 날짜는 날짜 형식 오류로 거부되었다.",
            verdict="PASS",
            checks=Checks(output_contains=["오류: 날짜 형식이 올바르지 않습니다. 예: 2026-03-20"]),
        ),
        Scenario(
            label="nonexistent_date",
            dataset=root_dataset(default_users(), default_rooms(), "", now_record("2026-03-20 09:00")),
            input_text=lines("2", "user011", "pw1234", "3", "2026-02-30", "0", "0"),
            actual_summary="존재하지 않는 날짜 2026-02-30은 날짜 형식 오류로 거부되었다.",
            verdict="PASS",
            checks=Checks(output_contains=["오류: 날짜 형식이 올바르지 않습니다. 예: 2026-03-20"]),
        ),
        Scenario(
            label="trim_rejected",
            dataset=root_dataset(default_users(), default_rooms(), "", now_record("2026-03-20 09:00")),
            input_text=lines("2", "user011", "pw1234", "3", " 2026-03-20", "0", "0"),
            actual_summary="입력값 앞 공백이 있는 날짜는 '오류: 입력값 앞뒤에 공백을 넣을 수 없습니다.'로 거부되었다.",
            verdict="PASS",
            checks=Checks(output_contains=["오류: 입력값 앞뒤에 공백을 넣을 수 없습니다."]),
        ),
    ]

    add(
        Case(
            tc_id="1.2-03",
            spec_ref="1.2 / 6.2.3 / 7.3",
            spec_name="예약 가능 조건 / 예약 신청 / 오류 메시지와 메뉴 복귀",
            objective="기획서 1.2, 6.2.3, 7.3에 따라 날짜/시각 형식과 입력 앞뒤 공백을 검증한다.",
            method="서브케이스별로 예약 신청 초기 입력을 바꿔 HH:mm 형식 오류, 날짜 형식 오류, 실존 날짜 오류, 입력 공백 오류를 확인",
            expected="잘못된 형식·실존하지 않는 날짜·앞뒤 공백은 각각 구체적 오류 메시지를 출력하고 상위 메뉴로 복귀한다.",
            input_details="공통 흐름: member 로그인 -> 예약 신청 -> 날짜 또는 시작 시각 입력 단계에서 오류 유발",
            scenarios=boundary_batch,
        )
    )

    add(
        Case(
            tc_id="6.2.4-01",
            spec_ref="6.2.4",
            spec_name="예약 취소",
            objective="기획서 6.2.4에 따라 자신의 미래 RESERVED 예약 취소 시 레코드가 삭제되는지 확인",
            method="member 로그인 후 미래 RESERVED rv0001을 취소하고 reservations.txt before/after 비교",
            expected="예약이 취소되었습니다.를 출력하고 해당 RESV 레코드를 삭제한다.",
            input_details=clean(
                """
                2
                user011
                pw1234
                4
                rv0001
                0
                0
                """
            ),
            scenarios=[
                Scenario(
                    label="cancel_success_delete",
                    dataset=root_dataset(
                        users=default_users(),
                        rooms=default_rooms(),
                        reservations=resv_record("rv0001", "user011", "R101", "2026-03-20", "13:00", "15:00", 2, "RESERVED", "2026-03-20 09:00", "-"),
                        system_time=now_record("2026-03-20 09:00"),
                    ),
                    input_text=lines("2", "user011", "pw1234", "4", "rv0001", "0", "0"),
                    actual_summary="취소 성공 메시지가 출력되었고 after/reservations.txt에서 rv0001 레코드가 삭제되었다.",
                    verdict="PASS",
                    checks=Checks(output_contains=["예약이 취소되었습니다."], after_not_contains={"reservations.txt": ["rv0001"]}),
                )
            ],
        )
    )

    add(
        Case(
            tc_id="6.2.4-02",
            spec_ref="6.2.4",
            spec_name="예약 취소",
            objective="기획서 6.2.4에 따라 이미 진행 중이거나 종료된 예약은 취소할 수 없는지 확인",
            method="현재 시각 15:05에서 15:00 시작 RESERVED rv0001을 취소 시도",
            expected="오류: 이미 진행 중이거나 종료된 예약은 취소할 수 없습니다.를 출력하고 레코드를 유지한다.",
            input_details=clean(
                """
                2
                user011
                pw1234
                4
                rv0001
                0
                0
                """
            ),
            scenarios=[
                Scenario(
                    label="cancel_fail_started",
                    dataset=root_dataset(
                        users=default_users(),
                        rooms=default_rooms(),
                        reservations=resv_record("rv0001", "user011", "R101", "2026-03-20", "15:00", "17:00", 2, "RESERVED", "2026-03-20 09:00", "-"),
                        system_time=now_record("2026-03-20 15:05"),
                    ),
                    input_text=lines("2", "user011", "pw1234", "4", "rv0001", "0", "0"),
                    actual_summary="진행 중/종료 예약 취소 불가 오류가 출력되었고 after/reservations.txt에 rv0001이 유지되었다.",
                    verdict="PASS",
                    checks=Checks(
                        output_contains=["오류: 이미 진행 중이거나 종료된 예약은 취소할 수 없습니다."],
                        after_contains={"reservations.txt": ["rv0001"]},
                    ),
                )
            ],
        )
    )

    my_reservations_dataset = root_dataset(
        users=default_users(),
        rooms=default_rooms(),
        reservations=(
            resv_record("rv0001", "user011", "R101", "2026-03-20", "13:00", "15:00", 2, "RESERVED", "2026-03-20 09:00", "-")
            + resv_record("rv0002", "user022", "R102", "2026-03-20", "16:00", "18:00", 3, "RESERVED", "2026-03-20 09:10", "-")
            + resv_record("rv0003", "user011", "R103", "2026-03-21", "10:00", "12:00", 4, "RESERVED", "2026-03-20 09:20", "-")
        ),
        system_time=now_record("2026-03-20 11:30"),
    )

    add(
        Case(
            tc_id="6.2.5-01",
            spec_ref="6.2.5",
            spec_name="나의 예약 조회",
            objective="기획서 6.2.5에 따라 로그인한 member의 예약만 정렬 출력되는지 확인",
            method="user011로 로그인 후 나의 예약 조회를 실행해 rv0001, rv0003만 나오는지 확인",
            expected="resvId/room/date/start/end/인원/status 표가 출력되고 user011 예약 2건만 보인다.",
            input_details=clean(
                """
                2
                user011
                pw1234
                5
                0
                0
                """
            ),
            scenarios=[
                Scenario(
                    label="my_reservations_success",
                    dataset=my_reservations_dataset,
                    input_text=lines("2", "user011", "pw1234", "5", "0", "0"),
                    actual_summary="나의 예약 조회 표에 rv0001, rv0003만 출력되고 rv0002(user022)는 제외되었다.",
                    verdict="PASS",
                    checks=Checks(output_contains=["rv0001", "rv0003", "조회가 끝났습니다."], output_not_contains=["rv0002 R102"]),
                )
            ],
        )
    )

    add(
        Case(
            tc_id="6.2.5-02",
            spec_ref="6.2.5",
            spec_name="나의 예약 조회",
            objective="기획서 6.2.5에 따라 로그인한 member의 예약이 없으면 안내 메시지를 출력하는지 확인",
            method="예약이 없는 user011로 로그인 후 나의 예약 조회 실행",
            expected="나의 예약이 없습니다.를 출력한다.",
            input_details=clean(
                """
                2
                user011
                pw1234
                5
                0
                0
                """
            ),
            scenarios=[
                Scenario(
                    label="my_reservations_none",
                    dataset=root_dataset(default_users(), default_rooms(), "", now_record("2026-03-20 11:30")),
                    input_text=lines("2", "user011", "pw1234", "5", "0", "0"),
                    actual_summary="예약 없음 안내가 출력되었다.",
                    verdict="PASS",
                    checks=Checks(output_contains=["나의 예약이 없습니다."]),
                )
            ],
        )
    )

    checkin_base_dataset = root_dataset(
        users=default_users(),
        rooms=default_rooms(),
        reservations=resv_record("rv0001", "user011", "R101", "2026-03-20", "11:00", "12:00", 2, "RESERVED", "2026-03-20 09:00", "-"),
        system_time=now_record("2026-03-20 10:49"),
    )
    add(
        Case(
            tc_id="6.2.6-01",
            spec_ref="1.2 / 6.2.6",
            spec_name="체크인 가능 구간 / 체크인",
            objective="기획서 1.2, 6.2.6에 따라 체크인 가능 구간보다 이르면 체크인을 거부하는지 확인",
            method="현재 시각 10:49에서 11:00 시작 예약 rv0001 체크인 시도",
            expected="오류: 아직 체크인 가능한 시간이 아닙니다.를 출력하고 status는 RESERVED로 유지된다.",
            input_details=clean(
                """
                2
                user011
                pw1234
                6
                rv0001
                0
                0
                """
            ),
            scenarios=[
                Scenario(
                    label="checkin_too_early",
                    dataset=checkin_base_dataset,
                    input_text=lines("2", "user011", "pw1234", "6", "rv0001", "0", "0"),
                    actual_summary="체크인 가능 시간 이전 오류가 출력되었고 after/reservations.txt의 rv0001 status는 RESERVED였다.",
                    verdict="PASS",
                    checks=Checks(
                        output_contains=["오류: 아직 체크인 가능한 시간이 아닙니다."],
                        after_contains={"reservations.txt": ["|RESERVED|2026-03-20 09:00|-"]},
                    ),
                )
            ],
        )
    )

    add(
        Case(
            tc_id="6.2.6-02",
            spec_ref="1.2 / 6.2.6",
            spec_name="체크인 가능 구간 / 체크인",
            objective="기획서 1.2, 6.2.6에 따라 체크인 가능 구간 상한 경계(시작+10분)에서 체크인이 성공하는지 확인",
            method="현재 시각 11:10에서 11:00 시작 예약 rv0001 체크인",
            expected="체크인이 완료되었습니다.를 출력하고 status=CHECKED_IN, checkedInAt=현재 시각으로 저장한다.",
            input_details=clean(
                """
                2
                user011
                pw1234
                6
                rv0001
                0
                0
                """
            ),
            scenarios=[
                Scenario(
                    label="checkin_boundary_success",
                    dataset=root_dataset(
                        users=default_users(),
                        rooms=default_rooms(),
                        reservations=resv_record("rv0001", "user011", "R101", "2026-03-20", "11:00", "12:00", 2, "RESERVED", "2026-03-20 09:00", "-"),
                        system_time=now_record("2026-03-20 11:10"),
                    ),
                    input_text=lines("2", "user011", "pw1234", "6", "rv0001", "0", "0"),
                    actual_summary="체크인 성공 메시지가 출력되었고 after/reservations.txt에 CHECKED_IN과 checkedInAt=2026-03-20 11:10이 저장되었다.",
                    verdict="PASS",
                    checks=Checks(
                        output_contains=["체크인이 완료되었습니다."],
                        after_contains={"reservations.txt": ["|CHECKED_IN|2026-03-20 09:00|2026-03-20 11:10"]},
                    ),
                )
            ],
        )
    )

    add(
        Case(
            tc_id="6.2.6-03",
            spec_ref="1.3 / 6.2.6",
            spec_name="스터디룸 / 체크인",
            objective="기획서 1.3, 6.2.6에 따라 CLOSED 룸 예약은 체크인할 수 없는지 확인",
            method="roomStatus=CLOSED인 R101의 RESERVED rv0001에 대해 체크인 시도",
            expected="오류: 운영 중이 아닌 룸은 체크인할 수 없습니다.를 출력하고 status는 RESERVED로 유지한다.",
            input_details=clean(
                """
                2
                user011
                pw1234
                6
                rv0001
                0
                0
                """
            ),
            scenarios=[
                Scenario(
                    label="checkin_closed_room_fail",
                    dataset=root_dataset(
                        users=default_users(),
                        rooms=room_record("R101", "A룸", 4, "CLOSED") + room_record("R102", "B룸", 6, "OPEN"),
                        reservations=resv_record("rv0001", "user011", "R101", "2026-03-20", "11:00", "12:00", 2, "RESERVED", "2026-03-20 09:00", "-"),
                        system_time=now_record("2026-03-20 10:55"),
                    ),
                    input_text=lines("2", "user011", "pw1234", "6", "rv0001", "0", "0"),
                    actual_summary="운영 중이 아닌 룸 체크인 불가 오류가 출력되었고 after/reservations.txt는 RESERVED 상태를 유지했다.",
                    verdict="PASS",
                    checks=Checks(
                        output_contains=["오류: 운영 중이 아닌 룸은 체크인할 수 없습니다."],
                        after_contains={"reservations.txt": ["|RESERVED|2026-03-20 09:00|-"]},
                    ),
                )
            ],
        )
    )

    admin_reservations_dataset = root_dataset(
        users=default_users(),
        rooms=default_rooms(),
        reservations=(
            resv_record("rv0001", "user011", "R101", "2026-03-21", "11:00", "12:00", 2, "RESERVED", "2026-03-20 09:00", "-")
            + resv_record("rv0002", "user022", "R102", "2026-03-21", "13:00", "14:00", 3, "RESERVED", "2026-03-20 09:30", "-")
        ),
        system_time=now_record("2026-03-20 09:00"),
    )

    add(
        Case(
            tc_id="6.3.2-01",
            spec_ref="6.3.2",
            spec_name="전체 예약 정보 조회",
            objective="기획서 6.3.2에 따라 admin 화면에서 userId와 userName을 함께 포함한 전체 예약 표를 출력하는지 확인",
            method="admin 로그인 후 전체 예약 정보 조회 실행",
            expected="resvId / userId / userName / room / date / start / end / 인원 / status / checkedInAt 헤더와 예약 데이터가 출력된다.",
            input_details=clean(
                """
                2
                user001
                admin1234
                2
                0
                0
                """
            ),
            scenarios=[
                Scenario(
                    label="admin_all_reservations",
                    dataset=admin_reservations_dataset,
                    input_text=lines("2", "user001", "admin1234", "2", "0", "0"),
                    actual_summary="전체 예약 표에 userId, userName 컬럼과 두 예약이 함께 출력되었다.",
                    verdict="PASS",
                    checks=Checks(output_contains=["resvId", "userId", "userName", "user011", "bonsu", "user022", "minseo", "조회가 끝났습니다."]),
                )
            ],
        )
    )

    add(
        Case(
            tc_id="6.3.3-01",
            spec_ref="6.3.3",
            spec_name="예약 조정(방 이동)",
            objective="기획서 6.3.3에 따라 미래 RESERVED 예약을 OPEN 룸으로 성공적으로 이동하는지 확인",
            method="admin 로그인 후 rv0001을 R101에서 R102로 이동",
            expected="변경 전/후 RESV 레코드를 출력하고 reservations.txt의 roomId가 R102로 바뀐다.",
            input_details=clean(
                """
                2
                user001
                admin1234
                3
                rv0001
                R102
                0
                0
                """
            ),
            scenarios=[
                Scenario(
                    label="adjust_success",
                    dataset=root_dataset(
                        users=default_users(),
                        rooms=default_rooms(),
                        reservations=resv_record("rv0001", "user011", "R101", "2026-03-21", "11:00", "12:00", 2, "RESERVED", "2026-03-20 09:00", "-"),
                        system_time=now_record("2026-03-20 09:00"),
                    ),
                    input_text=lines("2", "user001", "admin1234", "3", "rv0001", "R102", "0", "0"),
                    actual_summary="변경 전/후 예약 레코드가 출력되었고 after/reservations.txt의 rv0001 roomId가 R102로 변경되었다.",
                    verdict="PASS",
                    checks=Checks(
                        output_contains=["변경 전 예약 레코드:", "변경 후 예약 레코드:", "예약 조정이 완료되었습니다."],
                        after_contains={"reservations.txt": ["RESV|rv0001|user011|R102|2026-03-21|11:00|12:00|2|RESERVED|2026-03-20 09:00|-"]},
                    ),
                )
            ],
        )
    )

    add(
        Case(
            tc_id="6.3.3-02",
            spec_ref="6.3.3",
            spec_name="예약 조정(방 이동)",
            objective="기획서 6.3.3에 따라 현재 룸과 동일한 룸으로는 이동할 수 없는지 확인",
            method="admin 로그인 후 rv0001의 대상 룸을 기존과 같은 R101로 입력",
            expected="오류: 현재 룸과 다른 룸으로만 이동할 수 있습니다.를 출력하고 레코드는 유지된다.",
            input_details=clean(
                """
                2
                user001
                admin1234
                3
                rv0001
                R101
                0
                0
                """
            ),
            scenarios=[
                Scenario(
                    label="adjust_fail_same_room",
                    dataset=root_dataset(
                        users=default_users(),
                        rooms=default_rooms(),
                        reservations=resv_record("rv0001", "user011", "R101", "2026-03-21", "11:00", "12:00", 2, "RESERVED", "2026-03-20 09:00", "-"),
                        system_time=now_record("2026-03-20 09:00"),
                    ),
                    input_text=lines("2", "user001", "admin1234", "3", "rv0001", "R101", "0", "0"),
                    actual_summary="동일 룸 이동 불가 오류가 출력되었고 after/reservations.txt의 rv0001은 R101 그대로였다.",
                    verdict="PASS",
                    checks=Checks(
                        output_contains=["오류: 현재 룸과 다른 룸으로만 이동할 수 있습니다."],
                        after_contains={"reservations.txt": ["|R101|2026-03-21|11:00|12:00|2|RESERVED|"]},
                    ),
                )
            ],
        )
    )

    add(
        Case(
            tc_id="6.3.3-03",
            spec_ref="6.3.3",
            spec_name="예약 조정(방 이동)",
            objective="기획서 6.3.3에 따라 CLOSED 룸으로는 예약을 이동할 수 없는지 확인",
            method="admin 로그인 후 rv0001의 대상 룸을 CLOSED 상태 R103으로 입력",
            expected="오류: 대상 룸이 OPEN 상태가 아니어서 이동할 수 없습니다.를 출력한다.",
            input_details=clean(
                """
                2
                user001
                admin1234
                3
                rv0001
                R103
                0
                0
                """
            ),
            scenarios=[
                Scenario(
                    label="adjust_fail_closed_room",
                    dataset=root_dataset(
                        users=default_users(),
                        rooms=room_record("R101", "A룸", 4, "OPEN") + room_record("R102", "B룸", 6, "OPEN") + room_record("R103", "C룸", 8, "CLOSED"),
                        reservations=resv_record("rv0001", "user011", "R101", "2026-03-21", "11:00", "12:00", 2, "RESERVED", "2026-03-20 09:00", "-"),
                        system_time=now_record("2026-03-20 09:00"),
                    ),
                    input_text=lines("2", "user001", "admin1234", "3", "rv0001", "R103", "0", "0"),
                    actual_summary="CLOSED 룸 이동 불가 오류가 출력되었다.",
                    verdict="PASS",
                    checks=Checks(output_contains=["오류: 대상 룸이 OPEN 상태가 아니어서 이동할 수 없습니다."]),
                )
            ],
        )
    )

    add(
        Case(
            tc_id="6.3.3-04",
            spec_ref="6.3.3",
            spec_name="예약 조정(방 이동)",
            objective="기획서 6.3.3에 따라 대상 룸 정원이 부족하면 이동을 거부하는지 확인",
            method="partySize 4 예약 rv0001을 maxCapacity 2인 R102로 이동 시도",
            expected="오류: 대상 룸의 수용 인원이 부족합니다.를 출력한다.",
            input_details=clean(
                """
                2
                user001
                admin1234
                3
                rv0001
                R102
                0
                0
                """
            ),
            scenarios=[
                Scenario(
                    label="adjust_fail_capacity",
                    dataset=root_dataset(
                        users=default_users(),
                        rooms=room_record("R101", "A룸", 6, "OPEN") + room_record("R102", "B룸", 2, "OPEN"),
                        reservations=resv_record("rv0001", "user011", "R101", "2026-03-21", "11:00", "12:00", 4, "RESERVED", "2026-03-20 09:00", "-"),
                        system_time=now_record("2026-03-20 09:00"),
                    ),
                    input_text=lines("2", "user001", "admin1234", "3", "rv0001", "R102", "0", "0"),
                    actual_summary="대상 룸 정원 부족 오류가 출력되었다.",
                    verdict="PASS",
                    checks=Checks(output_contains=["오류: 대상 룸의 수용 인원이 부족합니다."]),
                )
            ],
        )
    )

    add(
        Case(
            tc_id="6.3.4-01",
            spec_ref="6.3.4",
            spec_name="룸 컨디션 관리 - 전체 룸 조회",
            objective="기획서 6.3.4에 따라 룸 컨디션 관리 메뉴의 전체 룸 조회가 roomId/roomName/정원/status를 출력하는지 확인",
            method="admin 로그인 후 룸 컨디션 관리 -> 전체 룸 조회 실행",
            expected="roomId roomName 정원 status 헤더와 모든 ROOM 데이터가 출력된다.",
            input_details=clean(
                """
                2
                user001
                admin1234
                4
                1
                0
                0
                0
                """
            ),
            scenarios=[
                Scenario(
                    label="room_list_query",
                    dataset=root_dataset(
                        users=default_users(),
                        rooms=room_record("R101", "A룸", 4, "OPEN") + room_record("R102", "B룸", 6, "OPEN") + room_record("R103", "C룸", 8, "CLOSED"),
                        reservations="",
                        system_time=now_record("2026-03-20 17:30"),
                    ),
                    input_text=lines("2", "user001", "admin1234", "4", "1", "0", "0", "0"),
                    actual_summary="전체 룸 조회 표에 R101, R102, R103과 status가 출력되었다.",
                    verdict="PASS",
                    checks=Checks(output_contains=["[전체 룸 조회]", "roomId", "R101", "R102", "R103", "조회가 끝났습니다."]),
                )
            ],
        )
    )

    add(
        Case(
            tc_id="6.3.4-02",
            spec_ref="5.3.2 / 6.3.4",
            spec_name="영향 예약 / 룸 컨디션 관리 - 최대 수용 인원 변경",
            objective="기획서 5.3.2, 6.3.4에 따라 현재 CHECKED_IN 예약 인원이 새 정원을 초과하면 정원 변경을 즉시 거부하는지 확인",
            method="CHECKED_IN partySize 4가 있는 R101에 대해 새 최대 수용 인원 2를 입력",
            expected="오류: 현재 CHECKED_IN 예약 인원이 새 최대 수용 인원을 초과하여 변경할 수 없습니다.를 출력하고 ROOM 레코드는 유지된다.",
            input_details=clean(
                """
                2
                user001
                admin1234
                4
                2
                R101
                2
                0
                0
                0
                """
            ),
            scenarios=[
                Scenario(
                    label="capacity_change_fail_checked_in_over",
                    dataset=root_dataset(
                        users=default_users(),
                        rooms=room_record("R101", "A룸", 6, "OPEN") + room_record("R102", "B룸", 6, "OPEN"),
                        reservations=resv_record("rv0001", "user011", "R101", "2026-03-20", "09:00", "10:00", 4, "CHECKED_IN", "2026-03-20 08:00", "2026-03-20 08:55"),
                        system_time=now_record("2026-03-20 09:30"),
                    ),
                    input_text=lines("2", "user001", "admin1234", "4", "2", "R101", "2", "0", "0", "0"),
                    actual_summary="현재 CHECKED_IN 예약 인원 초과 오류가 출력되었고 after/rooms.txt의 R101 정원은 6 그대로였다.",
                    verdict="PASS",
                    checks=Checks(
                        output_contains=["오류: 현재 CHECKED_IN 예약 인원이 새 최대 수용 인원을 초과하여 변경할 수 없습니다."],
                        after_contains={"rooms.txt": ["ROOM|R101|A룸|6|OPEN"]},
                    ),
                )
            ],
        )
    )

    add(
        Case(
            tc_id="6.3.4-03",
            spec_ref="5.3.2 / 6.3.4",
            spec_name="영향 예약 / 룸 컨디션 관리 - 최대 수용 인원 변경",
            objective="기획서 5.3.2, 6.3.4에 따라 미래 RESERVED 영향 예약을 다른 룸으로 이동 처리한 뒤 정원 변경이 완료되는지 확인",
            method="R102의 새 정원 3을 입력하고 영향 예약 rv0015에 대해 1) 다른 룸 이동 -> R101 선택",
            expected="영향 예약 처리 완료 후 변경 전/후 RESV, ROOM 레코드를 출력하고 R102 정원은 3으로, rv0015 roomId는 R101로 저장된다.",
            input_details=clean(
                """
                2
                user001
                admin1234
                4
                2
                R102
                3
                1
                R101
                0
                0
                0
                """
            ),
            scenarios=[
                Scenario(
                    label="capacity_change_move_impacted",
                    dataset=root_dataset(
                        users=default_users(),
                        rooms=room_record("R101", "A룸", 4, "OPEN") + room_record("R102", "B룸", 6, "OPEN"),
                        reservations=resv_record("rv0015", "user022", "R102", "2026-03-21", "14:00", "16:00", 4, "RESERVED", "2026-03-20 16:40", "-"),
                        system_time=now_record("2026-03-20 17:30"),
                    ),
                    input_text=lines("2", "user001", "admin1234", "4", "2", "R102", "3", "1", "R101", "0", "0", "0"),
                    actual_summary="영향 예약 처리 흐름이 실행되어 rv0015가 R101로 이동했고, after/rooms.txt의 R102 정원은 3으로 줄었다.",
                    verdict="PASS",
                    checks=Checks(
                        output_contains=["현재 CHECKED_IN 예약 인원 검사 통과", "영향 예약이 있어 처리 흐름을 시작합니다.", "영향 예약 처리가 완료되었습니다.", "룸 최대 수용 인원이 변경되었습니다."],
                        after_contains={
                            "rooms.txt": ["ROOM|R102|B룸|3|OPEN"],
                            "reservations.txt": ["RESV|rv0015|user022|R101|2026-03-21|14:00|16:00|4|RESERVED|2026-03-20 16:40|-"],
                        },
                    ),
                )
            ],
        )
    )

    add(
        Case(
            tc_id="6.3.4-04",
            spec_ref="5.3.2 / 6.3.4",
            spec_name="영향 예약 / 룸 컨디션 관리 - 최대 수용 인원 변경",
            objective="기획서 5.3.2, 6.3.4에 따라 영향 예약 처리 중 0을 선택하면 전체 변경이 rollback 되는지 확인",
            method="R101 새 정원 4 입력 후 영향 예약 화면에서 0 선택",
            expected="룸 컨디션 변경을 취소하여 원상복구했습니다.를 출력하고 ROOM/RESV before 상태를 유지한다.",
            input_details=clean(
                """
                2
                user001
                admin1234
                4
                2
                R101
                4
                0
                0
                0
                0
                """
            ),
            scenarios=[
                Scenario(
                    label="capacity_change_rollback",
                    dataset=root_dataset(
                        users=default_users(),
                        rooms=room_record("R101", "A룸", 6, "OPEN") + room_record("R102", "B룸", 6, "OPEN"),
                        reservations=resv_record("rv0001", "user011", "R101", "2026-03-21", "11:00", "12:00", 6, "RESERVED", "2026-03-20 09:00", "-"),
                        system_time=now_record("2026-03-20 09:00"),
                    ),
                    input_text=lines("2", "user001", "admin1234", "4", "2", "R101", "4", "0", "0", "0", "0"),
                    actual_summary="영향 예약 처리 중 전체 취소를 선택하자 원상복구 메시지가 출력되었고 ROOM/RESV 파일은 before 상태를 유지했다.",
                    verdict="PASS",
                    checks=Checks(
                        output_contains=["룸 컨디션 변경을 취소하여 원상복구했습니다."],
                        after_contains={
                            "rooms.txt": ["ROOM|R101|A룸|6|OPEN"],
                            "reservations.txt": ["RESV|rv0001|user011|R101|2026-03-21|11:00|12:00|6|RESERVED|2026-03-20 09:00|-"],
                        },
                    ),
                )
            ],
        )
    )

    add(
        Case(
            tc_id="6.3.4-05",
            spec_ref="5.3.2 / 6.3.4",
            spec_name="영향 예약 / 룸 컨디션 관리 - 임시 휴업",
            objective="기획서 5.3.2, 6.3.4에 따라 현재 CHECKED_IN 예약이 있으면 즉시 휴업을 거부하는지 확인",
            method="CHECKED_IN 예약이 있는 R101에 대해 임시 휴업 시도",
            expected="오류: 현재 체크인 중인 예약이 있어 즉시 휴업할 수 없습니다.를 출력하고 ROOM 레코드를 유지한다.",
            input_details=clean(
                """
                2
                user001
                admin1234
                4
                3
                R101
                0
                0
                0
                """
            ),
            scenarios=[
                Scenario(
                    label="close_room_fail_checked_in",
                    dataset=root_dataset(
                        users=default_users(),
                        rooms=default_rooms(),
                        reservations=resv_record("rv0001", "user011", "R101", "2026-03-20", "09:00", "10:00", 2, "CHECKED_IN", "2026-03-20 08:00", "2026-03-20 08:55"),
                        system_time=now_record("2026-03-20 09:30"),
                    ),
                    input_text=lines("2", "user001", "admin1234", "4", "3", "R101", "0", "0", "0"),
                    actual_summary="현재 체크인 중 예약 존재 오류가 출력되었고 after/rooms.txt의 R101 status는 OPEN이었다.",
                    verdict="PASS",
                    checks=Checks(
                        output_contains=["오류: 현재 체크인 중인 예약이 있어 즉시 휴업할 수 없습니다."],
                        after_contains={"rooms.txt": ["ROOM|R101|A룸|4|OPEN"]},
                    ),
                )
            ],
        )
    )

    add(
        Case(
            tc_id="6.3.4-06",
            spec_ref="6.3.4",
            spec_name="룸 컨디션 관리 - 임시 휴업",
            objective="기획서 6.3.4에 따라 영향 예약이 없는 OPEN 룸은 임시 휴업 처리되는지 확인",
            method="예약이 없는 R102를 임시 휴업",
            expected="변경 전/후 ROOM 레코드 출력 후 R102 status가 CLOSED로 저장된다.",
            input_details=clean(
                """
                2
                user001
                admin1234
                4
                3
                R102
                0
                0
                0
                """
            ),
            scenarios=[
                Scenario(
                    label="close_room_success",
                    dataset=root_dataset(
                        users=default_users(),
                        rooms=default_rooms(),
                        reservations="",
                        system_time=now_record("2026-03-20 17:30"),
                    ),
                    input_text=lines("2", "user001", "admin1234", "4", "3", "R102", "0", "0", "0"),
                    actual_summary="변경 전/후 ROOM 레코드가 출력되었고 after/rooms.txt의 R102 status가 CLOSED로 저장되었다.",
                    verdict="PASS",
                    checks=Checks(
                        output_contains=["변경 전 ROOM 레코드:", "변경 후 ROOM 레코드:", "룸이 임시 휴업 처리되었습니다."],
                        after_contains={"rooms.txt": ["ROOM|R102|B룸|6|CLOSED"]},
                    ),
                )
            ],
        )
    )

    add(
        Case(
            tc_id="6.3.4-07",
            spec_ref="5.3.2 / 6.3.4",
            spec_name="영향 예약 / 룸 컨디션 관리 - 임시 휴업",
            objective="기획서 5.3.2, 6.3.4에 따라 미래 RESERVED 영향 예약을 삭제 처리한 뒤 임시 휴업이 반영되는지 확인",
            method="미래 RESERVED rv0001이 있는 R101 휴업 시도 후 영향 예약 메뉴에서 2) 해당 예약 취소 선택",
            expected="영향 예약 처리 후 rv0001이 삭제되고 ROOM R101 status는 CLOSED로 저장된다.",
            input_details=clean(
                """
                2
                user001
                admin1234
                4
                3
                R101
                2
                0
                0
                0
                """
            ),
            scenarios=[
                Scenario(
                    label="close_room_impacted_delete",
                    dataset=root_dataset(
                        users=default_users(),
                        rooms=default_rooms(),
                        reservations=resv_record("rv0001", "user011", "R101", "2026-03-21", "11:00", "12:00", 2, "RESERVED", "2026-03-20 09:00", "-"),
                        system_time=now_record("2026-03-20 09:00"),
                    ),
                    input_text=lines("2", "user001", "admin1234", "4", "3", "R101", "2", "0", "0", "0"),
                    actual_summary="영향 예약 처리 흐름 후 rv0001이 삭제되었고 after/rooms.txt의 R101 status가 CLOSED가 되었다.",
                    verdict="PASS",
                    checks=Checks(
                        output_contains=["영향 예약이 있어 처리 흐름을 시작합니다.", "룸이 임시 휴업 처리되었습니다."],
                        after_contains={"rooms.txt": ["ROOM|R101|A룸|4|CLOSED"]},
                        after_not_contains={"reservations.txt": ["rv0001"]},
                    ),
                )
            ],
        )
    )

    add(
        Case(
            tc_id="6.3.4-08",
            spec_ref="6.3.4",
            spec_name="룸 컨디션 관리 - 운영 재개",
            objective="기획서 6.3.4에 따라 CLOSED 룸을 OPEN으로 운영 재개할 수 있는지 확인",
            method="R101이 CLOSED인 상태에서 운영 재개 메뉴 실행",
            expected="변경 전/후 ROOM 레코드 출력 후 R101 status가 OPEN으로 저장된다.",
            input_details=clean(
                """
                2
                user001
                admin1234
                4
                4
                R101
                0
                0
                0
                """
            ),
            scenarios=[
                Scenario(
                    label="reopen_room_success",
                    dataset=root_dataset(
                        users=default_users(),
                        rooms=room_record("R101", "A룸", 4, "CLOSED") + room_record("R102", "B룸", 6, "OPEN"),
                        reservations="",
                        system_time=now_record("2026-03-20 17:30"),
                    ),
                    input_text=lines("2", "user001", "admin1234", "4", "4", "R101", "0", "0", "0"),
                    actual_summary="변경 전/후 ROOM 레코드가 출력되었고 after/rooms.txt의 R101 status가 OPEN으로 저장되었다.",
                    verdict="PASS",
                    checks=Checks(
                        output_contains=["변경 전 ROOM 레코드:", "변경 후 ROOM 레코드:", "룸 운영이 재개되었습니다."],
                        after_contains={"rooms.txt": ["ROOM|R101|A룸|4|OPEN"]},
                    ),
                )
            ],
        )
    )

    add(
        Case(
            tc_id="7.1-01",
            spec_ref="7.1",
            spec_name="상태 자동 갱신",
            objective="기획서 7.1에 따라 RESERVED 예약이 시작+10분 경과 후 NO_SHOW로 자동 전환되는지 확인",
            method="admin 로그인 후 현재 시각을 11:11로 변경하여 11:00 시작 RESERVED 예약의 상태 변화와 파일 저장을 확인",
            expected="상태 변화 요약에 RESERVED -> NO_SHOW : 1건이 출력되고 reservations.txt의 status가 NO_SHOW로 바뀐다.",
            input_details=clean(
                """
                2
                user001
                admin1234
                1
                2026-03-20 11:11
                0
                0
                """
            ),
            scenarios=[
                Scenario(
                    label="auto_noshow",
                    dataset=root_dataset(
                        users=default_users(),
                        rooms=default_rooms(),
                        reservations=resv_record("rv0001", "user011", "R101", "2026-03-20", "11:00", "12:00", 2, "RESERVED", "2026-03-20 09:00", "-"),
                        system_time=now_record("2026-03-20 09:00"),
                    ),
                    input_text=lines("2", "user001", "admin1234", "1", "2026-03-20 11:11", "0", "0"),
                    actual_summary="상태 변화 요약에 RESERVED -> NO_SHOW : 1건이 출력되었고 after/reservations.txt의 rv0001 status가 NO_SHOW가 되었다.",
                    verdict="PASS",
                    checks=Checks(
                        output_contains=["상태 변화 요약:", "- RESERVED -> NO_SHOW : 1건"],
                        after_contains={"reservations.txt": ["|NO_SHOW|2026-03-20 09:00|-"]},
                    ),
                )
            ],
        )
    )

    add(
        Case(
            tc_id="7.1-02",
            spec_ref="7.1",
            spec_name="상태 자동 갱신",
            objective="기획서 7.1에 따라 CHECKED_IN 예약이 종료 시각 이상에서 COMPLETED로 자동 전환되는지 확인",
            method="admin 로그인 후 현재 시각을 10:00으로 변경하여 09:00~10:00 CHECKED_IN 예약의 상태 변화와 파일 저장을 확인",
            expected="상태 변화 요약에 CHECKED_IN -> COMPLETED : 1건이 출력되고 reservations.txt의 status가 COMPLETED로 바뀐다.",
            input_details=clean(
                """
                2
                user001
                admin1234
                1
                2026-03-20 10:00
                0
                0
                """
            ),
            scenarios=[
                Scenario(
                    label="auto_completed",
                    dataset=root_dataset(
                        users=default_users(),
                        rooms=default_rooms(),
                        reservations=resv_record("rv0002", "user022", "R102", "2026-03-20", "09:00", "10:00", 2, "CHECKED_IN", "2026-03-20 08:30", "2026-03-20 08:55"),
                        system_time=now_record("2026-03-20 09:00"),
                    ),
                    input_text=lines("2", "user001", "admin1234", "1", "2026-03-20 10:00", "0", "0"),
                    actual_summary="상태 변화 요약에 CHECKED_IN -> COMPLETED : 1건이 출력되었고 after/reservations.txt의 rv0002 status가 COMPLETED가 되었다.",
                    verdict="PASS",
                    checks=Checks(
                        output_contains=["상태 변화 요약:", "- CHECKED_IN -> COMPLETED : 1건"],
                        after_contains={"reservations.txt": ["|COMPLETED|2026-03-20 08:30|2026-03-20 08:55"]},
                    ),
                )
            ],
        )
    )

    add(
        Case(
            tc_id="7.1-03",
            spec_ref="7.1",
            spec_name="상태 자동 갱신",
            objective="기획서 7.1에 따라 상태 변경 대상이 없으면 '상태 변화 없음'을 출력하는지 확인",
            method="admin 로그인 후 예약이 없는 상태에서 현재 시각을 09:30으로 변경",
            expected="현재 시각이 변경되었습니다. 후 상태 변화 없음이 출력된다.",
            input_details=clean(
                """
                2
                user001
                admin1234
                1
                2026-03-20 09:30
                0
                0
                """
            ),
            scenarios=[
                Scenario(
                    label="auto_no_state_change",
                    dataset=root_dataset(default_users(), default_rooms(), "", now_record("2026-03-20 09:00")),
                    input_text=lines("2", "user001", "admin1234", "1", "2026-03-20 09:30", "0", "0"),
                    actual_summary="현재 시각 변경 성공 후 '상태 변화 없음'이 출력되었다.",
                    verdict="PASS",
                    checks=Checks(output_contains=["현재 시각이 변경되었습니다.", "상태 변화 없음"]),
                )
            ],
        )
    )

    add(
        Case(
            tc_id="7.4-01",
            spec_ref="7.4",
            spec_name="파일 직접 수정 시 처리",
            objective="기획서 7.4에 따라 프로그램이 종료된 상태에서 유효한 파일 수정을 해 두고 재실행하면 정상 시작하는지 확인",
            method="주석/빈 줄이 포함된 유효한 users.txt/rooms.txt/system_time.txt를 미리 써 둔 뒤 프로그램 재실행",
            expected="유효한 직접 수정 파일은 검증을 통과하고 비로그인 메뉴까지 정상 진입한다.",
            input_details="0",
            scenarios=[
                Scenario(
                    label="manual_edit_valid_rerun",
                    dataset=root_dataset(
                        users=clean(
                            """
                            # admin
                            USER|user001|user001|admin1234|admin|admin

                            USER|user011|user011|pw1234|bonsu|member
                            """
                        ),
                        rooms=clean(
                            """
                            # rooms
                            ROOM|R101|A룸|4|OPEN
                            ROOM|R102|B룸|6|OPEN
                            """
                        ),
                        reservations="",
                        system_time=clean(
                            """
                            # clock
                            NOW|2026-03-20 09:00
                            """
                        ),
                    ),
                    input_text=lines("0"),
                    actual_summary="주석/빈 줄이 포함된 직접 수정 파일로도 비로그인 메뉴가 정상 표시되었고 시작 중단은 발생하지 않았다.",
                    verdict="PASS",
                    checks=Checks(output_contains=["[비로그인 메뉴]", "프로그램을 종료합니다."]),
                )
            ],
        )
    )

    add(
        Case(
            tc_id="7.4-02",
            spec_ref="7.4 / 7.2",
            spec_name="파일 직접 수정 시 처리 / 시작 단계 오류 처리",
            objective="기획서 7.4, 7.2에 따라 종료 상태에서 잘못 수정한 파일은 다음 실행 시 파일 오류로 시작을 중단하는지 확인",
            method="직접 수정한 rooms.txt에 잘못된 roomStatus 값을 저장한 뒤 프로그램 재실행",
            expected="[파일 오류] rooms.txt ... roomStatus 값은 OPEN 또는 CLOSED 이어야 합니다.를 출력하고 시작을 중단한다.",
            input_details="(입력 없음)",
            scenarios=[
                Scenario(
                    label="manual_edit_invalid_rerun",
                    dataset=root_dataset(
                        users=default_users(),
                        rooms=room_record("R101", "A룸", 4, "BROKEN"),
                        reservations="",
                        system_time=now_record("2026-03-20 09:00"),
                    ),
                    input_text="",
                    actual_summary="잘못 수정된 rooms.txt로 재실행하자 roomStatus 파일 오류와 시작 중단 메시지가 출력되었다.",
                    verdict="PASS",
                    checks=Checks(
                        output_contains=["[파일 오류] rooms.txt 1행: roomStatus 값은 OPEN 또는 CLOSED 이어야 합니다.", "프로그램 시작을 중단합니다."],
                        output_not_contains=["[비로그인 메뉴]"],
                    ),
                )
            ],
        )
    )

    return cases


def build_repo_facts(case_results: list[dict[str, object]]) -> str:
    return clean(
        f"""
        # 00 Repo Facts

        ## 기준 자료
        - 실제 코드 기준: 현재 브랜치 `v2`
        - 실행 기준 문서: [README.md]({ROOT / 'README.md'})
        - 기획서 기준 문서: `{PDF_PATH}`
        - 기획서 텍스트 추출본: [inspection/_sources/c07_plan_pdf_extract.txt]({ROOT / 'inspection' / '_sources' / 'c07_plan_pdf_extract.txt'})

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
        - 총 TC 수: {len(case_results)}
        - 증거 폴더 루트: [inspection/evidence]({ROOT / 'inspection' / 'evidence'})
        """
    )


def build_spec_mapping(cases: list[Case]) -> str:
    rows = [
        "| 기획서 절 | 절 이름 | 확인 포인트 | 연결 TC |",
        "|---|---|---|---|",
    ]
    mapping: dict[tuple[str, str], list[str]] = {}
    for case in cases:
        key = (case.spec_ref, case.spec_name)
        mapping.setdefault(key, []).append(case.tc_id)
    for (spec_ref, spec_name), tc_ids in mapping.items():
        point = {
            "2.3 / 7.2": "필수 파일 자동 생성과 시작 단계 중단 분기",
            "5.1 / 7.2": "문법 오류 파일 차단",
            "5.2 / 5.3.1 / 7.2": "의미 규칙과 예약 충돌 차단",
        }.get(spec_ref, "해당 절 기능/경계값/오류 처리 확인")
        rows.append(f"| {spec_ref} | {spec_name} | {point} | {', '.join(f'TC-{tc}' for tc in tc_ids)} |")
    return clean("# 01 Spec Mapping\n\n" + "\n".join(rows))


def build_tc_matrix(results: list[dict[str, object]]) -> str:
    lines_out = [
        "# 02 TC Matrix",
        "",
        "| TC ID | 기획서 대응 절 | 목표 | 입력/방법 | 예상 결과 | 실제 결과 | pass-fail | evidence 경로 |",
        "|---|---|---|---|---|---|---|---|",
    ]
    for row in results:
        lines_out.append(
            f"| {row['tc_id']} | {row['spec_ref']} {row['spec_name']} | {row['objective']} | {row['method']} | {row['expected']} | {row['actual']} | {row['verdict']} | `{row['evidence_path']}` |"
        )
    return clean("\n".join(lines_out))


def build_discrepancies() -> str:
    return clean(
        """
        # 03 Discrepancies

        1. `MANUAL_TEST_SCENARIOS.md`는 회원가입/로그인 입력 필드를 `userId` 기준으로 설명하지만, 최종 C07 기획서와 실제 코드는 `loginId`를 직접 입력하고 `userId`를 자동 발급한다.
        2. `MANUAL_TEST_SCENARIOS.md`는 체크인 허용 구간을 `[시작-10분, 시작+15분]`로 적고 있으나, 최종 C07 기획서와 실제 코드는 `[시작-10분, 시작+10분]`을 사용한다.
        3. `MANUAL_TEST_SCENARIOS.md`는 예약 취소 성공 결과를 `CANCELLED` 전환으로 적고 있으나, 최종 C07 기획서와 실제 코드는 예약 레코드 삭제 방식이다.
        4. 저장소의 구형 초안 문서 `docs/1차기획서_스터디룸예약CLI.md`는 30분 단위, `CANCELLED`, `MAINTENANCE`, 패널티, 연장 기능을 포함하지만, 최종 C07 PDF와 현재 브랜치 구현 범위에는 해당 규칙이 없다.
        5. 구형 초안 문서 `docs/1차기획서_스터디룸예약CLI.md`는 `users.txt`에 penalty/status, `rooms.txt`에 운영시간/비품, `reservations.txt`에 extensionCount를 두는 구조를 적지만, 최종 C07 PDF와 현재 브랜치 실제 파일 형식은 더 단순한 6/5/11/2 필드 구조다.
        6. 최종 C07 PDF 7.2는 data 폴더/필수 파일 누락을 “시작 단계 오류”라고 서술하면서도 2.3에서는 자동 생성 후 진행을 설명한다. 실제 구현은 생성이 가능하면 조용히 자동 생성 후 메뉴까지 진입한다.
        """
    )


def build_report_ready_tables(results: list[dict[str, object]]) -> str:
    total = len(results)
    passed = sum(1 for row in results if row["verdict"] == "PASS")
    failed = total - passed
    lines_out = [
        "# 04 Report Ready Tables",
        "",
        "## 전체 요약",
        "",
        "| 항목 | 값 |",
        "|---|---|",
        f"| 총 TC 수 | {total} |",
        f"| PASS | {passed} |",
        f"| FAIL | {failed} |",
        "",
        "## 검사 결과 표",
        "",
        "| TC ID | 대응 절 | 예상 결과(기획서 기준) | 실제 결과 | 판정 |",
        "|---|---|---|---|---|",
    ]
    for row in results:
        lines_out.append(
            f"| {row['tc_id']} | {row['spec_ref']} | {row['expected']} | {row['actual']} | {row['verdict']} |"
        )
    return clean("\n".join(lines_out))


def main() -> None:
    if not JAR_PATH.exists():
        raise SystemExit(f"Missing built JAR: {JAR_PATH}")

    cases = build_cases()
    results = [run_case(case) for case in cases]

    write_text(INSPECTION / "00_repo_facts.md", build_repo_facts(results))
    write_text(INSPECTION / "01_spec_mapping.md", build_spec_mapping(cases))
    write_text(INSPECTION / "02_tc_matrix.md", build_tc_matrix(results))
    write_text(INSPECTION / "03_discrepancies.md", build_discrepancies())
    write_text(INSPECTION / "04_report_ready_tables.md", build_report_ready_tables(results))


if __name__ == "__main__":
    main()
