/* This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2, or (at your option)
 * any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA
 * 02111-1307, USA.
 *
 * http://www.gnu.org/copyleft/gpl.html
 */
package ru.agecold.gameserver.model;

import javolution.util.FastList;

/**
 *
 * @author -Nemesiss-
 */
public class L2ExtractableItem
{
	private final int _itemId;
	private final L2ExtractableProductItem[] _products;

	public L2ExtractableItem(int itemid, FastList<L2ExtractableProductItem> products)
	{
		_itemId = itemid;
		_products = new L2ExtractableProductItem[products.size()];
		products.toArray(_products);
	}

	public int getItemId()
	{
		return _itemId;
	}

	public L2ExtractableProductItem[] getProductItemsArray()
	{
		return _products;
	}

}
