package bot.boobbot.commands.dev

import bot.boobbot.BoobBot
import bot.boobbot.flight.Category
import bot.boobbot.flight.Command
import bot.boobbot.flight.CommandProperties
import bot.boobbot.flight.Context
import bot.boobbot.misc.Formats
import bot.boobbot.misc.thenException
import com.mewna.catnip.entity.user.Presence
import com.mewna.catnip.entity.util.Permission


@CommandProperties(description = "Settings", category = Category.DEV, developerOnly = true)
class Set : Command {


    override fun execute(ctx: Context) {

        when (ctx.args[0]) {

//            "name" -> {
//                val newName = ctx.args.drop(1).joinToString(" ")
//
//                BoobBot.catnip.rest().user().modifyCurrentUser(newName, avatar = null)
//
////                selfUser.manager.setName(newName).queue(
////                    { ctx.send(Formats.info("Set UserName to $newName")) },
////                    { ctx.send(Formats.error(" Failed to set UserName")) }
////                )
//
//            }

            "game" -> {

                val game = ctx.args.drop(2).joinToString(" ")

                when (ctx.args[1]) {

                    "playing" -> {

                        BoobBot.setGame = true
                        BoobBot.catnip.game(game, Presence.ActivityType.PLAYING, null)
                        ctx.send(Formats.info("Yes daddy, game set"))

                    }

                    "listening" -> {

                        BoobBot.setGame = true
                        BoobBot.catnip.game(game, Presence.ActivityType.LISTENING, null)
                        ctx.send(Formats.info("Yes daddy, game set"))

                    }

                    "watching" -> {

                        BoobBot.setGame = true
                        BoobBot.catnip.game(game, Presence.ActivityType.WATCHING, null)
                        ctx.send(Formats.info("Yes daddy, game set"))

                    }

                    "stream" -> {

                        val url = ctx.args[2]
                        val name = ctx.args.drop(3).joinToString(" ")

                        BoobBot.setGame = true
                        BoobBot.catnip.game(name, Presence.ActivityType.WATCHING, url)
                        ctx.send(Formats.info("Yes daddy, game set"))

                    }

                    "clear" -> {

                        BoobBot.setGame = false
                        BoobBot.catnip.game("bbhelp || bbinvite", Presence.ActivityType.PLAYING, null)
                        ctx.send(Formats.info("Yes daddy, cleared game"))

                    }

                    else -> {
                        //todo send com help
                    }
                }

            }

            "nick" -> {
                if (ctx.guild == null) {
                    return ctx.send("This can only be run in a guild")
                }

                if (ctx.botCan(Permission.CHANGE_NICKNAME)) {
                    val newNickname = ctx.args.drop(1).joinToString(" ")
                    ctx.guild.changeNickName(newNickname, "BoobBot nick set")
                        .thenAccept {
                            ctx.send(Formats.info("Yes daddy, nick set"))
                        }
                        .thenException {
                            ctx.send(Formats.error(" Failed to set nick"))
                        }
                }

            }

            "avatar" -> {

//                BoobBot.requestUtil.get(ctx.args[1]).queue {
//                    val image = it?.body()?.byteStream() ?: return@queue ctx.send("Unable to fetch avatar")
//
//                    BoobBot.catnip.rest().user().modifyCurrentUser()
//                    ctx.jda.selfUser.manager.setAvatar(Icon.from(image)).queue(
//                        { ctx.send(Formats.info("Yes daddy, avatar set")) },
//                        { ctx.send(Formats.error(" Failed to set avatar")) }
//                    )
//                    BoobBot.log.info("Setting New Avatar")
//                    BoobBot.manSetAvatar = true
//                }

            }

            "icons" -> {

//                BoobBot.requestUtil.get(ctx.args[1]).queue {
//                    val image = it?.body()?.byteStream() ?: return@queue ctx.send("Unable to fetch image")
//                    val icon = Icon.from(image)
//
//                    BoobBot.home?.manager?.setIcon(icon)?.queue()
//                    ctx.jda.selfUser.manager.setAvatar(icon).queue(
//                        { ctx.send(Formats.info("Yes daddy, icons set")) },
//                        { ctx.send(Formats.error(" Failed to set avatar")) }
//                    )
//                    BoobBot.log.info("Setting New icons")
//                    BoobBot.manSetAvatar = true
//                }

            }


            else -> {
                //todo send com help
            }

        }
    }

}