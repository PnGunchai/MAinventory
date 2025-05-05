import React, { useState, useEffect } from 'react';
import { orderApi } from '../services/api';
import AddItemsModal from './AddItemsModal';
import { useTranslation } from 'react-i18next';

export default function EditOrderModal({ isOpen, onClose, order, onOrderUpdated }) {
  const { t } = useTranslation();
  const [items, setItems] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [note, setNote] = useState('');
  const [newNote, setNewNote] = useState('');
  const [showAddItemsModal, setShowAddItemsModal] = useState(false);
  const [deleteConfirmation, setDeleteConfirmation] = useState({
    isOpen: false,
    productName: null,
    productBarcode: null,
    boxBarcode: null,
    quantity: null
  });

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
      setError(t('failedToFetchOrderItems'));
      console.error(err);
    } finally {
      setLoading(false);
    }
  };

  const handleRemoveItem = async (productBarcode, boxBarcode, quantity, productName) => {
    setDeleteConfirmation({
      isOpen: true,
      productName,
      productBarcode,
      boxBarcode,
      quantity
    });
  };

  const handleDeleteConfirm = async () => {
    try {
      const { productBarcode, boxBarcode, quantity } = deleteConfirmation;
      // For serial products, use the product barcode directly
      // For non-serial products, use boxBarcode:quantity format
      const identifier = productBarcode ? productBarcode : `${boxBarcode}:${quantity}`;
      
      await orderApi.removeItemFromOrder(order.orderId, identifier);
      // Refresh items list
      fetchOrderItems();
      if (onOrderUpdated) onOrderUpdated();
    } catch (err) {
      setError(t('failedToRemoveItem'));
      console.error(err);
    } finally {
      setDeleteConfirmation({
        isOpen: false,
        productName: null,
        productBarcode: null,
        boxBarcode: null,
        quantity: null
      });
    }
  };

  const handleDeleteCancel = () => {
    setDeleteConfirmation({
      isOpen: false,
      productName: null,
      productBarcode: null,
      boxBarcode: null,
      quantity: null
    });
  };

  const handleUpdateNotes = async () => {
    try {
      if (!newNote.trim()) return;
      await orderApi.updateOrderNotes(order.orderId, newNote);
      setNewNote('');
      if (onOrderUpdated) onOrderUpdated();
    } catch (err) {
      setError(t('failedToUpdateNotes'));
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
          <h2 className="text-xl font-bold text-gray-900">{t('editOrder')} #{order.orderId}</h2>
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
            <h3 className="text-lg font-medium text-gray-900">{t('items')}</h3>
            <button
              onClick={handleAddItemsClick}
              className="px-4 py-2 bg-blue-600 text-white rounded-md hover:bg-blue-700"
            >
              {t('addItems')}
            </button>
          </div>

          {loading ? (
            <div className="text-center py-4 text-gray-600">{t('loading')}</div>
          ) : (
            <div className="bg-white border rounded-lg overflow-hidden">
              <table className="min-w-full divide-y divide-gray-200">
                <thead className="bg-gray-50">
                  <tr>
                    <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                      {t('product')}
                    </th>
                    <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                      {t('quantitySerial')}
                    </th>
                    <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                      {t('actions')}
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
                        {item.productBarcode ? item.productBarcode : `${item.quantity} ${t('units')}`}
                      </td>
                      <td className="px-6 py-4 whitespace-nowrap">
                        <button
                          onClick={() => handleRemoveItem(item.productBarcode, item.boxBarcode, item.quantity, item.productName)}
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
          <h3 className="text-lg font-medium text-gray-900 mb-2">{t('notes')}</h3>
          <textarea
            value={newNote}
            onChange={(e) => setNewNote(e.target.value)}
            className="w-full rounded-lg border-2 border-gray-300 shadow-sm focus:border-blue-500 focus:ring-blue-500 text-gray-900 h-20"
            placeholder={t('addNewNote')}
          />
          <div className="flex justify-end space-x-3 pt-4">
            <button
              onClick={onClose}
              className="px-4 py-2 border border-gray-300 rounded-md text-gray-700 hover:bg-gray-50"
            >
              {t('cancel')}
            </button>
            <button
              onClick={handleUpdateNotes}
              className="px-4 py-2 bg-blue-600 text-white rounded-md hover:bg-blue-700"
              disabled={!newNote.trim()}
            >
              {t('addNote')}
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

        {/* Delete Confirmation Modal */}
        {deleteConfirmation.isOpen && (
          <div className="fixed inset-0 bg-gray-500 bg-opacity-75 flex items-center justify-center z-50">
            <div className="bg-white p-6 rounded-lg shadow-xl max-w-md w-full">
              <h3 className="text-lg font-medium text-gray-900 mb-4">
                {t('confirmDelete')}
              </h3>
              <p className="text-sm text-gray-500 mb-4">
                {t('confirmDeleteMessage', { productName: deleteConfirmation.productName, quantity: deleteConfirmation.quantity })}
                {deleteConfirmation.productBarcode && ` (${deleteConfirmation.productBarcode})`}
                {t('confirmDeleteWarning')}
              </p>
              <div className="flex justify-end space-x-4">
                <button
                  onClick={handleDeleteCancel}
                  className="px-4 py-2 text-sm font-medium text-gray-700 bg-gray-100 hover:bg-gray-200 rounded-md"
                >
                  {t('cancel')}
                </button>
                <button
                  onClick={handleDeleteConfirm}
                  className="px-4 py-2 text-sm font-medium text-white bg-red-600 hover:bg-red-700 rounded-md"
                >
                  {t('delete')}
                </button>
              </div>
            </div>
          </div>
        )}
      </div>
    </div>
  );
} 