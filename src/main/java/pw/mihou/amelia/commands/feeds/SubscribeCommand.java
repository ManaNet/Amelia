package pw.mihou.amelia.commands.feeds;

import org.javacord.api.entity.message.mention.AllowedMentionsBuilder;
import org.javacord.api.entity.permission.Role;
import org.javacord.api.entity.server.Server;
import org.javacord.api.entity.user.User;
import org.javacord.api.event.message.MessageCreateEvent;
import pw.mihou.amelia.commands.base.Command;
import pw.mihou.amelia.commands.db.FeedDB;
import pw.mihou.amelia.templates.Message;

import java.util.stream.Collectors;

public class SubscribeCommand extends Command {

    public SubscribeCommand() {
        super("subscribe", "Subscribes a role to the feed.", "subscribe [feed unique id] [roles]", true);
    }

    @Override
    protected void runCommand(MessageCreateEvent event, User user, Server server, String[] args) {

        if (args.length > 1) {
            if (!event.getMessage().getMentionedRoles().isEmpty()) {
                try {
                    long i = Long.parseLong(args[1]);
                    if (FeedDB.validate(i)) {
                        FeedDB.getServer(server.getId()).getFeedModel(i).ifPresentOrElse(feedModel -> {
                            event.getMessage().getMentionedRoles().forEach(role -> feedModel.subscribeRole(role.getId()));
                            feedModel.update(server.getId());
                            Message.msg("We have subscribed the following roles: " + event.getMessage().getMentionedRoles().stream().map(Role::getMentionTag)
                                    .collect(Collectors.joining(" ")))
                                    .setAllowedMentions(new AllowedMentionsBuilder()
                                            .setMentionRoles(false)
                                            .setMentionEveryoneAndHere(false)
                                            .setMentionUsers(false).build())
                                    .send(event.getChannel());
                        }, () -> event.getMessage().reply("Error: We couldn't find the feed with the unique id [" + i + "]." +
                                "\nPlease verify the unique id through `feeds`"));
                    } else {
                        event.getMessage().reply("Error: We couldn't find the feed with the unique id [" + i + "]." +
                                "\nPlease verify the unique id through `feeds`");
                    }
                } catch (NumberFormatException | ArithmeticException e) {
                    event.getMessage().reply("Error: Arithmetic, or NumberFormatException occurred. Are you sure you giving the proper value for args 1? [" + args[1] + "]");
                }
            }
        }
    }
}
