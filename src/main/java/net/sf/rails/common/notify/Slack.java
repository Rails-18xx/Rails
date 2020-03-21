package net.sf.rails.common.notify;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpHeaders;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.sf.rails.common.Config;
import net.sf.rails.game.Player;
import net.sf.rails.game.PlayerManager;
import net.sf.rails.game.RailsRoot;
import net.sf.rails.game.state.Observable;
import net.sf.rails.game.state.Observer;

public class Slack {
    private static final Logger log = LoggerFactory.getLogger(Slack.class);

    private static final Slack instance = new Slack();

    private CloseableHttpClient httpClient = null;

    private String webhook = null;
    private Map<String, String> playerNameMappings = new HashMap<>();
    private String body = null;
    private static final String messageTemplate = "Your turn @@";
    private static final String bodyTemplate = "{\"text\":\"@@\"}";

    public void setConfig() {
        webhook = StringUtils.trimToNull(Config.get("notify.slack.webhook"));
        String message = StringUtils.defaultIfBlank(Config.get("notify.message"), messageTemplate);
        body = StringUtils.replace(bodyTemplate, "@@", message);

        parseUserMappings(Config.get("notify.slack.user_mapping"));
    }

    public void parseUserMappings(String mappings) {
        if ( StringUtils.isBlank(mappings) ) {
            return;
        }
        playerNameMappings = Arrays.stream(mappings.split(","))
                .map(s -> s.split(":"))
                .collect(Collectors.toMap(e -> e[0], e -> "<@" + e[1] + ">"));
    }

    private class CurrentPlayerModelObserver implements Observer {
        private Player formerCurrentPlayer = null;
        private final PlayerManager pm;

        public CurrentPlayerModelObserver(PlayerManager pm) {
            this.pm = pm;
            if ( pm != null ) {
                formerCurrentPlayer = pm.getCurrentPlayer();
            }
        }

        public void update(String text) {
            String localPlayer = Config.get("local.player.name");
            log.debug("Slack called with f:{}/c:{}/l:{}", formerCurrentPlayer.getId(), pm.getCurrentPlayer().getId(), localPlayer);
            if ( formerCurrentPlayer != pm.getCurrentPlayer() ) {
                if ( formerCurrentPlayer.getId().equals(localPlayer ) ) {
                    // only alert if we are transitioning from the former player
                    if ( !localPlayer.equals(pm.getCurrentPlayer().getId()) ) {
                        // only send a notification if we are switching to a different user, ie not ourselves
                        sendMessage(pm.getCurrentPlayer().getId());
                    }
                }
                formerCurrentPlayer = pm.getCurrentPlayer();
            }
        }

        public Observable getObservable() {
            return pm.getCurrentPlayerModel();
        }
    }

    public static void notifyOfGameInit(final RailsRoot root) {
        instance.init(root);
    }

    private void init(final RailsRoot root) {
        httpClient = HttpClients.createDefault();

        final PlayerManager pm = root.getPlayerManager();
        if ( pm.getCurrentPlayerModel() != null ) {
            pm.getCurrentPlayerModel().addObserver(new CurrentPlayerModelObserver(pm));
        }
    }

    public void sendMessage(String player) {
        setConfig();
        if ( webhook == null ) {
            return;
        }
        String mapped = StringUtils.defaultIfBlank(playerNameMappings.get(player), player);
        String formatted = StringUtils.replace(body, "@@", mapped);
        log.debug("Sending message '{}' to Slack for user {}", formatted, player);

        HttpPost httpPost = new HttpPost(webhook);
        try {
            httpPost.setEntity(new StringEntity(formatted));
            httpPost.setHeader(HttpHeaders.CONTENT_TYPE, "application/json");
            httpPost.setHeader(HttpHeaders.USER_AGENT, "18xx Rails");
            CloseableHttpResponse response = httpClient.execute(httpPost);
            if ( response.getStatusLine().getStatusCode() != HttpStatus.SC_NO_CONTENT ) {
                // TODO: verify result
                log.debug("Unexpected Slack response: {}", response.toString());
            }
            response.close();
        }
        catch (IOException e) {
            log.error("Error sending message to Slack", e);
        }
    }
}
