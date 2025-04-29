'use client';
import { useState, useEffect } from 'react';
import { logsApi } from '@/services/api';

export default function Logs() {
  const [logs, setLogs] = useState([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);
  const [currentPage, setCurrentPage] = useState(0);
  const [totalPages, setTotalPages] = useState(0);
  const [searchTerm, setSearchTerm] = useState('');
  const [debouncedSearchTerm, setDebouncedSearchTerm] = useState('');
  const [allLogs, setAllLogs] = useState([]); // Store all fetched logs

  // Fetch logs
  const fetchLogs = async () => {
    setLoading(true);
    setError(null);
    try {
      const response = await logsApi.getLogs(currentPage);
      
      // Client-side filtering
      let filteredLogs = response.content || [];
      if (debouncedSearchTerm) {
        const searchLower = debouncedSearchTerm.toLowerCase();
        filteredLogs = filteredLogs.filter(log => 
          (log.productName || '').toLowerCase().includes(searchLower) ||
          (log.boxNumber || '').toString().toLowerCase().includes(searchLower) ||
          (log.productBarcode || '').toString().toLowerCase().includes(searchLower) ||
          (log.operation || '').toLowerCase().includes(searchLower) ||
          (log.orderId || '').toString().toLowerCase().includes(searchLower)
        );
      }
      
      setLogs(filteredLogs);
      setTotalPages(Math.ceil(filteredLogs.length / 20)); // 20 items per page
    } catch (err) {
      console.error('Error fetching logs:', err);
      setError(err.message || 'Failed to fetch logs');
      setLogs([]);
      setTotalPages(0);
    } finally {
      setLoading(false);
    }
  };

  // Debounce search with 300ms delay
  useEffect(() => {
    const timer = setTimeout(() => {
      setDebouncedSearchTerm(searchTerm);
      setCurrentPage(0); // Reset to first page when search changes
    }, 300); // Wait 300ms after last keystroke before searching

    return () => clearTimeout(timer); // Cleanup timer
  }, [searchTerm]); // Effect runs when searchTerm changes

  // Fetch when page changes or search term changes
  useEffect(() => {
    fetchLogs();
  }, [currentPage, debouncedSearchTerm]);

  return (
    <div className="space-y-6">
      <h1 className="text-3xl font-bold text-black">System Logs</h1>

      {/* Search */}
      <div className="bg-white shadow rounded-lg p-4">
        <div className="flex gap-4 items-center">
          <div className="flex-1">
            <input
              type="text"
              placeholder="Search by product name, box number, barcode, operation, or order ID..."
              value={searchTerm}
              onChange={(e) => setSearchTerm(e.target.value)}
              className="w-full rounded-lg border-2 border-gray-300 shadow-sm focus:border-blue-500 focus:ring-blue-500 text-gray-900 placeholder-gray-400"
            />
          </div>
        </div>
      </div>

      {/* Logs Table */}
      <div className="bg-white shadow rounded-lg">
        <div className="overflow-x-auto">
          <table className="min-w-full divide-y divide-gray-200">
            <thead className="bg-gray-50">
              <tr>
                <th className="px-6 py-3 text-left text-xs font-medium text-gray-700 uppercase tracking-wider">
                  Time
                </th>
                <th className="px-6 py-3 text-left text-xs font-medium text-gray-700 uppercase tracking-wider">
                  Product Name
                </th>
                <th className="px-6 py-3 text-left text-xs font-medium text-gray-700 uppercase tracking-wider">
                  Box Number
                </th>
                <th className="px-6 py-3 text-left text-xs font-medium text-gray-700 uppercase tracking-wider">
                  Product Barcode
                </th>
                <th className="px-6 py-3 text-left text-xs font-medium text-gray-700 uppercase tracking-wider">
                  Operation
                </th>
                <th className="px-6 py-3 text-left text-xs font-medium text-gray-700 uppercase tracking-wider">
                  Quantity
                </th>
                <th className="px-6 py-3 text-left text-xs font-medium text-gray-700 uppercase tracking-wider">
                  Order ID
                </th>
              </tr>
            </thead>
            <tbody className="bg-white divide-y divide-gray-200">
              {loading ? (
                <tr>
                  <td className="px-6 py-4 text-gray-700 text-center" colSpan="7">
                    Loading logs...
                  </td>
                </tr>
              ) : error ? (
                <tr>
                  <td className="px-6 py-4 text-red-600 text-center" colSpan="7">
                    {error}
                  </td>
                </tr>
              ) : logs.length === 0 ? (
                <tr>
                  <td className="px-6 py-4 text-gray-700 text-center" colSpan="7">
                    No logs found
                  </td>
                </tr>
              ) : (
                logs.map((log, index) => (
                  <tr key={index} className="hover:bg-gray-50">
                    <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-700">
                      {new Date(log.timestamp).toLocaleString('th-TH', {
                        year: 'numeric',
                        month: '2-digit',
                        day: '2-digit',
                        hour: '2-digit',
                        minute: '2-digit',
                        hour12: false
                      })}
                    </td>
                    <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-700">
                      {log.productName || '-'}
                    </td>
                    <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-700">
                      {log.boxNumber || '-'}
                    </td>
                    <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-700">
                      {log.productBarcode || '-'}
                    </td>
                    <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-700">
                      {log.operation || '-'}
                    </td>
                    <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-700">
                      {log.quantity || 1}
                    </td>
                    <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-700">
                      {log.orderId || '-'}
                    </td>
                  </tr>
                ))
              )}
            </tbody>
          </table>
        </div>

        {/* Pagination */}
        <div className="px-4 py-3 border-t border-gray-200">
          <div className="flex justify-between items-center">
            <div className="text-sm text-gray-700">
              Page {currentPage + 1} of {totalPages}
            </div>
            <div className="space-x-2">
              <button
                onClick={() => setCurrentPage(prev => Math.max(0, prev - 1))}
                disabled={currentPage === 0}
                className={`px-3 py-1 border rounded ${
                  currentPage === 0
                    ? 'text-gray-400 bg-gray-50 cursor-not-allowed'
                    : 'text-gray-600 hover:bg-gray-50'
                }`}
              >
                Previous
              </button>
              <button
                onClick={() => setCurrentPage(prev => Math.min(totalPages - 1, prev + 1))}
                disabled={currentPage >= totalPages - 1}
                className={`px-3 py-1 border rounded ${
                  currentPage >= totalPages - 1
                    ? 'text-gray-400 bg-gray-50 cursor-not-allowed'
                    : 'text-gray-600 hover:bg-gray-50'
                }`}
              >
                Next
              </button>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
} 