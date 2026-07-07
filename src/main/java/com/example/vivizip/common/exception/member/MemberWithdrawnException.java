package com.example.vivizip.common.exception.member;

import com.example.vivizip.common.exception.ErrorStatus;
import com.example.vivizip.common.exception.GeneralException;

public class MemberWithdrawnException extends GeneralException {
    public MemberWithdrawnException() {
        super(ErrorStatus.MEMBER_WITHDRAWN);
    }
}
