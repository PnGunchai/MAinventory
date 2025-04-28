// Process each item based on its destination
const processItem = (item, destinations) => {
    console.log('Processing item in processItem:', item);
    
    // For non-serialized products (0SN), we need to check the null key in destinations
    let destination;
    if (!item.productBarcode) {
        // This is a non-serialized product, use the 'null' key
        destination = destinations['null'];
        console.log('Non-serialized product, using null key. Destination:', destination);
    } else {
        // This is a serialized product, use the productBarcode
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

    // For non-serialized products (0SN)
    if (!item.productBarcode) {
        console.log('Processing non-serialized item:', item);
        const processedItem = {
            orderId: item.orderId,
            identifier: `${item.boxBarcode}:${item.quantity}`, // Use boxBarcode:quantity format
            quantity: item.quantity,
            status: item.status,
            isNonSerialized: true,
            destination
        };
        console.log('Processed non-serialized item:', processedItem);
        return processedItem;
    }

    // For serialized products (1SN, 2SN)
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

// Group items by destination
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

// Process the order
const processOrder = async () => {
    try {
        console.log('Processing Items for order:', orderId);
        console.log('All items:', items);
        console.log('Item destinations:', itemDestinations);

        const { returnToStock, moveToSales, markAsBroken } = groupItemsByDestination(items, itemDestinations);

        console.log('Final arrays:', { returnToStock, moveToSales, markAsBroken });

        // Prepare the request body
        const requestBody = {
            employeeId: items[0]?.employeeId || 'UNKNOWN',
            shopName: items[0]?.shopName || 'UNKNOWN',
            note: 'Batch processing',
            returnToStock,
            moveToSales,
            markAsBroken,
            condition: null,
            splitPairs: true,
            salesOrderId: generateSalesOrderId(orderId),
            lentItems: items // Pass all lent items for reference
        };

        console.log('Final request body:', requestBody);

        // Process the order
        await orderApi.processLentOrder(orderId, requestBody);
        
        // Refresh the data
        await refreshData();
        
        // Show success message
        toast.success('Order processed successfully');
    } catch (error) {
        console.error('Error processing order:', error);
        toast.error('Failed to process order: ' + (error.message || 'Unknown error'));
    }
}; 