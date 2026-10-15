package daybreak.abilitywar.utils.base.minecraft.version;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class VersionTest {
    private NMSVersion paper(String version) {
        return NMSVersion.resolve("org.bukkit.craftbukkit.CraftServer", version + "-R0.1-SNAPSHOT", true, true);
    }
    @Test void paperBuildAcceptsOnly12111() {
        assertEquals(NMSVersion.paper_1_21_11, paper("1.21.11"));
        for (String version : new String[]{"1.20.4", "1.20.6", "1.21", "1.21.10", "1.21.12", "26.1", "26.3", "unknown"}) {
            assertEquals(NMSVersion.UNSUPPORTED, paper(version), version);
        }
        assertEquals(NMSVersion.UNSUPPORTED, NMSVersion.resolve("org.bukkit.craftbukkit.v1_21_R7.CraftServer", "1.21.11-R0.1-SNAPSHOT", false, true));
    }
    @Test void legacyBuildKeepsItsExistingDetection() {
        assertEquals(NMSVersion.v1_20_R3, NMSVersion.resolve("org.bukkit.craftbukkit.v1_20_R3.CraftServer", "1.20.4-R0.1-SNAPSHOT", false, false));
        assertEquals(NMSVersion.UNSUPPORTED, NMSVersion.resolve("org.bukkit.craftbukkit.CraftServer", "1.21.11-R0.1-SNAPSHOT", true, false));
        assertTrue(NMSVersion.paper_1_21_11.isAboveOrEqual(NMSVersion.v1_20_R3));
    }
}
