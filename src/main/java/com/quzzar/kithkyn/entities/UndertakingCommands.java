package com.quzzar.kithkyn.entities;

import java.util.Optional;
import net.minecraft.commands.Commands;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;

/** Developer inspection of the one saved undertaking store; physical lifecycle verification lives in its world fixture. */
public final class UndertakingCommands {
  private UndertakingCommands() { }
  public static LiteralArgumentBuilder<CommandSourceStack> branch() {
    return Commands.literal("undertaking").then(Commands.literal("show")
        .then(Commands.argument("target", EntityArgument.entity()).executes(context -> {
          if (!(EntityArgument.getEntity(context, "target") instanceof RealPerson person)) return 0;
          String report = UndertakingService.briefing(person.getData(KithkynAttachments.UNDERTAKINGS.get()),
              person.getUUID(), Optional.empty(), 32);
          context.getSource().sendSuccess(() -> Component.literal(report), false); return 1;
        })));
  }
}
