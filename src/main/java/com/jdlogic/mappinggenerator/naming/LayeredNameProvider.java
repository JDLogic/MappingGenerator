package com.jdlogic.mappinggenerator.naming;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public class LayeredNameProvider implements INameProvider
{
    private final List<INameProvider> providers = new ArrayList<>();

    public LayeredNameProvider() {}

    public LayeredNameProvider(INameProvider... providers)
    {
        this(Arrays.asList(providers));
    }

    public LayeredNameProvider(Collection<INameProvider> providers)
    {
        this.providers.addAll(providers);
    }

    public void addProvider(INameProvider provider)
    {
        this.providers.add(provider);
    }

    @Override
    public Optional<String> getClassName(String clsName)
    {
        return this.providers.stream()
                .map(p -> p.getClassName(clsName))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .findFirst();
    }

    @Override
    public Optional<String> getFieldName(String clsName, String fieldName)
    {
        return this.providers.stream()
                .map(p -> p.getFieldName(clsName, fieldName))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .findFirst();
    }

    @Override
    public Optional<String> getMethodName(String clsName, String obfName, String desc)
    {
        return this.providers.stream()
                .map(p -> p.getMethodName(clsName, obfName, desc))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .findFirst();
    }
}
