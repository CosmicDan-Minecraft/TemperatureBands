package github.cosmicdan.temperaturebands;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.worldselection.CreateWorldScreen;

public class TbUtilsClient {
    public static boolean isOnWorldCreateScreen() {
        Screen currentScreen = Minecraft.getInstance().screen;
        if (currentScreen != null) {
            return (currentScreen instanceof CreateWorldScreen);
        }
        return false;
    }
}
