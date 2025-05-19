package org.eclipse.edc.extension.contracts;


import jakarta.json.Json;
import org.eclipse.edc.connector.controlplane.api.management.contractnegotiation.transform.JsonObjectFromContractNegotiationTransformer;
import org.eclipse.edc.connector.controlplane.api.management.contractnegotiation.transform.JsonObjectFromNegotiationStateTransformer;
import org.eclipse.edc.connector.controlplane.api.management.contractnegotiation.transform.JsonObjectToContractOfferTransformer;
import org.eclipse.edc.connector.controlplane.api.management.contractnegotiation.transform.JsonObjectToContractRequestTransformer;
import org.eclipse.edc.connector.controlplane.api.management.contractnegotiation.transform.JsonObjectToTerminateNegotiationCommandTransformer;
import org.eclipse.edc.connector.controlplane.api.management.contractnegotiation.v3.ContractNegotiationApiV3Controller;
import org.eclipse.edc.connector.controlplane.api.management.contractnegotiation.validation.ContractRequestValidator;
import org.eclipse.edc.connector.controlplane.api.management.contractnegotiation.validation.TerminateNegotiationValidator;
import org.eclipse.edc.connector.controlplane.contract.spi.negotiation.ConsumerContractNegotiationManager;
import org.eclipse.edc.connector.controlplane.contract.spi.negotiation.store.ContractNegotiationStore;
import org.eclipse.edc.connector.controlplane.services.query.QueryValidators;
import org.eclipse.edc.runtime.metamodel.annotation.Inject;
import org.eclipse.edc.spi.command.CommandHandlerRegistry;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.eclipse.edc.transaction.spi.TransactionContext;
import org.eclipse.edc.transform.spi.TypeTransformerRegistry;
import org.eclipse.edc.validator.spi.JsonObjectValidatorRegistry;
import org.eclipse.edc.web.spi.WebService;
import org.eclipse.edc.web.spi.configuration.ApiContext;

import java.util.Map;

import static org.eclipse.edc.connector.controlplane.contract.spi.types.command.TerminateNegotiationCommand.TERMINATE_NEGOTIATION_TYPE;
import static org.eclipse.edc.connector.controlplane.contract.spi.types.negotiation.ContractRequest.CONTRACT_REQUEST_TYPE;

/**
 * This extension registers @ContractNegotiationServiceCustomImpl for contract negotiation. However, this extension itself
 * is not registered in ServiceExtension.
 */
public class ContractNegotiationCustomApiExtension   implements ServiceExtension {
    public static final String NAME = "Management API: Contract Negotiation";

    @Inject
    private WebService webService;

    @Inject
    private TypeTransformerRegistry transformerRegistry;

    @Inject
    private JsonObjectValidatorRegistry validatorRegistry;

    @Inject
    private ContractNegotiationStore store;

    @Inject
    private ConsumerContractNegotiationManager consumerManager;

    @Inject
    private TransactionContext transactionContext;

    @Inject
    private CommandHandlerRegistry commandHandlerRegistry;

    static final String EDC_OAUTH_CLIENT_ID = "edc.oauth.client.id";


//    @Inject
//    private QueryValidator queryValidator;

    @Override
    public String name() {
        return NAME;
    }
    @Override
    public void initialize(ServiceExtensionContext context) {
        String edcClientId = context.getConfig().getString(EDC_OAUTH_CLIENT_ID);

        var factory = Json.createBuilderFactory(Map.of());
        var monitor = context.getMonitor();

        var managementApiTransformerRegistry = transformerRegistry.forContext("management-api");

        managementApiTransformerRegistry.register(new JsonObjectToContractRequestTransformer());
        managementApiTransformerRegistry.register(new JsonObjectToContractOfferTransformer());
        managementApiTransformerRegistry.register(new JsonObjectToTerminateNegotiationCommandTransformer());
        managementApiTransformerRegistry.register(new JsonObjectFromContractNegotiationTransformer(factory));
        managementApiTransformerRegistry.register(new JsonObjectFromNegotiationStateTransformer(factory));

        validatorRegistry.register(CONTRACT_REQUEST_TYPE, ContractRequestValidator.instance());
        validatorRegistry.register(TERMINATE_NEGOTIATION_TYPE, TerminateNegotiationValidator.instance());

        ContractNegotiationServiceCustomImpl contractNegotiationServiceCustom = new ContractNegotiationServiceCustomImpl(context.getMonitor(),edcClientId, store, consumerManager, transactionContext, commandHandlerRegistry, QueryValidators.contractNegotiation());
        context.registerService(ContractNegotiationServiceCustomImpl.class, contractNegotiationServiceCustom);
//        webService.registerResource(ApiContext.MANAGEMENT, new ContractNegotiationApiV2Controller(service, managementApiTransformerRegistry, monitor, validatorRegistry));
        webService.registerResource(ApiContext.MANAGEMENT, new ContractNegotiationApiV3Controller(contractNegotiationServiceCustom, managementApiTransformerRegistry, monitor, validatorRegistry));
    }
}
