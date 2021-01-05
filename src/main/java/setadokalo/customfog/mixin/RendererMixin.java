package setadokalo.customfog.mixin;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import setadokalo.customfog.CustomFog;
import setadokalo.customfog.CustomFogConfig;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.render.BackgroundRenderer;
import net.minecraft.client.render.Camera;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.fluid.FluidState;
import net.minecraft.tag.FluidTags;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;


@Mixin(BackgroundRenderer.class)
// This mod shouldn't even be installed on a server but w/e
@Environment(EnvType.CLIENT)
public class RendererMixin {
	@Inject(method = "applyFog", at=@At(value = "INVOKE", target = "com/mojang/blaze3d/systems/RenderSystem.setupNvFogDistance()V"), locals = LocalCapture.CAPTURE_FAILSOFT)
	private static void setFogFalloff(Camera camera, BackgroundRenderer.FogType fogType, float viewDistance, boolean thickFog, CallbackInfo ci, FluidState fluidState, Entity entity) {
		if (! (fluidState.isIn(FluidTags.LAVA)) || (entity instanceof LivingEntity && ((LivingEntity)entity).hasStatusEffect(StatusEffects.BLINDNESS))) {
			// For each dimension in the list of white or blacklisted dimensions, we check if the current dimension matches it
			boolean doContinue = CustomFog.config.listMode == CustomFogConfig.ListMode.BLACKLIST;
			for (String dimension: CustomFog.config.dimensionsList) {
				if (dimension.equals(entity.getEntityWorld().getRegistryKey().getValue().toString())) {
					// if it does match, we either break from the loop if in whitelist mode
					if (CustomFog.config.listMode == CustomFogConfig.ListMode.WHITELIST) {
						doContinue = true;
						break;
					} else {
						return;
					}
				}
			}
			if (!doContinue) {
				return;
			}
			changeFalloff(viewDistance);
		}
	}

	private static void changeFalloff(float viewDistance) {
		if (CustomFog.config.fogType == CustomFogConfig.FogType.LINEAR) {
			RenderSystem.fogStart(viewDistance * CustomFog.config.linearFogStartMultiplier);
			RenderSystem.fogEnd(viewDistance * CustomFog.config.linearFogEndMultiplier);
			RenderSystem.fogMode(GlStateManager.FogMode.LINEAR);
		}
		else if (CustomFog.config.fogType == CustomFogConfig.FogType.EXPONENTIAL) {
			RenderSystem.fogDensity(CustomFog.config.expFogMultiplier / viewDistance);
			RenderSystem.fogMode(GlStateManager.FogMode.EXP);
		} else if (CustomFog.config.fogType == CustomFogConfig.FogType.EXPONENTIAL_TWO) {
			RenderSystem.fogDensity(CustomFog.config.exp2FogMultiplier / viewDistance);
			RenderSystem.fogMode(GlStateManager.FogMode.EXP2);
		}
	}
}
