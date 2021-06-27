package vg.civcraft.mc.namelayer.command.commands;

import co.aikar.commands.annotation.CommandAlias;
import co.aikar.commands.annotation.Description;
import co.aikar.commands.annotation.Syntax;
import java.util.List;
import java.util.UUID;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import vg.civcraft.mc.namelayer.NameAPI;
import vg.civcraft.mc.namelayer.command.BaseCommandMiddle;
import vg.civcraft.mc.namelayer.command.TabCompleters.GroupTabCompleter;

@CommandAlias("nlgdg")
public class GetDefaultGroup extends BaseCommandMiddle {

	@Syntax("/nlgdg")
	@Description("Get a players default group")
	public void execute(CommandSender sender) {
		if (!(sender instanceof Player)){
			sender.sendMessage("I don't think you need to do that.");
			return;
		}
		Player p = (Player) sender;
		UUID uuid = NameAPI.getUUID(p.getName());

		String x = gm.getDefaultGroup(uuid);
		if(x == null){
			p.sendMessage(ChatColor.RED + "You do not currently have a default group use /nlsdg to set it");
		}
		else{
			p.sendMessage(ChatColor.GREEN + "Your current default group is " + x);
		}
	}

	public List<String> tabComplete(CommandSender sender, String[] args) {
		if (!(sender instanceof Player))
			return null;

		if (args.length == 1)
			return GroupTabCompleter.complete(args[0], null, (Player) sender);
		else{
			return GroupTabCompleter.complete(null, null, (Player)sender);
		}
	}
}
