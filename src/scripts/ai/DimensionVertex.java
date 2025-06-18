package scripts.ai;

import ru.agecold.Config;
import ru.agecold.gameserver.instancemanager.GrandBossManager;
import ru.agecold.gameserver.instancemanager.bosses.BaiumManager;
import ru.agecold.gameserver.model.L2Character;
import ru.agecold.gameserver.model.actor.instance.L2NpcInstance;
import ru.agecold.gameserver.model.actor.instance.L2PcInstance;
import ru.agecold.gameserver.templates.L2NpcTemplate;
import ru.agecold.util.Rnd;

public class DimensionVertex extends L2NpcInstance {

    private static String htmPath = "data/html/teleporter/";

    public DimensionVertex(int objectId, L2NpcTemplate template) {
        super(objectId, template);
    }

    @Override
    public void onSpawn() {
        super.onSpawn();
    }

    @Override
    public void onBypassFeedback(L2PcInstance player, String command) {
        if (command.startsWith("Chat")) {
            showChatWindow(player, htmPath + getNpcId() + "-" + Integer.parseInt(command.substring(4).trim()) + ".htm");
            return;
        }

        BaiumManager bm = BaiumManager.getInstance();
        switch (bm.getStatus()) {
            case 1:
                if (GrandBossManager.getInstance().getItem(player, 4295)) {
                    GrandBossManager.getInstance().getZone(116040, 17455, 10078).allowPlayerEntry(player, 9000000);
                    player.teleToLocation((114077 + Rnd.get(200)), (15882 + Rnd.get(200)), 10078);
                    bm.notifyEnter();
                } else {
                    showChatWindow(player, htmPath + getNpcId() + "-2.htm");
                }
                break;
            case 2:
            case 3:
                showChatWindow(player, htmPath + getNpcId() + "-3.htm");
                break;
            default:
                showChatWindow(player, htmPath + getNpcId() + "-4.htm");
                break;
        }
        player.sendActionFailed();
    }
}
