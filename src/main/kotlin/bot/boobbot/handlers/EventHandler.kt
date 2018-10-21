package bot.boobbot.handlers

import bot.boobbot.BoobBot
import bot.boobbot.BoobBot.Companion.setGame
import bot.boobbot.BoobBot.Companion.shardManager
import bot.boobbot.flight.Category
import bot.boobbot.misc.Constants
import bot.boobbot.misc.Formats
import bot.boobbot.misc.Utils
import bot.boobbot.misc.Utils.Companion.autoAvatar
import com.sun.management.OperatingSystemMXBean
import de.mxro.metrics.jre.Metrics
import io.ktor.application.call
import io.ktor.http.ContentType
import io.ktor.response.respondRedirect
import io.ktor.response.respondText
import io.ktor.routing.get
import io.ktor.routing.routing
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import net.dv8tion.jda.core.EmbedBuilder
import net.dv8tion.jda.core.JDA
import net.dv8tion.jda.core.Permission
import net.dv8tion.jda.core.entities.Game
import net.dv8tion.jda.core.entities.User
import net.dv8tion.jda.core.events.DisconnectEvent
import net.dv8tion.jda.core.events.ReadyEvent
import net.dv8tion.jda.core.events.ReconnectedEvent
import net.dv8tion.jda.core.events.ResumedEvent
import net.dv8tion.jda.core.events.guild.GuildJoinEvent
import net.dv8tion.jda.core.events.guild.GuildLeaveEvent
import net.dv8tion.jda.core.hooks.ListenerAdapter
import net.dv8tion.jda.webhook.WebhookClientBuilder
import net.dv8tion.jda.webhook.WebhookMessageBuilder
import org.apache.commons.lang3.StringUtils
import org.json.JSONArray
import org.json.JSONObject
import java.awt.Color
import java.lang.management.ManagementFactory
import java.text.DecimalFormat
import java.time.Instant.now
import java.util.concurrent.TimeUnit


