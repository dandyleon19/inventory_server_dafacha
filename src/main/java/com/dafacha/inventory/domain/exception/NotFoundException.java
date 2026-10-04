package com.dafacha.inventory.domain.exception;

import org.springframework.http.HttpStatus;

public class NotFoundException extends ApiException {

    public NotFoundException(String message) {
        super(HttpStatus.NOT_FOUND, "NOT_FOUND", message);
    }

    /** "No se encontró el producto con id: 5". Resource names come in English from the use cases. */
    public static NotFoundException forResource(String resource, Object id) {
        return new NotFoundException("No se encontró " + label(resource) + " con id: " + id);
    }

    private static String label(String resource) {
        return switch (resource) {
            case "Product" -> "el producto";
            case "Product category" -> "la categoría";
            case "Warehouse" -> "el almacén";
            case "Supplier" -> "el proveedor";
            case "Purchase order" -> "la orden de compra";
            case "Purchase order line" -> "la línea de la orden";
            case "Company" -> "la empresa";
            case "User" -> "el usuario";
            default -> resource;
        };
    }
}
