package com.github.devotedmc.hiddenore.listeners;
import org.bukkit.Material;
import org.bukkit.ChatColor;
import org.bukkit.block.Block;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPistonExtendEvent;
import org.bukkit.event.block.BlockPistonRetractEvent;
import org.bukkit.event.entity.EntityChangeBlockEvent;
import org.bukkit.inventory.ItemStack;

import java.util.logging.Level;

import com.github.devotedmc.hiddenore.BlockConfig;
import com.github.devotedmc.hiddenore.DropConfig;
import com.github.devotedmc.hiddenore.HiddenOre;
import com.github.devotedmc.hiddenore.Config;

public class BlockBreakListener implements Listener {

	/**
	 * On reflection I realized you could just push smoothstone into an unedited chunk layer
	 * and increase overall drops for a world. So to counter-balance, on each extension we
	 * track the location of the blocks and increment their Y counters.
	 * 
	 * @param event
	 */
	@EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
	public void onPistonExtend(BlockPistonExtendEvent event) {
		Block source = event.getBlock();
		Block extension = event.getBlock().getRelative(event.getDirection());
		for (Block b : event.getBlocks()) {
			Block next = b.getRelative(event.getDirection());
			if (next.equals(source) || next.equals(extension)) {
				continue;
			}
			HiddenOre.getTracking().trackBreak(next.getLocation());
		}
		HiddenOre.getTracking().trackBreak(source.getLocation());
		if (!source.equals(extension)) {
			HiddenOre.getTracking().trackBreak(extension.getLocation());
		}
	}
	
	/**
	 * Should cover even more clever implementations involving sticky pistons to game things.
	 * @param event
	 */
	@EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
	public void onPistonRetract(BlockPistonRetractEvent event) {
		if (!event.isSticky()) return; // only care about stick business.
		
		Block source = event.getBlock();
		debug("Piston event from {0}", source.getLocation());
		Block extension = event.getBlock().getRelative(event.getDirection());
		for (Block b : event.getBlocks()) {
			Block next = b.getRelative(event.getDirection());
			if (next.equals(source) || next.equals(extension)) {
				continue;
			}
			HiddenOre.getTracking().trackBreak(next.getLocation());
		}
		HiddenOre.getTracking().trackBreak(source.getLocation());
		if (!source.equals(extension)) {
			HiddenOre.getTracking().trackBreak(extension.getLocation());
		}
	}
	
	/**
	 * Prevent gaming by dropping sand/gravel/gravity blocks 
	 * @param event
	 */
	@EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
	public void onFallingThings(EntityChangeBlockEvent event) {
		Material from = event.getBlock().getType();
		if (!event.getBlock().isEmpty() && !event.getBlock().isLiquid()) {
			if (!from.hasGravity()) return;
			if (!from.isBlock()) return;
			// At this point we've confirmed that the FROM used to be a block, that is now falling.
			debug("Block about to fall from {0}, was a {1}", event.getBlock().getLocation(),
					from);
		} else {
			//if (event.getBlock().isEmpty() || event.getBlock().isLiquid()) {
			// At this point we've confirmed that the FROM is air or liquid e.g. this is a block
			// that is done falling and wants to be a block again.
			if (EntityType.FALLING_BLOCK != event.getEntityType()) return;
			debug("Block has fallen to {0}, was a {1}", event.getBlock().getLocation(),
					from);
		}
		// track a break at FROM and TO, so you can't game sand/gravel by dropping it into a chunk.
		HiddenOre.getTracking().trackBreak(event.getBlock().getLocation());
	}
	
    @SuppressWarnings("deprecation")
	@EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
    	if(!HiddenOre.getTracking().trackBreak(event.getBlock().getLocation())) {
    		debug("Drop skipped at {0} - layer break max met", event.getBlock().getLocation());
    		return;
    	}
    	
        Block b = event.getBlock();
        String blockName = b.getType().name();
        Byte sb = b.getData();
        BlockConfig bc = Config.isDropBlock(blockName, sb);
        
        if (bc == null) return;
        debug("Break of tracked type {0}", blockName);
        
        Player p = event.getPlayer();
        if(p.getItemInHand().getEnchantments().containsKey(Enchantment.SILK_TOUCH)) return;

		boolean hasDrop = false;

