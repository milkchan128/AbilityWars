package daybreak.abilitywar.patch.list;

import daybreak.abilitywar.patch.Patchable;

public class Patches {

	private Patches() {}

	public static final Patchable[] VALUES = org.bukkit.Bukkit.getPluginManager().getPlugin("Essentials") == null
			? new Patchable[0] : new Patchable[] {new EssentialsTeleportInvulnerability()};

}
