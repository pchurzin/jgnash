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

import jgnash.engine.Transaction;
import jgnash.util.NotNull;

/**
 * @author Craig Cavanaugh
 */
public interface Slip {

    void modifyTransaction(@NotNull final Transaction transaction);

    void clearForm();

    void handleCancelAction();

    void handleEnterAction();

    /**
     * Validates the form
     *
     * @return return {@code true} if the form entry is valid, {@code false} otherwise
     */
    boolean validateForm();

    /**
     * Builds and returns a new {@code Transaction} based on form contents
     *
     * @return new {@code Transaction} instance
     */
    @NotNull
    Transaction buildTransaction();
}
