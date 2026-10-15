package daybreak.abilitywar.test;

import daybreak.abilitywar.AbilityWar;
import daybreak.abilitywar.ability.AbilityBase;
import daybreak.abilitywar.ability.AbilityFactory;
import daybreak.abilitywar.ability.list.SwordMaster;
import daybreak.abilitywar.game.list.standard.StandardGame;
import daybreak.abilitywar.game.manager.AbilityList;
import daybreak.abilitywar.utils.base.color.RGB;
import daybreak.abilitywar.utils.base.minecraft.damage.Damages;
import daybreak.abilitywar.utils.base.minecraft.entity.AttributeX;
import daybreak.abilitywar.utils.base.minecraft.nms.*;
import daybreak.abilitywar.utils.base.minecraft.nms.paper_1_21_11.PlayerVisibility;
import daybreak.abilitywar.utils.base.minecraft.version.ServerVersion;
import daybreak.abilitywar.utils.base.minecraft.server.ServerType;
import daybreak.abilitywar.utils.library.MaterialX;
import daybreak.abilitywar.utils.library.ParticleLib;
import daybreak.abilitywar.utils.library.PotionEffects;
import daybreak.abilitywar.utils.library.SoundLib;
import daybreak.abilitywar.utils.library.item.EnchantLib;
import daybreak.abilitywar.ability.list.grapplinghook.Hooks;
import daybreak.abilitywar.utils.base.minecraft.raytrace.RayTrace;
import org.bukkit.*;
import org.bukkit.entity.*;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

