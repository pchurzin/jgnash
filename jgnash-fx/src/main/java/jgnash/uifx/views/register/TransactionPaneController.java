/*
 * jGnash, a personal finance application
 * Copyright (C) 2001-2015 Craig Cavanaugh
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package jgnash.uifx.views.register;

import java.net.URL;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.TextField;

import jgnash.engine.Account;
import jgnash.engine.InvestmentTransaction;
import jgnash.engine.ReconciledState;
import jgnash.engine.Transaction;
import jgnash.engine.TransactionEntry;
import jgnash.engine.TransactionType;
import jgnash.uifx.Options;
import jgnash.uifx.StaticUIMethods;
import jgnash.uifx.control.DatePickerEx;
import jgnash.uifx.control.DecimalTextField;
import jgnash.uifx.control.TransactionNumberComboBox;

/**
 * Transaction Entry Controller for Credits and Debits
 *
 * @author Craig Cavanaugh
 */
public class TransactionPaneController implements Initializable {

    @FXML
    protected TextField payeeTextField;

    @FXML
    protected Button splitsButton;

    @FXML
    protected TransactionNumberComboBox numberComboBox;

    @FXML
    protected DatePickerEx datePicker;

    @FXML
    protected DecimalTextField amountField;

    @FXML
    protected TextField memoTextField;

    @FXML
    protected AccountExchangePane accountExchangePane;

    @FXML
    protected CheckBox reconciledButton;

    @FXML
    private AttachmentPane attachmentPane;

    private ResourceBundle resources;

    final private ObjectProperty<Account> accountProperty = new SimpleObjectProperty<>();

    private PanelType panelType;

    private SplitTransactionDialog splitsDialog;

    private Transaction modTrans = null;

    private TransactionEntry modEntry = null;

    @Override
    public void initialize(final URL location, final ResourceBundle resources) {
        this.resources = resources;

        // Number combo needs to know the account in order to determine the next transaction number
        numberComboBox.getAccountProperty().bind(getAccountProperty());

        // Bind necessary properties to the exchange panel
        accountExchangePane.getBaseAccountProperty().bind(getAccountProperty());
        accountExchangePane.getAmountProperty().bindBidirectional(amountField.decimalProperty());
        accountExchangePane.getAmountEditable().bind(amountField.editableProperty());
    }

    ObjectProperty<Account> getAccountProperty() {
        return accountProperty;
    }

    void setPanelType(final PanelType panelType) {
        this.panelType = panelType;
    }

    public void modifyTransaction(final Transaction transaction) {
        if (transaction.areAccountsLocked()) {
            clearForm();
            StaticUIMethods.displayError(resources.getString("Message.TransactionModifyLocked"));
            return;
        }

        newTransaction(transaction); // load the form

        modTrans = transaction; // save reference to old transaction
        modTrans = attachmentPane.modifyTransaction(modTrans);

        if (!canModifyTransaction(transaction) && transaction.getTransactionType() == TransactionType.SPLITENTRY) {
            for (final TransactionEntry entry : transaction.getTransactionEntries()) {
                if (entry.getCreditAccount().equals(getAccountProperty().get()) || entry.getDebitAccount().equals(getAccountProperty().get())) {
                    modEntry = entry;
                    break;
                }
            }

            if (modEntry == null) {
                Logger logger = Logger.getLogger(TransactionPaneController.class.getName());
                logger.warning("Was not able to modify the transaction");
            }
        }
    }

