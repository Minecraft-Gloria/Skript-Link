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

import ch.njol.skript.Skript;
import ch.njol.skript.classes.Converter;
import ch.njol.skript.classes.Converter.ConverterInfo;
import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Examples;
import ch.njol.skript.doc.Name;
import ch.njol.skript.doc.Since;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.ExpressionType;
import ch.njol.skript.lang.SkriptParser.ParseResult;
import ch.njol.skript.lang.Variable;
import ch.njol.skript.lang.util.ConvertedExpression;
import ch.njol.skript.lang.util.SimpleExpression;
import ch.njol.skript.log.ErrorQuality;
import ch.njol.skript.registrations.Classes;
import ch.njol.skript.registrations.Converters;
import ch.njol.skript.sections.SecLoop;
import ch.njol.skript.util.Utils;
import ch.njol.util.Kleenean;
import me.marquez.variablelink.skript.addon.LinkVariable;
import org.bukkit.event.Event;
import org.eclipse.jdt.annotation.Nullable;

import java.lang.reflect.Array;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Used to access a loop's current value.
 * <p>
 * TODO expression to get the current # of execution (e.g. loop-index/number/count/etc (not number though));
 * 
 * @author Peter Güttinger
 */
@Name("Loop value")
@Description("The currently looped value.")
@Examples({"# countdown:",
		"loop 10 times:",
		"	message \"%11 - loop-number%\"",
		"	wait a second",
		"# generate a 10x10 floor made of randomly coloured wool below the player:",
		"loop blocks from the block below the player to the block 10 east of the block below the player:",
		"	loop blocks from the loop-block to the block 10 north of the loop-block:",
		"		set loop-block-2 to any wool"})
@Since("1.0")
public class ExprLoopValue extends SimpleExpression<Object> {
	static {
		Skript.registerExpression(ExprLoopValue.class, Object.class, ExpressionType.SIMPLE, "[the] loop-<.+>");
	}
	
	@SuppressWarnings("null")
	private String name;
	
	@SuppressWarnings("null")
	private SecLoop loop;
	
	// whether this loops a variable
	boolean isVariableLoop = false;
	// if this loops a variable and isIndex is true, return the index of the variable instead of the value
	boolean isIndex = false;
	
	@Override
	public boolean init(Expression<?>[] vars, int matchedPattern, Kleenean isDelayed, ParseResult parser) {
		name = parser.expr;
		String s = "" + parser.regexes.get(0).group();
		int i = -1;
		Matcher m = Pattern.compile("^(.+)-(\\d+)$").matcher(s);
		if (m.matches()) {
			s = "" + m.group(1);
			i = Utils.parseInt("" + m.group(2));
		}
		Class<?> c = Classes.getClassFromUserInput(s);
		int j = 1;
		SecLoop loop = null;

		for (SecLoop l : getParser().getCurrentSections(SecLoop.class)) {
			if ((c != null && c.isAssignableFrom(l.getLoopedExpression().getReturnType())) || "value".equals(s) || l.getLoopedExpression().isLoopOf(s)) {
				if (j < i) {
					j++;
					continue;
				}
				if (loop != null) {
					Skript.error("There are multiple loops that match loop-" + s + ". Use loop-" + s + "-1/2/3/etc. to specify which loop's value you want.", ErrorQuality.SEMANTIC_ERROR);
					return false;
				}
				loop = l;
				if (j == i)
					break;
			}
		}
		if (loop == null) {
			Skript.error("There's no loop that matches 'loop-" + s + "'", ErrorQuality.SEMANTIC_ERROR);
			return false;
		}
		if (loop.getLoopedExpression() instanceof Variable) {
			isVariableLoop = true;
			if (((Variable<?>) loop.getLoopedExpression()).isIndexLoop(s))
				isIndex = true;
		}else if (loop.getLoopedExpression() instanceof LinkVariable<?,?>) {
			isVariableLoop = true;
			if (((LinkVariable<?, ?>) loop.getLoopedExpression()).isIndexLoop(s))
				isIndex = true;
		}
		this.loop = loop;
		return true;
	}
	
	@Override
	public boolean isSingle() {
		return true;
	}
	
	@SuppressWarnings("unchecked")
	@Override
	@Nullable
	protected <R> ConvertedExpression<Object, ? extends R> getConvertedExpr(Class<R>... to) {
		if (isVariableLoop && !isIndex) {
			Class<R> superType = (Class<R>) Utils.getSuperType(to);
			return new ConvertedExpression<>(this, superType,
					new ConverterInfo<>(Object.class, superType, new Converter<Object, R>() {
				@Override
				@Nullable
				public R convert(Object o) {
					return Converters.convert(o, to);
				}
			}, 0));
		} else {
			return super.getConvertedExpr(to);
		}
	}
	
	@Override
	public Class<? extends Object> getReturnType() {
		if (isIndex)
			return String.class;
		return loop.getLoopedExpression().getReturnType();
	}
	
	@Override
	@Nullable
	protected Object[] get(Event e) {
		if (isVariableLoop) {
			@SuppressWarnings("unchecked") Entry<String, Object> current = (Entry<String, Object>) loop.getCurrent(e);
			if (current == null)
				return null;
			if (isIndex)
				return new String[] {current.getKey()};
			Object[] one = (Object[]) Array.newInstance(getReturnType(), 1);
			one[0] = current.getValue();
			return one;
		}
		Object[] one = (Object[]) Array.newInstance(getReturnType(), 1);
		one[0] = loop.getCurrent(e);
		return one;
	}
	
	@Override
	public String toString(@Nullable Event e, boolean debug) {
		if (e == null)
			return name;
		if (isVariableLoop) {
			@SuppressWarnings("unchecked") Entry<String, Object> current = (Entry<String, Object>) loop.getCurrent(e);
			if (current == null)
				return Classes.getDebugMessage(null);
			return isIndex ? "\"" + current.getKey() + "\"" : Classes.getDebugMessage(current.getValue());
		}
		return Classes.getDebugMessage(loop.getCurrent(e));
	}
	
}
