'use client';

import { useState, useEffect, useCallback } from 'react';
import { productApi } from '@/services/api';
import { debounce } from 'lodash';

export default function Stock() {
  // State for showing/hiding add/remove pages
  const [showAddPage, setShowAddPage] = useState(false);
  const [showRemovePage, setShowRemovePage] = useState(false);
  const [searchTerm, setSearchTerm] = useState('');
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [stockData, setStockData] = useState([]);
  const [sortConfig, setSortConfig] = useState({
    key: 'lastUpdated',
    direction: 'desc'
  });

  // Product details state
  const [productDetails, setProductDetails] = useState({});

  // Form states
  const [addFormData, setAddFormData] = useState({
    boxBarcode: '',
    productBarcodes: [''], // Array of product barcodes
    quantity: '',
    note: ''
  });

  const [removeFormData, setRemoveFormData] = useState({
    boxBarcode: '',
    productBarcodes: [''], // Changed from single productBarcode to array
    quantity: '',
    note: ''
  });

  // Form processing states
  const [addProcessing, setAddProcessing] = useState(false);
  const [removeProcessing, setRemoveProcessing] = useState(false);
  const [formError, setFormError] = useState(null);

  // Load stock data
    const loadStockData = async () => {
      try {
        setLoading(true);
        setError(null);
        const data = await productApi.getCurrentStock();
        setStockData(data);
      } catch (err) {
        console.error('Error loading stock data:', err);
        setError(err.message);
      } finally {
        setLoading(false);
      }
    };

  useEffect(() => {
    loadStockData();
  }, []);

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

  // Handle box barcode change for add form
  const handleAddBoxBarcodeChange = (value) => {
    setAddFormData(prev => ({
      ...prev,
      boxBarcode: value
    }));

    if (value) {
      debouncedFetchProduct(value);
    }
  };

  // Handle box barcode change for remove form
  const handleRemoveBoxBarcodeChange = (value) => {
    setRemoveFormData(prev => ({
      ...prev,
      boxBarcode: value
    }));

    if (value) {
      debouncedFetchProduct(value);
    }
  };

  // Handle add stock form submission
  const handleAddStock = async (e) => {
    e.preventDefault();
    setFormError(null);
    setAddProcessing(true);

    try {
      // Filter out empty product barcodes
      const validProductBarcodes = addFormData.productBarcodes.filter(barcode => barcode.trim() !== '');

      // Always use bulk add endpoint
      await productApi.addStockBulk({
        boxBarcode: addFormData.boxBarcode,
        productBarcodes: validProductBarcodes,
        quantity: parseInt(addFormData.quantity),
        note: addFormData.note
      });

      await loadStockData(); // Reload stock data
      setShowAddPage(false); // Close the modal
      setAddFormData({ boxBarcode: '', productBarcodes: [''], quantity: '', note: '' }); // Reset form
    } catch (err) {
      setFormError(err.message);
    } finally {
      setAddProcessing(false);
    }
  };

  // Handle remove stock form submission
  const handleRemoveStock = async (e) => {
    e.preventDefault();
    setFormError(null);
    setRemoveProcessing(true);

    try {
      // Filter out empty product barcodes
      const validProductBarcodes = removeFormData.productBarcodes.filter(barcode => barcode.trim() !== '');

      // Always use bulk remove endpoint
      await productApi.removeStockBulk({
        boxBarcode: removeFormData.boxBarcode,
        productBarcodes: validProductBarcodes,
        quantity: parseInt(removeFormData.quantity),
        note: removeFormData.note
      });

      await loadStockData(); // Reload stock data
      setShowRemovePage(false); // Close the modal
      setRemoveFormData({ boxBarcode: '', productBarcodes: [''], quantity: '', note: '' }); // Reset form
    } catch (err) {
      setFormError(err.message);
    } finally {
      setRemoveProcessing(false);
    }
  };

  // Add new product barcode field
  const addProductBarcodeField = () => {
    setAddFormData(prev => ({
      ...prev,
      productBarcodes: [...prev.productBarcodes, '']
    }));
  };

  // Remove product barcode field
  const removeProductBarcodeField = (index) => {
    setAddFormData(prev => ({
      ...prev,
      productBarcodes: prev.productBarcodes.filter((_, i) => i !== index)
    }));
  };

  // Update product barcode value
  const updateProductBarcode = (index, value) => {
    setAddFormData(prev => ({
      ...prev,
      productBarcodes: prev.productBarcodes.map((barcode, i) => 
        i === index ? value : barcode
      )
    }));
  };

  // Add new product barcode field for remove form
  const addRemoveProductBarcodeField = () => {
    setRemoveFormData(prev => ({
      ...prev,
      productBarcodes: [...prev.productBarcodes, '']
    }));
  };

  // Remove product barcode field from remove form
  const removeRemoveProductBarcodeField = (index) => {
    setRemoveFormData(prev => ({
      ...prev,
      productBarcodes: prev.productBarcodes.filter((_, i) => i !== index)
    }));
  };

  // Update product barcode value in remove form
  const updateRemoveProductBarcode = (index, value) => {
    setRemoveFormData(prev => ({
      ...prev,
      productBarcodes: prev.productBarcodes.map((barcode, i) => 
        i === index ? value : barcode
      )
    }));
  };

  // Handle search
  const handleSearch = (e) => {
    setSearchTerm(e.target.value);
  };

  // Handle sort
  const handleSort = (key) => {
    setSortConfig(prevConfig => ({
      key,
      direction: prevConfig.key === key && prevConfig.direction === 'asc' ? 'desc' : 'asc'
    }));
  };

  // Get sort indicator
  const getSortIndicator = (key) => {
    if (sortConfig.key !== key) return '↕';
    return sortConfig.direction === 'asc' ? '↑' : '↓';
  };

  // Sort and filter data
  const sortedAndFilteredData = [...stockData]
    .filter(item => item.productName.toLowerCase().includes(searchTerm.toLowerCase()))
    .sort((a, b) => {
      if (sortConfig.key === 'totalQuantity') {
        return sortConfig.direction === 'asc' 
          ? a.totalQuantity - b.totalQuantity
          : b.totalQuantity - a.totalQuantity;
      }
      if (sortConfig.key === 'lastUpdated') {
        return sortConfig.direction === 'asc'
          ? new Date(a.lastUpdated) - new Date(b.lastUpdated)
          : new Date(b.lastUpdated) - new Date(a.lastUpdated);
      }
      return 0;
    });

  // Form input component
  const FormInput = ({ label, name, value, onChange, type = 'text', required = false }) => (
    <div className="mb-4">
      <label className="block text-sm font-medium text-gray-700 mb-1">{label}</label>
      <input
        type={type}
        name={name}
        value={value}
        onChange={onChange}
        required={required}
        className="w-full px-3 py-2 rounded-lg border-2 border-gray-300 shadow-sm focus:outline-none focus:ring-blue-500 focus:border-blue-500 text-gray-900"
      />
    </div>
  );

  return (
    <div className="space-y-6">
      <div className="flex justify-between items-center">
        <h1 className="text-3xl font-bold text-black">Current Stock</h1>
        <div className="space-x-4">
          <button
            onClick={() => {
              setShowAddPage(true);
              setShowRemovePage(false);
              setFormError(null);
            }}
            className="bg-green-600 text-white px-4 py-2 rounded-lg hover:bg-green-700 transition-colors"
          >
            Add (+)
          </button>
          <button
            onClick={() => {
              setShowRemovePage(true);
              setShowAddPage(false);
              setFormError(null);
            }}
            className="bg-red-600 text-white px-4 py-2 rounded-lg hover:bg-red-700 transition-colors"
          >
            Remove (-)
          </button>
        </div>
      </div>

      {/* Search Box */}
      <div className="bg-white shadow rounded-lg p-4">
        <input
          type="text"
          placeholder="Search by product name..."
          value={searchTerm}
          onChange={handleSearch}
          className="w-full rounded-lg border-2 border-gray-300 shadow-sm focus:border-blue-500 focus:ring-blue-500 text-gray-900 placeholder-gray-400"
        />
      </div>

      {/* Add Stock Modal */}
      {showAddPage && (
        <div className="fixed inset-0 overflow-y-auto">
          <div className="flex items-end justify-center min-h-screen pt-4 px-4 pb-20 text-center sm:block sm:p-0">
            <div className="fixed inset-0 bg-gray-500 bg-opacity-75 transition-opacity"></div>
            <span className="hidden sm:inline-block sm:align-middle sm:h-screen">&#8203;</span>
            <div className="relative inline-block align-bottom bg-white rounded-lg text-left overflow-hidden shadow-xl transform transition-all sm:my-8 sm:align-middle sm:max-w-2xl sm:w-full">
              <div className="bg-white px-4 pt-5 pb-4 sm:p-6 sm:pb-4">
                <div className="flex justify-between items-center mb-4">
                  <h2 className="text-xl font-semibold text-black">Add Stock (+)</h2>
                  <button
                    onClick={() => {
                      setShowAddPage(false);
                      setFormError(null);
                    }}
                    className="text-gray-500 hover:text-gray-700"
                  >
                    ✕
                  </button>
                </div>
                
                {formError && (
                  <div className="mb-4 p-3 bg-red-100 border border-red-400 text-red-700 rounded">
                    {formError}
                  </div>
                )}

                <form onSubmit={handleAddStock}>
                  <div className="mb-4">
                    <label className="block text-sm font-medium text-gray-700 mb-1">Box Barcode</label>
                    <input
                      type="text"
                      name="boxBarcode"
                      value={addFormData.boxBarcode}
                      onChange={(e) => handleAddBoxBarcodeChange(e.target.value)}
                      required
                      className="w-full px-3 py-2 rounded-lg border-2 border-gray-300 shadow-sm focus:outline-none focus:ring-blue-500 focus:border-blue-500 text-gray-900"
                    />
                    {productDetails[addFormData.boxBarcode] && (
                      <div className="mt-1 text-sm text-gray-600">
                        Product: {productDetails[addFormData.boxBarcode].productName}
                      </div>
                    )}
                  </div>

                  {/* Product Barcodes Section */}
                  <div className="mb-4">
                    <label className="block text-sm font-medium text-gray-700 mb-1">Product Barcodes</label>
                    {addFormData.productBarcodes.map((barcode, index) => (
                      <div key={index} className="flex gap-2 mb-2">
                        <input
                          type="text"
                          value={barcode}
                          onChange={(e) => {
                            const newBarcodes = [...addFormData.productBarcodes];
                            newBarcodes[index] = e.target.value;
                            setAddFormData({ ...addFormData, productBarcodes: newBarcodes });
                          }}
                          placeholder={`Product Barcode ${index + 1}`}
                          className="flex-1 px-3 py-2 rounded-lg border-2 border-gray-300 shadow-sm focus:outline-none focus:ring-blue-500 focus:border-blue-500 text-gray-900"
                        />
                        {index > 0 && (
                          <button
                            type="button"
                            onClick={() => removeProductBarcodeField(index)}
                            className="px-3 py-2 text-red-600 hover:text-red-800"
                          >
                            ✕
                          </button>
                        )}
                      </div>
                    ))}
                    <button
                      type="button"
                      onClick={addProductBarcodeField}
                      className="mt-2 text-sm text-blue-600 hover:text-blue-800"
                    >
                      + Add More Items
                    </button>
                  </div>

                  <div className="mb-4">
                    <label className="block text-sm font-medium text-gray-700 mb-1">Quantity</label>
                    <input
                      type="number"
                      name="quantity"
                      value={addFormData.quantity}
                      onChange={(e) => setAddFormData({ ...addFormData, quantity: e.target.value })}
                      required
                      min="1"
                      className="w-full px-3 py-2 rounded-lg border-2 border-gray-300 shadow-sm focus:outline-none focus:ring-blue-500 focus:border-blue-500 text-gray-900"
                    />
                  </div>

                  <div className="mb-4">
                    <label className="block text-sm font-medium text-gray-700 mb-1">Note (optional)</label>
                    <input
                      type="text"
                      name="note"
                      value={addFormData.note}
                      onChange={(e) => setAddFormData({ ...addFormData, note: e.target.value })}
                      className="w-full px-3 py-2 rounded-lg border-2 border-gray-300 shadow-sm focus:outline-none focus:ring-blue-500 focus:border-blue-500 text-gray-900"
                    />
                  </div>

                  <div className="mt-6 flex justify-end space-x-3">
                    <button
                      type="button"
                      onClick={() => {
                        setShowAddPage(false);
                        setFormError(null);
                      }}
                      className="px-4 py-2 border border-gray-300 rounded-md text-gray-700 hover:bg-gray-50"
                      disabled={addProcessing}
                    >
                      Cancel
                    </button>
                    <button
                      type="submit"
                      className="px-4 py-2 bg-green-600 text-white rounded-md hover:bg-green-700 disabled:opacity-50"
                      disabled={addProcessing}
                    >
                      {addProcessing ? 'Adding...' : 'Add Stock'}
                    </button>
                  </div>
                </form>
              </div>
            </div>
          </div>
        </div>
      )}

      {/* Remove Stock Modal */}
      {showRemovePage && (
        <div className="fixed inset-0 overflow-y-auto">
          <div className="flex items-end justify-center min-h-screen pt-4 px-4 pb-20 text-center sm:block sm:p-0">
            <div className="fixed inset-0 bg-gray-500 bg-opacity-75 transition-opacity"></div>
            <span className="hidden sm:inline-block sm:align-middle sm:h-screen">&#8203;</span>
            <div className="relative inline-block align-bottom bg-white rounded-lg text-left overflow-hidden shadow-xl transform transition-all sm:my-8 sm:align-middle sm:max-w-2xl sm:w-full">
              <div className="bg-white px-4 pt-5 pb-4 sm:p-6 sm:pb-4">
                <div className="flex justify-between items-center mb-4">
                  <h2 className="text-xl font-semibold text-black">Remove Stock (-)</h2>
                  <button
                    onClick={() => {
                      setShowRemovePage(false);
                      setFormError(null);
                    }}
                    className="text-gray-500 hover:text-gray-700"
                  >
                    ✕
                  </button>
                </div>

                {formError && (
                  <div className="mb-4 p-3 bg-red-100 border border-red-400 text-red-700 rounded">
                    {formError}
                  </div>
                )}

                <form onSubmit={handleRemoveStock}>
                  <div className="mb-4">
                    <label className="block text-sm font-medium text-gray-700 mb-1">Box Barcode</label>
                    <input
                      type="text"
                      name="boxBarcode"
                      value={removeFormData.boxBarcode}
                      onChange={(e) => handleRemoveBoxBarcodeChange(e.target.value)}
                      required
                      className="w-full px-3 py-2 rounded-lg border-2 border-gray-300 shadow-sm focus:outline-none focus:ring-blue-500 focus:border-blue-500 text-gray-900"
                    />
                    {productDetails[removeFormData.boxBarcode] && (
                      <div className="mt-1 text-sm text-gray-600">
                        Product: {productDetails[removeFormData.boxBarcode].productName}
                      </div>
                    )}
                  </div>

                  {/* Product Barcodes Section */}
                  <div className="mb-4">
                    <label className="block text-sm font-medium text-gray-700 mb-1">Product Barcodes</label>
                    {removeFormData.productBarcodes.map((barcode, index) => (
                      <div key={index} className="flex gap-2 mb-2">
                        <input
                          type="text"
                          value={barcode}
                          onChange={(e) => {
                            const newBarcodes = [...removeFormData.productBarcodes];
                            newBarcodes[index] = e.target.value;
                            setRemoveFormData({ ...removeFormData, productBarcodes: newBarcodes });
                          }}
                          placeholder={`Product Barcode ${index + 1}`}
                          className="flex-1 px-3 py-2 rounded-lg border-2 border-gray-300 shadow-sm focus:outline-none focus:ring-blue-500 focus:border-blue-500 text-gray-900"
                        />
                        {index > 0 && (
                          <button
                            type="button"
                            onClick={() => removeRemoveProductBarcodeField(index)}
                            className="px-3 py-2 text-red-600 hover:text-red-800"
                          >
                            ✕
                          </button>
                        )}
                      </div>
                    ))}
                    <button
                      type="button"
                      onClick={addRemoveProductBarcodeField}
                      className="mt-2 text-sm text-blue-600 hover:text-blue-800"
                    >
                      + Add More Items
                    </button>
                  </div>

                  <div className="mb-4">
                    <label className="block text-sm font-medium text-gray-700 mb-1">Quantity</label>
                    <input
                      type="number"
                      name="quantity"
                      value={removeFormData.quantity}
                      onChange={(e) => setRemoveFormData({ ...removeFormData, quantity: e.target.value })}
                      required
                      min="1"
                      className="w-full px-3 py-2 rounded-lg border-2 border-gray-300 shadow-sm focus:outline-none focus:ring-blue-500 focus:border-blue-500 text-gray-900"
                    />
                  </div>

                  <div className="mb-4">
                    <label className="block text-sm font-medium text-gray-700 mb-1">Note (optional)</label>
                    <input
                      type="text"
                      name="note"
                      value={removeFormData.note}
                      onChange={(e) => setRemoveFormData({ ...removeFormData, note: e.target.value })}
                      className="w-full px-3 py-2 rounded-lg border-2 border-gray-300 shadow-sm focus:outline-none focus:ring-blue-500 focus:border-blue-500 text-gray-900"
                    />
                  </div>

                  <div className="mt-6 flex justify-end space-x-3">
                    <button
                      type="button"
                      onClick={() => {
                        setShowRemovePage(false);
                        setFormError(null);
                      }}
                      className="px-4 py-2 border border-gray-300 rounded-md text-gray-700 hover:bg-gray-50"
                      disabled={removeProcessing}
                    >
                      Cancel
                    </button>
                    <button
                      type="submit"
                      className="px-4 py-2 bg-red-600 text-white rounded-md hover:bg-red-700 disabled:opacity-50"
                      disabled={removeProcessing}
                    >
                      {removeProcessing ? 'Removing...' : 'Remove Stock'}
                    </button>
                  </div>
                </form>
              </div>
            </div>
          </div>
        </div>
      )}

      {/* Stock Table */}
      <div className="bg-white shadow rounded-lg">
        <div className="overflow-x-auto">
          <table className="min-w-full divide-y divide-gray-200">
            <thead className="bg-gray-50">
              <tr>
                <th className="px-6 py-3 text-left text-xs font-medium text-gray-900 uppercase tracking-wider">
                  Product Name
                </th>
                <th 
                  className="px-6 py-3 text-left text-xs font-medium text-gray-900 uppercase tracking-wider cursor-pointer hover:bg-gray-100"
                  onClick={() => handleSort('totalQuantity')}
                >
                  <div className="flex items-center justify-between">
                    <span>Quantity</span>
                    <span className={`transform transition-all duration-200 ${sortConfig.key === 'totalQuantity' ? 'opacity-100' : 'opacity-50'}`}>
                      {getSortIndicator('totalQuantity')}
                    </span>
                  </div>
                </th>
                <th 
                  className="px-6 py-3 text-left text-xs font-medium text-gray-900 uppercase tracking-wider cursor-pointer hover:bg-gray-100"
                  onClick={() => handleSort('lastUpdated')}
                >
                  <div className="flex items-center justify-between">
                    <span>Last Updated</span>
                    <span className={`transform transition-all duration-200 ${sortConfig.key === 'lastUpdated' ? 'opacity-100' : 'opacity-50'}`}>
                      {getSortIndicator('lastUpdated')}
                    </span>
                  </div>
                </th>
                <th className="px-6 py-3 text-left text-xs font-medium text-gray-900 uppercase tracking-wider">
                  Highest Box #
                </th>
              </tr>
            </thead>
            <tbody className="bg-white divide-y divide-gray-200">
              {loading ? (
                <tr>
                  <td colSpan="4" className="px-6 py-4 text-center text-gray-500">
                    Loading...
                  </td>
                </tr>
              ) : error ? (
                <tr>
                  <td colSpan="4" className="px-6 py-4 text-center text-red-600">
                    Error: {error}
                  </td>
                </tr>
              ) : sortedAndFilteredData.length === 0 ? (
                <tr>
                  <td colSpan="4" className="px-6 py-4 text-center text-gray-500">
                    No products found
                  </td>
                </tr>
              ) : (
                sortedAndFilteredData.map((item) => (
                  <tr key={item.productName} className="hover:bg-gray-50">
                    <td className="px-6 py-4 whitespace-nowrap text-gray-900">
                      {item.productName}
                    </td>
                    <td className="px-6 py-4 whitespace-nowrap text-gray-900">
                      {item.totalQuantity}
                    </td>
                    <td className="px-6 py-4 whitespace-nowrap text-gray-900">
                      {new Date(item.lastUpdated).toLocaleString()}
                    </td>
                    <td className="px-6 py-4 whitespace-nowrap text-gray-900">
                      {item.highestBoxNumber}
                    </td>
                  </tr>
                ))
              )}
            </tbody>
          </table>
        </div>
      </div>
    </div>
  );
} 