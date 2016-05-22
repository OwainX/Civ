package vg.civcraft.mc.civmodcore.chatDialog;

import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;

import org.bukkit.entity.Player;

public class DialogManager {
	private static Map <UUID, Dialog> dialogs;
	public static DialogManager instance;
	
	public DialogManager() {
		instance = this;
		dialogs = new TreeMap<UUID, Dialog>();
	}
	
	public Dialog getDialog(Player p) {
		return getDialog(p.getUniqueId());
	}
	
	public Dialog getDialog(UUID uuid) {
		return dialogs.get(uuid);
	}
	
	public void registerDialog(Player p, Dialog dialog) {
		dialogs.put(p.getUniqueId(), dialog);
	}
	
	public void forceEndDialog(Player p) {
		forceEndDialog(p.getUniqueId());
	}
	
	public void forceEndDialog(UUID uuid) {
		Dialog dia = dialogs.get(uuid);
		if (dia != null) {
			dialogs.remove(uuid);
			dia.end();
		}
	}
}
