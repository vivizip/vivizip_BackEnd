package com.example.vivizip.common.exception.member;

import com.example.vivizip.common.exception.ErrorStatus;
import com.example.vivizip.common.exception.GeneralException;

public class MemberNotFoundException extends GeneralException {
    public MemberNotFoundException() {
        super(ErrorStatus.MEMBER_NOT_FOUND);
    }
}
