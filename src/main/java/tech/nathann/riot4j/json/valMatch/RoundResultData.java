package tech.nathann.riot4j.json.valMatch;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.immutables.value.Value;
import tech.nathann.riot4j.enums.ValRoundResult;
import tech.nathann.riot4j.objects.ValTeamId;

import java.util.List;
import java.util.Optional;

@Value.Immutable
@JsonSerialize(as = ImmutableRoundResultData.class)
@JsonDeserialize(as = ImmutableRoundResultData.class)
public interface RoundResultData {
    int roundNum();
    String roundResult();
    String roundCeremony();
    ValTeamId winningTeam();
    Optional<String> bombPlanter();
    Optional<String> bombDefuser();
    int plantRoundTime();
    List<PlayerLocationData> plantPlayerLocations();
    LocationData plantLocation();
    String plantSite();
    int defuseRoundTime();
    List<PlayerLocationData> defusePlayerLocations();
    LocationData defuseLocation();
    List<PlayerRoundStatsData> playerStats();
    ValRoundResult roundResultCode();
    Optional<String> winningTeamRole();
}
