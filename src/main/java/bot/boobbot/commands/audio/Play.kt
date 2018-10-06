package bot.boobbot.commands.audio

import bot.boobbot.BoobBot.Companion.playerManager
import bot.boobbot.audio.AudioLoader
import bot.boobbot.flight.CommandProperties
import bot.boobbot.flight.Context
import bot.boobbot.models.VoiceCommand

@CommandProperties(description = "Plays a song from the given URL")
class Play : VoiceCommand {

    override fun execute(ctx: Context) {
        val shouldPlay = performVoiceChecks(ctx)

        if (!shouldPlay) {
            return
        }

        val player = ctx.audioPlayer!!
        val query = ctx.args.joinToString(" ")

        playerManager.loadItem(query, AudioLoader(player, ctx))
    }

}
