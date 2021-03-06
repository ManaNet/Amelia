package pw.mihou.amelia.commands.notifier;

import org.javacord.api.entity.message.Message;
import org.javacord.api.entity.message.embed.EmbedBuilder;
import org.javacord.api.entity.user.User;
import org.javacord.api.event.message.MessageCreateEvent;
import pw.mihou.amelia.db.UserDB;
import pw.mihou.amelia.models.SHUser;
import pw.mihou.amelia.templates.Embed;
import pw.mihou.velen.interfaces.VelenEvent;

import java.util.Collection;

public class Author implements VelenEvent {

    public EmbedBuilder userEmbed(Collection<SHUser> users) {
        EmbedBuilder embed = new Embed().setTitle("Your Associated Accounts")
                .setDescription("Here are all the accounts associated with you.").build();
        if (users.isEmpty())
            return embed.setDescription("There are no users associated with this account, please use `iam [username]` if you want to be notified when a user under that username" +
                    " gets to the top 9 trending on ScribbleHub!");

        users.forEach(shUser -> embed.addField(String.format("[ID: %d] %s", shUser.getUnique(), shUser.getName()),
                String.format("**Username**: %s\n**ID**: %d", shUser.getName(), shUser.getUnique())));
        return embed.addField("Note", "The names here may be outdated (since we cache the names from IAm command)" +
                " but our checks will check the updated name, so don't worry if your name " +
                "on here is different (after changing usernames) since it won't affect anything.");
    }

    @Override
    public void onEvent(MessageCreateEvent event, Message message, User user, String[] args) {
        if (args.length > 0) {
            if (args.length == 2 && args[0].equalsIgnoreCase("remove")) {
                try {
                    int unique = Integer.parseInt(args[1]);
                    if (UserDB.doesExist(user.getId(), unique)) {
                        UserDB.remove(user.getId(), unique);
                        event.getMessage().reply(String.format("**SUCCESS**! The account with the unique id: %d was removed.", unique));
                    } else {
                        event.getMessage().reply(String.format("**ERROR**: You do not have an account associated with the unique ID as %d, " +
                                "please check author me for all accounts associated with your Discord account.", unique));
                    }
                } catch (NumberFormatException | ArithmeticException e) {
                    event.getMessage().reply("**ERROR**: Unique Identification must be a number that isn't exceed 4 digits");
                }
            } else if (args.length == 1 && args[0].equalsIgnoreCase("me")) {
                event.getMessage().reply(userEmbed(UserDB.get(user.getId()).getAccounts()));
            }
        }
    }
}
