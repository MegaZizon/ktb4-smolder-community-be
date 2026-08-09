package com.dragoncommunity.domain.post.constant;

public final class PostConstant {

    public static final Integer DEFAULT_POST_GET_SIZE = 10;

    public static final String VIEW_COUNT_KEY_PREFIX  = "post:view:count:";

    public static final String POST_DETAIL_KEY_PREFIX  = "post:detail:";

    public static final String LOCK_KEY_PREFIX = "lock:";

    public static final Integer MAX_LOCK_ATTEMPTS = 3;

    public static final long LOCK_WAIT_TIME_MS = 100;

    public static final long LOCK_LEASE_TIME_MS = 3000;

    public static final String VIEW_COUNT_CHECK_LUA_SCRIPT = """
    if redis.call('EXISTS', KEYS[1]) == 1 then
        return redis.call('INCR', KEYS[1])
    else
        return -1
    end
    """;

    private PostConstant() {
    }
}
