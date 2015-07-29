package net.avatar.realms.spigot.bending.abilities.water;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import net.avatar.realms.spigot.bending.abilities.Abilities;
import net.avatar.realms.spigot.bending.abilities.BendingAbility;
import net.avatar.realms.spigot.bending.abilities.BendingPlayer;
import net.avatar.realms.spigot.bending.abilities.BendingType;
import net.avatar.realms.spigot.bending.abilities.IAbility;
import net.avatar.realms.spigot.bending.abilities.TempBlock;
import net.avatar.realms.spigot.bending.controller.ConfigManager;
import net.avatar.realms.spigot.bending.controller.Flight;
import net.avatar.realms.spigot.bending.utils.BlockTools;
import net.avatar.realms.spigot.bending.utils.EntityTools;
import net.avatar.realms.spigot.bending.utils.PluginTools;
import net.avatar.realms.spigot.bending.utils.ProtectionManager;
import net.avatar.realms.spigot.bending.utils.Tools;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffectType;

@BendingAbility(name="Water Spout", element=BendingType.Water)
public class WaterSpout implements IAbility {
	public static int SPEED = ConfigManager.waterSpoutRotationSpeed;
	private static Map<Player, WaterSpout> instances = new HashMap<Player, WaterSpout>();
	private static List<Block> affectedblocks = new LinkedList<Block>();
	private static List<Block> newaffectedblocks = new LinkedList<Block>();
	private static final int defaultheight = ConfigManager.waterSpoutHeight;

	private static final byte full = 0x0;
	private int currentCardinalPoint = 0;
	private Player player;
	private Block base;
	private TempBlock baseblock;
	private IAbility parent;

	public WaterSpout(Player player, IAbility parent) {
		this.parent = parent;
		if (BendingPlayer.getBendingPlayer(player).isOnCooldown(
				Abilities.WaterSpout))
			return;

		if (instances.containsKey(player)) {
			instances.get(player).remove();
			return;
		}
		this.player = player;
		if (canWaterSpout(player)) {
			new Flight(player);
			player.setAllowFlight(true);
			instances.put(player, this);
			spout();
		}

	}

	private void remove() {
		revertBaseBlock(player);
		instances.remove(player);
	}

	public static void progressAll() {
		newaffectedblocks.clear();

		List<WaterSpout> toRemoveSpout = new LinkedList<WaterSpout>();
		for (Entry<Player, WaterSpout> entry : instances.entrySet()) {
			Player player = entry.getKey();
			WaterSpout spout = entry.getValue();
			if (!player.isOnline() || player.isDead()) {
				toRemoveSpout.add(spout);
			} else if (EntityTools.hasAbility(player, Abilities.WaterSpout)
					&& EntityTools.canBend(player, Abilities.WaterSpout)) {
				boolean keep = spout.spout();
				if (!keep) {
					toRemoveSpout.add(spout);
				}
			} else {
				toRemoveSpout.add(spout);
			}
		}
		for (WaterSpout spout : toRemoveSpout) {
			spout.remove();
		}

		List<Block> toRemoveBlock = new LinkedList<Block>();
		for (Block block : affectedblocks) {
			if (!newaffectedblocks.contains(block)) {
				toRemoveBlock.add(block);
			}
		}
		for (Block block : toRemoveBlock) {
			affectedblocks.remove(block);
			TempBlock.revertBlock(block);
		}
	}

	private boolean spout() {
		player.setFallDistance(0);
		player.setSprinting(false);

		player.removePotionEffect(PotionEffectType.SPEED);
		Location location = player.getLocation().clone().add(0, .2, 0);
		Block block = location.clone().getBlock();
		int height = spoutableWaterHeight(location, player);

		// Tools.verbose(height + " " + WaterSpout.height + " "
		// + affectedblocks.size());
		if (height != -1) {
			location = base.getLocation();
			for (int i = 1, cardinalPoint = (int) (currentCardinalPoint / SPEED); i <= height; i++, cardinalPoint++) {
				if (cardinalPoint == 8) {
					cardinalPoint = 0;
				}

				block = location.clone().add(0, i, 0).getBlock();
				if (!TempBlock.isTempBlock(block)) {
					new TempBlock(block, Material.WATER, full);
				}
				if (!affectedblocks.contains(block)) {
					affectedblocks.add(block);
				}
				newaffectedblocks.add(block);

				switch (cardinalPoint) {
				case 0:
					block = location.clone().add(0, i, -1).getBlock();
					break;
				case 1:
					block = location.clone().add(-1, i, -1).getBlock();
					break;
				case 2:
					block = location.clone().add(-1, i, 0).getBlock();
					break;
				case 3:
					block = location.clone().add(-1, i, 1).getBlock();
					break;
				case 4:
					block = location.clone().add(0, i, 1).getBlock();
					break;
				case 5:
					block = location.clone().add(1, i, 1).getBlock();
					break;
				case 6:
					block = location.clone().add(1, i, 0).getBlock();
					break;
				case 7:
					block = location.clone().add(1, i, -1).getBlock();
					break;
				default:
					break;
				}

				if (block.getType().equals(Material.AIR)
						|| affectedblocks.contains(block)) {

					if (!TempBlock.isTempBlock(block)) {
						new TempBlock(block, Material.WATER, full);
					}
					if (!affectedblocks.contains(block)) {
						affectedblocks.add(block);
					}
					newaffectedblocks.add(block);
				}
			}
			currentCardinalPoint++;
			if (currentCardinalPoint == SPEED * 8) {
				currentCardinalPoint = 0;
			}

			if (player.getLocation().getBlockY() > block.getY()) {
				player.setFlying(false);
			} else {
				new Flight(player);
				player.setAllowFlight(true);
				player.setFlying(true);
			}

		} else {
			return false;
		}
		return true;
	}

