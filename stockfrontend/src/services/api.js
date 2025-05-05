import Cookies from 'js-cookie';

// Use environment variable for API URL with fallback
const API_BASE_URL = process.env.NEXT_PUBLIC_API_URL || 'mainventory-production.up.railway.app/api';

// Generic API call function with error handling
async function apiCall(endpoint, options = {}) {
    try {
        // Remove /api prefix from endpoint if API_BASE_URL already includes it
        const cleanEndpoint = endpoint.startsWith('/api/') ? endpoint.substring(4) : endpoint;
        const url = `${API_BASE_URL}${cleanEndpoint.startsWith('/') ? cleanEndpoint : '/' + cleanEndpoint}`;
        console.log(`Making API call to ${url}`, options); // Debug log

        // Get token from cookies or localStorage (for production reliability)
        let token = Cookies.get('token');
        if (!token && typeof window !== 'undefined') {
            token = window.localStorage.getItem('token') || window.sessionStorage.getItem('token');
        }
        // Always send Authorization header if token is found
        const authHeader = token ? { 'Authorization': `Bearer ${token}` } : {};

        const response = await fetch(url, {
            ...options,
            headers: {
                'Content-Type': 'application/json',
                'Accept': 'application/json',
                'Origin': window.location.origin,
                ...authHeader,
                ...options.headers,
            },
            credentials: 'include', // Include credentials for CORS
            mode: 'cors', // Enable CORS mode
        });

        // Log response details for debugging
        console.log('Response status:', response.status);
        console.log('Response headers:', Object.fromEntries(response.headers.entries()));

        if (!response.ok) {
            // Handle HTTP errors
            const error = new Error('API call failed');
            error.status = response.status;
            error.statusText = response.statusText;
            try {
                const errorData = await response.text();
                error.data = errorData ? JSON.parse(errorData) : null;
            } catch (e) {
                error.data = null;
            }
            throw error;
        }

        // For DELETE operations that return no content
        if (response.status === 204) {
            return null;
        }

        // Only try to parse JSON if there's content
        const text = await response.text();
        console.log('Response text:', text); // Debug log
        
        const data = text ? JSON.parse(text) : null;
        console.log('Parsed data:', data);

        return data;
    } catch (error) {
        console.error('API call error:', error);
        if (error.status === 401) {
            window.alert('Session expired. Please log in again.');
            window.location.href = '/login';
            return; // Prevent further execution
        }
        throw {
            status: error.status,
            data: error.data,
            message: error.message || 'Network error occurred'
        };
    }
}

export default apiCall;

