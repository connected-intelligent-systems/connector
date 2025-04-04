/*
 *  Copyright (c) 2022 Sovity GmbH
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Sovity GmbH - initial API and implementation
 *
 */

package org.eclipse.edc.extension.policy;

import org.eclipse.edc.participant.spi.ParticipantAgentPolicyContext;
import org.eclipse.edc.policy.engine.spi.AtomicConstraintRuleFunction;
import org.eclipse.edc.policy.model.Operator;
import org.eclipse.edc.policy.model.Permission;
import org.eclipse.edc.spi.monitor.Monitor;

import java.time.OffsetDateTime;
import java.time.format.DateTimeParseException;

import static java.lang.String.format;


public class TimeConstraintFactory {
    
    private final Monitor monitor;
    
    public TimeConstraintFactory(Monitor monitor) {
        this.monitor = monitor;
    }

    public <T extends ParticipantAgentPolicyContext> AtomicConstraintRuleFunction<Permission, T> generateRuleFunction() {
        return (Operator operator, Object rightValue, Permission rule, T context) -> {
            try {
                var policyDate = OffsetDateTime.parse((String) rightValue);
                monitor.info(format("Policy Date: %s", policyDate.toString()));
                var nowDate = OffsetDateTime.now();
                monitor.info(format("Current Date and Time: %s", nowDate));
                return switch (operator) {
                    case LT -> nowDate.isBefore(policyDate);
                    case LEQ -> nowDate.isBefore(policyDate) || nowDate.equals(policyDate);
                    case GT -> nowDate.isAfter(policyDate);
                    case GEQ -> nowDate.isAfter(policyDate) || nowDate.equals(policyDate);
                    case EQ -> nowDate.equals(policyDate);
                    case NEQ -> !nowDate.equals(policyDate);
                    default -> false;
                };
            } catch (DateTimeParseException e) {
                monitor.severe("Failed to parse right value of constraint to date.");
                return false;
            }
        };
    }
}
