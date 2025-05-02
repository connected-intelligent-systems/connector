package org.eclipse.edc.extension.contracts;

import org.eclipse.edc.connector.controlplane.contract.spi.negotiation.ConsumerContractNegotiationManager;
import org.eclipse.edc.connector.controlplane.contract.spi.negotiation.store.ContractNegotiationStore;
import org.eclipse.edc.connector.controlplane.contract.spi.types.negotiation.ContractNegotiation;
import org.eclipse.edc.connector.controlplane.contract.spi.types.negotiation.ContractRequest;
import org.eclipse.edc.connector.controlplane.contract.spi.types.offer.ContractOffer;
import org.eclipse.edc.connector.controlplane.services.contractnegotiation.ContractNegotiationServiceImpl;
import org.eclipse.edc.connector.controlplane.services.query.QueryValidator;
import org.eclipse.edc.policy.model.AtomicConstraint;
import org.eclipse.edc.policy.model.Constraint;
import org.eclipse.edc.policy.model.Expression;
import org.eclipse.edc.policy.model.LiteralExpression;
import org.eclipse.edc.policy.model.Operator;
import org.eclipse.edc.policy.model.Permission;
import org.eclipse.edc.policy.model.Policy;
import org.eclipse.edc.spi.command.CommandHandlerRegistry;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.transaction.spi.TransactionContext;

import java.util.ArrayList;
import java.util.List;

import static java.lang.String.format;

public class ContractNegotiationServiceCustomImpl extends ContractNegotiationServiceImpl {
    private Monitor monitor;
    private final ContractNegotiationStore store;
    private final ConsumerContractNegotiationManager consumerManager;
    private final TransactionContext transactionContext;
    private final CommandHandlerRegistry commandHandlerRegistry;
    private final QueryValidator queryValidator;
    public ContractNegotiationServiceCustomImpl(Monitor monitor, ContractNegotiationStore store, ConsumerContractNegotiationManager consumerManager, TransactionContext transactionContext, CommandHandlerRegistry commandHandlerRegistry, QueryValidator queryValidator) {
        super(store, consumerManager, transactionContext, commandHandlerRegistry, queryValidator);
        this.monitor = monitor;
        this.store = store;
        this.consumerManager = consumerManager;
        this.transactionContext = transactionContext;
        this.commandHandlerRegistry = commandHandlerRegistry;
        this.queryValidator = queryValidator;
    }

    public ContractNegotiation initiateNegotiation(ContractRequest request) {
        List<Permission> oldPermissions = new ArrayList<>(request.getContractOffer().getPolicy().getPermissions());

        List<Permission> newPermissions = new ArrayList<>();
        oldPermissions.forEach(permission -> {
            List<Constraint> newConstraints = new ArrayList<>();
            List<AtomicConstraint> constraints = permission.getConstraints().stream().filter(AtomicConstraint.class::isInstance)
                    .map(AtomicConstraint.class::cast)
                    .toList();
            constraints.forEach(constraint -> {
                Expression userTokenKey = constraint.getLeftExpression();
                Expression tokenValue = constraint.getRightExpression();
                monitor.info(format("++++++++++ User token key: %s",  userTokenKey));
                monitor.info(format("++++++++++ UsertokenValue: %s",  tokenValue));
                if(userTokenKey.toString().contains("https://w3id.org/edc/v0.0.1/ns/token")) {
                    // TODO: now here you have read actual token and extract userID information from it
                    LiteralExpression leftExp = new LiteralExpression("https://w3id.org/edc/v0.0.1/ns/user-id");
                    Operator operator = constraint.getOperator();
                    LiteralExpression rightExp = new LiteralExpression("kaaa");
                    monitor.info(format("++++++++++ userTokenKey after modification: %s",  leftExp));
                    monitor.info(format("++++++++++ UsertokenValue after modification: %s",  rightExp));
                    newConstraints.add(AtomicConstraint.Builder.newInstance()
                            .leftExpression(leftExp)
                            .operator(operator)
                            .rightExpression(rightExp)
                            .build());
                } else {
                    // add all other constraints here
                    newConstraints.add(constraint);
                }
                monitor.info("********** new constraint: " + constraint);
            });
            newPermissions.add(Permission.Builder.newInstance()
                    .action(permission.getAction())
                    .constraints(newConstraints)
                    .duties(permission.getDuties())
                    .build());
        });
        monitor.info("********** new Permissions: " + newPermissions);
        Policy newpolicy = Policy.Builder.newInstance()
                .prohibitions(request.getContractOffer().getPolicy().getProhibitions())
                .permissions(newPermissions)
                .profiles(request.getContractOffer().getPolicy().getProfiles())
                .duties(request.getContractOffer().getPolicy().getObligations())
                .assignee(request.getContractOffer().getPolicy().getAssignee())
                .assigner(request.getContractOffer().getPolicy().getAssigner())
                .target(request.getContractOffer().getPolicy().getTarget())
                .inheritsFrom(request.getContractOffer().getPolicy().getInheritsFrom())
                .type(request.getContractOffer().getPolicy().getType())
                .extensibleProperties(request.getContractOffer().getPolicy().getExtensibleProperties())
                .build();
        ContractOffer newRequestOffer = ContractOffer.Builder.newInstance()
                .id(request.getContractOffer().getId())
                .assetId(request.getContractOffer().getAssetId())
                .policy(newpolicy)
                .build();

        ContractRequest newContractRequest = ContractRequest.Builder.newInstance()
                .callbackAddresses(request.getCallbackAddresses())
                .protocol(request.getProtocol())
                .counterPartyAddress(request.getCounterPartyAddress())
                .contractOffer(newRequestOffer)
                .build();

        return super.initiateNegotiation(newContractRequest);
    }

}
