package com.rustprivate;

import net.minecraft.block.Block;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumHand;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvent;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraftforge.common.DimensionManager;
import net.minecraftforge.event.CommandEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.world.BlockEvent;
import net.minecraftforge.event.world.ExplosionEvent;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.common.ModContainer;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.PlayerEvent;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class PrivateHandler {

    // ------------------------------------------------------------ helpers

    static String id(Block block) {
        ResourceLocation rl = block.getRegistryName();
        return rl == null ? "" : rl.toString();
    }

    static String id(ItemStack stack) {
        if (stack == null || stack.isEmpty() || stack.getItem().getRegistryName() == null) return "";
        return stack.getItem().getRegistryName().toString();
    }

    static boolean in(String value, String[] list) {
        for (String s : list) if (s.equals(value)) return true;
        return false;
    }

    static boolean isCupboardBase(World w, BlockPos pos) {
        return id(w.getBlockState(pos).getBlock()).equals(PrivateConfig.cupboardBlock);
    }

    /** Bottom position of the cupboard that occupies pos, or null. */
    static BlockPos cupboardBase(World w, BlockPos pos) {
        String b = id(w.getBlockState(pos).getBlock());
        if (b.equals(PrivateConfig.cupboardBlock)) return pos;
        if (b.equals(PrivateConfig.cupboardUpperBlock)) {
            if (isCupboardBase(w, pos.down())) return pos.down();
        }
        return null;
    }

    static boolean bypass(EntityPlayer p) {
        return PrivateConfig.opsBypassInCreative && p.capabilities.isCreativeMode && p.canUseCommand(2, "");
    }

    static void say(EntityPlayer p, String text) {
        p.sendStatusMessage(new TextComponentString(text), true);
    }

    static void chat(EntityPlayer p, String text) {
        p.sendStatusMessage(new TextComponentString(text), false);
    }

    /** true when the player may build / break at pos (authorized in every cupboard covering pos). */
    static boolean canBuild(EntityPlayer p, World w, BlockPos pos) {
        if (p == null || bypass(p)) return true;
        ClaimData data = ClaimData.get(w);
        int r = PrivateConfig.radius;
        boolean ok = true;
        boolean changed = false;
        Iterator<Map.Entry<Long, LinkedHashMap<UUID, String>>> it = data.cupboards.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<Long, LinkedHashMap<UUID, String>> e = it.next();
            BlockPos c = BlockPos.fromLong(e.getKey());
            if (Math.abs(c.getX() - pos.getX()) > r || Math.abs(c.getY() - pos.getY()) > r
                    || Math.abs(c.getZ() - pos.getZ()) > r) continue;
            if (w.isBlockLoaded(c) && !isCupboardBase(w, c)) { // cupboard was destroyed (explosion etc.)
                it.remove();
                changed = true;
                continue;
            }
            if (!e.getValue().containsKey(p.getUniqueID())) ok = false;
        }
        if (changed) data.markDirty();
        return ok;
    }

    static final String DENY = "\u00a7c\u0417\u0434\u0435\u0441\u044c \u0447\u0443\u0436\u043e\u0439 \u043f\u0440\u0438\u0432\u0430\u0442 (\u0448\u043a\u0430\u0444). \u0421\u0442\u0440\u043e\u0438\u0442\u044c \u0438 \u043b\u043e\u043c\u0430\u0442\u044c \u043d\u0435\u043b\u044c\u0437\u044f!";

    // ------------------------------------------------------------ protection

    @SubscribeEvent(priority = EventPriority.HIGH)
    public void onBreak(BlockEvent.BreakEvent event) {
        World w = event.getWorld();
        if (w.isRemote) return;
        EntityPlayer p = event.getPlayer();
        BlockPos pos = event.getPos();
        if (!canBuild(p, w, pos)) {
            event.setCanceled(true);
            say(p, DENY);
            return;
        }
        String b = id(event.getState().getBlock());
        if (b.equals(PrivateConfig.cupboardBlock) || b.equals(PrivateConfig.cupboardUpperBlock)) {
            BlockPos base = b.equals(PrivateConfig.cupboardBlock) ? pos : pos.down();
            ClaimData data = ClaimData.get(w);
            if (data.cupboards.remove(base.toLong()) != null) data.markDirty();
        } else if (b.equals(PrivateConfig.sleepingBagBlock)) {
            bagRemoved(w, pos);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public void onPlace(BlockEvent.PlaceEvent event) {
        World w = event.getWorld();
        if (w.isRemote) return;
        EntityPlayer p = event.getPlayer();
        if (p == null) return;
        String b = id(event.getPlacedBlock().getBlock());
        if (in(b, PrivateConfig.allowedBlocks)) return; // C4 / satchel are always allowed
        if (!canBuild(p, w, event.getPos())) {
            event.setCanceled(true);
            say(p, DENY);
            return;
        }
        if (b.equals(PrivateConfig.cupboardBlock)) {
            ClaimData data = ClaimData.get(w);
            LinkedHashMap<UUID, String> map = data.cupboard(event.getPos().toLong());
            map.put(p.getUniqueID(), p.getName());
            data.markDirty();
            chat(p, "\u00a7a\u0428\u043a\u0430\u0444 \u043f\u043e\u0441\u0442\u0430\u0432\u043b\u0435\u043d. \u0422\u044b \u0430\u0432\u0442\u043e\u0440\u0438\u0437\u043e\u0432\u0430\u043d, \u0440\u0430\u0434\u0438\u0443\u0441 \u043f\u0440\u0438\u0432\u0430\u0442\u0430: " + PrivateConfig.radius + " \u0431\u043b\u043e\u043a\u043e\u0432.");
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        World w = event.getWorld();
        EntityPlayer p = event.getEntityPlayer();
        BlockPos base = cupboardBase(w, event.getPos());
        if (base != null) {
            event.setCanceled(true);
            event.setCancellationResult(EnumActionResult.SUCCESS);
            if (!w.isRemote && event.getHand() == EnumHand.MAIN_HAND && p instanceof EntityPlayerMP) {
                openCupboard((EntityPlayerMP) p, base, true);
            }
            return;
        }
        if (w.isRemote) return;
        if (in(id(event.getItemStack()), PrivateConfig.buildTools) && !canBuild(p, w, event.getPos())) {
            event.setCanceled(true);
            event.setCancellationResult(EnumActionResult.FAIL);
            say(p, DENY);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public void onRightClickItem(PlayerInteractEvent.RightClickItem event) {
        World w = event.getWorld();
        if (w.isRemote) return;
        EntityPlayer p = event.getEntityPlayer();
        if (in(id(event.getItemStack()), PrivateConfig.buildTools) && !canBuild(p, w, p.getPosition())) {
            event.setCanceled(true);
            event.setCancellationResult(EnumActionResult.FAIL);
            say(p, DENY);
        }
    }

    // ------------------------------------------------------------ cupboard menu

    public static void openCupboard(EntityPlayerMP p, BlockPos base, boolean sound) {
        ClaimData data = ClaimData.get(p.world);
        LinkedHashMap<UUID, String> map = data.cupboard(base.toLong());
        List<String> names = new ArrayList<String>();
        List<String> uuids = new ArrayList<String>();
        for (Map.Entry<UUID, String> e : map.entrySet()) {
            uuids.add(e.getKey().toString());
            names.add(e.getValue());
        }
        boolean authorized = map.containsKey(p.getUniqueID()) || bypass(p);
        RustPrivate.NET.sendTo(new MsgOpen(base.toLong(), authorized, PrivateConfig.radius, names, uuids), p);
        if (sound) {
            SoundEvent s = SoundEvent.REGISTRY.getObject(new ResourceLocation("minerustaddons:cupboardprivateopen"));
            if (s != null) p.world.playSound(null, base, s, SoundCategory.NEUTRAL, 1.0F, 1.0F);
        }
    }

    public static void handleAction(EntityPlayerMP p, MsgAction msg) {
        World w = p.world;
        BlockPos base = BlockPos.fromLong(msg.pos);
        if (!isCupboardBase(w, base) || p.getDistanceSq(base) > 64.0D) return;
        ClaimData data = ClaimData.get(w);
        LinkedHashMap<UUID, String> map = data.cupboard(base.toLong());
        boolean authorized = map.containsKey(p.getUniqueID()) || bypass(p);
        switch (msg.action) {
            case MsgAction.AUTHORIZE:
                map.put(p.getUniqueID(), p.getName());
                data.markDirty();
                say(p, "\u00a7a\u0422\u044b \u0430\u0432\u0442\u043e\u0440\u0438\u0437\u043e\u0432\u0430\u043d \u0432 \u0448\u043a\u0430\u0444\u0443");
                break;
            case MsgAction.DEAUTHORIZE:
                if (!authorized) return;
                try {
                    UUID target = UUID.fromString(msg.target);
                    String name = map.remove(target);
                    data.markDirty();
                    if (name != null) say(p, "\u00a7e" + name + " \u0434\u0435\u0430\u0432\u0442\u043e\u0440\u0438\u0437\u043e\u0432\u0430\u043d");
                } catch (IllegalArgumentException ignored) {
                }
                break;
            case MsgAction.CLEAR:
                if (!authorized) return;
                map.clear();
                data.markDirty();
                say(p, "\u00a7e\u0421\u043f\u0438\u0441\u043e\u043a \u0430\u0432\u0442\u043e\u0440\u0438\u0437\u0430\u0446\u0438\u0438 \u043e\u0447\u0438\u0449\u0435\u043d");
                break;
            case MsgAction.STORAGE:
                if (!authorized) {
                    say(p, "\u00a7c\u0421\u043d\u0430\u0447\u0430\u043b\u0430 \u0430\u0432\u0442\u043e\u0440\u0438\u0437\u0443\u0439\u0441\u044f \u0432 \u0448\u043a\u0430\u0444\u0443");
                    return;
                }
                openStorage(p, base);
                return;
            default:
                return;
        }
        openCupboard(p, base, false);
    }

    /** Opens the original MineRust cupboard inventory. */
    static void openStorage(EntityPlayerMP p, BlockPos base) {
        ModContainer mc = Loader.instance().getIndexedModList().get("minerustaddons");
        if (mc == null) return;
        int guiId = 11;
        try {
            guiId = Class.forName("net.mcreator.minerustaddons.MCreatorCupboardprivategui").getField("GUIID").getInt(null);
        } catch (Throwable ignored) {
        }
        p.openGui(mc.getMod(), guiId, p.world, base.getX(), base.getY(), base.getZ());
    }

    // ------------------------------------------------------------ sleeping bag

    static final String SPAWN_SENDER = "net.mcreator.minerustaddons.MCreatorINSTALLSPAWNPOINT";

    /** The MineRust "set spawn point" button runs "/spawnpoint @a[r=1]" which puts the spawn INSIDE the
     *  sleeping bag block, so vanilla rejects it on respawn. Replace it with a correct implementation. */
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onCommand(CommandEvent event) {
        ICommandSender sender = event.getSender();
        if (sender == null || !sender.getClass().getName().startsWith(SPAWN_SENDER)) return;
        String name = event.getCommand().getName();
        if ("tellraw".equals(name)) { // original success message, we send our own
            event.setCanceled(true);
            return;
        }
        if (!"spawnpoint".equals(name)) return;
        event.setCanceled(true);
        Entity e = sender.getCommandSenderEntity();
        if (e instanceof EntityPlayerMP) setBagSpawn((EntityPlayerMP) e);
    }

    static void setBagSpawn(EntityPlayerMP p) {
        World w = p.world;
        BlockPos at = p.getPosition();
        BlockPos best = null;
        double bestDist = Double.MAX_VALUE;
        for (BlockPos q : BlockPos.getAllInBox(at.add(-5, -3, -5), at.add(5, 3, 5))) {
            if (id(w.getBlockState(q).getBlock()).equals(PrivateConfig.sleepingBagBlock)) {
                double d = q.distanceSq(at);
                if (d < bestDist) {
                    bestDist = d;
                    best = q.toImmutable();
                }
            }
        }
        if (best == null) {
            chat(p, "\u00a7c[!] \u0421\u043f\u0430\u043b\u044c\u043d\u0438\u043a \u043d\u0435 \u043d\u0430\u0439\u0434\u0435\u043d \u0440\u044f\u0434\u043e\u043c");
            return;
        }
        int dim = w.provider.getDimension();
        p.setSpawnChunk(best.up(), true, dim);
        ClaimData data = ClaimData.get(w);
        data.bags.put(p.getUniqueID(), best.toLong());
        data.markDirty();
        chat(p, "\u00a7e[!] \u00a7f\u0422\u043e\u0447\u043a\u0430 \u0432\u043e\u0437\u0440\u043e\u0436\u0434\u0435\u043d\u0438\u044f \u0443\u0441\u0442\u0430\u043d\u043e\u0432\u043b\u0435\u043d\u0430 \u043d\u0430 \u0441\u043f\u0430\u043b\u044c\u043d\u0438\u043a\u0435!");
    }

    static final String BAG_GONE = "\u00a7c[!] \u0422\u0432\u043e\u0439 \u0441\u043f\u0430\u043b\u044c\u043d\u0438\u043a \u0443\u043d\u0438\u0447\u0442\u043e\u0436\u0435\u043d - \u0442\u043e\u0447\u043a\u0430 \u0432\u043e\u0437\u0440\u043e\u0436\u0434\u0435\u043d\u0438\u044f \u0441\u0431\u0440\u043e\u0448\u0435\u043d\u0430";

    static void clearSpawn(EntityPlayer p, BlockPos bag, int dim) {
        BlockPos cur = p.getBedLocation(dim);
        if (cur != null && (cur.equals(bag.up()) || cur.equals(bag))) {
            p.setSpawnChunk(null, false, dim);
            chat(p, BAG_GONE);
        }
    }

    /** A sleeping bag at pos was removed: reset spawn of its online owners now (offline ones on login/death). */
    static void bagRemoved(World w, BlockPos pos) {
        ClaimData data = ClaimData.get(w);
        long key = pos.toLong();
        int dim = w.provider.getDimension();
        Iterator<Map.Entry<UUID, Long>> it = data.bags.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<UUID, Long> e = it.next();
            if (e.getValue() != key) continue;
            EntityPlayer owner = w.getMinecraftServer() == null ? null
                    : w.getMinecraftServer().getPlayerList().getPlayerByUUID(e.getKey());
            if (owner != null) {
                clearSpawn(owner, pos, dim);
                it.remove();
                data.markDirty();
            }
        }
    }

    /** Removes the spawn point if the recorded sleeping bag no longer exists. */
    static void validate(EntityPlayer p) {
        if (p.world.isRemote) return;
        for (WorldServer ws : DimensionManager.getWorlds()) {
            ClaimData data = ClaimData.get(ws);
            Long key = data.bags.get(p.getUniqueID());
            if (key == null) continue;
            BlockPos bag = BlockPos.fromLong(key);
            if (!id(ws.getBlockState(bag).getBlock()).equals(PrivateConfig.sleepingBagBlock)) {
                data.bags.remove(p.getUniqueID());
                data.markDirty();
                clearSpawn(p, bag, ws.provider.getDimension());
            }
        }
    }

    @SubscribeEvent
    public void onExplosion(ExplosionEvent.Detonate event) {
        World w = event.getWorld();
        if (w.isRemote) return;
        for (BlockPos pos : event.getAffectedBlocks()) {
            if (id(w.getBlockState(pos).getBlock()).equals(PrivateConfig.sleepingBagBlock)) bagRemoved(w, pos);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onDeath(LivingDeathEvent event) {
        if (event.getEntityLiving() instanceof EntityPlayerMP) validate((EntityPlayer) event.getEntityLiving());
    }

    @SubscribeEvent
    public void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.player instanceof EntityPlayerMP) validate(event.player);
    }
}
