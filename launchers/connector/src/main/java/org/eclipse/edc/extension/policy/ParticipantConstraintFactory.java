/*
 *  Copyright (c) 2024 Materna Information & Communications SE
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Materna Information & Communications SE - initial API and implementation
 *
 */

package org.eclipse.edc.extension.policy;

import org.eclipse.edc.participant.spi.ParticipantAgentPolicyContext;
import org.eclipse.edc.policy.engine.spi.AtomicConstraintRuleFunction;
import org.eclipse.edc.policy.model.Operator;
import org.eclipse.edc.policy.model.Permission;
import org.eclipse.edc.spi.monitor.Monitor;

import java.util.Arrays;
import java.util.Objects;

import static java.lang.String.format;


public class ParticipantConstraintFactory {
    
    private final Monitor monitor;
    
    public ParticipantConstraintFactory(Monitor monitor) {
        this.monitor = monitor;
    }

    public <T extends ParticipantAgentPolicyContext> AtomicConstraintRuleFunction<Permission, T> generateRuleFunction() {
        return (Operator operator, Object rightValue, Permission rule, T context) -> {
            var participantId = context.participantAgent().getClaims().get("participant_id");
        
            monitor.info(format("ParticipantAgent participantId claim: %s", participantId.toString()));
            monitor.info(format("Evaluating constraint: participantId %s %s", operator, rightValue.toString()));
            
            return switch (operator) {
                case EQ -> Objects.equals(participantId, rightValue);
                case NEQ -> !Objects.equals(participantId, rightValue);
                // case IN -> ((Collection<?>) rightValue).contains(participantId);
    
                // Support for the IN case (equivalent to odrl:isPartOf)
                // Example of request body for policy definition:
                //                 "constraint": {
                //                     "@type": "AtomicConstraint",
                //                     "leftOperand": "participant_id",
                //                     "operator": {
                //                         "@id": "odrl:isPartOf"
                //                         },
                //                     "rightOperand": "company1, company2, company3"
                //                 }
                case IN -> Arrays.stream(rightValue.toString().split(",")).map(String::trim).anyMatch(participantId::equals);
                case IS_NONE_OF -> !Arrays.stream(rightValue.toString().split(",")).map(String::trim).anyMatch(participantId::equals);
                default -> false;
            };
        };
    }

}
