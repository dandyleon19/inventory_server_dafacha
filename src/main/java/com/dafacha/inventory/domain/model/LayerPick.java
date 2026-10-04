package com.dafacha.inventory.domain.model;

import java.math.BigDecimal;

/** How much to take from one cost layer, chosen by the user (company cost method MANUAL). */
public record LayerPick(Long layerId, BigDecimal quantity) {
}
