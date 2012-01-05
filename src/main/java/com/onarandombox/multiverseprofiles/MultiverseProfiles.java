package com.onarandombox.multiverseprofiles;

import com.onarandombox.MultiverseCore.MultiverseCore;
import com.onarandombox.MultiverseCore.api.MVPlugin;
import com.onarandombox.multiverseprofiles.config.ProfilesConfig;
import com.onarandombox.multiverseprofiles.config.SimpleProfilesConfig;
import com.onarandombox.multiverseprofiles.data.ProfilesData;
import com.onarandombox.multiverseprofiles.data.SimpleProfilesData;
import com.onarandombox.multiverseprofiles.listener.ProfilesPlayerListener;
import com.onarandombox.multiverseprofiles.locale.Messager;
import com.onarandombox.multiverseprofiles.locale.Messaging;
import com.onarandombox.multiverseprofiles.locale.MultiverseMessage;
import com.onarandombox.multiverseprofiles.locale.SimpleMessager;
import com.onarandombox.multiverseprofiles.player.PlayerProfile;
import com.onarandombox.multiverseprofiles.player.SimplePlayerProfile;
import com.onarandombox.multiverseprofiles.util.ProfilesDebug;
import com.onarandombox.multiverseprofiles.util.ProfilesLog;
import com.onarandombox.multiverseprofiles.world.*;
import com.pneumaticraft.commandhandler.CommandHandler;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.configuration.serialization.ConfigurationSerialization;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.logging.Level;

/**
 * @author dumptruckman
 */
public class MultiverseProfiles extends JavaPlugin implements MVPlugin, Messaging {

    private final Shares defaultShares = new SimpleShares(
            Sharing.FALSE, Sharing.FALSE, Sharing.FALSE, Sharing.FALSE, Sharing.FALSE);

    protected CommandHandler commandHandler;
    private final int requiresProtocol = 10;
    private MultiverseCore core = null;

    private final ProfilesPlayerListener playerListener = new ProfilesPlayerListener(this);

    private ProfilesConfig config = null;
    private ProfilesData data = null;

    private Messager messager = new SimpleMessager(this);

    private HashMap<String, WorldProfile> worldProfiles = new HashMap<String, WorldProfile>();
    private HashMap<String, List<WorldGroup>> worldGroups = new HashMap<String, List<WorldGroup>>();

    static {
        ConfigurationSerialization.registerClass(SimplePlayerProfile.class);
        ConfigurationSerialization.registerClass(SimpleWorldProfile.class);
    }
    
    final public void onDisable() {
        // Save the plugin data
        this.getData().save(true);

        // Display disable message/version info
        ProfilesLog.info("disabled.", true);
    }

    final public void onEnable() {
        ProfilesLog.init(this);

        MultiverseCore core;
        core = (MultiverseCore) this.getServer().getPluginManager().getPlugin("Multiverse-Core");
        // Test if the Core was found, if not we'll disable this plugin.
        if (core == null) {
            ProfilesLog.info("Multiverse-Core not found, will keep looking.");
            this.getServer().getPluginManager().disablePlugin(this);
            return;
        }
        this.setCore(core);

        if (this.getCore().getProtocolVersion() < this.getRequiredProtocol()) {
            ProfilesLog.severe("Your Multiverse-Core is OUT OF DATE");
            ProfilesLog.severe("This version of Profiles requires Protocol Level: " + this.getRequiredProtocol());
            ProfilesLog.severe("Your of Core Protocol Level is: " + this.getCore().getProtocolVersion());
            ProfilesLog.severe("Grab an updated copy at: ");
            ProfilesLog.severe("http://bukkit.onarandombox.com/?dir=multiverse-core");
            this.getServer().getPluginManager().disablePlugin(this);
            return;
        }

        try {
            this.getMessager().setLocale(new Locale(this.getConf().getLocale()));
        } catch (IllegalArgumentException e) {
            ProfilesLog.severe(e.getMessage());
            this.getServer().getPluginManager().disablePlugin(this);
            return;
        }
        
        ProfilesDebug.init(this);

        this.getCore().incrementPluginCount();

        // Grab the PluginManager
        final PluginManager pm = this.getServer().getPluginManager();

        // Register Events
        this.registerEvents();

        // Display enable message/version info
        ProfilesLog.info("enabled.", true);
    }

    private void registerEvents() {
        final PluginManager pm = getServer().getPluginManager();
        // Event registering goes here
        pm.registerEvent(Event.Type.PLAYER_CHANGED_WORLD, playerListener, Event.Priority.Normal, this);
        //pm.registerEvent(Event.Type.PLAYER_LOGIN, playerListener, Event.Priority.Normal, this);
    }

