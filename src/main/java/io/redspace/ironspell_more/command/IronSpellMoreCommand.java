package io.redspace.ironspell_more.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import io.redspace.ironspell_more.IronSpellMore;
import io.redspace.ironspell_more.compat.api.AnimationCue;
import io.redspace.ironspell_more.compat.api.CompatResult;
import io.redspace.ironspell_more.compat.epicfight.EpicFightCompat;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.Arrays;

/**
 * Commands for testing and invoking ironspell_more features and compatibility animations.
 */
@Mod.EventBusSubscriber(modid = IronSpellMore.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class IronSpellMoreCommand {
    private IronSpellMoreCommand() {
    }

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        register(event.getDispatcher());
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
            Commands.literal("ironspell_more")
                .requires(source -> source.hasPermission(2))
                .then(Commands.literal("play_animation")
                    .then(Commands.argument("cue", StringArgumentType.word())
                        .suggests((context, builder) -> SharedSuggestionProvider.suggest(
                            Arrays.stream(AnimationCue.values()).map(c -> c.name().toLowerCase()),
                            builder
                        ))
                        .executes(context -> {
                            ServerPlayer player = context.getSource().getPlayerOrException();
                            String cueStr = StringArgumentType.getString(context, "cue");
                            return executeAnimation(player, cueStr);
                        })
                    )
                )
                .then(Commands.literal("test_blazing_chakra")
                    .executes(context -> {
                        ServerPlayer player = context.getSource().getPlayerOrException();
                        return executeAnimation(player, AnimationCue.BLAZING_CHAKRA.name());
                    })
                )
        );

        // Convenient short alias: /ism
        dispatcher.register(
            Commands.literal("ism")
                .requires(source -> source.hasPermission(2))
                .then(Commands.literal("play_animation")
                    .then(Commands.argument("cue", StringArgumentType.word())
                        .suggests((context, builder) -> SharedSuggestionProvider.suggest(
                            Arrays.stream(AnimationCue.values()).map(c -> c.name().toLowerCase()),
                            builder
                        ))
                        .executes(context -> {
                            ServerPlayer player = context.getSource().getPlayerOrException();
                            String cueStr = StringArgumentType.getString(context, "cue");
                            return executeAnimation(player, cueStr);
                        })
                    )
                )
                .then(Commands.literal("test_blazing_chakra")
                    .executes(context -> {
                        ServerPlayer player = context.getSource().getPlayerOrException();
                        return executeAnimation(player, AnimationCue.BLAZING_CHAKRA.name());
                    })
                )
        );
    }

    private static int executeAnimation(ServerPlayer player, String cueStr) {
        AnimationCue targetCue = null;
        for (AnimationCue cue : AnimationCue.values()) {
            if (cue.name().equalsIgnoreCase(cueStr)) {
                targetCue = cue;
                break;
            }
        }

        if (targetCue == null) {
            player.sendSystemMessage(Component.literal("Unknown animation cue: " + cueStr).withStyle(ChatFormatting.RED));
            return 0;
        }

        CompatResult result = EpicFightCompat.playAnimation(player, targetCue);
        if (result == CompatResult.APPLIED) {
            player.sendSystemMessage(Component.literal("Successfully played animation: " + targetCue.name()).withStyle(ChatFormatting.GREEN));
            return 1;
        } else if (result == CompatResult.UNAVAILABLE) {
            String message = EpicFightCompat.isAvailable()
                ? "Animation cue is not registered: " + targetCue.name()
                : "Epic Fight compatibility is not currently available.";
            player.sendSystemMessage(Component.literal(message).withStyle(ChatFormatting.YELLOW));
            return 0;
        } else if (result == CompatResult.UNSUPPORTED) {
            player.sendSystemMessage(Component.literal("Player does not support Epic Fight entity patch.").withStyle(ChatFormatting.YELLOW));
            return 0;
        } else {
            player.sendSystemMessage(Component.literal("Failed to play animation (linkage error).").withStyle(ChatFormatting.RED));
            return 0;
        }
    }
}
