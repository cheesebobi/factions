package io.icker.factions.config;

import com.google.gson.annotations.SerializedName;

import java.util.ArrayList;
import java.util.List;

public class SafeConfig {
    @SerializedName("enderChest")
    public boolean ENDER_CHEST = true;

    @SerializedName("double")
    public boolean DOUBLE = true;

    @SerializedName("blockedItems")
    public List<String> BLOCKED_ITEMS = new ArrayList<>();
}
