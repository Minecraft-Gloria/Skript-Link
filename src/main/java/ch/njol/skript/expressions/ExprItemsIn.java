/**
 *   This file is part of Skript.
 *
 *  Skript is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  Skript is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with Skript.  If not, see <http://www.gnu.org/licenses/>.
 *
 * Copyright Peter Güttinger, SkriptLang team and contributors
 */
package ch.njol.skript.expressions;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.NoSuchElementException;

import ch.njol.skript.aliases.ItemType;
import org.bukkit.event.Event;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.eclipse.jdt.annotation.Nullable;

import ch.njol.skript.Skript;
import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Examples;
import ch.njol.skript.doc.Name;
import ch.njol.skript.doc.Since;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.ExpressionType;
import ch.njol.skript.lang.SkriptParser.ParseResult;
import ch.njol.skript.lang.Variable;
import ch.njol.skript.lang.util.SimpleExpression;
import ch.njol.skript.util.slot.InventorySlot;
import ch.njol.skript.util.slot.Slot;
import ch.njol.util.Kleenean;

@Name("Items In")
@Description({
	"All items or specific type(s) of items in an inventory. Useful for looping or storing in a list variable.",
	"Please note that the positions of the items in the inventory are not saved, only their order is preserved."
})
@Examples({
	"loop all items in the player's inventory:",
	"\tloop-item is enchanted",
	"\tremove loop-item from the player",
	"set {inventory::%uuid of player%::*} to items in the player's inventory"
})
@Since("2.0, INSERT VERSION (specific types of items)")
public class ExprItemsIn extends SimpleExpression<Slot> {

	static {
		Skript.registerExpression(ExprItemsIn.class, Slot.class, ExpressionType.PROPERTY, "[(all [[of] the]|the)] (items|%-itemtypes%) ([with]in|of|contained in|out of) (|1¦inventor(y|ies)) %inventories%");
	}

	@SuppressWarnings("null")
	private Expression<Inventory> inventories;

	@Nullable
	private Expression<ItemType> types;

	@Override
	@SuppressWarnings({"unchecked", "null"})
	/*
	 * the parse result will be null if it is used via the ExprInventory expression, however the expression will never
	 * be a variable when used with that expression (it is always a anonymous SimpleExpression)
	 */
	public boolean init(Expression<?>[] exprs, int matchedPattern, Kleenean isDelayed, @Nullable ParseResult parseResult) {
		types = (Expression<ItemType>) exprs[0];
		inventories = (Expression<Inventory>) exprs[1];
		if (inventories instanceof Variable && !inventories.isSingle() && parseResult.mark != 1)
			Skript.warning("'items in {variable::*}' does not actually represent the items stored in the variable. Use either '{variable::*}' (e.g. 'loop {variable::*}') if the variable contains items, or 'items in inventories {variable::*}' if the variable contains inventories.");
		return true;
	}

	private boolean isAllowedItem(@Nullable ItemType[] types, @Nullable ItemStack item) {
		if (types == null)
			return item != null;
		else if (item == null)
			return false;

		ItemType potentiallyAllowedItem = new ItemType(item);
		for (ItemType type : types) {
			if (potentiallyAllowedItem.isSimilar(type))
				return true;
		}

		return false;
	}

	@Override
	@SuppressWarnings("null")
	protected Slot[] get(final Event e) {
		ArrayList<Slot> r = new ArrayList<>();
		ItemType[] types = this.types == null ? null : this.types.getArray(e);
		for (Inventory invi : inventories.getArray(e)) {
			for (int i = 0; i < invi.getSize(); i++) {
				if (isAllowedItem(types, invi.getItem(i)))
					r.add(new InventorySlot(invi, i));
			}
		}
		return r.toArray(new Slot[r.size()]);
	}

	@Override
	@Nullable
	public Iterator<Slot> iterator(Event e) {
		Iterator<? extends Inventory> is = inventories.iterator(e);
		ItemType[] types = this.types == null ? null : this.types.getArray(e);
		if (is == null || !is.hasNext())
			return null;
		return new Iterator<Slot>() {
			@SuppressWarnings("null")
			Inventory current = is.next();

			int next = 0;

			@Override
			@SuppressWarnings("null")
			public boolean hasNext() {
				while (next < current.getSize() && !isAllowedItem(types, current.getItem(next)))
					next++;
				while (next >= current.getSize() && is.hasNext()) {
					current = is.next();
					next = 0;
					while (next < current.getSize() && !isAllowedItem(types, current.getItem(next)))
						next++;
				}
				return next < current.getSize();
			}

			@Override
			public Slot next() {
				if (!hasNext())
					throw new NoSuchElementException();
				return new InventorySlot(current, next++);
			}

			@Override
			public void remove() {
				throw new UnsupportedOperationException();
			}
		};
	}

	@Override
	public boolean isLoopOf(final String s) {
		return s.equalsIgnoreCase("item");
	}

	@Override
	public String toString(final @Nullable Event e, final boolean debug) {
		return "items in " + inventories.toString(e, debug);
	}

	@Override
	public boolean isSingle() {
		return false;
	}

	@Override
	public Class<Slot> getReturnType() {
		return Slot.class;
	}

}
