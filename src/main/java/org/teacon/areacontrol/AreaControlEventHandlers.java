package org.teacon.areacontrol;

import it.unimi.dsi.fastutil.objects.ObjectArrays;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.living.LivingSpawnEvent;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.world.BlockEvent;
import net.minecraftforge.event.world.ExplosionEvent;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.server.permission.PermissionAPI;
import org.teacon.areacontrol.api.Area;
import org.teacon.areacontrol.api.AreaProperties;

@Mod.EventBusSubscriber(modid = "area_control")
public final class AreaControlEventHandlers {
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onCheckSpawn(LivingSpawnEvent.CheckSpawn event) {
        final BlockPos spawnPos = new BlockPos(event.getX(), event.getY(), event.getZ());
        final Area targetArea = AreaManager.INSTANCE.findBy(event.getWorld(), spawnPos);
        if (!AreaProperties.getBool(targetArea, "area.allow_spawn")) {
            event.setResult(Event.Result.DENY);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onSpecialSpawn(LivingSpawnEvent.SpecialSpawn event) {
        final BlockPos spawnPos = new BlockPos(event.getX(), event.getY(), event.getZ());
        final Area targetArea = AreaManager.INSTANCE.findBy(event.getWorld(), spawnPos);
        if (!AreaProperties.getBool(targetArea, "area.allow_special_spawn")) {
            event.setCanceled(true);
        }
    }

    // This one is fired when player directly attacks something else
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onAttackEntity(AttackEntityEvent event) {
        if (event.getPlayer().level.isClientSide) {
            return;
        }
        // We use the location of target entity to find the area.
        final Entity target = event.getTarget();
        final Area targetArea = AreaManager.INSTANCE.findBy(target.getCommandSenderWorld().dimension(), target.blockPosition());
        if (target instanceof Player) {
            if (!AreaProperties.getBool(targetArea, "area.allow_pvp") && !PermissionAPI.getPermission((ServerPlayer) event.getPlayer(), AreaControlPermissions.BYPASS_PVP)) {
                event.getPlayer().displayClientMessage(new TranslatableComponent("area_control.notice.pvp_disabled", ObjectArrays.EMPTY_ARRAY), true);
                event.setCanceled(true);
            }
        } else {
            if (!AreaProperties.getBool(targetArea, "area.allow_attack") && !PermissionAPI.getPermission((ServerPlayer) event.getPlayer(), AreaControlPermissions.BYPASS_ATTACK)) {
                event.getPlayer().displayClientMessage(new TranslatableComponent("area_control.notice.pve_disabled", ObjectArrays.EMPTY_ARRAY), true);
                event.setCanceled(true); // TODO Show notice when this action is blocked
            }
        }
    }

    // This one is fired when player is using "indirect" tools, e.g. ranged weapons such as bows
    // and crossbows, to attack something else.
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onAttackEntity(LivingAttackEvent event) {
        final Entity src = event.getSource().getEntity();
        if (src instanceof Player) {
            if (src.level.isClientSide) {
                return;
            }
            // Same above, we use the location of target entity to find the area.
            final Entity target = event.getEntity();
            final Area targetArea = AreaManager.INSTANCE.findBy(target.getCommandSenderWorld().dimension(), target.blockPosition());
            if (target instanceof Player) {
                if (!AreaProperties.getBool(targetArea, "area.allow_pvp") && !PermissionAPI.getPermission((ServerPlayer) src, AreaControlPermissions.BYPASS_PVP)) {
                    ((Player) src).displayClientMessage(new TranslatableComponent("area_control.notice.pvp_disabled", ObjectArrays.EMPTY_ARRAY), true);
                    event.setCanceled(true);
                }
            } else {
                if (!AreaProperties.getBool(targetArea, "area.allow_attack") && !PermissionAPI.getPermission((ServerPlayer) src, AreaControlPermissions.BYPASS_ATTACK)) {
                    ((Player) src).displayClientMessage(new TranslatableComponent("area_control.notice.pve_disabled", ObjectArrays.EMPTY_ARRAY), true);
                    event.setCanceled(true);
                }
            }
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onInteractEntitySpecific(PlayerInteractEvent.EntityInteractSpecific event) {
        if (event.getPlayer().level.isClientSide) {
            return;
        }
        final Area targetArea = AreaManager.INSTANCE.findBy(event.getWorld(), event.getPos());
        if (!AreaProperties.getBool(targetArea, "area.allow_interact_entity_specific")
                && !PermissionAPI.getPermission((ServerPlayer) event.getPlayer(), AreaControlPermissions.BYPASS_INTERACT_ENTITY_SPECIFIC)) {
                event.setCanceled(true);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onInteractEntity(PlayerInteractEvent.EntityInteract event) {
        if (event.getWorld().isClientSide) {
            return;
        }
        final Area targetArea = AreaManager.INSTANCE.findBy(event.getWorld(), event.getPos());
        if (!AreaProperties.getBool(targetArea, "area.allow_interact_entity")
                && !PermissionAPI.getPermission((ServerPlayer) event.getPlayer(), AreaControlPermissions.BYPASS_INTERACT_ENTITY)) {
                event.setCanceled(true);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onBreakBlock(BlockEvent.BreakEvent event) {
        if (event.getWorld().isClientSide()) {
            return;
        }
        final var p = event.getPlayer();
        final Area targetArea = AreaManager.INSTANCE.findBy(event.getWorld(), event.getPos());
        final var block = event.getWorld().getBlockState(event.getPos());
        final var blockName = ForgeRegistries.BLOCKS.getKey(block.getBlock());
        final var blockSpecificPerm = blockName != null ? "area.allow_break_block." + blockName : null;
        final var modSpecificPerm = blockName != null ? "area.allow_break_block." + blockName.getNamespace() : null;
        var allowed = true;
        if (AreaProperties.keyPresent(targetArea, blockSpecificPerm)) {
            allowed = AreaProperties.getBool(targetArea, blockSpecificPerm);
        } else if (AreaProperties.keyPresent(targetArea, modSpecificPerm)) {
            allowed = AreaProperties.getBool(targetArea, modSpecificPerm);
        } else {
            allowed = AreaProperties.getBool(targetArea, "area.allow_break_block");
        }
        var override = PermissionAPI.getPermission((ServerPlayer) event.getPlayer(), AreaControlPermissions.BYPASS_BREAK_BLOCK);
        if (override != null) {
            allowed |= override;
        }
        if (!allowed) {
            p.displayClientMessage(new TranslatableComponent("area_control.notice.break_block_disabled", ObjectArrays.EMPTY_ARRAY), true);
            event.setCanceled(true);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onClickBlock(PlayerInteractEvent.LeftClickBlock event) {
        if (event.getWorld().isClientSide) {
            return;
        }
        final var  p = event.getPlayer();
        final Area targetArea = AreaManager.INSTANCE.findBy(event.getWorld(), event.getPos());
        if (!AreaProperties.getBool(targetArea, "area.allow_click_block") && !PermissionAPI.getPermission((ServerPlayer) event.getPlayer(), AreaControlPermissions.BYPASS_CLICK_BLOCK)) {
            p.displayClientMessage(new TranslatableComponent("area_control.notice.click_block_disabled", ObjectArrays.EMPTY_ARRAY), true);
            event.setCanceled(true);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onActivateBlock(PlayerInteractEvent.RightClickBlock event) {
        if (event.getWorld().isClientSide) {
            return;
        }
        final Area targetArea = AreaManager.INSTANCE.findBy(event.getWorld(), event.getPos());
        if (!AreaProperties.getBool(targetArea, "area.allow_activate_block")
                && !PermissionAPI.getPermission((ServerPlayer) event.getPlayer(), AreaControlPermissions.BYPASS_ACTIVATE_BLOCK)) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onUseItem(PlayerInteractEvent.RightClickItem event) {
        if (event.getWorld().isClientSide) {
            return;
        }
        final var p = event.getPlayer();
        final Area targetArea = AreaManager.INSTANCE.findBy(event.getWorld(), event.getPos());
        final var itemName = event.getItemStack().getItem().getRegistryName();
        final var itemSpecificPerm = itemName != null ? "area.allow_use_item." + itemName : null;
        final var modSpecificPerm = itemName != null ? "area.allow_use_item." + itemName.getNamespace() : null;
        var allow = true;
        if (AreaProperties.keyPresent(targetArea, itemSpecificPerm)) {
            allow = AreaProperties.getBool(targetArea, itemSpecificPerm);
        } else if (AreaProperties.keyPresent(targetArea, modSpecificPerm)) {
            allow = AreaProperties.getBool(targetArea, modSpecificPerm);
        } else {
            allow = AreaProperties.getBool(targetArea, "area.allow_use_item");
        }
        var override = PermissionAPI.getPermission((ServerPlayer) event.getPlayer(), AreaControlPermissions.BYPASS_USE_ITEM);
        if (override != null) {
            allow |= override;
        }
        if (!allow) {
            p.displayClientMessage(new TranslatableComponent("area_control.notice.use_item_disabled", ObjectArrays.EMPTY_ARRAY), true);
            event.setCanceled(true);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onTramplingFarmland(BlockEvent.FarmlandTrampleEvent event) {
        if (event.getWorld().isClientSide()) {
            return;
        }
        final Area targetArea = AreaManager.INSTANCE.findBy(event.getWorld(), event.getPos());
        if (!AreaProperties.getBool(targetArea, "area.allow_trample_farmland")) {
            // TODO area_control.bypass.trample_farmland?
            event.setCanceled(true);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onPlaceBlock(BlockEvent.EntityPlaceEvent event) {
        if (event.getWorld().isClientSide()) {
            return;
        }
        final Area targetArea = AreaManager.INSTANCE.findBy(event.getWorld(), event.getPos());
        final var block = event.getWorld().getBlockState(event.getPos());
        final var blockName = ForgeRegistries.BLOCKS.getKey(block.getBlock());
        final var blockSpecificPerm = blockName != null ? "area.allow_place_block." + blockName : null;
        final var modSpecificPerm = blockName != null ? "area.allow_place_block." + blockName.getNamespace() : null;
        var allowed = true;
        if (AreaProperties.keyPresent(targetArea, blockSpecificPerm)) {
            allowed = AreaProperties.getBool(targetArea, blockSpecificPerm);
        } else if (AreaProperties.keyPresent(targetArea, modSpecificPerm)) {
            allowed = AreaProperties.getBool(targetArea, modSpecificPerm);
        } else {
            allowed = AreaProperties.getBool(targetArea, "area.allow_place_block");
        }
        final var placer = event.getEntity();
        if (placer instanceof ServerPlayer p) {
            var override = PermissionAPI.getPermission(p, AreaControlPermissions.BYPASS_BREAK_BLOCK);
            if (override != null) {
                allowed |= override;
            }
        }
        if (!allowed) {
            // TODO Client will falsely report item being consumed; however it will return to normal if you click again in inventory GUI
            event.setCanceled(true);
            if (placer instanceof ServerPlayer p) {
                p.displayClientMessage(new TranslatableComponent("area_control.notice.place_block_disabled", ObjectArrays.EMPTY_ARRAY), true);
            }
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void beforeExplosion(ExplosionEvent.Start event) {
        if (event.getWorld().isClientSide()) {
            return;
        }
        final Area targetArea = AreaManager.INSTANCE.findBy(event.getWorld(), new BlockPos(event.getExplosion().getPosition()));
        if (!AreaProperties.getBool(targetArea, "area.allow_explosion")) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void afterExplosion(ExplosionEvent.Detonate event) {
        if (event.getWorld().isClientSide()) {
            return;
        }
        final Area targetArea = AreaManager.INSTANCE.findBy(event.getWorld(), new BlockPos(event.getExplosion().getPosition()));
        if (!AreaProperties.getBool(targetArea, "area.allow_explosion_affect_blocks")) {
            event.getAffectedBlocks().clear();
        } else {
            for (var itr = event.getAffectedBlocks().iterator(); itr.hasNext();) {
                BlockPos affected = itr.next();
                final Area a = AreaManager.INSTANCE.findBy(event.getWorld(), affected);
                if (!AreaProperties.getBool(a, "area.allow_explosion_affect_blocks")) {
                    itr.remove();
                }
            }
        }
        if (!AreaProperties.getBool(targetArea, "area.allow_explosion_affect_entities")) {
            event.getAffectedEntities().clear();
        }
    }
}