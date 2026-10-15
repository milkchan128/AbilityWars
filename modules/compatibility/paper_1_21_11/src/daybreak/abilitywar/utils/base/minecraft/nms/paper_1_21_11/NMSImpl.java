package daybreak.abilitywar.utils.base.minecraft.nms.paper_1_21_11;

import daybreak.abilitywar.utils.base.minecraft.SkinInfo;
import daybreak.abilitywar.utils.base.minecraft.boundary.EntityBoundingBox;
import daybreak.abilitywar.utils.base.minecraft.nms.*;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.*;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.PositionMoveRotation;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.craftbukkit.CraftServer;
import org.bukkit.craftbukkit.CraftWorld;
import org.bukkit.craftbukkit.entity.*;
import org.bukkit.entity.*;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.profile.PlayerProfile;
import org.bukkit.util.BoundingBox;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Set;
import java.util.UUID;
import java.lang.reflect.Method;

import static daybreak.abilitywar.utils.base.minecraft.item.Skulls.LINK_HEAD;
import static daybreak.abilitywar.utils.base.minecraft.item.Skulls.customSkulls;

public class NMSImpl implements INMS {
    private static final Method SET_IN_GROUND;
    static {
        try {
            SET_IN_GROUND = net.minecraft.world.entity.projectile.arrow.AbstractArrow.class.getDeclaredMethod("setInGround", boolean.class);
            SET_IN_GROUND.setAccessible(true);
        } catch (ReflectiveOperationException e) { throw new ExceptionInInitializerError(e); }
    }
    @Override public void respawn(Player player) { player.spigot().respawn(); }
    @Override public void clearTitle(Player player) { player.resetTitle(); }
    @Override public void sendTitle(Player player, String title, String subtitle, int fadeIn, int stay, int fadeOut) {
        player.sendTitle(title, subtitle, fadeIn, stay, fadeOut);
    }
    @Override public void sendActionbar(Player player, String text, int fadeIn, int stay, int fadeOut) {
        ((CraftPlayer) player).getHandle().connection.send(new ClientboundSetTitlesAnimationPacket(fadeIn, stay, fadeOut));
        ((CraftPlayer) player).getHandle().connection.send(new ClientboundSetActionBarTextPacket(Component.literal(text)));
    }
    @Override public float getAttackCooldown(Player player) { return player.getAttackCooldown(); }
    @Override public void rotateHead(Player receiver, Entity entity, float yaw, float pitch) {
        var handle = ((CraftEntity) entity).getHandle();
        var connection = ((CraftPlayer) receiver).getHandle().connection;
        connection.send(ClientboundTeleportEntityPacket.teleport(entity.getEntityId(), PositionMoveRotation.of(handle), Set.of(), entity.isOnGround()));
        connection.send(new ClientboundMoveEntityPacket.Rot(entity.getEntityId(), (byte) (yaw * 256 / 360), (byte) (pitch * 256 / 360), entity.isOnGround()));
        connection.send(new ClientboundRotateHeadPacket(handle, (byte) (yaw * 256 / 360)));
    }
    @Override public IHologram newHologram(World world, double x, double y, double z, String text) { return new HologramImpl(world, x, y, z, text); }
    @Override public IHologram newHologram(World world, double x, double y, double z) { return newHologram(world, x, y, z, ""); }
    @Override public IDummy createDummy(Location location) { return createDummy(location, IDummy.DEFAULT_SKIN); }
    @Override public IDummy createDummy(Location location, SkinInfo skin) {
        return new DummyImpl(((CraftServer) Bukkit.getServer()).getServer(), ((CraftWorld) location.getWorld()).getHandle(), location, skin);
    }
    @Override public float getAbsorptionHearts(LivingEntity entity) { return (float) entity.getAbsorptionAmount(); }
    @Override public void setAbsorptionHearts(LivingEntity entity, float amount) {
        if (!Float.isFinite(amount) || amount < 0) throw new IllegalArgumentException("Invalid absorption amount");
        var maximum = entity.getAttribute(org.bukkit.attribute.Attribute.MAX_ABSORPTION);
        if (maximum != null && maximum.getValue() < amount) maximum.setBaseValue(amount);
        entity.setAbsorptionAmount(amount);
    }
    @Override public void broadcastEntityEffect(Entity entity, byte status) {
        var handle = ((CraftEntity) entity).getHandle();
        handle.level().broadcastEntityEvent(handle, status);
    }
    @Override public void setLocation(Entity entity, double x, double y, double z, float yaw, float pitch) {
        ((CraftEntity) entity).getHandle().absSnapTo(x, y, z, yaw, pitch);
    }
    @Override public void removeBoundingBox(ArmorStand stand) { stand.setMarker(true); }
    @Override public void setArrowsInBody(Player player, int count) {
        if (count < 0) throw new IllegalArgumentException("count cannot be negative");
        player.setArrowsInBody(count);
    }
    @Override public void setInvisible(Entity entity, boolean invisible) { ((CraftEntity) entity).getHandle().setInvisible(invisible); }
    @Override public boolean isInvisible(Entity entity) { return ((CraftEntity) entity).getHandle().isInvisible(); }
    @Override public void setCooldown(Player player, Material material, int ticks) { player.setCooldown(material, ticks); }
    @Override public boolean hasCooldown(Player player, Material material) { return player.hasCooldown(material); }
    @Override public int getCooldown(Player player, Material material) { return player.getCooldown(material); }
    @Override public void fakeCollect(Entity entity, Item item) {
        var packet = new ClientboundTakeItemEntityPacket(item.getEntityId(), entity.getEntityId(), item.getItemStack().getAmount());
        for (Player viewer : Bukkit.getOnlinePlayers()) ((CraftPlayer) viewer).getHandle().connection.send(packet);
    }
    @Override public void clearActiveItem(LivingEntity entity) { ((CraftLivingEntity) entity).getHandle().stopUsingItem(); }
    @Override public void swingHand(LivingEntity entity, Hand hand) {
        ((CraftLivingEntity) entity).getHandle().swing(hand == Hand.MAIN_HAND ? InteractionHand.MAIN_HAND : InteractionHand.OFF_HAND);
    }
    @Override public EntityBoundingBox getBoundingBox(Entity entity) {
        BoundingBox box = entity.getBoundingBox();
        Location origin = entity.getLocation();
        return new EntityBoundingBox(entity, box.getMinX() - origin.getX(), box.getMinY() - origin.getY(), box.getMinZ() - origin.getZ(),
                box.getMaxX() - origin.getX(), box.getMaxY() - origin.getY(), box.getMaxZ() - origin.getZ());
    }
    @Override public void setCamera(Player receiver, Entity entity) {
        ((CraftPlayer) receiver).getHandle().connection.send(new ClientboundSetCameraPacket(((CraftEntity) entity).getHandle()));
    }
    @Override public IWorldBorder createWorldBorder(World world) { return new WorldBorderImpl(world); }
    @Override public IWorldBorder createWorldBorder(org.bukkit.WorldBorder border) {
        var result = new WorldBorderImpl(border.getCenter().getWorld());
        result.setCenter(border.getCenter().getX(), border.getCenter().getZ());
        result.setDamageAmount(border.getDamageAmount());
        result.setDamageBuffer(border.getDamageBuffer());
        result.setSize(border.getSize());
        result.setWarningDistance(border.getWarningDistance());
        result.setWarningTime(border.getWarningTime());
        return result;
    }
    @Override public void setWorldBorder(Player receiver, IWorldBorder border) {
        if (!(border instanceof WorldBorderImpl)) throw new IllegalArgumentException("Invalid world border");
        ((CraftPlayer) receiver).getHandle().connection.send(new ClientboundInitializeBorderPacket((WorldBorderImpl) border));
    }
    @Override public void resetWorldBorder(Player receiver) {
        var handle = ((CraftPlayer) receiver).getHandle();
        handle.connection.send(new ClientboundInitializeBorderPacket(handle.level().getWorldBorder()));
    }
    @Override public void setInGround(Arrow arrow, boolean inGround) {
        try { SET_IN_GROUND.invoke(((CraftArrow) arrow).getHandle(), inGround); }
        catch (ReflectiveOperationException e) { throw new IllegalStateException("Cannot update arrow state", e); }
    }
    @Override public boolean isArrow(Entity entity) { return entity instanceof AbstractArrow; }
    @Override public PickupStatus getPickupStatus(Projectile arrow) {
        if (!(arrow instanceof AbstractArrow)) throw new IllegalArgumentException("arrow must be an AbstractArrow");
        return PickupStatus.valueOf(((AbstractArrow) arrow).getPickupStatus().name());
    }
    @Override public void setPickupStatus(Projectile arrow, PickupStatus status) {
        if (!(arrow instanceof AbstractArrow)) throw new IllegalArgumentException("arrow must be an AbstractArrow");
        ((AbstractArrow) arrow).setPickupStatus(AbstractArrow.PickupStatus.valueOf(status.name()));
    }
    @Override public boolean isJumpingInVehicle(LivingEntity entity) {
        return entity instanceof Player ? ((Player) entity).getCurrentInput().isJump() : ((CraftLivingEntity) entity).getHandle().jumping;
    }
    @Override public SteeringDirection getSteeringDirection(LivingEntity entity) {
        if (entity instanceof Player player) {
            var input = player.getCurrentInput();
            return SteeringDirection.get((input.isLeft() ? 1 : 0) - (input.isRight() ? 1 : 0), (input.isForward() ? 1 : 0) - (input.isBackward() ? 1 : 0));
        }
        var handle = ((CraftLivingEntity) entity).getHandle();
        return SteeringDirection.get(handle.xxa, handle.zza);
    }
    @Override public SkullMeta setOwningPlayer(SkullMeta meta, String name) {
        meta.setPlayerProfile(Bukkit.createProfile(name));
        return meta;
    }
    @Override public ItemStack createCustomSkull(String url) {
        String key = url.startsWith(LINK_HEAD) ? url.substring(LINK_HEAD.length()) : url;
        ItemStack cached = customSkulls.getIfPresent(key);
        if (cached != null) return cached.clone();
        ItemStack stack = new ItemStack(Material.PLAYER_HEAD);
        if (url.isEmpty()) return stack;
        PlayerProfile profile = Bukkit.createPlayerProfile(UUID.randomUUID());
        var textures = profile.getTextures();
        try { textures.setSkin(new URL(url.startsWith(LINK_HEAD) ? url : LINK_HEAD + url)); }
        catch (MalformedURLException e) { throw new IllegalArgumentException("Invalid skin URL", e); }
        profile.setTextures(textures);
        SkullMeta meta = (SkullMeta) stack.getItemMeta();
        meta.setOwnerProfile(profile);
        stack.setItemMeta(meta);
        customSkulls.put(key, stack.clone());
        return stack;
    }
}
