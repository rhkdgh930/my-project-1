package com.example.my_project_1.user.domain;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum SuspensionReason {
    SPAM("스팸 및 광고 게시"),
    ABUSE("욕설 및 비하 발언"),
    INAPPROPRIATE_CONTENT("부적절한 콘텐츠 게시"),
    FRAUD("사기 의심 행위"),
    OTHER("기타 운영 정책 위반");

    private final String description;
}
