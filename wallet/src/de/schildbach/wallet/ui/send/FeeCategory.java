/*
 * Copyright 2015 the original author or authors.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package de.schildbach.wallet.ui.send;

import org.bitcoinj.core.Coin;

/**
 * @author Andreas Schildbach
 */
public enum FeeCategory
{
	/**
	 * We don't care when it confirms, but it should confirm at some time. Can be days or weeks.
	 */
	ECONOMIC(Coin.valueOf(5000)), // 0.00005 FEC

	/**
	 * Under normal network conditions, confirms within the next hour. Can take longer, but this should be an
	 * exception. And it should not take days or weeks.
	 */
	NORMAL(Coin.valueOf(10000000)), // 0.1 FEC

	/**
	 * Confirms within the next minute.
	 */
	PRIORITY(Coin.valueOf(2000000000)); // 20 FEC

	public final Coin feePerKb;

	private FeeCategory(final Coin feePerKb)
	{
		this.feePerKb = feePerKb;
	}
}
