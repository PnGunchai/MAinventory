'use client';
import { useState, useCallback, useEffect } from 'react';
import { productApi, orderApi } from '@/services/api';

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

export default function NewOrderModal({ isOpen, onClose }) {
  const [formData, setFormData] = useState({
    orderId: '',
    shopName: '',
    employeeId: '',
    note: '',
    destination: 'sales', // Default to sales
    nonSerialItems: [], // Changed to empty array by default
    serialGroups: [], // Changed to empty array by default
    isDirectSales: true // Default to true for direct sales
  });
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);
  const [productDetails, setProductDetails] = useState({}); // Store product details for each barcode
  const [showNonSerial, setShowNonSerial] = useState(false); // New state for toggling non-serial section
  const [showSerial, setShowSerial] = useState(false); // New state for toggling serial section

  // Function to fetch product details
  const fetchProductDetails = async (boxBarcode) => {
    if (!boxBarcode || boxBarcode.length < 4) return null; // Don't fetch if barcode is too short
    
    try {
      const data = await productApi.getProductByBoxBarcode(boxBarcode);
      setProductDetails(prev => ({
        ...prev,
        [boxBarcode]: data
      }));
      return data;
    } catch (error) {
      // Only set error state for network errors, not 404s
      if (error.message !== 'Product not found') {
        console.error('Error fetching product details:', error);
      }
      setProductDetails(prev => ({
        ...prev,
        [boxBarcode]: null
      }));
      return null;
    }
  };

  // Debounced version of fetchProductDetails
  const debouncedFetchProduct = useCallback(
    debounce((barcode) => fetchProductDetails(barcode), 500),
    []
  );

  // Handle box barcode change for non-serialized items
  const handleNonSerialBoxBarcodeChange = async (index, value) => {
    const newItems = [...formData.nonSerialItems];
    newItems[index].boxBarcode = value;
    setFormData(prev => ({
      ...prev,
      nonSerialItems: newItems
    }));

    if (value) {
      debouncedFetchProduct(value);
    }
  };

  // Handle box barcode change for serialized groups
  const handleSerialBoxBarcodeChange = async (groupIndex, value) => {
    const newGroups = [...formData.serialGroups];
    newGroups[groupIndex].boxBarcode = value;
    setFormData(prev => ({
      ...prev,
      serialGroups: newGroups
    }));

    if (value) {
      debouncedFetchProduct(value);
    }
  };

  // Handle product barcode change for serialized items within a group
  const handleSerialProductBarcodeChange = async (groupIndex, itemIndex, value) => {
    const newGroups = [...formData.serialGroups];
    newGroups[groupIndex].items[itemIndex].productBarcode = value;
    setFormData(prev => ({
      ...prev,
      serialGroups: newGroups
    }));
  };

  // Handle split pair toggle within a group
  const handleSplitPairChange = (groupIndex, itemIndex, checked) => {
    const newGroups = [...formData.serialGroups];
    newGroups[groupIndex].items[itemIndex].splitPair = checked;
    setFormData(prev => ({
      ...prev,
      serialGroups: newGroups
    }));
  };

  // Add new group
  const addSerialGroup = () => {
    setFormData(prev => ({
      ...prev,
      serialGroups: [...prev.serialGroups, {
        boxBarcode: '',
        items: [{ 
          productBarcode: '', 
          splitPair: false // Default to false for auto-pairing
        }]
      }]
    }));
  };

  // Remove group
  const removeSerialGroup = (groupIndex) => {
    setFormData(prev => ({
      ...prev,
      serialGroups: prev.serialGroups.filter((_, i) => i !== groupIndex)
    }));
  };

  // Add item to group
  const addItemToGroup = (groupIndex) => {
    const newGroups = [...formData.serialGroups];
    newGroups[groupIndex].items.push({ 
      productBarcode: '', 
      splitPair: false // Default to false for auto-pairing
    });
    setFormData(prev => ({
      ...prev,
      serialGroups: newGroups
    }));
  };

  // Remove item from group
  const removeItemFromGroup = (groupIndex, itemIndex) => {
    const newGroups = [...formData.serialGroups];
    newGroups[groupIndex].items = newGroups[groupIndex].items.filter((_, i) => i !== itemIndex);
    setFormData(prev => ({
      ...prev,
      serialGroups: newGroups
    }));
  };

  // Effect to validate product type when product details change
  useEffect(() => {
    let hasError = false;
    
    // Check non-serialized items
    formData.nonSerialItems.forEach((item) => {
      const product = productDetails[item.boxBarcode];
      if (product && product.numberSn !== 0) {
        setError(`Product ${item.boxBarcode} is a serialized product. Please use the serialized items section.`);
        hasError = true;
      }
    });

    // Check serialized items
    formData.serialGroups.forEach((group) => {
      const product = productDetails[group.boxBarcode];
      if (product && product.numberSn === 0) {
        setError(`Product ${group.boxBarcode} is a non-serialized product. Please use the non-serialized items section.`);
        hasError = true;
      }
    });

    // Clear error if no validation errors found
    if (!hasError) {
      setError(null);
    }
  }, [productDetails, formData.nonSerialItems, formData.serialGroups]);

  useEffect(() => {
    if (formData.destination === 'sales' && !formData.isDirectSales) {
      setFormData(prev => ({ ...prev, isDirectSales: true }));
    }
  }, [formData.destination]);

  const validateFormData = () => {
    const errors = [];
    
    // Validate basic fields
    if (!formData.destination) {
      errors.push('Destination is required');
    }
    
    if (formData.destination === 'sales' || formData.destination === 'lent') {
      if (!formData.orderId?.trim()) {
        errors.push('Order ID is required');
      } else if (!/^[A-Za-z0-9-]+$/.test(formData.orderId)) {
        errors.push('Order ID can only contain letters, numbers, and hyphens');
      }
      
      if (!formData.shopName?.trim()) {
        errors.push('Shop name is required');
      }
      
      if (!formData.employeeId?.trim()) {
        errors.push('Employee ID is required');
      }
    }
    
    // Validate non-serial items
    const nonSerialErrors = formData.nonSerialItems
      .map((item, index) => {
        if (item.boxBarcode?.trim() && !/^[A-Za-z0-9-]+$/.test(item.boxBarcode)) {
          return `Non-serial item ${index + 1}: Box barcode can only contain letters, numbers, and hyphens`;
        }
        if (item.quantity <= 0) {
          return `Non-serial item ${index + 1}: Quantity must be greater than 0`;
        }
        return null;
      })
      .filter(Boolean);
    
    errors.push(...nonSerialErrors);
    
    // Validate serial groups
    const serialErrors = formData.serialGroups
      .map((group, groupIndex) => {
        const groupErrors = [];
        
        if (group.boxBarcode?.trim() && !/^[A-Za-z0-9-]+$/.test(group.boxBarcode)) {
          groupErrors.push(`Serial group ${groupIndex + 1}: Box barcode can only contain letters, numbers, and hyphens`);
        }
        
        group.items.forEach((item, itemIndex) => {
          if (item.productBarcode?.trim() && !/^[A-Za-z0-9-]+$/.test(item.productBarcode)) {
            groupErrors.push(`Serial group ${groupIndex + 1}, item ${itemIndex + 1}: Product barcode can only contain letters, numbers, and hyphens`);
          }
        });
        
        return groupErrors;
      })
      .flat()
      .filter(Boolean);
    
    errors.push(...serialErrors);
    
    return errors;
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    try {
      setLoading(true);
      setError(null);
      
      // Validate form data
      const validationErrors = validateFormData();
      if (validationErrors.length > 0) {
        setError(validationErrors.join('\n'));
        return;
      }
      
      // Sanitize input data
      const sanitizedFormData = {
        ...formData,
        orderId: formData.orderId?.trim(),
        shopName: formData.shopName?.trim(),
        employeeId: formData.employeeId?.trim(),
        note: formData.note?.trim(),
        nonSerialItems: formData.nonSerialItems.map(item => ({
          ...item,
          boxBarcode: item.boxBarcode?.trim()
        })),
        serialGroups: formData.serialGroups.map(group => ({
          boxBarcode: group.boxBarcode?.trim(),
          items: group.items.map(item => ({
            ...item,
            productBarcode: item.productBarcode?.trim()
          }))
        })),
        isDirectSales: formData.isDirectSales
      };
      
      // Prepare the API request
      const requestData = {
        orderId: sanitizedFormData.orderId,
        shopName: sanitizedFormData.shopName,
        employeeId: sanitizedFormData.employeeId,
        note: sanitizedFormData.note,
        isDirectSales: sanitizedFormData.isDirectSales,
        // Always use the products array format
        products: [
          // Add non-serialized items
          ...sanitizedFormData.nonSerialItems
            .filter(item => item.boxBarcode)
            .map(item => ({
              identifier: `${item.boxBarcode}:${item.quantity}`,
              splitPair: true // Always true now
            })),
          
          // Add serialized items
          ...sanitizedFormData.serialGroups.flatMap(group => {
            return group.items
              .filter(item => item.productBarcode)
              .map(item => ({
                identifier: item.productBarcode,
                splitPair: true // Always true now
              }));
          })
        ]
      };

      console.log('Final request data:', requestData); // Debug log

      // Make the API call based on the destination
      let response;
      if (sanitizedFormData.destination === 'sales') {
        response = await orderApi.createSalesOrder(requestData);
      } else if (sanitizedFormData.destination === 'lent') {
        response = await orderApi.createLentOrder(requestData);
      }

      onClose();
    } catch (error) {
      console.error('Error creating order:', error);
      setError(error.message || 'Failed to create order');
    } finally {
      setLoading(false);
    }
  };

  // Non-serial items handlers
  const addNonSerialItem = () => {
    setFormData(prev => ({
      ...prev,
      nonSerialItems: [...prev.nonSerialItems, { boxBarcode: '', quantity: 1 }]
    }));
  };

  const removeNonSerialItem = (index) => {
    setFormData(prev => ({
      ...prev,
      nonSerialItems: prev.nonSerialItems.filter((_, i) => i !== index)
    }));
  };

  const updateNonSerialItem = (index, field, value) => {
    setFormData(prev => ({
      ...prev,
      nonSerialItems: prev.nonSerialItems.map((item, i) => 
        i === index ? { ...item, [field]: value } : item
      )
    }));
  };

  // Toggle non-serial section
  const toggleNonSerialSection = () => {
    setShowNonSerial(prev => {
      if (!prev) {
        // If showing the section, add one empty item
        setFormData(formData => ({
          ...formData,
          nonSerialItems: [{ boxBarcode: '', quantity: 1 }]
        }));
      } else {
        // If hiding the section, clear all items
        setFormData(formData => ({
          ...formData,
          nonSerialItems: []
        }));
      }
      return !prev;
    });
  };

  // Toggle serial section
  const toggleSerialSection = () => {
    setShowSerial(prev => {
      if (!prev) {
        // If showing the section, add one empty group
        setFormData(formData => ({
          ...formData,
          serialGroups: [{
            boxBarcode: '',
            items: [{ productBarcode: '', splitPair: false }]
          }]
        }));
      } else {
        // If hiding the section, clear all groups
        setFormData(formData => ({
          ...formData,
          serialGroups: []
        }));
      }
      return !prev;
    });
  };

  if (!isOpen) return null;

  return (
    <div className="fixed inset-0 bg-gray-600 bg-opacity-50 overflow-y-auto h-full w-full z-50">
      <div className="relative top-20 mx-auto p-5 border w-full max-w-2xl shadow-lg rounded-lg bg-white">
        <div className="flex justify-between items-center mb-4">
          <h2 className="text-xl font-bold text-gray-900">New Order</h2>
          <button
            onClick={() => onClose(false)}
            className="text-gray-500 hover:text-gray-700"
          >
            ✕
          </button>
        </div>

        {error && (
          <div className="bg-red-50 border border-red-200 text-red-600 px-4 py-3 rounded">
            {error}
          </div>
        )}

        <form onSubmit={handleSubmit} className="space-y-6">
          <div className="flex gap-4 justify-start">
            <button
              type="button"
              onClick={() => setFormData(prev => ({ ...prev, destination: 'sales' }))}
              className={`px-6 py-2 rounded-md font-medium transition-colors duration-200 ${
                formData.destination === 'sales'
                  ? 'bg-blue-600 text-white'
                  : 'bg-gray-100 text-gray-600 hover:bg-gray-200 hover:text-gray-900'
              }`}
            >
              Sale
            </button>
            <button
              type="button"
              onClick={() => setFormData(prev => ({ ...prev, destination: 'lent' }))}
              className={`px-6 py-2 rounded-md font-medium transition-colors duration-200 ${
                formData.destination === 'lent'
                  ? 'bg-green-600 text-white'
                  : 'bg-gray-100 text-gray-600 hover:bg-gray-200 hover:text-gray-900'
              }`}
            >
              Lent
            </button>
          </div>

          <div>
            <label className="block text-sm font-medium text-gray-900 mb-1">
              Order ID *
            </label>
            <input
              type="text"
              value={formData.orderId}
              onChange={(e) => setFormData(prev => ({ ...prev, orderId: e.target.value }))}
              className="w-full rounded-lg border-2 border-gray-300 shadow-sm focus:border-blue-500 focus:ring-blue-500 text-gray-900"
              placeholder="Leave blank for auto-generated ID"
            />
          </div>

          <div className="grid grid-cols-2 gap-4">
            <div>
              <label className="block text-sm font-medium text-gray-900 mb-1">
                Shop Name *
              </label>
              <input
                type="text"
                required
                value={formData.shopName}
                onChange={(e) => setFormData(prev => ({ ...prev, shopName: e.target.value }))}
                className="w-full rounded-lg border-2 border-gray-300 shadow-sm focus:border-blue-500 focus:ring-blue-500 text-gray-900"
              />
            </div>
            <div>
              <label className="block text-sm font-medium text-gray-900 mb-1">
                Employee ID *
              </label>
              <input
                type="text"
                required
                value={formData.employeeId}
                onChange={(e) => setFormData(prev => ({ ...prev, employeeId: e.target.value }))}
                className="w-full rounded-lg border-2 border-gray-300 shadow-sm focus:border-blue-500 focus:ring-blue-500 text-gray-900"
              />
            </div>
          </div>

          <div className="border-t pt-4">
            <div className="flex justify-between items-center mb-2">
              <h3 className="text-lg font-medium text-gray-900">Non-serialized Items</h3>
              <button
                type="button"
                onClick={toggleNonSerialSection}
                className="text-blue-600 hover:text-blue-800 text-sm font-medium"
              >
                {showNonSerial ? '- Remove Section' : '+ Add Section'}
              </button>
            </div>
            {showNonSerial && (
              <div className="space-y-3">
                {formData.nonSerialItems.map((item, index) => (
                  <div key={index} className="flex gap-3 items-start bg-gray-50 p-3 rounded-lg">
                    <div className="flex-1">
                      <input
                        type="text"
                        required
                        value={item.boxBarcode}
                        onChange={(e) => handleNonSerialBoxBarcodeChange(index, e.target.value)}
                        placeholder="Box Barcode"
                        className="w-full rounded-lg border-2 border-gray-300 shadow-sm focus:border-blue-500 focus:ring-blue-500 text-gray-900"
                      />
                      {productDetails[item.boxBarcode] && (
                        <div className="mt-1 text-sm text-gray-600">
                          Product: {productDetails[item.boxBarcode].productName}
                        </div>
                      )}
                    </div>
                    <div className="w-24">
                      <input
                        type="number"
                        required
                        min="1"
                        value={item.quantity}
                        onChange={(e) => {
                          const val = parseInt(e.target.value);
                          if (!isNaN(val) && val >= 1) {
                            updateNonSerialItem(index, 'quantity', val);
                          }
                        }}
                        className="w-full rounded-lg border-2 border-gray-300 shadow-sm focus:border-blue-500 focus:ring-blue-500 text-gray-900"
                      />
                    </div>
                    {formData.nonSerialItems.length > 1 && (
                      <button
                        type="button"
                        onClick={() => removeNonSerialItem(index)}
                        className="text-red-600 hover:text-red-800"
                      >
                        ✕
                      </button>
                    )}
                  </div>
                ))}
                <button
                  type="button"
                  onClick={addNonSerialItem}
                  className="text-blue-600 hover:text-blue-800 text-sm font-medium"
                >
                  + Add Item
                </button>
              </div>
            )}
          </div>

          <div className="border-t pt-4">
            <div className="flex justify-between items-center mb-2">
              <h3 className="text-lg font-medium text-gray-900">Serialized Items</h3>
              <button
                type="button"
                onClick={toggleSerialSection}
                className="text-blue-600 hover:text-blue-800 text-sm font-medium"
              >
                {showSerial ? '- Remove Section' : '+ Add Section'}
              </button>
            </div>
            {showSerial && (
              <div className="space-y-6">
                {formData.serialGroups.map((group, groupIndex) => (
                  <div key={groupIndex} className="bg-gray-50 p-4 rounded-lg">
                    <div className="flex gap-4 items-start">
                      <div className="flex-1">
                        <label className="block text-sm font-medium text-gray-700 mb-1">
                          Box Barcode
                        </label>
                        <input
                          type="text"
                          required
                          value={group.boxBarcode}
                          onChange={(e) => handleSerialBoxBarcodeChange(groupIndex, e.target.value)}
                          placeholder="Box Barcode"
                          className="w-full rounded-lg border-2 border-gray-300 shadow-sm focus:border-blue-500 focus:ring-blue-500 text-gray-900"
                        />
                        {productDetails[group.boxBarcode] && (
                          <div className="mt-2 text-sm">
                            <div className="text-gray-600">
                              Product: {productDetails[group.boxBarcode].productName}
                              <span className="ml-2 px-2 py-0.5 rounded-full text-xs font-medium" style={{
                                backgroundColor: productDetails[group.boxBarcode].numberSn === 2 ? '#EDE9FE' : '#E0F2FE',
                                color: productDetails[group.boxBarcode].numberSn === 2 ? '#5B21B6' : '#0369A1'
                              }}>
                                {productDetails[group.boxBarcode].numberSn}SN
                              </span>
                            </div>
                          </div>
                        )}
                      </div>
                      {formData.serialGroups.length > 1 && (
                        <button
                          type="button"
                          onClick={() => removeSerialGroup(groupIndex)}
                          className="text-red-600 hover:text-red-800"
                        >
                          Remove Group
                        </button>
                      )}
                    </div>

                    <div className="mt-4 space-y-3">
                      {group.items.map((item, itemIndex) => (
                        <div key={itemIndex} className="flex gap-3 items-start pl-4 border-l-2 border-gray-200">
                          <div className="flex-1">
                            <label className="block text-sm font-medium text-gray-700 mb-1">
                              Product Barcode
                            </label>
                            <input
                              type="text"
                              required
                              value={item.productBarcode}
                              onChange={(e) => handleSerialProductBarcodeChange(groupIndex, itemIndex, e.target.value)}
                              placeholder="Product Barcode"
                              className="w-full rounded-lg border-2 border-gray-300 shadow-sm focus:border-blue-500 focus:ring-blue-500 text-gray-900"
                            />
                          </div>
                          <button
                            type="button"
                            onClick={() => removeItemFromGroup(groupIndex, itemIndex)}
                            className="text-red-600 hover:text-red-800"
                            disabled={group.items.length === 1}
                          >
                            Remove
                          </button>
                        </div>
                      ))}
                      <button
                        type="button"
                        onClick={() => addItemToGroup(groupIndex)}
                        className="ml-4 text-blue-600 hover:text-blue-800 text-sm"
                      >
                        + Add Product Barcode
                      </button>
                    </div>
                  </div>
                ))}
                <button
                  type="button"
                  onClick={addSerialGroup}
                  className="text-blue-600 hover:text-blue-800 text-sm font-medium"
                >
                  + Add Group
                </button>
              </div>
            )}
          </div>

          <div>
            <label className="block text-sm font-medium text-gray-900 mb-1">
              Note
            </label>
            <textarea
              value={formData.note}
              onChange={(e) => setFormData(prev => ({ ...prev, note: e.target.value }))}
              rows="3"
              className="w-full rounded-lg border-2 border-gray-300 shadow-sm focus:border-blue-500 focus:ring-blue-500 text-gray-900"
            />
          </div>

          <div className="flex justify-end space-x-3 pt-4">
            <button
              type="button"
              onClick={() => onClose(false)}
              className="px-4 py-2 border border-gray-300 rounded-md text-gray-700 hover:bg-gray-50"
            >
              Cancel
            </button>
            <button
              type="submit"
              disabled={loading}
              className={`px-4 py-2 bg-blue-600 text-white rounded-md hover:bg-blue-700 
                ${loading ? 'opacity-50 cursor-not-allowed' : ''}`}
            >
              {loading ? 'Creating...' : 'Create Order'}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
} 