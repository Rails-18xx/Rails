package net.sf.rails.common.notify;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.text.StrSubstitutor;
import org.apache.commons.text.StringSubstitutor;
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
import net.sf.rails.ui.swing.GameUIManager;

public class Discord {
    private static final Logger log = LoggerFactory.getLogger(Discord.class);

    private final RailsRoot root;
    private final GameUIManager gameUiManager;

    private CurrentPlayerModelObserver observer;

    private final CloseableHttpClient httpClient;

    private String webhook = null;
    private Map<String, String> playerNameMappings = new HashMap<>();
    private String body = null;
    private static final String MESSAGE_TEMPLATE = "Your turn ${current}";
    private static final String BODY_TEMPLATE = "{\"content\":\"@@\", \"username\":\"Rails\"}";

    public void setConfig() {
        webhook = StringUtils.trimToNull(Config.get("notify.discord.webhook"));
        String message = StringUtils.defaultIfBlank(Config.get("notify.message"), MESSAGE_TEMPLATE);
        body = StringUtils.replace(BODY_TEMPLATE, "@@", message);

        parseUserMappings(Config.get("notify.discord.user_mapping"));
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
                // this is often incorrect during a file load
                formerCurrentPlayer = pm.getCurrentPlayer();
            }
        }

        public void update(String text) {
            String localPlayer = Config.get("local.player.name");
            log.debug("Discord called with f:{}/c:{}/l:{}", formerCurrentPlayer.getId(), pm.getCurrentPlayer().getId(), localPlayer);
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

        public Player getFormerPlayer() {
            return formerCurrentPlayer;
        }
    }

    public Discord(final GameUIManager gameUIManger, final RailsRoot root) {
        this.gameUiManager = gameUIManger;
        this.root = root;
        httpClient = HttpClients.createDefault();

        final PlayerManager pm = root.getPlayerManager();
        if ( pm.getCurrentPlayerModel() != null ) {
            observer = new CurrentPlayerModelObserver(pm);
            pm.getCurrentPlayerModel().addObserver(observer);
        }
    }

    public void sendMessage(String player) {
        setConfig();
        if ( webhook == null ) {
            return;
        }
        Map<String, String> keys = new HashMap<>();
        keys.put("game", root.getGameName());
        //keys.put("gameName", StringUtils.defaultIfBlank(root.getGameData().getUsersGameName(), "[none]"));
        keys.put("round", gameUiManager.getCurrentRound().getRoundName());
        keys.put("current", StringUtils.defaultIfBlank(playerNameMappings.get(player), player));
        keys.put("previous", StringUtils.defaultIfBlank(observer.getFormerPlayer().getId(), "[none]"));

        String msgBody = StringSubstitutor.replace(body, keys);
        log.debug("Sending message '{}' to Discord for user {}", msgBody, player);

        HttpPost httpPost = new HttpPost(webhook);
        try {
            httpPost.setEntity(new StringEntity(msgBody));
            httpPost.setHeader(HttpHeaders.CONTENT_TYPE, "application/json");
            httpPost.setHeader(HttpHeaders.USER_AGENT, "18xx Rails");
            CloseableHttpResponse response = httpClient.execute(httpPost);
            if ( response.getStatusLine().getStatusCode() != HttpStatus.SC_NO_CONTENT ) {
                log.debug("Unexpected Discord  response: {}", response);
            }
            response.close();
        }
        catch (IOException e) {
            log.error("Error sending message to Discord", e);
        }
    }
}
