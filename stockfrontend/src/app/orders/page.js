'use client';
import { useState, useEffect, useCallback } from 'react';
import { orderApi } from '@/services/api';
import { useAuthStore } from '@/store/authStore';
import NewOrderModal from './NewOrderModal';
import EditOrderModal from '../../components/EditOrderModal';
import AddItemsModal from '../../components/AddItemsModal';
import { useTranslation } from 'react-i18next';
import { formatDateTime } from '@/utils/dateUtils';
import Button from '@/components/Button';

// Debounce helper function
function debounce(func, wait) {
  let timeout;
  return function executedFunction(...args) {
    const later = () => {
      clearTimeout(timeout);
      func(...args);
    };
    clearTimeout(timeout);
    timeout = setTimeout(later, wait);
  };
}

export default function Orders() {
  const { t, i18n } = useTranslation();
  const hasPermission = useAuthStore(state => state.hasPermission);
  const [activeTab, setActiveTab] = useState('sales');
  const [orders, setOrders] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [searchTerm, setSearchTerm] = useState('');
  const [debouncedSearchTerm, setDebouncedSearchTerm] = useState('');
  const [currentPage, setCurrentPage] = useState(0);
  const [totalPages, setTotalPages] = useState(0);
  const [totalElements, setTotalElements] = useState(0);
  const [showNewOrderModal, setShowNewOrderModal] = useState(false);
  const [sortField, setSortField] = useState('timestamp');
  const [sortDirection, setSortDirection] = useState('desc');

  // Add new state for processing modal
  const [selectedOrder, setSelectedOrder] = useState(null);
  const [showProcessModal, setShowProcessModal] = useState(false);
  const [processingItems, setProcessingItems] = useState([]);
  const [processingError, setProcessingError] = useState(null);
  const [newSalesOrderId, setNewSalesOrderId] = useState('');
  const [itemDestinations, setItemDestinations] = useState({});
  const [salesQuantities, setSalesQuantities] = useState({});

  // Add new state for view modal
  const [showViewModal, setShowViewModal] = useState(false);
  const [viewOrder, setViewOrder] = useState(null);
  const [viewOrderItems, setViewOrderItems] = useState([]);
  const [viewOrderLoading, setViewOrderLoading] = useState(false);
  const [viewOrderError, setViewOrderError] = useState(null);

  // Add this state at the top with other useState declarations
  const [showSalesQuantityInputs, setShowSalesQuantityInputs] = useState(false);

  // Add new state for edit modal
  const [showEditModal, setShowEditModal] = useState(false);
  const [showAddItemsModal, setShowAddItemsModal] = useState(false);
  const [selectedOrderForEdit, setSelectedOrderForEdit] = useState(null);

  // Add mounted state to ensure client-only rendering for permission-based UI
  const [mounted, setMounted] = useState(false);
  useEffect(() => setMounted(true), []);

  // Initialize item destinations when items are loaded
  useEffect(() => {
    if (processingItems.length > 0) {
      const initialDestinations = {};
      processingItems.forEach(item => {
        if (item.status === 'lent') {
          // For non-serialized items (no productBarcode), use boxBarcode as identifier
          const identifier = !item.productBarcode ? item.boxBarcode : item.productBarcode;
          console.log('Setting initial destination for item:', {
            boxBarcode: item.boxBarcode,
            productBarcode: item.productBarcode,
            identifier,
            isNonSerialized: !item.productBarcode,
            quantity: item.quantity,
            productName: item.productName
          });
          // Default all items to 'return'
          initialDestinations[identifier] = 'return';
        }
      });
      console.log('Initial destinations:', initialDestinations);
      setItemDestinations(initialDestinations);
    }
  }, [processingItems]);

  // Fetch lent order items when opening process modal
  useEffect(() => {
    const fetchLentItems = async () => {
      if (showProcessModal && selectedOrder) {
        try {
          const items = await orderApi.getLentOrderItems(selectedOrder.orderId);
          // Log complete structure of first item
          console.log('Complete item structure:', items[0]);
          setProcessingItems(items);
          setProcessingError(null);
          
          // Initialize destinations for all items
          const initialDestinations = {};
          items.forEach(item => {
            if (item.status === 'lent') {
              // For non-serialized items (no productBarcode), use boxBarcode as identifier
              const identifier = !item.productBarcode ? item.boxBarcode : item.productBarcode;
              console.log('Setting initial destination for item:', {
                boxBarcode: item.boxBarcode,
                productBarcode: item.productBarcode,
                identifier: identifier,
                isNonSerialized: !item.productBarcode,
                quantity: item.quantity,
                productName: item.productName
              });
              // Default all items to 'return'
              initialDestinations[identifier] = 'return';
            }
          });
          console.log('Initial destinations:', initialDestinations);
          setItemDestinations(initialDestinations);
        } catch (error) {
          console.error('Error fetching lent items:', error);
          setProcessingError('Failed to fetch lent items');
          setProcessingItems([]);
        }
      }
    };

    fetchLentItems();
  }, [showProcessModal, selectedOrder]);

  // Handle process button click
  const handleProcessClick = (order) => {
    setSelectedOrder({
      ...order,
      orderId: order.lentId,
      employeeId: order.employeeId,
      shopName: order.shopName,
      status: order.status
    });
    setShowProcessModal(true);
    setProcessingError(null);
  };

  // Handle process modal close
  const handleProcessModalClose = (shouldRefresh = false) => {
    setShowProcessModal(false);
    setSelectedOrder(null);
    setProcessingItems([]);
    setProcessingError(null);
    setNewSalesOrderId('');
    setItemDestinations({});
    setSalesQuantities({});
    if (shouldRefresh) {
      refreshOrders();
    }
  };

  // Handle destination change
  const handleDestinationChange = (item, destination) => {
    const identifier = item.productBarcode || item.boxBarcode;
    setItemDestinations(prev => ({
      ...prev,
      [identifier]: destination
    }));

    // Reset sales quantity if destination is not sales
    if (destination !== 'sales' && !item.productBarcode) {
      setSalesQuantities(prev => {
        const newQuantities = { ...prev };
        delete newQuantities[item.boxBarcode];
        return newQuantities;
      });
    }
  };

  // Process handlers
  const handleProcessItems = async () => {
    try {
      setProcessingError(null);

      const moveToSales = [];
      const returnToStock = [];
      const markAsBroken = [];

      // Check if sales order ID is provided when needed
      const hasSalesItems = Object.values(itemDestinations).some(dest => dest === 'sales');
      if (hasSalesItems && !newSalesOrderId.trim()) {
        throw new Error(t('pleaseEnterInvoiceNumberForSales'));
      }

      // Validate all items have destinations and quantities where needed
      for (const item of processingItems) {
        if (item.status === 'lent') {
          const identifier = item.productBarcode || item.boxBarcode;
          const destination = itemDestinations[identifier];

          if (!destination) {
            throw new Error(t('pleaseSelectDestinationFor', { productName: item.productName }));
          }

          if (destination === 'sales') {
            if (!item.productBarcode) {
              // For non-serialized products, validate and use quantity
              const salesQty = salesQuantities[item.boxBarcode];
              if (!salesQty || salesQty <= 0) {
                throw new Error(t('pleaseSpecifyValidQuantityFor', { productName: item.productName }));
              }
              if (salesQty > item.quantity) {
                throw new Error(t('salesQuantityCannotExceedAvailableQuantity', { quantity: item.quantity, productName: item.productName }));
              }
              moveToSales.push(`${item.boxBarcode}:${salesQty}`);
              
              // Automatically return remaining quantity to stock
              const remainingQty = item.quantity - salesQty;
              if (remainingQty > 0) {
                returnToStock.push(`${item.boxBarcode}:${remainingQty}`);
              }
            } else {
              // For serialized products, use the product barcode
              moveToSales.push(item.productBarcode);
            }
          } else if (destination === 'return') {
            if (!item.productBarcode) {
              // For non-serialized products, return full quantity
              returnToStock.push(`${item.boxBarcode}:${item.quantity}`);
            } else {
              returnToStock.push(item.productBarcode);
            }
          } else if (destination === 'broken') {
            if (!item.productBarcode) {
              // For non-serialized products, mark full quantity as broken
              markAsBroken.push(`${item.boxBarcode}:${item.quantity}`);
            } else {
              markAsBroken.push(item.productBarcode);
            }
          }
        }
      }

      // Validate that at least one item is being processed
      if (moveToSales.length === 0 && returnToStock.length === 0 && markAsBroken.length === 0) {
        throw new Error(t('pleaseSelectDestinationForAtLeastOneItem'));
      }

      // Create request body
      const requestBody = {
        employeeId: selectedOrder.employeeId,
        shopName: selectedOrder.shopName,
        note: 'Batch processing',
        moveToSales,
        returnToStock,
        markAsBroken,
        condition: null,
        salesOrderId: hasSalesItems ? newSalesOrderId.trim() : undefined,
        isDirectSales: false // Add this flag
      };

      console.log('Sending request:', requestBody);

      // Process the items
      await orderApi.processLentOrder(selectedOrder.orderId, requestBody);

      // Close modal and refresh
      handleProcessModalClose(true);
      setProcessingError(t('itemsProcessedSuccessfully'));
    } catch (error) {
      console.error('Error processing items:', error);
      setProcessingError(error.message || t('failedToProcessItems'));
    }
  };

  // Update the table to show product information
  const getProductDisplay = (item) => {
    if (item.quantity > 1) {
      // For non-serialized items (SN=0)
      return `${item.productName}`;
    }
    // For serialized items (SN=1 or SN=2)
    return `${item.productName} (${item.productBarcode})`;
  };

  // Update the item identifier getter
  const getItemIdentifier = (item) => {
    return item.productBarcode || item.boxBarcode;
  };

  // Fetch orders based on active tab
  const fetchOrders = async () => {
    try {
      setLoading(true);
      setError(null);
      
      const response = activeTab === 'sales' 
        ? await orderApi.getSalesOrders(currentPage, 10, sortField, sortDirection, debouncedSearchTerm)
        : await orderApi.getLentOrders(currentPage, 10, sortField, sortDirection, debouncedSearchTerm);
      
      setOrders(response.content || []); 
      setTotalPages(response.totalPages); 
      setTotalElements(response.totalElements);
    } catch (err) {
      console.error('Error fetching orders:', err);
      setError(err.message || t('failedToFetchOrders'));
      setOrders([]);
      setTotalPages(0);
      setTotalElements(0);
    } finally {
      setLoading(false);
    }
  };

  // Refresh orders
  const refreshOrders = () => {
    setCurrentPage(0);
    fetchOrders();
  };

  // Debounced search effect
  useEffect(() => {
    const handler = setTimeout(() => {
      setDebouncedSearchTerm(searchTerm);
      setCurrentPage(0); // Reset to first page when search changes
    }, 300);

    return () => {
      clearTimeout(handler);
    };
  }, [searchTerm]);

  // Fetch orders when page, active tab, or debounced search term changes
  useEffect(() => {
    fetchOrders();
  }, [currentPage, activeTab, sortField, sortDirection, debouncedSearchTerm]);

  // Handle modal close
  const handleModalClose = (shouldRefresh = false) => {
    setShowNewOrderModal(false);
    if (shouldRefresh) {
      refreshOrders();
    }
  };

  // Handle view button click
  const handleViewClick = async (order) => {
    setViewOrder({
      ...order,
      note: order.note // Ensure note is included
    });
    setShowViewModal(true);
    setViewOrderLoading(true);
    setViewOrderError(null);

    try {
      let items;
      if (activeTab === 'lent') {
        items = await orderApi.getLentOrderItems(order.lentId);
      } else {
        items = await orderApi.getSalesOrderItems(order.orderId);
      }
      setViewOrderItems(items);
    } catch (error) {
      console.error('Error fetching order items:', error);
      setViewOrderError(error.message || t('failedToFetchOrderItems'));
      setViewOrderItems([]);
    } finally {
      setViewOrderLoading(false);
    }
  };

  // Handle view modal close
  const handleViewModalClose = () => {
    setShowViewModal(false);
    setViewOrder(null);
    setViewOrderItems([]);
    setViewOrderError(null);
  };

  // Update the search input handler
  const handleSearch = (e) => {
    setSearchTerm(e.target.value);
    setCurrentPage(0); // Reset to first page when search changes
  };

  // Add this handler for quantity changes
  const handleQuantityChange = (boxBarcode, quantity) => {
    setSalesQuantities(prev => ({
      ...prev,
      [boxBarcode]: parseInt(quantity) || 0
    }));
  };

  // Handle edit button click
  const handleEditClick = (order) => {
    setSelectedOrderForEdit(order);
    setShowEditModal(true);
  };

  // Handle order update
  const handleOrderUpdated = () => {
    refreshOrders();
  };

  const toggleLanguage = () => {
    const newLang = i18n.language === 'en' ? 'th' : 'en';
    i18n.changeLanguage(newLang);
  };

  return (
    <div className="space-y-6">
      <div className="flex justify-between items-center mb-6">
        <h1 className="text-3xl font-bold text-black">{t('orders')}</h1>
        {mounted && hasPermission('canCreateOrder') && (
          <Button 
            onClick={() => setShowNewOrderModal(true)}
            className="bg-blue-600 text-white px-4 py-2 rounded-lg hover:bg-blue-700 transition-colors"
          >
            {t('createOrder')}
          </Button>
        )}
      </div>

      {/* New Order Modal */}
      <NewOrderModal 
        isOpen={showNewOrderModal} 
        onClose={handleModalClose} 
      />

      {/* Order Type Tabs */}
      <div className="bg-white shadow-md rounded-lg border border-gray-300">
        <div className="border-b border-gray-300">
          <nav className="-mb-px flex space-x-8 px-4" aria-label="Tabs">
            <Button 
              onClick={() => setActiveTab('sales')}
              className={`${
                activeTab === 'sales'
                  ? 'border-blue-500 text-blue-600'
                  : 'border-transparent text-gray-500 hover:text-gray-700 hover:border-gray-300'
              } whitespace-nowrap py-4 px-1 border-b-2 font-medium text-sm`}
            >
              {t('salesOrders')}
            </Button>
            <Button 
              onClick={() => setActiveTab('lent')}
              className={`${
                activeTab === 'lent'
                  ? 'border-blue-500 text-blue-600'
                  : 'border-transparent text-gray-500 hover:text-gray-700 hover:border-gray-300'
              } whitespace-nowrap py-4 px-1 border-b-2 font-medium text-sm`}
            >
              {t('lentOrders')}
            </Button>
          </nav>
        </div>

        {/* Orders Table */}
        <div className="bg-white shadow rounded-lg">
          {/* Search Section */}
          <div className="p-4">
            <div className="flex gap-4 items-center">
              <div className="flex-1">
                <input
                  type="text"
                  placeholder={t('searchByOrderIdEmployeeIdShopName')}
                  value={searchTerm}
                  onChange={handleSearch}
                  className="w-full rounded-lg border-2 border-gray-300 shadow-sm focus:border-blue-500 focus:ring-blue-500 text-gray-900 placeholder-gray-400"
                />
              </div>
            </div>
          </div>

          <div className="overflow-x-auto">
            <table className="min-w-full divide-y divide-gray-200">
              <thead className="bg-gray-50">
                <tr>
                  <th 
                    onClick={() => {
                      setSortField(activeTab === 'sales' ? 'invoice' : 'lentId');
                      setSortDirection(sortField === (activeTab === 'sales' ? 'invoice' : 'lentId') && sortDirection === 'asc' ? 'desc' : 'asc');
                    }}
                    className="px-6 py-3 text-left text-xs font-medium text-gray-700 uppercase tracking-wider cursor-pointer hover:bg-gray-100"
                  >
                    {activeTab === 'sales' ? t('invoiceNumber') : t('lentId')}
                    {sortField === (activeTab === 'sales' ? 'invoice' : 'lentId') && (
                      <span className="ml-2">{sortDirection === 'asc' ? '↑' : '↓'}</span>
                    )}
                  </th>
                  <th 
                    onClick={() => {
                      setSortField('timestamp');
                      setSortDirection(sortField === 'timestamp' && sortDirection === 'asc' ? 'desc' : 'asc');
                    }}
                    className="px-6 py-3 text-left text-xs font-medium text-gray-700 uppercase tracking-wider cursor-pointer hover:bg-gray-100"
                  >
                    {t('date')}
                    {sortField === 'timestamp' && (
                      <span className="ml-2">{sortDirection === 'asc' ? '↑' : '↓'}</span>
                    )}
                  </th>
                  <th 
                    onClick={() => {
                      setSortField('shopName');
                      setSortDirection(sortField === 'shopName' && sortDirection === 'asc' ? 'desc' : 'asc');
                    }}
                    className="px-6 py-3 text-left text-xs font-medium text-gray-700 uppercase tracking-wider cursor-pointer hover:bg-gray-100"
                  >
                    {t('shopName')}
                    {sortField === 'shopName' && (
                      <span className="ml-2">{sortDirection === 'asc' ? '↑' : '↓'}</span>
                    )}
                  </th>
                  <th 
                    onClick={() => {
                      setSortField('employeeId');
                      setSortDirection(sortField === 'employeeId' && sortDirection === 'asc' ? 'desc' : 'asc');
                    }}
                    className="px-6 py-3 text-left text-xs font-medium text-gray-700 uppercase tracking-wider cursor-pointer hover:bg-gray-100"
                  >
                    {t('employeeId')}
                    {sortField === 'employeeId' && (
                      <span className="ml-2">{sortDirection === 'asc' ? '↑' : '↓'}</span>
                    )}
                  </th>
                  {activeTab === 'lent' && (
                    <th 
                      onClick={() => {
                        setSortField('status');
                        setSortDirection(sortField === 'status' && sortDirection === 'asc' ? 'desc' : 'asc');
                      }}
                      className="px-6 py-3 text-left text-xs font-medium text-gray-700 uppercase tracking-wider cursor-pointer hover:bg-gray-100"
                    >
                      {t('status')}
                      {sortField === 'status' && (
                        <span className="ml-2">{sortDirection === 'asc' ? '↑' : '↓'}</span>
                      )}
                    </th>
                  )}
                  <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                    {t('actions')}
                  </th>
                </tr>
              </thead>
              <tbody className="bg-white divide-y divide-gray-200">
                {loading ? (
                  <tr>
                    <td className="px-6 py-4 text-gray-700 text-center" colSpan={activeTab === 'lent' ? 6 : 5}>
                      {t('loading')}
                    </td>
                  </tr>
                ) : error ? (
                  <tr>
                    <td className="px-6 py-4 text-red-600 text-center" colSpan={activeTab === 'lent' ? 6 : 5}>
                      {error}
                    </td>
                  </tr>
                ) : orders.length === 0 ? (
                  <tr>
                    <td className="px-6 py-4 text-gray-700 text-center" colSpan={activeTab === 'lent' ? 6 : 5}>
                      {t('noOrdersFound')}
                    </td>
                  </tr>
                ) : orders.map((order) => (
                    <tr key={activeTab === 'sales' ? order.orderId : order.lentId} className="hover:bg-gray-50">
                      <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-700">
                        {activeTab === 'sales' ? (order.orderId || 'N/A') : (order.lentId || 'N/A')}
                      </td>
                      <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-700">
                        {order.timestamp ? formatDateTime(order.timestamp) : 'N/A'}
                      </td>
                      <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-700">
                        {order.shopName || 'N/A'}
                      </td>
                      <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-700">
                        {order.employeeId || 'N/A'}
                      </td>
                      {activeTab === 'lent' && (
                        <td className="px-6 py-4">
                          <span className={`inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-medium ${
                            order.status === 'active' 
                              ? 'bg-yellow-100 text-yellow-800' 
                              : 'bg-green-100 text-green-800'
                          }`}>
                            {order.status === 'active' ? t('active') : t('completed')}
                          </span>
                        </td>
                      )}
                      <td className="px-6 py-4 text-sm text-gray-500">
                        <div className="flex gap-2">
                          <Button
                            onClick={() => handleViewClick(order)}
                            className="text-blue-600 hover:text-blue-900"
                          >
                            {t('view')}
                          </Button>
                          {mounted && hasPermission('canCreateOrder') && activeTab === 'sales' && (
                            <Button
                              onClick={() => handleEditClick(order)}
                              className="text-green-600 hover:text-green-900"
                            >
                              {t('edit')}
                            </Button>
                          )}
                          {mounted && hasPermission('canCreateOrder') && activeTab === 'lent' && order.status === 'active' && (
                            <Button
                              onClick={() => handleProcessClick(order)}
                              className="text-yellow-600 hover:text-yellow-900"
                            >
                              {t('process')}
                            </Button>
                          )}
                        </div>
                      </td>
                    </tr>
                ))}
              </tbody>
            </table>
          </div>

          {/* Pagination */}
          {!loading && !error && orders.length > 0 && (
            <div className="px-4 py-3 flex items-center justify-between border-t border-gray-200">
              <div className="flex-1 flex justify-between items-center">
                <p className="text-sm text-gray-700">
                  {t('showingPage')} {currentPage + 1} {t('of')} {totalPages} ({totalElements} {t('totalOrders')})
                </p>
                <div className="space-x-2">
                  <Button
                    onClick={() => setCurrentPage(prev => Math.max(0, prev - 1))}
                    disabled={currentPage === 0}
                    className={`px-4 py-2 border rounded-md text-sm font-medium ${
                      currentPage === 0
                        ? 'bg-gray-100 text-gray-400 cursor-not-allowed'
                        : 'bg-white text-gray-700 hover:bg-gray-50'
                    }`}
                  >
                    {t('previous')}
                  </Button>
                  <Button
                    onClick={() => setCurrentPage(prev => Math.min(totalPages - 1, prev + 1))}
                    disabled={currentPage === totalPages - 1}
                    className={`px-4 py-2 border rounded-md text-sm font-medium ${
                      currentPage >= totalPages - 1
                        ? 'bg-gray-100 text-gray-400 cursor-not-allowed'
                        : 'bg-white text-gray-700 hover:bg-gray-50'
                    }`}
                  >
                    {t('next')}
                  </Button>
                </div>
              </div>
            </div>
          )}
        </div>
      </div>

      {/* Process Modal */}
      {showProcessModal && selectedOrder && (
        <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center p-4">
          <div className="bg-white rounded-lg max-w-4xl w-full max-h-[90vh] overflow-y-auto p-6 shadow-xl border border-gray-300">
            <div className="flex justify-between items-center mb-6">
              <h3 className="text-lg font-medium text-gray-900">{t('processLentOrder')}</h3>
              <button
                onClick={() => handleProcessModalClose(false)}
                className="text-gray-900 hover:text-gray-700"
              >
                ×
              </button>
            </div>

            <div className="space-y-4">
              {processingError && (
                <div className={`p-4 rounded-md mb-4 ${
                  processingError.includes('success') ? 'bg-green-50 text-green-700' : 'bg-red-50 text-red-700'
                }`}>
                  {processingError}
                </div>
              )}

              <div className="bg-gray-50 p-4 rounded-lg border border-gray-300 mb-4">
                <h4 className="font-medium mb-2 text-gray-900">{t('orderDetails')}</h4>
                <p className="text-gray-900">{t('lentId')}: {selectedOrder.orderId}</p>
                <p className="text-gray-900">{t('shop')}: {selectedOrder.shopName}</p>
                <p className="text-gray-900">{t('employee')}: {selectedOrder.employeeId}</p>
                <p className="text-gray-900">{t('activeItems')}: {processingItems.filter(item => item.status === 'lent').length}</p>
              </div>

              {/* Info alert about automatic returns */}
              <div className="bg-blue-50 border border-blue-200 text-blue-700 p-4 rounded-md mb-4">
                <p className="flex items-center">
                  <svg className="w-5 h-5 mr-2" fill="currentColor" viewBox="0 0 20 20">
                    <path fillRule="evenodd" d="M18 10a8 8 0 11-16 0 8 8 0 0116 0zm-7-4a1 1 0 11-2 0 1 1 0 012 0zM9 9a1 1 0 000 2v3a1 1 0 001 1h1a1 1 0 100-2v-3a1 1 0 00-1-1H9z" clipRule="evenodd" />
                  </svg>
                  {t('infoAboutAutomaticReturns')}
                </p>
              </div>

              {/* Sales Order ID input */}
              {Object.values(itemDestinations).some(dest => dest === 'sales') && (
                <div className="bg-white border border-gray-300 p-4 rounded-md mb-4">
                  <label htmlFor="salesOrderId" className="block text-sm font-medium text-gray-700 mb-2">
                    {t('salesOrderId')}
                  </label>
                  <input
                    type="text"
                    id="salesOrderId"
                    value={newSalesOrderId}
                    onChange={(e) => setNewSalesOrderId(e.target.value)}
                    className="w-full px-3 py-2 border-2 border-gray-700 bg-gray-100 rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-blue-700 text-gray-900"
                    placeholder={t('enterInvoiceNumberForSales')}
                    required
                  />
                </div>
              )}

              <div className="overflow-x-auto">
                <table className="min-w-full divide-y divide-gray-200">
                  <thead className="bg-gray-50">
                    <tr>
                      <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                        {t('product')}
                      </th>
                      <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                        {t('boxNumber')}
                      </th>
                      <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                        {t('quantity')}
                      </th>
                      <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                        {t('status')}
                      </th>
                      <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                        {t('action')}
                      </th>
                    </tr>
                  </thead>
                  <tbody className="bg-white divide-y divide-gray-200">
                    {processingItems
                      .filter(item => item.status !== 'processed') // Filter out processed items
                      .map((item, index) => (
                      <tr key={index} className={item.status !== 'lent' ? 'bg-gray-50' : ''}>
                        <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-900">
                          {item.productName}
                          <div className="text-xs text-gray-500">{item.boxBarcode}</div>
                          {item.productBarcode && <div className="text-xs font-bold text-blue-600">{item.productBarcode}</div>}
                        </td>
                        <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-500">
                          {item.boxNumber}
                        </td>
                        <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-500">
                          {!item.productBarcode ? item.quantity : 1}
                        </td>
                        <td className="px-6 py-4 whitespace-nowrap text-sm">
                          <span className={`px-2 inline-flex text-xs leading-5 font-semibold rounded-full ${
                            item.status === 'lent' ? 'bg-green-100 text-green-800' : 'bg-gray-100 text-gray-800'
                          }`}>
                            {item.status}
                          </span>
                        </td>
                        <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-500">
                          {item.status === 'lent' && (
                            <div className="flex flex-col space-y-2">
                              {!item.productBarcode ? (
                                // For non-serialized items
                                <div>
                                  <div className="flex items-center space-x-2 mb-2">
                                    <label className="text-sm font-medium text-gray-700">
                                      {t('moveToSales')}
                                    </label>
                                    <input
                                      type="number"
                                      min="0"
                                      max={item.quantity}
                                      value={salesQuantities[item.boxBarcode] || ''}
                                      onChange={(e) => {
                                        const value = Math.min(Math.max(0, parseInt(e.target.value) || 0), item.quantity);
                                        handleQuantityChange(item.boxBarcode, value);
                                        // If quantity is greater than 0, set destination to sales, otherwise to return
                                        handleDestinationChange(item, value > 0 ? 'sales' : 'return');
                                      }}
                                      className="w-24 px-2 py-1 border border-gray-300 rounded-md focus:outline-none focus:ring-blue-500 focus:border-blue-500 sm:text-sm"
                                      placeholder="0"
                                    />
                                    <span className="text-sm text-gray-500">/ {item.quantity}</span>
                                  </div>
                                  {salesQuantities[item.boxBarcode] > 0 ? (
                                    <p className="text-sm text-blue-600">
                                      {item.quantity - salesQuantities[item.boxBarcode]} items will be returned to stock
                                    </p>
                                  ) : (
                                    <p className="text-sm text-gray-500">
                                      {t('allItemsWillBeReturnedToStock')}
                                    </p>
                                  )}
                                </div>
                              ) : (
                                // For serialized items
                                <div className="flex items-center space-x-2">
                                  <input
                                    type="checkbox"
                                    id={`moveToSales-${item.productBarcode}`}
                                    checked={itemDestinations[item.productBarcode] === 'sales'}
                                    onChange={e => handleDestinationChange(item, e.target.checked ? 'sales' : 'return')}
                                    className="form-checkbox h-5 w-5 text-blue-600"
                                  />
                                  <label htmlFor={`moveToSales-${item.productBarcode}`} className="text-sm text-gray-700">
                                    {t('moveToSales')}
                                  </label>
                                </div>
                              )}
                            </div>
                          )}
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>

              <div className="flex justify-end space-x-4 mt-4">
                <Button
                  onClick={() => handleProcessModalClose(false)}
                  className="bg-white text-gray-700 hover:bg-gray-50 px-4 py-2 rounded-md border"
                >
                  {t('cancel')}
                </Button>
                <Button
                  onClick={handleProcessItems}
                  disabled={!processingItems.some(item => item.status === 'lent')}
                  className="bg-blue-600 text-white hover:bg-blue-700 px-4 py-2 rounded-md disabled:opacity-50 disabled:cursor-not-allowed"
                >
                  {t('processItems')}
                </Button>
              </div>
            </div>
          </div>
        </div>
      )}

      {/* View Modal */}
      {showViewModal && viewOrder && (
        <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center p-4">
          <div className="bg-white rounded-lg max-w-4xl w-full max-h-[90vh] overflow-y-auto p-6 shadow-xl border border-gray-300">
            <div className="flex justify-between items-center mb-6">
              <h3 className="text-lg font-medium text-gray-900">{activeTab === 'sales' ? t('salesOrderDetails') : t('lentOrderDetails')}</h3>
              <Button
                onClick={handleViewModalClose}
                className="text-gray-900 hover:text-gray-700"
              >
                ×
              </Button>
            </div>

            <div className="space-y-4">
              {viewOrderError && (
                <div className="bg-red-50 text-red-700 p-4 rounded-md mb-4">
                  {viewOrderError}
                </div>
              )}

              <div className="bg-gray-50 p-4 rounded-lg border border-gray-300 mb-4">
                <h4 className="font-medium mb-2 text-gray-900">{t('orderDetails')}</h4>
                <p className="text-gray-900">{t('orderId')}: {viewOrder.orderId}</p>
                <p className="text-gray-900">{t('shop')}: {viewOrder.shopName}</p>
                <p className="text-gray-900">{t('employee')}: {viewOrder.employeeId}</p>
                <p className="text-gray-900">{t('date')}: {formatDateTime(viewOrder.timestamp)}</p>
                {activeTab === 'lent' && (
                  <p className="text-gray-900">{t('status')}: {t(viewOrder.status)}</p>
                )}
                {viewOrder.note && (
                  <div className="mt-2">
                    <p className="font-medium text-gray-900">{t('notes')}:</p>
                    <p className="text-gray-700 whitespace-pre-wrap">{viewOrder.note}</p>
                  </div>
                )}
                {activeTab === 'sales' && viewOrder.editCount > 0 && (
                  <div className="mt-4 pt-4 border-t border-gray-200">
                    <div className="flex items-center mb-2">
                      <p className="font-medium text-gray-900">{t('editHistory')}</p>
                      <span className="ml-2 px-2 py-1 text-xs font-medium bg-blue-100 text-blue-800 rounded-full">
                        {viewOrder.editCount} {viewOrder.editCount === 1 ? t('edit') : t('edits')}
                      </span>
                    </div>
                    <div className="bg-gray-50 p-3 rounded-md max-h-40 overflow-y-auto">
                      <pre className="text-sm text-gray-700 whitespace-pre-wrap">{viewOrder.editHistory}</pre>
                    </div>
                  </div>
                )}
              </div>

              <div className="border border-gray-300 rounded-lg overflow-hidden shadow-sm">
                <table className="min-w-full divide-y divide-gray-200">
                  <thead className="bg-gray-50">
                    <tr>
                      <th className="px-6 py-3 text-left text-xs font-medium text-gray-700 uppercase tracking-wider">{t('productName')}</th>
                      <th className="px-6 py-3 text-left text-xs font-medium text-gray-700 uppercase tracking-wider">{t('productNumber')}</th>
                      <th className="px-6 py-3 text-left text-xs font-medium text-gray-700 uppercase tracking-wider">{t('quantity')}</th>
                      {activeTab === 'lent' && (
                        <th className="px-6 py-3 text-left text-xs font-medium text-gray-700 uppercase tracking-wider">{t('status')}</th>
                      )}
                    </tr>
                  </thead>
                  <tbody className="bg-white divide-y divide-gray-200">
                    {viewOrderLoading ? (
                      <tr>
                        <td colSpan={activeTab === 'lent' ? 4 : 3} className="px-6 py-4 text-gray-700 text-center">
                          {t('loadingItems')}
                        </td>
                      </tr>
                    ) : viewOrderItems.length === 0 ? (
                      <tr>
                        <td colSpan={activeTab === 'lent' ? 4 : 3} className="px-6 py-4 text-gray-700 text-center">
                          {t('noItemsFound')}
                        </td>
                      </tr>
                    ) : (
                      viewOrderItems
                        .filter(item => item.status !== 'processed')
                        .map((item) => (
                        <tr key={`${item.salesId || item.lendId}-${item.productBarcode || item.boxBarcode}`} className="hover:bg-gray-50">
                          <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-700">
                            {item.productName}
                          </td>
                          <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-700">
                            {item.quantity > 1 ? "-" : item.productBarcode}
                          </td>
                          <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-700">
                            {item.quantity ?? 1}
                          </td>
                          {activeTab === 'lent' && (
                            <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-700">
                              {t(item.status) || '-'}
                            </td>
                          )}
                        </tr>
                      ))
                    )}
                  </tbody>
                </table>
              </div>

              <div className="flex justify-end mt-4">
                <Button
                  onClick={handleViewModalClose}
                  className="bg-white text-gray-700 hover:bg-gray-50 px-4 py-2 rounded-md border"
                >
                  {t('close')}
                </Button>
              </div>
            </div>
          </div>
        </div>
      )}

      {/* Edit Order Modal */}
      <EditOrderModal
        isOpen={showEditModal}
        onClose={() => setShowEditModal(false)}
        order={selectedOrderForEdit}
        onOrderUpdated={handleOrderUpdated}
      />

      {/* Add Items Modal */}
      <AddItemsModal
        isOpen={showAddItemsModal}
        onClose={() => setShowAddItemsModal(false)}
        orderId={selectedOrderForEdit?.orderId}
        onItemsAdded={handleOrderUpdated}
      />
    </div>
  );
} 