    void newTransaction(final Transaction t) {
        clearForm();

        amountField.setDecimal(t.getAmount(getAccountProperty().get()).abs());

        memoTextField.setText(t.getMemo());
        payeeTextField.setText(t.getPayee());
        numberComboBox.setValue(t.getNumber());

        // JPA may slip in a java.sql.Date which throws an exception when .toInstance is called. Wrap in a new java.util.Date instance
        datePicker.setValue(new Date(t.getDate().getTime()).toInstant().atZone(ZoneId.systemDefault()).toLocalDate());
        reconciledButton.setSelected(t.getReconciled(getAccountProperty().get()) != ReconciledState.NOT_RECONCILED);

        if (t.getTransactionType() == TransactionType.SPLITENTRY) {
            accountExchangePane.setSelectedAccount(t.getCommonAccount()); // display common account
            accountExchangePane.setEnabled(false);

            if (canModifyTransaction(t)) { // split as the same base account

                //  clone the splits for modification
                getSplitsDialog().getTransactionEntries().clear();

                for (TransactionEntry entry : t.getTransactionEntries()) {
                    try {
                        getSplitsDialog().getTransactionEntries().add((TransactionEntry) entry.clone());
                    } catch (CloneNotSupportedException e) {
                        Logger.getLogger(TransactionPaneController.class.getName()).log(Level.SEVERE, e.getLocalizedMessage(), e);
                    }
                }
                amountField.setEditable(false);
                amountField.setDecimal(t.getAmount(getAccountProperty().get()).abs());
            } else { // not the same common account, can only modify the entry
                splitsButton.setDisable(true);
                payeeTextField.setEditable(false);
                numberComboBox.setDisable(true);
                datePicker.setEditable(false);

                amountField.setEditable(true);
                amountField.setDecimal(t.getAmount(getAccountProperty().get()).abs());

                for (TransactionEntry entry : t.getTransactionEntries()) {
                    if (entry.getCreditAccount() == getAccountProperty().get()) {
                        accountExchangePane.setExchangedAmount(entry.getDebitAmount().abs());
                        break;
                    } else if (entry.getDebitAccount() == getAccountProperty().get()) {
                        accountExchangePane.setExchangedAmount(entry.getCreditAmount());
                        break;
                    }
                }
            }
        } else if (t instanceof InvestmentTransaction) {
            Logger logger = Logger.getLogger(TransactionPaneController.class.getName());
            logger.warning("unsupported transaction type");
        } else { // DoubleEntryTransaction
            accountExchangePane.setEnabled(!t.areAccountsHidden());

            amountField.setDisable(false);
            datePicker.setEditable(true);
        }

        // setup the accountCombo correctly
        if (t.getTransactionType() == TransactionType.DOUBLEENTRY) {
            TransactionEntry entry = t.getTransactionEntries().get(0);

            if (panelType == PanelType.DECREASE) {
                accountExchangePane.setSelectedAccount(entry.getCreditAccount());
                accountExchangePane.setExchangedAmount(entry.getCreditAmount());
            } else {
                accountExchangePane.setSelectedAccount(entry.getDebitAccount());
                accountExchangePane.setExchangedAmount(entry.getDebitAmount().abs());
            }
        }
    }

    void clearForm() {
        getSplitsDialog().getTransactionEntries().clear();

        modEntry = null;
        modTrans = null;

        amountField.setEditable(true);
        amountField.setDecimal(null);

        accountExchangePane.setEnabled(true);
        accountExchangePane.setExchangedAmount(null);

        splitsButton.setDisable(false);

        reconciledButton.setDisable(false);
        reconciledButton.setSelected(false);

        payeeTextField.setEditable(true);
        payeeTextField.setText(null);

        datePicker.setEditable(true);
        if (!Options.getRememberLastDate()) {
            datePicker.setValue(LocalDate.now());
        }

        memoTextField.setText(null);

        numberComboBox.setValue(null);
        numberComboBox.setDisable(false);

        attachmentPane.clear();
    }

    protected boolean canModifyTransaction(final Transaction t) {
        boolean result = false;

        switch (t.getTransactionType()) {
            case DOUBLEENTRY:
                result = true;
                break;
            case SPLITENTRY:
                if (t.getCommonAccount().equals(accountProperty.get())) {
                    result = true;
                }
                break;
            default:
                break;
        }

        return result;
    }

    @FXML
    private void okAction() {
    }

    @FXML
    private void cancelAction() {
        clearForm();
    }

    private SplitTransactionDialog getSplitsDialog() {
        if (splitsDialog == null) { // Lazy init
            splitsDialog = new SplitTransactionDialog();
            splitsDialog.getAccountProperty().setValue(getAccountProperty().get());
        }
        return splitsDialog;
    }

    @FXML
    private void splitsAction() {
        getSplitsDialog().showAndWait();
    }
}
