package daybreak.abilitywar.utils.base.minecraft.nms.paper_1_21_11;

import com.mojang.datafixers.util.Pair;
import daybreak.abilitywar.AbilityWar;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelOutboundHandlerAdapter;
import io.netty.channel.ChannelPromise;
import net.minecraft.network.protocol.game.ClientboundSetEntityDataPacket;
import net.minecraft.network.protocol.game.ClientboundSetEquipmentPacket;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import org.bukkit.Bukkit;
import org.bukkit.craftbukkit.entity.CraftPlayer;
import org.bukkit.craftbukkit.inventory.CraftItemStack;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.*;

public final class PlayerVisibility implements Listener {
    private static final List<Pair<EquipmentSlot, ItemStack>> EMPTY_EQUIPMENT = Arrays.stream(new EquipmentSlot[]{
            EquipmentSlot.MAINHAND, EquipmentSlot.OFFHAND, EquipmentSlot.FEET, EquipmentSlot.LEGS, EquipmentSlot.CHEST, EquipmentSlot.HEAD
    }).map(slot -> Pair.of(slot, ItemStack.EMPTY)).toList();
    private final Player owner;
    private final int entityId;
    private final String handlerName;
    private final Map<UUID, Channel> channels = new HashMap<>();
    private volatile boolean hidden;

    public PlayerVisibility(Player owner) {
        this.owner = owner;
        entityId = owner.getEntityId();
        handlerName = "abilitywar-visibility-" + UUID.randomUUID();
    }
    private Channel channel(Player player) { return ((CraftPlayer) player).getHandle().connection.connection.channel; }
    public void hide() {
        if (hidden) return;
        hidden = true;
        Bukkit.getPluginManager().registerEvents(this, AbilityWar.getPlugin());
        owner.setArrowsInBody(0);
        owner.setBeeStingersInBody(0);
        for (Player viewer : Bukkit.getOnlinePlayers()) inject(viewer);
        ((CraftPlayer) owner).getHandle().setInvisible(true);
    }
    private void inject(Player viewer) {
        if (!viewer.isOnline() || channels.containsKey(viewer.getUniqueId())) return;
        boolean self = viewer.getUniqueId().equals(owner.getUniqueId());
        Channel channel = channel(viewer);
        channels.put(viewer.getUniqueId(), channel);
        channel.eventLoop().execute(() -> {
            if (!hidden || !channel.isActive() || channel.pipeline().get(handlerName) != null) return;
            channel.pipeline().addBefore("packet_handler", handlerName, new ChannelOutboundHandlerAdapter() {
                @Override public void write(ChannelHandlerContext context, Object message, ChannelPromise promise) throws Exception {
                    if (hidden && message instanceof ClientboundSetEquipmentPacket equipment && equipment.getEntity() == entityId && !self) {
                        message = new ClientboundSetEquipmentPacket(entityId, EMPTY_EQUIPMENT);
                    } else if (hidden && message instanceof ClientboundSetEntityDataPacket metadata && metadata.id() == entityId) {
                        List<SynchedEntityData.DataValue<?>> data = new ArrayList<>(metadata.packedItems());
                        for (int index = 0; index < data.size(); index++) {
                            var value = data.get(index);
                            if (value.id() == 0 && value.serializer() == EntityDataSerializers.BYTE) {
                                data.set(index, new SynchedEntityData.DataValue<>(0, EntityDataSerializers.BYTE, (byte) ((Byte) value.value() | 0x20)));
                            }
                        }
                        message = new ClientboundSetEntityDataPacket(entityId, data);
                    }
                    super.write(context, message, promise);
                }
            });
            if (!self) ((CraftPlayer) viewer).getHandle().connection.send(new ClientboundSetEquipmentPacket(entityId, EMPTY_EQUIPMENT));
        });
    }
    private void remove(UUID id) {
        Channel channel = channels.remove(id);
        if (channel != null) channel.eventLoop().execute(() -> {
            if (channel.pipeline().get(handlerName) != null) channel.pipeline().remove(handlerName);
        });
    }
    public void show() {
        if (!hidden) return;
        hidden = false;
        for (UUID id : Set.copyOf(channels.keySet())) remove(id);
        HandlerList.unregisterAll(this);
        ((CraftPlayer) owner).getHandle().setInvisible(false);
        var inventory = owner.getInventory();
        var equipment = new ClientboundSetEquipmentPacket(entityId, List.of(
                Pair.of(EquipmentSlot.MAINHAND, CraftItemStack.asNMSCopy(inventory.getItemInMainHand())),
                Pair.of(EquipmentSlot.OFFHAND, CraftItemStack.asNMSCopy(inventory.getItemInOffHand())),
                Pair.of(EquipmentSlot.FEET, CraftItemStack.asNMSCopy(inventory.getBoots())),
                Pair.of(EquipmentSlot.LEGS, CraftItemStack.asNMSCopy(inventory.getLeggings())),
                Pair.of(EquipmentSlot.CHEST, CraftItemStack.asNMSCopy(inventory.getChestplate())),
                Pair.of(EquipmentSlot.HEAD, CraftItemStack.asNMSCopy(inventory.getHelmet()))));
        for (Player viewer : Bukkit.getOnlinePlayers()) ((CraftPlayer) viewer).getHandle().connection.send(equipment);
    }
    @EventHandler public void onJoin(PlayerJoinEvent event) {
        Bukkit.getScheduler().runTaskLater(AbilityWar.getPlugin(), () -> {
            if (hidden) inject(event.getPlayer());
        }, 2L);
    }
    @EventHandler public void onQuit(PlayerQuitEvent event) { remove(event.getPlayer().getUniqueId()); }
}
