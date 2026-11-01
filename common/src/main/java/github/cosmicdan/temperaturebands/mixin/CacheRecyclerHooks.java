package github.cosmicdan.temperaturebands.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import github.cosmicdan.temperaturebands.util.CacheRecylerHooksUtil;
import net.minecraft.core.Holder;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.biome.Climate;
import net.minecraft.world.level.biome.MultiNoiseBiomeSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;

public class CacheRecyclerHooks {
    @Mixin(MultiNoiseBiomeSource.class)
    public static abstract class MultiNoiseBiomeSourceHooks extends BiomeSource {
        @Shadow
        public abstract Holder<Biome> getNoiseBiome(Climate.TargetPoint targetPoint);

        @Unique
        private static final ThreadLocal<CacheRecylerHooksUtil.ThreadState> tb_$THREAD_CACHE = ThreadLocal.withInitial(CacheRecylerHooksUtil.ThreadState::new);

        @WrapMethod(
                method = "getNoiseBiome(IIILnet/minecraft/world/level/biome/Climate$Sampler;)Lnet/minecraft/core/Holder;"
        )
        public Holder<Biome> onGetNoiseBiomeWithSampler(int i, int j, int k, Climate.Sampler sampler, Operation<Holder<Biome>> original) {
            CacheRecylerHooksUtil.ThreadState stateData = tb_$THREAD_CACHE.get();
            CacheRecylerHooksUtil.initIfNeeded(stateData, sampler, this);
            if (stateData.doCacheRecycle) {
                Climate.TargetPoint result = CacheRecylerHooksUtil.onGetNoiseBiomeWithSampler(stateData, i, j, k, sampler);
                if (result != null) // Only happens if the cache recycler is disabled, or the cache isn't ready yet
                    return getNoiseBiome(result);
            }
            return original.call(i, j, k, sampler);
        }
    }
}