		StringBuffer alertUser = null;
		StringBuffer customAlerts = null;

    	String biomeName = b.getBiome().name();
        
    	if (bc.dropMultiple){
	    	for(String drop : bc.getDrops()) {
	    		DropConfig dc = bc.getDropConfig(drop);
	    		
	    		if (!dc.dropsWithTool(biomeName, p.getItemInHand().getType().name())) {
	    			debug("Cannot drop {0} - wrong tool", drop);
	    			continue;
	    		}
	    		
	    		if	(b.getLocation().getBlockY() > dc.getMaxY(biomeName) || 
	    				b.getLocation().getBlockY() < dc.getMinY(biomeName)) {
	    			debug("Cannot drop {0} - wrong Y", drop);
	    			continue;
	    		}
	    		
	    		double dropChance = dc.getChance(biomeName);
	    		
	    		//Random check to decide whether or not the special drop should be dropped
	    		if(dropChance > Math.random()) {
	    			//Remove block, drop special drop and cancel the event
	    			b.setType(Material.AIR);
	    			ItemStack item = dc.renderDrop(drop, biomeName);
	    			b.getWorld().dropItem(b.getLocation(), item);
	    			event.setCancelled(true);
					log("For {5} at {6} replacing {0}:{1} with {2} {3} at dura {4}", blockName, sb, 
							item.getAmount(), drop, item.getDurability(), p.getDisplayName(), p.getLocation());
					if (Config.isAlertUser()) {
						if (alertUser == null) {
							alertUser = new StringBuffer().append(Config.instance.defaultPrefix);
						}
						if (bc.hasCustomPrefix(drop)) {
							customAlerts = new StringBuffer();
							customAlerts.append(bc.getPrefix(drop))
									.append(" ").append(item.getAmount()).append(" ").append(Config.getPrettyName(drop, item.getDurability()));
							event.getPlayer().sendMessage(ChatColor.GOLD + customAlerts.toString());
							customAlerts = null;
						} else{
							if (Config.isListDrops()) {
								alertUser.append(" ").append(item.getAmount()).append(" ").append(Config.getPrettyName(drop, item.getDurability())).append(",");
							}
							hasDrop = true;
						}
					}
	    		}
	    	}
    	} else {
    		String drop = bc.getDropConfig(Math.random(), biomeName, 
    				p.getItemInHand().getType().name(), b.getLocation().getBlockY());
    		
    		if (drop != null) {
        		DropConfig dc = bc.getDropConfig(drop);
    			//Remove block, drop special drop and cancel the event
    			b.setType(Material.AIR);
    			ItemStack item = dc.renderDrop(drop, biomeName);
    			b.getWorld().dropItem(b.getLocation(), item);
    			event.setCancelled(true);
    			log("For {5} at {6} replacing {0}:{1} with {2} {3} at dura {4}", blockName, sb, 
						item.getAmount(), drop, item.getDurability(), p.getDisplayName(), p.getLocation());
				if (Config.isAlertUser()) {
					if (alertUser == null) {
						alertUser = new StringBuffer().append(Config.instance.defaultPrefix);
					}
					if (bc.hasCustomPrefix(drop)) {
						customAlerts = new StringBuffer();
						customAlerts.append(bc.getPrefix(drop))
								.append(" ").append(item.getAmount()).append(" ").append(Config.getPrettyName(drop, item.getDurability()));
						event.getPlayer().sendMessage(ChatColor.GOLD + customAlerts.toString());
						customAlerts = null;
					} else{
						if (Config.isListDrops()) {
							alertUser.append(" ").append(item.getAmount()).append(" ").append(Config.getPrettyName(drop, item.getDurability())).append(",");
						}
						hasDrop = true;
					}
				}
    		}
    	}
		if (Config.isAlertUser() && hasDrop) {
			if (Config.isListDrops()) {
				alertUser.deleteCharAt(alertUser.length() - 1);
			}
			
			event.getPlayer().sendMessage(ChatColor.GOLD + alertUser.toString());
		}
    }

	private void log(String message, Object...replace) {
		HiddenOre.getPlugin().getLogger().log(Level.INFO, message, replace);
	}
	
	private void debug(String message, Object...replace) {
		if (Config.isDebug) {
			HiddenOre.getPlugin().getLogger().log(Level.INFO, message, replace);
		}
	}
}
