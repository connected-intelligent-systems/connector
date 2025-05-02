/*
 *  Copyright (c) 2024 Fraunhofer Institute for Software and Systems Engineering
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Fraunhofer Institute for Software and Systems Engineering - initial API and implementation
 *
 */

package org.eclipse.edc.extension.policy;

import org.eclipse.edc.participant.spi.ParticipantAgentPolicyContext;
import org.eclipse.edc.policy.engine.spi.AtomicConstraintRuleFunction;
import org.eclipse.edc.policy.model.Operator;
import org.eclipse.edc.policy.model.Permission;
import org.eclipse.edc.spi.monitor.Monitor;

import static java.lang.String.format;

public class NoRestrictionPolicy {

    private final Monitor monitor;

    public NoRestrictionPolicy(Monitor monitor) {
        this.monitor = monitor;
    }

    public <T extends ParticipantAgentPolicyContext> AtomicConstraintRuleFunction<Permission, T> generateRuleFunction() {
        return (Operator operator, Object rightValue, Permission rule, T context) -> {
            monitor.info(format("ParticipantAgent running super simple policy"));
            monitor.info(format("Evaluating constraint: location %s %s", operator, rightValue.toString()));

            return true;
        };
    }
}
