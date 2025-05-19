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

import java.util.Arrays;

import static java.lang.String.format;

public class RolesVerificationPolicy {

    private final Monitor monitor;

    public RolesVerificationPolicy(Monitor monitor) {
        this.monitor = monitor;
    }

    public <T extends ParticipantAgentPolicyContext> AtomicConstraintRuleFunction<Permission, T> generateRuleFunction() {
        return (Operator operator, Object rightValue, Permission rule, T context) -> {

            monitor.info("All claims information from JWT token: "+ context.participantAgent().getClaims());
            var wetransformRole = context.participantAgent().getClaims().get("wetransform_role");
            if(wetransformRole == null) {
                monitor.info(format("wetransform_role claim is null for participant %s could not verify roles information so returning false " , context.participantAgent().getClaims().get("participant_id")));
                return false;
            }
            monitor.debug(format("ParticipantAgent wetransform_role from claim: %s", wetransformRole));
            return switch (operator) {
                case EQ -> wetransformRole.toString().equalsIgnoreCase(rightValue.toString());
                case NEQ -> !wetransformRole.toString().equalsIgnoreCase(rightValue.toString());
                case IN -> Arrays.stream(rightValue.toString().split(",")).map(String::trim).anyMatch(wetransformRole.toString()::equals);
                case IS_NONE_OF -> !Arrays.stream(rightValue.toString().split(",")).map(String::trim).anyMatch(wetransformRole.toString()::equals);
                default -> false;
            };
        };
    }
}
