package net.avatar.realms.spigot.bending.commands.subcommands;

import java.util.List;

import org.bukkit.command.CommandSender;

import net.avatar.realms.spigot.bending.commands.BendingCommand;

public class PathExecution extends BendingCommand {

	public PathExecution() {
		super();
		this.command = "path";
		this.aliases.add("p");
	}

	@Override
	public boolean execute(CommandSender sender, List<String> args) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public void printUsage(CommandSender sender) {
		// TODO Auto-generated method stub

	}

}
