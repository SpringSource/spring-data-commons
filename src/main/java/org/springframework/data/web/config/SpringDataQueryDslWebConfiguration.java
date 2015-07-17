/*
 * Copyright 2015 the original author or authors.
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
package org.springframework.data.web.config;

import java.util.List;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.web.querydsl.QueryDslPredicateArgumentResolver;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurerAdapter;

/**
 * @author Christoph Strobl
 * @since 1.11
 */
@Configuration
public class SpringDataQueryDslWebConfiguration extends WebMvcConfigurerAdapter {

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.web.config.SpringDataWebConfiguration#pageableResolver()
	 */
	@Bean
	public QueryDslPredicateArgumentResolver queryDslPredicateArgumentResolver() {
		return new QueryDslPredicateArgumentResolver();
	}

	@Override
	public void addArgumentResolvers(List<HandlerMethodArgumentResolver> argumentResolvers) {
		argumentResolvers.add(0, queryDslPredicateArgumentResolver());
	}

}
