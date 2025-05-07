package org.eclipse.edc.extension.policy;

import org.eclipse.edc.participant.spi.ParticipantAgentPolicyContext;
import org.eclipse.edc.policy.engine.spi.AtomicConstraintRuleFunction;
import org.eclipse.edc.policy.model.AtomicConstraint;
import org.eclipse.edc.policy.model.Expression;
import org.eclipse.edc.policy.model.Operator;
import org.eclipse.edc.policy.model.Permission;
import org.eclipse.edc.spi.monitor.Monitor;

import java.util.List;

import static java.lang.String.format;

public class ReferringUserConstraintFunction {

    private final Monitor monitor;

    public ReferringUserConstraintFunction(Monitor monitor) {
        this.monitor = monitor;
    }

    public <T extends ParticipantAgentPolicyContext> AtomicConstraintRuleFunction<Permission, T> generateRuleFunction() {
        return (Operator operator, Object rightValue, Permission rule, T context) -> {
//        var region = context.participantAgent().getClaims().get("region");

            List<AtomicConstraint> constraints = rule.getConstraints().stream()
                    .filter(AtomicConstraint.class::isInstance)
                    .map(AtomicConstraint.class::cast)
                    .toList();
            Expression leftExpression = constraints.get(0).getLeftExpression();
            var userId = context.participantAgent().getClaims().get("Researcher");


            monitor.info(format("************ ReferringUserConstraintFunction# Evaluating constraint leftExpression: %s operator: %s rightValue: %s", leftExpression, operator, rightValue.toString()));
            monitor.info(format("************ ReferringUserConstraintFunction# returning true when Evaluating userId: %s operator: %s rightValue: %s", userId, operator, rightValue));
            return true;
//            return switch (operator) {
//                case EQ -> Objects.equals(userId, rightValue);
//                case NEQ -> !Objects.equals(userId, rightValue);
//                case IN -> ((Collection<?>) rightValue).contains(userId);
//                default -> false;
//            };
        };
    }
}