// Product Catalog API functions
export const productApi = {
    // Get all products with pagination
    getProducts: async (page = 0, size = 20, sort = 'boxBarcode', direction = 'asc') => {
        const response = await apiCall(`/products?page=${page}&size=${size}&sort=${sort}&direction=${direction}`);
        console.log('Raw API response:', response); // Debug log

        // Handle array response
        if (Array.isArray(response)) {
            return {
                data: response,
                totalElements: response.length,
                totalPages: 1,
                currentPage: 0
            };
        }

        // Handle paginated response
        return {
            data: response.content || [],
            totalElements: response.totalElements || response.length || 0,
            totalPages: response.totalPages || 1,
            currentPage: response.number || 0
        };
    },

    // Create new product
    createProduct: async (productData) => {
        console.log('Creating product with data:', productData); // Debug log
        const params = new URLSearchParams({
            boxBarcode: productData.boxBarcode,
            productName: productData.productName,
            numberSn: productData.numberSn
        });
        return apiCall(`/products?${params.toString()}`, {
            method: 'POST'
        });
    },

    // Delete product
    deleteProduct: async (boxBarcode) => {
        return apiCall(`/products/${boxBarcode}`, {
            method: 'DELETE'
        });
    },

    // Get product by box barcode
    getProductByBoxBarcode: async (boxBarcode) => {
        return apiCall(`/products/${boxBarcode}`);
    },

    // Get current stock data
    getCurrentStock: async () => {
        const response = await apiCall('/stock');
        console.log('Raw stock API response:', response);

        // Transform the data to group by product name
        const groupedData = response.reduce((acc, item) => {
            const existingProduct = acc.find(p => p.productName === item.productName);
            
            if (existingProduct) {
                // Update existing product
                existingProduct.totalQuantity += item.quantity;
                // Update last updated if this item is more recent
                if (new Date(item.lastUpdated) > new Date(existingProduct.lastUpdated)) {
                    existingProduct.lastUpdated = item.lastUpdated;
                }
                // Update highest box number if this one is higher
                const currentBoxNum = parseInt(existingProduct.highestBoxNumber);
                const newBoxNum = item.boxNumber;
                if (newBoxNum > currentBoxNum) {
                    existingProduct.highestBoxNumber = String(newBoxNum).padStart(4, '0');
                }
            } else {
                // Add new product
                acc.push({
                    productName: item.productName,
                    totalQuantity: item.quantity,
                    lastUpdated: item.lastUpdated,
                    highestBoxNumber: String(item.boxNumber).padStart(4, '0')
                });
            }
            return acc;
        }, []);

        return groupedData;
    },

    // Add stock
    addStock: async (stockData) => {
        console.log('Adding stock with data:', stockData);
        const params = new URLSearchParams();
        
        // Add required parameters
        params.append('boxBarcode', stockData.boxBarcode);
        params.append('quantity', stockData.quantity);
        
        // Add optional parameters
        if (stockData.productBarcode) {
            params.append('productBarcode', stockData.productBarcode);
        }
        if (stockData.note) {
            params.append('note', stockData.note);
        }

        return apiCall(`/stock/add?${params.toString()}`, {
            method: 'POST'
        });
    },

    // Add stock in bulk
    addStockBulk: async (bulkData) => {
        console.log('Adding stock in bulk with data:', bulkData);
        return apiCall('/stock/add-bulk', {
            method: 'POST',
            body: JSON.stringify(bulkData)
        });
    },

    // Remove stock
    removeStock: async (stockData) => {
        console.log('Removing stock with data:', stockData);
        const params = new URLSearchParams();
        
        // Add required parameters
        params.append('boxBarcode', stockData.boxBarcode);
        params.append('quantity', stockData.quantity);
        
        // Add optional parameters
        if (stockData.productBarcode) {
            params.append('productBarcode', stockData.productBarcode);
        }
        if (stockData.note) {
            params.append('note', stockData.note);
        }

        return apiCall(`/stock/remove?${params.toString()}`, {
            method: 'POST'
        });
    },

    // Remove stock in bulk
    removeStockBulk: async (bulkData) => {
        console.log('Removing stock in bulk with data:', bulkData);
        return apiCall('/stock/remove-bulk', {
            method: 'POST',
            body: JSON.stringify(bulkData)
        });
    },
};

