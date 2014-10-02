/*
 * Copyright 2014 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.data.repository.inmemory.map;

import java.util.Comparator;

import org.springframework.expression.spel.standard.SpelExpression;
import org.springframework.expression.spel.standard.SpelExpressionParser;

/**
 * @author Christoph Strobl
 * @param <T>
 */
public class SpelComparator<T> implements Comparator<T> {

	boolean asc = true;
	SpelExpression expression;

	public SpelComparator(String path) {
		this.expression = new SpelExpressionParser().parseRaw(buildExpressionForPath(path));
	}

	public SpelComparator(SpelExpression expression) {
		this.expression = expression;
	}

	public void asc() {
		this.asc = true;
	}

	public void desc() {
		this.asc = false;
	}

	protected String buildExpressionForPath(String path) {

		StringBuilder rawExpression = new StringBuilder();
		rawExpression.append("#arg1?.");
		rawExpression.append(path != null ? path.replace(".", ".?") : "");
		rawExpression.append("?.compareTo(");
		rawExpression.append("#arg2?.");
		rawExpression.append(path != null ? path.replace(".", ".?") : "");
		rawExpression.append(")");
		return rawExpression.toString();
	}

	@Override
	public int compare(T o1, T o2) {

		expression.getEvaluationContext().setVariable("arg1", o1);
		expression.getEvaluationContext().setVariable("arg2", o2);

		return expression.getValue(Integer.class) * (asc ? 1 : -1);
	}

}
