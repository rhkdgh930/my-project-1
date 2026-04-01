package com.example.my_project_1.user.event;

public enum UserAccountChangedType {

    PROFILE_UPDATED(false, false),
    DORMANT_REQUEST(true, true),
    DORMANT_RELEASED(true, false),
    WITHDRAWAL_REQUEST(true,true),
    WITHDRAWAL_RESTORED(true, false),
    SECURITY_CHANGED(true, true); // 비번 변경, 차단, 탈퇴 완료 등

    private final boolean shouldEvictCache;
    private final boolean shouldInvalidateToken;

    UserAccountChangedType(boolean shouldEvictCache, boolean shouldInvalidateToken) {
        this.shouldEvictCache = shouldEvictCache;
        this.shouldInvalidateToken = shouldInvalidateToken;
    }

    public boolean shouldEvictCache() {
        return shouldEvictCache;
    }

    public boolean shouldInvalidateToken() {
        return shouldInvalidateToken;
    }
}
