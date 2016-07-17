/*
 * Copyright 2016 Red Hat, Inc. and/or its affiliates.
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

package org.optaplanner.test.impl.score.buildin.hardsoft;

import java.math.BigDecimal;
import java.math.BigInteger;

import org.junit.Before;
import org.optaplanner.core.api.domain.solution.PlanningSolution;
import org.optaplanner.core.api.score.Score;
import org.optaplanner.core.api.score.buildin.hardsoft.HardSoftScore;
import org.optaplanner.core.api.score.constraint.ConstraintMatchTotal;
import org.optaplanner.core.api.solver.Solver;
import org.optaplanner.core.api.solver.SolverFactory;
import org.optaplanner.core.impl.domain.solution.descriptor.SolutionDescriptor;
import org.optaplanner.core.impl.score.definition.ScoreDefinition;
import org.optaplanner.core.impl.score.director.InnerScoreDirectorFactory;
import org.optaplanner.core.impl.score.director.ScoreDirector;
import org.optaplanner.core.impl.score.director.ScoreDirectorFactory;
import org.optaplanner.test.impl.score.AbstractScoreConstraintTest;

import static org.junit.Assert.assertEquals;

/**
 * To test the constraints (including score rules) of a {@link SolverFactory}
 * that uses a {@link HardSoftScore}.
 * @param <Solution_> the solution type, the class with the {@link PlanningSolution} annotation
 */
public abstract class HardSoftScoreConstraintTest<Solution_> extends AbstractScoreConstraintTest<Solution_> {

    @Override
    protected Class<? extends Score> getExpectedScoreClass() {
        return HardSoftScore.class;
    }

    /**
     * Assert that the constraint (which is usually a score rule) of {@link PlanningSolution}
     * has the expected weight for that score level
     * @param constraintName never null, the name of the constraint, which is usually the name of the score rule
     * @param expectedWeight the total weight for all matches of that 1 constraint
     * @param solution never null, the actual {@link PlanningSolution}
     */
    protected void assertHardConstraintWeight(String constraintName, int expectedWeight, Solution_ solution) {
        assertHardConstraintWeight(null, constraintName, expectedWeight, solution);
    }

    /**
     * Assert that the constraint (which is usually a score rule) of {@link PlanningSolution}
     * has the expected weight for that score level.
     * @param constraintPackage sometimes null.
     * When null, {@code constraintName} for the {@code scoreLevel} must be unique.
     * @param constraintName never null, the name of the constraint, which is usually the name of the score rule
     * @param expectedWeight the total weight for all matches of that 1 constraint
     * @param solution never null, the actual {@link PlanningSolution}
     */
    protected void assertHardConstraintWeight(String constraintPackage, String constraintName, int expectedWeight, Solution_ solution) {
        assertConstraintWeight(constraintPackage, constraintName, 0, Integer.valueOf(expectedWeight), solution);
    }

    /**
     * Assert that the constraint (which is usually a score rule) of {@link PlanningSolution}
     * has the expected weight for that score level.
     * @param constraintName never null, the name of the constraint, which is usually the name of the score rule
     * @param expectedWeight the total weight for all matches of that 1 constraint
     * @param solution never null, the actual {@link PlanningSolution}
     */
    protected void assertSoftConstraintWeight(String constraintName, int expectedWeight, Solution_ solution) {
        assertSoftConstraintWeight(null, constraintName, expectedWeight, solution);
    }

    /**
     * Assert that the constraint (which is usually a score rule) of {@link PlanningSolution}
     * has the expected weight for that score level.
     * @param constraintPackage sometimes null.
     * When null, {@code constraintName} for the {@code scoreLevel} must be unique.
     * @param constraintName never null, the name of the constraint, which is usually the name of the score rule
     * @param expectedWeight the total weight for all matches of that 1 constraint
     * @param solution never null, the actual {@link PlanningSolution}
     */
    protected void assertSoftConstraintWeight(String constraintPackage, String constraintName, int expectedWeight, Solution_ solution) {
        assertConstraintWeight(constraintPackage, constraintName, 1, Integer.valueOf(expectedWeight), solution);
    }

}
