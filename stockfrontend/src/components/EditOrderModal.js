import React, { useState, useEffect } from 'react';
import { orderApi } from '../services/api';
import AddItemsModal from './AddItemsModal';

export default function EditOrderModal({ isOpen, onClose, order, onOrderUpdated }) {
  const [items, setItems] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [note, setNote] = useState('');
  const [newNote, setNewNote] = useState('');
  const [showAddItemsModal, setShowAddItemsModal] = useState(false);

  useEffect(() => {
    if (isOpen && order) {
      fetchOrderItems();
      setNote(order.note || '');
      setNewNote('');
    }
  }, [isOpen, order]);

  const fetchOrderItems = async () => {
    try {
      setLoading(true);
      const items = await orderApi.getSalesOrderItems(order.orderId);
      setItems(items);
    } catch (err) {
      setError('Failed to fetch order items');
      console.error(err);
    } finally {
      setLoading(false);
    }
  };

  const handleRemoveItem = async (productBarcode, boxBarcode, quantity) => {
    try {
      // For serial products, use the product barcode directly
      // For non-serial products, use boxBarcode:quantity format
      const identifier = productBarcode ? productBarcode : `${boxBarcode}:${quantity}`;
      
      await orderApi.removeItemFromOrder(order.orderId, identifier);
      // Refresh items list
      fetchOrderItems();
      if (onOrderUpdated) onOrderUpdated();
    } catch (err) {
      setError('Failed to remove item');
      console.error(err);
    }
  };

  const handleUpdateNotes = async () => {
    try {
      if (!newNote.trim()) return;
      await orderApi.updateOrderNotes(order.orderId, newNote);
      setNewNote('');
      if (onOrderUpdated) onOrderUpdated();
    } catch (err) {
      setError('Failed to update notes');
      console.error(err);
    }
  };

  const handleAddItemsClick = () => {
    setShowAddItemsModal(true);
  };

  const handleAddItemsClose = () => {
    setShowAddItemsModal(false);
    fetchOrderItems(); // Refresh items after adding
    if (onOrderUpdated) onOrderUpdated();
  };

  if (!isOpen) return null;

  return (
    <div className="fixed inset-0 bg-gray-600 bg-opacity-50 overflow-y-auto h-full w-full z-50">
      <div className="relative top-20 mx-auto p-5 border w-full max-w-2xl shadow-lg rounded-lg bg-white">
        <div className="flex justify-between items-center mb-4">
          <h2 className="text-xl font-bold text-gray-900">Edit Order #{order.orderId}</h2>
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

        <div className="border-t pt-4">
          <div className="flex justify-between items-center mb-2">
            <h3 className="text-lg font-medium text-gray-900">Items</h3>
            <button
              onClick={handleAddItemsClick}
              className="px-4 py-2 bg-blue-600 text-white rounded-md hover:bg-blue-700"
            >
              Add Items
            </button>
          </div>

          {loading ? (
            <div className="text-center py-4 text-gray-600">Loading...</div>
          ) : (
            <div className="bg-white border rounded-lg overflow-hidden">
              <table className="min-w-full divide-y divide-gray-200">
                <thead className="bg-gray-50">
                  <tr>
                    <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                      Product
                    </th>
                    <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                      Quantity/Serial
                    </th>
                    <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                      Actions
                    </th>
                  </tr>
                </thead>
                <tbody className="bg-white divide-y divide-gray-200">
                  {items.map((item) => (
                    <tr key={item.productBarcode || item.boxBarcode}>
                      <td className="px-6 py-4 whitespace-nowrap text-gray-900">
                        {item.productName}
                      </td>
                      <td className="px-6 py-4 whitespace-nowrap text-gray-900">
                        {item.productBarcode ? item.productBarcode : `${item.quantity} units`}
                      </td>
                      <td className="px-6 py-4 whitespace-nowrap">
                        <button
                          onClick={() => handleRemoveItem(item.productBarcode, item.boxBarcode, item.quantity)}
                          className="text-red-600 hover:text-red-800"
                        >
                          ✕
                        </button>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}
        </div>

        <div className="border-t pt-4 mt-4">
          <h3 className="text-lg font-medium text-gray-900 mb-2">Notes</h3>
          {/* Only show new note input */}
          <textarea
            value={newNote}
            onChange={(e) => setNewNote(e.target.value)}
            className="w-full rounded-lg border-2 border-gray-300 shadow-sm focus:border-blue-500 focus:ring-blue-500 text-gray-900 h-20"
            placeholder="Add a new note to append..."
          />
          <div className="flex justify-end space-x-3 pt-4">
            <button
              onClick={onClose}
              className="px-4 py-2 border border-gray-300 rounded-md text-gray-700 hover:bg-gray-50"
            >
              Cancel
            </button>
            <button
              onClick={handleUpdateNotes}
              className="px-4 py-2 bg-blue-600 text-white rounded-md hover:bg-blue-700"
              disabled={!newNote.trim()}
            >
              Add Note
            </button>
          </div>
        </div>

        {/* Add Items Modal */}
        {showAddItemsModal && (
          <AddItemsModal
            isOpen={showAddItemsModal}
            onClose={handleAddItemsClose}
            orderId={order.orderId}
            onItemsAdded={() => {
              fetchOrderItems();
              if (onOrderUpdated) onOrderUpdated();
            }}
          />
        )}
      </div>
    </div>
  );
} 