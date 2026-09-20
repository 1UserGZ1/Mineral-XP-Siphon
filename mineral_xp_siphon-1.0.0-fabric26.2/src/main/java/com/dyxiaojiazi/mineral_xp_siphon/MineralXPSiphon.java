package com.dyxiaojiazi.mineral_xp_siphon;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.minecraft.core.Holder;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

public class MineralXPSiphon implements ModInitializer {
	public static final String MOD_ID = "mineral_xp_siphon";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	// 矿物 → 经验值映射
	public static final Map<Item, Integer> XP_MAP = Map.of(
			Items.REDSTONE, 20,
			Items.LAPIS_LAZULI, 30,
			Items.QUARTZ, 40,
			Items.EMERALD, 80,
			Items.DIAMOND, 120,
			Items.AMETHYST_SHARD, 150
	);

	// 中毒概率与时长：10% 概率，5 秒 = 100 tick
	private static final float POISON_CHANCE = 0.10f;
	private static final int POISON_DURATION = 100;

	// 反胃概率与时长：20% 概率，5 秒 = 100 tick
	private static final float NAUSEA_CHANCE = 0.20f;
	private static final int NAUSEA_DURATION = 100;

	@Override
	public void onInitialize() {
		LOGGER.info("[Mineral XP Siphon] 矿物经验汲取模组加载完成！");

		UseItemCallback.EVENT.register((Player player, Level level, InteractionHand hand) -> {
			// 只处理主手
			if (hand != InteractionHand.MAIN_HAND) {
				return InteractionResult.PASS;
			}

			ItemStack stack = player.getItemInHand(hand);
			Item item = stack.getItem();

			// 非矿物 → 不拦截
			Integer xpValue = XP_MAP.get(item);
			if (xpValue == null) {
				return InteractionResult.PASS;
			}

			// 客户端：返回成功以播放音效和动画
			if (level.isClientSide()) {
				return InteractionResult.SUCCESS;
			}

			// ===== 服务端逻辑 =====
			// 1. 消耗物品
			stack.shrink(1);

			// 2. 给予经验
			player.giveExperiencePoints(xpValue);

			// 3. 10% 概率中毒 5 秒（可叠加时长）
			if (player.getRandom().nextFloat() < POISON_CHANCE) {
				applyStackableEffect(player, MobEffects.POISON, POISON_DURATION);
			}

			// 4. 20% 概率反胃 5 秒（可叠加时长）
			if (player.getRandom().nextFloat() < NAUSEA_CHANCE) {
				applyStackableEffect(player, MobEffects.NAUSEA, NAUSEA_DURATION);
			}

			// 5. 食用音效
			level.playSound(null, player.getX(), player.getY(), player.getZ(),
					SoundEvents.PLAYER_BURP, SoundSource.PLAYERS,
					0.5F, 1.0F);

			return InteractionResult.SUCCESS;
		});
	}

	/**
	 * 施加可叠加时长的效果。
	 * 若玩家已拥有该效果，则新效果时长为「当前剩余时间 + 新增时长」，
	 * 等级取两者中较高者。
	 */
	private static void applyStackableEffect(Player player, Holder<MobEffect> effect, int additionalDuration) {
		MobEffectInstance existing = player.getEffect(effect);
		if (existing != null) {
			int newDuration = existing.getDuration() + additionalDuration;
			int amplifier = existing.getAmplifier();
			player.addEffect(new MobEffectInstance(effect, newDuration, amplifier));
		} else {
			player.addEffect(new MobEffectInstance(effect, additionalDuration, 0));
		}
	}
}