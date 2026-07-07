package com.example.vivizip.common.exception;

import java.util.List;

//여러 에러 코드를 하나의 카테고리로 묶는 역할
public enum PredefinedErrorStatus {
    DEFAULT(List.of(ErrorStatus._INTERNAL_SERVER_ERROR)),
    AUTH(List.of(
            ErrorStatus._INTERNAL_SERVER_ERROR,
            ErrorStatus._UNAUTHORIZED_LOGIN_DATA_RETRIEVAL_ERROR,
            ErrorStatus._ASSIGNABLE_PARAMETER,
            ErrorStatus.MEMBER_NOT_FOUND
    )); //인증 관련 에러(TODO : 추후 controller에서 @ApiErrorCodeExample(PredefinedErrorStatus.AUTH)로 자동 등록 가능)

    private final List<ErrorStatus> errorStatuses;

    PredefinedErrorStatus(List<ErrorStatus> errorStatuses) {
        this.errorStatuses = errorStatuses;
    }

    public List<ErrorStatus> getErrorStatuses() {
        return errorStatuses;
    }
}
