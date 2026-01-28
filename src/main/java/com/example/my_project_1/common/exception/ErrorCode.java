package com.example.my_project_1.common.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ErrorCode {

    /**
     * 200:
     */


    /* 400 BAD_REQUEST: 잘못된 요청 (비즈니스 로직 위반) */
    INVALID_INPUT_VALUE(HttpStatus.BAD_REQUEST, "입력 값이 유효하지 않습니다."),
    INVALID_BOARD_POST_RELATION(HttpStatus.BAD_REQUEST, "해당 게시판에 속하지 않은 게시글입니다."),
    DUPLICATED_EMAIL(HttpStatus.BAD_REQUEST, "이미 존재하는 이메일입니다."),
    USER_NOT_ACTIVE(HttpStatus.BAD_REQUEST, "활성화된 유저가 아닙니다."),
    SAME_PASSWORD(HttpStatus.BAD_REQUEST, "기존 비밀번호와 동일합니다."),
    INVALID_EMAIL_TOKEN(HttpStatus.BAD_REQUEST, "유효하지 않은 이메일 인증 토큰입니다."),
    EXPIRED_VERIFICATION_CODE(HttpStatus.BAD_REQUEST, "만료된 이메일 인증 토큰입니다."),
    WRONG_VERIFICATION_CODE(HttpStatus.BAD_REQUEST, "인증 토큰이 일치하지 않습니다."),
    UNVERIFIED_EMAIL(HttpStatus.BAD_REQUEST, "인증되지 않은 이메일 입니다."),

    // 추가: 이미 처리된 상태에 대한 요청
    ALREADY_VERIFIED_USER(HttpStatus.BAD_REQUEST, "이미 이메일 인증이 완료된 사용자입니다."),
    ALREADY_EXIST_BOARD_NAME(HttpStatus.BAD_REQUEST, "이미 존재하는 게시판 이름 입니다."),
    ALREADY_DELETED_BOARD(HttpStatus.BAD_REQUEST, "이미 삭제된 게시판 입니다."),
    ALREADY_DELETED_POST(HttpStatus.BAD_REQUEST, "이미 삭제된 게시글 입니다."),

    // 추가: 상태 관련
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "존재하지 않는 사용자입니다."),
    BOARD_NOT_FOUND(HttpStatus.NOT_FOUND, "존재하지 않는 게시판 입니다."),
    POST_NOT_FOUND(HttpStatus.NOT_FOUND, "존재하지 않는 게시물 입니다."),

    /* 401 UNAUTHORIZED: 인증 실패 */
    LOGOUT_USER(HttpStatus.UNAUTHORIZED, "다시 로그인해주세요."),
    INVALID_CREDENTIALS(HttpStatus.UNAUTHORIZED, "이메일 또는 비밀번호가 올바르지 않습니다."),
    INVALID_PASSWORD(HttpStatus.UNAUTHORIZED, "비밀번호가 올바르지 않습니다."),
    AUTHENTICATION_FAILED(HttpStatus.UNAUTHORIZED, "인증에 실패했습니다."),

    INVALID_TOKEN(HttpStatus.UNAUTHORIZED, "유효하지 않은 토큰입니다."),
    INVALID_ACCESS_TOKEN(HttpStatus.UNAUTHORIZED, "유효하지 않은 엑세스 토큰입니다."),
    INVALID_REFRESH_TOKEN(HttpStatus.UNAUTHORIZED, "유효하지 않은 리프레시 토큰입니다."),
    EXPIRED_ACCESS_TOKEN(HttpStatus.UNAUTHORIZED, "만료된 엑세스 토큰입니다."),
    EXPIRED_REFRESH_TOKEN(HttpStatus.UNAUTHORIZED, "만료된 리프레시 토큰입니다."),

    /* 403 FORBIDDEN: 권한 없음 */
    ACCESS_DENIED(HttpStatus.FORBIDDEN, "접근 권한이 없습니다."),
    USER_SUSPENDED(HttpStatus.FORBIDDEN, "차단된 계정입니다."),

    TOO_MANY_LOGIN_FAIL(HttpStatus.TOO_MANY_REQUESTS, "너무 많은 로그인 시도 실패로 인해 계정이 일시 차단되었습니다, 10분뒤에 다시 시도해주세요."),

    /* 500 INTERNAL_SERVER_ERROR: 서버 내부 오류 */
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "서버에서 알 수 없는 오류가 발생했습니다.");


    private final HttpStatus httpStatus;
    private final String message;
}
