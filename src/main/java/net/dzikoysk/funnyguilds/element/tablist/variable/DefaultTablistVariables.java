package net.dzikoysk.funnyguilds.element.tablist.variable;

import net.dzikoysk.funnyguilds.FunnyGuilds;
import net.dzikoysk.funnyguilds.basic.guild.Guild;
import net.dzikoysk.funnyguilds.basic.guild.GuildUtils;
import net.dzikoysk.funnyguilds.basic.user.User;
import net.dzikoysk.funnyguilds.basic.user.UserUtils;
import net.dzikoysk.funnyguilds.data.configs.MessageConfiguration;
import net.dzikoysk.funnyguilds.data.configs.PluginConfiguration;
import net.dzikoysk.funnyguilds.element.tablist.variable.impl.GuildDependentTablistVariable;
import net.dzikoysk.funnyguilds.element.tablist.variable.impl.SimpleTablistVariable;
import net.dzikoysk.funnyguilds.element.tablist.variable.impl.TimeFormattedVariable;
import net.dzikoysk.funnyguilds.hook.PluginHook;
import net.dzikoysk.funnyguilds.util.IntegerRange;
import net.dzikoysk.funnyguilds.util.commons.ChatUtils;
import net.dzikoysk.funnyguilds.util.commons.bukkit.MinecraftServerUtils;
import org.apache.commons.lang3.StringUtils;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.time.format.TextStyle;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

public final class DefaultTablistVariables {

    private static final Map<String, TablistVariable> FUNNY_VARIABLES = new ConcurrentHashMap<>();
    private static final Locale POLISH_LOCALE = new Locale("pl", "PL");

    private DefaultTablistVariables() {}

    public static Map<String, TablistVariable> getFunnyVariables() {
        if (FUNNY_VARIABLES.isEmpty()) {
            createFunnyVariables();
        }
        
        return new ConcurrentHashMap<>(FUNNY_VARIABLES);
    }
    
    public static void clearFunnyVariables() {
        FUNNY_VARIABLES.clear();
    }
    
    public static void install(TablistVariablesParser parser) {
        parser.add(new TimeFormattedVariable("HOUR", (user, time) -> time.getHour()));
        parser.add(new TimeFormattedVariable("MINUTE", (user, time) -> time.getMinute()));
        parser.add(new TimeFormattedVariable("SECOND", (user, time) -> time.getSecond()));
        parser.add(new TimeFormattedVariable("DAY_OF_WEEK", (user, time) -> time.getDayOfWeek().getDisplayName(TextStyle.FULL, POLISH_LOCALE)));
        parser.add(new TimeFormattedVariable("DAY_OF_MONTH", (user, time) -> time.getDayOfMonth()));
        parser.add(new TimeFormattedVariable("MONTH", (user, time) -> time.getMonth().getDisplayName(TextStyle.FULL, POLISH_LOCALE)));
        parser.add(new TimeFormattedVariable("MONTH_NUMBER", (user, time) -> time.getMonthValue()));
        parser.add(new TimeFormattedVariable("YEAR", (user, time) -> time.getYear()));

        parser.add(new SimpleTablistVariable("TPS", user -> {
            try {
                return MinecraftServerUtils.getRecentTPS(0);
            } catch (IntegerRange.MissingFormatException missingFormatException) {
                return "0";
            }
        }));
        
        parser.add(new SimpleTablistVariable("WORLD", user -> {
            Player userPlayer = user.getPlayer();

            if (userPlayer == null) {
                return "";
            }
            
            return userPlayer.getWorld().getName();
        }));
        
        parser.add(new SimpleTablistVariable("ONLINE", user -> {
            Player userPlayer = user.getPlayer();

            if (userPlayer == null) {
                return "";
            }
            
            return Bukkit.getOnlinePlayers().stream().filter(onlinePlayer -> onlinePlayer != null && userPlayer.canSee(onlinePlayer)).count();
        }));

        for (TablistVariable variable : getFunnyVariables().values()) {
            parser.add(variable);
        }
        
        if (PluginHook.isPresent(PluginHook.PLUGIN_WORLDGUARD)) {
            String wgRegionNoValue = FunnyGuilds.getInstance().getMessageConfiguration().wgRegionNoValue;

            parser.add(new SimpleTablistVariable("WG-REGION", user -> {
                List<String> regionNames = getWorldGuardRegionNames(user);
                return regionNames != null && !regionNames.isEmpty() ? regionNames.get(0) : wgRegionNoValue;
            }));

            parser.add(new SimpleTablistVariable("WG-REGIONS", user -> {
                List<String> regionNames = getWorldGuardRegionNames(user);
                return regionNames != null && !regionNames.isEmpty() ? StringUtils.join(regionNames, ", ") : wgRegionNoValue;
            }));
        }
    }

