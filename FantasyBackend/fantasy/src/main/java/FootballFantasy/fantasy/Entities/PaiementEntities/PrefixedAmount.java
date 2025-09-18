package FootballFantasy.fantasy.Entities.PaiementEntities;
import lombok.Getter;

@Getter
public enum PrefixedAmount {
    AMOUNT_10(10),
    AMOUNT_20(20),
    AMOUNT_50(50),
    AMOUNT_100(100);

    private final int value;

    PrefixedAmount(int value) {
        this.value = value;
    }

}