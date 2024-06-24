package eu.karim.jsaispastrop;

import org.bukkit.plugin.java.JavaPlugin;

public class Main extends JavaPlugin {
    private BlockEntityVisibilityManager blockEntityVisibilityManager;
    //TODO private DuelManager duelManager;

    @Override
    public void onEnable() {
        // Initialize the block and entity visibility manager
        blockEntityVisibilityManager = new BlockEntityVisibilityManager(this);

        //TODO Initialize the duel manager
        // duelManager = new DuelManager(blockEntityVisibilityManager);

        getLogger().info("Practice Plugin has been enabled!");
    }

    @Override
    public void onDisable() {
        getLogger().info("Plugin has been disabled!");
    }

    public BlockEntityVisibilityManager getBlockEntityVisibilityManager() {
        return blockEntityVisibilityManager;
    }

    //TODO
    /*public DuelManager getDuelManager() {
        return duelManager;
    }*/
}