// Order API functions
export const orderApi = {
    // Get all sales records with pagination
    getSalesOrders: async (page = 0, size = 20, sortField = 'timestamp', sortDirection = 'desc', search = '') => {
        const params = new URLSearchParams({
            page: page.toString(),
            size: size.toString(),
            sort: sortField,
            direction: sortDirection
        });
        if (search) {
            params.append('search', search);
        }
        const response = await apiCall(`/data/invoices?${params.toString()}`);
        return {
            content: response.content.map(invoice => ({
                orderId: invoice.invoice,
                employeeId: invoice.employeeId,
                shopName: invoice.shopName,
                timestamp: invoice.timestamp,
                type: 'sales',
                note: invoice.note,
                editCount: invoice.editCount,
                editHistory: invoice.editHistory
            })),
            totalPages: response.totalPages,
            totalElements: response.totalElements
        };
    },

    // Get all lent orders with pagination
    getLentOrders: async (page = 0, size = 20, sortField = 'timestamp', sortDirection = 'desc', search = '') => {
        const params = new URLSearchParams({
            page: page.toString(),
            size: size.toString(),
            sort: sortField,
            direction: sortDirection
        });
        if (search) {
            params.append('search', search);
        }
        console.log('Fetching lent orders with params:', params.toString());
        const response = await apiCall(`/lent-ids?${params.toString()}`);
        console.log('Raw lent orders response:', response);
        
        if (!response || !Array.isArray(response.content)) {
            console.error('Invalid response format:', response);
            return {
                content: [],
                totalPages: 0,
                totalElements: 0
            };
        }
        
        return {
            content: response.content.map(lent => ({
                lentId: lent.lentId,
                employeeId: lent.employeeId,
                shopName: lent.shopName,
                timestamp: lent.timestamp,
                type: 'lent',
                status: lent.status,
                note: lent.note
            })),
            totalPages: response.totalPages,
            totalElements: response.totalElements
        };
    },

    // Get all broken orders with pagination
    getBrokenOrders: async (page = 0, size = 20) => {
        const response = await apiCall(`/broken-ids/filter?page=${page}&size=${size}`);
        return {
            content: response.content.map(broken => ({
                orderId: broken.brokenId, // Map brokenId to orderId for consistency
                employeeId: broken.employeeId,
                condition: broken.condition,
                timestamp: broken.timestamp,
                type: 'broken'
            })),
            totalPages: response.totalPages,
            totalElements: response.totalElements
        };
    },

    // Create new sales order
    createSalesOrder: async (orderData) => {
        console.log('Creating sales order with data:', orderData); // Debug log
        return apiCall('/sales', {
            method: 'POST',
            body: JSON.stringify({
                orderId: orderData.orderId,
                shopName: orderData.shopName,
                employeeId: orderData.employeeId,
                productIdentifiers: orderData.productIdentifiers,
                products: orderData.products, // Include if using product-specific splitPair
                note: orderData.note
            })
        });
    },

    // Create new lent order
    createLentOrder: async (orderData) => {
        console.log('Creating lent order with data:', orderData); // Debug log
        return apiCall('/lent-orders', {
            method: 'POST',
            body: JSON.stringify({
                orderId: orderData.orderId,
                shopName: orderData.shopName,
                employeeId: orderData.employeeId,
                productIdentifiers: orderData.productIdentifiers,
                products: orderData.products, // Include if using product-specific splitPair
                note: orderData.note
            })
        });
    },

    // Process lent order items
    processLentOrder: async (orderId, data) => {
        console.log('Processing lent order:', orderId, data);
        
        // Transform moveToSales array to handle non-serialized products
        const transformedMoveToSales = (data.moveToSales || []).map(identifier => {
            // If it's already in the format we want (boxBarcode:quantity), use it as is
            if (identifier.includes(':')) {
                console.log('Using pre-formatted identifier:', identifier);
                return identifier;
            }
            
            // For serialized products, use the identifier as is
            return identifier;
        }).filter(id => id !== null && id !== undefined);
        
        console.log('Transformed moveToSales array:', transformedMoveToSales);

        // Transform returnToStock array to handle non-serialized products
        const transformedReturnToStock = (data.returnToStock || []).map(identifier => {
            // If it's already in the format we want (boxBarcode:quantity), use it as is
            if (identifier.includes(':')) {
                console.log('Using pre-formatted identifier for return:', identifier);
                return identifier;
            }
            
            // For serialized products, use the identifier as is
            return identifier;
        }).filter(id => id !== null && id !== undefined);

        // Transform markAsBroken array to handle non-serialized products
        const transformedMarkAsBroken = (data.markAsBroken || []).map(identifier => {
            // If it's already in the format we want (boxBarcode:quantity), use it as is
            if (identifier.includes(':')) {
                console.log('Using pre-formatted identifier for broken:', identifier);
                return identifier;
            }
            
            // For serialized products, use the identifier as is
            return identifier;
        }).filter(id => id !== null && id !== undefined);

        // Prepare the request body
        const requestBody = {
            ...data,
            moveToSales: transformedMoveToSales,
            returnToStock: transformedReturnToStock,
            markAsBroken: transformedMarkAsBroken,
            // Ensure salesOrderId is included when moving items to sales
            salesOrderId: transformedMoveToSales.length > 0 ? data.salesOrderId : undefined,
            isDirectSales: data.isDirectSales || false
        };

        console.log('Final request body:', requestBody);

        try {
            const response = await apiCall(`/lent-orders/orders/${orderId}/process`, {
                method: 'POST',
                body: JSON.stringify(requestBody)
            });
            return response;
        } catch (error) {
            console.error('Error processing lent order:', error);
            throw error;
        }
    },

    // Get lent order items
    getLentOrderItems: async (orderId) => {
        console.log('Fetching lent order items:', orderId);
        const response = await apiCall(`/lent-items/order/${orderId}`);
        return response;
    },

    // Get items for a sales order
    getSalesOrderItems: async (orderId) => {
        console.log('Fetching sales order items:', orderId);
        const response = await apiCall(`/sales/search/order/${orderId}`);
        return response.map(item => ({
            productBarcode: item.productBarcode,
            boxBarcode: item.boxBarcode,
            productName: item.productName,
            quantity: item.quantity
        }));
    },

    // Create broken order
    createBrokenOrder: async (data) => {
        try {
            const response = await apiCall('/broken', {
                method: 'POST',
                body: JSON.stringify({
                    employeeId: data.employeeId || null,
                    condition: data.condition,
                    products: [{
                        identifier: data.boxBarcode,
                        productBarcode: data.productBarcode,
                        splitPair: true
                    }]
                })
            });
            return response;
        } catch (error) {
            throw new Error(error.response?.data?.message || 'Failed to create broken order');
        }
    },

    // Add items to order
    addItemsToOrder: async (orderId, data) => {
        return await apiCall(`/sales/orders/${orderId}/items`, {
            method: 'POST',
            body: JSON.stringify(data)
        });
    },

    // Remove item from order
    removeItemFromOrder: async (orderId, productIdentifier) => {
        return await apiCall(`/sales/orders/${orderId}/items/${productIdentifier}`, {
            method: 'DELETE'
        });
    },

    // Update order notes
    updateOrderNotes: async (orderId, note) => {
        return await apiCall(`/sales/orders/${orderId}/notes`, {
            method: 'PUT',
            body: JSON.stringify({ note })
        });
    },
};

