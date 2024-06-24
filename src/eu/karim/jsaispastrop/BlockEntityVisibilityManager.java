package eu.karim.jsaispastrop;

import com.comphenix.protocol.wrappers.BlockPosition;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.Vector;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketEvent;
import com.comphenix.protocol.wrappers.WrappedBlockData;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.HashSet;
import java.util.Objects;

public class BlockEntityVisibilityManager implements Listener {
    private final ProtocolManager protocolManager;
    private final Map<UUID, String> playerGroups; // Store the group of each player
    private final Map<String, Set<BlockData>> groupBlocks; // Store blocks placed by each group
    private final Map<UUID, Set<UUID>> hiddenEntities; // Store hidden entities per player

    public BlockEntityVisibilityManager(JavaPlugin plugin) {
        protocolManager = ProtocolLibrary.getProtocolManager();
        playerGroups = new HashMap<>();
        groupBlocks = new HashMap<>();
        hiddenEntities = new HashMap<>();

        Bukkit.getPluginManager().registerEvents(this, plugin);

        for (Player player : Bukkit.getOnlinePlayers()) {
            assignPlayerToGroup(player, "A");
        }

        protocolManager.addPacketListener(new PacketAdapter(plugin, PacketType.Play.Server.BLOCK_CHANGE) {
            @Override
            public void onPacketSending(PacketEvent event) {
                Player player = event.getPlayer();
                WrappedBlockData blockData = event.getPacket().getBlockData().read(0);
                Material blockType = blockData.getType();
                BlockPosition blockPosition = event.getPacket().getBlockPositionModifier().read(0);
                BlockData blockLocation = new BlockData(blockPosition.toVector(), blockType);

                // Hide water and lava placed by other groups
                if (blockType == Material.WATER || blockType == Material.LAVA) {
                    if (!isBlockPlacedByGroup(player, blockLocation)) {
                        event.setCancelled(true);
                    }
                }
            }
        });

        protocolManager.addPacketListener(new PacketAdapter(plugin, PacketType.Play.Server.SPAWN_ENTITY) {
            @Override
            public void onPacketSending(PacketEvent event) {
                Player player = event.getPlayer();
                Entity entity = event.getPacket().getEntityModifier(event).read(0);

                // Hide entities placed by other groups
                if (!isEntityVisibleToPlayer(player, entity)) {
                    event.setCancelled(true);
                }
            }
        });
    }

    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event) {
        Player player = event.getPlayer();
        Material blockType = event.getBlock().getType();

        // Track water and lava blocks placed by players
        if (blockType == Material.WATER || blockType == Material.LAVA) {
            String group = playerGroups.get(player.getUniqueId());
            groupBlocks.computeIfAbsent(group, k -> new HashSet<>()).add(new BlockData(event.getBlock().getLocation().toVector(), blockType));
        }
    }

    private boolean isBlockPlacedByGroup(Player player, BlockData blockData) {
        String group = playerGroups.get(player.getUniqueId());
        Set<BlockData> blocks = groupBlocks.get(group);
        return blocks != null && blocks.contains(blockData);
    }

    private boolean isEntityVisibleToPlayer(Player player, Entity entity) {
        UUID playerId = player.getUniqueId();
        Set<UUID> hiddenEntitiesForPlayer = hiddenEntities.get(playerId);
        return hiddenEntitiesForPlayer == null || !hiddenEntitiesForPlayer.contains(entity.getUniqueId());
    }

    public void hideEntityForPlayer(Player player, Entity entity) {
        UUID playerId = player.getUniqueId();
        hiddenEntities.computeIfAbsent(playerId, k -> new HashSet<>()).add(entity.getUniqueId());
    }

    public void showEntityForPlayer(Player player, Entity entity) {
        UUID playerId = player.getUniqueId();
        Set<UUID> hiddenEntitiesForPlayer = hiddenEntities.get(playerId);
        if (hiddenEntitiesForPlayer != null) {
            hiddenEntitiesForPlayer.remove(entity.getUniqueId());
        }
    }

    public void assignPlayerToGroup(Player player, String group) {
        playerGroups.put(player.getUniqueId(), group);
    }

    private static class BlockData {
        private final Vector location;
        private final Material type;

        public BlockData(Vector location, Material type) {
            this.location = location;
            this.type = type;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) return true;
            if (obj == null || getClass() != obj.getClass()) return false;
            BlockData blockData = (BlockData) obj;
            return location.equals(blockData.location) && type == blockData.type;
        }

        @Override
        public int hashCode() {
            return Objects.hash(location, type);
        }
    }
}
