package io.github.dimaniojk.nofingerprint.mixin.client;

//? if >=1.20.5 {
import io.github.dimaniojk.nofingerprint.protection.NoFingerprintComponentCodec;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.serialization.Codec;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import java.util.function.Function;

@Mixin(ComponentSerialization.class)
public class ComponentSerializationMixin {

    @WrapOperation(
        method = "<clinit>",
        at = @At(value = "INVOKE",
            target = "Lcom/mojang/serialization/Codec;recursive(Ljava/lang/String;Ljava/util/function/Function;)Lcom/mojang/serialization/Codec;"))
    private static Codec<Component> nofingerprint$wrapRecursive(String name,
            Function<Codec<Component>, Codec<Component>> body,
            Operation<Codec<Component>> original) {
        return new NoFingerprintComponentCodec(original.call(name, body));
    }
}
//?} else {
/*
import io.github.dimaniojk.nofingerprint.protection.NoFingerprintFromPacketAccess;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonElement;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentContents;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.contents.TranslatableContents;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.lang.reflect.Type;

// Pre-1.20.5: no ComponentSerialization codec. Components are parsed via the Gson
// adapter Component$Serializer.deserialize, called from FriendlyByteBuf.readComponent
// on the netty I/O thread (so PacketContext is not set yet — it goes true later on
// the game thread inside PacketUtilsMixin). Tag every multiplayer-context deserialize
// return tree so TranslatableContentsMixin / KeybindContentsMixin's per-instance
// nofingerprint$fromPacket flag is set before the component is ever rendered.
@Mixin(Component.Serializer.class)
public class ComponentSerializationMixin {

    @Inject(
        method = "deserialize(Lcom/google/gson/JsonElement;Ljava/lang/reflect/Type;Lcom/google/gson/JsonDeserializationContext;)Lnet/minecraft/network/chat/MutableComponent;",
        at = @At("RETURN")
    )
    private void nofingerprint$tagDeserialized(JsonElement element, Type type, JsonDeserializationContext context,
            CallbackInfoReturnable<MutableComponent> cir) {
        MutableComponent result = cir.getReturnValue();
        if (result == null) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc != null && mc.hasSingleplayerServer()) return;
        nofingerprint$markTree(result);
    }

    @Unique
    private static void nofingerprint$markTree(Component component) {
        if (component == null) return;
        ComponentContents contents = component.getContents();
        if (contents instanceof NoFingerprintFromPacketAccess access) access.nofingerprint$setFromPacket();
        if (contents instanceof TranslatableContents tc) {
            for (Object arg : tc.getArgs()) {
                if (arg instanceof Component argComp) nofingerprint$markTree(argComp);
            }
        }
        for (Component sibling : component.getSiblings()) nofingerprint$markTree(sibling);
    }
}
*///?}
