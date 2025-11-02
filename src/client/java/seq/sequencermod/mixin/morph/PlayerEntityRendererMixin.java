package seq.sequencermod.mixin.morph;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.PlayerEntityRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import seq.sequencermod.net.client.MorphClientSync;
import seq.sequencermod.net.client.SimpleMorphs;

@Mixin(PlayerEntityRenderer.class)
public abstract class PlayerEntityRendererMixin {

    // Главный рендер тела игрока
    @Inject(method = "render(Lnet/minecraft/client/network/AbstractClientPlayerEntity;FFLnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;I)V",
            at = @At("HEAD"), cancellable = true)
    private void sequencer$renderMorph(AbstractClientPlayerEntity player,
                                       float entityYaw,
                                       float tickDelta,
                                       MatrixStack matrices,
                                       VertexConsumerProvider vertexConsumers,
                                       int light,
                                       CallbackInfo ci) {
        Identifier typeId = MorphClientSync.getMorphType(player.getUuid());
        if (typeId == null) return; // нет морфа — рендерим игрока как обычно

        // Скрываем модель игрока и рисуем зеркало морфа на его месте
        ci.cancel();
        // Вызов с действительной сигнатурой метода SimpleMorphs.renderForPlayer(...)
        SimpleMorphs.renderForPlayer(player, typeId, tickDelta, matrices, vertexConsumers, light);
    }

    // Руки в первом лице: оставляем их, если shouldHidePlayerModel говорит "не скрывать"
    @Inject(method = "renderLeftArm", at = @At("HEAD"), cancellable = true)
    private void sequencer$hideLeftArm(MatrixStack matrices,
                                       VertexConsumerProvider vertexConsumers,
                                       int light,
                                       AbstractClientPlayerEntity player,
                                       CallbackInfo ci) {
        boolean isLocal = MinecraftClient.getInstance().player == player;
        boolean firstPerson = isLocal && MinecraftClient.getInstance().options.getPerspective().isFirstPerson();
        Identifier typeId = MorphClientSync.getMorphType(player.getUuid());
        if (typeId == null) return;

        boolean hide = MorphClientSync.shouldHidePlayerModel(player.getUuid(), isLocal, firstPerson);
        if (hide) ci.cancel();
    }

    @Inject(method = "renderRightArm", at = @At("HEAD"), cancellable = true)
    private void sequencer$hideRightArm(MatrixStack matrices,
                                        VertexConsumerProvider vertexConsumers,
                                        int light,
                                        AbstractClientPlayerEntity player,
                                        CallbackInfo ci) {
        boolean isLocal = MinecraftClient.getInstance().player == player;
        boolean firstPerson = isLocal && MinecraftClient.getInstance().options.getPerspective().isFirstPerson();
        Identifier typeId = MorphClientSync.getMorphType(player.getUuid());
        if (typeId == null) return;

        boolean hide = MorphClientSync.shouldHidePlayerModel(player.getUuid(), isLocal, firstPerson);
        if (hide) ci.cancel();
    }
}