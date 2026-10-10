package com.example.demo.config.graphql;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

import org.jspecify.annotations.Nullable;
import org.springframework.aop.support.AopUtils;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.context.ApplicationContext;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.stereotype.Component;

import com.example.demo.tenant.config.TenantAware;
import com.netflix.graphql.dgs.DgsComponent;
import com.netflix.graphql.dgs.DgsData;
import com.netflix.graphql.dgs.DgsMutation;
import com.netflix.graphql.dgs.DgsQuery;

/**
 * Index of the access policy of every top-level operation declared with DGS
 * annotations. Anything not found here is treated as "authenticated users only".
 */
@Component
public class GraphQlOperationRegistry implements SmartInitializingSingleton {

	public record OperationPolicy(boolean isPublic, @Nullable TenantAware tenantAware) {
		static final OperationPolicy DEFAULT = new OperationPolicy(false, null);
	}

	private final ApplicationContext applicationContext;
	private volatile Map<String, OperationPolicy> policies = Map.of();

	GraphQlOperationRegistry(ApplicationContext applicationContext) {
		this.applicationContext = applicationContext;
	}

	@Override
	public void afterSingletonsInstantiated() {
		Map<String, OperationPolicy> found = new HashMap<>();
		for (Object bean : applicationContext.getBeansWithAnnotation(DgsComponent.class).values()) {
			Class<?> type = AopUtils.getTargetClass(bean);
			for (Method method : type.getDeclaredMethods()) {
				register(found, type, method);
			}
		}
		policies = Map.copyOf(found);
	}

	private void register(Map<String, OperationPolicy> found, Class<?> type, Method method) {
		String parentType = null;
		String field = null;

		var query = AnnotatedElementUtils.findMergedAnnotation(method, DgsQuery.class);
		var mutation = AnnotatedElementUtils.findMergedAnnotation(method, DgsMutation.class);
		var data = AnnotatedElementUtils.findMergedAnnotation(method, DgsData.class);
		if (query != null) {
			parentType = "Query";
			field = query.field();
		} else if (mutation != null) {
			parentType = "Mutation";
			field = mutation.field();
		} else if (data != null && ("Query".equals(data.parentType()) || "Mutation".equals(data.parentType()))) {
			parentType = data.parentType();
			field = data.field();
		}
		if (parentType == null) {
			return;
		}
		if (field == null || field.isEmpty()) {
			field = method.getName();
		}

		boolean isPublic = method.isAnnotationPresent(PublicOperation.class);
		TenantAware tenantAware = method.getAnnotation(TenantAware.class);
		if (isPublic && tenantAware != null) {
			throw new IllegalStateException(
					"%s#%s cannot be both @PublicOperation and @TenantAware".formatted(type.getName(), method.getName()));
		}
		found.put(parentType + "." + field, new OperationPolicy(isPublic, tenantAware));
	}

	public OperationPolicy policyFor(String parentType, String field) {
		return policies.getOrDefault(parentType + "." + field, OperationPolicy.DEFAULT);
	}
}
