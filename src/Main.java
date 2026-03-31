import org.dreambot.api.methods.container.impl.Inventory;
import org.dreambot.api.methods.container.impl.bank.Bank;
import org.dreambot.api.methods.container.impl.equipment.Equipment;
import org.dreambot.api.methods.interactive.GameObjects;
import org.dreambot.api.methods.interactive.Players;
import org.dreambot.api.methods.skills.Skill;
import org.dreambot.api.methods.skills.Skills;
import org.dreambot.api.methods.input.Camera;
import org.dreambot.api.methods.walking.impl.Walking;
import org.dreambot.api.methods.map.Tile;
import org.dreambot.api.methods.grandexchange.LivePrices;
import org.dreambot.api.methods.tabs.Tab;
import org.dreambot.api.methods.tabs.Tabs;
import org.dreambot.api.methods.worldhopper.WorldHopper;
import org.dreambot.api.methods.world.Worlds;
import org.dreambot.api.methods.world.World;
import org.dreambot.api.script.AbstractScript;
import org.dreambot.api.script.ScriptManifest;
import org.dreambot.api.script.Category;
import org.dreambot.api.utilities.Sleep;
import org.dreambot.api.wrappers.interactive.GameObject;

import java.awt.*;
import java.util.Random;

@ScriptManifest(
        name = "Progressive Woodcutter",
        description = "Woodcutting progressif F2P",
        author = "stygm",
        version = 1.0,
        category = Category.WOODCUTTING
)
public class Main extends AbstractScript {

    // --- AXES ---
    String[] AXES       = { "Rune axe", "Adamant axe", "Mithril axe", "Bronze axe" };
    int[]    AXE_LEVELS = { 41, 31, 21, 1 };

    // --- ARBRES PAR NIVEAU ---
    String[] TREE_NAMES  = { "Yew tree", "Willow tree", "Oak tree", "Tree" };
    int[]    TREE_LEVELS = { 60, 30, 15, 1 };

    // --- ZONES DE COUPE ---
    Tile TILE_REGULAR = new Tile(3184, 3426, 0);
    Tile TILE_OAK     = new Tile(3186, 3418, 0);
    Tile TILE_WILLOW  = new Tile(3086, 3235, 0);
    Tile TILE_YEW     = new Tile(3227, 3209, 0);

    // --- VARIABLES INTERFACE ---
    String currentAction = "Demarrage...";
    String currentTree   = "Aucun";
    String currentZone   = "Lumbridge";
    long   startTime     = System.currentTimeMillis();
    int    logsChopped   = 0;
    int    totalProfit   = 0;

    // --- ANTI BAN : timer ---
    long lastAntiBan      = 0;
    int  nextAntiBanDelay = 0;

    // --- WORLD HOP : timer ---
    // Entre 30 et 40 minutes en ms
    long lastHop      = 0;
    int  nextHopDelay = 0;

    Random random = new Random();

    // --- HELPERS ---
    private int getWCLevel() {
        return Skills.getRealLevel(Skill.WOODCUTTING);
    }

    private String getBestTree() {
        int lvl = getWCLevel();
        for (int i = 0; i < TREE_LEVELS.length; i++) {
            if (lvl >= TREE_LEVELS[i]) return TREE_NAMES[i];
        }
        return "Tree";
    }

    private Tile getBestTile() {
        switch (getBestTree()) {
            case "Yew tree":    return TILE_YEW;
            case "Willow tree": return TILE_WILLOW;
            case "Oak tree":    return TILE_OAK;
            default:            return TILE_REGULAR;
        }
    }

    private String getZoneName() {
        switch (getBestTree()) {
            case "Yew tree":    return "Lumbridge Eglise";
            case "Willow tree": return "Draynor Village";
            default:            return "Lumbridge";
        }
    }

    private String getLogName() {
        switch (getBestTree()) {
            case "Yew tree":    return "Yew logs";
            case "Willow tree": return "Willow logs";
            case "Oak tree":    return "Oak logs";
            default:            return "Logs";
        }
    }

    private int getPricePerLog() {
        int price = LivePrices.get(getLogName());
        return price > 0 ? price : 1;
    }

