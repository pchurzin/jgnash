package ru.pchurzin.jgnash.plugin;

import javafx.stage.FileChooser;
import jgnash.engine.*;
import jgnash.resource.util.ResourceUtils;
import jgnash.uifx.StaticUIMethods;
import jgnash.uifx.views.main.MainView;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.ResourceBundle;
import java.util.prefs.Preferences;

public class PortfolioCsvAction {
    private static final String LAST_DIR = "lastDir";
    private static final List<String> columnNames = new ArrayList<>();

    static {
        ResourceBundle bundle = ResourceUtils.getBundle();
        columnNames.add(bundle.getString("Column.Security"));
        columnNames.add(bundle.getString("Column.Short.Quantity"));
        columnNames.add(bundle.getString("Column.CostBasis"));
        columnNames.add(bundle.getString("Column.TotalCostBasis"));
        columnNames.add(bundle.getString("Column.Price"));
        columnNames.add(bundle.getString("Column.MktValue"));
        columnNames.add(bundle.getString("Column.Short.UnrealizedGain"));
        columnNames.add(bundle.getString("Column.Short.RealizedGain"));
        columnNames.add(bundle.getString("Column.Short.TotalGain"));
        columnNames.add(bundle.getString("Column.Short.TotalGainPercentage"));
        columnNames.add(bundle.getString("Column.Short.InternalRateOfReturn"));
//        columnNames.add(bundle.getString("Column.Short.PercentagePortfolio"));

    }

    static void exportPortfolio() {
        final Engine engine = EngineFactory.getEngine(EngineFactory.DEFAULT);
        Objects.requireNonNull(engine);

        final Preferences preferences = Preferences.userNodeForPackage(PortfolioCsvPlugin.class);


        final FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle(ResourceUtils.getString("Title.SaveFile"));

        final String lastDir = preferences.get(LAST_DIR, null);
        if (lastDir != null) {
            fileChooser.setInitialDirectory(new File(lastDir));
        }

        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("CSV", "*.csv")
        );

        final File file = fileChooser.showSaveDialog(MainView.getPrimaryStage());

        if (file == null) {
            return;
        }

        preferences.put(LAST_DIR, file.getParent());

        CurrencyNode baseCurrency = engine.getDefaultCurrency();

        try (final BufferedWriter writer = Files.newBufferedWriter(Paths.get(file.getAbsolutePath()), StandardCharsets.UTF_8)) {
            for (int i = 0; i < columnNames.size(); i++) {
                if (i > 0) {
                    writer.write(",");
                }
                writer.write(columnNames.get(i));
            }
            writer.newLine();
            for (Account a : engine.getInvestmentAccountList()) {
                InvestmentPerformanceSummary performanceSummary = new InvestmentPerformanceSummary(a, true);
                for (SecurityNode sn : performanceSummary.getSecurities()) {
                    InvestmentPerformanceSummary.SecurityPerformanceData pd = performanceSummary.getPerformanceData(sn);
                    writer.write(pd.getNode().getSymbol());
                    writer.write(",");
                    writer.write(pd.getSharesHeld().toString());
                    writer.write(",");
                    writer.write(pd.getCostBasisPerShare().toString());
                    writer.write(",");
                    writer.write(pd.getHeldCostBasis().toString());
                    writer.write(",");
                    writer.write(pd.getPrice(baseCurrency).toString());
                    writer.write(",");
                    writer.write(pd.getMarketValue(baseCurrency).toString());
                    writer.write(",");
                    writer.write(pd.getUnrealizedGains().toString());
                    writer.write(",");
                    writer.write(pd.getRealizedGains().toString());
                    writer.write(",");
                    writer.write(pd.getTotalGains().toString());
                    writer.write(",");
                    writer.write(pd.getTotalGainsPercentage().toString());
                    double irr = pd.getInternalRateOfReturn();
                    String sIrr = (Double.isNaN(irr)) ? null : BigDecimal.valueOf(irr).toString();
                    writer.write(",");
                    writer.write(sIrr);
                    writer.newLine();
                }
            }
        } catch (IOException e) {
            StaticUIMethods.displayError(e.getMessage());
        }
    }
}
