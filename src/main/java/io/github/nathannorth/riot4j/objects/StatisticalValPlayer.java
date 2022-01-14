package io.github.nathannorth.riot4j.objects;

import io.github.nathannorth.riot4j.json.valMatch.PlayerData;
import io.github.nathannorth.riot4j.json.valMatch.PlayerStatsData;

import java.util.Optional;

public class StatisticalValPlayer implements PlayerData {
    private final PlayerData data;

    private final int combatScore;
    private final float headShotPercentage;

    public StatisticalValPlayer(PlayerData data, int combatScore, float headShotPercentage) {
        this.data = data;
        this.combatScore = combatScore;
        this.headShotPercentage = headShotPercentage;
    }

    public int getCombatScore() {
        return combatScore;
    }

    public float getHeadShotPercentage() {
        return headShotPercentage;
    }

    @Override
    public String puuid() {
        return data.puuid();
    }

    @Override
    public String gameName() {
        return data.gameName();
    }

    @Override
    public String tagLine() {
        return data.tagLine();
    }

    @Override
    public String teamId() {
        return data.teamId();
    }

    @Override
    public String partyId() {
        return data.partyId();
    }

    @Override
    public Optional<String> characterId() {
        return data.characterId();
    }

    @Override
    public Optional<PlayerStatsData> stats() {
        return data.stats();
    }

    @Override
    public int competitiveTier() {
        return data.competitiveTier();
    }

    @Override
    public String playerCard() {
        return data.playerCard();
    }

    @Override
    public String playerTitle() {
        return data.playerTitle();
    }
}