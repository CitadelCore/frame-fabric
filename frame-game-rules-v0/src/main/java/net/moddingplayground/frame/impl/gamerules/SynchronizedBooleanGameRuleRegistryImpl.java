package net.moddingplayground.frame.impl.gamerules;

import com.google.common.collect.Maps;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.PlayerLookup;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import net.minecraft.util.Util;
import net.minecraft.world.World;
import net.moddingplayground.frame.api.gamerules.v0.SynchronizedBooleanGameRuleRegistry;
import net.moddingplayground.frame.api.util.ReverseMemoizeFunction;
import net.moddingplayground.frame.mixin.gamerules.GameRulesAccessor;
import net.moddingplayground.frame.mixin.gamerules.GameRulesRuleAccessor;

import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import static net.minecraft.world.GameRules.*;

public final class SynchronizedBooleanGameRuleRegistryImpl implements SynchronizedBooleanGameRuleRegistry {
    public static final Identifier PACKET_ID = new Identifier("frame", "game_rule_sync");

    public final Function<Type<?>, Key<?>> keyCache;
    public final ReverseMemoizeFunction<BooleanRule, String> idCache;
    public final Map<Key<BooleanRule>, Boolean> defaults, values;

    public SynchronizedBooleanGameRuleRegistryImpl() {
        this.keyCache = Util.memoize(this::typeToKey);
        this.idCache = ReverseMemoizeFunction.create(this::ruleToId);

        this.defaults = Maps.newHashMap();
        this.values = Maps.newHashMap();
    }

    @Override
    public Key<BooleanRule> register(Key<BooleanRule> key, boolean defaultValue) {
        this.defaults.put(key, defaultValue);
        this.values.put(key, defaultValue);
        return key;
    }

    @Override
    public boolean get(World world, Key<BooleanRule> key) {
        return world.isClient ? this.values.get(key) : world.getGameRules().getBoolean(key);
    }

    public void set(Key<BooleanRule> key, boolean value) {
        this.values.put(key, value);
    }

    public void synchronize(MinecraftServer server, BooleanRule rule) {
        String id = this.idCache.apply(rule);
        for (ServerPlayerEntity player : PlayerLookup.all(server)) this.synchronize(player, id, rule.get());
    }

    public void synchronize(ServerPlayerEntity player, BooleanRule rule) {
        String id = this.idCache.apply(rule);
        this.synchronize(player, id, rule.get());
    }

    public void synchronize(ServerPlayerEntity player, String id, boolean value) {
        PacketByteBuf buf = PacketByteBufs.create();
        buf.writeString(id);
        buf.writeBoolean(value);
        ServerPlayNetworking.send(player, PACKET_ID, buf);
    }

    private Key<?> typeToKey(Type<?> type) {
        Map<Key<?>, Type<?>> map = GameRulesAccessor.getRULE_TYPES();
        Set<Map.Entry<Key<?>, Type<?>>> entries = map.entrySet();
        Map<Type<?>, Key<?>> inverse = entries.stream().collect(Collectors.toMap(Map.Entry::getValue, Map.Entry::getKey));
        return inverse.get(type);
    }

    public String ruleToId(BooleanRule rule) {
        Key<?> key = this.keyCache.apply(((GameRulesRuleAccessor) rule).getType());
        return key.getName();
    }
}
