"use client";
import React from 'react';
// TODO: Import toast, orderApi, generateSalesOrderId, refreshData, etc.

// Placeholder/mock data for demonstration
const orderId = "ORDER_ID_PLACEHOLDER";
const items = [];
const itemDestinations = {};

const processItem = (item, destinations) => {
    console.log('Processing item in processItem:', item);
    let destination;
    if (!item.productBarcode) {
        destination = destinations['null'];
        console.log('Non-serialized product, using null key. Destination:', destination);
    } else {
        destination = destinations[item.productBarcode];
        console.log('Serialized product, using productBarcode. Destination:', destination);
    }
    if (!destination) {
        console.log('No destination found for item:', {
            boxBarcode: item.boxBarcode,
            productBarcode: item.productBarcode,
            isNonSerialized: !item.productBarcode
        });
        return null;
    }
    if (!item.productBarcode) {
        console.log('Processing non-serialized item:', item);
        const processedItem = {
            orderId: item.orderId,
            identifier: `${item.boxBarcode}:${item.quantity}`,
            quantity: item.quantity,
            status: item.status,
            isNonSerialized: true,
            destination
        };
        console.log('Processed non-serialized item:', processedItem);
        return processedItem;
    }
    const processedItem = {
        orderId: item.orderId,
        identifier: item.productBarcode,
        quantity: item.quantity || 1,
        status: item.status,
        isNonSerialized: false,
        destination
    };
    console.log('Processed serialized item:', processedItem);
    return processedItem;
};

const groupItemsByDestination = (items, destinations) => {
    console.log('Grouping items by destination:', { items, destinations });
    const returnToStock = [];
    const moveToSales = [];
    const markAsBroken = [];
    items.forEach(item => {
        const processedItem = processItem(item, destinations);
        if (!processedItem) {
            console.log('Skipping item:', {
                identifier: String(item.productBarcode || item.boxBarcode),
                orderId: item.orderId,
                expectedOrderId: orderId,
                reason: 'destination not found'
            });
            return;
        }
        console.log('Processing item:', processedItem);
        switch (processedItem.destination) {
            case 'return':
                returnToStock.push(processedItem.identifier);
                break;
            case 'sales':
                moveToSales.push(processedItem.identifier);
                break;
            case 'broken':
                markAsBroken.push(processedItem.identifier);
                break;
        }
    });
    console.log('Grouped items:', { returnToStock, moveToSales, markAsBroken });
    return { returnToStock, moveToSales, markAsBroken };
};

const processOrder = async () => {
    try {
        console.log('Processing Items for order:', orderId);
        console.log('All items:', items);
        console.log('Item destinations:', itemDestinations);
        const { returnToStock, moveToSales, markAsBroken } = groupItemsByDestination(items, itemDestinations);
        console.log('Final arrays:', { returnToStock, moveToSales, markAsBroken });
        const requestBody = {
            employeeId: items[0]?.employeeId || 'UNKNOWN',
            shopName: items[0]?.shopName || 'UNKNOWN',
            note: 'Batch processing',
            returnToStock,
            moveToSales,
            markAsBroken,
            condition: null,
            splitPairs: true,
            salesOrderId: typeof generateSalesOrderId !== 'undefined' ? generateSalesOrderId(orderId) : 'SALES_ORDER_ID_PLACEHOLDER',
            lentItems: items
        };
        console.log('Final request body:', requestBody);
        // await orderApi.processLentOrder(orderId, requestBody);
        // await refreshData();
        // toast.success('Order processed successfully');
    } catch (error) {
        console.error('Error processing order:', error);
        // toast.error('Failed to process order: ' + (error.message || 'Unknown error'));
    }
};

export default function LentClient() {
    return (
        <div>
            <h1>Lent Page Client Logic</h1>
            {/* Add your UI and logic here */}
        </div>
    );
} 