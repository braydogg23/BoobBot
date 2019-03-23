package bot.boobbot.commands.bot

import bot.boobbot.BoobBot
import bot.boobbot.flight.Category
import bot.boobbot.flight.Command
import bot.boobbot.flight.CommandProperties
import bot.boobbot.flight.Context
import bot.boobbot.misc.Colors
import bot.boobbot.misc.Formats
import bot.boobbot.misc.Utils
import bot.boobbot.models.Config
import com.mewna.catnip.entity.builder.EmbedBuilder
import java.time.Instant

@CommandProperties(
    description = "help, --dm for dm",
    aliases = ["halp", "halllp", "coms", "commands"],
    category = Category.MISC
)
class Help : Command {

    override fun execute(ctx: Context) {
        val commands = BoobBot.commands.values

        if (ctx.args.isEmpty() || ctx.args[0] == "--dm") {
            val builder = EmbedBuilder()

            builder.author(
                "${ctx.selfUser?.username()} help ${Formats.MAGIC_EMOTE}",
                BoobBot.inviteUrl,
                ctx.selfUser?.effectiveAvatarUrl()
            )

            builder.color(Colors.getEffectiveColor(ctx.message))

            Category.values().forEach { category ->
                val list = commands
                    .filter { it.properties.category == category && !it.properties.developerOnly }
                    .joinToString("\n") { "`bb${padEnd(it.name)}:` ${it.properties.description}" }

                if (list.isNotEmpty()) {
                    builder.field(category.title, list, false)
                }
            }

            builder.field("${Formats.LINK_EMOTE} Links", Formats.LING_MSG, false)
            builder.footer("Help requested by ${ctx.author.username()}", ctx.author.effectiveAvatarUrl())
            builder.timestamp(Instant.now())

            if (!ctx.args.isEmpty() && ctx.args[0] == "--dm") {
                ctx.message.react("\uD83D\uDCEC")
                ctx.dm(builder.build())
            } else {
                ctx.embed(builder.build())
            }

            if (Config.owners.contains(ctx.author.idAsLong())) {
                val d = EmbedBuilder()

                d.title("You're a developer!")
                d.color(Colors.getEffectiveColor(ctx.message))

                Category.values().forEach { category ->
                    val list = commands
                        .filter { it.properties.category == category && it.properties.developerOnly }
                        .joinToString("\n") { "`bb${padEnd(it.name)}:` ${it.properties.description}" }

                    if (list.isNotEmpty()) {
                        d.field(category.title, list, false)
                    }
                }

                if (!ctx.args.isEmpty() && ctx.args[0] == "--dm") {
                    ctx.dm(d.build())
                } else {
                    ctx.embed(d.build())
                }
            }

            return
        }

        val command = Utils.getCommand(ctx.args[0])
            ?: return ctx.send("That command doesn't exist")

        val mappedAliases = command.properties.aliases.joinToString(", ")
        val aliases = if (mappedAliases.isEmpty()) "None" else mappedAliases

        val commandHelp = EmbedBuilder()
            .color(Colors.getEffectiveColor(ctx.message))
            .author(
                "${ctx.selfUser?.username()} Command Info",
                BoobBot.inviteUrl,
                ctx.selfUser?.effectiveAvatarUrl()
            )
            .field(
                Formats.info("Info"),
                String.format(
                    "Command:\n**%s%s**\nAliases:\n**%s**\nDescription:\n**%s**",
                    "bb", command.name, aliases, command.properties.description
                ),
                false
            )
            .footer("Help requested by ${ctx.author.username()}", ctx.author.effectiveAvatarUrl())
            .timestamp(Instant.now())

        if (ctx.args.size >= 2 && ctx.args[1].toLowerCase() == "--dm") {
            ctx.dm(commandHelp.build())
            ctx.message.react("\uD83D\uDCEC")
        } else {
            ctx.embed(commandHelp.build())
        }
    }

    private fun padEnd(str: String, length: Int = 15): String {
        return str + "\u200B ".repeat(length - str.length)
    }

}