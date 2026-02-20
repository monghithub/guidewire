package com.guidewire.billing.mapper;

import com.guidewire.billing.dto.CreateInvoiceRequest;
import com.guidewire.billing.dto.InvoiceItemDto;
import com.guidewire.billing.dto.InvoiceResponse;
import com.guidewire.billing.entity.Invoice;
import com.guidewire.billing.entity.InvoiceItem;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

import java.util.List;

@Mapper(componentModel = "spring")
public interface InvoiceMapper {

    InvoiceResponse toResponse(Invoice invoice);

    List<InvoiceResponse> toResponseList(List<Invoice> invoices);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "items", ignore = true)
    Invoice toEntity(CreateInvoiceRequest request);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "invoice", ignore = true)
    InvoiceItem toItemEntity(InvoiceItemDto dto);

    List<InvoiceItem> toItemEntities(List<InvoiceItemDto> dtos);

    InvoiceItemDto toItemDto(InvoiceItem item);

    List<InvoiceItemDto> toItemDtos(List<InvoiceItem> items);
}
