package com.rustprivate;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.world.World;
import net.minecraft.world.storage.MapStorage;
import net.minecraft.world.storage.WorldSavedData;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/** Per-dimension storage: tool cupboards with their authorized players and sleeping bag owners. */
public class ClaimData extends WorldSavedData {
    public static final String NAME = "rustprivate_claims";

    /** cupboard bottom block position -> (player uuid -> player name) */
    public final Map<Long, LinkedHashMap<UUID, String>> cupboards = new HashMap<Long, LinkedHashMap<UUID, String>>();
    /** player uuid -> sleeping bag position */
    public final Map<UUID, Long> bags = new HashMap<UUID, Long>();

    public ClaimData() {
        super(NAME);
    }

    public ClaimData(String name) {
        super(name);
    }

    public static ClaimData get(World world) {
        MapStorage storage = world.getPerWorldStorage();
        ClaimData data = (ClaimData) storage.getOrLoadData(ClaimData.class, NAME);
        if (data == null) {
            data = new ClaimData();
            storage.setData(NAME, data);
        }
        return data;
    }

    public LinkedHashMap<UUID, String> cupboard(long pos) {
        LinkedHashMap<UUID, String> map = cupboards.get(pos);
        if (map == null) {
            map = new LinkedHashMap<UUID, String>();
            cupboards.put(pos, map);
            markDirty();
        }
        return map;
    }

    @Override
    public void readFromNBT(NBTTagCompound nbt) {
        cupboards.clear();
        bags.clear();
        NBTTagList list = nbt.getTagList("cupboards", 10);
        for (int i = 0; i < list.tagCount(); i++) {
            NBTTagCompound c = list.getCompoundTagAt(i);
            LinkedHashMap<UUID, String> players = new LinkedHashMap<UUID, String>();
            NBTTagList pl = c.getTagList("players", 10);
            for (int j = 0; j < pl.tagCount(); j++) {
                NBTTagCompound p = pl.getCompoundTagAt(j);
                try {
                    players.put(UUID.fromString(p.getString("uuid")), p.getString("name"));
                } catch (IllegalArgumentException ignored) {
                }
            }
            cupboards.put(c.getLong("pos"), players);
        }
        NBTTagList bl = nbt.getTagList("bags", 10);
        for (int i = 0; i < bl.tagCount(); i++) {
            NBTTagCompound b = bl.getCompoundTagAt(i);
            try {
                bags.put(UUID.fromString(b.getString("uuid")), b.getLong("pos"));
            } catch (IllegalArgumentException ignored) {
            }
        }
    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound nbt) {
        NBTTagList list = new NBTTagList();
        for (Map.Entry<Long, LinkedHashMap<UUID, String>> e : cupboards.entrySet()) {
            NBTTagCompound c = new NBTTagCompound();
            c.setLong("pos", e.getKey());
            NBTTagList pl = new NBTTagList();
            for (Map.Entry<UUID, String> p : e.getValue().entrySet()) {
                NBTTagCompound t = new NBTTagCompound();
                t.setString("uuid", p.getKey().toString());
                t.setString("name", p.getValue());
                pl.appendTag(t);
            }
            c.setTag("players", pl);
            list.appendTag(c);
        }
        nbt.setTag("cupboards", list);
        NBTTagList bl = new NBTTagList();
        for (Map.Entry<UUID, Long> e : bags.entrySet()) {
            NBTTagCompound b = new NBTTagCompound();
            b.setString("uuid", e.getKey().toString());
            b.setLong("pos", e.getValue());
            bl.appendTag(b);
        }
        nbt.setTag("bags", bl);
        return nbt;
    }
}
