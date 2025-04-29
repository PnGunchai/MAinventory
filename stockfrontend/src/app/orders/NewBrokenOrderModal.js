'use client';
import { useState } from 'react';
import { orderApi } from '@/services/api';

export default function NewBrokenOrderModal({ isOpen, onClose }) {
  const [employeeId, setEmployeeId] = useState('');
  const [shopName, setShopName] = useState('');
  const [boxBarcode, setBoxBarcode] = useState('');
  const [productBarcode, setProductBarcode] = useState('');
  const [condition, setCondition] = useState('');
  const [error, setError] = useState(null);
  const [loading, setLoading] = useState(false);

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError(null);
    setLoading(true);

    try {
      await orderApi.createBrokenOrder({
        employeeId,
        shopName,
        boxBarcode,
        productBarcode,
        condition
      });
      onClose(true);
    } catch (err) {
      setError(err.message || 'Failed to create broken order');
    } finally {
      setLoading(false);
    }
  };

  const handleClose = () => {
    setEmployeeId('');
    setShopName('');
    setBoxBarcode('');
    setProductBarcode('');
    setCondition('');
    setError(null);
    onClose(false);
  };

  if (!isOpen) return null;

  return (
    <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center p-4">
      <div className="bg-white rounded-lg max-w-md w-full p-6">
        <div className="flex justify-between items-center mb-6">
          <h3 className="text-lg font-medium text-gray-900">Create Broken Order</h3>
          <button
            onClick={handleClose}
            className="text-gray-900 hover:text-gray-700"
          >
            ×
          </button>
        </div>

        {error && (
          <div className="bg-red-50 text-red-700 p-4 rounded-md mb-4">
            {error}
          </div>
        )}

        <form onSubmit={handleSubmit} className="space-y-4">
          <div>
            <label htmlFor="productBarcode" className="block text-sm font-medium text-gray-900 mb-1">
              Product Barcode
            </label>
            <input
              type="text"
              id="productBarcode"
              value={productBarcode}
              onChange={(e) => setProductBarcode(e.target.value)}
              className="w-full rounded-md border-gray-300 shadow-sm focus:border-blue-500 focus:ring-blue-500"
              required
            />
          </div>

          <div>
            <label htmlFor="boxBarcode" className="block text-sm font-medium text-gray-900 mb-1">
              Box Barcode
            </label>
            <input
              type="text"
              id="boxBarcode"
              value={boxBarcode}
              onChange={(e) => setBoxBarcode(e.target.value)}
              className="w-full rounded-md border-gray-300 shadow-sm focus:border-blue-500 focus:ring-blue-500"
              required
            />
          </div>

          <div>
            <label htmlFor="condition" className="block text-sm font-medium text-gray-900 mb-1">
              Condition
            </label>
            <textarea
              id="condition"
              value={condition}
              onChange={(e) => setCondition(e.target.value)}
              className="w-full rounded-lg border-2 border-gray-300 shadow-sm focus:border-blue-500 focus:ring-blue-500"
              rows={3}
              required
            />
          </div>

          <div>
            <label htmlFor="shopName" className="block text-sm font-medium text-gray-900 mb-1">
              Shop Name (Optional)
            </label>
            <input
              type="text"
              id="shopName"
              value={shopName}
              onChange={(e) => setShopName(e.target.value)}
              className="w-full rounded-lg border-2 border-gray-300 shadow-sm focus:border-blue-500 focus:ring-blue-500"
            />
          </div>

          <div>
            <label htmlFor="employeeId" className="block text-sm font-medium text-gray-900 mb-1">
              Employee ID (Optional)
            </label>
            <input
              type="text"
              id="employeeId"
              value={employeeId}
              onChange={(e) => setEmployeeId(e.target.value)}
              className="w-full rounded-lg border-2 border-gray-300 shadow-sm focus:border-blue-500 focus:ring-blue-500"
            />
          </div>

          <div className="flex justify-end space-x-4 mt-6">
            <button
              type="button"
              onClick={handleClose}
              className="bg-white text-gray-700 hover:bg-gray-50 px-4 py-2 rounded-md border"
            >
              Cancel
            </button>
            <button
              type="submit"
              disabled={loading}
              className="bg-blue-600 text-white hover:bg-blue-700 px-4 py-2 rounded-md disabled:opacity-50 disabled:cursor-not-allowed"
            >
              {loading ? 'Creating...' : 'Create'}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
} 