class EventHandler : ListenerAdapter() {
    var self: User? = null // just to hold self for discon webhooks
    override fun onReady(event: ReadyEvent) {
        BoobBot.metrics.record(Metrics.happened("Ready"))
        BoobBot.log.info("Ready on shard: ${event.jda.shardInfo.shardId}, Ping: ${event.jda.ping}ms, Status: ${event.jda.status}")
        val readyClient = WebhookClientBuilder(Constants.RDY_WEBHOOK).build()
        readyClient.send(WebhookMessageBuilder().addEmbeds(EmbedBuilder().setColor(Color.magenta)
                .setAuthor(
                        event.jda.selfUser.name,
                        event.jda.selfUser.effectiveAvatarUrl,
                        event.jda.selfUser.effectiveAvatarUrl
                ).setTitle("```Ready on shard: ${event.jda.shardInfo.shardId}, Ping: ${event.jda.ping}ms, Status: ${event.jda.status}```", event.jda.asBot().getInviteUrl(Permission.ADMINISTRATOR))
                .setTimestamp(now()).build()).setUsername(event.jda.selfUser.name).setAvatarUrl(event.jda.selfUser.effectiveAvatarUrl)
                .build())

        if (BoobBot.shardManager.statuses.entries.stream().filter { e -> e.value.name == "CONNECTED" }.count().toInt() == BoobBot.shardManager.shardsTotal - 1 && !BoobBot.isReady) {
            BoobBot.isReady = true
            if (!BoobBot.isDebug) { // dont need this is testing
                BoobBot.Scheduler.scheduleAtFixedRate(Utils.auto(autoAvatar()), 1, 2, TimeUnit.HOURS)
            }
            self = event.jda.selfUser // set
            // health check for status page
            embeddedServer(Netty, 8888) {
                routing {

                    get("/") {
                        call.respondRedirect("https://boob.bot", true)
                    }

                    get("/stats") {
                        val dpFormatter = DecimalFormat("0.00")
                        val rUsedRaw = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()
                        val rPercent = dpFormatter.format(rUsedRaw.toDouble() / Runtime.getRuntime().totalMemory() * 100)
                        val usedMB = dpFormatter.format(rUsedRaw.toDouble() / 1048576)

                        val servers = BoobBot.shardManager.guildCache.size()
                        val users = BoobBot.shardManager.userCache.size()

                        val shards = BoobBot.shardManager.shardsTotal
                        val shardsOnline = BoobBot.shardManager.shards.asSequence().filter { s -> s.status == JDA.Status.CONNECTED }.count()
                        val averageShardLatency = BoobBot.shardManager.averagePing.toInt()

                        val osBean: OperatingSystemMXBean = ManagementFactory.getPlatformMXBean(OperatingSystemMXBean::class.java)
                        val procCpuUsage = dpFormatter.format(osBean.processCpuLoad * 100)
                        val sysCpuUsage = dpFormatter.format(osBean.systemCpuLoad * 100)
                        val players = BoobBot.musicManagers.filter { p -> p.value.player.playingTrack != null }.count()

                        val jvm = JSONObject()
                                .put("Uptime", Utils.fTime(System.currentTimeMillis() - BoobBot.startTime))
                                .put("JVM_CPU_Usage", procCpuUsage)
                                .put("System_CPU_Usage", sysCpuUsage)
                                .put("RAM_Usage", "${usedMB}MB($rPercent%)")
                                .put("Threads", Thread.activeCount())

                        val bb = JSONObject()
                                .put("Guilds", servers)
                                .put("Users", users)
                                .put("Audio_Players", players)
                                .put("Shards_Online", "$shardsOnline/$shards")
                                .put("Average_Latenc", "${averageShardLatency}ms")

                        call.respondText("{\"stats\": ${JSONObject().put("bb", bb).put("jvm", jvm)}}", ContentType.Application.Json)


                    }

                    get("/metrics") {
                        call.respondText("{\"metrics\": ${BoobBot.metrics.render().get()}}", ContentType.Application.Json)
                    }

                    get("/health") {
                        call.respondText("{\"health\": \"ok\", \"ping\": ${BoobBot.shardManager.averagePing}}", ContentType.Application.Json)
                    }

                    get("/pings") {
                        val pings = JSONArray()
                        for (e in shardManager.statuses.entries) pings.put(JSONObject().put("shard", e.key.shardInfo.shardId).put("ping", e.key.ping).put("status", e.value))
                        call.respondText("{\"status\": $pings}", ContentType.Application.Json)
                    }

                    get("/commands") {
                        val categoryJson = JSONObject()
                        Category.values().filter { c -> c.name != "DEV" }.forEach { category ->

                            val commands = JSONArray()

                            BoobBot.commands.values.filter { it -> it.properties.category == category }.forEach { command ->

                                commands.put(JSONObject()
                                        .put("command", command.name)
                                        .put("category", command.properties.category)
                                        .put("description", command.properties.description)
                                        .put("aliases", "[${command.properties.aliases.joinToString(", ")}]"))
                            }

                            categoryJson.put(category.name, commands)
                        }

                        call.respondText("{\"commands\": $categoryJson}", ContentType.Application.Json)
                    }

                }
            }.start(wait = false)
            BoobBot.log.info(Formats.getReadyFormat())
            readyClient.send(WebhookMessageBuilder().setContent("Ready").addEmbeds(EmbedBuilder().setColor(Color.magenta)
                    .setAuthor(
                            event.jda.selfUser.name,
                            event.jda.selfUser.effectiveAvatarUrl,
                            event.jda.selfUser.effectiveAvatarUrl)
                    .setTitle("${event.jda.selfUser.name} Fully Ready", event.jda.asBot().getInviteUrl(Permission.ADMINISTRATOR))
                    .setThumbnail(event.jda.selfUser.effectiveAvatarUrl).addField("Ready info", "``` ${Formats.getReadyFormat()}```", false)
                    .setTimestamp(now())
                    .build()).setUsername(event.jda.selfUser.name).setAvatarUrl(event.jda.selfUser.effectiveAvatarUrl)
                    .build())
            readyClient.close()
        }
        readyClient.close()
    }


    override fun onReconnect(event: ReconnectedEvent?) {
        BoobBot.metrics.record(Metrics.happened("Reconnected"))
        BoobBot.log.info("Reconnected on shard: ${event?.jda?.shardInfo?.shardId}, Status: ${event?.jda?.status}")
        val readyClient = WebhookClientBuilder(Constants.RDY_WEBHOOK).build()
        try {
            readyClient.send(WebhookMessageBuilder().addEmbeds(EmbedBuilder().setColor(Color.green)
                    .setAuthor(
                            self?.name,
                            self?.effectiveAvatarUrl,
                            self?.effectiveAvatarUrl
                    ).setTitle("```Reconnected on shard: ${event?.jda?.shardInfo?.shardId}, Status: ${event?.jda?.status}```")
                    .setTimestamp(now()).build()).setUsername(self?.name).setAvatarUrl(self?.effectiveAvatarUrl)
                    .build())
            readyClient.close()
        } catch (ex: Exception) {
            readyClient.close()
            BoobBot.log.warn("error on reconnected event", ex)
        }
    }

    override fun onResume(event: ResumedEvent?) {
        BoobBot.metrics.record(Metrics.happened("Resumed"))
        BoobBot.log.info("Resumed on shard: ${event?.jda?.shardInfo?.shardId}, Status: ${event?.jda?.status}")
        val readyClient = WebhookClientBuilder(Constants.RDY_WEBHOOK).build()
        try {
            readyClient.send(WebhookMessageBuilder().addEmbeds(EmbedBuilder().setColor(Color.green)
                    .setAuthor(
                            self?.name,
                            self?.effectiveAvatarUrl,
                            self?.effectiveAvatarUrl
                    ).setTitle("```Resumed on shard: ${event?.jda?.shardInfo?.shardId}, Status: ${event?.jda?.status}```")
                    .setTimestamp(now()).build()).setUsername(self?.name).setAvatarUrl(self?.effectiveAvatarUrl)
                    .build())
            readyClient.close()
        } catch (ex: Exception) {
            readyClient.close()
            BoobBot.log.warn("error on resumed event", ex)
        }
    }

