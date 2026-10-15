package daybreak.abilitywar.ability.list.grapplinghook.paper_1_21_11;

import daybreak.abilitywar.utils.base.minecraft.ability.list.grapplinghook.HookEntity;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.FishingHook;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.LevelWriter;
import net.minecraft.world.level.Level;
import org.bukkit.Location;
import org.bukkit.craftbukkit.entity.CraftLivingEntity;
import org.bukkit.entity.LivingEntity;

public class EntityHook extends FishingHook implements HookEntity {

	private static final EntityDataAccessor<Integer> HOOKED_ENTITY = FishingHook.DATA_HOOKED_ENTITY;

	private final Entity target;

	EntityHook(Level world, Player entityhuman, Location location) {
		super(entityhuman, world, 0, 0);
		this.target = new EntityStand(world, location);
		this.hookedIn = target;
		((Entity) this).getEntityData().set(HOOKED_ENTITY, target.getId() + 1);
		((Entity) this).setPos(location.getX(), location.getY(), location.getZ());
		((LevelWriter) world).addFreshEntity(this);
	}

	EntityHook(Level world, Player entityhuman, LivingEntity targetEntity) {
		super(entityhuman, world, 0, 0);
		this.target = ((CraftLivingEntity) targetEntity).getHandle();
		this.hookedIn = target;
		((Entity) this).getEntityData().set(HOOKED_ENTITY, target.getId() + 1);
		((Entity) this).setPos(target.getX(), target.getY(), target.getZ());
		((LevelWriter) world).addFreshEntity(this);
	}

	@Override
	public void tick() {
		((Entity) this).getEntityData().set(HOOKED_ENTITY, target.getId() + 1);
	}

	@Override
	public int retrieve(ItemStack stack) {
		return 0;
	}

	@Override
	public int retrieve(ItemStack stack, net.minecraft.world.InteractionHand hand) {
		return 0;
	}

	@Override
	public void remove(Entity.RemovalReason reason, org.bukkit.event.entity.EntityRemoveEvent.Cause cause) {
		if (target instanceof EntityStand) target.discard();
		super.remove(reason, cause);
	}

	@Override
	public void die() {
		((Entity) this).discard();
	}
}
