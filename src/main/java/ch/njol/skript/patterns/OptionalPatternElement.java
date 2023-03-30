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
package ch.njol.skript.patterns;

import org.jetbrains.annotations.Nullable;

import ch.njol.skript.Skript;

/**
 * A {@link PatternElement} that contains an optional part, for example {@code [hello world]}.
 */
public class OptionalPatternElement extends PatternElement {

	private final PatternElement patternElement;
	private boolean negated;

	public OptionalPatternElement(PatternElement patternElement) {
		this.patternElement = patternElement;
	}

	@Override
	void setNext(@Nullable PatternElement next) {
		super.setNext(next);
		patternElement.setLastNext(next);
	}

	void setNegated(boolean negated) {
		Skript.info("SETTING NEGATED ON " + patternElement.getClass().getName());
		this.negated = negated;
	}

	public boolean isNegated() {
		return negated;
	}

	@Override
	@Nullable
	public MatchResult match(String expr, MatchResult matchResult) {
		MatchResult newMatchResult = patternElement.match(expr, matchResult.copy());
		if (newMatchResult != null)
			return newMatchResult;
		return matchNext(expr, matchResult);
	}

	public PatternElement getPatternElement() {
		return patternElement;
	}

	@Override
	public String toString() {
		if (negated)
			return "";
		return "[" + patternElement.toFullString() + "]";
	}

}
