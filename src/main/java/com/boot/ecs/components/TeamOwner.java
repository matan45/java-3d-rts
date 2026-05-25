package com.boot.ecs.components;

import dev.dominion.ecs.api.Entity;

public final class TeamOwner {

    public final Team team;

    public TeamOwner(Team team) {
        this.team = team;
    }

    public static boolean isPlayer(Entity e) {
        if (e == null) return false;
        TeamOwner t = e.get(TeamOwner.class);
        return t != null && t.team == Team.PLAYER;
    }

    public static Team teamOf(Entity e) {
        if (e == null) return Team.NEUTRAL;
        TeamOwner t = e.get(TeamOwner.class);
        return t != null ? t.team : Team.NEUTRAL;
    }
}
