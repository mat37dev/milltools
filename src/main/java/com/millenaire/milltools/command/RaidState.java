package com.millenaire.milltools.command;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;

public class RaidState extends SavedData {

    private static final String SAVE_KEY = "milltools_state";
    private static final SavedData.Factory<RaidState> FACTORY = new SavedData.Factory<>(
            RaidState::new,
            RaidState::load
    );

    private boolean enabled = true;

    public RaidState() {}

    public static RaidState load(CompoundTag tag, HolderLookup.Provider provider) {
        RaidState state = new RaidState();
        if (tag.contains("enabled")) {
            state.enabled = tag.getBoolean("enabled");
        }
        return state;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider provider) {
        tag.putBoolean("enabled", enabled);
        return tag;
    }

    public static RaidState get(ServerLevel level) {
        return level.getServer()
                .overworld()
                .getDataStorage()
                .computeIfAbsent(FACTORY, SAVE_KEY);
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
        setDirty();
    }
}
