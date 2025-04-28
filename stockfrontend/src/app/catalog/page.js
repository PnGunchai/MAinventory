'use client';

import { useState, useEffect } from 'react';
import { productApi } from '@/services/api';

export default function Catalog() {
  // State for form data
  const [formData, setFormData] = useState({
    boxBarcode: '',
    productName: '',
    numberSn: 0,
    description: ''
  });

  // State for products list
  const [products, setProducts] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  // Search and sort state
  const [searchTerm, setSearchTerm] = useState('');
  const [sortField, setSortField] = useState('boxBarcode');
  const [sortDirection, setSortDirection] = useState('asc');

  // Pagination state
  const [page, setPage] = useState(0);
  const [size, setSize] = useState(20);
  const [totalItems, setTotalItems] = useState(0);
  const [totalPages, setTotalPages] = useState(0);

  // Delete confirmation state
  const [deleteConfirmation, setDeleteConfirmation] = useState({
    isOpen: false,
    boxBarcode: null,
    productName: null
  });

  // Add tableLoading state separate from initial loading
  const [tableLoading, setTableLoading] = useState(false);

  // Add filter state
  const [snFilter, setSnFilter] = useState('all');

  // Load products
  const loadProducts = async () => {
    try {
      setTableLoading(true);
      setError(null);
      const response = await productApi.getProducts(page, size, sortField, sortDirection);
      console.log('Loaded products:', response);
      
      let filteredData = response.data;
      
      // Apply search filter if searchTerm exists
      if (searchTerm) {
        const searchLower = searchTerm.toLowerCase();
        filteredData = filteredData.filter(product => 
          product.boxBarcode.toLowerCase().includes(searchLower) ||
          product.productName.toLowerCase().includes(searchLower)
        );
      }

      // Apply SN filter if not 'all'
      if (snFilter !== 'all') {
        filteredData = filteredData.filter(product => 
          product.numberSn === parseInt(snFilter)
        );
      }

      setProducts(filteredData);
      setTotalItems(response.totalElements);
      setTotalPages(response.totalPages);
    } catch (err) {
      console.error('Error loading products:', err);
      setError(err.message);
    } finally {
      setTableLoading(false);
    }
  };

  // Initial load
  useEffect(() => {
    setLoading(true);
    loadProducts().finally(() => setLoading(false));
  }, []);

  // Load products when pagination, sort, search, or filter changes
  useEffect(() => {
    if (!loading) { // Don't reload if initial loading is happening
      loadProducts();
    }
  }, [page, size, sortField, sortDirection, snFilter]);

  // Debounce search to avoid too many API calls
  useEffect(() => {
    const timer = setTimeout(() => {
      setPage(0); // Reset to first page when searching
      loadProducts();
    }, 300);

    return () => clearTimeout(timer);
  }, [searchTerm]);

  // Handle search input change
  const handleSearch = (e) => {
    setSearchTerm(e.target.value);
  };

  // Handle column sort
  const handleColumnSort = (field) => {
    if (sortField === field) {
      // If clicking the same field, toggle direction
      setSortDirection(sortDirection === 'asc' ? 'desc' : 'asc');
    } else {
      // If clicking a new field, set it with ascending direction
      setSortField(field);
      setSortDirection('asc');
    }
    setPage(0); // Reset to first page when sorting
  };

  // Helper function to render sort indicator
  const getSortIndicator = (field) => {
    if (sortField !== field) return '↕';
    return sortDirection === 'asc' ? '↑' : '↓';
  };

  // Handle form input changes
  const handleInputChange = (e) => {
    const { name, value } = e.target;
    setFormData(prev => ({
      ...prev,
      [name]: name === 'numberSn' ? parseInt(value, 10) : value
    }));
  };

  // Handle form submission
  const handleSubmit = async (e) => {
    e.preventDefault();
    try {
      setError(null);
      console.log('Submitting form data:', formData); // Debug log
      await productApi.createProduct(formData);
      // Clear form
      setFormData({
        boxBarcode: '',
        productName: '',
        numberSn: 0,
        description: ''
      });
      // Reload products
      await loadProducts();
    } catch (err) {
      console.error('Error submitting form:', err); // Debug log
      setError(err.message);
    }
  };

  // Handle delete click
  const handleDeleteClick = (boxBarcode, productName) => {
    setDeleteConfirmation({
      isOpen: true,
      boxBarcode,
      productName
    });
  };

  // Handle delete confirmation
  const handleDeleteConfirm = async () => {
    try {
      await productApi.deleteProduct(deleteConfirmation.boxBarcode);
      // Refresh the product list
      loadProducts();
      // Show success message
      setError(null);
    } catch (err) {
      console.error('Error deleting product:', err);
      setError('Failed to delete product: ' + err.message);
    } finally {
      // Close the confirmation dialog
      setDeleteConfirmation({
        isOpen: false,
        boxBarcode: null,
        productName: null
      });
    }
  };

  // Handle delete cancel
  const handleDeleteCancel = () => {
    setDeleteConfirmation({
      isOpen: false,
      boxBarcode: null,
      productName: null
    });
  };

  // Handle SN filter change
  const handleSnFilterChange = (e) => {
    setSnFilter(e.target.value);
    setPage(0); // Reset to first page when filter changes
  };

  return (
    <div className="space-y-6">
      <h1 className="text-3xl font-bold text-black">Product Catalog</h1>

      {/* Error message */}
      {error && (
        <div className="bg-red-50 border border-red-200 text-red-700 px-4 py-3 rounded relative">
          {error}
        </div>
      )}

      {/* Add Product Form */}
      <div className="bg-white shadow rounded-lg">
        <form onSubmit={handleSubmit} className="p-4">
          <h2 className="text-xl font-semibold text-black mb-4">Add New Product</h2>
          <div className="space-y-4">
            <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
              <div>
                <label className="block text-sm font-medium text-gray-900">Box Barcode *</label>
                <input
                  type="text"
                  name="boxBarcode"
                  value={formData.boxBarcode}
                  onChange={handleInputChange}
                  className="mt-1 block w-full rounded-lg border-2 border-gray-300 shadow-sm focus:border-blue-500 focus:ring-blue-500 text-gray-900"
                  placeholder="Enter box barcode"
                  required
                />
              </div>
              <div>
                <label className="block text-sm font-medium text-gray-900">Product Name *</label>
                <input
                  type="text"
                  name="productName"
                  value={formData.productName}
                  onChange={handleInputChange}
                  className="mt-1 block w-full rounded-lg border-2 border-gray-300 shadow-sm focus:border-blue-500 focus:ring-blue-500 text-gray-900"
                  placeholder="Enter product name"
                  required
                />
              </div>
              <div>
                <label className="block text-sm font-medium text-gray-900">Serial Number Type *</label>
                <select
                  name="numberSn"
                  value={formData.numberSn}
                  onChange={handleInputChange}
                  className="mt-1 block w-full rounded-md border-gray-300 shadow-sm focus:border-blue-500 focus:ring-blue-500 text-gray-900"
                  required
                >
                  <option value={0}>SN=0 (Non-serialized product)</option>
                  <option value={1}>SN=1 (One serial number per box)</option>
                  <option value={2}>SN=2 (Pair serial numbers per box)</option>
                </select>
              </div>
            </div>
            <div>
              <label className="block text-sm font-medium text-gray-900">Description</label>
              <input
                type="text"
                name="description"
                value={formData.description}
                onChange={handleInputChange}
                className="mt-1 block w-full rounded-lg border-2 border-gray-300 shadow-sm focus:border-blue-500 focus:ring-blue-500 text-gray-900"
                placeholder="Enter product description"
              />
            </div>
            <div>
              <button
                type="submit"
                className="bg-blue-600 text-white px-4 py-2 rounded-lg hover:bg-blue-700"
                disabled={loading}
              >
                {loading ? 'Adding...' : 'Add Product'}
              </button>
            </div>
          </div>
        </form>
      </div>

      {/* Catalog Table */}
      <div className="bg-white shadow rounded-lg">
        {/* Search and Filter */}
        <div className="p-4 border-b border-gray-200">
          <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
            <div className="md:col-span-2">
              <input
                type="text"
                placeholder="Search by product name or box barcode..."
                className="w-full rounded-lg border-2 border-gray-300 shadow-sm focus:border-blue-500 focus:ring-blue-500 text-gray-900 placeholder-gray-400"
                value={searchTerm}
                onChange={handleSearch}
              />
            </div>
            <div>
              <select
                className="w-full rounded-lg border-2 border-gray-300 shadow-sm focus:border-blue-500 focus:ring-blue-500 text-gray-900"
                value={snFilter}
                onChange={handleSnFilterChange}
              >
                <option value="all">All Serial Number Types</option>
                <option value="0">SN=0 (Non-serialized)</option>
                <option value="1">SN=1 (Single Serial)</option>
                <option value="2">SN=2 (Pair Serial)</option>
              </select>
            </div>
          </div>
        </div>

        {/* Table */}
        <div className="overflow-x-auto relative">
          {/* Loading Overlay */}
          {tableLoading && (
            <div className="absolute inset-0 bg-white bg-opacity-75 flex items-center justify-center z-10">
              <div className="flex items-center space-x-2">
                <div className="w-2 h-2 bg-blue-600 rounded-full animate-bounce [animation-delay:-0.3s]"></div>
                <div className="w-2 h-2 bg-blue-600 rounded-full animate-bounce [animation-delay:-0.15s]"></div>
                <div className="w-2 h-2 bg-blue-600 rounded-full animate-bounce"></div>
              </div>
            </div>
          )}
          
          <table className="min-w-full divide-y divide-gray-200">
            <thead className="bg-gray-50">
              <tr>
                {/* Column headers with transition for sort indicators */}
                <th 
                  className="px-6 py-3 text-left text-xs font-medium text-gray-900 uppercase tracking-wider cursor-pointer hover:bg-gray-100 transition-all duration-200"
                  onClick={() => handleColumnSort('boxBarcode')}
                >
                  <div className="flex items-center justify-between">
                    <span>Box Barcode</span>
                    <span className={`transform transition-all duration-200 ${sortField === 'boxBarcode' ? 'opacity-100' : 'opacity-50'}`}>
                      {getSortIndicator('boxBarcode')}
                    </span>
                  </div>
                </th>
                <th 
                  className="px-6 py-3 text-left text-xs font-medium text-gray-900 uppercase tracking-wider cursor-pointer hover:bg-gray-100 transition-all duration-200"
                  onClick={() => handleColumnSort('productName')}
                >
                  <div className="flex items-center justify-between">
                    <span>Product Name</span>
                    <span className={`transform transition-all duration-200 ${sortField === 'productName' ? 'opacity-100' : 'opacity-50'}`}>
                      {getSortIndicator('productName')}
                    </span>
                  </div>
                </th>
                <th 
                  className="px-6 py-3 text-left text-xs font-medium text-gray-900 uppercase tracking-wider cursor-pointer hover:bg-gray-100 transition-all duration-200"
                  onClick={() => handleColumnSort('numberSn')}
                >
                  <div className="flex items-center justify-between">
                    <span>Serial Number Type</span>
                    <span className={`transform transition-all duration-200 ${sortField === 'numberSn' ? 'opacity-100' : 'opacity-50'}`}>
                      {getSortIndicator('numberSn')}
                    </span>
                  </div>
                </th>
                <th 
                  className="px-6 py-3 text-left text-xs font-medium text-gray-900 uppercase tracking-wider"
                >
                  Description
                </th>
                <th 
                  className="px-6 py-3 text-left text-xs font-medium text-gray-900 uppercase tracking-wider"
                >
                  Actions
                </th>
              </tr>
            </thead>
            <tbody className={`bg-white divide-y divide-gray-200 transition-all duration-300 ${tableLoading ? 'opacity-40 blur-[0.5px]' : 'opacity-100'}`}>
              {loading ? (
                <tr>
                  <td colSpan="5" className="px-6 py-4 text-center text-gray-900">
                    Loading...
                  </td>
                </tr>
              ) : error ? (
                <tr>
                  <td colSpan="5" className="px-6 py-4 text-center text-red-600">
                    {error}
                  </td>
                </tr>
              ) : products.length === 0 ? (
                <tr>
                  <td colSpan="5" className="px-6 py-4 text-center text-gray-900">
                    No products found
                  </td>
                </tr>
              ) : (
                products.map((product) => (
                  <tr 
                    key={product.boxBarcode}
                    className="transition-all duration-200 hover:bg-blue-50"
                  >
                    <td className="px-6 py-4 whitespace-nowrap text-gray-900">
                      {product.boxBarcode}
                    </td>
                    <td className="px-6 py-4 whitespace-nowrap text-gray-900">
                      {product.productName}
                    </td>
                    <td className="px-6 py-4 whitespace-nowrap text-gray-900">
                      {product.numberSn}
                    </td>
                    <td className="px-6 py-4 whitespace-nowrap text-gray-900">
                      {product.description || '-'}
                    </td>
                    <td className="px-6 py-4 whitespace-nowrap text-right text-sm font-medium">
                      <button
                        onClick={() => handleDeleteClick(product.boxBarcode, product.productName)}
                        className="text-red-600 hover:text-red-900 transition-all duration-200 hover:scale-105"
                      >
                        Delete
                      </button>
                    </td>
                  </tr>
                ))
              )}
            </tbody>
          </table>
        </div>

        {/* Pagination */}
        <div className="bg-white px-4 py-3 border-t border-gray-200 sm:px-6">
          <div className="flex justify-between items-center">
            <div className="text-sm text-gray-900">
              Showing <span className="font-medium">{products.length}</span> of{' '}
              <span className="font-medium">{totalItems}</span> results
            </div>
            <div className="flex items-center space-x-4">
              <select
                className="rounded-md border-gray-300 shadow-sm focus:border-blue-500 focus:ring-blue-500 text-gray-900"
                value={size}
                onChange={(e) => {
                  setSize(Number(e.target.value));
                  setPage(0);
                }}
              >
                <option value="20">20 per page</option>
                <option value="50">50 per page</option>
                <option value="100">100 per page</option>
              </select>
              <div className="space-x-2">
                <button
                  className="px-3 py-1 border rounded text-gray-900 hover:bg-gray-50"
                  onClick={() => setPage(Math.max(0, page - 1))}
                  disabled={page === 0 || loading}
                >
                  Previous
                </button>
                <button
                  className="px-3 py-1 border rounded text-gray-900 hover:bg-gray-50"
                  onClick={() => setPage(page + 1)}
                  disabled={page >= totalPages - 1 || loading}
                >
                  Next
                </button>
              </div>
            </div>
          </div>
        </div>
      </div>

      {/* Delete Confirmation Dialog */}
      {deleteConfirmation.isOpen && (
        <div className="fixed inset-0 bg-gray-500 bg-opacity-75 flex items-center justify-center">
          <div className="bg-white p-6 rounded-lg shadow-xl max-w-md w-full">
            <h3 className="text-lg font-medium text-gray-900 mb-4">
              Confirm Delete
            </h3>
            <p className="text-sm text-gray-500 mb-4">
              Are you sure you want to delete the product "{deleteConfirmation.productName}" (Box Barcode: {deleteConfirmation.boxBarcode})?
              This action cannot be undone.
            </p>
            <div className="flex justify-end space-x-4">
              <button
                onClick={handleDeleteCancel}
                className="px-4 py-2 text-sm font-medium text-gray-700 bg-gray-100 hover:bg-gray-200 rounded-md"
              >
                Cancel
              </button>
              <button
                onClick={handleDeleteConfirm}
                className="px-4 py-2 text-sm font-medium text-white bg-red-600 hover:bg-red-700 rounded-md"
              >
                Delete
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
} 