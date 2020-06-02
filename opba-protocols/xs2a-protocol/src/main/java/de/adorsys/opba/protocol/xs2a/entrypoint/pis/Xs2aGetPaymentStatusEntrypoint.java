package de.adorsys.opba.protocol.xs2a.entrypoint.pis;

import de.adorsys.opba.protocol.api.common.ProtocolAction;
import de.adorsys.opba.protocol.api.dto.context.ServiceContext;
import de.adorsys.opba.protocol.api.dto.request.payments.PaymentStatusBody;
import de.adorsys.opba.protocol.api.dto.request.payments.PaymentStatusRequest;
import de.adorsys.opba.protocol.api.dto.result.fromprotocol.Result;
import de.adorsys.opba.protocol.api.dto.result.fromprotocol.ok.SuccessResult;
import de.adorsys.opba.protocol.api.pis.GetPaymentStatusState;
import de.adorsys.opba.protocol.api.services.scoped.consent.ProtocolFacingConsent;
import de.adorsys.opba.protocol.xs2a.context.pis.Xs2aPisContext;
import de.adorsys.opba.protocol.xs2a.entrypoint.ExtendWithServiceContext;
import de.adorsys.opba.protocol.xs2a.entrypoint.helpers.UuidMapper;
import de.adorsys.opba.protocol.xs2a.service.dto.ValidatedQueryHeaders;
import de.adorsys.opba.protocol.xs2a.service.mapper.QueryHeadersMapperTemplate;
import de.adorsys.opba.protocol.xs2a.service.xs2a.dto.DtoMapper;
import de.adorsys.opba.protocol.xs2a.service.xs2a.dto.payment.PaymentStateHeaders;
import de.adorsys.opba.protocol.xs2a.service.xs2a.dto.payment.PaymentStateParameters;
import de.adorsys.xs2a.adapter.service.PaymentInitiationService;
import de.adorsys.xs2a.adapter.service.Response;
import de.adorsys.xs2a.adapter.service.model.PaymentInitiationStatus;
import lombok.RequiredArgsConstructor;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;

import static de.adorsys.opba.protocol.xs2a.constant.GlobalConst.SPRING_KEYWORD;
import static de.adorsys.opba.protocol.xs2a.constant.GlobalConst.XS2A_MAPPERS_PACKAGE;

/**
 * Entry point to get payment status. BPMN engine and process is not touched.
 */
@Service("xs2aGetPaymentStatusState")
@RequiredArgsConstructor
public class Xs2aGetPaymentStatusEntrypoint implements GetPaymentStatusState {
    public static final String SEPA_CREDIT_TRANSFERS = "sepa-credit-transfers";
    private final PaymentInitiationService pis;
    private final PaymentStatusToBodyMapper mapper;
    private final Xs2aGetPaymentStatusEntrypoint.Extractor extractor;
    private final Xs2aGetPaymentStatusEntrypoint.FromRequest request2ContextMapper;
    private final ExtendWithServiceContext extender;

    @Override
    public CompletableFuture<Result<PaymentStatusBody>> execute(ServiceContext<PaymentStatusRequest> context) {
        ProtocolFacingConsent consent = context.getRequestScoped().consentAccess().findByCurrentServiceSession()
                .orElseThrow(() -> new IllegalStateException("Context not found"));

        ValidatedQueryHeaders<PaymentStateParameters, PaymentStateHeaders> params = extractor.forExecution(prepareContext(context));

        Response<PaymentInitiationStatus> paymentStatus = pis.getSinglePaymentInitiationStatus(
                SEPA_CREDIT_TRANSFERS,
                consent.getConsentId(),
                params.getHeaders().toHeaders(),
                params.getQuery().toParameters()
        );

        Result<PaymentStatusBody> result = new SuccessResult<>(mapper.map(paymentStatus.getBody()));
        return CompletableFuture.completedFuture(result);
    }

    @Mapper(componentModel = SPRING_KEYWORD, implementationPackage = XS2A_MAPPERS_PACKAGE)
    public interface PaymentStatusToBodyMapper {
        PaymentStatusBody map(PaymentInitiationStatus paymentStatus);
    }


    protected Xs2aPisContext prepareContext(ServiceContext<PaymentStatusRequest> serviceContext) {
        Xs2aPisContext context = request2ContextMapper.map(serviceContext.getRequest());
        context.setAction(ProtocolAction.GET_PAYMENT_STATUS);
        extender.extend(context, serviceContext);
        return context;
    }

    /**
     * Mapper to convert incoming user request to processable request context.
     */
    @Mapper(componentModel = SPRING_KEYWORD, uses = UuidMapper.class, implementationPackage = XS2A_MAPPERS_PACKAGE)
    public interface FromRequest extends DtoMapper<PaymentStatusRequest, Xs2aPisContext> {

        @Mapping(source = "facadeServiceable.bankId", target = "aspspId")
        @Mapping(source = "facadeServiceable.requestId", target = "requestId")
        Xs2aPisContext map(PaymentStatusRequest ctx);
    }

    @Service
    public static class Extractor extends QueryHeadersMapperTemplate<
            Xs2aPisContext,
            PaymentStateParameters,
            PaymentStateHeaders> {

        public Extractor(
                DtoMapper<Xs2aPisContext, PaymentStateHeaders> toHeaders,
                DtoMapper<Xs2aPisContext, PaymentStateParameters> toQuery) {
            super(toHeaders, toQuery);
        }
    }
}