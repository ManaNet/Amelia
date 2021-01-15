package pw.mihou.amelia.commands.test;

import org.javacord.api.entity.permission.Role;
import org.javacord.api.entity.server.Server;
import org.javacord.api.entity.user.User;
import org.javacord.api.event.message.MessageCreateEvent;
import pw.mihou.amelia.commands.base.Command;
import pw.mihou.amelia.commands.db.FeedDB;
import pw.mihou.amelia.commands.db.MessageDB;
import pw.mihou.amelia.io.rome.ReadRSS;
import pw.mihou.amelia.templates.Message;

import java.util.ArrayList;

public class TestCommand extends Command {

    public TestCommand(){
        super("test", "Test runs a feed.", "test", true);
    }

    @Override
    protected void runCommand(MessageCreateEvent event, User user, Server server, String[] args) {

        if(args.length > 1){
            try {
                long id = Long.parseLong(args[1]);
                if (FeedDB.validate(id)) {
                    FeedDB.getServer(server.getId()).getChannel(event.getChannel().getId()).getFeedModel(id).ifPresentOrElse(feedModel ->
                                    ReadRSS.getLatest(feedModel.getFeedURL()).ifPresentOrElse(syndEntry ->
                             server.getTextChannelById(feedModel.getChannel()).ifPresentOrElse(tc ->
                                     // Replace all placeholders ahead of time.
                                     Message.msg(MessageDB.requestFormat(tc.getServer().getId()).replaceAll("\\{title}", syndEntry.getTitle())
                                             .replaceAll("\\{author}", syndEntry.getAuthor()).replaceAll("\\{link}", syndEntry.getLink())
                                             .replaceAll("\\{subscribed}", getMentions(feedModel.getMentions(), tc.getServer()))).send(tc),
                            () -> Message.msg("Error: The channel provided does not exist.").send(event.getChannel())),
                            () -> Message.msg("Error: We couldn't connect to ScribbleHub's RSS feed, please try again later.").send(event.getChannel())),
                            () -> Message.msg("We couldn't find the feed, are you sure you are using the feed's unique ID?").send(event.getChannel()));
                } else {
                    Message.msg("Error: We couldn't find the feed, are you sure you are using the feed's unique id." +
                            "\nPlease verify using `feeds`").send(event.getChannel());
                }
            } catch (NumberFormatException | ArithmeticException e){
                Message.msg("Error: Number format exception, or arithmetic exception.").send(event.getChannel());
            }
        } else {
            Message.msg("Error: Lacking arguments [feed id]").send(event.getChannel());
        }
    }

    private String getMentions(ArrayList<Long> roles, Server server){
        StringBuilder builder = new StringBuilder();
        roles.forEach(aLong -> builder.append(server.getRoleById(aLong).map(Role::getMentionTag).orElse("[Vanished Role]")));
        return builder.toString();
    }
}
