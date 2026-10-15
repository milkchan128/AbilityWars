package daybreak.abilitywar.utils.base.minecraft.nms.paper_1_21_11;

import daybreak.abilitywar.AbilityWar;
import daybreak.abilitywar.utils.base.minecraft.nms.IHologram;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.*;
import net.minecraft.world.entity.PositionMoveRotation;
import net.minecraft.world.entity.decoration.ArmorStand;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.craftbukkit.CraftWorld;
import org.bukkit.craftbukkit.entity.CraftPlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class HologramImpl implements IHologram, Listener {
    private final ArmorStand stand;
    private final Set<UUID> viewers = new HashSet<>();
    private boolean unregistered;

    HologramImpl(World world, double x, double y, double z, String text) {
        stand = new ArmorStand(((CraftWorld) world).getHandle(), x, y, z);
        stand.setInvisible(true);
        stand.setMarker(true);
        stand.setCustomNameVisible(true);
        stand.setCustomName(Component.literal(text));
        Bukkit.getPluginManager().registerEvents(this, AbilityWar.getPlugin());
    }
    private void checkRegistered() {
        if (unregistered) throw new IllegalStateException("Hologram unregistered");
    }
    private void send(Player player, Packet<?> packet) {
        ((CraftPlayer) player).getHandle().connection.send(packet);
    }
    private void spawn(Player player) {
        if (!player.isOnline() || !player.getWorld().equals(getLocation().getWorld()) || unregistered) return;
        send(player, new ClientboundAddEntityPacket(stand, 0, BlockPos.containing(stand.position())));
        send(player, new ClientboundSetEntityDataPacket(stand.getId(), stand.getEntityData().getNonDefaultValues()));
    }
    @EventHandler public void onQuit(PlayerQuitEvent event) { viewers.remove(event.getPlayer().getUniqueId()); }
    @EventHandler public void onRespawn(PlayerRespawnEvent event) { redisplay(event.getPlayer()); }
    @EventHandler public void onWorldChange(PlayerChangedWorldEvent event) { redisplay(event.getPlayer()); }
    private void redisplay(Player player) {
        if (viewers.contains(player.getUniqueId())) Bukkit.getScheduler().runTaskLater(AbilityWar.getPlugin(), () -> spawn(player), 2L);
    }
    @Override public void display(Player player) {
        checkRegistered();
        if (viewers.add(player.getUniqueId())) spawn(player);
    }
    @Override public void hide(Player player) {
        checkRegistered();
        if (viewers.remove(player.getUniqueId())) send(player, new ClientboundRemoveEntitiesPacket(stand.getId()));
    }
    @Override public void teleport(World world, double x, double y, double z, float yaw, float pitch) {
        checkRegistered();
        boolean changedWorld = !world.equals(getLocation().getWorld());
        if (changedWorld) for (UUID id : viewers) {
            Player player = Bukkit.getPlayer(id);
            if (player != null) send(player, new ClientboundRemoveEntitiesPacket(stand.getId()));
        }
        stand.setLevel(((CraftWorld) world).getHandle());
        stand.absSnapTo(x, y, z, yaw, pitch);
        for (UUID id : viewers) {
            Player player = Bukkit.getPlayer(id);
            if (player == null || !player.getWorld().equals(world)) continue;
            if (changedWorld) spawn(player);
            else send(player, ClientboundTeleportEntityPacket.teleport(stand.getId(), PositionMoveRotation.of(stand), Set.of(), false));
        }
    }
    @Override public Location getLocation() { return new Location(stand.level().getWorld(), stand.getX(), stand.getY(), stand.getZ(), stand.getYRot(), stand.getXRot()); }
    @Override public String getText() { return stand.getCustomName() == null ? "" : stand.getCustomName().getString(); }
    @Override public void setText(String text) {
        checkRegistered();
        stand.setCustomName(Component.literal(text));
        var packet = new ClientboundSetEntityDataPacket(stand.getId(), stand.getEntityData().getNonDefaultValues());
        for (UUID id : viewers) {
            Player player = Bukkit.getPlayer(id);
            if (player != null && player.getWorld().equals(getLocation().getWorld())) send(player, packet);
        }
    }
    @Override public void unregister() {
        checkRegistered();
        for (UUID id : Set.copyOf(viewers)) {
            Player player = Bukkit.getPlayer(id);
            if (player != null) hide(player);
        }
        viewers.clear();
        unregistered = true;
        HandlerList.unregisterAll(this);
    }
    @Override public boolean isUnregistered() { return unregistered; }
}
