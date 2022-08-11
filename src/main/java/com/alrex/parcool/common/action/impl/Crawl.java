package com.alrex.parcool.common.action.impl;

import com.alrex.parcool.client.input.KeyBindings;
import com.alrex.parcool.client.input.KeyRecorder;
import com.alrex.parcool.common.action.Action;
import com.alrex.parcool.common.capability.Parkourability;
import com.alrex.parcool.common.capability.Stamina;
import com.alrex.parcool.utilities.BufferUtil;
import com.alrex.parcool.utilities.EntityUtil;
import com.alrex.parcool.utilities.VectorUtil;
import net.minecraft.entity.Pose;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.event.TickEvent;

import java.nio.ByteBuffer;

public class Crawl extends Action {
	private int slidingTick = 0;
	private Vector3d slidingVec = null;
	private boolean crawling = false;
	private boolean sliding = false;

	@Override
	public void onTick(PlayerEntity player, Parkourability parkourability, Stamina stamina) {
		if (sliding) slidingTick++;
		else slidingTick = 0;
		if (crawling || sliding) {
			player.setSprinting(false);
			player.setPose(Pose.SWIMMING);
		}
	}

	@OnlyIn(Dist.CLIENT)
	@Override
	public void onClientTick(PlayerEntity player, Parkourability parkourability, Stamina stamina) {
		if (player.isLocalPlayer()) {
			if (
					parkourability.getPermission().canCrawl()
							&& !crawling
							&& KeyRecorder.keyCrawlState.isPressed()
							&& !parkourability.getRoll().isRolling()
							&& !player.isInWaterOrBubble()
							&& player.isOnGround()
			) {
				//sliding
				if (parkourability.getFastRun().getDashTick(parkourability.getAdditionalProperties()) > 5) {
					sliding = true;
					Vector3d lookVec = player.getLookAngle();
					slidingVec = new Vector3d(lookVec.x(), 0, lookVec.z()).normalize();
				}
				//crawl
				else {
					sliding = false;
					slidingVec = null;
				}
				crawling = true;
			}
			if (slidingVec == null) {
				sliding = false;
			}
			if (sliding) {
				if (player.isOnGround()) {
					Vector3d vec = slidingVec.scale(0.2);
					EntityUtil.addVelocity(player, vec);
				}
			}
			if (slidingTick >= parkourability.getActionInfo().getMaxSlidingTick()) {
				sliding = false;
				slidingTick = 0;
				crawling = true;
				slidingVec = null;
			}
			if (crawling && !KeyBindings.getKeyCrawl().isDown()) {
				crawling = false;
				sliding = false;
				slidingVec = null;
			}
		}
	}

	@Override
	public void onRender(TickEvent.RenderTickEvent event, PlayerEntity player, Parkourability parkourability) {
		if (slidingVec == null || !sliding) return;
		player.yRot = (float) VectorUtil.toYawDegree(slidingVec);
	}

	@Override
	public void restoreState(ByteBuffer buffer) {
		crawling = BufferUtil.getBoolean(buffer);
		sliding = BufferUtil.getBoolean(buffer);
	}

	@Override
	public void saveState(ByteBuffer buffer) {
		BufferUtil.wrap(buffer)
				.putBoolean(crawling)
				.putBoolean(sliding);
	}

	public boolean isCrawling() {
		return crawling;
	}

	public boolean isSliding() {
		return sliding;
	}
}
