package me.cortex.voxy.client.config;

import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import me.cortex.voxy.client.core.SSAO;
import me.cortex.voxy.common.Logger;
import me.cortex.voxy.common.util.cpu.CpuLayout;
import me.cortex.voxy.commonImpl.VoxyCommon;

import java.io.FileReader;
import java.io.IOException;
import java.lang.reflect.Modifier;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
//? if 1.20.1
import me.jellysquid.mods.sodium.client.gui.options.storage.OptionStorage;

public class VoxyConfig
//? if 1.20.1
    implements OptionStorage<VoxyConfig>
{
    private static final Gson GSON = new GsonBuilder()
            .setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
            .setPrettyPrinting()
            .excludeFieldsWithModifiers(Modifier.PRIVATE)
            .create();

    public static VoxyConfig CONFIG = loadOrCreate();

    public boolean enabled = true;
    public boolean enableRendering = true;
    public boolean ingestEnabled = true;
    public float sectionRenderDistance = 16;
    public int serviceThreads = (int) Math.max(CpuLayout.getCoreCount()/1.5, 1);
    public float subDivisionSize = 64;
    public int skyFogDistance = 96;
    public float fogIntensity = 1.0f;
    public float fogDensity = 0.0f;
    public boolean adaptCloudDistance = true;
    public int cloudDistance = 0;
    public boolean dontUseSodiumBuilderThreads = false;

    /**
     * How far (in blocks) the chunk bound used to suppress LoD inside the vanilla render distance
     * is pulled back from the vanilla render distance edge.
     * <p>
     * The bound is the far envelope of the AABB of every vanilla section, and the LoD in front of
     * that envelope is discarded. Because the AABB is a full 16 block section, its far side sits
     * roughly a section behind the terrain surface it wraps, so the envelope reaches past the point
     * where the vanilla renderer actually stops drawing and cuts away LoD that nothing else covers
     * (a ring of void at the vanilla/LoD boundary). Pulling the bound back removes that void, but
     * pulling it back lets the (coarser) LoD surface poke through the vanilla terrain instead, and
     * while chunks stream in that seam flickers. In practice leaving the bound at the vanilla edge
     * (0) looks best once the LoD subdivision threshold keeps the LoD level transitions far enough
     * out, so raise this only if a ring of void shows up at the vanilla render distance.
     */
    public int chunkBoundInset = 0;

    public String ssaoMode;

    public boolean useEnvironmentalFog = true;

    public SSAO.SSAOMode getSSAOMode() {
        if (this.ssaoMode == null) return SSAO.SSAOMode.AUTO;
        try {
            return SSAO.SSAOMode.valueOf(this.ssaoMode.toUpperCase(Locale.ROOT));
        } catch (Exception e) { return SSAO.SSAOMode.AUTO; }
    }

    public void setSSAOMode(SSAO.SSAOMode mode) {
        this.ssaoMode = mode.name().toLowerCase(Locale.ROOT);
    }

    private static VoxyConfig loadOrCreate() {
        if (VoxyCommon.isAvailable()) {
            var path = getConfigPath();
            if (Files.exists(path)) {
                try (FileReader reader = new FileReader(path.toFile())) {
                    var conf = GSON.fromJson(reader, VoxyConfig.class);
                    if (conf != null) {
                        conf.save();
                        return conf;
                    } else {
                        Logger.error("Failed to load voxy config, resetting");
                    }
                } catch (IOException e) {
                    Logger.error("Could not parse config", e);
                }
            }
            Logger.info("Config doesnt exist, creating new");
            var config = new VoxyConfig();
            config.save();
            return config;
        } else {
            var config = new VoxyConfig();
            config.enabled = false;
            config.enableRendering = false;
            return config;
        }
    }

    public void save() {
        if (!VoxyCommon.isAvailable()) {
            Logger.info("Not saving config since voxy is unavalible");
            return;
        }

        try {
            Files.writeString(getConfigPath(), GSON.toJson(this));
        } catch (IOException e) {
            Logger.error("Failed to write config file", e);
        }
    }

    private static Path getConfigPath() {
        return VoxyCommon.getPlatformUtil().getConfigDir().resolve("voxy-config.json");
    }

    //? if 1.20.1 {
    @Override
    public VoxyConfig getData() {
        return this;
    }
    //? }

    public boolean isRenderingEnabled() {
        return VoxyCommon.isAvailable() && this.enabled && this.enableRendering;
    }
}
