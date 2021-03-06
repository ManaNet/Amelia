package pw.mihou.amelia.commands.feeds;

import org.javacord.api.entity.channel.ServerTextChannel;
import org.javacord.api.entity.message.Message;
import org.javacord.api.entity.message.embed.EmbedBuilder;
import org.javacord.api.entity.server.Server;
import org.javacord.api.entity.user.User;
import org.javacord.api.event.message.MessageCreateEvent;
import pw.mihou.amelia.Amelia;
import pw.mihou.amelia.commands.db.FeedDB;
import pw.mihou.amelia.models.FeedModel;
import pw.mihou.amelia.models.FeedNavigator;
import pw.mihou.amelia.templates.Embed;
import pw.mihou.velen.interfaces.VelenEvent;

import java.util.ArrayList;
import java.util.concurrent.TimeUnit;

public class Feeds implements VelenEvent {

    @Override
    public void onEvent(MessageCreateEvent event, Message msg, User user, String[] args) {
        if (event.getServer().isEmpty())
            return;

        Server server = event.getServer().get();
        FeedNavigator navigator = new FeedNavigator(FeedDB.getServer(server.getId()).getModels());
        if (!navigator.getModels().isEmpty()) {
            event.getMessage().reply(embed(server, navigator.current().orElse(new ArrayList<>()), 1)).
                    thenAccept(message -> {
                        if (navigator.hasNext()) {
                            message.addReactions("⬅", "trash:775601666845573140", "➡");
                        } else {
                            message.addReaction("trash:775601666845573140");
                        }
                        message.addReactionAddListener(e -> {
                            if (e.getUserId() == event.getMessageAuthor().getId()) {
                                if (e.getEmoji().equalsEmoji("➡")) {
                                    if (navigator.hasNext()) {
                                        message.edit(embed(server, navigator.next().orElse(new ArrayList<>()), navigator.getPage()));
                                    }
                                } else if (e.getEmoji().equalsEmoji("⬅")) {
                                    if (navigator.canReverse()) {
                                        message.edit(embed(server, navigator.backwards().orElse(new ArrayList<>()), navigator.getPage()));
                                    }
                                }
                                if (e.getEmoji().getMentionTag().equalsIgnoreCase("<:trash:775601666845573140>")) {
                                    message.delete();
                                    msg.delete();
                                    navigator.reset();
                                }
                            }
                            if (e.getUserId() != event.getApi().getYourself().getId()) {
                                e.removeReaction();
                            }
                        }).removeAfter(5, TimeUnit.MINUTES).addRemoveHandler(() -> {
                            message.removeAllReactions();
                            navigator.reset();
                        });
                    });
        } else {
            msg.reply(embed(server, new ArrayList<>(), 1));
        }
    }

    private EmbedBuilder embed(Server server, ArrayList<FeedModel> objects, int page) {
        EmbedBuilder embed = new Embed().setTitle(server.getName() + "'s feeds").setFooter("Page: " + page)
                .setDescription(!objects.isEmpty() ? "Here are the feeds registered on the server." : "The server has no feeds registered.").build();
        if (!objects.isEmpty()) {
            for (FeedModel object : objects) {
                embed.addField("[" + object.getUnique() + "] " + object.getName(), "\n" +
                        "\nLink: " + object.getFeedURL() +
                        "\nFeed Unique ID: `" + object.getUnique() +
                        "`\nFeed ID: `" + object.getId() +
                        "`\nFeed Name: `" + object.getName() +
                        "`\nRoles Subscribed: " + Amelia.getMentions(object.getMentions(), server) +
                        "\nLast Update: `" + object.getDate().toString() +
                        "`\nAssigned Channel: " + server.getTextChannelById(object.getChannel())
                        .map(ServerTextChannel::getMentionTag).orElse("Unknown (possibly deleted?)") +
                        "\nCreated by: " + server.getMemberById(object.getUser()).map(User::getMentionTag).orElse("Unknown (possibly left?)"));
            }
        }

        return embed.setFooter("Please use the Unique ID for removing feeds, etc.");
    }
}
