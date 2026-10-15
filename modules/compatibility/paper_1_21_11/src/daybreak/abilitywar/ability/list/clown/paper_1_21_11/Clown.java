package daybreak.abilitywar.ability.list.clown.paper_1_21_11;

import daybreak.abilitywar.ability.list.clown.AbstractClown;
import daybreak.abilitywar.game.AbstractGame.Participant;
import daybreak.abilitywar.utils.base.minecraft.nms.paper_1_21_11.PlayerVisibility;

public class Clown extends AbstractClown {
    private final PlayerVisibility visibility;
    public Clown(Participant participant) {
        super(participant);
        visibility = new PlayerVisibility(getPlayer());
    }
    @Override protected void hide0() { visibility.hide(); }
    @Override protected void show0() { visibility.show(); }
}
