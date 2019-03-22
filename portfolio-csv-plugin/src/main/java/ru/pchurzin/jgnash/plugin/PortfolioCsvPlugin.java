package ru.pchurzin.jgnash.plugin;


import javafx.application.Platform;
import javafx.scene.control.MenuBar;
import javafx.scene.control.MenuItem;
import jgnash.plugin.FxPlugin;
import jgnash.resource.util.ResourceUtils;
import jgnash.uifx.views.main.MainView;

public class PortfolioCsvPlugin implements FxPlugin {
    private static final int MENU_INDEX = 2;

    @Override
    public String getName() {
        return "Portfolio csv export plugin";
    }

    @Override
    public void start(final PluginPlatform pluginPlatform) {
        if (pluginPlatform != PluginPlatform.Fx) {
            throw new RuntimeException("Invalid platform");
        }
        final MenuItem csvExport = new MenuItem(ResourceUtils.getString("Menu.Portfolio.Name") + " (csv)");
        csvExport.setOnAction(event -> PortfolioCsvAction.exportPortfolio());
        final MenuBar menuBar = MainView.getInstance().getMenuBar();

        Platform.runLater(() -> {
            menuBar.getMenus().stream().filter(menu -> menu.getId().equals("reportMenu")).forEach(menu -> {
                menu.getItems().add(csvExport);
            });
        });
    }
}
