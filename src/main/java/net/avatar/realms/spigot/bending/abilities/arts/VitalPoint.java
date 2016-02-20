package net.avatar.realms.spigot.bending.abilities.arts;

import java.util.Map;

import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import net.avatar.realms.spigot.bending.abilities.AbilityManager;
import net.avatar.realms.spigot.bending.abilities.BendingAbility;
import net.avatar.realms.spigot.bending.abilities.ABendingAbility;
import net.avatar.realms.spigot.bending.abilities.BendingAbilityState;
import net.avatar.realms.spigot.bending.abilities.BendingActiveAbility;
import net.avatar.realms.spigot.bending.abilities.BendingAffinity;
import net.avatar.realms.spigot.bending.abilities.BendingPath;
import net.avatar.realms.spigot.bending.abilities.RegisteredAbility;
import net.avatar.realms.spigot.bending.controller.ConfigurationParameter;
import net.avatar.realms.spigot.bending.utils.EntityTools;

/**
 *
 * This ability will be modified : When you hit an entity, you deal a small
 * amount of damage to it and it gets slown. The more you hit it, the more it
 * get slown.
 *
 */
@ABendingAbility(name = VitalPoint.NAME, affinity=BendingAffinity.Chi, shift=false)
public class VitalPoint extends BendingActiveAbility {
	public final static String NAME = "VitalPoint";

	@ConfigurationParameter("Damage")
	private static int DAMAGE = 1;

	@ConfigurationParameter("Damage-Increment")
	private static int DAMAGE_INCREMENT = 1;

	@ConfigurationParameter("Cooldown")
	private static long COOLDOWN = 10000;

	@ConfigurationParameter("Duration")
	private static long DURATION = 2500;

	@ConfigurationParameter("Slown-Duration")
	private static int SLOW_DURATION = 5; // In seconds

	@ConfigurationParameter("Chiblock-Duration")
	private static long CHIBLOCK_DURATION = 3000;

	@ConfigurationParameter("Max-Range")
	private static float MAX_RANGE = 3.5f;

	private static final PotionEffectType TYPE = PotionEffectType.SLOW;

	private int damage;
	private long cooldown;
	private LivingEntity target;
	private int amplifier;

	public VitalPoint(RegisteredAbility register, Player player) {
		super(register, player);

		this.amplifier = 0;
		this.damage = DAMAGE;
		this.cooldown = COOLDOWN;
		
		if(this.bender.hasPath(BendingPath.Seeker)) {
			this.damage *= 1.5;
		}
		if(this.bender.hasPath(BendingPath.Restless)) {
			this.damage *= 0.6;
		}
	}

	@Override
	public boolean swing() {
		if(getState() == BendingAbilityState.Start) {
			this.target = EntityTools.getTargetedEntity(this.player, MAX_RANGE);
			if (this.target == null) {
				return false;
			}

			this.cooldown = 1000;
			if (this.player.isSneaking()) {
				this.damage += DAMAGE_INCREMENT;
				this.target.damage(this.damage, this.player);
				if (this.target instanceof Player) {
					EntityTools.blockChi((Player) this.target, CHIBLOCK_DURATION);
				}
				this.target.addPotionEffect(new PotionEffect(TYPE, (int) (DURATION / 20), 130));
				this.cooldown += COOLDOWN / (6);
			}
			else {
				this.target.damage(this.damage, this.player);
				this.target.addPotionEffect(new PotionEffect(TYPE, (int) (DURATION / 20), this.amplifier));
			}

			if (this.bender.hasPath(BendingPath.Seeker)) {
				this.cooldown *= 1.4;
			}
			if (this.bender.hasPath(BendingPath.Restless)) {
				this.cooldown *= 0.5;
			}
			this.bender.cooldown(NAME, this.cooldown);
		}
		return true;
	}

	@Override
	public boolean canBeInitialized() {
		if (!super.canBeInitialized()) {
			return false;
		}

		if (EntityTools.isTool(this.player.getItemInHand().getType())) {
			return false;
		}

		Map<Object, BendingAbility> instances = AbilityManager.getManager().getInstances(NAME);

		if ((instances == null) || instances.isEmpty()) {
			return true;
		}

		return !instances.containsKey(this.player);
	}

	@Override
	protected long getMaxMillis() {
		return 2000;
	}

	@Override
	public Object getIdentifier() {
		return this.player;
	}

	@Override
	public void progress() {
		
	}

	@Override
	public void stop() {
		
	}
}