	private static int spoutableWaterHeight(Location location, Player player) {
		WaterSpout spout = instances.get(player);
		int height = defaultheight;
		if (Tools.isNight(player.getWorld()))
			height = (int) PluginTools.waterbendingNightAugment(
					(double) height, player.getWorld());
		int maxheight = (int) ((double) defaultheight * ConfigManager.nightFactor) + 5;
		Block blocki;
		for (int i = 0; i < maxheight; i++) {
			blocki = location.clone().add(0, -i, 0).getBlock();
			if (ProtectionManager.isRegionProtectedFromBending(player, Abilities.WaterSpout,
					blocki.getLocation()))
				return -1;
			if (!affectedblocks.contains(blocki)) {
				if (blocki.getType() == Material.WATER
						|| blocki.getType() == Material.STATIONARY_WATER) {
					if (!TempBlock.isTempBlock(blocki)) {
						revertBaseBlock(player);
					}
					spout.base = blocki;
					if (i > height)
						return height;
					return i;
				}
				if (blocki.getType() == Material.ICE
						|| blocki.getType() == Material.SNOW
						|| blocki.getType() == Material.SNOW_BLOCK) {
					if (!TempBlock.isTempBlock(blocki)) {
						revertBaseBlock(player);
						instances.get(player).baseblock = new TempBlock(blocki, Material.WATER, full);
					}
					spout.base = blocki;
					if (i > height)
						return height;
					return i;
				}
				if ((blocki.getType() != Material.AIR && (!BlockTools
						.isPlant(blocki) || !EntityTools.canPlantbend(player)))) {
					revertBaseBlock(player);
					return -1;
				}
			}
		}
		revertBaseBlock(player);
		return -1;
	}

	public static void revertBaseBlock(Player player) {
		if (instances.containsKey(player)) {
			if (instances.get(player).baseblock != null) {
				instances.get(player).baseblock.revertBlock();
				instances.get(player).baseblock = null;
			}
		}
	}

	public static void removeAll() {
		instances.clear();
		for (Block block : affectedblocks) {
			TempBlock.revertBlock(block);
		}
		affectedblocks.clear();
	}

	public static List<Player> getPlayers() {
		return new LinkedList<Player>(instances.keySet());
	}

	public static void removeSpouts(Location loc0, double radius,
			Player sourceplayer) {
		List<WaterSpout> toRemove = new LinkedList<WaterSpout>();
		for (Player player : instances.keySet()) {
			if (!player.equals(sourceplayer)) {
				Location loc1 = player.getLocation().getBlock().getLocation();
				loc0 = loc0.getBlock().getLocation();
				double dx = loc1.getX() - loc0.getX();
				double dy = loc1.getY() - loc0.getY();
				double dz = loc1.getZ() - loc0.getZ();

				double distance = Math.sqrt(dx * dx + dz * dz);

				if (distance <= radius && dy > 0 && dy < defaultheight)
					toRemove.add(instances.get(player));
			}
		}
		for (WaterSpout spout : toRemove) {
			spout.remove();
		}
	}

	public static boolean isBending(Player player) {
		return instances.containsKey(player);
	}

	public static boolean isAffected(Block block) {
		return affectedblocks.contains(block);
	}

	public static boolean canWaterSpout(Player player) {
		Location loc = player.getLocation();
		if (BlockTools.isWaterBased(loc.getBlock())){
			return true;
		}
		while (loc.getBlock().getType() == Material.AIR && loc.getBlockY() > 0) {
			loc = loc.add(0, -1, 0);
			if (BlockTools.isWaterBased(loc.getBlock())) {
				return true;
			}
		}
		return false;
	}

	@Override
	public IAbility getParent() {
		return parent;
	}
}