// Records API functions for viewing historical records
export const recordsApi = {
    // Get sales records with pagination
    getSalesRecords: async (page = 0, size = 20, sortField = 'timestamp', sortDirection = 'desc') => {
        const response = await apiCall(`/sales/filter?page=${page}&size=${size}&sortBy=${sortField}&sortDirection=${sortDirection}`);
        return {
            content: response.content.map(sale => ({
                orderId: sale.orderId,
                employeeId: sale.employeeId,
                shopName: sale.shopName,
                timestamp: sale.timestamp,
                type: 'sales',
                productBarcode: sale.productBarcode || '-',
                boxBarcode: sale.boxBarcode,
                productName: sale.productName,
                quantity: sale.quantity || 1,
                status: 'Sold'
            })),
            totalPages: response.totalPages,
            totalElements: response.totalElements
        };
    },

    // Get lent records with pagination
    getLentRecords: async (page = 0, size = 20, sortField = 'timestamp', sortDirection = 'desc') => {
        const response = await apiCall(`/data/lent?page=${page}&size=${size}&sort=${sortField}&direction=${sortDirection}`);
        return {
            content: response.content.map(lent => ({
                orderId: lent.orderId,
                employeeId: lent.employeeId,
                shopName: lent.shopName,
                timestamp: lent.timestamp,
                type: 'lent',
                productBarcode: lent.productBarcode || '-',
                boxBarcode: lent.boxBarcode,
                productName: lent.productName,
                quantity: lent.quantity,
                status: lent.status
            })),
            totalPages: response.totalPages,
            totalElements: response.totalElements
        };
    },

    // Get broken records with pagination
    getBrokenRecords: async (page = 0, size = 20, sortField = 'timestamp', sortDirection = 'desc') => {
        const response = await apiCall(`/data/broken?page=${page}&size=${size}&sort=${sortField}&direction=${sortDirection}`);
        return {
            content: response.content.map(broken => ({
                orderId: broken.orderId,
                employeeId: broken.employeeId,
                timestamp: broken.timestamp,
                type: 'broken',
                productBarcode: broken.productBarcode || '-',
                boxBarcode: broken.boxBarcode,
                productName: broken.productName,
                quantity: broken.quantity,
                condition: broken.condition
            })),
            totalPages: response.totalPages,
            totalElements: response.totalElements
        };
    }
};

// Logs API
export const logsApi = {
    // Get logs with pagination and filters
    getLogs: async (page = 0, search = '') => {
        try {
            // Create filter DTO
            const filterData = {
                page,
                size: 20,
                sortBy: 'timestamp',
                sortDirection: 'desc'
            };
            
            // Add search parameters for partial matching
            if (search) {
                if (search.match(/^[0-9]+$/)) {
                    // If numeric, search in product barcode and box barcode
                    filterData.productBarcode = search;
                  // filterData.boxBarcode = search;
                } else {
                    // If text, search in operation and product name
                   // filterData.operation = search;
                  //  filterData.productName = search;
                }
            }
            
            const response = await apiCall('/logs/filter', {
                method: 'POST',
                body: JSON.stringify(filterData)
            });
            

            console.log("response", response?.content);
            return {
                content: response.content?.map(log => ({
                    timestamp: log.timestamp,
                    productName: log.productName,
                    boxNumber: log.boxNumber,
                    productBarcode: log.productBarcode,
                    operation: log.operation,
                    quantity: log.quantity || 1,
                    orderId: log.orderId
                })) || [],
                totalPages: response.totalPages || 0,
                totalElements: response.totalElements || 0
            };
        } catch (error) {
            console.error('Error fetching logs:', error);
            throw new Error(error.response?.data?.message || 'Failed to fetch logs');
        }
    }
};

// InStock API functions
export const inStockApi = {
    // Get all in stock items
    getAllInStock: async () => {
        const response = await apiCall('/in-stock');
        return response;
    },

    // Get in stock items by box barcode
    getInStockByBoxBarcode: async (boxBarcode) => {
        return apiCall(`/in-stock/box/${boxBarcode}`);
    },

    // Get in stock item by product barcode
    getInStockByProductBarcode: async (productBarcode) => {
        return apiCall(`/in-stock/product/${productBarcode}`);
    }
}; 