package net.moddingplayground.frame.mixin.toymaker;

import com.google.common.collect.Lists;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

import java.util.List;
import java.util.Set;

public class DataGenMixinPlugin implements IMixinConfigPlugin {
    @Override
    public void onLoad(String mixinPackage) {}

    @Override
    public String getRefMapperConfig() {
        return null;
    }

    @Override
    public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
        return true;
    }

    @Override
    public void acceptTargets(Set<String> myTargets, Set<String> otherTargets) {}

    @Override
    public List<String> getMixins() {
        // We need these here just to have them under a system property
        if (Boolean.parseBoolean(System.getProperty("toymaker.datagen"))) {
            return Lists.newArrayList(
                "DataGenMixin",
                "DataGeneratorMixin",

                "AbstractBlockMixin",
                "AdvancementBuilderMixin",
                "BlockLootTableGeneratorAccessor",
                "BlockLootTableGeneratorInvoker",
                "CookingRecipeJsonBuilderMixin",
                "EntityTypeMixin",
                "EulaReaderMixin",
                "RecipeProviderAccessor",
                "ShapedRecipeJsonBuilderMixin",
                "ShapelessRecipeJsonBuilderMixin",
                "SharedConstantsMixin",
                "SingleItemRecipeJsonBuilderAccessor",
                "SingleItemRecipeJsonBuilderMixin",
                "TagManagerLoaderAccessor"
            );
        }
        return null;
    }

    @Override
    public void preApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {}

    @Override
    public void postApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {}
}