    private static void createFunnyVariables() {
        PluginConfiguration config = FunnyGuilds.getInstance().getPluginConfiguration();
        MessageConfiguration messages = FunnyGuilds.getInstance().getMessageConfiguration();

        putSimple("player", "PLAYER", User::getName);
        putSimple("guilds", "GUILDS", user -> GuildUtils.getGuilds().size());
        putSimple("users", "USERS", user -> UserUtils.getUsers().size());
        putSimple("ping", "PING", User::getPing);
        putSimple("points", "POINTS", user -> user.getRank().getPoints());
        putSimple("position", "POSITION", user -> user.getRank().getPosition());
        putSimple("kills", "KILLS", user -> user.getRank().getKills());
        putSimple("deaths", "DEATHS", user -> user.getRank().getDeaths());
        putSimple("kdr", "KDR", user -> String.format(Locale.US, "%.2f", user.getRank().getKDR()));

        putSimple("ping-format", "PING-FORMAT", user ->
                IntegerRange.inRangeToString(user.getPing(), config.pingFormat)
                        .replace("{PING}", String.valueOf(user.getPing())));

        putSimple("points-format", "POINTS-FORMAT", user ->
                IntegerRange.inRangeToString(user.getRank().getPoints(), config.pointsFormat)
                        .replace("{POINTS}", String.valueOf(user.getRank().getPoints())));

        FUNNY_VARIABLES.put("g-name", GuildDependentTablistVariable.ofGuild("G-NAME", Guild::getName, user -> messages.gNameNoValue));
        FUNNY_VARIABLES.put("g-tag", GuildDependentTablistVariable.ofGuild("G-TAG", Guild::getTag, user -> messages.gTagNoValue));
        FUNNY_VARIABLES.put("g-owner", GuildDependentTablistVariable.ofGuild("G-OWNER", guild -> guild.getOwner().getName(), user -> messages.gOwnerNoValue));

        FUNNY_VARIABLES.put("g-deputies", GuildDependentTablistVariable.ofGuild("G-DEPUTIES",
                guild -> guild.getDeputies().isEmpty()
                        ? messages.gDeputiesNoValue
                        : ChatUtils.toString(UserUtils.getNames(guild.getDeputies()), false),
                user -> messages.gDeputiesNoValue));

        FUNNY_VARIABLES.put("g-deputy", GuildDependentTablistVariable.ofGuild("G-DEPUTY",
                guild -> guild.getDeputies().isEmpty()
                        ? messages.gDeputyNoValue
                        : guild.getDeputies().iterator().next().getName(),
                user -> messages.gDeputyNoValue));

        //FUNNY_VARIABLES.put("g-deputy", GuildDependentTablistVariable.ofGuild("G-DEPUTY", guild -> guild.getDeputies().isEmpty() ? messages.gDeputyNoValue : guild.getDeputies().iterator().next(RandomUtils.RANDOM_INSTANCE.nextInt(guild.getDeputies().size())).getName(), user -> messages.gDeputyNoValue));

        putGuild("g-lives", "G-LIVES", user -> user.getGuild().getLives(), user -> "0");
        putGuild("g-allies", "G-ALLIES", user -> user.getGuild().getAllies().size(), user -> "0");
        putGuild("g-points", "G-POINTS", user -> user.getGuild().getRank().getPoints(), user -> "0");
        putGuild("g-kills", "G-KILLS", user -> user.getGuild().getRank().getKills(), user -> "0");
        putGuild("g-deaths", "G-DEATHS", user -> user.getGuild().getRank().getDeaths(), user -> "0");
        putGuild("g-kdr", "G-KDR", user -> String.format(Locale.US, "%.2f", user.getGuild().getRank().getKDR()), user -> "0.00");
        putGuild("g-members-online", "G-MEMBERS-ONLINE", user -> user.getGuild().getOnlineMembers().size(), user -> "0");
        putGuild("g-members-all", "G-MEMBERS-ALL", user -> user.getGuild().getMembers().size(), user -> "0");

        putGuild("g-validity", "G-VALIDITY",
                user -> FunnyGuilds.getInstance().getPluginConfiguration().dateFormat.format(user.getGuild().getValidityDate()),
                user -> messages.gValidityNoValue);

        putGuild("g-points-format", "G-POINTS-FORMAT",
                user -> IntegerRange.inRangeToString(user.getGuild().getRank().getPoints(), config.pointsFormat)
                            .replace("{POINTS}", String.valueOf(user.getGuild().getRank().getPoints())),
                user -> IntegerRange.inRangeToString(0, config.pointsFormat)
                            .replace("{POINTS}", "0"));

        putGuild("g-position", "G-POSITION",
                user -> user.getGuild().getMembers().size() >= FunnyGuilds.getInstance().getPluginConfiguration().minMembersToInclude
                        ? String.valueOf(user.getGuild().getRank().getPosition())
                        : messages.minMembersToIncludeNoValue,
                user -> messages.minMembersToIncludeNoValue);

        putGuild("g-region-size", "G-REGION-SIZE",
                user -> FunnyGuilds.getInstance().getPluginConfiguration().regionsEnabled
                        ? String.valueOf(user.getGuild().getRegion().getSize())
                        : messages.gRegionSizeNoValue,
                user -> messages.gRegionSizeNoValue);
    }

    private static void putSimple(String variable, String placeholder, Function<User, Object> function) {
        FUNNY_VARIABLES.put(variable, new SimpleTablistVariable(placeholder, function));
    }

    private static void putGuild(String variable, String placeholder, Function<User, Object> whenInGuild, Function<User, Object> whenNotInGuild) {
        FUNNY_VARIABLES.put(variable, new GuildDependentTablistVariable(placeholder, whenInGuild, whenNotInGuild));
    }

    private static List<String> getWorldGuardRegionNames(User user) {
        if (user == null || user.getPlayer() == null) {
            return Collections.emptyList();
        }

        Location location = user.getPlayer().getLocation();
        List<String> regionNames = PluginHook.WORLD_GUARD.getRegionNames(location);

        if (regionNames != null && ! regionNames.isEmpty()) {
            return regionNames;
        }

        return null;
    }

}