    private boolean hasAxe() {
        for (String axe : AXES) {
            if (Inventory.contains(axe) || Equipment.contains(axe)) return true;
        }
        return false;
    }

    private String getBestAxe() {
        int lvl = getWCLevel();
        for (int i = 0; i < AXES.length; i++) {
            if (lvl >= AXE_LEVELS[i] && Bank.contains(AXES[i])) return AXES[i];
        }
        return null;
    }

    private boolean isAtTree() {
        return Players.getLocal().getTile().distance(getBestTile()) < 15;
    }

    // --- WORLD HOP ---
    private void doWorldHop() {
        long now = System.currentTimeMillis();
        if (now - lastHop < nextHopDelay) return;

        // On hoppe seulement si on ne coupe pas et inventaire pas plein
        if (Players.getLocal().isAnimating()) return;
        if (Inventory.isFull()) return;

        log("[World Hop] Changement de serveur F2P...");
        currentAction = "Change de serveur...";

        World world = Worlds.getRandomWorld(w ->
                w != null && w.isF2P() && !w.isPVP() && w.getMinimumLevel() == 0
        );

        if (world != null) {
            WorldHopper.hopWorld(world);
            Sleep.sleep(4000 + random.nextInt(2000));
            log("[World Hop] Serveur change !");
        }

        lastHop      = System.currentTimeMillis();
        // Prochain hop entre 30 et 40 minutes
        nextHopDelay = (30 + random.nextInt(10)) * 60 * 1000;
    }

    // --- ANTI BAN NATUREL ---
    private void doAntiBan() {
        long now = System.currentTimeMillis();
        if (now - lastAntiBan < nextAntiBanDelay) return;

        int roll = random.nextInt(100);

        if (roll < 25) {
            log("[Anti-ban] Regarde les skills");
            Tabs.open(Tab.SKILLS);
            Sleep.sleep(1000 + random.nextInt(2000));
            Tabs.open(Tab.INVENTORY);

        } else if (roll < 45) {
            int currentYaw = Camera.getYaw();
            int delta = random.nextInt(30) - 15;
            Camera.rotateTo(currentYaw + delta, Camera.getPitch());
            Sleep.sleep(200 + random.nextInt(300));

        } else if (roll < 58) {
            Sleep.sleep(600 + random.nextInt(1200));

        } else if (roll < 65) {
            log("[Anti-ban] Regarde l'inventaire");
            Tabs.open(Tab.INVENTORY);
            Sleep.sleep(500 + random.nextInt(800));
        }
        // 35% du temps on ne fait rien

        lastAntiBan      = System.currentTimeMillis();
        nextAntiBanDelay = 15000 + random.nextInt(30000);
    }

    @Override
    public void onStart() {
        log("Progressive Woodcutter demarre !");
        startTime        = System.currentTimeMillis();
        nextAntiBanDelay = 20000 + random.nextInt(30000);
        // Premier hop entre 30 et 40 minutes
        nextHopDelay     = (30 + random.nextInt(10)) * 60 * 1000;
        lastHop          = System.currentTimeMillis();
    }