    public void log(Level level, String msg) {
        ProfilesLog.log(level, msg);
        ProfilesDebug.log(level, msg);
    }

    public MultiverseCore getCore() {
        return this.core;
    }

    public void setCore(MultiverseCore core) {
        this.core = core;
    }

    public int getProtocolVersion() {
        return 1;
    }

    public String dumpVersionInfo(String buffer) {
        buffer += this.logAndAddToPasteBinBuffer("Multiverse-Inventories Version: " + this.getDescription().getVersion());
        buffer += this.logAndAddToPasteBinBuffer("Bukkit Version: " + this.getServer().getVersion());
        buffer += this.logAndAddToPasteBinBuffer("Special Code: FRN001");
        return buffer;
    }

    private String logAndAddToPasteBinBuffer(String string) {
        ProfilesLog.info(string);
        return ProfilesLog.getString(string + "\n", false);
    }
    
    public ProfilesConfig getConf() {
        if (this.config == null) {
            // Loads the configuration
            try {
                this.config = new SimpleProfilesConfig(this);
            } catch (Exception e) {  // Catch errors loading the config file and exit out if found.
                ProfilesLog.severe(this.getMessager().getMessage(MultiverseMessage.ERROR_CONFIG_LOAD));
                ProfilesLog.severe(e.getMessage());
                Bukkit.getPluginManager().disablePlugin(this);
                return null;
            }
        }
        return this.config;
    }

    public ProfilesData getData() {
        if (this.data == null) {
            // Loads the data
            try {
                this.data = new SimpleProfilesData(this);
            } catch (IOException e) {  // Catch errors loading the language file and exit out if found.
                ProfilesLog.severe(this.getMessager().getMessage(MultiverseMessage.ERROR_DATA_LOAD));
                ProfilesLog.severe(e.getMessage());
                Bukkit.getPluginManager().disablePlugin(this);
                return null;
            }
        }
        return this.data;
    }

    /**
     * {@inheritDoc}
     */
    public Messager getMessager() {
        return messager;
    }

    /**
     * {@inheritDoc}
     */
    public void setMessager(Messager messager) {
        if (messager == null)
            throw new IllegalArgumentException("The new messager can't be null!");

        this.messager = messager;
    }
    
    public int getRequiredProtocol() {
        return this.requiresProtocol;
    }

    public void addWorldProfile(WorldProfile worldProfile) {
        this.worldProfiles.put(worldProfile.getWorld().getName(), worldProfile);
    }

    public WorldProfile getWorldProfile(String worldName) {
        return this.worldProfiles.get(worldName);
    }

    public HashMap<String, List<WorldGroup>> getWorldGroups() {
        return this.worldGroups;
    }

    public Shares getDefaultShares() {
        return defaultShares;
    }

    public void handleSharing(Player player, World fromWorld, World toWorld, Shares shares) {
        PlayerProfile fromWorldProfile = this.getWorldProfile(fromWorld.getName()).getPlayerData(player);
        PlayerProfile toWorldProfile = this.getWorldProfile(fromWorld.getName()).getPlayerData(player);

        // persist current stats for previous world if not sharing
        // then load any saved data
        if (shares.isSharingInventory() != Sharing.TRUE) {
            fromWorldProfile.setInventoryContents(player.getInventory().getContents());
            fromWorldProfile.setArmorContents(player.getInventory().getArmorContents());
            player.getInventory().clear();
            player.getInventory().setContents(toWorldProfile.getInventoryContents());
            player.getInventory().setArmorContents(toWorldProfile.getArmorContents());
        }
        if (shares.isSharingHealth() != Sharing.TRUE) {
            fromWorldProfile.setHealth(player.getHealth());
            player.setHealth(toWorldProfile.getHealth());
        }
        if (shares.isSharingHunger() != Sharing.TRUE) {
            fromWorldProfile.setFoodLevel(player.getFoodLevel());
            fromWorldProfile.setExhaustion(player.getExhaustion());
            fromWorldProfile.setSaturation(player.getSaturation());
            player.setFoodLevel(toWorldProfile.getFoodLevel());
            player.setExhaustion(toWorldProfile.getExhaustion());
            player.setSaturation(toWorldProfile.getSaturation());
        }
        if (shares.isSharingExp() != Sharing.TRUE) {
            fromWorldProfile.setExp(player.getExp());
            fromWorldProfile.setLevel(player.getLevel());
            player.setExp(toWorldProfile.getExp());
            player.setLevel(toWorldProfile.getLevel());
        }
        if (shares.isSharingEffects() != Sharing.TRUE) {
            // Where is the effects API??
        }

        this.getData().save(false);
    }
}
