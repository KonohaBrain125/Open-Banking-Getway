package de.adorsys.opba.protocol.api.dto.context;

import de.adorsys.opba.protocol.api.dto.request.FacadeServiceableRequest;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ServiceContext<T> {

    private Long bankProtocolId;

    /**
     * These parameters should take precedence on those defined in {@link ServiceContext#request}.
     */
    private FacadeServiceableRequest facadeServiceable;

    private T request;
}