import React, { useState } from 'react';
import { orderApi } from '../services/api';

export default function AddItemsModal({ isOpen, onClose, orderId, onItemsAdded }) {
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);

  // State for non-serialized items
  const [nonSerializedItems, setNonSerializedItems] = useState([{ boxBarcode: '', quantity: 1 }]);

  // State for serialized items
  const [serializedGroups, setSerializedGroups] = useState([{
    boxBarcode: '',
    productBarcodes: ['']
  }]);

  const handleAddNonSerializedItem = () => {
    setNonSerializedItems([...nonSerializedItems, { boxBarcode: '', quantity: 1 }]);
  };

  const handleRemoveNonSerializedItem = (index) => {
    const newItems = nonSerializedItems.filter((_, i) => i !== index);
    setNonSerializedItems(newItems.length ? newItems : [{ boxBarcode: '', quantity: 1 }]);
  };

  const handleNonSerializedChange = (index, field, value) => {
    const newItems = [...nonSerializedItems];
    newItems[index] = { ...newItems[index], [field]: value };
    setNonSerializedItems(newItems);
  };

  const handleAddSerializedGroup = () => {
    setSerializedGroups([...serializedGroups, { boxBarcode: '', productBarcodes: [''] }]);
  };

  const handleRemoveSerializedGroup = (index) => {
    const newGroups = serializedGroups.filter((_, i) => i !== index);
    setSerializedGroups(newGroups.length ? newGroups : [{ boxBarcode: '', productBarcodes: [''] }]);
  };

  const handleAddProductBarcode = (groupIndex) => {
    const newGroups = [...serializedGroups];
    newGroups[groupIndex].productBarcodes.push('');
    setSerializedGroups(newGroups);
  };

  const handleRemoveProductBarcode = (groupIndex, barcodeIndex) => {
    const newGroups = [...serializedGroups];
    newGroups[groupIndex].productBarcodes = newGroups[groupIndex].productBarcodes.filter((_, i) => i !== barcodeIndex);
    if (newGroups[groupIndex].productBarcodes.length === 0) {
      newGroups[groupIndex].productBarcodes = [''];
    }
    setSerializedGroups(newGroups);
  };

  const handleSerializedChange = (groupIndex, field, value, barcodeIndex = null) => {
    const newGroups = [...serializedGroups];
    if (barcodeIndex !== null) {
      newGroups[groupIndex].productBarcodes[barcodeIndex] = value;
    } else {
      newGroups[groupIndex][field] = value;
    }
    setSerializedGroups(newGroups);
  };

  const handleSubmit = async () => {
    try {
      setLoading(true);
      setError(null);

      // Prepare products array
      const products = [
        // Add non-serialized items
        ...nonSerializedItems
          .filter(item => item.boxBarcode.trim())
          .map(item => ({
            identifier: `${item.boxBarcode}:${item.quantity}`
          })),
        // Add serialized items
        ...serializedGroups.flatMap(group => 
          group.productBarcodes
            .filter(barcode => barcode.trim())
            .map(barcode => ({
              identifier: barcode
            }))
        )
      ];

      if (products.length === 0) {
        setError('Please add at least one item');
        return;
      }

      await orderApi.addItemsToOrder(orderId, { products });
      if (onItemsAdded) onItemsAdded();
      onClose();
    } catch (err) {
      setError(err.message || 'Failed to add items');
    } finally {
      setLoading(false);
    }
  };

  if (!isOpen) return null;

  return (
    <div className="fixed inset-0 bg-gray-600 bg-opacity-50 overflow-y-auto h-full w-full z-50">
      <div className="relative top-20 mx-auto p-5 border w-full max-w-2xl shadow-lg rounded-lg bg-white">
        <div className="flex justify-between items-center mb-4">
          <h2 className="text-xl font-bold text-gray-900">Add Items</h2>
          <button
            onClick={onClose}
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

        {/* Non-serialized Items Section */}
        <div className="border-t pt-4">
          <div className="flex justify-between items-center mb-2">
            <h3 className="text-lg font-medium text-gray-900">Non-serialized Items</h3>
            <button
              onClick={() => setNonSerializedItems([])}
              className="text-blue-600 hover:text-blue-800 text-sm font-medium"
            >
              Remove Section
            </button>
          </div>
          {nonSerializedItems.map((item, index) => (
            <div key={index} className="flex gap-3 items-start bg-gray-50 p-3 rounded-lg mb-3">
              <div className="flex-1">
                <input
                  type="text"
                  value={item.boxBarcode}
                  onChange={(e) => handleNonSerializedChange(index, 'boxBarcode', e.target.value)}
                  className="w-full rounded-lg border-2 border-gray-300 shadow-sm focus:border-blue-500 focus:ring-blue-500 text-gray-900"
                  placeholder="Box Barcode"
                />
              </div>
              <div className="w-24">
                <input
                  type="number"
                  min="1"
                  value={item.quantity}
                  onChange={(e) => handleNonSerializedChange(index, 'quantity', parseInt(e.target.value) || 1)}
                  className="w-full rounded-lg border-2 border-gray-300 shadow-sm focus:border-blue-500 focus:ring-blue-500 text-gray-900"
                />
              </div>
              <button
                onClick={() => handleRemoveNonSerializedItem(index)}
                className="text-red-600 hover:text-red-800"
              >
                ✕
              </button>
            </div>
          ))}
          <button
            onClick={handleAddNonSerializedItem}
            className="text-blue-600 hover:text-blue-800 text-sm font-medium"
          >
            + Add Item
          </button>
        </div>

        {/* Serialized Items Section */}
        <div className="border-t pt-4 mt-4">
          <div className="flex justify-between items-center mb-2">
            <h3 className="text-lg font-medium text-gray-900">Serialized Items</h3>
            <button
              onClick={() => setSerializedGroups([])}
              className="text-blue-600 hover:text-blue-800 text-sm font-medium"
            >
              Remove Section
            </button>
          </div>
          {serializedGroups.map((group, groupIndex) => (
            <div key={groupIndex} className="bg-gray-50 p-4 rounded-lg mb-4">
              <div className="flex gap-4 items-start">
                <div className="flex-1">
                  <label className="block text-sm font-medium text-gray-700 mb-1">
                    Box Barcode
                  </label>
                  <input
                    type="text"
                    value={group.boxBarcode}
                    onChange={(e) => handleSerializedChange(groupIndex, 'boxBarcode', e.target.value)}
                    className="w-full rounded-lg border-2 border-gray-300 shadow-sm focus:border-blue-500 focus:ring-blue-500 text-gray-900"
                    placeholder="Box Barcode"
                  />
                </div>
                <button
                  onClick={() => handleRemoveSerializedGroup(groupIndex)}
                  className="text-red-600 hover:text-red-800"
                >
                  Remove Group
                </button>
              </div>
              {group.productBarcodes.map((barcode, barcodeIndex) => (
                <div key={barcodeIndex} className="flex gap-3 items-start pl-4 border-l-2 border-gray-200 mt-3">
                  <div className="flex-1">
                    <label className="block text-sm font-medium text-gray-700 mb-1">
                      Product Barcode
                    </label>
                    <input
                      type="text"
                      value={barcode}
                      onChange={(e) => handleSerializedChange(groupIndex, 'productBarcodes', e.target.value, barcodeIndex)}
                      className="w-full rounded-lg border-2 border-gray-300 shadow-sm focus:border-blue-500 focus:ring-blue-500 text-gray-900"
                      placeholder="Product Barcode"
                    />
                  </div>
                  <button
                    onClick={() => handleRemoveProductBarcode(groupIndex, barcodeIndex)}
                    className="text-red-600 hover:text-red-800"
                  >
                    Remove
                  </button>
                </div>
              ))}
              <button
                onClick={() => handleAddProductBarcode(groupIndex)}
                className="ml-4 text-blue-600 hover:text-blue-800 text-sm mt-3"
              >
                + Add Product Barcode
              </button>
            </div>
          ))}
          <button
            onClick={handleAddSerializedGroup}
            className="text-blue-600 hover:text-blue-800 text-sm font-medium"
          >
            + Add Group
          </button>
        </div>

        <div className="flex justify-end space-x-3 pt-4 mt-4 border-t">
          <button
            onClick={onClose}
            className="px-4 py-2 border border-gray-300 rounded-md text-gray-700 hover:bg-gray-50"
          >
            Cancel
          </button>
          <button
            onClick={handleSubmit}
            disabled={loading}
            className={`px-4 py-2 bg-blue-600 text-white rounded-md hover:bg-blue-700 
              ${loading ? 'opacity-50 cursor-not-allowed' : ''}`}
          >
            {loading ? 'Adding...' : 'Add Items'}
          </button>
        </div>
      </div>
    </div>
  );
} 