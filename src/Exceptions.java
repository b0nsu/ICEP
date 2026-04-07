/*
 * 데이터 검증, 사용자 취소, 정상 종료, 치명적 종료처럼
 * CLI 흐름 제어에 사용하는 공통 예외 타입들을 모아 둔 파일이다.
 */
class AppDataException extends Exception {
    private final String fileName;
    private final int lineNumber;

    AppDataException(String fileName, int lineNumber, String message) {
        super(message);
        this.fileName = fileName;
        this.lineNumber = lineNumber;
    }

    String getFileName() {
        return fileName;
    }

    int getLineNumber() {
        return lineNumber;
    }
}

class FatalAppException extends RuntimeException {
    FatalAppException() {
        super();
    }
}

class ActionAbortedException extends RuntimeException {
    ActionAbortedException() {
        super();
    }
}

class CancelledActionException extends Exception {
    CancelledActionException() {
        super();
    }
}

class ExitProgramException extends RuntimeException {
    ExitProgramException() {
        super();
    }
}
