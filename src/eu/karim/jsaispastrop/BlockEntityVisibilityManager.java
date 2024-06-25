package eu.karim.jsaispastrop;

import com.comphenix.protocol.wrappers.BlockPosition;
import net.minecraft.server.v1_8_R3.Block;
import net.minecraft.server.v1_8_R3.PacketPlayOutBlockChange;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.craftbukkit.v1_8_R3.entity.CraftPlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockFromToEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.Vector;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.HashSet;

public class BlockEntityVisibilityManager extends JavaPlugin implements Listener {
    private final Map<UUID, Set<Vector>> playerBlocks = new HashMap<>(); // Store blocks placed by each player

    @Override
    public void onEnable() {
        // Register event listener
        Bukkit.getPluginManager().registerEvents(this, this);
    }

    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event) {
        Player player = event.getPlayer();
        Material blockType = event.getBlock().getType();

        // Track water and lava blocks placed by players
        if (blockType == Material.WATER || blockType == Material.LAVA) {
            UUID playerId = player.getUniqueId();
            playerBlocks.computeIfAbsent(playerId, k -> new HashSet<>()).add(event.getBlock().getLocation().toVector());

            // Hide the block for other players
            hideBlockForOtherPlayers(player, event.getBlock().getLocation().toVector(), blockType);
        }
    }

    @EventHandler
    public void onBlockFromTo(BlockFromToEvent event) {
        Material blockType = event.getBlock().getType();

        // Track flowing water and lava
        if (blockType == Material.WATER || blockType == Material.LAVA) {
            Vector from = event.getBlock().getLocation().toVector();
            Vector to = event.getToBlock().getLocation().toVector();

            // Hide the flowing block for other players
            hideFlowingBlockForOtherPlayers(from, to, blockType);
        }
    }

    private void hideBlockForOtherPlayers(Player placingPlayer, Vector blockPosition, Material blockType) {
        UUID placingPlayerId = placingPlayer.getUniqueId();

        for (Player player : Bukkit.getOnlinePlayers()) {
            UUID playerId = player.getUniqueId();

            if (!placingPlayerId.equals(playerId)) {
                sendBlockChange(player, blockPosition, Material.AIR);
            }
        }
    }

    private void hideFlowingBlockForOtherPlayers(Vector from, Vector to, Material blockType) {
        for (Player player : Bukkit.getOnlinePlayers()) {
            UUID playerId = player.getUniqueId();

            // Hide the original block if not placed by this player
            if (!isBlockPlacedByPlayer(playerId, from)) {
                sendBlockChange(player, from, Material.AIR);
            }

            // Hide the flowing block if not placed by this player
            if (!isBlockPlacedByPlayer(playerId, to)) {
                sendBlockChange(player, to, Material.AIR);
            }
        }
    }

    private boolean isBlockPlacedByPlayer(UUID playerId, Vector blockPosition) {
        Set<Vector> blocks = playerBlocks.get(playerId);
        return blocks != null && blocks.contains(blockPosition);
    }

    private void sendBlockChange(Player player, Vector blockPosition, Material newMaterial) {
        BlockPosition bp = new BlockPosition(blockPosition.getBlockX(), blockPosition.getBlockY(), blockPosition.getBlockZ());
        Block newBlock = (Block) Block.getByCombinedId(newMaterial.getId());
        PacketPlayOutBlockChange packet = new PacketPlayOutBlockChange();
        packet.block = newBlock.getBlockData();

        ((CraftPlayer) player).getHandle().playerConnection.sendPacket(packet);
    }

    public void addPlayerToGroup(Player player) {
        UUID playerId = player.getUniqueId();
        playerBlocks.computeIfAbsent(playerId, k -> new HashSet<>());
    }

    public void removePlayerFromGroup(Player player) {
        UUID playerId = player.getUniqueId();
        playerBlocks.remove(playerId);
    }
}