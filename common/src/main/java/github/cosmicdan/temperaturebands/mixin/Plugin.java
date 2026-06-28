package github.cosmicdan.temperaturebands.mixin;

import com.llamalad7.mixinextras.MixinExtrasBootstrap;
import github.cosmicdan.temperaturebands.TemperatureBands;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

import java.util.List;
import java.util.Set;

public class Plugin implements IMixinConfigPlugin {
    private static boolean doneLogMsg = false;

    @Override
    public void onLoad(String s) {
        // This might be necessary for some setups (e.g. Forge 1.18.2), in any case it's best to be sure
        MixinExtrasBootstrap.init();
    }

    @Override
    public String getRefMapperConfig() { return null; }

    @Override
    public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
        if (targetClassName.startsWith("com.caeruleusTait.world.preview.")) {
            try {
                Class.forName(targetClassName);
            } catch (ClassNotFoundException ignored) {
                return false; // World Preview not installed
            }
        }
        return true;
    }

    @Override
    public void acceptTargets(Set<String> myTargets, Set<String> otherTargets) {}

    @Override
    public List<String> getMixins() { return null; }

    @Override
    public void preApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {}

    @Override
    public void postApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {}
}
