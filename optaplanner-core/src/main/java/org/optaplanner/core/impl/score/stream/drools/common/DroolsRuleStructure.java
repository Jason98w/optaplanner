/*
 * Copyright 2020 Red Hat, Inc. and/or its affiliates.
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

package org.optaplanner.core.impl.score.stream.drools.common;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.function.LongSupplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.drools.model.DSL;
import org.drools.model.DeclarationSource;
import org.drools.model.PatternDSL;
import org.drools.model.RuleItemBuilder;
import org.drools.model.Variable;
import org.drools.model.consequences.ConsequenceBuilder;
import org.drools.model.view.ViewItem;
import org.optaplanner.core.impl.score.stream.drools.DroolsConstraintFactory;
import org.optaplanner.core.impl.score.stream.drools.bi.DroolsBiRuleStructure;
import org.optaplanner.core.impl.score.stream.drools.quad.DroolsQuadRuleStructure;
import org.optaplanner.core.impl.score.stream.drools.tri.DroolsTriRuleStructure;
import org.optaplanner.core.impl.score.stream.drools.uni.DroolsUniRuleStructure;

import static org.drools.model.DSL.from;

/**
 * Represents the left-hand side of a Drools rule.
 */
public abstract class DroolsRuleStructure {

    private final LongSupplier variableIdSupplier;

    protected DroolsRuleStructure(LongSupplier variableIdSupplier) {
        this.variableIdSupplier = variableIdSupplier;
    }

    /**
     * Declare a new {@link Variable} in this rule, with a given name and no declared source. Delegates to
     * {@link DSL#declarationOf(Class, String)}.
     * Creating variables via this method guarantees unique variable name within the context of a rule through the use
     * of {@link DroolsConstraintFactory#getVariableIdSupplier()}.
     *
     * @param clz type of the variable. Using {@link Object} will work in all cases, but Drools will spend unnecessary
     * amount of time looking up applicable instances of that variable, as it has to traverse instances of all types in
     * the working memory. Therefore, it is desirable to be as specific as possible.
     * @param name name of the variable, mostly useful for debugging purposes. Will be decorated by a pseudo-random
     * numeric identifier to prevent multiple variables of the same name to exist within left-hand side of a single
     * rule.
     * @param <X> Generic type of the variable.
     * @return new variable declaration, not yet bound to anything
     */
    public final <X> Variable<? extends X> createVariable(Class<X> clz, String name) {
        return DSL.declarationOf(clz, decorateVariableName(name));
    }

    /**
     * Declare a new {@link Variable} in this rule, with a given name and a declaration source.
     * Delegates to {@link DSL#declarationOf(Class, String, DeclarationSource)}.
     * Creating variables via this method guarantees unique variable names within the context of a rule through the use
     * of {@link DroolsConstraintFactory#getVariableIdSupplier()}.
     *
     * @param clz type of the variable. Using {@link Object} will work in all cases, but Drools will spend unnecessary
     * amount of time looking up applicable instances of that variable, as it has to traverse instances of all types in
     * the working memory. Therefore, it is desirable to be as specific as possible.
     * @param name name of the variable, mostly useful for debugging purposes. Will be decorated by a pseudo-random
     * numeric identifier to prevent multiple variables of the same name to exist within left-hand side of a single
     * rule.
     * @param source declaration source of the variable
     * @param <X> Generic type of the variable.
     * @return new variable declaration, not yet bound to anything
     */
    public final <X> Variable<? extends X> createVariable(Class<X> clz, String name, DeclarationSource source) {
        return DSL.declarationOf(clz, decorateVariableName(name), source);
    }

    private String decorateVariableName(String name) {
        return "$var" + variableIdSupplier.getAsLong() + "_" + name;
    }

    /**
     * Declares a new {@link Object}-typed variable, see {@link #createVariable(Class, String, DeclarationSource)} for
     * details.
     */
    public final <X> Variable<X> createVariable(String name) {
        return (Variable<X>) createVariable(Object.class, name);
    }

    /**
     * Declares a new {@link Object}-typed variable, see {@link #createVariable(Class, String)} for
     * details.
     */
    public final <X> Variable<X> createVariable(String name, DeclarationSource source) {
        return (Variable<X>) createVariable(Object.class, name, source);
    }

    public final List<RuleItemBuilder<?>> finish(ConsequenceBuilder.AbstractValidBuilder<?> consequence) {
        List<RuleItemBuilder<?>> closed = getClosedRuleItems();
        List<RuleItemBuilder<?>> open = getOpenRuleItems();
        List<RuleItemBuilder<?>> result = new ArrayList<>(closed.size() + open.size() + 2);
        result.addAll(closed);
        result.addAll(open);
        result.add(getPrimaryPattern().build());
        result.add(consequence);
        return result;
    }

    public LongSupplier getVariableIdSupplier() {
        return variableIdSupplier;
    }

    /**
     * Returns the pattern that the subsequent streams may further expand. Consider the following left-hand side of a
     * Drools rule:
     *
     * <pre>
     *     $a1: A()
     *     $a2: A(this != $a1)
     * </pre>
     *
     * The primary pattern would be the latter one ($a2), as that is the pattern you would use to further expand your
     * output in both $a1 and $a2.
     *
     * @return the primary pattern as defined
     */
    public abstract DroolsPatternBuilder<Object> getPrimaryPattern();

    /**
     * Every other pattern necessary for the {@link #getPrimaryPattern()} to function properly within the Drools rule's
     * left-hand side. In the example rule (see {@link #getPrimaryPattern()}, this method would return one and only
     * open {@link RuleItemBuilder}, the one representing $a1.
     *
     * @return all open rule items as defined, in the correct order
     */
    public abstract List<RuleItemBuilder<?>> getOpenRuleItems();

