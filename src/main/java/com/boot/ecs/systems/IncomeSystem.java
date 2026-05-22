package com.boot.ecs.systems;

import com.boot.ecs.EcsWorld;
import com.boot.ecs.components.IncomeSource;

public final class IncomeSystem {

    private IncomeSystem() {}

    public static int totalRate(EcsWorld ecs) {
        int[] sum = { 0 };
        ecs.dominion().findEntitiesWith(IncomeSource.class)
                .stream().forEach(r -> sum[0] += r.comp().ratePerSec());
        return sum[0];
    }
}
