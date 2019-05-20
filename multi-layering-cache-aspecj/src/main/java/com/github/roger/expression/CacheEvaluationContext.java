package com.github.roger.expression;

import org.springframework.context.expression.MethodBasedEvaluationContext;
import org.springframework.core.ParameterNameDiscoverer;

import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.Set;

public class CacheEvaluationContext extends MethodBasedEvaluationContext {

    private final Set<String> unavailableVariables = new HashSet<String>(1);

    public CacheEvaluationContext(CacheExpressionRootObject rootObject, Method targetMethod, Object[] args, ParameterNameDiscoverer parameterNameDiscoverer) {
        super(rootObject, targetMethod, args, parameterNameDiscoverer);
    }

    public void addUnavailableVariable(String name) {
        this.unavailableVariables.add(name);
    }

    @Override
    public Object lookupVariable(String name) {
        if(this.unavailableVariables.contains(name)){
            throw new VariableNotAvailableException(name);
        }
        return super.lookupVariable(name);
    }
}