    public abstract List<RuleItemBuilder<?>> getClosedRuleItems();

    private List<RuleItemBuilder<?>> mergeClosedItems(RuleItemBuilder<?>... newClosedItems) {
        return Stream.concat(getClosedRuleItems().stream(), Stream.of(newClosedItems))
                .collect(Collectors.toList());
    }

    public <NewA> DroolsUniRuleStructure<NewA> recollect(Variable<NewA> newA, ViewItem<?> accumulatePattern) {
        DroolsPatternBuilder<NewA> newPrimaryPattern = new DroolsPatternBuilder<>(newA);
        // The accumulate pattern is the new open item, as it is where the primary pattern gets the variable from.
        List<RuleItemBuilder<?>> newOpenItems = Collections.singletonList(accumulatePattern);
        return new DroolsUniRuleStructure<>(newA, newPrimaryPattern, newOpenItems, getClosedRuleItems(),
                getVariableIdSupplier());
    }

    public <NewA> DroolsUniRuleStructure<NewA> regroup(Variable<Set<NewA>> newASource,
            PatternDSL.PatternDef<Set<NewA>> collectPattern, ViewItem<?> accumulatePattern) {
        Variable<NewA> newA = createVariable("groupKey", from(newASource));
        DroolsPatternBuilder<NewA> newPrimaryPattern = new DroolsPatternBuilder<>(newA);
        return new DroolsUniRuleStructure<>(newA, newPrimaryPattern, Arrays.asList(collectPattern),
                mergeClosedItems(accumulatePattern), getVariableIdSupplier());
    }

    public <NewA, NewB> DroolsBiRuleStructure<NewA, NewB> regroupBi(Variable<Set<BiTuple<NewA, NewB>>> newSource,
            PatternDSL.PatternDef<Set<BiTuple<NewA, NewB>>> collectPattern, ViewItem<?> accumulatePattern) {
        Variable<BiTuple<NewA, NewB>> newTuple =
                (Variable<BiTuple<NewA, NewB>>) createVariable(BiTuple.class,"groupKey", from(newSource));
        Variable<NewA> newA = createVariable("newA");
        Variable<NewB> newB = createVariable("newB");
        DroolsPatternBuilder<BiTuple<NewA, NewB>> newPrimaryPattern = new DroolsPatternBuilder<>(newTuple)
                .expand(p -> p.bind(newA, tuple -> tuple.a))
                .expand(p -> p.bind(newB, tuple -> tuple.b));
        return new DroolsBiRuleStructure<>(newA, newB, newPrimaryPattern, Arrays.asList(collectPattern),
                mergeClosedItems(accumulatePattern), getVariableIdSupplier());
    }

    public <NewA, NewB, NewC> DroolsTriRuleStructure<NewA, NewB, NewC> regroupBiToTri(
            Variable<Set<TriTuple<NewA, NewB, NewC>>> newSource,
            PatternDSL.PatternDef<Set<TriTuple<NewA, NewB, NewC>>> collectPattern, ViewItem<?> accumulatePattern) {
        Variable<TriTuple<NewA, NewB, NewC>> newTuple =
                (Variable<TriTuple<NewA, NewB, NewC>>) createVariable(TriTuple.class, "groupKey", from(newSource));
        Variable<NewA> newA = createVariable("newA");
        Variable<NewB> newB = createVariable("newB");
        Variable<NewC> newC = createVariable("newC");
        DroolsPatternBuilder<TriTuple<NewA, NewB, NewC>> newPrimaryPattern = new DroolsPatternBuilder<>(newTuple)
                .expand(p -> p.bind(newA, tuple -> tuple.a))
                .expand(p -> p.bind(newB, tuple -> tuple.b))
                .expand(p -> p.bind(newC, tuple -> tuple.c));
        return new DroolsTriRuleStructure<>(newA, newB, newC, newPrimaryPattern, Arrays.asList(collectPattern),
                mergeClosedItems(accumulatePattern), getVariableIdSupplier());
    }

    public <NewA, NewB, NewC, NewD> DroolsQuadRuleStructure<NewA, NewB, NewC, NewD> regroupBiToQuad(
            Variable<Set<QuadTuple<NewA, NewB, NewC, NewD>>> newSource,
            PatternDSL.PatternDef<Set<QuadTuple<NewA, NewB, NewC, NewD>>> collectPattern,
            ViewItem<?> accumulatePattern) {
        Variable<QuadTuple<NewA, NewB, NewC, NewD>> newTuple =
                (Variable<QuadTuple<NewA, NewB, NewC, NewD>>) createVariable(QuadTuple.class, "groupKey", from(newSource));
        Variable<NewA> newA = createVariable("newA");
        Variable<NewB> newB = createVariable("newB");
        Variable<NewC> newC = createVariable("newC");
        Variable<NewD> newD = createVariable("newD");
        DroolsPatternBuilder<QuadTuple<NewA, NewB, NewC, NewD>> newPrimaryPattern = new DroolsPatternBuilder<>(newTuple)
                .expand(p -> p.bind(newA, tuple -> tuple.a))
                .expand(p -> p.bind(newB, tuple -> tuple.b))
                .expand(p -> p.bind(newC, tuple -> tuple.c))
                .expand(p -> p.bind(newD, tuple -> tuple.d));
        return new DroolsQuadRuleStructure<>(newA, newB, newC, newD, newPrimaryPattern, Arrays.asList(collectPattern),
                mergeClosedItems(accumulatePattern), getVariableIdSupplier());
    }

}