    override fun onDisconnect(event: DisconnectEvent?) {
        BoobBot.metrics.record(Metrics.happened("Disconnect"))
        BoobBot.log.info("Disconnect on shard: ${event?.jda?.shardInfo?.shardId}, Status: ${event?.jda?.status}")
        val readyClient = WebhookClientBuilder(Constants.RDY_WEBHOOK).build()
        try {
            readyClient.send(WebhookMessageBuilder().addEmbeds(EmbedBuilder().setColor(Color.green)
                    .setAuthor(
                            self?.name,
                            self?.effectiveAvatarUrl,
                            self?.effectiveAvatarUrl
                    ).setTitle("```Disconnect on shard: ${event?.jda?.shardInfo?.shardId}, Status: ${event?.jda?.status}```")
                    .setTimestamp(now()).build()).setUsername(self?.name).setAvatarUrl(self?.effectiveAvatarUrl)
                    .build())
            readyClient.close()
        } catch (ex: Exception) {
            readyClient.close()
            BoobBot.log.warn("error on Disconnect event", ex)
        }
    }

    override fun onGuildJoin(event: GuildJoinEvent?) {
        BoobBot.metrics.record(Metrics.happened("GuildJoin"))
        if (!BoobBot.isReady) {
            return
        }
        val jda = event!!.jda
        val guild = event.guild
        if (!setGame) {
            event.jda.asBot().shardManager.setGame(Game.playing("bbhelp || bbinvite"))
        }
        BoobBot.log.info("New Guild Joined ${guild.name}(${guild.id})")
        val em = EmbedBuilder()
                .setColor(Color.green)
                .setAuthor(guild.name, guild.iconUrl, guild.iconUrl)
                .setTitle("Joined ${guild.name}")
                .setThumbnail(guild.iconUrl)
                .setDescription("Guild info")
                .addField(
                        Formats.info("info"),
                        "**${guild.jda.shardInfo}**\n" +
                                "Guilds: **${jda.asBot().shardManager.guilds.size}**\n" +
                                "Owner: **${guild.owner.effectiveName}**\n" +
                                "Guild Users: **${guild.members.size}**\n",
                        false)
                .setTimestamp(now())
                .build()
        val guildJoinClient = WebhookClientBuilder(Constants.GJLOG_WEBHOOK).build()
        try {
            guildJoinClient.send(
                    WebhookMessageBuilder()
                            .addEmbeds(em)
                            .setUsername(if (guild.name.length > 3) StringUtils.abbreviate(guild.name, 20) else "Shity name")
                            .setAvatarUrl(guild.iconUrl)
                            .build())
            guildJoinClient.close()
        } catch (ex: java.lang.Exception) {
            guildJoinClient.close()
            BoobBot.log.warn("error on Guild join event", ex)
        }
    }

    override fun onGuildLeave(event: GuildLeaveEvent?) {
        BoobBot.metrics.record(Metrics.happened("GuildLeave"))
        if (!BoobBot.isReady) {
            return
        }
        val jda = event!!.jda
        val guild = event.guild
        if (!setGame) {
            event.jda.asBot().shardManager.setGame(Game.playing("bbhelp || bbinvite"))
        }
        BoobBot.log.info("Guild left ${guild.name}(${guild.id})")
        val guildLeaveClient = WebhookClientBuilder(Constants.GLLOG_WEBHOOK).build()
        try {
            guildLeaveClient.send(
                    WebhookMessageBuilder()
                            .addEmbeds(
                                    EmbedBuilder()
                                            .setColor(Color.red)
                                            .setAuthor(guild.name, guild.iconUrl, guild.iconUrl)
                                            .setTitle("Left ${guild.name}")
                                            .setThumbnail(guild.iconUrl)
                                            .setDescription("Guild info")
                                            .addField(
                                                    Formats.info("info"),
                                                    "**${guild.jda.shardInfo}**\n" +
                                                            "Guilds: **${jda.asBot().shardManager.guilds.size}**\n" +
                                                            "Owner: **${guild.owner.effectiveName}**\n" +
                                                            "Guild Users: **${guild.members.size}**\n",
                                                    false)
                                            .build())
                            .setUsername(if (guild.name.length > 3) StringUtils.abbreviate(guild.name, 20) else "Shity name")
                            .setAvatarUrl(guild.iconUrl)
                            .build())
            guildLeaveClient.close()
        } catch (ex: Exception) {
            guildLeaveClient.close()
            BoobBot.log.warn("error on Guild leave event", ex)
        }
    }
}