    @Override
    public int onLoop() {

        currentZone = getZoneName();

        // World hop si le timer est pret
        doWorldHop();

        // Pas de hache → banque
        if (!hasAxe()) {
            currentAction = "Cherche une hache en banque";

            if (!Bank.isOpen()) {
                Bank.open();
                Sleep.sleepUntil(Bank::isOpen, 5000);
            }

            if (Bank.isOpen()) {
                String bestAxe = getBestAxe();
                if (bestAxe != null) {
                    Bank.withdraw(bestAxe, 1);
                    Sleep.sleepUntil(() -> Inventory.contains(bestAxe), 3000);
                } else {
                    log("Aucune hache en banque, script arrete !");
                    stop();
                }
                Bank.close();
            }
            return 600;
        }

        // Inventaire plein → banque
        if (Inventory.isFull()) {
            currentAction = "Banque les logs";

            if (!Bank.isOpen()) {
                Bank.open();
                Sleep.sleepUntil(Bank::isOpen, 5000);
            }

            if (Bank.isOpen()) {
                int logCount = Inventory.count(getLogName());
                totalProfit += logCount * getPricePerLog();
                logsChopped += logCount;

                Bank.depositAllExcept(item -> item.getName().toLowerCase().contains("axe"));
                Sleep.sleepUntil(() -> !Inventory.isFull(), 3000);
                Bank.close();
            }
            return 600;
        }

        // Pas dans la bonne zone → on marche
        if (!isAtTree()) {
            currentAction = "Marche vers " + currentZone;
            Walking.walk(getBestTile());
            Sleep.sleepUntil(this::isAtTree, 10000);
            return 600;
        }

        // Coupe l'arbre
        GameObject tree = GameObjects.closest(getBestTree());

        if (tree != null) {

            // Si le perso coupe deja, on attend sans recliquer
            if (Players.getLocal().isAnimating()) {
                currentAction = "Coupe " + currentTree;
                Sleep.sleepUntil(() -> {
                    doAntiBan();
                    return !Players.getLocal().isAnimating();
                }, 10000);
                return 500 + random.nextInt(300);
            }

            currentTree   = tree.getName();
            currentAction = "Coupe " + currentTree;
            tree.interact("Chop down");
            Sleep.sleepUntil(() -> Players.getLocal().isAnimating(), 3000);

            Sleep.sleepUntil(() -> {
                doAntiBan();
                return !Players.getLocal().isAnimating();
            }, 10000);

        } else {
            currentAction = "Cherche un arbre...";
            currentTree   = "Aucun";
        }

        return 500 + random.nextInt(300);
    }

    // --- INTERFACE ---
    @Override
    public void onPaint(Graphics2D g) {
        long elapsed = System.currentTimeMillis() - startTime;
        long seconds = elapsed / 1000;
        long minutes = seconds / 60;
        long hours   = minutes / 60;
        String time  = String.format("%02d:%02d:%02d", hours, minutes % 60, seconds % 60);

        double hoursElapsed = elapsed / 3600000.0;
        int logsPerHour     = hoursElapsed > 0 ? (int)(logsChopped / hoursElapsed) : 0;
        int profitPerHour   = hoursElapsed > 0 ? (int)(totalProfit / hoursElapsed) : 0;
        int priceNow        = getPricePerLog();

        // Prochain hop en minutes
        long timeUntilHop = (nextHopDelay - (System.currentTimeMillis() - lastHop)) / 60000;
        timeUntilHop = Math.max(0, timeUntilHop);

        // Fond
        g.setColor(new Color(0, 0, 0, 160));
        g.fillRoundRect(10, 10, 240, 210, 15, 15);

        // Bordure
        g.setColor(new Color(80, 200, 80));
        g.drawRoundRect(10, 10, 240, 210, 15, 15);

        // Titre
        g.setFont(new Font("Arial", Font.BOLD, 14));
        g.setColor(new Color(80, 200, 80));
        g.drawString("Progressive Woodcutter", 20, 32);

        // Separateur
        g.setColor(new Color(80, 200, 80, 100));
        g.drawLine(20, 38, 240, 38);

        // Infos
        g.setFont(new Font("Arial", Font.PLAIN, 12));
        g.setColor(Color.WHITE);
        g.drawString("Temps    : " + time,                                     20, 57);
        g.drawString("Zone     : " + currentZone,                              20, 74);
        g.drawString("Arbre    : " + currentTree,                              20, 91);
        g.drawString("Action   : " + currentAction,                            20, 108);
        g.drawString("Logs     : " + logsChopped + " (" + logsPerHour + "/h)", 20, 125);
        g.drawString("WC lvl   : " + getWCLevel(),                             20, 142);
        g.drawString("Prix/log : " + priceNow + " gp (GE live)",               20, 159);
        g.drawString("Hop dans : " + timeUntilHop + " min",                    20, 176);

        g.setColor(new Color(255, 215, 0));
        g.drawString("Profit   : " + totalProfit + " gp",                      20, 193);
        g.drawString("Profit/h : " + profitPerHour + " gp",                    20, 210);
    }

    @Override
    public void onExit() {
        log("Script arrete. Profit total : " + totalProfit + " gp");
    }
}