package bot.boobbot.flight;

import net.dv8tion.jda.core.events.message.MessageReceivedEvent;

public interface Command {

    public void execute(MessageReceivedEvent event, String[] args);

    default public CommandProperties getProperties() {
        return this.getClass().getAnnotation(CommandProperties.class);
    }

}