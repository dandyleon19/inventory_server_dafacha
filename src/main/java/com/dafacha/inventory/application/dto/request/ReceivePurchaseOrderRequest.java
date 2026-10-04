package com.dafacha.inventory.application.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ReceivePurchaseOrderRequest {

    @NotEmpty(message = "Se necesita al menos un producto")
    @Valid
    private List<ReceiveItemRequest> items;

    /** The day the goods arrived (yyyy-MM-dd). Optional: missing means today. Not in the future. */
    private LocalDate receivedDate;
}
