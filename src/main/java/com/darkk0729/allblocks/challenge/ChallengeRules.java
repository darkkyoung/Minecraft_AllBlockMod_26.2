package com.darkk0729.allblocks.challenge;

public record ChallengeRules(
        boolean progressEventsEnabled,
        boolean dayRaidEventsEnabled,
        boolean finalDayLimitEnabled,
        boolean deathPenaltyEnabled
) {
    public static ChallengeRules from(ChallengeDifficulty difficulty) {
        ChallengeDifficulty safeDifficulty = difficulty == null
                ? ChallengeDifficulty.HARD
                : difficulty;

        return switch (safeDifficulty) {
            case EASY -> new ChallengeRules(
                    false,  // 진행률 이벤트 없음
                    false,  // Day 습격 없음
                    false,  // 100일 제한 없음
                    true    // 사망 패널티 있음
            );

            case NORMAL -> new ChallengeRules(
                    false,  // 현재 보통 전용 이벤트는 아직 없음
                    false,  // Day 습격 없음
                    true,   // 100일 제한 있음
                    true    // 사망 패널티 있음
            );

            case HARD -> new ChallengeRules(
                    true,   // 진행률 이벤트 있음
                    true,   // Day 습격 있음
                    true,   // 100일 제한 있음
                    true    // 사망 패널티 있음
            );
        };
    }
}