public final class PaperSmoke extends JavaPlugin {
    private IDummy fixture;
    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
    @Override public void onEnable() {
        Bukkit.getScheduler().runTaskLater(this, () -> {
            try {
                fixture = NMS.createDummy(Bukkit.getWorlds().getFirst().getSpawnLocation().add(0, 3, 0));
                for (Player viewer : Bukkit.getOnlinePlayers()) fixture.display(viewer);
                Bukkit.getScheduler().runTaskLater(this, this::runChecks, 5L);
            } catch (Throwable error) {
                getLogger().log(java.util.logging.Level.SEVERE, "PAPER_SMOKE_FAIL", error);
                Bukkit.shutdown();
            }
        }, 40L);
    }
    private void runChecks() {
        IDummy dummy = fixture;
        IHologram hologram = null;
        StandardGame game = null;
        Zombie target = null;
        Arrow arrow = null;
        boolean passed = false;
        try {
            require(AbilityWar.getPlugin().isEnabled(), "AbilityWar did not enable");
            require(AbilityWar.getPlugin().getForkUpdater() != null, "Fork updater did not initialize");
            require(ServerVersion.getName().equals("paper_1_21_11"), "Wrong compatibility adapter");
            require(ServerType.getServerType() == ServerType.PAPER, "Wrong Paper compatibility family");
            require(AbilityFactory.isRegistered(SwordMaster.class), "Paper-only ability missing");
            World world = Bukkit.getWorlds().getFirst();
            Location location = world.getSpawnLocation().add(0, 3, 0);
            location.getChunk().load();
            require(AttributeX.GENERIC_MAX_HEALTH != null && AttributeX.GENERIC_ARMOR != null, "Attributes missing");
            require(EnchantLib.DAMAGE_ALL.getEnchantment() != null && EnchantLib.ARROW_INFINITE.getEnchantment() != null, "Enchantments missing");
            for (PotionEffects effect : PotionEffects.values()) {
                require(effect.getEffectType() != null, "Missing potion: " + effect);
                require(PotionEffects.valueOf(effect.getEffectType()).get() == effect, "Potion reverse lookup: " + effect);
            }
            // 가짜 플레이어를 사용하므로 접속 클라이언트가 없어도 능력 생성과 내부 코드 경로를 확인할 수 있습니다.
            Player player = dummy.getBukkitEntity();
            require(dummy.isAlive() && player.isValid(), "Dummy did not spawn or tick");
            require(dummy.getUniqueID().equals(player.getUniqueId()), "Dummy UUID mismatch");
            dummy.addDamage(2);
            NMS.setArrowsInBody(player, 1);
            require(player.getArrowsInBody() == 1, "Arrow metadata");
            NMS.setAbsorptionHearts(player, 4);
            require(NMS.getAbsorptionHearts(player) == 4, "Absorption");
            NMS.setInvisible(player, true);
            require(NMS.isInvisible(player), "Invisibility");
            NMS.setInvisible(player, false);
            NMS.setCooldown(player, Material.BOW, 20);
            require(NMS.hasCooldown(player, Material.BOW) && NMS.getCooldown(player, Material.BOW) == 20, "Cooldown");
            NMS.setCooldown(player, Material.BOW, 0);
            NMS.getBoundingBox(player);
            NMS.getSteeringDirection(player);
            NMS.isJumpingInVehicle(player);
            NMS.rotateHead(player, player, 45, 10);
            NMS.setCamera(player, player);
            NMS.setWorldBorder(player, NMS.createWorldBorder(world.getWorldBorder()));
            NMS.resetWorldBorder(player);
            NMS.sendActionbar(player, "한글 \"따옴표\" 확인", 0, 20, 0);
            NMS.swingHand(player, Hand.MAIN_HAND);
            ItemStack skull = NMS.createCustomSkull("c3832ce7fbf00dd0eaa537918612cd2f2e9a2aa8b6ec5821932d38768f955ce9");
            require(skull.getType() == Material.PLAYER_HEAD && skull.hasItemMeta(), "Skull texture");
            hologram = NMS.newHologram(world, location.getX(), location.getY(), location.getZ(), "한글 \"홀로그램\"");
            require(hologram.getText().contains("한글"), "Hologram text");
            hologram.display(player);
            hologram.setText("변경됨");
            hologram.teleport(world, location.getX() + 1, location.getY(), location.getZ(), 0, 0);
            hologram.hide(player);
            var hook = Hooks.createHook(player, location);
            hook.die();
            target = world.spawn(location, Zombie.class);
            target.setAI(false);
            target.setInvulnerable(false);
            target.setNoDamageTicks(0);
            require(Damages.damageFixed(target, player, 1), "Fixed damage");
            target.setNoDamageTicks(0);
            require(Damages.damageMagic(target, player, true, 1), "Magic damage");
            target.setNoDamageTicks(0);
            require(Damages.damageArrow(target, player, 1), "Arrow damage");
            target.setNoDamageTicks(0);
            require(Damages.damageThorn(target, player, 1), "Thorns damage");
            target.setNoDamageTicks(0);
            require(Damages.damageExplosion(target, player, 1), "Explosion damage");
            arrow = world.spawn(location, Arrow.class);
            NMS.setInGround(arrow, true);
            require(arrow.isInBlock(), "Arrow in-ground state");
            NMS.setPickupStatus(arrow, PickupStatus.DISALLOWED);
            require(NMS.getPickupStatus(arrow) == PickupStatus.DISALLOWED, "Arrow pickup status");
            ParticleLib.REDSTONE.spawnParticle(location, RGB.of(255, 0, 0));
            ParticleLib.SPELL_MOB.spawnParticle(location, RGB.of(0, 0, 0));
            ParticleLib.SPELL.spawnParticle(location);
            ParticleLib.SPELL_INSTANT.spawnParticle(location);
            ParticleLib.DRAGON_BREATH.spawnParticle(location);
            ParticleLib.FLASH.spawnParticle(player, location);
            ParticleLib.BARRIER.spawnParticle(player, location);
            ParticleLib.BLOCK_CRACK.spawnParticle(location, 0, 0, 0, 1, MaterialX.STONE);
            SoundLib.ENTITY_PLAYER_LEVELUP.playSound(location);
            RayTrace.hitsBlock(world, 0, 100, 0, 0, 99, 0);
            game = new StandardGame();
            game.addParticipant(player);
            require(game.start(), "Test game did not start");
            var participant = game.getParticipant(player);
            int count = 0;
            for (var registration : AbilityList.values()) {
                try {
                    AbilityBase ability = AbilityBase.create(registration, participant);
                    ability.destroy();
                } catch (Throwable error) { throw new AssertionError("Ability: " + registration.getManifest().name(), error); }
                count++;
            }
            require(AbilityFactory.nameValues().contains("광대") && AbilityFactory.nameValues().contains("헤르밋"), "Ported abilities missing");
            var visibility = new PlayerVisibility(player);
            visibility.hide();
            require(NMS.isInvisible(player), "Visibility hide");
            visibility.show();
            require(!NMS.isInvisible(player), "Visibility restore");
            getLogger().info("PAPER_SMOKE_PASS abilities=" + count + " server=" + Bukkit.getName());
            passed = true;
        } catch (Throwable error) {
            getLogger().log(java.util.logging.Level.SEVERE, "PAPER_SMOKE_FAIL", error);
        } finally {
            if (arrow != null) arrow.remove();
            if (target != null) target.remove();
            if (game != null) game.stop(true);
            if (hologram != null && !hologram.isUnregistered()) hologram.unregister();
            if (dummy != null) dummy.remove();
            if (passed && Boolean.getBoolean("abilitywar.smoke.network")) waitForPlayers(0);
            else Bukkit.getScheduler().runTaskLater(this, Bukkit::shutdown, 10L);
        }
    }
    private void waitForPlayers(int attempts) {
        if (Bukkit.getOnlinePlayers().size() < 2) {
            if (attempts >= 60) {
                getLogger().severe("PAPER_NETWORK_FAIL: two clients did not connect");
                Bukkit.shutdown();
            } else Bukkit.getScheduler().runTaskLater(this, () -> waitForPlayers(attempts + 1), 20L);
            return;
        }
        Player owner = Bukkit.getPlayerExact("AWOwner");
        Player viewer = Bukkit.getPlayerExact("AWViewer");
        require(owner != null && viewer != null, "Network test client names");
        Location location = owner.getWorld().getSpawnLocation().add(0, 3, 0);
        owner.setGameMode(GameMode.CREATIVE);
        viewer.setGameMode(GameMode.CREATIVE);
        owner.teleport(location);
        viewer.teleport(location.clone().add(1, 0, 0));
        owner.getInventory().setItemInMainHand(new ItemStack(Material.DIAMOND_SWORD));
        owner.getInventory().setHelmet(new ItemStack(Material.DIAMOND_HELMET));
        var visibility = new PlayerVisibility(owner);
        var hologram = NMS.newHologram(location.getWorld(), location.getX(), location.getY() + 2, location.getZ(), "개인 홀로그램");
        Bukkit.getScheduler().runTaskLater(this, () -> {
            getLogger().info("PAPER_NETWORK_HIDE owner=" + owner.getEntityId());
            visibility.hide();
            hologram.display(viewer);
            Bukkit.getScheduler().runTaskLater(this, () -> {
                getLogger().info("PAPER_NETWORK_SHOW");
                visibility.show();
                hologram.setText("변경됨");
                hologram.teleport(location.getWorld(), location.getX() + 1, location.getY() + 2, location.getZ(), 0, 0);
                Bukkit.getScheduler().runTaskLater(this, () -> {
                    hologram.unregister();
                    getLogger().info("PAPER_NETWORK_SERVER_PASS");
                    Bukkit.shutdown();
                }, 30L);
            }, 30L);
        }, 30L);
    }
}
