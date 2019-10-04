package com.untamedears.JukeAlert.model.field;

import java.util.Collection;

import org.bukkit.Location;

import com.google.common.collect.Lists;
import com.untamedears.JukeAlert.model.Snitch;
import com.untamedears.JukeAlert.model.SnitchQTEntry;

public class SingleCuboidRangeManager implements FieldManager {

	private Snitch snitch;
	private SnitchQTEntry entry;
	private int range;

	public SingleCuboidRangeManager(int range, Snitch snitch) {
		entry = new SnitchQTEntry(snitch, snitch.getLocation(), range);
		this.snitch = snitch;
	}

	@Override
	public boolean isInside(Location location) {
		int y = snitch.getLocation().getBlockY();
		 return location.getBlockY() <= (y + range) && location.getBlockY() >= (y - range);
	}

	@Override
	public Collection<SnitchQTEntry> getQTEntries() {
		return Lists.asList(entry, new SnitchQTEntry[0]);
	}

}
